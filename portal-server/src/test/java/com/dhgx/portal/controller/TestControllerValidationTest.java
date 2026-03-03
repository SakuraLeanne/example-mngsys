package com.dhgx.portal.controller;

import com.dhgx.portal.common.PortalActionTicketUtil;
import com.dhgx.portal.common.api.ErrorCode;
import com.dhgx.portal.common.exception.GlobalExceptionHandler;
import com.dhgx.portal.common.exception.InvalidReturnUrlException;
import com.dhgx.portal.entity.PortalUser;
import com.dhgx.portal.service.PortalUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TestController.class)
@Import(GlobalExceptionHandler.class)
class TestControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PortalActionTicketUtil portalActionTicketUtil;

    @MockBean
    private PortalUserService portalUserService;

    @Test
    void shouldReturnInvalidArgumentWhenUserIdIsNotPositiveOnPwd() throws Exception {
        mockMvc.perform(get("/test/action-ticket/pwd")
                        .param("userId", "0")
                        .param("returnUrl", "https://example.com/callback"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_ARGUMENT.getCode()))
                .andExpect(jsonPath("$.message").value("参数userId必须大于0"));
    }

    @Test
    void shouldReturnInvalidArgumentWhenReturnUrlIsBlankOnProfile() throws Exception {
        mockMvc.perform(get("/test/action-ticket/profile")
                        .param("userId", "1")
                        .param("returnUrl", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_ARGUMENT.getCode()))
                .andExpect(jsonPath("$.message").value("参数returnUrl不能为空"));
    }

    @Test
    void shouldReturnNotFoundWhenUserDoesNotExist() throws Exception {
        given(portalUserService.getById("1")).willReturn(null);

        mockMvc.perform(get("/test/action-ticket/pwd")
                        .param("userId", "1")
                        .param("returnUrl", "https://example.com/callback"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value("用户不存在"));
    }

    @Test
    void shouldReturnUserDisabledWhenUserStatusIsDisabled() throws Exception {
        PortalUser user = new PortalUser();
        user.setId("1");
        user.setStatus(0);
        given(portalUserService.getById("1")).willReturn(user);

        mockMvc.perform(get("/test/action-ticket/profile")
                        .param("userId", "1")
                        .param("returnUrl", "https://example.com/callback"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.USER_DISABLED.getCode()))
                .andExpect(jsonPath("$.message").value("账号已被停用，请联系管理员"));
    }

    @Test
    void shouldReturnInvalidReturnUrlWhenReturnUrlNotInWhitelist() throws Exception {
        PortalUser user = new PortalUser();
        user.setId("1");
        user.setStatus(1);
        given(portalUserService.getById("1")).willReturn(user);
        given(portalActionTicketUtil.createPwdChangeJumpUrl(1L, "https://blocked.example.com/callback"))
                .willThrow(new InvalidReturnUrlException("回调地址域名未在白名单，请联系管理员"));

        mockMvc.perform(get("/test/action-ticket/pwd")
                        .param("userId", "1")
                        .param("returnUrl", "https://blocked.example.com/callback"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_RETURN_URL.getCode()))
                .andExpect(jsonPath("$.message").value("回调地址域名未在白名单，请联系管理员"));
    }
}
