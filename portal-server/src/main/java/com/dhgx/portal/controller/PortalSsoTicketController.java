package com.dhgx.portal.controller;

import com.dhgx.common.portal.dto.PortalLoginResponse;
import com.dhgx.common.portal.dto.PortalSsoTicketVerifyRequest;
import com.dhgx.portal.common.api.ApiResponse;
import com.dhgx.portal.common.api.ErrorCode;
import com.dhgx.portal.service.PortalSsoTicketService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * SSO ticket 验证接口。
 */
@RestController
@RequestMapping("/sso/ticket")
@Validated
public class PortalSsoTicketController {

    private final PortalSsoTicketService portalSsoTicketService;

    public PortalSsoTicketController(PortalSsoTicketService portalSsoTicketService) {
        this.portalSsoTicketService = portalSsoTicketService;
    }

    @PostMapping("/verify")
    public ApiResponse<PortalLoginResponse> verify(@Valid @RequestBody PortalSsoTicketVerifyRequest request) {
        PortalSsoTicketService.VerifyResult result = portalSsoTicketService.verifyAndConsume(
                request.getSystemCode(),
                request.getTicket(),
                request.getRedirectUri(),
                request.getState());
        if (!result.isSuccess()) {
            ErrorCode errorCode = result.getErrorCode() == null ? ErrorCode.SSO_TICKET_SYSTEM_ERROR : result.getErrorCode();
            return ApiResponse.failure(errorCode);
        }
        return ApiResponse.success(result.getLoginResponse());
    }
}
