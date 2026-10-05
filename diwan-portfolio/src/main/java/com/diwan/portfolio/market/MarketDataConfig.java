package com.diwan.portfolio.market;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;

/** Picks the market data and FX providers from configuration (see application.properties). */
@Configuration
public class MarketDataConfig {

    private static final Logger log = LoggerFactory.getLogger(MarketDataConfig.class);

    private static RestClient.Builder withTimeouts(RestClient.Builder builder) {
        return builder.clone().requestFactory(ClientHttpRequestFactories.get(ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(5)).withReadTimeout(Duration.ofSeconds(15))));
    }

    @Bean
    public MarketDataProvider marketDataProvider(
            @Value("${diwan.portfolio.market-data.provider:auto}") String provider,
            @Value("${diwan.portfolio.market-data.finnhub.api-key:}") String apiKey,
            @Value("${diwan.portfolio.market-data.finnhub.base-url:https://finnhub.io/api/v1}") String baseUrl,
            @Value("${diwan.portfolio.market-data.finnhub.calls-per-minute:50}") int callsPerMinute,
            RestClient.Builder builder, Clock clock) {
        boolean finnhub = switch (provider) {
            case "finnhub" -> {
                if (apiKey.isBlank()) {
                    throw new IllegalStateException("MARKET_DATA_PROVIDER=finnhub needs FINNHUB_API_KEY");
                }
                yield true;
            }
            case "fake" -> false;
            case "auto" -> !apiKey.isBlank();
            default -> throw new IllegalStateException("Unknown market data provider '" + provider + "'");
        };
        if (finnhub) {
            log.info("[MarketData] using Finnhub ({} calls/minute)", callsPerMinute);
            return new FinnhubMarketDataProvider(withTimeouts(builder), baseUrl, apiKey.trim(), callsPerMinute);
        }
        log.warn("[MarketData] using the FAKE provider (demo prices and news); set FINNHUB_API_KEY for real data");
        return new FakeMarketDataProvider(clock);
    }

    @Bean
    public FxProvider fxProvider(@Value("${diwan.portfolio.fx.provider:open-er-api}") String provider,
                                 @Value("${diwan.portfolio.fx.base-url:https://open.er-api.com/v6}") String baseUrl,
                                 RestClient.Builder builder, Clock clock) {
        return switch (provider) {
            case "open-er-api" -> new OpenErApiFxProvider(withTimeouts(builder), baseUrl);
            case "fake" -> new FakeFxProvider(clock);
            default -> throw new IllegalStateException("Unknown FX provider '" + provider + "'");
        };
    }
}
