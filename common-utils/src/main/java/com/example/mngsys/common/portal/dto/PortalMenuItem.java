package com.example.mngsys.common.portal.dto;

/**
 * 菜单项信息。
 */
public class PortalMenuItem {
    private final String code;
    private final String name;
    private final String path;

    public PortalMenuItem(String code, String name, String path) {
        this.code = code;
        this.name = name;
        this.path = path;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }
}
