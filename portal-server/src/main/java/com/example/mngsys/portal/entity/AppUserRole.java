package com.example.mngsys.portal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("app_user_role")
/**
 * AppUserRole。
 * <p>
 * 用户与角色的关联表，记录用户在某应用中具备的角色集合，用于鉴权计算。
 * </p>
 */
public class AppUserRole {
    /** 主键 ID，自增。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 用户 ID，关联门户用户表。 */
    private Long userId;
    /** 角色 ID，关联 {@link AppRole#id}。 */
    private Long roleId;
    /** 关联创建时间。 */
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
