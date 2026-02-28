package com.buurman.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Sort order direction for paginated queries")
public enum SortDirection {
  ASC,
  DESC
}
