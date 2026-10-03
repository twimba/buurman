package com.buurman.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import com.buurman.domain.Contract;
import com.buurman.domain.ContractLeaseClause;
import com.buurman.domain.LeaseAvailability;
import com.buurman.domain.LeaseClauseTemplate;
import com.buurman.domain.LeaseKind;
import com.buurman.dto.response.ResolvedLeaseClauseResponse;
import com.buurman.exception.LeaseNotAvailableException;
import com.buurman.repository.ContractLeaseClauseRepository;
import com.buurman.repository.LeaseClauseTemplateRepository;

/**
 * Single source of truth for "what clauses apply to this contract right now" — combines
 * country-keyed default templates with per-contract overrides, resolved to the contract's locale.
 * Used by both letter generation and the clause-toggle UI so they never disagree.
 */
@Service
public class LeaseClauseResolver {

  private final LeaseClauseTemplateRepository templateRepository;
  private final ContractLeaseClauseRepository overrideRepository;
  private final MessageSource messageSource;

  public LeaseClauseResolver(
      LeaseClauseTemplateRepository templateRepository,
      ContractLeaseClauseRepository overrideRepository,
      @Qualifier("letterMessageSource") MessageSource messageSource) {
    this.templateRepository = templateRepository;
    this.overrideRepository = overrideRepository;
    this.messageSource = messageSource;
  }

  /** Availability state plus the templates it was derived from (empty when unavailable). */
  public record Availability(LeaseAvailability state, List<LeaseClauseTemplate> templates) {}

  /**
   * Non-throwing availability check: walks {@link LeaseKind#fallbackChain()} (e.g. furnished ->
   * residential -> legacy) until a kind has templates configured. Templates of kind LEGACY are
   * placeholder example text rather than a reviewed document.
   */
  public Availability availabilityFor(Contract contract, LeaseKind kind) {
    return contract
        .getCountryCode()
        .map(
            countryCode ->
                kind.fallbackChain().stream()
                    .map(k -> templateRepository.findByCountryAndKind(countryCode, k))
                    .filter(templates -> !templates.isEmpty())
                    .findFirst()
                    .map(
                        templates ->
                            new Availability(
                                templates.stream()
                                        .allMatch(t -> t.getLeaseKind() == LeaseKind.LEGACY)
                                    ? LeaseAvailability.AVAILABLE_EXAMPLE_TEXT
                                    : LeaseAvailability.AVAILABLE_DOCUMENT,
                                templates))
                    .orElseGet(
                        () -> new Availability(LeaseAvailability.UNAVAILABLE_COUNTRY, List.of())))
        .orElseGet(() -> new Availability(LeaseAvailability.UNAVAILABLE_NO_COUNTRY, List.of()));
  }

  /**
   * Templates for the contract's country and kind.
   *
   * @throws LeaseNotAvailableException when the contract has no country or the country has none
   */
  public List<LeaseClauseTemplate> templatesFor(Contract contract, LeaseKind kind) {
    Availability availability = availabilityFor(contract, kind);
    if (availability.state() == LeaseAvailability.UNAVAILABLE_NO_COUNTRY) {
      throw LeaseNotAvailableException.noCountry();
    }
    if (availability.state() == LeaseAvailability.UNAVAILABLE_COUNTRY) {
      throw LeaseNotAvailableException.forCountry(contract.getCountryCode().orElse("?"));
    }
    return availability.templates();
  }

  public List<ResolvedLeaseClauseResponse> resolve(
      Contract contract, Locale locale, LeaseKind kind) {
    return resolve(contract, locale, templatesFor(contract, kind));
  }

  /**
   * Same resolution, but using a template list the caller already fetched (e.g. {@code
   * LeaseClauseService#updateClauses}, which needs the same country's templates to validate the
   * request before calling this) instead of fetching it again here.
   */
  public List<ResolvedLeaseClauseResponse> resolve(
      Contract contract, Locale locale, List<LeaseClauseTemplate> templates) {
    if (templates.isEmpty()) {
      throw contract
          .getCountryCode()
          .map(LeaseNotAvailableException::forCountry)
          .orElseGet(LeaseNotAvailableException::noCountry);
    }

    Map<UUID, ContractLeaseClause> overridesByTemplateId =
        overrideRepository
            .findByContractIdAndTeamId(contract.getId(), contract.getTeamId())
            .stream()
            .collect(
                Collectors.toMap(ContractLeaseClause::getClauseTemplateId, Function.identity()));

    List<Resolved> ordered =
        templates.stream()
            .map(
                t -> {
                  ContractLeaseClause override = overridesByTemplateId.get(t.getId());
                  boolean included =
                      override != null ? override.isIncluded() : t.isDefaultIncluded();
                  // Required clauses are always included, regardless of a stored override or the
                  // template's default — this is the single source of truth enforcing that a
                  // non-optional clause can never be excluded from the generated document, even
                  // if a template was changed from optional to required after overrides existed.
                  included = included || !t.isOptional();
                  // A pinned clause keeps its template position; any override sortOrder is ignored.
                  int sortOrder =
                      override != null && !t.isPinned()
                          ? override.getSortOrder()
                          : t.getSortOrder();
                  return new Resolved(t, included, sortOrder);
                })
            .sorted(
                Comparator.comparing((Resolved r) -> !r.template().isPinned())
                    .thenComparingInt(Resolved::sortOrder)
                    .thenComparingInt(r -> r.template().getSortOrder()))
            .toList();

    List<ResolvedLeaseClauseResponse> result = new ArrayList<>();
    int article = 0;
    for (Resolved r : ordered) {
      LeaseClauseTemplate t = r.template();
      if (r.included()) {
        article++;
      }
      result.add(
          new ResolvedLeaseClauseResponse(
              t.getIdentifier().orElseThrow(),
              t.getClauseKey(),
              messageSource.getMessage(t.getTitleI18nKey(), null, locale),
              messageSource.getMessage(t.getBodyI18nKey(), null, locale),
              r.included(),
              t.isOptional(),
              r.sortOrder(),
              t.isPinned(),
              r.included() ? article : 0));
    }
    return List.copyOf(result);
  }

  private record Resolved(LeaseClauseTemplate template, boolean included, int sortOrder) {}
}
