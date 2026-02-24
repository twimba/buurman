package com.buurman.dto.response;

import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BulkCreateResult<T>(int index, Optional<T> result, Optional<String> error) {

  public BulkCreateResult {
    result = Objects.requireNonNullElse(result, Optional.empty());
    error = Objects.requireNonNullElse(error, Optional.empty());
  }

  public boolean isSuccess() {
    return error.isEmpty();
  }

  public static <T> BulkCreateResult<T> success(int index, T result) {
    return new BulkCreateResult<>(index, Optional.of(result), Optional.empty());
  }

  public static <T> BulkCreateResult<T> error(int index, String error) {
    return new BulkCreateResult<>(index, Optional.empty(), Optional.of(error));
  }
}
