package com.buurman.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;

import com.buurman.domain.Contract;
import com.buurman.domain.ContractLeaseClause;
import com.buurman.domain.LeaseClauseTemplate;
import com.buurman.domain.Sid;
import com.buurman.exception.BusinessRuleException;
import com.buurman.repository.ContractLeaseClauseRepository;
import com.buurman.repository.LeaseClauseTemplateRepository;

class LeaseClauseResolverTest {

  private final LeaseClauseTemplateRepository templateRepository =
      mock(LeaseClauseTemplateRepository.class);
  private final ContractLeaseClauseRepository overrideRepository =
      mock(ContractLeaseClauseRepository.class);
  private final MessageSource messageSource = mock(MessageSource.class);

  private final LeaseClauseResolver resolver =
      new LeaseClauseResolver(templateRepository, overrideRepository, messageSource);

  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID CONTRACT_ID = UUID.randomUUID();

  private Contract contract(String countryCode) {
    return Contract.builder()
        .id(CONTRACT_ID)
        .teamId(TEAM_ID)
        .countryCode(Optional.of(countryCode))
        .build();
  }

  private LeaseClauseTemplate template(
      String key, boolean defaultIncluded, boolean optional, int sortOrder) {
    return LeaseClauseTemplate.builder()
        .id(UUID.randomUUID())
        .identifier(Optional.of(Sid.of("LCT0000000000000000000000001")))
        .countryCode("NL")
        .clauseKey(key)
        .titleI18nKey("lease." + key + ".title")
        .bodyI18nKey("lease." + key + ".body")
        .defaultIncluded(defaultIncluded)
        .optional(optional)
        .sortOrder(sortOrder)
        .version(1)
        .build();
  }

  @Test
  void defaultInclusionAppliesWhenNoOverrideExists() {
    LeaseClauseTemplate parties = template("parties", true, false, 1);
    when(templateRepository.findByCountryCode("NL")).thenReturn(List.of(parties));
    when(overrideRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID)).thenReturn(List.of());
    when(messageSource.getMessage(any(), any(), any(Locale.class))).thenReturn("resolved text");

    var resolved = resolver.resolve(contract("NL"), Locale.ENGLISH);

    assertThat(resolved).hasSize(1);
    assertThat(resolved.get(0).included()).isTrue();
  }

  @Test
  void overrideFlipsInclusion() {
    LeaseClauseTemplate houseRules = template("house-rules", true, true, 5);
    when(templateRepository.findByCountryCode("NL")).thenReturn(List.of(houseRules));
    when(overrideRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
        .thenReturn(
            List.of(
                ContractLeaseClause.builder()
                    .clauseTemplateId(houseRules.getId())
                    .included(false)
                    .sortOrder(5)
                    .build()));
    when(messageSource.getMessage(any(), any(), any(Locale.class))).thenReturn("resolved text");

    var resolved = resolver.resolve(contract("NL"), Locale.ENGLISH);

    assertThat(resolved.get(0).included()).isFalse();
  }

  @Test
  void requiredTemplateIsAlwaysIncludedEvenWithAnExclusionOverride() {
    // Simulates a template that was made required after an override excluding it was already
    // stored (e.g. back when it was still optional) — the resolver must force it back in.
    LeaseClauseTemplate parties = template("parties", true, false, 1);
    when(templateRepository.findByCountryCode("NL")).thenReturn(List.of(parties));
    when(overrideRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
        .thenReturn(
            List.of(
                ContractLeaseClause.builder()
                    .clauseTemplateId(parties.getId())
                    .included(false)
                    .sortOrder(1)
                    .build()));
    when(messageSource.getMessage(any(), any(), any(Locale.class))).thenReturn("resolved text");

    var resolved = resolver.resolve(contract("NL"), Locale.ENGLISH);

    assertThat(resolved.get(0).included()).isTrue();
  }

  @Test
  void countryWithNoTemplatesRejectsWithBusinessRuleException() {
    when(templateRepository.findByCountryCode("GB")).thenReturn(List.of());

    assertThatThrownBy(() -> resolver.resolve(contract("GB"), Locale.ENGLISH))
        .isInstanceOf(BusinessRuleException.class);
  }
}
