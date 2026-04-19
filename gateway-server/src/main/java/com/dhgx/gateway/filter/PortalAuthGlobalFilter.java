package com.dhgx.gateway.filter;

import com.dhgx.common.api.ErrorCode;
import com.dhgx.common.feign.AuthFeignClient;
import com.dhgx.common.feign.PortalFeignClient;
import com.dhgx.common.gateway.GatewaySecurityProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PortalAuthGlobalFilter。
 * <p>
 * 全局鉴权过滤器：优先 Bearer，再回退 Cookie satoken。
 * </p>
 */
@Component
public class PortalAuthGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(PortalAuthGlobalFilter.class);

    private final GatewaySecurityProperties securityProperties;
    private final AuthFeignClient authFeignClient;
    private final PortalFeignClient portalFeignClient;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public PortalAuthGlobalFilter(GatewaySecurityProperties securityProperties,
                                  AuthFeignClient authFeignClient,
                                  PortalFeignClient portalFeignClient,
                                  ObjectMapper objectMapper) {
        this.securityProperties = securityProperties;
        this.authFeignClient = authFeignClient;
        this.portalFeignClient = portalFeignClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (isWhitelisted(path)) {
            return chain.filter(exchange);
        }

        String bearer = resolveBearer(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        if (StringUtils.isNotBlank(bearer)) {
            return Mono.fromCallable(() -> portalFeignClient.introspect(bearer))
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMap(response -> handleBearerResponse(chain, exchange, response))
                    .onErrorResume(ex -> {
                        log.warn("bearer introspect error: {}", ex.getMessage());
                        return writeUnauthorized(exchange, "登录已失效，请重新登录");
                    });
        }

        String cookie = exchange.getRequest().getHeaders().getFirst(HttpHeaders.COOKIE);
        String cookieHeader = cookie == null ? "" : cookie;
        if (!hasSaTokenCookie(cookieHeader)) {
            return writeUnauthorized(exchange, "登录凭证缺失，请先登录");
        }
        return Mono.fromCallable(() -> authFeignClient.sessionMe(cookieHeader))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(response -> handleCookieResponse(chain, exchange, response))
                .onErrorResume(ex -> writeUnauthorized(exchange, null));
    }

    private Mono<Void> handleBearerResponse(GatewayFilterChain chain,
                                            ServerWebExchange exchange,
                                            Response response) {
        if (response == null) {
            return writeUnauthorized(exchange, null);
        }
        Map<String, Object> payload = readJsonPayload(response);
        if (response.status() >= 200 && response.status() < 300 && isSuccessCode(payload)) {
            Map<String, Object> data = readData(payload);
            String userId = asText(data.get("userId"));
            String clientType = asText(data.get("clientType"));
            ServerHttpRequest request = exchange.getRequest().mutate()
                    .header("X-User-Id", userId == null ? "" : userId)
                    .header("X-Client-Type", clientType == null ? "" : clientType)
                    .header("X-Auth-Source", "ACCESS_TOKEN")
                    .build();
            return chain.filter(exchange.mutate().request(request).build());
        }
        return writeUnauthorized(exchange, resolveErrorMessage(payload));
    }

    private Mono<Void> handleCookieResponse(GatewayFilterChain chain,
                                            ServerWebExchange exchange,
                                            Response response) {
        if (response.status() >= 200 && response.status() < 300) {
            ServerHttpRequest request = exchange.getRequest().mutate()
                    .header("X-Auth-Source", "SATOKEN")
                    .build();
            return chain.filter(exchange.mutate().request(request).build());
        }
        return writeUnauthorized(exchange, resolveErrorMessage(readJsonPayload(response)));
    }

    private boolean isSuccessCode(Map<String, Object> payload) {
        Object code = payload == null ? null : payload.get("code");
        if (code instanceof Number) {
            return ((Number) code).intValue() == 0;
        }
        return "0".equals(asText(code));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readData(Map<String, Object> payload) {
        Object data = payload == null ? null : payload.get("data");
        if (data instanceof Map) {
            return (Map<String, Object>) data;
        }
        return new HashMap<>();
    }

    private String asText(Object value) {
        return value == null ? null : value.toString();
    }

    private String resolveBearer(String authorization) {
        if (StringUtils.isBlank(authorization)) {
            return null;
        }
        final String prefix = "Bearer ";
        if (authorization.startsWith(prefix)) {
            return authorization.substring(prefix.length()).trim();
        }
        return authorization.trim();
    }

    private boolean isWhitelisted(String path) {
        List<String> whitelist = securityProperties.getWhitelist();
        if (whitelist == null || whitelist.isEmpty()) {
            return false;
        }
        return whitelist.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private Mono<Void> writeUnauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] payload = buildUnauthorizedPayload(message);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(payload)));
    }

    private byte[] buildUnauthorizedPayload(String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", ErrorCode.UNAUTHENTICATED.getCode());
        String resolvedMessage = message == null || StringUtils.isBlank(message)
                ? ErrorCode.UNAUTHENTICATED.getMessage()
                : message;
        body.put("message", resolvedMessage);
        body.put("data", null);
        try {
            return objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException ex) {
            return ("{\"code\":" + ErrorCode.UNAUTHENTICATED.getCode()
                    + ",\"message\":\"" + resolvedMessage
                    + "\",\"data\":null}").getBytes(StandardCharsets.UTF_8);
        }
    }

    private String resolveErrorMessage(Map<String, Object> payload) {
        Object value = payload == null ? null : payload.get("message");
        return value == null ? null : value.toString();
    }

    private Map<String, Object> readJsonPayload(Response response) {
        if (response == null || response.body() == null) {
            return new HashMap<>();
        }
        try (InputStream inputStream = response.body().asInputStream()) {
            return objectMapper.readValue(inputStream, new TypeReference<Map<String, Object>>() {
            });
        } catch (IOException ex) {
            return new HashMap<>();
        }
    }

    private boolean hasSaTokenCookie(String cookieHeader) {
        if (cookieHeader == null || StringUtils.isBlank(cookieHeader)) {
            return false;
        }
        String[] parts = cookieHeader.split(";");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.startsWith("satoken=")) {
                String value = trimmed.substring("satoken=".length()).trim();
                return !value.isEmpty();
            }
        }
        return false;
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
