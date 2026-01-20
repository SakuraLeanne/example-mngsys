package com.dhgx.portal.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;
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
    /** 主键 ID，字符串雪花/UUID。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;
    /** 用户名（登录账号，默认同手机号）。 */
    private String username;
    /** 手机号，唯一且必填。 */
    private String mobile;
    /** 手机号是否已验证：0-否，1-是。 */
    @TableField("mobile_verified")
    private Integer mobileVerified;
    /** 邮箱。 */
    private String email;
    /** 邮箱是否已验证：0-否，1-是。 */
    @TableField("email_verified")
    private Integer emailVerified;
    /** 密码哈希串（仅保存编码后的值）。 */
    @JsonIgnore
    private String password;
    /** 用户状态：0-禁用，1-正常，2-冻结。 */
    private Integer status;
    /** 用户真实姓名。 */
    @TableField("real_name")
    private String realName;
    /** 昵称。 */
    @TableField("nick_name")
    private String nickName;
    /** 性别。 */
    private String gender;
    /** 出生日期。 */
    private LocalDate birthday;
    /** 公司名称。 */
    @TableField("company_name")
    private String companyName;
    /** 部门。 */
    private String department;
    /** 职位。 */
    private String position;
    /** 租户/园区 ID。 */
    @TableField("tenant_id")
    private String tenantId;
    /** 备注。 */
    private String remark;
    /** 创建时间。 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    /** 更新时间。 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    /** 创建人 ID。 */
    @TableField("create_by")
    private String createBy;
    /** 修改人 ID。 */
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

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    public void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
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
