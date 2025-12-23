package com.example.mngsys.portal.controller.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * AppMenuTreeNode。
 */
public class AppMenuTreeNode {
    private final Long id;
    private final String appCode;
    private final String menuCode;
    private final String menuName;
    private final String menuPath;
    private final String menuType;
    private final Long parentId;
    private final String permission;
    private final Integer sort;
    private final Integer status;
    private final List<AppMenuTreeNode> children = new ArrayList<>();

    public AppMenuTreeNode(Long id, String appCode, String menuCode, String menuName, String menuPath,
                           String menuType, Long parentId, String permission, Integer sort, Integer status) {
        this.id = id;
        this.appCode = appCode;
        this.menuCode = menuCode;
        this.menuName = menuName;
        this.menuPath = menuPath;
        this.menuType = menuType;
        this.parentId = parentId;
        this.permission = permission;
        this.sort = sort;
        this.status = status;
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

    public List<AppMenuTreeNode> getChildren() {
        return children;
    }
}
