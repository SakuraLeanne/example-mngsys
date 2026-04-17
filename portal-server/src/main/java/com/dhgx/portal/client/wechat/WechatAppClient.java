package com.dhgx.portal.client.wechat;

import com.dhgx.portal.config.PortalProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * WechatAppClient。
 * <p>
 * 负责调用微信开放平台 APP 授权接口，用 code 换取 openId/unionId。
 * </p>
 */
@Component
public class WechatAppClient {

    private static final Logger log = LoggerFactory.getLogger(WechatAppClient.class);
    private final RestTemplate restTemplate = new RestTemplate();
    private final PortalProperties portalProperties;

    public WechatAppClient(PortalProperties portalProperties) {
        this.portalProperties = portalProperties;
    }

    public WechatAppOauthResponse code2Oauth(String code) {
        PortalProperties.Wechat.App app = portalProperties.getWechat().getApp();
        if (!StringUtils.hasText(app.getAppId()) || !StringUtils.hasText(app.getAppSecret())) {
            throw new IllegalStateException("微信APP appId/appSecret 未配置");
        }
        String url = UriComponentsBuilder.fromHttpUrl(app.getOauthAccessTokenUrl())
                .queryParam("appid", app.getAppId())
                .queryParam("secret", app.getAppSecret())
                .queryParam("code", code)
                .queryParam("grant_type", "authorization_code")
                .build(true)
                .toUriString();
        ResponseEntity<WechatAppOauthResponse> response = restTemplate.getForEntity(url, WechatAppOauthResponse.class);
        WechatAppOauthResponse body = response.getBody();
        if (body == null) {
            throw new IllegalStateException("微信APP授权接口无响应");
        }
        if (body.getErrcode() != null && body.getErrcode() != 0) {
            log.warn("wechat app oauth failed, errcode={}, errmsg={}", body.getErrcode(), body.getErrmsg());
            throw new IllegalArgumentException("微信APP登录失败：" + body.getErrmsg());
        }
        if (!StringUtils.hasText(body.getOpenid())) {
            throw new IllegalStateException("微信APP返回 openId 为空");
        }
        return body;
    }
}
