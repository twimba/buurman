package com.buurman.dto.request;

import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.TerminationGivenBy;

import jakarta.validation.constraints.NotNull;

public record TerminateContractRequest(
    @NotNull(message = "givenBy is required") TerminationGivenBy givenBy,
    @NotNull(message = "noticeDate is required") LocalDate noticeDate,
    Optional<String> groundCode,
    Optional<LocalDate> effectiveEndDate,
    Optional<String> overrideReason,
    Optional<LocalDate> inspectionDate) {}
