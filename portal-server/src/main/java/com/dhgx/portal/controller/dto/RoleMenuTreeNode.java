package com.dhgx.portal.controller.dto;

import com.dhgx.portal.entity.AppMenuResource;

import java.util.ArrayList;
import java.util.List;

/**
 * 角色菜单树节点 DTO，包含授权标记。
 */
public class RoleMenuTreeNode {
    private final Long id;
    private final String appCode;
    private final String menuCode;
    private final String menuModule;
    private final String menuName;
    private final String menuPath;
    private final String menuType;
    private final Long parentId;
    private final String permission;
    private final Integer sort;
    private final Integer status;
    private final boolean granted;
    private final List<RoleMenuTreeNode> children = new ArrayList<>();

    private RoleMenuTreeNode(Long id, String appCode, String menuCode, String menuModule, String menuName,
                             String menuPath, String menuType, Long parentId, String permission, Integer sort,
                             Integer status, boolean granted) {
        this.id = id;
        this.appCode = appCode;
        this.menuCode = menuCode;
        this.menuModule = menuModule;
        this.menuName = menuName;
        this.menuPath = menuPath;
        this.menuType = menuType;
        this.parentId = parentId;
        this.permission = permission;
        this.sort = sort;
        this.status = status;
        this.granted = granted;
    }

    public static RoleMenuTreeNode from(AppMenuResource menu, boolean granted) {
        return new RoleMenuTreeNode(menu.getId(), menu.getAppCode(), menu.getMenuCode(), menu.getMenuModule(),
                menu.getMenuName(), menu.getMenuPath(), menu.getMenuType(), menu.getParentId(),
                menu.getPermission(), menu.getSort(), menu.getStatus(), granted);
    }

    public Long getId() {
        return id;
    }

    public String getAppCode() {
        return appCode;
    }

    public String getMenuCode() {
        return menuCode;
    }

    public String getMenuModule() {
        return menuModule;
    }

    public String getMenuName() {
        return menuName;
    }

    public String getMenuPath() {
        return menuPath;
    }

    public String getMenuType() {
        return menuType;
    }

    public Long getParentId() {
        return parentId;
    }

    public String getPermission() {
        return permission;
    }

    public Integer getSort() {
        return sort;
    }

    public Integer getStatus() {
        return status;
    }

    public boolean isGranted() {
        return granted;
    }

    public List<RoleMenuTreeNode> getChildren() {
        return children;
    }
}
