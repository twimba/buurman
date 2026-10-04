package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.RENT_REGULATION_TERMINATION_RULES;

import java.util.List;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.TerminationGivenBy;
import com.buurman.domain.regulation.TerminationNoticeRule;
import com.buurman.mapper.TerminationNoticeRuleRecordMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class TerminationNoticeRuleRepository {

  private final DSLContext dsl;
  private final TerminationNoticeRuleRecordMapper mapper;

  public List<TerminationNoticeRule> findByCountryAndParty(
      UUID countryId, TerminationGivenBy partyType) {
    return List.copyOf(
        dsl.selectFrom(RENT_REGULATION_TERMINATION_RULES)
            .where(
                RENT_REGULATION_TERMINATION_RULES
                    .COUNTRY_ID
                    .eq(countryId)
                    .and(RENT_REGULATION_TERMINATION_RULES.PARTY_TYPE.eq(partyType.name())))
            .fetch()
            .map(mapper::toDomain));
  }

  public List<TerminationNoticeRule> findAll() {
    return List.copyOf(
        dsl.selectFrom(RENT_REGULATION_TERMINATION_RULES).fetch().map(mapper::toDomain));
  }

  /**
   * Inserts a termination rule row. Used by {@code RentRegulationCatalogService.reload} to re-seed
   * this table after {@code RentRegulationRepository.deleteAllReferenceData} wipes it — the table
   * has no representation in the bundled rent-regulation catalog, so a reload must recapture and
   * reinsert whatever rows existed beforehand rather than losing them permanently.
   */
  public TerminationNoticeRule save(TerminationNoticeRule rule) {
    UUID id = UUID.randomUUID();
    dsl.insertInto(RENT_REGULATION_TERMINATION_RULES)
        .set(RENT_REGULATION_TERMINATION_RULES.ID, id)
        .set(RENT_REGULATION_TERMINATION_RULES.COUNTRY_ID, rule.getCountryId())
        .set(RENT_REGULATION_TERMINATION_RULES.REGION_ID, rule.getRegionId().orElse(null))
        .set(RENT_REGULATION_TERMINATION_RULES.PARTY_TYPE, rule.getPartyType().name())
        .set(
            RENT_REGULATION_TERMINATION_RULES.MIN_TENANCY_MONTHS,
            rule.getMinTenancyMonths().orElse(null))
        .set(RENT_REGULATION_TERMINATION_RULES.NOTICE_DAYS, rule.getNoticeDays())
        .set(RENT_REGULATION_TERMINATION_RULES.GROUNDS_REQUIRED, rule.isGroundsRequired())
        .set(
            RENT_REGULATION_TERMINATION_RULES.GROUNDS_CODES,
            rule.getGroundsCodes().isEmpty() ? null : rule.getGroundsCodes().toArray(new String[0]))
        .set(RENT_REGULATION_TERMINATION_RULES.SOURCE_URL, rule.getSourceUrl().orElse(null))
        .set(RENT_REGULATION_TERMINATION_RULES.NOTES, rule.getNotes().orElse(null))
        .execute();
    rule.setId(id);
    return rule;
  }
}
