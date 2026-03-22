package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.Contract;
import com.buurman.domain.Sid;
import com.buurman.util.Generated;

@Generated
public record ContractSummary(
    Sid identifier,
    PropertySummary property,
    Optional<ContactSummary> primaryContact,
    LocalDate startDate,
    Optional<LocalDate> endDate,
    BigDecimal rentAmount,
    Contract.ContractStatus status) {}
