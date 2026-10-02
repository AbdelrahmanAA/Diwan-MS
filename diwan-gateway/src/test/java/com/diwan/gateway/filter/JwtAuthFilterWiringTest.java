package com.diwan.gateway.filter;

import com.diwan.common.autoconfigure.DiwanSecurityAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/** The filter gets its JwtService/GatewaySigner from the diwan-common auto-configuration. */
class JwtAuthFilterWiringTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DiwanSecurityAutoConfiguration.class))
            .withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class))
            .withBean(JwtAuthFilter.class);

    @Test
    void filterIsWiredWithSharedBeans() {
        runner.withPropertyValues("diwan.jwt.secret=0123456789012345678901234567890123456789")
                .run(ctx -> assertThat(ctx).hasSingleBean(JwtAuthFilter.class));
    }

    @Test
    void contextFailsWithoutSecret() {
        runner.run(ctx -> assertThat(ctx).hasFailed());
    }
}
