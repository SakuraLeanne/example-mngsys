package com.dhgx.portal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("portal_audit_log")
/**
 * PortalAuditLog。
 * <p>
 * 门户审计日志实体，记录用户在管理系统中的关键操作、资源访问及结果，便于追踪与合规审计。
 * </p>
 */
public class PortalAuditLog {
    /** 主键 ID，自增。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 操作用户 ID。 */
    @TableField("user_id")
    private String userId;
    /** 操作用户名。 */
    private String username;
    /** 操作类型/动作。 */
    private String action;
    /** 目标资源标识。 */
    private String resource;
    /** 操作详情或请求参数摘要。 */
    private String detail;
    /** 操作来源 IP。 */
    private String ip;
    /** 操作状态，1 为成功，其他为失败或异常。 */
    private Integer status;
    /** 记录创建时间。 */
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
