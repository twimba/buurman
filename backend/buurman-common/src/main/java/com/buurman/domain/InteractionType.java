package com.buurman.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InteractionType {
  PHONE_CALL("Phone Call"),
  MEETING("Meeting"),
  VIEWING("Viewing"),
  KEY_HANDOVER("Key Handover"),
  INSPECTION("Inspection"),
  NOTE("Note"),
  OTHER("Other");

  private final String displayName;
}
