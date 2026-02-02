package com.buurman.dto.response;

import com.buurman.domain.Contract;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ContractSummary(
        UUID id,
        String identifier,
        PropertySummary property,
        TenantSummary tenant,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal rentAmount,
        Contract.ContractStatus status
) {
}
