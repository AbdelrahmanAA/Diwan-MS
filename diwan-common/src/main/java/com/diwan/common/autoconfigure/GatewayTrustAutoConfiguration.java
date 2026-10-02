package com.diwan.common.autoconfigure;

import com.diwan.common.security.GatewaySigner;
import com.diwan.common.security.GatewayTrustFilter;
import jakarta.servlet.Filter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

import java.util.Arrays;
import java.util.List;

/**
 * Protects a servlet service with {@link GatewayTrustFilter}. Configure per service:
 * <pre>
 * diwan.trust.url-patterns=/api/transactions/*,/api/transactions   (required to enable)
 * diwan.trust.public-paths=/api/users/login                         (optional)
 * diwan.trust.required-role=ADMIN                                   (optional)
 * </pre>
 */
@AutoConfiguration(after = DiwanSecurityAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(Filter.class)
@ConditionalOnProperty("diwan.trust.url-patterns")
public class GatewayTrustAutoConfiguration {

    @Bean
    public FilterRegistrationBean<GatewayTrustFilter> gatewayTrustFilterRegistration(
            GatewaySigner signer,
            @Value("${diwan.trust.url-patterns}") String urlPatterns,
            @Value("${diwan.trust.public-paths:}") String publicPaths,
            @Value("${diwan.trust.required-role:}") String requiredRole) {
        FilterRegistrationBean<GatewayTrustFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new GatewayTrustFilter(signer, split(publicPaths),
                requiredRole == null || requiredRole.isBlank() ? null : requiredRole.trim()));
        reg.setUrlPatterns(split(urlPatterns));
        reg.setOrder(1);
        return reg;
    }

    private static List<String> split(String commaSeparated) {
        return Arrays.stream(commaSeparated.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
