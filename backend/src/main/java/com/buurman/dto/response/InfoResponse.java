package com.buurman.dto.response;

import java.time.Instant;

public record InfoResponse(
    String version, String environment, Instant buildTime, DatabaseInfo database) {
  public record DatabaseInfo(
      String currentVersion,
      String description,
      Instant installedOn,
      int migrationsApplied,
      int migrationsPending) {}
}
