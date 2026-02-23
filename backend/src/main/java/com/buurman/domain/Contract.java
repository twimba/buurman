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
public class Contract {

  public enum ContractType {
    FIXED_TERM,
    INDEFINITE,
    FURNISHED,
    UNFURNISHED
  }

  public enum PaymentFrequency {
    MONTHLY,
    QUARTERLY,
    ANNUALLY
  }

  public enum ContractStatus {
    DRAFT,
    ACTIVE,
    EXPIRED,
    TERMINATED,
    PENDING_SIGNATURE
  }

  private UUID id;
  private String identifier;
  private UUID teamId;
  private UUID propertyId;
  private ContractType contractType;
  private LocalDate startDate;
  @Builder.Default private Optional<LocalDate> endDate = Optional.empty();
  @Builder.Default private Optional<LocalDate> signedDate = Optional.empty();
  private BigDecimal rentAmount;
  @Builder.Default private Optional<BigDecimal> depositAmount = Optional.empty();
  @Builder.Default private Optional<BigDecimal> securityDeposit = Optional.empty();
  private String rentAmountCurrency;
  @Builder.Default private Optional<String> depositAmountCurrency = Optional.empty();
  @Builder.Default private Optional<String> securityDepositCurrency = Optional.empty();
  private PaymentFrequency paymentFrequency;
  @Builder.Default private Optional<Integer> paymentDueDay = Optional.empty();
  @Builder.Default private Boolean autoRenewal = false;
  @Builder.Default private Integer renewalNoticeDays = 30;
  @Builder.Default private Integer terminationNoticeDays = 30;
  @Builder.Default private Optional<BigDecimal> lateFeePercentage = Optional.empty();
  private ContractStatus status;
  @Builder.Default private Optional<String> termsAndConditions = Optional.empty();
  @Builder.Default private Optional<String> notes = Optional.empty();
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  @Builder.Default private Optional<Instant> deletedAt = Optional.empty();
}
