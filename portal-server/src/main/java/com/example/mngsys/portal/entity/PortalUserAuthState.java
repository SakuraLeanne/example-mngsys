package com.example.mngsys.portal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("portal_user_auth_state")
/**
 * PortalUserAuthState。
 * <p>
 * 门户用户的鉴权状态与版本信息，记录权限/档案版本号及重要动作时间，便于缓存刷新与审计。
 * </p>
 */
public class PortalUserAuthState {
    /** 用户 ID（主键）。 */
    @TableId(type = IdType.INPUT)
    private Long userId;
    /** 权限数据版本号，用于增量同步。 */
    private Long authVersion;
    /** 档案数据版本号，用于增量同步。 */
    private Long profileVersion;
    /** 最近密码修改时间。 */
    private LocalDateTime lastPwdChangeTime;
    /** 最近档案更新（个人信息修改）时间。 */
    private LocalDateTime lastProfileUpdateTime;
    /** 最近被禁用时间。 */
    private LocalDateTime lastDisableTime;
    /** 记录更新时间。 */
    private LocalDateTime updateTime;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getAuthVersion() {
        return authVersion;
    }

    public void setAuthVersion(Long authVersion) {
        this.authVersion = authVersion;
    }

    public Long getProfileVersion() {
        return profileVersion;
    }

    public void setProfileVersion(Long profileVersion) {
        this.profileVersion = profileVersion;
    }

    public LocalDateTime getLastPwdChangeTime() {
        return lastPwdChangeTime;
    }

    public void setLastPwdChangeTime(LocalDateTime lastPwdChangeTime) {
        this.lastPwdChangeTime = lastPwdChangeTime;
    }

    public LocalDateTime getLastProfileUpdateTime() {
        return lastProfileUpdateTime;
    }

    public void setLastProfileUpdateTime(LocalDateTime lastProfileUpdateTime) {
        this.lastProfileUpdateTime = lastProfileUpdateTime;
    }

    public LocalDateTime getLastDisableTime() {
        return lastDisableTime;
    }

    public void setLastDisableTime(LocalDateTime lastDisableTime) {
        this.lastDisableTime = lastDisableTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
