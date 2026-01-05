package com.example.mngsys.portal.controller;

import com.example.mngsys.portal.client.AuthClient;
import com.example.mngsys.portal.common.api.ApiResponse;
import com.example.mngsys.portal.common.api.ErrorCode;
import com.example.mngsys.portal.service.PortalActionService;
import com.example.mngsys.portal.service.PortalAuthService;
import com.example.mngsys.portal.service.PortalPasswordService;
import com.example.mngsys.portal.service.PortalProfileService;
import com.example.mngsys.portal.service.PortalUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PortalApiController.class)
class PortalApiControllerPasswordResetPassthroughTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PortalAuthService portalAuthService;

    @MockBean
    private PortalActionService portalActionService;

    @MockBean
    private PortalPasswordService portalPasswordService;

    @MockBean
    private PortalProfileService portalProfileService;

    @MockBean
    private PortalUserService portalUserService;

    @MockBean
    private AuthClient authClient;

    @Test
    void shouldPassthroughForgotSendErrorMessageFromAuth() throws Exception {
        given(authClient.sendForgotPasswordSms("13800000000"))
                .willReturn(ApiResponse.failure(ErrorCode.INVALID_ARGUMENT, "重置令牌已失效"));

        mockMvc.perform(post("/portal/api/password/forgot/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SmsSendRequest("13800000000"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_ARGUMENT.getCode()))
                .andExpect(jsonPath("$.message").value("重置令牌已失效"));
    }

    @Test
    void shouldPassthroughVerifyResponseFromAuth() throws Exception {
        AuthClient.ResetTokenResponse data = new AuthClient.ResetTokenResponse();
        data.setResetToken("token-from-auth");
        given(authClient.verifyForgotPassword("13800000000", "123456"))
                .willReturn(ApiResponse.success(data));

        mockMvc.perform(post("/portal/api/password/forgot/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SmsVerifyRequest("13800000000", "123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.resetToken").value("token-from-auth"));
    }

    @Test
    void shouldPassthroughResetErrorMessageFromAuth() throws Exception {
        given(authClient.resetForgotPassword("13800000000", "bad-token", "cipher", "newPass"))
                .willReturn(ApiResponse.failure(ErrorCode.INVALID_ARGUMENT, "重置验证失败"));

        mockMvc.perform(post("/portal/api/password/forgot/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PasswordResetRequest("13800000000", "bad-token", "cipher", "newPass"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_ARGUMENT.getCode()))
                .andExpect(jsonPath("$.message").value("重置验证失败"));
    }

    static class SmsSendRequest {
        private String mobile;

        public SmsSendRequest(String mobile) {
            this.mobile = mobile;
        }

        public String getMobile() {
            return mobile;
        }

        public void setMobile(String mobile) {
            this.mobile = mobile;
        }
    }

    static class SmsVerifyRequest {
        private String mobile;
        private String code;

        public SmsVerifyRequest(String mobile, String code) {
            this.mobile = mobile;
            this.code = code;
        }

        public String getMobile() {
            return mobile;
        }

        public void setMobile(String mobile) {
            this.mobile = mobile;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    }

    static class PasswordResetRequest {
        private String mobile;
        private String resetToken;
        private String encryptedPassword;
        private String newPassword;

        public PasswordResetRequest(String mobile, String resetToken, String encryptedPassword, String newPassword) {
            this.mobile = mobile;
            this.resetToken = resetToken;
            this.encryptedPassword = encryptedPassword;
            this.newPassword = newPassword;
        }

        public String getMobile() {
            return mobile;
        }

        public void setMobile(String mobile) {
            this.mobile = mobile;
        }

        public String getResetToken() {
            return resetToken;
        }

        public void setResetToken(String resetToken) {
            this.resetToken = resetToken;
        }

        public String getEncryptedPassword() {
            return encryptedPassword;
        }

        public void setEncryptedPassword(String encryptedPassword) {
            this.encryptedPassword = encryptedPassword;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }
}
