package com.buurman.dto.request;

public record PageRequest(
        int page,
        int size,
        String sort,
        String direction
) {
    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 25;
    public static final int MAX_SIZE = 500;
    public static final String DEFAULT_DIRECTION = "desc";

    public static PageRequest of(Integer page, Integer size, String sort, String direction) {
        int p = (page != null && page >= 0) ? page : DEFAULT_PAGE;
        int s = (size != null && size > 0) ? Math.min(size, MAX_SIZE) : DEFAULT_SIZE;
        String d = "asc".equalsIgnoreCase(direction) ? "asc" : DEFAULT_DIRECTION;
        return new PageRequest(p, s, sort != null ? sort : null, d);
    }

    public int offset() {
        return page * size;
    }
}
