package com.buurman.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.buurman.domain.ContactTag;
import com.buurman.domain.ContactType;
import com.buurman.domain.DataRetentionStatus;
import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record ContactResponse(
    Sid identifier,
    ContactType contactType,
    String displayName,
    Optional<String> firstName,
    Optional<String> lastName,
    Optional<String> companyName,
    Optional<String> tradeName,
    Optional<String> industry,
    Optional<String> email,
    Optional<String> invoiceEmail,
    Optional<String> phone,
    Optional<String> website,
    Optional<String> taxNumber,
    Optional<String> idNumber,
    Optional<LocalDate> dateOfBirth,
    Optional<LocalDate> idExpiryDate,
    Optional<String> notes,
    Optional<String> mainPhotoUrl,
    Optional<String> mainPhotoThumbnailUrl,
    List<ContactTag> tags,
    DataRetentionStatus dataRetentionStatus,
    List<ContactPropertyAssignment> activeProperties,
    Optional<ContactBalanceSummary> balanceSummary,
    Instant createdAt,
    Optional<Instant> updatedAt) {}
