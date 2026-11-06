package com.diwan.portfolio.market;

import com.diwan.portfolio.domain.AssetType;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SaudiAndGoldProvidersTest {

    private static final String BASE = "https://sahmk.test/api/v1";
    /** Sunday 12:00 in Riyadh */
    private static final Instant TRADING = Instant.parse("2026-11-08T09:00:00Z");
    /** Friday 12:00 in Riyadh */
    private static final Instant WEEKEND = Instant.parse("2026-11-06T09:00:00Z");
    private static final String COMPANIES = """
            {"count":3,"total":3,"limit":500,"offset":0,"results":[
              {"symbol":"2222","name_ar":"أرامكو السعودية","name_en":"Saudi Aramco","security_type":"Equity","is_etf":false},
              {"symbol":"1120","name_ar":"مصرف الراجحي","name_en":"Al Rajhi Bank","security_type":"Equity","is_etf":false},
              {"symbol":"9405","name_ar":"صندوق","name_en":"Falcom Saudi Equity ETF","security_type":"ETF","is_etf":true}]}""";

    private static class MovableClock extends Clock {
        private Instant now;

        MovableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { throw new UnsupportedOperationException(); }
        @Override public Instant instant() { return now; }
    }

    private static SahmkMarketDataProvider sahmk(RestClient.Builder b, int daily, Clock clock) {
        return new SahmkMarketDataProvider(b, BASE, "secret-key", new SahmkMarketDataProvider.Settings(daily, 10, 6000,
                LocalTime.of(10, 15), LocalTime.of(16, 0), Duration.ofMinutes(15)), clock);
    }

    private static SahmkMarketDataProvider freeSahmk(Clock clock) {
        return sahmk(RestClient.builder(), 95, clock);
    }

    @Test
    void sahmkQuoteDropsTheSuffixSendsTheKeyInAHeaderAndMapsTheFields() {
        RestClient.Builder b = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(b).build();
        server.expect(requestTo(BASE + "/quote/2222/"))
                .andExpect(header("X-API-Key", "secret-key"))
                .andRespond(withSuccess("""
                        {"symbol":"2222","price":26.16,"previous_close":26.0,"change":0.16,"change_percent":0.62,
                         "high":26.3,"low":25.9,"updated_at":"2026-11-08T12:00:00+03:00","is_delayed":true}""",
                        MediaType.APPLICATION_JSON));

        var q = sahmk(b, 95, Clock.fixed(TRADING, ZoneOffset.UTC)).quote("2222.SR").orElseThrow();

        assertThat(q.price()).isEqualByComparingTo("26.16");
        assertThat(q.previousClose()).isEqualByComparingTo("26");
        assertThat(q.changePercent()).isEqualByComparingTo("0.62");
        assertThat(q.time()).isEqualTo(Instant.parse("2026-11-08T09:00:00Z"));
        server.verify();
    }

    @Test
    void sahmkStopsCallingOnceTheDailyLimitIsReachedAndStartsAgainAfterMidnightInRiyadh() {
        RestClient.Builder b = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(b).build();
        server.expect(ExpectedCount.times(3), requestTo(BASE + "/quote/2222/"))
                .andRespond(withSuccess("{\"price\":26}", MediaType.APPLICATION_JSON));
        MovableClock clock = new MovableClock(Instant.parse("2026-11-08T20:00:00Z")); // 23:00 in Riyadh
        var p = sahmk(b, 2, clock);

        assertThat(p.quote("2222.SR")).isPresent();
        assertThat(p.quote("2222.SR")).isPresent();
        assertThat(p.quote("2222.SR")).isEmpty(); // no request made

        clock.advance(Duration.ofHours(2)); // 01:00 in Riyadh, still the same day in UTC
        assertThat(p.quote("2222.SR")).isPresent();
        server.verify();
    }

    @Test
    void sahmkTakesOverTheUsageItReportsSoARestartDoesNotOverspend() {
        RestClient.Builder b = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(b).build();
        HttpHeaders used = new HttpHeaders();
        used.add("X-RateLimit-Limit", "100");
        used.add("X-RateLimit-Remaining", "5");
        server.expect(ExpectedCount.once(), requestTo(BASE + "/quote/2222/"))
                .andRespond(withSuccess("{\"price\":26}", MediaType.APPLICATION_JSON).headers(used));
        var p = sahmk(b, 95, Clock.fixed(TRADING, ZoneOffset.UTC));

        assertThat(p.quote("2222.SR")).isPresent();
        assertThat(p.quote("2222.SR")).isEmpty();
        server.verify();
    }

    @Test
    void sahmkQuotaErrorEndsTheDayButAPerMinuteLimitDoesNot() {
        RestClient.Builder b = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(b).build();
        server.expect(requestTo(BASE + "/quote/2222/")).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                .contentType(MediaType.APPLICATION_JSON).body("{\"error\":{\"code\":\"RATE_LIMITED\"}}"));
        server.expect(requestTo(BASE + "/quote/2222/")).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                .contentType(MediaType.APPLICATION_JSON).body("{\"error\":{\"code\":\"RATE_LIMITED\"},\"upgrade\":{}}"));
        var p = sahmk(b, 95, Clock.fixed(TRADING, ZoneOffset.UTC));

        assertThat(p.quote("2222.SR")).isEmpty();
        assertThat(p.quote("2222.SR")).isEmpty();
        assertThat(p.quote("2222.SR")).isEmpty(); // third: no request made
        server.verify();
    }

    @Test
    void sahmkSearchLoadsTheCompanyListOnceAndFindsArabicNames() {
        RestClient.Builder b = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(b).build();
        server.expect(ExpectedCount.once(), requestTo(BASE + "/companies/?limit=500&offset=0"))
                .andRespond(withSuccess(COMPANIES, MediaType.APPLICATION_JSON));
        var p = sahmk(b, 95, Clock.fixed(TRADING, ZoneOffset.UTC));

        assertThat(p.search("aramco")).extracting(MarketDataProvider.SymbolMatch::symbol).containsExactly("2222.SR");
        assertThat(p.search("الراجحي")).extracting(MarketDataProvider.SymbolMatch::symbol).containsExactly("1120.SR");
        assertThat(p.search("1120")).hasSize(1);
        assertThat(p.search("nothing")).isEmpty();
        server.verify();
    }

    @Test
    void sahmkProfileIsAlwaysSaudiArabiaInRiyalsAndUnknownSymbolsAreRejected() {
        RestClient.Builder b = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(b).build();
        server.expect(ExpectedCount.once(), requestTo(BASE + "/companies/?limit=500&offset=0"))
                .andRespond(withSuccess(COMPANIES, MediaType.APPLICATION_JSON));
        var p = sahmk(b, 95, Clock.fixed(TRADING, ZoneOffset.UTC));

        var aramco = p.profile("2222.SR").orElseThrow();
        assertThat(aramco.countryCode()).isEqualTo("SA");
        assertThat(aramco.currency()).isEqualTo("SAR");
        assertThat(aramco.assetType()).isEqualTo(AssetType.STOCK);
        assertThat(aramco.name()).isEqualTo("Saudi Aramco");
        assertThat(p.profile("9405.SR").orElseThrow().assetType()).isEqualTo(AssetType.ETF);
        assertThat(p.profile("0000.SR")).isEmpty();
        server.verify();
    }

    @Test
    void sahmkRefreshesInsideTheWindowOnlyButAlwaysPricesANewSymbol() {
        Map<String, Instant> symbols = new HashMap<>();
        symbols.put("2222.SR", Instant.parse("2026-11-05T12:00:00Z"));
        symbols.put("1120.SR", null);

        assertThat(freeSahmk(Clock.fixed(TRADING, ZoneOffset.UTC)).dueForRefresh(symbols))
                .containsExactly("1120.SR", "2222.SR");
        assertThat(freeSahmk(Clock.fixed(WEEKEND, ZoneOffset.UTC)).dueForRefresh(symbols))
                .containsExactly("1120.SR");
        assertThat(freeSahmk(Clock.fixed(Instant.parse("2026-11-08T14:00:00Z"), ZoneOffset.UTC)).dueForRefresh(symbols))
                .containsExactly("1120.SR"); // Sunday 17:00 in Riyadh: after the window
    }

    @Test
    void sahmkSpreadsItsAllowanceSoAFullDayStaysInsideTheLimit() {
        var p = freeSahmk(Clock.fixed(TRADING, ZoneOffset.UTC));

        assertThat(p.refreshInterval(1)).isEqualTo(Duration.ofMinutes(15));
        for (int symbols : List.of(5, 10, 20, 90)) {
            long refreshesPerSymbol = Duration.ofMinutes(345).dividedBy(p.refreshInterval(symbols));
            assertThat(refreshesPerSymbol * symbols).isLessThanOrEqualTo(85);
        }
    }

    @Test
    void sahmkDoesNotRetryAFailedSymbolOnEveryRun() {
        RestClient.Builder b = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(b).build();
        server.expect(requestTo(BASE + "/quote/2222/")).andRespond(withStatus(HttpStatus.CONFLICT));
        var p = sahmk(b, 95, Clock.fixed(TRADING, ZoneOffset.UTC));
        Map<String, Instant> symbols = new HashMap<>();
        symbols.put("2222.SR", null);

        assertThat(p.quote("2222.SR")).isEmpty();
        assertThat(p.dueForRefresh(symbols)).isEmpty();
    }

    @Test
    void goldKaratsComeFromOneSpotPriceInUsdPerGram() {
        RestClient.Builder b = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(b).build();
        server.expect(ExpectedCount.once(), requestTo("https://gold.test/price/XAU"))
                .andRespond(withSuccess("""
                        {"currency":"USD","name":"Gold","price":3110.34768,"symbol":"XAU","updatedAt":"2026-11-06T15:00:05Z"}""",
                        MediaType.APPLICATION_JSON));
        var p = new GoldApiMarketDataProvider(b, "https://gold.test", Clock.fixed(TRADING, ZoneOffset.UTC));

        assertThat(p.quote("GOLD-24K").orElseThrow().price()).isEqualByComparingTo("100");
        assertThat(p.quote("GOLD-21K").orElseThrow().price()).isEqualByComparingTo("87.5");
        assertThat(p.quote("GOLD-18K").orElseThrow().price()).isEqualByComparingTo("75");
        assertThat(p.quote("GOLD-18K").orElseThrow().time()).isEqualTo(Instant.parse("2026-11-06T15:00:05Z"));
        assertThat(p.quote("GOLD-9K")).isEmpty();
        server.verify();
    }

    @Test
    void goldOutageIsEmptyAndWaitsBeforeTryingAgain() {
        RestClient.Builder b = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(b).build();
        server.expect(ExpectedCount.once(), requestTo("https://gold.test/price/XAU"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        var p = new GoldApiMarketDataProvider(b, "https://gold.test", Clock.fixed(TRADING, ZoneOffset.UTC));

        assertThat(p.quote("GOLD-24K")).isEmpty();
        assertThat(p.quote("GOLD-21K")).isEmpty();
        server.verify();
    }

    @Test
    void goldIsFoundInEnglishAndArabicAndCanBeHeldInAnyCurrency() {
        var p = new GoldApiMarketDataProvider(RestClient.builder(), "https://gold.test", Clock.systemUTC());

        assertThat(p.search("gold")).hasSize(4);
        assertThat(p.search("ذهب")).hasSize(4);
        assertThat(p.search("21k")).extracting(MarketDataProvider.SymbolMatch::symbol).containsExactly("GOLD-21K");
        assertThat(p.search("aapl")).isEmpty();
        var profile = p.profile("GOLD-21K").orElseThrow();
        assertThat(profile.currency()).isEqualTo("USD");
        assertThat(profile.countryCode()).isNull();
        assertThat(profile.assetType()).isEqualTo(AssetType.COMMODITY);
    }

    @Test
    void routingSendsEachSymbolToItsProviderAndKeepsNewsOnTheDefaultOne() {
        Clock clock = Clock.fixed(TRADING, ZoneOffset.UTC);
        var fake = new FakeMarketDataProvider(clock);
        var routing = new RoutingMarketDataProvider(fake,
                new GoldApiMarketDataProvider(RestClient.builder(), "https://gold.test", clock), freeSahmk(clock));

        assertThat(routing.nameFor("AAPL")).isEqualTo("fake");
        assertThat(routing.nameFor("2222.SR")).isEqualTo("sahmk");
        assertThat(routing.nameFor("GOLD-21K")).isEqualTo("gold-api");
        assertThat(routing.name()).isEqualTo("fake");
        assertThat(routing.credits()).containsExactly("Saudi market data provided by SAHMK");
        LocalDate day = LocalDate.parse("2026-11-08");
        assertThat(routing.news("GOLD-21K", day, day)).first()
                .satisfies(n -> assertThat(n.headline()).contains("SPDR Gold Shares"));
        assertThat(routing.news("2222.SR", day, day)).isNotEmpty();
    }

    @Test
    void routingLeavesEverythingOnTheDefaultProviderWhenNothingElseIsConfigured() {
        var fake = new FakeMarketDataProvider(Clock.fixed(TRADING, ZoneOffset.UTC));
        var routing = new RoutingMarketDataProvider(fake, null, null);

        assertThat(routing.nameFor("2222.SR")).isEqualTo("fake");
        assertThat(routing.quote("2222.SR")).isPresent();
        assertThat(routing.quote("GOLD-24K")).isPresent();
        assertThat(routing.credits()).isEmpty();
    }

    @Test
    void routingAsksEachProviderWhatIsDue() {
        Clock clock = Clock.fixed(WEEKEND, ZoneOffset.UTC);
        var routing = new RoutingMarketDataProvider(new FakeMarketDataProvider(clock), null, freeSahmk(clock));
        Map<String, Instant> symbols = new HashMap<>();
        symbols.put("AAPL", WEEKEND.minus(Duration.ofMinutes(5)));
        symbols.put("2222.SR", WEEKEND.minus(Duration.ofDays(1)));

        assertThat(routing.dueForRefresh(symbols)).containsExactly("AAPL");
    }
}
