package com.buurman.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
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

    List<LeaseClauseTemplate> templates = templateRepository.findByCountryCode(countryCode);
    if (templates.isEmpty()) {
      throw new BusinessRuleException(
          "No lease clause templates are configured for country " + countryCode);
    }

    Map<UUID, ContractLeaseClause> overridesByTemplateId =
        overrideRepository
            .findByContractIdAndTeamId(contract.getId(), contract.getTeamId())
            .stream()
            .collect(Collectors.toMap(ContractLeaseClause::getClauseTemplateId, o -> o));

    return templates.stream()
        .map(
            t -> {
              ContractLeaseClause override = overridesByTemplateId.get(t.getId());
              boolean included = override != null ? override.isIncluded() : t.isDefaultIncluded();
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
        .sorted((a, b) -> Integer.compare(a.sortOrder(), b.sortOrder()))
        .toList();
  }
}
