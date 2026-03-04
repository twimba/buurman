package com.buurman.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
public class PropertyFinancing {

  public enum FinancingType {
    MORTGAGE,
    LEASING,
    LOAN,
    LINE_OF_CREDIT,
    PRIVATE_FINANCING,
    OTHER
  }

  public enum RateType {
    FIXED,
    VARIABLE,
    INTEREST_ONLY,
    HYBRID
  }

  public enum FinancingStatus {
    ACTIVE,
    PAID_OFF,
    REFINANCED,
    DEFAULTED
  }

  private UUID id;
  @Builder.Default private Optional<Sid> identifier = Optional.empty();
  private UUID propertyId;
  private UUID teamId;
  private FinancingType financingType;
  private RateType rateType;
  @Builder.Default private Optional<String> lenderName = Optional.empty();
  @Builder.Default private Optional<String> loanNumber = Optional.empty();
  private BigDecimal originalAmount;
  private String originalAmountCurrency;
  @Builder.Default private Optional<BigDecimal> currentBalance = Optional.empty();
  @Builder.Default private Optional<String> currentBalanceCurrency = Optional.empty();
  @Builder.Default private Optional<BigDecimal> interestRate = Optional.empty();
  @Builder.Default private Optional<BigDecimal> monthlyPayment = Optional.empty();
  @Builder.Default private Optional<String> monthlyPaymentCurrency = Optional.empty();
  private boolean paymentVariable;
  private LocalDate startDate;
  @Builder.Default private Optional<LocalDate> endDate = Optional.empty();
  @Builder.Default private Optional<Integer> termMonths = Optional.empty();
  private FinancingStatus status;
  @Builder.Default private Optional<String> notes = Optional.empty();
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  @Builder.Default private Optional<Instant> deletedAt = Optional.empty();
}
