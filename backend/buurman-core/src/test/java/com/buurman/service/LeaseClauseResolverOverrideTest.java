package com.buurman.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;

import com.buurman.domain.Contract;
import com.buurman.domain.ContractLeaseClause;
import com.buurman.domain.LeaseClauseTemplate;
import com.buurman.dto.response.ResolvedLeaseClauseResponse;
import com.buurman.repository.ContractLeaseClauseRepository;
import com.buurman.repository.LeaseClauseTemplateRepository;
import com.buurman.service.LeaseClauseResolver.ClauseOverride;

@DisplayName("LeaseClauseResolver explicit overrides")
class LeaseClauseResolverOverrideTest {

  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID CONTRACT_ID = UUID.randomUUID();

  private final ContractLeaseClauseRepository overrideRepository =
      mock(ContractLeaseClauseRepository.class);
  private final MessageSource messageSource = mock(MessageSource.class);
  private final LeaseClauseResolver resolver =
      new LeaseClauseResolver(
          mock(LeaseClauseTemplateRepository.class), overrideRepository, messageSource);

  private static LeaseClauseTemplate template(
      String key, boolean optional, boolean pinned, int sortOrder) {
    return LeaseClauseTemplate.builder()
        .id(UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)))
        .identifier(
            Optional.of(com.buurman.domain.Sid.of(String.format("LCT%023d", (int) key.charAt(0)))))
        .clauseKey(key)
        .titleI18nKey("t." + key)
        .bodyI18nKey("b." + key)
        .defaultIncluded(true)
        .optional(optional)
        .pinned(pinned)
        .sortOrder(sortOrder)
        .build();
  }

  @Test
  @DisplayName("stored overrides and equivalent explicit overrides resolve identically")
  void storedAndExplicitOverridesAgree() {
    when(messageSource.getMessage(any(String.class), any(), any(Locale.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    List<LeaseClauseTemplate> templates =
        List.of(
            template("a-pinned", false, true, 1),
            template("b-required", false, false, 2),
            template("c-optional", true, false, 3),
            template("d-optional", true, false, 4));
    List<ContractLeaseClause> stored =
        List.of(
            override(templates.get(0), false, 99),
            override(templates.get(1), false, 5),
            override(templates.get(2), false, 0),
            override(templates.get(3), true, 1));
    when(overrideRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID)).thenReturn(stored);
    Contract contract = Contract.builder().id(CONTRACT_ID).teamId(TEAM_ID).build();

    List<ResolvedLeaseClauseResponse> viaContract =
        resolver.resolve(contract, Optional.of("NL"), Locale.ENGLISH, templates);
    List<ResolvedLeaseClauseResponse> viaOverrides =
        resolver.resolve(
            templates,
            stored.stream()
                .map(
                    o ->
                        new ClauseOverride(
                            o.getClauseTemplateId(), o.isIncluded(), o.getSortOrder()))
                .toList(),
            Optional.of("NL"),
            Locale.ENGLISH);

    assertThat(viaOverrides).isEqualTo(viaContract);
    assertThat(viaOverrides)
        .extracting(ResolvedLeaseClauseResponse::clauseKey)
        .containsExactly("a-pinned", "c-optional", "d-optional", "b-required");
    assertThat(viaOverrides)
        .extracting(ResolvedLeaseClauseResponse::included)
        .containsExactly(true, false, true, true);
  }

  private static ContractLeaseClause override(
      LeaseClauseTemplate t, boolean included, int sortOrder) {
    return ContractLeaseClause.builder()
        .contractId(CONTRACT_ID)
        .clauseTemplateId(t.getId())
        .included(included)
        .sortOrder(sortOrder)
        .build();
  }
}
