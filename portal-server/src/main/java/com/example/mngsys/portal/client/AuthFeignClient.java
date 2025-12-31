package com.example.mngsys.portal.client;

import com.example.mngsys.portal.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "auth-server", path = "${auth.feign.path:${server.servlet.context-path:}/auth/api}")
/**
 * AuthFeignClient。
 */
public interface AuthFeignClient {

    @PostMapping("/login")
    ApiResponse<AuthClient.LoginResponse> login(@RequestBody AuthClient.LoginRequest request);

    @PostMapping("/sms/send")
    ApiResponse<Void> sendSms(@RequestBody AuthClient.SmsSendRequest request);

    @PostMapping("/logout")
    ApiResponse<Void> logout(@RequestHeader(value = "Cookie", required = false) String cookie);

    @GetMapping("/session/me")
    ApiResponse<AuthClient.SessionResponse> sessionMe(@RequestHeader(value = "Cookie", required = false) String cookie);

    @PostMapping("/session/kick")
    ApiResponse<Void> kick(@RequestHeader("X-Internal-Token") String internalToken,
                           @RequestBody AuthClient.KickRequest request);
}
