package com.buurman.domain;

import java.util.Optional;

public record EvaluatedFlag(boolean enabled, Optional<String> value) {

  public static EvaluatedFlag disabled() {
    return new EvaluatedFlag(false, Optional.empty());
  }
}
