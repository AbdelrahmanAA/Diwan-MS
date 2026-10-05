package com.diwan.portfolio.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TimeConfig {

    /** Injected everywhere "now" or "today" is needed, so tests can fix the date. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
