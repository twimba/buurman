package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.Contract;
import com.buurman.domain.Sid;
import com.buurman.util.Generated;

@Generated
public record UpcomingRenewalResponse(
    Sid contractIdentifier,
    Optional<String> propertyName,
    Optional<String> contactName,
    LocalDate effectiveEndDate,
    Contract.RenewalMode renewalMode,
    Optional<Integer> renewalTermMonths,
    BigDecimal currentRentAmount,
    String currency,
    int daysUntilExpiry) {}
