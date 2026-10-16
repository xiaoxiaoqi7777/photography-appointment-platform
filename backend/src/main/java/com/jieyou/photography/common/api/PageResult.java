package com.jieyou.photography.common.api;

import java.util.List;

/**
 * 分页响应结构，依据 docs/API接口约定.md 2.6 节：{ total, page, pageSize, list }。
 */
public class PageResult<T> {

    private long total;
    private int page;
    private int pageSize;
    private List<T> list;

    public PageResult() {
    }

    public PageResult(long total, int page, int pageSize, List<T> list) {
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
        this.list = list;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }
}
