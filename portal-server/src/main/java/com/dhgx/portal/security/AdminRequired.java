package com.dhgx.portal.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
/**
 * AdminRequired。
 */
public @interface AdminRequired {
    /**
     * 权限范围：portal 表示门户管理员，app 表示应用管理员。
     */
    String scope() default "portal";

    /**
     * 请求参数中的 appCode 名称。
     */
    String appCodeParam() default "appCode";

    /**
     * 当无法解析 appCode 时，是否允许任意应用管理员访问。
     */
    boolean allowAnyAppAdmin() default false;
}
