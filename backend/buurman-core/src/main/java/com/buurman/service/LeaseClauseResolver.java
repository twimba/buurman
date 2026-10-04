package com.buurman.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import com.buurman.domain.Contract;
import com.buurman.domain.LeaseAvailability;
import com.buurman.domain.LeaseClauseTemplate;
import com.buurman.domain.LeaseKind;
import com.buurman.domain.Property;
import com.buurman.domain.metadata.CountryMetadataRegistry;
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

  private static final Pattern ISO_ALPHA2 = Pattern.compile("[A-Z]{2}");

  /**
   * The country that decides lease availability: the contract's own country when set, otherwise the
   * property's (a contract only copies the property's country while it is a draft, so an older
   * contract can lack one the property has since gained). Blank or malformed counts as none.
   * Callers compute it once per request and pass it to {@link #availabilityFor} / {@link
   * #templatesFor}.
   */
  public static Optional<String> effectiveCountryCode(Contract contract, Property property) {
    return contract
        .getCountryCode()
        .flatMap(LeaseClauseResolver::leaseCountry)
        .or(
            () ->
                Optional.ofNullable(property.getCountryCode())
                    .flatMap(LeaseClauseResolver::leaseCountry));
  }

  /**
   * Known countries and names normalise to their ISO code; any other well-formed two-letter code
   * (the property picker offers more countries than the supported list) is kept upper-cased so the
   * state is "no templates for JP" rather than "no country".
   */
  private static Optional<String> leaseCountry(String raw) {
    String trimmed = raw.trim();
    if (trimmed.isEmpty()) {
      return Optional.empty();
    }
    return Optional.ofNullable(CountryMetadataRegistry.normalizeCountryCode(trimmed))
        .filter(c -> !c.isBlank())
        .or(
            () ->
                Optional.of(trimmed.toUpperCase(Locale.ROOT))
                    .filter(c -> ISO_ALPHA2.matcher(c).matches()));
  }

  /** Availability state plus the templates it was derived from (empty when unavailable). */
  public record Availability(LeaseAvailability state, List<LeaseClauseTemplate> templates) {}

  /**
   * Non-throwing availability check: walks {@link LeaseKind#fallbackChain()} (e.g. furnished ->
   * residential -> legacy) until a kind has templates configured. Templates of kind LEGACY are
   * placeholder example text rather than a reviewed document.
   */
  public Availability availabilityFor(Optional<String> country, LeaseKind kind) {
    return country
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
   * Templates for the given effective country and kind.
   *
   * @throws LeaseNotAvailableException when there is no country or the country has no templates
   */
  public List<LeaseClauseTemplate> templatesFor(Optional<String> country, LeaseKind kind) {
    Availability availability = availabilityFor(country, kind);
    if (!availability.state().isAvailable()) {
      throw LeaseNotAvailableException.forContract(country);
    }
    return availability.templates();
  }

  /**
   * Resolves the given templates (already fetched by the caller via {@link #availabilityFor} or
   * {@link #templatesFor}) against the contract's overrides. {@code country} is the effective
   * country the caller already computed; it is only used to report an empty template list.
   */
  public List<ResolvedLeaseClauseResponse> resolve(
      Contract contract,
      Optional<String> country,
      Locale locale,
      List<LeaseClauseTemplate> templates) {
    if (templates.isEmpty()) {
      throw LeaseNotAvailableException.forContract(country);
    }

    List<ClauseOverride> overrides =
        overrideRepository
            .findByContractIdAndTeamId(contract.getId(), contract.getTeamId())
            .stream()
            .map(o -> new ClauseOverride(o.getClauseTemplateId(), o.isIncluded(), o.getSortOrder()))
            .toList();
    return resolve(templates, overrides, country, locale);
  }

  /** A per-contract (or synthetic) choice for one template: inclusion and position. */
  public record ClauseOverride(UUID templateId, boolean included, int sortOrder) {}

  /**
   * Same rules as {@link #resolve(Contract, Optional, Locale, List)} but against explicit overrides
   * instead of the contract's stored ones, so required/pinned/numbering stay in one place.
   */
  public List<ResolvedLeaseClauseResponse> resolve(
      List<LeaseClauseTemplate> templates,
      Collection<ClauseOverride> overrides,
      Optional<String> country,
      Locale locale) {
    if (templates.isEmpty()) {
      throw LeaseNotAvailableException.forContract(country);
    }

    Map<UUID, ClauseOverride> overridesByTemplateId =
        overrides.stream()
            .collect(Collectors.toMap(ClauseOverride::templateId, Function.identity()));

    List<Resolved> ordered =
        templates.stream()
            .map(
                t -> {
                  ClauseOverride override = overridesByTemplateId.get(t.getId());
                  boolean included = override != null ? override.included() : t.isDefaultIncluded();
                  // Required clauses are always included, regardless of a stored override or the
                  // template's default — this is the single source of truth enforcing that a
                  // non-optional clause can never be excluded from the generated document, even
                  // if a template was changed from optional to required after overrides existed.
                  included = included || !t.isOptional();
                  // A pinned clause keeps its template position; any override sortOrder is ignored.
                  int sortOrder =
                      override != null && !t.isPinned() ? override.sortOrder() : t.getSortOrder();
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
