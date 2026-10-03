package com.buurman.domain;

import java.time.Instant;
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
public class LeaseClauseTemplate {

  private UUID id;
  @Builder.Default private Optional<Sid> identifier = Optional.empty();
  private String countryCode;
  @Builder.Default private LeaseKind leaseKind = LeaseKind.RESIDENTIAL;
  private String clauseKey;
  private String titleI18nKey;
  private String bodyI18nKey;
  private boolean defaultIncluded;
  private boolean optional;
  private boolean pinned;
  private int sortOrder;
  private int version;
  private Instant createdAt;
  private Instant updatedAt;
  @Builder.Default private Optional<Instant> deletedAt = Optional.empty();
  private UUID createdBy;
  private UUID updatedBy;
}
