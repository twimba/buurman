package com.buurman.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import com.buurman.domain.metadata.ContractCountryMetadata;
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
public class Contract {

  public enum ContractType {
    FIXED_TERM,
    INDEFINITE
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

  public enum RenewalMode {
    NONE,
    AUTOMATIC,
    MANUAL
  }

  public enum LandlordType {
    NATURAL_PERSON,
    LEGAL_ENTITY
  }

  private UUID id;
  @Builder.Default private Optional<Sid> identifier = Optional.empty();
  private UUID teamId;
  private UUID propertyId;
  private ContractType contractType;
  private LocalDate startDate;
  @Builder.Default private Optional<LocalDate> endDate = Optional.empty();
  @Builder.Default private Optional<LocalDate> signedDate = Optional.empty();
  private MoneyAmount rentAmount;
  @Builder.Default private Optional<MoneyAmount> depositAmount = Optional.empty();
  @Builder.Default private Optional<MoneyAmount> securityDeposit = Optional.empty();
  private PaymentFrequency paymentFrequency;
  @Builder.Default private Optional<Integer> paymentDueDay = Optional.empty();
  @Builder.Default private Integer terminationNoticeDays = 30;
  @Builder.Default private Optional<BigDecimal> lateFeePercentage = Optional.empty();
  private ContractStatus status;
  @Builder.Default private Optional<String> termsAndConditions = Optional.empty();
  @Builder.Default private Optional<String> notes = Optional.empty();
  @Builder.Default private Optional<String> countryCode = Optional.empty();
  @Builder.Default private Optional<ContractCountryMetadata> countryMetadata = Optional.empty();
  @Builder.Default private RenewalMode renewalMode = RenewalMode.NONE;
  @Builder.Default private Optional<Integer> renewalTermMonths = Optional.empty();
  @Builder.Default private Optional<Integer> maxRenewals = Optional.empty();
  @Builder.Default private Integer landlordNoticeDays = 30;
  @Builder.Default private Integer tenantNoticeDays = 30;
  @Builder.Default private Boolean requiresTenantConfirmation = false;

  @Builder.Default
  private ContractExtension.RentAdjustmentType rentAdjustmentType =
      ContractExtension.RentAdjustmentType.NONE;

  @Builder.Default private Optional<BigDecimal> rentAdjustmentValue = Optional.empty();
  @Builder.Default private Optional<LandlordType> landlordType = Optional.empty();
  @Builder.Default private Optional<String> regionCode = Optional.empty();
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  @Builder.Default private Optional<Instant> deletedAt = Optional.empty();
}
