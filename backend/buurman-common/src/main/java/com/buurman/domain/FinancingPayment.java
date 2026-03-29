package com.buurman.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import com.buurman.util.MoneyAmount;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@SuppressWarnings("NullAway.Init")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancingPayment {

  public enum PaymentStatus {
    SCHEDULED,
    COMPLETED,
    MISSED,
    LATE
  }

  private UUID id;
  @Builder.Default private Optional<Sid> identifier = Optional.empty();
  private UUID financingId;
  private UUID teamId;
  private LocalDate paymentDate;
  private MoneyAmount totalAmount;
  @Builder.Default private Optional<BigDecimal> principalAmount = Optional.empty();
  @Builder.Default private Optional<BigDecimal> interestAmount = Optional.empty();
  @Builder.Default private Optional<BigDecimal> escrowAmount = Optional.empty();
  @Builder.Default private Optional<BigDecimal> extraPayment = Optional.empty();
  private PaymentStatus status;
  @Builder.Default private Optional<String> notes = Optional.empty();
  private boolean balanceDeducted;
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  @Builder.Default private Optional<Instant> deletedAt = Optional.empty();
}
