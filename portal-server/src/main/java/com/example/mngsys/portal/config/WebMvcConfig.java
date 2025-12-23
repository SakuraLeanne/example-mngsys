package com.example.mngsys.portal.config;

import com.example.mngsys.portal.security.AdminRequiredInterceptor;
import com.example.mngsys.portal.security.AuthSessionInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthSessionInterceptor authSessionInterceptor;
    private final AdminRequiredInterceptor adminRequiredInterceptor;

    public WebMvcConfig(AuthSessionInterceptor authSessionInterceptor, AdminRequiredInterceptor adminRequiredInterceptor) {
        this.authSessionInterceptor = authSessionInterceptor;
        this.adminRequiredInterceptor = adminRequiredInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authSessionInterceptor)
                .addPathPatterns("/portal/api/**");
        registry.addInterceptor(adminRequiredInterceptor)
                .addPathPatterns("/portal/api/**");
    }
}
