package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.buurman.domain.Contract;
import com.buurman.domain.Ulid;
import com.buurman.domain.metadata.ContractCountryMetadata;

public record ContractResponse(
    Ulid identifier,
    Optional<PropertySummary> property,
    List<ContractPartyResponse> parties,
    Optional<TenantSummary> primaryTenant,
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
    Boolean autoRenewal,
    Integer renewalNoticeDays,
    Integer terminationNoticeDays,
    Optional<BigDecimal> lateFeePercentage,
    Contract.ContractStatus status,
    Optional<String> termsAndConditions,
    Optional<String> notes,
    Optional<String> countryCode,
    Optional<ContractCountryMetadata> countryMetadata,
    Instant createdAt,
    Optional<Instant> updatedAt) {}
