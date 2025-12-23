package com.example.mngsys.portal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("portal_user_auth_state")
public class PortalUserAuthState {
    @TableId(type = IdType.INPUT)
    private Long userId;
    private Long authVersion;
    private Long profileVersion;
    private LocalDateTime lastPwdChangeTime;
    private LocalDateTime lastProfileUpdateTime;
    private LocalDateTime lastDisableTime;
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
