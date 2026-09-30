package com.buurman.dto.request;

import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.TerminationGivenBy;

public record TerminateContractRequest(
    TerminationGivenBy givenBy,
    LocalDate noticeDate,
    Optional<String> groundCode,
    Optional<LocalDate> effectiveEndDate,
    Optional<String> overrideReason,
    Optional<LocalDate> inspectionDate) {}
