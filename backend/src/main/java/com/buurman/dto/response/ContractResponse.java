package com.buurman.dto.response;

import com.buurman.domain.Contract;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ContractResponse(
        String identifier,
        PropertySummary property,
        List<ContractPartyResponse> parties,
        TenantSummary primaryTenant,
        Contract.ContractType contractType,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate signedDate,
        BigDecimal rentAmount,
        BigDecimal depositAmount,
        BigDecimal securityDeposit,
        String currency,
        Contract.PaymentFrequency paymentFrequency,
        Integer paymentDueDay,
        Boolean autoRenewal,
        Integer renewalNoticeDays,
        Integer terminationNoticeDays,
        BigDecimal lateFeePercentage,
        Contract.ContractStatus status,
        String termsAndConditions,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
}
