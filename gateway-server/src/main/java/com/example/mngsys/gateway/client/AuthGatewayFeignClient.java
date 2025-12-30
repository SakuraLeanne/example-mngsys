package com.example.mngsys.gateway.client;

import feign.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "auth-server")
/**
 * AuthGatewayFeignClient。
 */
public interface AuthGatewayFeignClient {

    @GetMapping("/auth/api/session/me")
    Response sessionMe(@RequestHeader(value = "Cookie", required = false) String cookie);
}
