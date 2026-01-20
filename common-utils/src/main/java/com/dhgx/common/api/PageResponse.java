package com.dhgx.common.api;

import java.io.Serializable;
import java.util.List;

/**
 * 分页响应包装，包含记录总数和数据列表。
 *
 * @param <T> 数据类型
 */
public final class PageResponse<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 总记录数。
     */
    private final long total;
    /**
     * 当前页码。
     */
    private final long page;
    /**
     * 每页大小。
     */
    private final long size;
    /**
     * 当前页记录列表。
     */
    private final List<T> records;

    public PageResponse(long total, long page, long size, List<T> records) {
        this.total = total;
        this.page = page;
        this.size = size;
        this.records = records;
    }

    public long getTotal() {
        return total;
    }

    public long getPage() {
        return page;
    }

    public long getSize() {
        return size;
    }

    public List<T> getRecords() {
        return records;
    }
}
