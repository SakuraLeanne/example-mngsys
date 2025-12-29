package com.example.mngsys.portal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("portal_user")
/**
 * PortalUser。
 * <p>
 * 门户用户表实体类，映射表 {@code portal_user}，记录用户的基础信息、状态以及审计字段。
 * 使用 MyBatis-Plus 进行 ORM 映射，支撑登录鉴权和用户管理。
 * </p>
 */
public class PortalUser {
    /** 主键 ID，自增。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 用户名（登录账号）。 */
    private String username;
    /** 用户真实姓名。 */
    private String realName;
    /** 手机号。 */
    private String mobile;
    /** 邮箱。 */
    private String email;
    /** 用户状态，通常 1 表示启用，0 表示禁用。 */
    private Integer status;
    /** 禁用原因。 */
    private String disableReason;
    /** 禁用时间。 */
    private LocalDateTime disableTime;
    /** 创建时间。 */
    private LocalDateTime createTime;
    /** 更新时间。 */
    private LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getDisableReason() {
        return disableReason;
    }

    public void setDisableReason(String disableReason) {
        this.disableReason = disableReason;
    }

    public LocalDateTime getDisableTime() {
        return disableTime;
    }

    public void setDisableTime(LocalDateTime disableTime) {
        this.disableTime = disableTime;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
