package com.example.mngsys.portal.client;

import com.example.mngsys.portal.common.api.ApiResponse;
import com.example.mngsys.portal.config.AuthClientProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

@Component
/**
 * AuthClient。
 */
public class AuthClient {

    private final RestTemplate restTemplate;
    private final AuthClientProperties authClientProperties;
    private final ObjectMapper objectMapper;

    public AuthClient(RestTemplate restTemplate, AuthClientProperties authClientProperties, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.authClientProperties = authClientProperties;
        this.objectMapper = objectMapper;
    }

    public ApiResponse<LoginResponse> login(String username, String password) {
        LoginRequest request = new LoginRequest(username, password);
        return restTemplate.postForEntity(buildUrl("/auth/api/login"), request, ApiResponse.class).getBody();
    }

    public ResponseEntity<ApiResponse> loginWithResponse(String username, String password) {
        LoginRequest request = new LoginRequest(username, password);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LoginRequest> entity = new HttpEntity<>(request, headers);
        return exchangeSafely(buildUrl("/auth/api/login"), HttpMethod.POST, entity);
    }

    public ApiResponse<Void> logout() {
        return restTemplate.postForEntity(buildUrl("/auth/api/logout"), null, ApiResponse.class).getBody();
    }

    public ResponseEntity<ApiResponse> logoutWithResponse(String cookie) {
        HttpHeaders headers = new HttpHeaders();
        if (StringUtils.hasText(cookie)) {
            headers.add(HttpHeaders.COOKIE, cookie);
        }
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        return exchangeSafely(buildUrl("/auth/api/logout"), HttpMethod.POST, entity);
    }

    public ApiResponse<SessionResponse> sessionMe() {
        return restTemplate.getForEntity(buildUrl("/auth/api/session/me"), ApiResponse.class).getBody();
    }

    public ResponseEntity<ApiResponse> sessionMe(String cookie) {
        HttpHeaders headers = new HttpHeaders();
        if (StringUtils.hasText(cookie)) {
            headers.add(HttpHeaders.COOKIE, cookie);
        }
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        return exchangeSafely(buildUrl("/auth/api/session/me"), HttpMethod.GET, entity);
    }

    private ResponseEntity<ApiResponse> exchangeSafely(String url, HttpMethod method, HttpEntity<?> entity) {
        try {
            return restTemplate.exchange(url, method, entity, ApiResponse.class);
        } catch (HttpStatusCodeException ex) {
            ApiResponse response = parseErrorResponse(ex.getResponseBodyAsString());
            return ResponseEntity.status(ex.getStatusCode()).body(response);
        }
    }

    private ApiResponse parseErrorResponse(String body) {
        if (!StringUtils.hasText(body)) {
            return ApiResponse.failure(com.example.mngsys.portal.common.api.ErrorCode.INTERNAL_ERROR, "调用鉴权服务失败");
        }
        try {
            return objectMapper.readValue(body, ApiResponse.class);
        } catch (JsonProcessingException ex) {
            return ApiResponse.failure(com.example.mngsys.portal.common.api.ErrorCode.INTERNAL_ERROR, "调用鉴权服务失败");
        }
    }

    public ApiResponse<Void> kick(String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Token", authClientProperties.getInternalToken());
        KickRequest request = new KickRequest(userId);
        HttpEntity<KickRequest> entity = new HttpEntity<>(request, headers);
        ResponseEntity<ApiResponse> response = restTemplate.exchange(
                buildUrl("/auth/api/session/kick"),
                HttpMethod.POST,
                entity,
                ApiResponse.class);
        return response.getBody();
    }

    private String buildUrl(String path) {
        String baseUrl = authClientProperties.getServerBaseUrl();
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + path;
        }
        return baseUrl + path;
    }

    public static class LoginRequest {
        private String username;
        private String password;

        public LoginRequest() {
        }

        public LoginRequest(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class LoginResponse {
        private String userId;
        private String username;

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }

    public static class SessionResponse {
        private String userId;

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }
    }

    public static class KickRequest {
        private String userId;

        public KickRequest() {
        }

        public KickRequest(String userId) {
            this.userId = userId;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }
    }
}
