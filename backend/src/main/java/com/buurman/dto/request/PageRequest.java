package com.buurman.dto.request;

import com.buurman.domain.SortDirection;

import static com.buurman.domain.SortDirection.DESC;

public record PageRequest(
        int page,
        int size,
        String sort,
        SortDirection direction
) {
    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 25;
    public static final int MAX_SIZE = 500;

    public static PageRequest of(Integer page, Integer size, String sort, SortDirection direction) {
        int p = (page != null && page >= 0) ? page : DEFAULT_PAGE;
        int s = (size != null && size > 0) ? Math.min(size, MAX_SIZE) : DEFAULT_SIZE;
        SortDirection d = direction != null ? direction : DESC;
        return new PageRequest(p, s, sort, d);
    }

    public int offset() {
        return page * size;
    }
}
