package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

import com.buurman.domain.Contract;

public record ContractSummary(
    String identifier,
    PropertySummary property,
    @Nullable TenantSummary primaryTenant,
    LocalDate startDate,
    @Nullable LocalDate endDate,
    BigDecimal rentAmount,
    Contract.ContractStatus status) {}
