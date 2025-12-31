package com.example.mngsys.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("portal_user")
/**
 * AuthUser。
 * <p>
 * 认证中心使用的用户实体，映射门户用户表 {@code portal_user}。
 * 仅包含认证所需的核心字段，其他字段由门户端负责维护。
 * </p>
 */
public class AuthUser {
    /** 主键 ID。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;
    /** 用户名。 */
    private String username;
    /** 手机号。 */
    private String mobile;
    /** 手机号是否已验证。 */
    @TableField("mobile_verified")
    private Integer mobileVerified;
    /** 邮箱。 */
    private String email;
    /** 邮箱是否已验证。 */
    @TableField("email_verified")
    private Integer emailVerified;
    /** 密码（加密存储）。 */
    private String password;
    /** 状态。 */
    private Integer status;
    /** 真实姓名。 */
    @TableField("real_name")
    private String realName;
    /** 昵称。 */
    @TableField("nick_name")
    private String nickName;
    /** 创建时间。 */
    @TableField("create_time")
    private LocalDateTime createTime;
    /** 更新时间。 */
    @TableField("update_time")
    private LocalDateTime updateTime;
    /** 创建人。 */
    @TableField("create_by")
    private String createBy;
    /** 更新人。 */
    @TableField("update_by")
    private String updateBy;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public Integer getMobileVerified() {
        return mobileVerified;
    }

    public void setMobileVerified(Integer mobileVerified) {
        this.mobileVerified = mobileVerified;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Integer emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
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

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }
}
