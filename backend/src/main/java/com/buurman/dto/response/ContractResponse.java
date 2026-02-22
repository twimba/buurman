package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.buurman.domain.Contract;

public record ContractResponse(
    String identifier,
    @Nullable PropertySummary property,
    List<ContractPartyResponse> parties,
    @Nullable TenantSummary primaryTenant,
    Contract.ContractType contractType,
    LocalDate startDate,
    @Nullable LocalDate endDate,
    @Nullable LocalDate signedDate,
    BigDecimal rentAmount,
    @Nullable BigDecimal depositAmount,
    @Nullable BigDecimal securityDeposit,
    String rentAmountCurrency,
    @Nullable String depositAmountCurrency,
    @Nullable String securityDepositCurrency,
    Contract.PaymentFrequency paymentFrequency,
    @Nullable Integer paymentDueDay,
    @Nullable Boolean autoRenewal,
    @Nullable Integer renewalNoticeDays,
    @Nullable Integer terminationNoticeDays,
    @Nullable BigDecimal lateFeePercentage,
    Contract.ContractStatus status,
    @Nullable String termsAndConditions,
    @Nullable String notes,
    Instant createdAt,
    @Nullable Instant updatedAt) {}
