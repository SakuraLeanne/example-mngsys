package com.example.mngsys.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * GatewaySecurityProperties。
 * <p>
 * 网关安全配置属性，主要包含接口白名单。
 * </p>
 */
@ConfigurationProperties(prefix = "gateway.security")
public class GatewaySecurityProperties {
    /** 网关放行的白名单路径集合。 */
    private List<String> whitelist = new ArrayList<>();

    /**
     * 获取白名单列表。
     *
     * @return 白名单路径集合
     */
    public List<String> getWhitelist() {
        return whitelist;
    }

    /**
     * 设置白名单列表。
     *
     * @param whitelist 需要放行的路径模式集合
     */
    public void setWhitelist(List<String> whitelist) {
        this.whitelist = whitelist;
    }
}
