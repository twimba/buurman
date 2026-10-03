package com.buurman.dto.response;

import java.util.List;
import java.util.Optional;

import com.buurman.domain.LeaseAvailability;

public record LeaseClausesResponse(
    LeaseAvailability availability,
    Optional<String> countryCode,
    List<ResolvedLeaseClauseResponse> clauses) {}
