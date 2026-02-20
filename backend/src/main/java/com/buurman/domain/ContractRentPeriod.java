package com.buurman.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
  private LocalDate effectiveTo;
  private String notes;
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  private Instant deletedAt;
}
