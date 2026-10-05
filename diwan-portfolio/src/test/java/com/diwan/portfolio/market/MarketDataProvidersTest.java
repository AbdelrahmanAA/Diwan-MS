package com.diwan.portfolio.market;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MarketDataProvidersTest {

    @Test
    void fakeProviderIsDeterministicWithinFiveMinutes() {
        Clock t1 = Clock.fixed(Instant.parse("2026-03-02T10:01:00Z"), ZoneOffset.UTC);
        Clock t2 = Clock.fixed(Instant.parse("2026-03-02T10:04:00Z"), ZoneOffset.UTC);
        var a = new FakeMarketDataProvider(t1).quote("AAPL").orElseThrow();
        var b = new FakeMarketDataProvider(t2).quote("AAPL").orElseThrow();

        assertThat(a.price()).isEqualByComparingTo(b.price());
        assertThat(a.price().doubleValue()).isBetween(190 * 0.9, 190 * 1.1);
        assertThat(new FakeMarketDataProvider(t1).quote("NOPE")).isEmpty();
        assertThat(new FakeMarketDataProvider(t1).profile("COMI.CA").orElseThrow().currency()).isEqualTo("EGP");
    }

    private static RestClient.Builder builder() {
        return RestClient.builder();
    }

    @Test
    void finnhubQuoteSendsTheKeyInAHeaderAndMapsTheFields() {
        RestClient.Builder b = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(b).build();
        server.expect(requestTo("https://finnhub.test/api/v1/quote?symbol=AAPL"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Finnhub-Token", "secret-key"))
                .andRespond(withSuccess("""
                        {"c":191.5,"d":1.5,"dp":0.7895,"h":192,"l":189,"o":190,"pc":190,"t":1767000000}""",
                        MediaType.APPLICATION_JSON));

        var q = new FinnhubMarketDataProvider(b, "https://finnhub.test/api/v1", "secret-key", 6000)
                .quote("AAPL").orElseThrow();

        assertThat(q.price()).isEqualByComparingTo("191.5");
        assertThat(q.previousClose()).isEqualByComparingTo("190");
        assertThat(q.changePercent()).isEqualByComparingTo("0.7895");
        assertThat(q.time()).isEqualTo(Instant.ofEpochSecond(1767000000));
        server.verify();
    }

    @Test
    void finnhubUnknownSymbolAndErrorsAreEmptyNotExceptions() {
        RestClient.Builder b = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(b).build();
        server.expect(requestTo("https://finnhub.test/api/v1/quote?symbol=ZZZZ"))
                .andRespond(withSuccess("{\"c\":0,\"d\":null,\"dp\":null,\"h\":0,\"l\":0,\"o\":0,\"pc\":0,\"t\":0}",
                        MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://finnhub.test/api/v1/quote?symbol=AAPL"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        var p = new FinnhubMarketDataProvider(b, "https://finnhub.test/api/v1", "k", 6000);
        assertThat(p.quote("ZZZZ")).isEmpty();
        assertThat(p.quote("AAPL")).isEmpty();
        server.verify();
    }

    @Test
    void finnhubNewsSkipsItemsWithoutUrlOrHeadline() {
        RestClient.Builder b = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(b).build();
        server.expect(requestTo("https://finnhub.test/api/v1/company-news?symbol=AAPL&from=2026-03-01&to=2026-03-03"))
                .andRespond(withSuccess("""
                        [{"datetime":1767000000,"headline":"Apple news","url":"https://n.example/1","source":"Reuters","summary":"s","image":""},
                         {"datetime":1767000000,"headline":"","url":"https://n.example/2"}]""", MediaType.APPLICATION_JSON));

        var items = new FinnhubMarketDataProvider(b, "https://finnhub.test/api/v1", "k", 6000)
                .news("AAPL", LocalDate.parse("2026-03-01"), LocalDate.parse("2026-03-03"));

        assertThat(items).hasSize(1);
        assertThat(items.get(0).source()).isEqualTo("Reuters");
        assertThat(items.get(0).imageUrl()).isNull();
    }

    @Test
    void openErApiRatesAreParsed() {
        RestClient.Builder b = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(b).build();
        server.expect(requestTo("https://fx.test/v6/latest/USD")).andRespond(withSuccess("""
                {"result":"success","time_last_update_unix":1767225600,"rates":{"USD":1,"EGP":48.7,"SAR":3.75}}""",
                MediaType.APPLICATION_JSON));

        var rates = new OpenErApiFxProvider(b, "https://fx.test/v6").latest().orElseThrow();

        assertThat(rates.perUsd()).containsKeys("USD", "EGP", "SAR");
        assertThat(rates.perUsd().get("EGP")).isEqualByComparingTo("48.7");
        assertThat(rates.date()).isEqualTo(LocalDate.parse("2026-01-01"));
    }
}
