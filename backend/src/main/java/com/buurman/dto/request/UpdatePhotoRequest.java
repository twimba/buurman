package com.buurman.dto.request;

import java.util.Objects;
import java.util.Optional;

public record UpdatePhotoRequest(Optional<String> title, Optional<String> notes) {
  public UpdatePhotoRequest {
    title = Objects.requireNonNullElse(title, Optional.empty());
    notes = Objects.requireNonNullElse(notes, Optional.empty());
  }
}
