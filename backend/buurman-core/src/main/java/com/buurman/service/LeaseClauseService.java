package com.buurman.service;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.Contract;
import com.buurman.domain.ContractLeaseClause;
import com.buurman.domain.LeaseClauseTemplate;
import com.buurman.domain.LeaseKind;
import com.buurman.domain.Property;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.dto.request.UpdateContractLeaseClausesRequest;
import com.buurman.dto.response.LeaseClausesResponse;
import com.buurman.dto.response.ResolvedLeaseClauseResponse;
import com.buurman.exception.BadRequestException;
import com.buurman.exception.LeaseNotAvailableException;
import com.buurman.repository.ContractLeaseClauseRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.util.DocumentLanguages;

import lombok.RequiredArgsConstructor;

/**
 * Contract-facing clause toggle: reads the resolved clause list for a contract and lets a landlord
 * change which clauses apply, subject to server-side enforcement that a non-optional clause can
 * never be excluded — that check happens before any write, not merely before the response.
 */
@Service
@RequiredArgsConstructor
public class LeaseClauseService {

  private final ContractRepository contractRepository;
  private final ContractLeaseClauseRepository overrideRepository;
  private final LeaseClauseResolver resolver;
  private final PropertyRepository propertyRepository;
  private final LeaseKindResolver leaseKindResolver;

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR', 'TEAM_VIEWER')")
  public LeaseClausesResponse getClauses(
      ContractIdentifier contractIdentifier, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Contract contract = contractRepository.getByIdentifierAndTeamId(contractIdentifier, teamId);
    LeaseClauseResolver.Availability availability =
        resolver.availabilityFor(contract, leaseKind(contract, teamId));
    return envelope(contract, availability);
  }

  private LeaseClausesResponse envelope(
      Contract contract, LeaseClauseResolver.Availability availability) {
    List<ResolvedLeaseClauseResponse> clauses =
        availability.state().isAvailable()
            ? resolver.resolve(contract, contractLocale(contract), availability.templates())
            : List.of();
    return new LeaseClausesResponse(availability.state(), contract.getCountryCode(), clauses);
  }

  // ContractLeaseClauseRepository.replaceForContract() hard-deletes every existing override and
  // re-inserts the new set row by row — without a surrounding transaction, a failure partway
  // through the inserts would leave the contract with zero overrides (silently reverting every
  // clause to its template default) rather than either the old or the new set intact.
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  @Transactional
  public LeaseClausesResponse updateClauses(
      ContractIdentifier contractIdentifier,
      UpdateContractLeaseClausesRequest request,
      UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Contract contract = contractRepository.getByIdentifierAndTeamId(contractIdentifier, teamId);
    if (contract.getCountryCode().isEmpty()) {
      throw LeaseNotAvailableException.noCountry();
    }

    // A duplicate templateIdentifier would otherwise reach the repository and hit
    // uq_contract_lease_clauses_contract_template as a raw constraint-violation 500. Checked
    // before any write, before templates are even resolved.
    Set<String> seenTemplateIdentifiers = new HashSet<>();
    for (UpdateContractLeaseClausesRequest.ClauseSelection selection : request.clauses()) {
      if (!seenTemplateIdentifiers.add(selection.templateIdentifier())) {
        throw new BadRequestException(
            "Duplicate clause template in request: " + selection.templateIdentifier());
      }
    }

    LeaseClauseResolver.Availability availability =
        resolver.availabilityFor(contract, leaseKind(contract, teamId));
    if (!availability.state().isAvailable()) {
      throw LeaseNotAvailableException.forCountry(contract.getCountryCode().orElse("?"));
    }
    List<LeaseClauseTemplate> templates = availability.templates();

    List<ContractLeaseClause> toSave =
        request.clauses().stream().map(selection -> toClause(selection, templates)).toList();

    overrideRepository.replaceForContract(contract.getId(), teamId, principal.getUserId(), toSave);
    return envelope(contract, availability);
  }

  private ContractLeaseClause toClause(
      UpdateContractLeaseClausesRequest.ClauseSelection selection,
      List<LeaseClauseTemplate> templates) {
    LeaseClauseTemplate template =
        templates.stream()
            .filter(
                t ->
                    t.getIdentifier()
                        .map(id -> id.value().equals(selection.templateIdentifier()))
                        .orElse(false))
            .findFirst()
            .orElseThrow(
                () ->
                    new BadRequestException(
                        "Unknown clause template: " + selection.templateIdentifier()));

    // Server-side enforcement — a non-optional clause cannot be excluded, regardless of what the
    // client sends. This is checked before any write: a UI-only checkbox-disable is not
    // sufficient for a legal document.
    if (!template.isOptional() && !selection.included()) {
      throw new BadRequestException(
          "Clause '" + template.getClauseKey() + "' is required and cannot be excluded");
    }

    return ContractLeaseClause.builder()
        .clauseTemplateId(template.getId())
        .included(selection.included())
        .sortOrder(selection.sortOrder())
        .build();
  }

  private LeaseKind leaseKind(Contract contract, UUID teamId) {
    Property property = propertyRepository.getByIdAndTeamId(contract.getPropertyId(), teamId);
    return leaseKindResolver.resolveFor(contract, property, teamId);
  }

  private Locale contractLocale(Contract contract) {
    return Locale.forLanguageTag(
        DocumentLanguages.firstSupportedOrDefault(contract.getDocumentLanguages()));
  }
}
