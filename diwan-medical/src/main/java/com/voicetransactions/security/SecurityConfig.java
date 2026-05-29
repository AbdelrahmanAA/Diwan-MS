package com.voicetransactions.security;
import com.voicetransactions.filter.UserValidationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final UserValidationFilter userValidationFilter;
    public SecurityConfig(UserValidationFilter userValidationFilter){
        this.userValidationFilter = userValidationFilter;
    }
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(c -> c.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a.anyRequest().permitAll());
        return http.build();
    }
    @Bean
    public FilterRegistrationBean<UserValidationFilter> userValidationFilterReg(){
        FilterRegistrationBean<UserValidationFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(userValidationFilter);
        reg.addUrlPatterns("/api/medical/*", "/api/medical");
        reg.setOrder(1);
        return reg;
    }
}