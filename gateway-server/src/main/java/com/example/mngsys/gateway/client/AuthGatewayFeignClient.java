package com.example.mngsys.gateway.client;

import feign.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * AuthGatewayFeignClient。
 * <p>
 * 调用认证服务的 Feign 客户端，负责校验会话。
 * </p>
 */
@FeignClient(name = "auth-server", path = "${auth.feign.path:${server.servlet.context-path:}/auth/api}")
public interface AuthGatewayFeignClient {

    /**
     * 查询当前登录会话信息。
     *
     * @param cookie 传递的 Cookie 头
     * @return 认证服务的原始响应
     */
    @GetMapping("/session/me")
    Response sessionMe(@RequestHeader(value = "Cookie", required = false) String cookie);
}
