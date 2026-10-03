package com.buurman.service.backoffice;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.buurman.domain.LeaseClauseTemplate;
import com.buurman.domain.LeaseKind;
import com.buurman.domain.Sid;
import com.buurman.dto.request.backoffice.UpsertLeaseClauseTemplateRequest;
import com.buurman.dto.response.LeaseClauseTemplateResponse;
import com.buurman.exception.BadRequestException;
import com.buurman.repository.LeaseClauseTemplateRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BackofficeLeaseClauseTemplateService {

  private final LeaseClauseTemplateRepository repository;

  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public List<LeaseClauseTemplateResponse> list(String countryCode, Optional<LeaseKind> kind) {
    List<LeaseClauseTemplate> templates =
        kind.map(k -> repository.findByCountryAndKind(countryCode, k))
            .orElseGet(() -> repository.findByCountryCode(countryCode));
    return templates.stream().map(this::toResponse).toList();
  }

  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public LeaseClauseTemplateResponse create(
      UpsertLeaseClauseTemplateRequest request, UUID actorId) {
    rejectInvalidClauseKey(request.clauseKey());
    // No default kind: a RESIDENTIAL default would make a lone new row shadow a country's whole
    // LEGACY set (the resolver only falls back when a kind has no rows at all).
    LeaseKind leaseKind =
        Optional.ofNullable(request.leaseKind())
            .orElseThrow(() -> new BadRequestException("leaseKind is required"));
    rejectRequiredButExcludedByDefault(request);
    LeaseClauseTemplate saved =
        repository.save(
            LeaseClauseTemplate.builder()
                .countryCode(request.countryCode())
                .leaseKind(leaseKind)
                .clauseKey(request.clauseKey())
                .titleI18nKey(request.titleI18nKey())
                .bodyI18nKey(request.bodyI18nKey())
                .defaultIncluded(request.defaultIncluded())
                .optional(request.optional())
                .pinned(request.pinned())
                .sortOrder(request.sortOrder())
                .version(1)
                .createdBy(actorId)
                .updatedBy(actorId)
                .build());
    return toResponse(saved);
  }

  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public LeaseClauseTemplateResponse update(
      Sid identifier, UpsertLeaseClauseTemplateRequest request, UUID actorId) {
    rejectRequiredButExcludedByDefault(request);
    LeaseClauseTemplate existing = repository.getByIdentifier(identifier);
    Optional.ofNullable(request.leaseKind())
        .filter(kind -> kind != existing.getLeaseKind())
        .ifPresent(
            kind -> {
              throw new BadRequestException("Lease kind of an existing template cannot be changed");
            });
    existing.setTitleI18nKey(request.titleI18nKey());
    existing.setBodyI18nKey(request.bodyI18nKey());
    existing.setDefaultIncluded(request.defaultIncluded());
    existing.setOptional(request.optional());
    existing.setPinned(request.pinned());
    existing.setSortOrder(request.sortOrder());
    existing.setUpdatedBy(actorId);
    // Bumped on every content change so a stale cached resolution (keyed by version) is
    // detectable, and so the admin UI can show "this clause has been edited since X".
    existing.setVersion(existing.getVersion() + 1);
    return toResponse(repository.save(existing));
  }

  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public void delete(Sid identifier, UUID actorId) {
    repository.softDeleteByIdentifier(identifier);
  }

  // The clause key selects a "clause-{key}" fragment in the lease documents, so it must be a safe
  // slug. Existing seeded keys (e.g. termination-reference) all match.
  private void rejectInvalidClauseKey(String clauseKey) {
    if (clauseKey == null || !LeaseClauseTemplate.CLAUSE_KEY_PATTERN.matcher(clauseKey).matches()) {
      throw new BadRequestException("Clause key must match ^[a-z0-9-]{1,64}$");
    }
  }

  // A required (non-optional) clause that defaults to excluded is a nonsensical, dangerous
  // combination: any contract without an explicit override would silently produce a lease
  // missing a clause the template says must always be present.
  private void rejectRequiredButExcludedByDefault(UpsertLeaseClauseTemplateRequest request) {
    if (!request.optional() && !request.defaultIncluded()) {
      throw new BadRequestException(
          "A required clause template cannot default to excluded (optional=false requires"
              + " defaultIncluded=true)");
    }
  }

  private LeaseClauseTemplateResponse toResponse(LeaseClauseTemplate t) {
    return new LeaseClauseTemplateResponse(
        t.getIdentifier().orElseThrow(),
        t.getCountryCode(),
        t.getLeaseKind(),
        t.getClauseKey(),
        t.getTitleI18nKey(),
        t.getBodyI18nKey(),
        t.isDefaultIncluded(),
        t.isOptional(),
        t.isPinned(),
        t.getSortOrder(),
        t.getVersion());
  }
}
