package com.buurman.util;

import java.util.UUID;

public class Constants {

  private Constants() {
    // Private constructor to prevent instantiation
  }

  /**
   * System user ID for automated operations (scheduler, background jobs) Used for
   * createdBy/updatedBy fields when no user context exists
   */
  public static final UUID SYSTEM_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
}
