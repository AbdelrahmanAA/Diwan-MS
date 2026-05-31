package com.diwan.users.security;
import com.diwan.users.filter.GatewayValidationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final GatewayValidationFilter gatewayValidationFilter;

    public SecurityConfig(GatewayValidationFilter gatewayValidationFilter) {
        this.gatewayValidationFilter = gatewayValidationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(c -> c.ignoringRequestMatchers("/h2-console/**").disable())
            .headers(h -> h.frameOptions(f -> f.sameOrigin()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/h2-console/**").permitAll()
                .anyRequest().permitAll());
        return http.build();
    }

    @Bean
    public FilterRegistrationBean<GatewayValidationFilter> gatewayValidationFilterReg(){
        FilterRegistrationBean<GatewayValidationFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(gatewayValidationFilter);
        reg.addUrlPatterns("/api/users/*");
        reg.setOrder(1);
        return reg;
    }

    @Bean
    public PasswordEncoder passwordEncoder(){ return new BCryptPasswordEncoder(); }
}