package com.example.mngsys.common.feign;

import com.example.mngsys.common.feign.dto.AuthKickRequest;
import com.example.mngsys.common.feign.dto.AuthLoginRequest;
import com.example.mngsys.common.feign.dto.AuthPasswordResetRequest;
import com.example.mngsys.common.feign.dto.AuthSmsSendRequest;
import com.example.mngsys.common.feign.dto.AuthSmsVerifyRequest;
import feign.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * AuthFeignClient。
 * <p>
 * 认证服务 Feign 接口，统一由 common-utils 暴露，供网关与门户共用，
 * 以避免多处定义导致路径或模型不一致。
 * </p>
 */
@FeignClient(name = "auth-server", path = "${auth.feign.path:/auth-server/auth/api}")
public interface AuthFeignClient {

    /**
     * 用户登录。
     *
     * @param request 登录请求
     * @return 原始响应
     */
    @PostMapping("/login")
    Response login(@RequestBody AuthLoginRequest request);

    /**
     * 发送登录短信验证码。
     *
     * @param request 短信请求
     * @return 原始响应
     */
    @PostMapping("/sms/send")
    Response sendSms(@RequestBody AuthSmsSendRequest request);

    /**
     * 校验短信验证码。
     *
     * @param request 校验请求
     * @return 原始响应
     */
    @PostMapping("/sms/verify")
    Response verifySms(@RequestBody AuthSmsVerifyRequest request);

    /**
     * 忘记密码发送验证码。
     *
     * @param request 短信请求
     * @return 原始响应
     */
    @PostMapping("/password/forgot/send")
    Response sendForgotPassword(@RequestBody AuthSmsSendRequest request);

    /**
     * 忘记密码校验验证码。
     *
     * @param request 校验请求
     * @return 原始响应
     */
    @PostMapping("/password/forgot/verify")
    Response verifyForgotPassword(@RequestBody AuthSmsVerifyRequest request);

    /**
     * 忘记密码重置密码。
     *
     * @param request 重置请求
     * @return 原始响应
     */
    @PostMapping("/password/forgot/reset")
    Response resetForgotPassword(@RequestBody AuthPasswordResetRequest request);

    /**
     * 退出登录。
     *
     * @param cookie Cookie 头
     * @return 原始响应
     */
    @PostMapping("/logout")
    Response logout(@RequestHeader(value = "Cookie", required = false) String cookie);

    /**
     * 查询登录会话信息。
     *
     * @param cookie Cookie 头
     * @return 原始响应
     */
    @GetMapping("/login/session-info")
    Response sessionMe(@RequestHeader(value = "Cookie", required = false) String cookie);

    /**
     * 踢出指定用户。
     *
     * @param internalToken 内部鉴权 Token
     * @param request       请求体
     * @return 原始响应
     */
    @PostMapping("/session/kick")
    Response kick(@RequestHeader("X-Internal-Token") String internalToken,
                  @RequestBody AuthKickRequest request);
}
