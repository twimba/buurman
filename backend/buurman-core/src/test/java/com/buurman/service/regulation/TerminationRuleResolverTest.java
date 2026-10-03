package com.buurman.service.regulation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.buurman.domain.Contract;
import com.buurman.domain.RentRegulationCountry;
import com.buurman.domain.TerminationGivenBy;
import com.buurman.domain.regulation.TerminationNoticeRule;
import com.buurman.repository.RentRegulationRepository;
import com.buurman.repository.TerminationNoticeRuleRepository;
import com.buurman.util.MoneyAmount;

class TerminationRuleResolverTest {

  private final RentRegulationRepository rentRegulationRepository =
      mock(RentRegulationRepository.class);
  private final TerminationNoticeRuleRepository terminationNoticeRuleRepository =
      mock(TerminationNoticeRuleRepository.class);

  private final TerminationRuleResolver resolver =
      new TerminationRuleResolver(rentRegulationRepository, terminationNoticeRuleRepository);

  private static final UUID COUNTRY_ID = UUID.randomUUID();

  private Contract contractStartedYearsAgo(int years, Optional<String> countryCode) {
    return Contract.builder()
        .startDate(LocalDate.now().minusYears(years))
        .countryCode(countryCode)
        .landlordNoticeDays(30)
        .tenantNoticeDays(30)
        .rentAmount(MoneyAmount.of(new BigDecimal("1000.00"), "EUR"))
        .build();
  }

  @Test
  void picksCatalogRuleWhenOneMatchesTenancyLength() {
    when(rentRegulationRepository.findCountryByCode("DE"))
        .thenReturn(
            Optional.of(RentRegulationCountry.builder().id(COUNTRY_ID).countryCode("DE").build()));
    when(terminationNoticeRuleRepository.findByCountryAndParty(
            COUNTRY_ID, TerminationGivenBy.LANDLORD))
        .thenReturn(
            List.of(
                TerminationNoticeRule.builder()
                    .countryId(COUNTRY_ID)
                    .partyType(TerminationGivenBy.LANDLORD)
                    .minTenancyMonths(Optional.of(0))
                    .noticeDays(90)
                    .build(),
                TerminationNoticeRule.builder()
                    .countryId(COUNTRY_ID)
                    .partyType(TerminationGivenBy.LANDLORD)
                    .minTenancyMonths(Optional.of(96))
                    .noticeDays(270)
                    .build()));

    var result =
        resolver.resolve(
            contractStartedYearsAgo(9, Optional.of("DE")),
            TerminationGivenBy.LANDLORD,
            LocalDate.now());

    assertThat(result.noticeDays()).isEqualTo(270);
    assertThat(result.source()).isEqualTo(TerminationRuleResolver.Source.CATALOG_RULE);
  }

  @Test
  void breaksATieBetweenTwoRulesWithTheSameMinTenancyMonthsDeterministically() {
    when(rentRegulationRepository.findCountryByCode("DE"))
        .thenReturn(
            Optional.of(RentRegulationCountry.builder().id(COUNTRY_ID).countryCode("DE").build()));
    TerminationNoticeRule ruleA =
        TerminationNoticeRule.builder()
            .id(UUID.fromString("00000000-0000-0000-0000-00000000000a"))
            .countryId(COUNTRY_ID)
            .partyType(TerminationGivenBy.LANDLORD)
            .minTenancyMonths(Optional.of(0))
            .noticeDays(90)
            .build();
    // Nothing stops two rules from sharing the same threshold (e.g. a country-wide rule and a
    // region-specific one both set to 0); the fix must still pick the same one every time
    // regardless of which order the repository happens to return them in.
    TerminationNoticeRule ruleB =
        TerminationNoticeRule.builder()
            .id(UUID.fromString("00000000-0000-0000-0000-00000000000b"))
            .countryId(COUNTRY_ID)
            .partyType(TerminationGivenBy.LANDLORD)
            .minTenancyMonths(Optional.of(0))
            .noticeDays(120)
            .build();

    when(terminationNoticeRuleRepository.findByCountryAndParty(
            COUNTRY_ID, TerminationGivenBy.LANDLORD))
        .thenReturn(List.of(ruleA, ruleB))
        .thenReturn(List.of(ruleB, ruleA));

    var firstOrder =
        resolver.resolve(
            contractStartedYearsAgo(1, Optional.of("DE")),
            TerminationGivenBy.LANDLORD,
            LocalDate.now());
    var secondOrder =
        resolver.resolve(
            contractStartedYearsAgo(1, Optional.of("DE")),
            TerminationGivenBy.LANDLORD,
            LocalDate.now());

    assertThat(firstOrder.noticeDays()).isEqualTo(secondOrder.noticeDays());
  }

  @Test
  void fallsBackToContractFieldWhenNoCatalogRuleForCountry() {
    when(rentRegulationRepository.findCountryByCode("XX")).thenReturn(Optional.empty());

    var result =
        resolver.resolve(
            contractStartedYearsAgo(1, Optional.of("XX")),
            TerminationGivenBy.LANDLORD,
            LocalDate.now());

    assertThat(result.noticeDays()).isEqualTo(30);
    assertThat(result.source()).isEqualTo(TerminationRuleResolver.Source.CONTRACT_FALLBACK);
  }

  @Test
  void fallsBackToHardcodedDefaultWhenContractHasNoCountryCode() {
    Contract contract =
        Contract.builder()
            .startDate(LocalDate.now().minusYears(1))
            .countryCode(Optional.empty())
            .landlordNoticeDays(null)
            .rentAmount(MoneyAmount.of(new BigDecimal("1000.00"), "EUR"))
            .build();

    var result = resolver.resolve(contract, TerminationGivenBy.LANDLORD, LocalDate.now());

    assertThat(result.noticeDays()).isEqualTo(TerminationRuleResolver.DEFAULT_NOTICE_DAYS);
    assertThat(result.source()).isEqualTo(TerminationRuleResolver.Source.HARDCODED_DEFAULT);
  }
}
