package com.diwan.common.autoconfigure;

import com.diwan.common.web.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Shared error format; a service with its own contract sets {@code diwan.web.error-handler=false}. */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(ResponseEntityExceptionHandler.class)
@ConditionalOnProperty(name = "diwan.web.error-handler", havingValue = "true", matchIfMissing = true)
public class DiwanWebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler diwanGlobalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
