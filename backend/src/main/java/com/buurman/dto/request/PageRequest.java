package com.buurman.dto.request;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.buurman.domain.SortDirection;

public record PageRequest(
    int page, int size, Optional<String> sort, Optional<SortDirection> direction) {
  public static final int DEFAULT_PAGE = 0;
  public static final int DEFAULT_SIZE = 25;
  public static final int MAX_SIZE = 500;

  public static PageRequest of(
      @Nullable Integer page,
      @Nullable Integer size,
      @Nullable String sort,
      @Nullable SortDirection direction) {
    int p = (page != null && page >= 0) ? page : DEFAULT_PAGE;
    int s = (size != null && size > 0) ? Math.min(size, MAX_SIZE) : DEFAULT_SIZE;
    return new PageRequest(p, s, Optional.ofNullable(sort), Optional.ofNullable(direction));
  }

  public int offset() {
    return page * size;
  }
}
