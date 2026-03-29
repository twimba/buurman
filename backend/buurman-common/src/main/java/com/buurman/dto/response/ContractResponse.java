package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.buurman.domain.Contract;
import com.buurman.domain.ContractExtension;
import com.buurman.domain.Sid;
import com.buurman.domain.metadata.ContractCountryMetadata;
import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record ContractResponse(
    Sid identifier,
    Optional<PropertySummary> property,
    List<ContractPartyResponse> parties,
    Optional<ContactSummary> primaryContact,
    Contract.ContractType contractType,
    LocalDate startDate,
    Optional<LocalDate> endDate,
    Optional<LocalDate> signedDate,
    BigDecimal rentAmount,
    Optional<BigDecimal> depositAmount,
    Optional<BigDecimal> securityDeposit,
    String rentAmountCurrency,
    Optional<String> depositAmountCurrency,
    Optional<String> securityDepositCurrency,
    Contract.PaymentFrequency paymentFrequency,
    Optional<Integer> paymentDueDay,
    Integer terminationNoticeDays,
    Optional<BigDecimal> lateFeePercentage,
    Contract.ContractStatus status,
    Optional<String> termsAndConditions,
    Optional<String> notes,
    Optional<String> countryCode,
    Optional<ContractCountryMetadata> countryMetadata,
    // Renewal configuration
    Contract.RenewalMode renewalMode,
    Optional<Integer> renewalTermMonths,
    Optional<Integer> maxRenewals,
    Integer landlordNoticeDays,
    Integer tenantNoticeDays,
    Boolean requiresTenantConfirmation,
    ContractExtension.RentAdjustmentType rentAdjustmentType,
    Optional<BigDecimal> rentAdjustmentValue,
    Optional<Contract.LandlordType> landlordType,
    Optional<String> regionCode,
    List<String> documentLanguages,
    // Effective end date (computed from extensions)
    Optional<LocalDate> effectiveEndDate,
    // Extension statistics
    int extensionCount,
    Optional<Integer> extensionsRemaining,
    // Rent components breakdown
    List<RentComponentResponse> rentComponents,
    Instant createdAt,
    Optional<Instant> updatedAt) {}
