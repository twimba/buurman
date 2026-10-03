package com.buurman.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;

import com.buurman.domain.Contract;
import com.buurman.domain.ContractLeaseClause;
import com.buurman.domain.LeaseClauseTemplate;
import com.buurman.domain.LeaseKind;
import com.buurman.domain.Sid;
import com.buurman.dto.response.ResolvedLeaseClauseResponse;
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
    return template(key, defaultIncluded, optional, sortOrder, false);
  }

  private LeaseClauseTemplate template(
      String key, boolean defaultIncluded, boolean optional, int sortOrder, boolean pinned) {
    return LeaseClauseTemplate.builder()
        .pinned(pinned)
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
    when(templateRepository.findByCountryAndKind("NL", LeaseKind.RESIDENTIAL))
        .thenReturn(List.of(parties));
    when(overrideRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID)).thenReturn(List.of());
    when(messageSource.getMessage(any(), any(), any(Locale.class))).thenReturn("resolved text");

    var resolved = resolver.resolve(contract("NL"), Locale.ENGLISH, LeaseKind.RESIDENTIAL);

    assertThat(resolved).hasSize(1);
    assertThat(resolved.get(0).included()).isTrue();
  }

  @Test
  void overrideFlipsInclusion() {
    LeaseClauseTemplate houseRules = template("house-rules", true, true, 5);
    when(templateRepository.findByCountryAndKind("NL", LeaseKind.RESIDENTIAL))
        .thenReturn(List.of(houseRules));
    when(overrideRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
        .thenReturn(
            List.of(
                ContractLeaseClause.builder()
                    .clauseTemplateId(houseRules.getId())
                    .included(false)
                    .sortOrder(5)
                    .build()));
    when(messageSource.getMessage(any(), any(), any(Locale.class))).thenReturn("resolved text");

    var resolved = resolver.resolve(contract("NL"), Locale.ENGLISH, LeaseKind.RESIDENTIAL);

    assertThat(resolved.get(0).included()).isFalse();
  }

  @Test
  void requiredTemplateIsAlwaysIncludedEvenWithAnExclusionOverride() {
    // Simulates a template that was made required after an override excluding it was already
    // stored (e.g. back when it was still optional) — the resolver must force it back in.
    LeaseClauseTemplate parties = template("parties", true, false, 1);
    when(templateRepository.findByCountryAndKind("NL", LeaseKind.RESIDENTIAL))
        .thenReturn(List.of(parties));
    when(overrideRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
        .thenReturn(
            List.of(
                ContractLeaseClause.builder()
                    .clauseTemplateId(parties.getId())
                    .included(false)
                    .sortOrder(1)
                    .build()));
    when(messageSource.getMessage(any(), any(), any(Locale.class))).thenReturn("resolved text");

    var resolved = resolver.resolve(contract("NL"), Locale.ENGLISH, LeaseKind.RESIDENTIAL);

    assertThat(resolved.get(0).included()).isTrue();
  }

  @Test
  void countryWithNoTemplatesRejectsWithBusinessRuleException() {
    when(templateRepository.findByCountryAndKind("GB", LeaseKind.RESIDENTIAL))
        .thenReturn(List.of());
    when(templateRepository.findByCountryAndKind("GB", LeaseKind.LEGACY)).thenReturn(List.of());

    assertThatThrownBy(
            () -> resolver.resolve(contract("GB"), Locale.ENGLISH, LeaseKind.RESIDENTIAL))
        .isInstanceOf(BusinessRuleException.class);
  }

  private ContractLeaseClause override(LeaseClauseTemplate t, boolean included, int sortOrder) {
    return ContractLeaseClause.builder()
        .clauseTemplateId(t.getId())
        .included(included)
        .sortOrder(sortOrder)
        .build();
  }

  @Test
  void fallsBackToLegacyTemplatesWithContiguousArticleNumbers() {
    List<LeaseClauseTemplate> legacy =
        IntStream.rangeClosed(1, 7).mapToObj(i -> template("legacy" + i, true, false, i)).toList();
    when(templateRepository.findByCountryAndKind("NL", LeaseKind.COMMERCIAL)).thenReturn(List.of());
    when(templateRepository.findByCountryAndKind("NL", LeaseKind.LEGACY)).thenReturn(legacy);
    when(overrideRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID)).thenReturn(List.of());
    when(messageSource.getMessage(any(), any(), any(Locale.class))).thenReturn("text");

    var resolved = resolver.resolve(contract("NL"), Locale.ENGLISH, LeaseKind.COMMERCIAL);

    assertThat(resolved).hasSize(7);
    assertThat(resolved)
        .extracting(ResolvedLeaseClauseResponse::articleNumber)
        .containsExactly(1, 2, 3, 4, 5, 6, 7);
  }

  @Test
  void pinnedClauseIgnoresConflictingOverrideSortOrder() {
    var parties = template("parties", true, false, 1, true);
    var rent = template("rent", true, false, 2);
    when(templateRepository.findByCountryAndKind("NL", LeaseKind.RESIDENTIAL))
        .thenReturn(List.of(parties, rent));
    when(overrideRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
        .thenReturn(List.of(override(parties, true, 99), override(rent, true, 5)));
    when(messageSource.getMessage(any(), any(), any(Locale.class))).thenReturn("text");

    var resolved = resolver.resolve(contract("NL"), Locale.ENGLISH, LeaseKind.RESIDENTIAL);

    assertThat(resolved)
        .extracting(ResolvedLeaseClauseResponse::clauseKey)
        .containsExactly("parties", "rent");
    assertThat(resolved.get(0).pinned()).isTrue();
    assertThat(resolved.get(0).sortOrder()).isEqualTo(1);
    assertThat(resolved.get(1).sortOrder()).isEqualTo(5);
  }

  @Test
  void pinnedClausesComeFirstEvenWhenNonPinnedHasLowerSortOrder() {
    var rent = template("rent", true, false, 1);
    var parties = template("parties", true, false, 5, true);
    when(templateRepository.findByCountryAndKind("NL", LeaseKind.RESIDENTIAL))
        .thenReturn(List.of(rent, parties));
    when(overrideRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID)).thenReturn(List.of());
    when(messageSource.getMessage(any(), any(), any(Locale.class))).thenReturn("text");

    var resolved = resolver.resolve(contract("NL"), Locale.ENGLISH, LeaseKind.RESIDENTIAL);

    assertThat(resolved)
        .extracting(ResolvedLeaseClauseResponse::clauseKey)
        .containsExactly("parties", "rent");
  }

  @Test
  void excludedClauseGetsArticleZeroAndLaterArticlesRenumber() {
    var a = template("a", true, false, 1);
    var b = template("b", true, true, 2);
    var c = template("c", true, false, 3);
    when(templateRepository.findByCountryAndKind("NL", LeaseKind.RESIDENTIAL))
        .thenReturn(List.of(a, b, c));
    when(overrideRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
        .thenReturn(List.of(override(b, false, 2)));
    when(messageSource.getMessage(any(), any(), any(Locale.class))).thenReturn("text");

    var resolved = resolver.resolve(contract("NL"), Locale.ENGLISH, LeaseKind.RESIDENTIAL);

    assertThat(resolved)
        .extracting(
            ResolvedLeaseClauseResponse::clauseKey, ResolvedLeaseClauseResponse::articleNumber)
        .containsExactly(tuple("a", 1), tuple("b", 0), tuple("c", 2));
  }

  @Test
  void tiesOnEffectiveSortOrderBreakByTemplateSortOrder() {
    var a = template("a", true, true, 1);
    var b = template("b", true, true, 2);
    when(templateRepository.findByCountryAndKind("NL", LeaseKind.RESIDENTIAL))
        .thenReturn(List.of(b, a));
    when(overrideRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
        .thenReturn(List.of(override(a, true, 10), override(b, true, 10)));
    when(messageSource.getMessage(any(), any(), any(Locale.class))).thenReturn("text");

    var resolved = resolver.resolve(contract("NL"), Locale.ENGLISH, LeaseKind.RESIDENTIAL);

    assertThat(resolved)
        .extracting(ResolvedLeaseClauseResponse::clauseKey)
        .containsExactly("a", "b");
  }

  @Test
  void furnishedKindUsesResidentialTemplatesWhenNoFurnishedOnesExist() {
    var residential = template("parties", true, false, 1);
    when(templateRepository.findByCountryAndKind("NL", LeaseKind.RESIDENTIAL_FURNISHED))
        .thenReturn(List.of());
    when(templateRepository.findByCountryAndKind("NL", LeaseKind.RESIDENTIAL))
        .thenReturn(List.of(residential));

    assertThat(resolver.templatesFor(contract("NL"), LeaseKind.RESIDENTIAL_FURNISHED))
        .containsExactly(residential);
  }

  @Test
  void furnishedKindPrefersItsOwnTemplates() {
    var furnished = template("furnished-parties", true, false, 1);
    when(templateRepository.findByCountryAndKind("NL", LeaseKind.RESIDENTIAL_FURNISHED))
        .thenReturn(List.of(furnished));

    assertThat(resolver.templatesFor(contract("NL"), LeaseKind.RESIDENTIAL_FURNISHED))
        .containsExactly(furnished);
  }

  @Test
  void furnishedKindFallsAllTheWayToLegacy() {
    var legacy = template("term", true, false, 1);
    when(templateRepository.findByCountryAndKind("DE", LeaseKind.LEGACY))
        .thenReturn(List.of(legacy));

    assertThat(resolver.templatesFor(contract("DE"), LeaseKind.RESIDENTIAL_FURNISHED))
        .containsExactly(legacy);
  }
}
