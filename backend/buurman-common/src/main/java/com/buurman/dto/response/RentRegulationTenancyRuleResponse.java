package com.buurman.dto.response;

import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.Sid;
import com.buurman.domain.TenancyRuleTopic;
import com.buurman.util.SkipTestCoverage;

/** A display-only tenancy-law reference entry for a regulation country. */
@SkipTestCoverage
public record RentRegulationTenancyRuleResponse(
    Sid identifier,
    TenancyRuleTopic topic,
    Optional<String> regionCode,
    String label,
    String value,
    Optional<LocalDate> effectiveFrom,
    Optional<String> legalBasis,
    Optional<String> sourceUrl,
    Optional<String> notes) {}
