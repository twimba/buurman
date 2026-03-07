package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.buurman.domain.Sid;
import com.buurman.dto.request.WwsCalculationRequest;

public record WwsCalculationResponse(
    Optional<Sid> identifier,
    BigDecimal totalPoints,
    String sectorClassification,
    Optional<BigDecimal> maxRentIndication,
    String systemVersion,
    LocalDate calculationDate,
    List<WwsCategoryBreakdown> breakdown,
    Optional<WwsCalculationRequest> inputData) {}
