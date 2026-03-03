package com.dhgx.portal.controller;

import com.dhgx.portal.common.api.ErrorCode;
import com.dhgx.portal.common.exception.GlobalExceptionHandler;
import com.dhgx.portal.common.PortalActionTicketUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

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
}
