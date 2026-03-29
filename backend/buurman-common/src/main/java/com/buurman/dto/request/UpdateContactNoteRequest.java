package com.buurman.dto.request;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.InteractionType;
import com.buurman.util.SkipTestCoverage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@SkipTestCoverage
public record UpdateContactNoteRequest(
    @NotNull(message = "Interaction type is required") InteractionType interactionType,
    Optional<String> subject,
    @NotBlank(message = "Body is required") String body,
    @NotNull(message = "Occurred at is required") Instant occurredAt,
    Optional<LocalDate> followUpDate) {}
