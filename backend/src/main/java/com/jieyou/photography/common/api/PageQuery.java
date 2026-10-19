package com.jieyou.photography.common.api;

/**
 * 分页请求参数归一化，依据 docs/API接口约定.md 2.6 节：
 * 页码从 1 开始，每页默认 10 条、上限 100 条。
 */
public record PageQuery(int page, int pageSize) {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    public static PageQuery of(Integer page, Integer pageSize) {
        int normalizedPage = (page == null || page < 1) ? DEFAULT_PAGE : page;
        int normalizedPageSize = (pageSize == null || pageSize < 1)
                ? DEFAULT_PAGE_SIZE
                : Math.min(pageSize, MAX_PAGE_SIZE);
        return new PageQuery(normalizedPage, normalizedPageSize);
    }
}
