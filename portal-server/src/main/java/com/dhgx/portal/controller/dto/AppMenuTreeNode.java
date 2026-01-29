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
     * 菜单模块。
     */
    private final String menuModule;
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
     * 排序值。
     */
    private final Integer sort;
    /**
     * 状态。
     */
    private final Integer status;
    /**
     * 是否已授权，默认 false。
     */
    private boolean granted = false;
    /**
     * 子菜单列表。
     */
    private final List<AppMenuTreeNode> children = new ArrayList<>();

    public AppMenuTreeNode(Long id, String appCode, String menuCode, String menuModule, String menuName,
                           String menuPath, String menuType, Integer sort,
                           Integer status) {
        this.id = id;
        this.appCode = appCode;
        this.menuCode = menuCode;
        this.menuModule = menuModule;
        this.menuName = menuName;
        this.menuPath = menuPath;
        this.menuType = menuType;
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

    public Integer getSort() {
        return sort;
    }

    public Integer getStatus() {
        return status;
    }

    public boolean isGranted() {
        return granted;
    }

    public void setGranted(boolean granted) {
        this.granted = granted;
    }

    public List<AppMenuTreeNode> getChildren() {
        return children;
    }
}
