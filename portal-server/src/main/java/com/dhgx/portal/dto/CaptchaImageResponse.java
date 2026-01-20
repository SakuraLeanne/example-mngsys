package com.dhgx.portal.dto;

/**
 * CaptchaImageResponse.
 */
public class CaptchaImageResponse {
    private final String captchaId;
    private final long expireIn;
    private final String imageBase64;

    public CaptchaImageResponse(String captchaId, long expireIn, String imageBase64) {
        this.captchaId = captchaId;
        this.expireIn = expireIn;
        this.imageBase64 = imageBase64;
    }

    public String getCaptchaId() {
        return captchaId;
    }

    public long getExpireIn() {
        return expireIn;
    }

    public String getImageBase64() {
        return imageBase64;
    }
}
