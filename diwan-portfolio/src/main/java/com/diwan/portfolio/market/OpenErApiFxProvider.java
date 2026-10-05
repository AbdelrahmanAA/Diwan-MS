package com.diwan.portfolio.market;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

/**
 * Free daily exchange rates from <a href="https://www.exchangerate-api.com/docs/free">open.er-api.com</a>
 * (no key, ~160 currencies including EGP, SAR and AED; their terms ask for attribution in the app).
 */
public class OpenErApiFxProvider implements FxProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenErApiFxProvider.class);

    private final RestClient http;

    public OpenErApiFxProvider(RestClient.Builder builder, String baseUrl) {
        this.http = builder.baseUrl(baseUrl).build();
    }

    @Override
    public String name() {
        return "open-er-api";
    }

    @Override
    public Optional<Rates> latest() {
        try {
            JsonNode n = http.get().uri("/latest/USD").retrieve().body(JsonNode.class);
            if (n == null || !"success".equals(n.path("result").asText())) {
                log.warn("[FX] open.er-api returned no rates: {}", n == null ? null : n.path("error-type").asText());
                return Optional.empty();
            }
            Map<String, BigDecimal> rates = new HashMap<>();
            for (Iterator<Map.Entry<String, JsonNode>> it = n.path("rates").fields(); it.hasNext(); ) {
                Map.Entry<String, JsonNode> e = it.next();
                if (e.getKey().length() == 3 && e.getValue().isNumber() && e.getValue().decimalValue().signum() > 0) {
                    rates.put(e.getKey(), e.getValue().decimalValue());
                }
            }
            LocalDate date = LocalDate.ofInstant(
                    Instant.ofEpochSecond(n.path("time_last_update_unix").asLong()), ZoneOffset.UTC);
            return Optional.of(new Rates(date, rates));
        } catch (RestClientException e) {
            log.warn("[FX] open.er-api failed: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
