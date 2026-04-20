package com.dhgx.common.feign;

import feign.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * PortalFeignClient。
 * <p>
 * 网关调用门户内部接口（如移动 token introspect）。
 * </p>
 */
@FeignClient(name = "portal-server", path = "${portal.feign.path:/portal-server/portal/api}")
public interface PortalFeignClient {

    @PostMapping("/mobile/token/introspect")
    Response introspect(@RequestHeader(value = "Authorization", required = false) String authorization);
}
