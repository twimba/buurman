package com.buurman.service;

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
import com.buurman.domain.LeaseClauseTemplate;
import com.buurman.dto.response.ResolvedLeaseClauseResponse;
import com.buurman.exception.BusinessRuleException;
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

  public List<ResolvedLeaseClauseResponse> resolve(Contract contract, Locale locale) {
    String countryCode =
        contract
            .getCountryCode()
            .orElseThrow(() -> new BusinessRuleException("Contract has no country code set"));
    return resolve(contract, locale, templateRepository.findByCountryCode(countryCode));
  }

  /**
   * Same resolution, but using a template list the caller already fetched (e.g. {@code
   * LeaseClauseService#updateClauses}, which needs the same country's templates to validate the
   * request before calling this) instead of fetching it again here.
   */
  public List<ResolvedLeaseClauseResponse> resolve(
      Contract contract, Locale locale, List<LeaseClauseTemplate> templates) {
    if (templates.isEmpty()) {
      String countryCode = contract.getCountryCode().orElse("?");
      throw new BusinessRuleException(
          "No lease clause templates are configured for country " + countryCode);
    }

    Map<UUID, ContractLeaseClause> overridesByTemplateId =
        overrideRepository
            .findByContractIdAndTeamId(contract.getId(), contract.getTeamId())
            .stream()
            .collect(
                Collectors.toMap(ContractLeaseClause::getClauseTemplateId, Function.identity()));

    return templates.stream()
        .map(
            t -> {
              ContractLeaseClause override = overridesByTemplateId.get(t.getId());
              boolean included = override != null ? override.isIncluded() : t.isDefaultIncluded();
              // Required clauses are always included, regardless of a stored override or the
              // template's default — this is the single source of truth enforcing that a
              // non-optional clause can never be excluded from the generated document, even if
              // a template was changed from optional to required after overrides already existed.
              included = included || !t.isOptional();
              int sortOrder = override != null ? override.getSortOrder() : t.getSortOrder();
              String title = messageSource.getMessage(t.getTitleI18nKey(), null, locale);
              String body = messageSource.getMessage(t.getBodyI18nKey(), null, locale);
              return new ResolvedLeaseClauseResponse(
                  t.getIdentifier().orElseThrow(),
                  t.getClauseKey(),
                  title,
                  body,
                  included,
                  t.isOptional(),
                  sortOrder);
            })
        .sorted(Comparator.comparingInt(ResolvedLeaseClauseResponse::sortOrder))
        .toList();
  }
}
