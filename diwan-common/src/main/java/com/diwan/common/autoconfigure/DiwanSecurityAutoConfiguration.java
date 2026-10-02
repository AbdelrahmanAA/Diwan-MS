package com.diwan.common.autoconfigure;

import com.diwan.common.security.GatewaySigner;
import com.diwan.common.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/** JWT + gateway-signing beans, active in every service that defines {@code diwan.jwt.secret}. */
@AutoConfiguration
@ConditionalOnProperty("diwan.jwt.secret")
public class DiwanSecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JwtService jwtService(@Value("${diwan.jwt.secret}") String secret,
                                 @Value("${diwan.jwt.expiration:604800000}") long expirationMillis) {
        GatewaySigner.fromSecret(secret); // same length rule everywhere: fail fast on short secrets
        return new JwtService(secret, expirationMillis);
    }

    @Bean
    @ConditionalOnMissingBean
    public GatewaySigner gatewaySigner(@Value("${diwan.jwt.secret}") String secret) {
        return GatewaySigner.fromSecret(secret);
    }
}
