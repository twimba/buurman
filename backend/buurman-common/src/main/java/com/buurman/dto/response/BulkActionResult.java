package com.buurman.dto.response;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Per-item outcome of a bulk action keyed by the item's public identifier. Unlike {@link
 * BulkCreateResult}, which is positional because created items have no identifier yet, bulk actions
 * on existing items report back by identifier so the UI can update rows in place.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
public record BulkActionResult<T>(String identifier, Optional<T> result, Optional<String> error) {

  public boolean isSuccess() {
    return error.isEmpty();
  }

  public static <T> BulkActionResult<T> success(String identifier, T result) {
    return new BulkActionResult<>(identifier, Optional.of(result), Optional.empty());
  }

  public static <T> BulkActionResult<T> error(String identifier, String error) {
    return new BulkActionResult<>(identifier, Optional.empty(), Optional.of(error));
  }
}
