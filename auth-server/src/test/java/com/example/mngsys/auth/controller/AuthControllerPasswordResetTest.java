package com.example.mngsys.auth.controller;

import com.example.mngsys.auth.common.api.ErrorCode;
import com.example.mngsys.auth.common.exception.GlobalExceptionHandler;
import com.example.mngsys.auth.common.exception.LocalizedBusinessException;
import com.example.mngsys.auth.config.AuthProperties;
import com.example.mngsys.auth.service.AuthService;
import com.example.mngsys.auth.service.PasswordResetService;
import com.example.mngsys.auth.service.SmsCodeService;
import com.example.mngsys.common.security.PasswordCryptoService;
import com.example.mngsys.common.security.PasswordEncryptProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@Import({GlobalExceptionHandler.class})
class AuthControllerPasswordResetTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private SmsCodeService smsCodeService;

    @MockBean
    private PasswordResetService passwordResetService;

    @MockBean
    private PasswordCryptoService passwordCryptoService;

    @Test
    void shouldReturnChineseMessageWhenResetTokenExpired() throws Exception {
        given(passwordCryptoService.decrypt(anyString(), anyString())).willReturn("PlainPassw0rd!");
        willThrow(new LocalizedBusinessException(
                ErrorCode.INVALID_ARGUMENT,
                "error.reset.token.expired",
                "重置令牌已失效，请重新获取验证码"))
                .given(passwordResetService).resetPassword(eq("13800000000"), eq("expired-token"), eq("PlainPassw0rd!"));

        mockMvc.perform(MockMvcRequestBuilders.post("/auth/api/password/forgot/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN")
                        .content(objectMapper.writeValueAsString(buildRequestPayload("13800000000", "expired-token"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_ARGUMENT.getCode()))
                .andExpect(jsonPath("$.message").value("重置令牌已失效，请重新获取验证码"));
    }

    @Test
    void shouldReturnEnglishMessageWhenLocaleIsEnglish() throws Exception {
        given(passwordCryptoService.decrypt(anyString(), anyString())).willReturn("PlainPassw0rd!");
        willThrow(new LocalizedBusinessException(
                ErrorCode.INVALID_ARGUMENT,
                "error.reset.token.mismatch",
                "Reset verification failed, please retry"))
                .given(passwordResetService).resetPassword(eq("13800000000"), eq("wrong-token"), eq("PlainPassw0rd!"));

        mockMvc.perform(MockMvcRequestBuilders.post("/auth/api/password/forgot/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                        .content(objectMapper.writeValueAsString(buildRequestPayload("13800000000", "wrong-token"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_ARGUMENT.getCode()))
                .andExpect(jsonPath("$.message").value("The reset verification failed. Please request a new code."));
    }

    private Map<String, Object> buildRequestPayload(String mobile, String token) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("mobile", mobile);
        payload.put("resetToken", token);
        payload.put("encryptedPassword", "cipher");
        payload.put("newPassword", "PlainPassw0rd!");
        return payload;
    }

    @TestConfiguration
    static class AuthTestConfig {
        @Bean
        AuthProperties authProperties() {
            AuthProperties properties = new AuthProperties();
            PasswordEncryptProperties encryptProperties = new PasswordEncryptProperties();
            encryptProperties.setEnabled(false);
            properties.setPasswordEncrypt(encryptProperties);

            AuthProperties.PasswordResetProperties resetProperties = new AuthProperties.PasswordResetProperties();
            resetProperties.setTokenTtlSeconds(300);
            resetProperties.setIssueIntervalSeconds(60);
            resetProperties.setMaxVerifyFailures(3);
            properties.setPasswordReset(resetProperties);
            properties.setInternalToken("test-token");
            return properties;
        }
    }
}
