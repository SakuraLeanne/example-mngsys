package com.example.mngsys.common.portal.dto;

import javax.validation.constraints.NotBlank;

/**
 * 动作票据请求。
 */
public class PortalActionTicketRequest {
    @NotBlank(message = "ticket 不能为空")
    private String ticket;

    public String getTicket() {
        return ticket;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }
}
