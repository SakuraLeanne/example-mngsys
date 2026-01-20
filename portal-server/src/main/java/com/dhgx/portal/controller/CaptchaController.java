package com.dhgx.portal.controller;

import com.dhgx.portal.common.api.ApiResponse;
import com.dhgx.portal.service.CaptchaService;
import com.dhgx.portal.dto.CaptchaImageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CaptchaController。
 */
@RestController
public class CaptchaController {

    private final CaptchaService captchaService;

    public CaptchaController(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    /**
     * 获取图形验证码。
     *
     * @return captcha 信息
     */
    @GetMapping("/captcha/image")
    public ApiResponse<CaptchaImageResponse> imageCaptcha() {
        return ApiResponse.success(captchaService.generateCaptcha());
    }
}
