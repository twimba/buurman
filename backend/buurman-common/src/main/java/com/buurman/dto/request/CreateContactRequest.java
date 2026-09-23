package com.buurman.dto.request;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.buurman.domain.ContactTag;
import com.buurman.domain.ContactType;
import com.buurman.util.SkipTestCoverage;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@SkipTestCoverage
public record CreateContactRequest(
    @NotNull(message = "Contact type is required") ContactType contactType,
    Optional<@Size(max = 255) String> firstName,
    Optional<String> lastName,
    Optional<String> companyName,
    Optional<String> tradeName,
    Optional<String> industry,
    Optional<@Email(message = "Email must be valid") String> email,
    Optional<@Email(message = "Invoice email must be valid") String> invoiceEmail,
    Optional<
            @Pattern(
                regexp = "^\\+[1-9]\\d{1,14}$",
                message = "Phone must be in E.164 format (e.g. +31612345678)")
            String>
        phone,
    Optional<String> website,
    Optional<String> taxNumber,
    Optional<String> idNumber,
    Optional<LocalDate> dateOfBirth,
    Optional<LocalDate> idExpiryDate,
    Optional<String> notes,
    Optional<List<ContactTag>> tags,
    Optional<Boolean> paymentRemindersEnabled) {}
