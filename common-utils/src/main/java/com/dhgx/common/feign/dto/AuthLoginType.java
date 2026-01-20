package com.dhgx.common.feign.dto;

/**
 * 登录方式。
 */
public enum AuthLoginType {
    /** 手机号验证码登录。 */
    SMS,
    /** 用户名密码登录。 */
    USERNAME_PASSWORD,
    /** 二维码登录，预留扩展。 */
    QR_CODE
}
