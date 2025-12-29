package com.example.mngsys.portal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("app_role_menu")
/**
 * AppRoleMenu。
 * <p>
 * 角色与菜单的关联关系表，一条记录表示角色拥有某个菜单/资源的访问权限。
 * </p>
 */
public class AppRoleMenu {
    /** 主键 ID，自增。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 角色 ID，关联 {@link AppRole#id}。 */
    private Long roleId;
    /** 菜单/资源 ID，关联 {@link AppMenuResource#id}。 */
    private Long menuId;
    /** 关联创建时间。 */
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Long getMenuId() {
        return menuId;
    }

    public void setMenuId(Long menuId) {
        this.menuId = menuId;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
