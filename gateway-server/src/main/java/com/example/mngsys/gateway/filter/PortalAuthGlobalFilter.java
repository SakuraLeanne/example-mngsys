package com.example.mngsys.gateway.filter;

import com.example.mngsys.gateway.config.GatewaySecurityProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PortalAuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String PORTAL_API_PREFIX = "/portal/api/";
    private static final String AUTH_SESSION_PATH = "/auth/api/session/me";

    private final GatewaySecurityProperties securityProperties;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public PortalAuthGlobalFilter(GatewaySecurityProperties securityProperties,
                                  WebClient.Builder webClientBuilder,
                                  ObjectMapper objectMapper) {
        this.securityProperties = securityProperties;
        this.webClientBuilder = webClientBuilder;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (!path.startsWith(PORTAL_API_PREFIX)) {
            return chain.filter(exchange);
        }
        if (isWhitelisted(path)) {
            return chain.filter(exchange);
        }
        String cookie = exchange.getRequest().getHeaders().getFirst(HttpHeaders.COOKIE);
        return webClientBuilder.build()
                .get()
                .uri(securityProperties.getAuthServerBaseUrl() + AUTH_SESSION_PATH)
                .header(HttpHeaders.COOKIE, cookie == null ? "" : cookie)
                .exchange()
                .flatMap(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return chain.filter(exchange);
                    }
                    return writeUnauthorized(exchange);
                })
                .onErrorResume(ex -> writeUnauthorized(exchange));
    }

    private boolean isWhitelisted(String path) {
        List<String> whitelist = securityProperties.getWhitelist();
        if (whitelist == null || whitelist.isEmpty()) {
            return false;
        }
        return whitelist.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private Mono<Void> writeUnauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] payload = buildUnauthorizedPayload();
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                .bufferFactory()
                .wrap(payload)));
    }

    private byte[] buildUnauthorizedPayload() {
        Map<String, Object> body = new HashMap<>();
        body.put("code", 100100);
        body.put("message", "未登录");
        body.put("data", null);
        try {
            return objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException ex) {
            return "{\"code\":100100,\"message\":\"未登录\",\"data\":null}".getBytes(StandardCharsets.UTF_8);
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
