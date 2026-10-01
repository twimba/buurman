package com.buurman.repository.backoffice;

import static com.buurman.jooq.generated.Tables.CONTACTS;
import static com.buurman.jooq.generated.Tables.CONTRACTS;
import static com.buurman.jooq.generated.Tables.DOCUMENTS;
import static com.buurman.jooq.generated.Tables.EXPENSES;
import static com.buurman.jooq.generated.Tables.PAYMENTS;
import static com.buurman.jooq.generated.Tables.PROPERTIES;
import static com.buurman.jooq.generated.Tables.UNITS;
import static org.jooq.impl.DSL.count;
import static org.jooq.impl.DSL.sum;

import java.math.BigDecimal;
import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.Contract;
import com.buurman.dto.response.backoffice.BackofficeTeamDetailResponse.DataCounts;
import com.buurman.repository.UnitRepository;
import com.buurman.repository.UnitScope;
import com.buurman.util.MoneyAmount;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class BackofficeTeamStatsRepository {

  private final DSLContext dsl;
  private final UnitRepository unitRepository;

  public DataCounts countEntitiesForTeam(UUID teamId) {
    long properties =
        fetchCount(
            dsl.selectCount()
                .from(PROPERTIES)
                .where(PROPERTIES.TEAM_ID.eq(teamId).and(PROPERTIES.DELETED_AT.isNull())));
    // Delegates to UnitRepository so the same properties-join predicate (units of a
    // soft-deleted property are excluded) is used everywhere this figure is surfaced.
    long units = unitRepository.countActiveByTeamId(teamId);
    long contacts =
        fetchCount(
            dsl.selectCount()
                .from(CONTACTS)
                .where(CONTACTS.TEAM_ID.eq(teamId).and(CONTACTS.DELETED_AT.isNull())));
    long contracts =
        fetchCount(
            dsl.selectCount()
                .from(CONTRACTS)
                .where(CONTRACTS.TEAM_ID.eq(teamId).and(CONTRACTS.DELETED_AT.isNull())));
    long expenses =
        fetchCount(
            dsl.selectCount()
                .from(EXPENSES)
                .where(EXPENSES.TEAM_ID.eq(teamId).and(EXPENSES.DELETED_AT.isNull())));
    long payments =
        fetchCount(
            dsl.selectCount()
                .from(PAYMENTS)
                .where(PAYMENTS.TEAM_ID.eq(teamId).and(PAYMENTS.DELETED_AT.isNull())));
    long documents =
        fetchCount(
            dsl.selectCount()
                .from(DOCUMENTS)
                .where(DOCUMENTS.TEAM_ID.eq(teamId).and(DOCUMENTS.DELETED_AT.isNull())));
    return new DataCounts(properties, units, contacts, contracts, expenses, payments, documents);
  }

  private static long fetchCount(org.jooq.SelectConditionStep<?> query) {
    Long result = query.fetchOne(0, Long.class);
    return result != null ? result : 0L;
  }

  /**
   * Sums the rent of in-force contracts ({@code ACTIVE} or {@code NOTICE_GIVEN}) grouped by
   * currency. Returns the total for the most-used currency along with that currency code. Returns
   * (ZERO, null) when no in-force contracts exist.
   */
  public Map.Entry<BigDecimal, String> sumActiveRentForTeam(UUID teamId) {
    var rows =
        dsl.select(CONTRACTS.RENT_AMOUNT_CURRENCY, sum(CONTRACTS.RENT_AMOUNT))
            .from(CONTRACTS)
            .where(
                CONTRACTS
                    .TEAM_ID
                    .eq(teamId)
                    .and(CONTRACTS.STATUS.in(Contract.ContractStatus.IN_FORCE_NAMES))
                    .and(CONTRACTS.DELETED_AT.isNull())
                    .and(CONTRACTS.RENT_AMOUNT_CURRENCY.isNotNull()))
            .groupBy(CONTRACTS.RENT_AMOUNT_CURRENCY)
            .fetch();

    if (rows.isEmpty()) {
      return new AbstractMap.SimpleEntry<>(BigDecimal.ZERO, null);
    }

    // Pick the currency group with the largest sum
    String bestCurrency = null;
    BigDecimal bestSum = BigDecimal.ZERO;
    for (var row : rows) {
      String cur = row.value1();
      BigDecimal total = row.value2() != null ? row.value2() : BigDecimal.ZERO;
      if (total.compareTo(bestSum) > 0) {
        bestSum = total;
        bestCurrency = cur;
      }
    }
    return new AbstractMap.SimpleEntry<>(
        MoneyAmount.sumToMajorUnits(bestSum, bestCurrency), bestCurrency);
  }

  /**
   * Kept the name for continuity with {@link
   * com.buurman.dto.response.backoffice.BackofficeTeamDetailResponse#propertyStatusDistribution},
   * but this is now a unit-status breakdown — {@code properties.status} was dropped in V070 in
   * favor of per-unit status. This was the one live regression left by that migration: nothing
   * caught the {@code UnsupportedOperationException} it used to throw, so the backoffice
   * team-detail screen 500'd.
   */
  public Map<String, Long> propertyStatusDistribution(UUID teamId) {
    Map<String, Long> result = new LinkedHashMap<>();
    dsl.select(UNITS.STATUS, count())
        .from(UNITS)
        .join(PROPERTIES)
        .on(PROPERTIES.ID.eq(UNITS.PROPERTY_ID))
        .where(UNITS.TEAM_ID.eq(teamId).and(UnitScope.active()))
        .groupBy(UNITS.STATUS)
        .fetch()
        .forEach(r -> result.put(r.value1(), r.value2().longValue()));
    return result;
  }

  public Map<String, Long> contractStatusDistribution(UUID teamId) {
    Map<String, Long> result = new LinkedHashMap<>();
    dsl.select(CONTRACTS.STATUS, count())
        .from(CONTRACTS)
        .where(CONTRACTS.TEAM_ID.eq(teamId).and(CONTRACTS.DELETED_AT.isNull()))
        .groupBy(CONTRACTS.STATUS)
        .fetch()
        .forEach(r -> result.put(r.value1(), r.value2().longValue()));
    return result;
  }

  public Map<String, Long> paymentStatusDistribution(UUID teamId) {
    Map<String, Long> result = new LinkedHashMap<>();
    dsl.select(PAYMENTS.STATUS, count())
        .from(PAYMENTS)
        .where(PAYMENTS.TEAM_ID.eq(teamId).and(PAYMENTS.DELETED_AT.isNull()))
        .groupBy(PAYMENTS.STATUS)
        .fetch()
        .forEach(r -> result.put(r.value1(), r.value2().longValue()));
    return result;
  }

  public Map<String, Long> propertyCategoryDistribution(UUID teamId) {
    Map<String, Long> result = new LinkedHashMap<>();
    dsl.select(PROPERTIES.PROPERTY_CATEGORY, count())
        .from(PROPERTIES)
        .where(PROPERTIES.TEAM_ID.eq(teamId).and(PROPERTIES.DELETED_AT.isNull()))
        .groupBy(PROPERTIES.PROPERTY_CATEGORY)
        .fetch()
        .forEach(r -> result.put(r.value1(), r.value2().longValue()));
    return result;
  }
}
