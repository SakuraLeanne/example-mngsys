package com.example.mngsys.portal.client;

import com.example.mngsys.portal.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "auth-server", url = "${auth.serverBaseUrl}")
/**
 * AuthFeignClient。
 */
public interface AuthFeignClient {

    @PostMapping("/auth/api/login")
    ApiResponse<AuthClient.LoginResponse> login(@RequestBody AuthClient.LoginRequest request);

    @PostMapping("/auth/api/logout")
    ApiResponse<Void> logout(@RequestHeader(value = "Cookie", required = false) String cookie);

    @GetMapping("/auth/api/session/me")
    ApiResponse<AuthClient.SessionResponse> sessionMe(@RequestHeader(value = "Cookie", required = false) String cookie);

    @PostMapping("/auth/api/session/kick")
    ApiResponse<Void> kick(@RequestHeader("X-Internal-Token") String internalToken,
                           @RequestBody AuthClient.KickRequest request);
}
