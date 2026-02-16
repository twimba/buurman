package com.buurman.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PaymentReceival {

  private UUID id;
  private String identifier;
  private UUID teamId;
  private UUID paymentId;
  private BigDecimal amount;
  private LocalDate receivalDate;
  private String notes;
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  private Instant deletedAt;
}
