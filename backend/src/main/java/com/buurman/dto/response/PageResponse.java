package com.buurman.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Paginated response wrapper with content and pagination metadata")
public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    int numberOfElements,
    boolean first,
    boolean last,
    boolean empty) {
  public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
    int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
    return new PageResponse<>(
        content,
        page,
        size,
        totalElements,
        totalPages,
        content.size(),
        page == 0,
        page >= totalPages - 1 || totalPages == 0,
        content.isEmpty());
  }
}
