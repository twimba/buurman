package com.buurman.dto.response;

import java.util.List;
import java.util.Optional;

import com.buurman.domain.LeaseAvailability;

/**
 * Lease clause catalog for a contract.
 *
 * @param documentLanguages languages a lease agreement can be generated in for the contract's
 *     country and kind, the country's authoritative language first; empty when unavailable
 */
public record LeaseClausesResponse(
    LeaseAvailability availability,
    Optional<String> countryCode,
    List<ResolvedLeaseClauseResponse> clauses,
    List<String> documentLanguages) {}
