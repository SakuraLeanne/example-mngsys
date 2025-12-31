package com.example.mngsys.gateway.filter;

import com.example.mngsys.gateway.client.AuthGatewayFeignClient;
import com.example.mngsys.gateway.config.GatewaySecurityProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PortalAuthGlobalFilter。
 * <p>
 * 全局鉴权过滤器，拦截 Portal API 请求，调用认证服务校验登录状态，
 * 未通过校验时返回 401 JSON 响应。
 * </p>
 */
@Component
public class PortalAuthGlobalFilter implements GlobalFilter, Ordered {

    /** Portal API 前缀，只有匹配路径才会做鉴权。 */
    private static final String PORTAL_API_PREFIX = "/portal/api/";
    /** 会话校验接口路径，用于透传调用认证服务。 */
    private static final String AUTH_SESSION_PATH = "/auth/api/session/me";

    /** 安全配置，包含白名单配置。 */
    private final GatewaySecurityProperties securityProperties;
    /** 调用认证服务的 Feign 客户端。 */
    private final AuthGatewayFeignClient authGatewayFeignClient;
    /** JSON 序列化工具。 */
    private final ObjectMapper objectMapper;
    /** 路径匹配器，用于匹配白名单。 */
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 构造函数，注入依赖。
     *
     * @param securityProperties 安全配置项
     * @param authGatewayFeignClient 认证服务 Feign 客户端
     * @param objectMapper JSON 序列化工具
     */
    public PortalAuthGlobalFilter(GatewaySecurityProperties securityProperties,
                                  AuthGatewayFeignClient authGatewayFeignClient,
                                  ObjectMapper objectMapper) {
        this.securityProperties = securityProperties;
        this.authGatewayFeignClient = authGatewayFeignClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 过滤入口，对 Portal API 请求执行登录校验。
     *
     * @param exchange 当前请求上下文
     * @param chain    过滤器链
     * @return 继续链路或返回未登录响应的 Mono
     */
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
        String cookieHeader = cookie == null ? "" : cookie;
        return Mono.fromCallable(() -> authGatewayFeignClient.sessionMe(cookieHeader))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(response -> handleResponse(chain, exchange, response))
                .onErrorResume(ex -> writeUnauthorized(exchange));
    }

    /**
     * 处理认证服务返回结果，成功则放行，失败则返回 401。
     *
     * @param chain    过滤器链
     * @param exchange 请求上下文
     * @param response 认证服务响应
     * @return 下一步处理的 Mono
     */
    private Mono<Void> handleResponse(GatewayFilterChain chain,
                                      ServerWebExchange exchange,
                                      Response response) {
        if (response.status() >= 200 && response.status() < 300) {
            return chain.filter(exchange);
        }
        return writeUnauthorized(exchange);
    }

    /**
     * 判断请求路径是否命中白名单。
     *
     * @param path 请求路径
     * @return true 表示命中白名单
     */
    private boolean isWhitelisted(String path) {
        List<String> whitelist = securityProperties.getWhitelist();
        if (whitelist == null || whitelist.isEmpty()) {
            return false;
        }
        return whitelist.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * 返回未登录的 401 JSON 响应。
     *
     * @param exchange 请求上下文
     * @return 写入响应的 Mono
     */
    private Mono<Void> writeUnauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] payload = buildUnauthorizedPayload();
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                .bufferFactory()
                .wrap(payload)));
    }

    /**
     * 构建未登录响应的 JSON 字节数组。
     *
     * @return JSON 内容字节
     */
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

    /**
     * 设置过滤器排序，值越小越先执行。
     *
     * @return 排序值
     */
    @Override
    public int getOrder() {
        return -100;
    }
}
