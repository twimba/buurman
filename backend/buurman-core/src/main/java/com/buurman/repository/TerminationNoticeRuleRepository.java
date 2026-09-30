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
}
