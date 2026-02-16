package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.buurman.domain.Contract;

public record ContractSummary(
    String identifier,
    PropertySummary property,
    TenantSummary primaryTenant,
    LocalDate startDate,
    LocalDate endDate,
    BigDecimal rentAmount,
    Contract.ContractStatus status) {}
