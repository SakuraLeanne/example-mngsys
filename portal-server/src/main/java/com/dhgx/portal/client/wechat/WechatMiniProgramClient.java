package com.dhgx.portal.client.wechat;

import com.dhgx.portal.config.PortalProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;

/**
 * WechatMiniProgramClient。
 * <p>
 * 负责调用微信 jscode2session 接口，用 code 换 openId/session_key。
 * </p>
 */
@Component
public class WechatMiniProgramClient {

    private static final Logger log = LoggerFactory.getLogger(WechatMiniProgramClient.class);
    private final RestTemplate restTemplate = new RestTemplate();
    private final PortalProperties portalProperties;


    /**
     * 返回当前配置的 appId，用于记录身份绑定来源。
     */
    public String resolveConfiguredAppId() {
        return portalProperties.getWechat().getMiniProgram().getAppId();
    }

    public WechatMiniProgramClient(PortalProperties portalProperties) {
        this.portalProperties = portalProperties;
    }

    public WechatMiniProgramSessionResponse code2Session(String code) {
        PortalProperties.Wechat.MiniProgram miniProgram = portalProperties.getWechat().getMiniProgram();
        if (!StringUtils.hasText(miniProgram.getAppId()) || !StringUtils.hasText(miniProgram.getAppSecret())) {
            throw new IllegalStateException("微信小程序 appId/appSecret 未配置");
        }
        String url = UriComponentsBuilder.fromHttpUrl(miniProgram.getJsCode2SessionUrl())
                .queryParam("appid", miniProgram.getAppId())
                .queryParam("secret", miniProgram.getAppSecret())
                .queryParam("js_code", code)
                .queryParam("grant_type", "authorization_code")
                .build(true)
                .toUriString();
        ResponseEntity<WechatMiniProgramSessionResponse> response = restTemplate.getForEntity(url, WechatMiniProgramSessionResponse.class);
        WechatMiniProgramSessionResponse body = response.getBody();
        if (body == null) {
            throw new IllegalStateException("微信接口无响应");
        }
        if (body.getErrcode() != null && body.getErrcode() != 0) {
            log.warn("wechat code2session failed, errcode={}, errmsg={}", body.getErrcode(), body.getErrmsg());
            throw new IllegalArgumentException("微信登录失败：" + body.getErrmsg());
        }
        if (!StringUtils.hasText(body.getOpenid())) {
            throw new IllegalStateException("微信返回 openId 为空");
        }
        return body;
    }

    public WechatAccessTokenResponse getAccessToken() {
        PortalProperties.Wechat.MiniProgram miniProgram = portalProperties.getWechat().getMiniProgram();
        if (!StringUtils.hasText(miniProgram.getAppId()) || !StringUtils.hasText(miniProgram.getAppSecret())) {
            throw new IllegalStateException("微信小程序 appId/appSecret 未配置");
        }
        String url = UriComponentsBuilder.fromHttpUrl(miniProgram.getAccessTokenUrl())
                .queryParam("grant_type", "client_credential")
                .queryParam("appid", miniProgram.getAppId())
                .queryParam("secret", miniProgram.getAppSecret())
                .build(true)
                .toUriString();
        ResponseEntity<WechatAccessTokenResponse> response = restTemplate.getForEntity(url, WechatAccessTokenResponse.class);
        WechatAccessTokenResponse body = response.getBody();
        if (body == null) {
            throw new IllegalStateException("微信 access_token 接口无响应");
        }
        if (body.getErrcode() != null && body.getErrcode() != 0) {
            log.warn("wechat getAccessToken failed, errcode={}, errmsg={}", body.getErrcode(), body.getErrmsg());
            throw new IllegalArgumentException("获取微信 access_token 失败：" + body.getErrmsg());
        }
        if (!StringUtils.hasText(body.getAccessToken())) {
            throw new IllegalStateException("微信 access_token 为空");
        }
        return body;
    }

    public WechatPhoneNumberResponse getPhoneNumber(String accessToken, String code) {
        PortalProperties.Wechat.MiniProgram miniProgram = portalProperties.getWechat().getMiniProgram();
        if (!StringUtils.hasText(accessToken)) {
            throw new IllegalArgumentException("accessToken 不能为空");
        }
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("code 不能为空");
        }
        String url = UriComponentsBuilder.fromHttpUrl(miniProgram.getPhoneNumberUrl())
                .queryParam("access_token", accessToken)
                .build(true)
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<java.util.Map<String, String>> requestEntity = new HttpEntity<>(
                Collections.singletonMap("code", code), headers);
        ResponseEntity<WechatPhoneNumberResponse> response = restTemplate.exchange(
                url, HttpMethod.POST, requestEntity, WechatPhoneNumberResponse.class);
        WechatPhoneNumberResponse body = response.getBody();
        if (body == null) {
            throw new IllegalStateException("微信手机号接口无响应");
        }
        return body;
    }
}
