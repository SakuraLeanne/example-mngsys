package com.example.mngsys.portal.client;

import com.example.mngsys.common.feign.AuthFeignClient;
import com.example.mngsys.common.feign.dto.AuthKickRequest;
import com.example.mngsys.common.feign.dto.AuthLoginRequest;
import com.example.mngsys.common.feign.dto.AuthLoginResponse;
import com.example.mngsys.common.feign.dto.AuthPasswordResetRequest;
import com.example.mngsys.common.feign.dto.AuthResetTokenResponse;
import com.example.mngsys.common.feign.dto.AuthSessionResponse;
import com.example.mngsys.common.feign.dto.AuthSmsSendRequest;
import com.example.mngsys.common.feign.dto.AuthSmsVerifyRequest;
import com.example.mngsys.portal.common.api.ApiResponse;
import com.example.mngsys.portal.common.api.ErrorCode;
import com.example.mngsys.portal.config.AuthClientProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Response;
import feign.Util;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

@Component
/**
 * AuthClient。
 * <p>
 * 使用 common-utils 中的 Feign 接口，集中处理响应解析与错误转换，
 * 避免在多个模块重复维护调用逻辑。
 * </p>
 */
public class AuthClient {

    private static final String GENERIC_AUTH_FAILURE_MESSAGE = "调用鉴权服务失败";

    private final AuthFeignClient authFeignClient;
    private final AuthClientProperties authClientProperties;
    private final ObjectMapper objectMapper;

    public AuthClient(AuthFeignClient authFeignClient, AuthClientProperties authClientProperties, ObjectMapper objectMapper) {
        this.authFeignClient = authFeignClient;
        this.authClientProperties = authClientProperties;
        this.objectMapper = objectMapper;
    }

    public ApiResponse<AuthLoginResponse> login(AuthLoginRequest request) {
        return parseResponseBody(authFeignClient.login(request), new TypeReference<ApiResponse<AuthLoginResponse>>() {
        });
    }

    public ResponseEntity<ApiResponse<AuthLoginResponse>> loginWithResponse(AuthLoginRequest request) {
        return exchangeSafely(() -> authFeignClient.login(request),
                new TypeReference<ApiResponse<AuthLoginResponse>>() {
                });
    }

    public ApiResponse<Void> logout() {
        return parseResponseBody(authFeignClient.logout(null), new TypeReference<ApiResponse<Void>>() {
        });
    }

    public ApiResponse<String> sendLoginSms(String mobile) {
        return parseResponseBody(authFeignClient.sendSms(AuthSmsSendRequest.loginScene(mobile)),
                new TypeReference<ApiResponse<String>>() {
                });
    }

    public ApiResponse<Void> verifySms(String mobile, String code) {
        return parseResponseBody(authFeignClient.verifySms(new AuthSmsVerifyRequest(mobile, code)),
                new TypeReference<ApiResponse<Void>>() {
                });
    }

    public ApiResponse<String> sendForgotPasswordSms(String mobile) {
        return parseResponseBody(authFeignClient.sendForgotPassword(AuthSmsSendRequest.verificationScene(mobile)),
                new TypeReference<ApiResponse<String>>() {
                });
    }

    public ApiResponse<ResetTokenResponse> verifyForgotPassword(String mobile, String code) {
        return parseResponseBody(authFeignClient.verifyForgotPassword(new AuthSmsVerifyRequest(mobile, code)),
                new TypeReference<ApiResponse<ResetTokenResponse>>() {
                });
    }

    public ApiResponse<Void> resetForgotPassword(String mobile, String resetToken, String encryptedPassword, String newPassword) {
        AuthPasswordResetRequest request = new AuthPasswordResetRequest(mobile, resetToken, encryptedPassword, newPassword);
        return parseResponseBody(authFeignClient.resetForgotPassword(request),
                new TypeReference<ApiResponse<Void>>() {
                });
    }

    public ResponseEntity<ApiResponse<Void>> logoutWithResponse(String cookie) {
        return exchangeSafely(() -> authFeignClient.logout(cookie), new TypeReference<ApiResponse<Void>>() {
        });
    }

    public ApiResponse<AuthSessionResponse> sessionMe() {
        return parseResponseBody(authFeignClient.sessionMe(null), new TypeReference<ApiResponse<AuthSessionResponse>>() {
        });
    }

    public ResponseEntity<ApiResponse<AuthSessionResponse>> sessionMe(String cookie) {
        return exchangeSafely(() -> authFeignClient.sessionMe(cookie),
                new TypeReference<ApiResponse<AuthSessionResponse>>() {
                });
    }

    public ApiResponse<Void> kick(String userId) {
        AuthKickRequest request = new AuthKickRequest(userId);
        return parseResponseBody(authFeignClient.kick(authClientProperties.getInternalToken(), request),
                new TypeReference<ApiResponse<Void>>() {
                });
    }

    private <T> ResponseEntity<ApiResponse<T>> exchangeSafely(Supplier<Response> supplier,
                                                              TypeReference<ApiResponse<T>> typeReference) {
        try {
            Response response = supplier.get();
            ApiResponse<T> body = parseResponseBody(response, typeReference);
            HttpHeaders headers = toHttpHeaders(response.headers());
            HttpStatus status = HttpStatus.resolve(response.status());
            if (status == null) {
                status = HttpStatus.INTERNAL_SERVER_ERROR;
            }
            return ResponseEntity.status(status)
                    .headers(headers)
                    .body(body);
        } catch (FeignException ex) {
            ApiResponse<T> response = parseErrorResponse(ex.contentUTF8());
            return ResponseEntity.status(ex.status()).body(response);
        }
    }

    private <T> ApiResponse<T> parseResponseBody(Response response, TypeReference<ApiResponse<T>> typeReference) {
        if (response == null) {
            return ApiResponse.failure(ErrorCode.INTERNAL_ERROR, GENERIC_AUTH_FAILURE_MESSAGE);
        }
        String body = readBody(response);
        if (!StringUtils.hasText(body)) {
            return ApiResponse.failure(ErrorCode.INTERNAL_ERROR, GENERIC_AUTH_FAILURE_MESSAGE);
        }
        try {
            return objectMapper.readValue(body, typeReference);
        } catch (IOException ex) {
            return ApiResponse.failure(ErrorCode.INTERNAL_ERROR, GENERIC_AUTH_FAILURE_MESSAGE);
        }
    }

    private <T> ApiResponse<T> parseErrorResponse(String body) {
        if (!StringUtils.hasText(body)) {
            return ApiResponse.failure(ErrorCode.INTERNAL_ERROR, GENERIC_AUTH_FAILURE_MESSAGE);
        }
        try {
            return objectMapper.readValue(body, new TypeReference<ApiResponse<T>>() {
            });
        } catch (JsonProcessingException ex) {
            return ApiResponse.failure(ErrorCode.INTERNAL_ERROR, GENERIC_AUTH_FAILURE_MESSAGE);
        }
    }

    private String readBody(Response response) {
        if (response == null || response.body() == null) {
            return null;
        }
        try (Reader reader = response.body().asReader(StandardCharsets.UTF_8)) {
            return Util.toString(reader);
        } catch (IOException ex) {
            return null;
        }
    }

    private HttpHeaders toHttpHeaders(Map<String, Collection<String>> feignHeaders) {
        HttpHeaders httpHeaders = new HttpHeaders();
        if (feignHeaders == null || feignHeaders.isEmpty()) {
            return httpHeaders;
        }
        feignHeaders.forEach((key, values) -> {
            if (values != null) {
                httpHeaders.put(key, new ArrayList<>(values));
            }
        });
        return httpHeaders;
    }

    /**
     * 保持向后兼容的重置令牌返回对象，继承自共享 DTO。
     */
    public static class ResetTokenResponse extends AuthResetTokenResponse {
    }
}
