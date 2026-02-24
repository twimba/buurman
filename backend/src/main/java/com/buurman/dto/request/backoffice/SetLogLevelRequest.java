package com.buurman.dto.request.backoffice;

import java.util.Objects;
import java.util.Optional;

public record SetLogLevelRequest(Optional<String> level) {
  public SetLogLevelRequest {
    level = Objects.requireNonNullElse(level, Optional.empty());
  }
}
