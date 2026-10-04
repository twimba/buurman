package com.buurman.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.ContractTerminationStatus;
import com.buurman.domain.Sid;
import com.buurman.domain.TerminationGivenBy;

public record ContractTerminationResponse(
    Sid identifier,
    Sid contractIdentifier,
    TerminationGivenBy givenBy,
    LocalDate noticeDate,
    Optional<String> groundCode,
    LocalDate computedEndDate,
    LocalDate effectiveEndDate,
    Optional<String> overrideReason,
    Optional<LocalDate> inspectionDate,
    Optional<Sid> noticeLetterDocumentIdentifier,
    ContractTerminationStatus status,
    Instant createdAt,
    Instant updatedAt) {}
