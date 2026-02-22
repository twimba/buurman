package com.buurman.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@SuppressWarnings("NullAway.Init")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractRentPeriod {
  private UUID id;
  private String identifier;
  private UUID teamId;
  private UUID contractId;
  private BigDecimal rentAmount;
  private String currency;
  private LocalDate effectiveFrom;
  private @Nullable LocalDate effectiveTo;
  private @Nullable String notes;
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  private @Nullable Instant deletedAt;
}
