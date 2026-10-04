package com.buurman.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@SuppressWarnings("NullAway.Init")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedContractFilter {

  private UUID id;
  @Builder.Default private Optional<Sid> identifier = Optional.empty();
  private UUID teamId;
  // Deliberately no separate createdBy/updatedBy: a personal filter preset is only ever
  // created or edited by the one user it belongs to, so userId already is the audit trail.
  private UUID userId;
  private String name;
  private Map<String, Object> criteria;
  private Instant createdAt;
  private Instant updatedAt;
}
