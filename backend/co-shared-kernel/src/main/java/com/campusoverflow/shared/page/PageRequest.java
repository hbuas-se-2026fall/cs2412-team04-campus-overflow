package com.campusoverflow.shared.page;

/** 分页参数，统一做边界保护：page ≥ 1，1 ≤ size ≤ 50，page 上限防止 offset 溢出。 */
public record PageRequest(int page, int size) {

    public static final int MAX_SIZE = 50;
    public static final int MAX_PAGE = 1_000_000;

    public PageRequest {
        page = Math.min(Math.max(page, 1), MAX_PAGE);
        size = Math.min(Math.max(size, 1), MAX_SIZE);
    }

    /** 用 long 计算，避免 (page-1)*size 在 int 下溢出成负数传给 JDBC。 */
    public long offset() {
        return ((long) page - 1) * size;
    }
}
