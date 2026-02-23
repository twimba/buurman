package com.buurman.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BulkCreateResult<T>(int index, T result, String error) {

  public boolean isSuccess() {
    return error == null;
  }

  public static <T> BulkCreateResult<T> success(int index, T result) {
    return new BulkCreateResult<>(index, result, null);
  }

  public static <T> BulkCreateResult<T> error(int index, String error) {
    return new BulkCreateResult<>(index, null, error);
  }
}
