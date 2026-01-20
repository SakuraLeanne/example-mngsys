package com.dhgx.auth.common.exception;

import com.dhgx.auth.common.api.ApiResponse;
import com.dhgx.auth.common.api.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.DummyController.class)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnLocalizedMessageForBusinessException() throws Exception {
        mockMvc.perform(get("/error/business")
                        .header("Accept-Language", "en")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_ARGUMENT.getCode()))
                .andExpect(jsonPath("$.message").value("The reset token has expired. Please request a new code."));
    }

    @Test
    void shouldReturnGenericLocalizedMessageForIllegalArgument() throws Exception {
        mockMvc.perform(get("/error/illegal")
                        .header("Accept-Language", "zh-CN")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_ARGUMENT.getCode()))
                .andExpect(jsonPath("$.message").value("debug-info"));
    }

    @RestController
    static class DummyController {
        @GetMapping("/error/business")
        public ApiResponse<Void> businessError() {
            throw new LocalizedBusinessException(ErrorCode.INVALID_ARGUMENT, "error.reset.token.expired",
                    "重置令牌已失效，请重新获取验证码");
        }

        @GetMapping("/error/illegal")
        public ApiResponse<Void> illegalArgument() {
            throw new IllegalArgumentException("debug-info");
        }
    }
}
