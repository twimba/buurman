package com.buurman.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

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

  /**
   * A clause key selects a {@code clause-<key>} template fragment, so only safe slugs are valid.
   */
  public static final Pattern CLAUSE_KEY_PATTERN = Pattern.compile("^[a-z0-9-]{1,64}$");

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
