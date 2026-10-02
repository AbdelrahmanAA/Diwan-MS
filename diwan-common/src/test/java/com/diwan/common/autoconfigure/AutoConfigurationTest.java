package com.diwan.common.autoconfigure;

import com.diwan.common.security.GatewaySigner;
import com.diwan.common.security.GatewayTrustFilter;
import com.diwan.common.security.JwtService;
import com.diwan.common.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import static org.assertj.core.api.Assertions.assertThat;

class AutoConfigurationTest {

    private static final String SECRET = "diwan.jwt.secret=0123456789012345678901234567890123456789";

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DiwanSecurityAutoConfiguration.class,
                    GatewayTrustAutoConfiguration.class, DiwanWebAutoConfiguration.class));

    @Test
    void nothingIsRegisteredWithoutASecret() {
        runner.run(ctx -> {
            assertThat(ctx).doesNotHaveBean(JwtService.class);
            assertThat(ctx).doesNotHaveBean(GatewaySigner.class);
            assertThat(ctx).doesNotHaveBean(FilterRegistrationBean.class);
        });
    }

    @Test
    void shortSecretFailsStartup() {
        runner.withPropertyValues("diwan.jwt.secret=too-short").run(ctx -> assertThat(ctx).hasFailed());
    }

    @Test
    void registersJwtSigningAndErrorHandling() {
        runner.withPropertyValues(SECRET).run(ctx -> {
            assertThat(ctx).hasSingleBean(JwtService.class);
            assertThat(ctx).hasSingleBean(GatewaySigner.class);
            assertThat(ctx).hasSingleBean(GlobalExceptionHandler.class);
            assertThat(ctx).doesNotHaveBean(FilterRegistrationBean.class); // no diwan.trust.url-patterns
        });
    }

    @Test
    void trustFilterIsRegisteredForConfiguredPatterns() {
        runner.withPropertyValues(SECRET,
                "diwan.trust.url-patterns=/api/x/*,/api/x",
                "diwan.trust.public-paths=/api/x/open",
                "diwan.trust.required-role=ADMIN").run(ctx -> {
            @SuppressWarnings("unchecked")
            FilterRegistrationBean<GatewayTrustFilter> reg = ctx.getBean(FilterRegistrationBean.class);
            assertThat(reg.getUrlPatterns()).containsExactlyInAnyOrder("/api/x/*", "/api/x");
            assertThat(reg.getFilter()).isInstanceOf(GatewayTrustFilter.class);
        });
    }

    @Test
    void errorHandlerCanBeDisabled() {
        runner.withPropertyValues(SECRET, "diwan.web.error-handler=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(GlobalExceptionHandler.class));
    }
}
