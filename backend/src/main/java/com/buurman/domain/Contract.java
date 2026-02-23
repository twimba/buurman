package com.buurman.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

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
  private @Nullable LocalDate endDate;
  private @Nullable LocalDate signedDate;
  private BigDecimal rentAmount;
  private BigDecimal depositAmount;
  private BigDecimal securityDeposit;
  private String rentAmountCurrency;
  private String depositAmountCurrency;
  private String securityDepositCurrency;
  private PaymentFrequency paymentFrequency;
  private @Nullable Integer paymentDueDay;
  @Builder.Default private Boolean autoRenewal = false;
  @Builder.Default private Integer renewalNoticeDays = 30;
  @Builder.Default private Integer terminationNoticeDays = 30;
  private @Nullable BigDecimal lateFeePercentage;
  private ContractStatus status;
  private String termsAndConditions;
  private @Nullable String notes;
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  private @Nullable Instant deletedAt;
}
