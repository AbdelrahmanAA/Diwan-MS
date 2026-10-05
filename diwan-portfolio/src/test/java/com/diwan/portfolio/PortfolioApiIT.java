package com.diwan.portfolio;

import com.diwan.common.security.GatewaySigner;
import com.diwan.portfolio.entity.Instrument;
import com.diwan.portfolio.kafka.AlertPublisher;
import com.diwan.portfolio.kafka.PortfolioAlertEvent;
import com.diwan.portfolio.market.MarketDataProvider;
import com.diwan.portfolio.repository.InstrumentRepository;
import com.diwan.portfolio.service.AlertService;
import com.diwan.portfolio.service.FxRefreshService;
import com.diwan.portfolio.service.QuoteService;
import com.diwan.portfolio.service.UserDataService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The whole service against a real MySQL: requests carry gateway-signed identity headers, market data comes
 * from the fake provider, exchange rates from the fake FX provider (1 USD = 48.50 EGP).
 */
@SpringBootTest(properties = {
        "diwan.portfolio.jobs.enabled=false",
        "spring.kafka.listener.auto-startup=false",
        "diwan.portfolio.market-data.provider=fake",
        "diwan.portfolio.fx.provider=fake"})
@AutoConfigureMockMvc
@ActiveProfiles("DEV")
@Testcontainers
class PortfolioApiIT {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    private static final String SECRET = "dev-only-jwt-secret-do-not-use-outside-dev-0123456789";
    private static final GatewaySigner SIGNER = GatewaySigner.fromSecret(SECRET);
    private static final AtomicLong USERS = new AtomicLong(1000);

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired FxRefreshService fx;
    @Autowired QuoteService quotes;
    @Autowired AlertService alerts;
    @Autowired InstrumentRepository instruments;
    @Autowired UserDataService userData;
    @MockBean AlertPublisher publisher;

    private long user;

    @BeforeEach
    void setUp() {
        fx.refresh();
        user = USERS.incrementAndGet();
    }

    private static MockHttpServletRequestBuilder signed(MockHttpServletRequestBuilder req, long userId) {
        long ts = System.currentTimeMillis();
        String id = String.valueOf(userId);
        return req.header("X-User-Id", id).header("X-User-Email", "u" + id + "@test").header("X-User-Role", "USER")
                .header("X-Gateway-Timestamp", ts).header("X-Gateway-Signature", SIGNER.sign(id, "u" + id + "@test", "USER", ts));
    }

    private ResultActions call(MockHttpServletRequestBuilder req, Object body) throws Exception {
        return call(req, body, user, "en");
    }

    private ResultActions call(MockHttpServletRequestBuilder req, Object body, long userId, String lang) throws Exception {
        MockHttpServletRequestBuilder r = signed(req, userId).header("Accept-Language", lang);
        if (body != null) {
            r = r.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        }
        return mvc.perform(r);
    }

    private JsonNode body(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }

    private long createPortfolio(String name, String currency) throws Exception {
        return body(call(post("/api/portfolio/portfolios"),
                java.util.Map.of("name", name, "baseCurrency", currency)).andExpect(status().isCreated()))
                .get("id").asLong();
    }

    private static java.util.Map<String, Object> buy(String qty, String price) {
        return java.util.Map.of("type", "BUY", "tradeDate", LocalDate.now().minusDays(30).toString(),
                "quantity", qty, "price", price, "fees", "0");
    }

    @Test
    void requestsWithoutAGatewaySignatureAreRejected() throws Exception {
        mvc.perform(get("/api/portfolio/portfolios").header("X-User-Id", "1")).andExpect(status().isUnauthorized());
    }

    @Test
    void fullPortfolioFlowGroupedByCountryThenType() throws Exception {
        long pid = createPortfolio("Retirement", "USD");

        // US stock through a symbol: country/currency come from the instrument
        JsonNode aapl = body(call(post("/api/portfolio/portfolios/" + pid + "/holdings"), java.util.Map.of(
                "assetType", "STOCK", "name", "Apple", "symbol", "aapl",
                "initialTransaction", buy("10", "150"))).andExpect(status().isCreated()));
        assertThat(aapl.get("countryCode").asText()).isEqualTo("US");
        assertThat(aapl.get("currency").asText()).isEqualTo("USD");
        assertThat(aapl.get("priceSource").asText()).isEqualTo("QUOTE");
        assertThat(aapl.get("quantity").decimalValue()).isEqualByComparingTo("10");

        // Egyptian apartment with a manual valuation, in EGP
        JsonNode flat = body(call(post("/api/portfolio/portfolios/" + pid + "/holdings"), java.util.Map.of(
                "assetType", "REAL_ESTATE", "name", "شقة التجمع", "countryCode", "EG", "currency", "EGP",
                "attributes", java.util.Map.of("address", "New Cairo, Building 12"),
                "initialTransaction", buy("1", "4850000"))).andExpect(status().isCreated()));
        long flatId = flat.get("id").asLong();
        call(post("/api/portfolio/holdings/" + flatId + "/valuations"), java.util.Map.of(
                "valuationDate", LocalDate.now().toString(), "value", "5820000")).andExpect(status().isCreated());

        // Cash deposit in EGP
        call(post("/api/portfolio/portfolios/" + pid + "/holdings"), java.util.Map.of(
                "assetType", "CASH", "name", "Savings", "countryCode", "EG", "currency", "EGP",
                "initialTransaction", java.util.Map.of("type", "DEPOSIT", "tradeDate", LocalDate.now().toString(),
                        "amount", "97000"))).andExpect(status().isCreated());

        JsonNode summary = body(call(get("/api/portfolio/portfolios/" + pid + "/summary"), null).andExpect(status().isOk()));
        JsonNode groups = summary.get("groups");
        assertThat(groups).hasSize(2);
        JsonNode egypt = groups.get(0); // 5820000 + 97000 EGP = 122000 USD, above the Apple position
        assertThat(egypt.get("key").asText()).isEqualTo("EG");
        assertThat(egypt.get("label").asText()).isEqualTo("Egypt");
        assertThat(egypt.get("totals").get("marketValue").decimalValue()).isEqualByComparingTo("122000.00");
        assertThat(egypt.get("groups")).extracting(g -> g.get("key").asText()).containsExactly("REAL_ESTATE", "CASH");
        JsonNode flatView = egypt.get("groups").get(0).get("holdings").get(0);
        assertThat(flatView.get("name").asText()).isEqualTo("شقة التجمع");
        assertThat(flatView.get("unrealizedPnl").decimalValue()).isEqualByComparingTo("20000.00"); // 970000 EGP
        assertThat(flatView.get("attributes").get("address").asText()).isEqualTo("New Cairo, Building 12");
        assertThat(summary.get("totals").get("marketValue").decimalValue()).isGreaterThan(new BigDecimal("122000"));

        // Arabic labels for the same data
        JsonNode ar = body(call(get("/api/portfolio/portfolios/" + pid + "/summary"), null, user, "ar"));
        assertThat(ar.get("groups").get(0).get("label").asText()).isEqualTo("مصر");
        assertThat(ar.get("groups").get(0).get("groups").get(0).get("label").asText()).isEqualTo("عقارات");

        // Sensitive text is ciphertext in the database
        String rawName = jdbc.queryForObject("select name from holding where id = ?", String.class, flatId);
        String rawAttributes = jdbc.queryForObject("select attributes from holding where id = ?", String.class, flatId);
        assertThat(rawName).startsWith("enc:dev:").doesNotContain("شقة");
        assertThat(rawAttributes).startsWith("enc:dev:").doesNotContain("Cairo");
        assertThat(jdbc.queryForObject("select name from portfolio where id = ?", String.class, pid)).startsWith("enc:");
    }

    @Test
    void sellingMoreThanHeldIsRejectedAndChangesNothing() throws Exception {
        long pid = createPortfolio("Trading", "USD");
        long hid = body(call(post("/api/portfolio/portfolios/" + pid + "/holdings"), java.util.Map.of(
                "assetType", "STOCK", "name", "Microsoft", "symbol", "MSFT", "initialTransaction", buy("5", "400"))))
                .get("id").asLong();

        call(post("/api/portfolio/holdings/" + hid + "/transactions"), java.util.Map.of(
                "type", "SELL", "tradeDate", LocalDate.now().toString(), "quantity", "6", "price", "420"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("transaction.exceeds_quantity"));
        call(post("/api/portfolio/holdings/" + hid + "/transactions"), java.util.Map.of(
                "type", "SELL", "tradeDate", LocalDate.now().toString(), "quantity", "6", "price", "420"), user, "ar")
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.startsWith("هذه العملية تبيع")));

        JsonNode h = body(call(get("/api/portfolio/holdings/" + hid), null));
        assertThat(h.get("quantity").decimalValue()).isEqualByComparingTo("5");

        JsonNode sell = body(call(post("/api/portfolio/holdings/" + hid + "/transactions"), java.util.Map.of(
                "type", "SELL", "tradeDate", LocalDate.now().toString(), "quantity", "2", "price", "450", "fees", "1"))
                .andExpect(status().isCreated()));
        h = body(call(get("/api/portfolio/holdings/" + hid), null));
        assertThat(h.get("quantity").decimalValue()).isEqualByComparingTo("3");
        assertThat(h.get("realizedPnl").decimalValue()).isEqualByComparingTo("99.00");

        // deleting the sell restores the position
        call(delete("/api/portfolio/transactions/" + sell.get("id").asLong()), null).andExpect(status().isNoContent());
        assertThat(body(call(get("/api/portfolio/holdings/" + hid), null)).get("quantity").decimalValue()).isEqualByComparingTo("5");
    }

    @Test
    void anotherUserCannotSeeOrChangeAnything() throws Exception {
        long pid = createPortfolio("Mine", "USD");
        long hid = body(call(post("/api/portfolio/portfolios/" + pid + "/holdings"), java.util.Map.of(
                "assetType", "PRIVATE_BUSINESS", "name", "Bakery", "countryCode", "EG", "currency", "EGP")))
                .get("id").asLong();
        long intruder = user + 500_000;

        call(get("/api/portfolio/portfolios/" + pid), null, intruder, "en").andExpect(status().isNotFound());
        call(get("/api/portfolio/portfolios/" + pid + "/summary"), null, intruder, "en").andExpect(status().isNotFound());
        call(get("/api/portfolio/holdings/" + hid), null, intruder, "en").andExpect(status().isNotFound());
        call(delete("/api/portfolio/holdings/" + hid), null, intruder, "en").andExpect(status().isNotFound());
        call(post("/api/portfolio/portfolios/" + pid + "/holdings"), java.util.Map.of(
                "assetType", "CASH", "name", "x", "countryCode", "EG", "currency", "EGP"), intruder, "en")
                .andExpect(status().isNotFound());
        assertThat(body(call(get("/api/portfolio/portfolios"), null, intruder, "en"))).isEmpty();
        call(get("/api/portfolio/holdings/" + hid), null).andExpect(status().isOk());
    }

    @Test
    void targetsShowDriftAndRebalancingSuggestion() throws Exception {
        long pid = createPortfolio("Balanced", "USD");
        call(post("/api/portfolio/portfolios/" + pid + "/holdings"), java.util.Map.of(
                "assetType", "CASH", "name", "Cash", "countryCode", "US", "currency", "USD",
                "initialTransaction", java.util.Map.of("type", "DEPOSIT", "tradeDate", LocalDate.now().toString(),
                        "amount", "10000"))).andExpect(status().isCreated());

        call(put("/api/portfolio/portfolios/" + pid + "/targets"), java.util.Map.of("dimension", "ASSET_TYPE",
                "targets", List.of(java.util.Map.of("key", "STOCK", "percent", 60), java.util.Map.of("key", "CASH", "percent", 30))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("target.sum_not_100"));
        call(put("/api/portfolio/portfolios/" + pid + "/targets"), java.util.Map.of("dimension", "ASSET_TYPE",
                "targets", List.of(java.util.Map.of("key", "STOCK", "percent", 60), java.util.Map.of("key", "CASH", "percent", 40))))
                .andExpect(status().isOk());

        JsonNode alloc = body(call(get("/api/portfolio/portfolios/" + pid + "/allocation?dimension=ASSET_TYPE"), null));
        assertThat(alloc.get("hasTargets").asBoolean()).isTrue();
        assertThat(alloc.get("maxDriftPercent").decimalValue()).isEqualByComparingTo("60");
        JsonNode cash = alloc.get("rows").get(0);
        assertThat(cash.get("key").asText()).isEqualTo("CASH");
        assertThat(cash.get("suggestedChange").decimalValue()).isEqualByComparingTo("-6000.00");
        JsonNode stock = alloc.get("rows").get(1);
        assertThat(stock.get("key").asText()).isEqualTo("STOCK");
        assertThat(stock.get("suggestedChange").decimalValue()).isEqualByComparingTo("6000.00");

        JsonNode goal = body(call(post("/api/portfolio/portfolios/" + pid + "/goals"), java.util.Map.of(
                "name", "House", "targetAmount", "40000", "targetDate", LocalDate.now().plusYears(2).toString()))
                .andExpect(status().isCreated()));
        assertThat(goal.get("progressPercent").decimalValue()).isEqualByComparingTo("25");
        assertThat(goal.get("remainingAmount").decimalValue()).isEqualByComparingTo("30000.00");
    }

    @Test
    void alertsFireOnceWithinTheCooldownAndArePublished() throws Exception {
        call(post("/api/portfolio/alerts/rules"), java.util.Map.of(
                "type", "PRICE_ABOVE", "symbol", "NVDA", "threshold", "1")).andExpect(status().isCreated());

        Instrument nvda = instruments.findBySymbol("NVDA").orElseThrow();
        var quote = quotes.store(nvda.getId(), new MarketDataProvider.Quote(new BigDecimal("125"), new BigDecimal("120"),
                new BigDecimal("5"), new BigDecimal("4.1667"), null, null, Instant.now()));

        assertThat(alerts.evaluateQuote(nvda, quote)).isEqualTo(1);
        assertThat(alerts.evaluateQuote(nvda, quote)).isZero(); // cooldown

        JsonNode inbox = body(call(get("/api/portfolio/alerts"), null, user, "ar"));
        assertThat(inbox.get("totalItems").asLong()).isEqualTo(1);
        assertThat(inbox.get("items").get(0).get("message").asText()).contains("NVDA").contains("125.00");
        assertThat(body(call(get("/api/portfolio/alerts/unread-count"), null)).get("unread").asLong()).isEqualTo(1);
        call(post("/api/portfolio/alerts/read-all"), null).andExpect(status().isOk());
        assertThat(body(call(get("/api/portfolio/alerts/unread-count"), null)).get("unread").asLong()).isZero();

        ArgumentCaptor<PortfolioAlertEvent> event = ArgumentCaptor.forClass(PortfolioAlertEvent.class);
        verify(publisher, atLeastOnce()).publish(event.capture());
        assertThat(event.getValue().userId()).isEqualTo(user);
        assertThat(event.getValue().title()).contains("NVDA");
    }

    @Test
    void referenceDataIsLocalized() throws Exception {
        JsonNode ar = body(call(get("/api/portfolio/reference"), null, user, "ar").andExpect(status().isOk()));
        assertThat(ar.get("assetTypes")).anySatisfy(n -> {
            assertThat(n.get("code").asText()).isEqualTo("STOCK");
            assertThat(n.get("label").asText()).isEqualTo("أسهم");
        });
        assertThat(ar.get("countries")).anySatisfy(n -> {
            assertThat(n.get("code").asText()).isEqualTo("SA");
            assertThat(n.get("label").asText()).isEqualTo("المملكة العربية السعودية");
        });
        assertThat(ar.get("marketDataProvider").asText()).isEqualTo("fake");
    }

    @Test
    void validationMessagesFollowTheRequestLanguage() throws Exception {
        call(post("/api/portfolio/portfolios"), java.util.Map.of("name", ""), user, "ar")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("name: حقل مطلوب"));
        call(post("/api/portfolio/portfolios"), java.util.Map.of("name", ""), user, "en")
                .andExpect(jsonPath("$.message").value("name: is required"));
    }

    @Test
    void deletingTheUserErasesAllTheirData() throws Exception {
        long pid = createPortfolio("Gone soon", "USD");
        call(post("/api/portfolio/portfolios/" + pid + "/holdings"), java.util.Map.of(
                "assetType", "STOCK", "name", "Tesla", "symbol", "TSLA", "initialTransaction", buy("1", "200"))).andExpect(status().isCreated());
        call(post("/api/portfolio/tags"), java.util.Map.of("name", "growth")).andExpect(status().isCreated());
        call(post("/api/portfolio/watchlist"), java.util.Map.of("symbol", "AMZN")).andExpect(status().isOk());

        userData.deleteAll(user);
        userData.deleteAll(user); // events arrive at least once

        assertThat(body(call(get("/api/portfolio/portfolios"), null))).isEmpty();
        assertThat(body(call(get("/api/portfolio/tags"), null))).isEmpty();
        assertThat(body(call(get("/api/portfolio/watchlist"), null))).isEmpty();
        assertThat(jdbc.queryForObject("select count(*) from holding h join portfolio p on p.id = h.portfolio_id where p.user_id = ?",
                Integer.class, user)).isZero();
    }
}
