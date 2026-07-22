package com.premtsd.linkedin.platform.persistence;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registers {@link ReadYourWritesInterceptor} on the MVC chain (active under 'feeddb'). */
@Configuration
@Profile("feeddb")
class ReadYourWritesWebConfig implements WebMvcConfigurer {

    private final ReadYourWritesInterceptor interceptor;

    ReadYourWritesWebConfig(ReadYourWritesInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor);
    }
}
