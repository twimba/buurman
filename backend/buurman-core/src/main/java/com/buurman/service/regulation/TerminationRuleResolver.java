package com.buurman.service.regulation;

import java.time.LocalDate;
import java.time.Period;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.buurman.domain.Contract;
import com.buurman.domain.RentRegulationCountry;
import com.buurman.domain.TerminationGivenBy;
import com.buurman.domain.regulation.TerminationNoticeRule;
import com.buurman.repository.RentRegulationRepository;
import com.buurman.repository.TerminationNoticeRuleRepository;

import lombok.RequiredArgsConstructor;

/**
 * Resolves the notice period for a contract termination. Precedence: catalog rule (most specific
 * matching min-tenancy-months threshold) &gt; contract's own landlordNoticeDays/tenantNoticeDays
 * &gt; hardcoded default — the same three-tier pattern {@code
 * PaymentFormalNoticeExporter.resolveDeadlineDays()} already uses for formal-notice deadlines.
 *
 * <p>Catalog notice-day figures are example/starting data (see the seed migration's comments), not
 * verified legal advice — this class computes from stored data, it doesn't certify correctness.
 */
@Service
@RequiredArgsConstructor
public class TerminationRuleResolver {

  static final int DEFAULT_NOTICE_DAYS = 30;

  public enum Source {
    CATALOG_RULE,
    CONTRACT_FALLBACK,
    HARDCODED_DEFAULT
  }

  public record TerminationComputation(
      int noticeDays, boolean groundsRequired, List<String> groundsCodes, Source source) {}

  private final RentRegulationRepository rentRegulationRepository;
  private final TerminationNoticeRuleRepository terminationNoticeRuleRepository;

  public TerminationComputation resolve(
      Contract contract, TerminationGivenBy givenBy, LocalDate noticeDate) {
    Optional<RentRegulationCountry> country =
        contract.getCountryCode().flatMap(rentRegulationRepository::findCountryByCode);

    Optional<TerminationNoticeRule> matchingRule =
        country.flatMap(
            c -> bestMatchingRule(c.getId(), givenBy, contract.getStartDate(), noticeDate));

    if (matchingRule.isPresent()) {
      TerminationNoticeRule rule = matchingRule.get();
      return new TerminationComputation(
          rule.getNoticeDays(),
          rule.isGroundsRequired(),
          rule.getGroundsCodes(),
          Source.CATALOG_RULE);
    }

    Integer contractDays =
        givenBy == TerminationGivenBy.LANDLORD
            ? contract.getLandlordNoticeDays()
            : contract.getTenantNoticeDays();
    if (contractDays != null) {
      return new TerminationComputation(contractDays, false, List.of(), Source.CONTRACT_FALLBACK);
    }

    return new TerminationComputation(
        DEFAULT_NOTICE_DAYS, false, List.of(), Source.HARDCODED_DEFAULT);
  }

  private Optional<TerminationNoticeRule> bestMatchingRule(
      UUID countryId, TerminationGivenBy givenBy, LocalDate contractStart, LocalDate noticeDate) {
    long totalMonths = Period.between(contractStart, noticeDate).toTotalMonths();
    int tenancyMonths = totalMonths >= 0 ? (int) totalMonths : 0;

    // thenComparing(id) breaks ties deterministically: nothing stops two rules sharing the same
    // minTenancyMonths (e.g. a country + a region-specific rule both set to the same threshold),
    // and without a secondary key, max() over the DB's unordered result picks arbitrarily between
    // runs instead of consistently.
    return terminationNoticeRuleRepository.findByCountryAndParty(countryId, givenBy).stream()
        .filter(rule -> rule.getMinTenancyMonths().map(min -> tenancyMonths >= min).orElse(true))
        .max(
            Comparator.comparing(
                    (TerminationNoticeRule rule) -> rule.getMinTenancyMonths().orElse(0))
                .thenComparing(TerminationNoticeRule::getId));
  }
}
