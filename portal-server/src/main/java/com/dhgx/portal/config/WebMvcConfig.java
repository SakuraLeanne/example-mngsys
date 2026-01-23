package com.dhgx.portal.config;

import com.dhgx.portal.security.AdminRequiredInterceptor;
import com.dhgx.portal.security.AuthSessionInterceptor;
import com.dhgx.portal.security.PtkInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
/**
 * WebMvcConfig。
 */
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthSessionInterceptor authSessionInterceptor;
    private final PtkInterceptor ptkInterceptor;
    private final AdminRequiredInterceptor adminRequiredInterceptor;

    public WebMvcConfig(AuthSessionInterceptor authSessionInterceptor,
                        PtkInterceptor ptkInterceptor,
                        AdminRequiredInterceptor adminRequiredInterceptor) {
        this.authSessionInterceptor = authSessionInterceptor;
        this.ptkInterceptor = ptkInterceptor;
        this.adminRequiredInterceptor = adminRequiredInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 登录态拦截：通过 Cookie 向鉴权服务校验会话并写入 RequestContext 的 userId
        registry.addInterceptor(authSessionInterceptor)
                .addPathPatterns("/app/menus")
                .addPathPatterns("/password/change")
                .addPathPatterns("/profile")
                .addPathPatterns("/login/**")
                .addPathPatterns("/password/forgot/**")
                .addPathPatterns("/loginuser/session-info")
                .addPathPatterns("/portal/api/**")
                .addPathPatterns("/admin/**");
        // PTK 拦截：校验一次性令牌（如改密/编辑资料等场景）并写入 RequestContext
        registry.addInterceptor(ptkInterceptor)
                .addPathPatterns("/password/change")
                .addPathPatterns("/profile");
        // 管理员权限拦截：检查当前用户是否具备管理端权限
        registry.addInterceptor(adminRequiredInterceptor)
                .addPathPatterns("/portal/api/**")
                .addPathPatterns("/admin/**");
    }
}
