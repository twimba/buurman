package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.Contract;

public record ContractSummary(
    String identifier,
    PropertySummary property,
    Optional<TenantSummary> primaryTenant,
    LocalDate startDate,
    Optional<LocalDate> endDate,
    BigDecimal rentAmount,
    Contract.ContractStatus status) {}
