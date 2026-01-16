package com.example.mngsys.common.api;

import java.io.Serializable;

/**
 * 通用操作结果响应。
 * <p>
 * 用于描述简单操作是否成功的统一返回结构。
 * </p>
 */
public final class ActionResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 是否成功。
     */
    private final boolean success;

    public ActionResponse(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }
}
