package com.buurman.dto.response;

import com.buurman.domain.Contract;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContractSummary(
        String identifier,
        PropertySummary property,
        TenantSummary primaryTenant,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal rentAmount,
        Contract.ContractStatus status
) {
}
