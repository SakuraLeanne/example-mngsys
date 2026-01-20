package com.dhgx.portal.controller.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单树节点 DTO，表示包含子节点的菜单信息。
 */
public class AppMenuTreeNode {
    /**
     * 菜单主键 ID。
     */
    private final Long id;
    /**
     * 应用编码。
     */
    private final String appCode;
    /**
     * 菜单编码。
     */
    private final String menuCode;
    /**
     * 菜单名称。
     */
    private final String menuName;
    /**
     * 菜单路径。
     */
    private final String menuPath;
    /**
     * 菜单类型。
     */
    private final String menuType;
    /**
     * 父级菜单 ID。
     */
    private final Long parentId;
    /**
     * 权限标识。
     */
    private final String permission;
    /**
     * 排序值。
     */
    private final Integer sort;
    /**
     * 状态。
     */
    private final Integer status;
    /**
     * 子菜单列表。
     */
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
