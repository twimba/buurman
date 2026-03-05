package com.buurman.repository.backoffice;

import static com.buurman.jooq.generated.Tables.CONTRACTS;
import static com.buurman.jooq.generated.Tables.DOCUMENTS;
import static com.buurman.jooq.generated.Tables.EXPENSES;
import static com.buurman.jooq.generated.Tables.PAYMENTS;
import static com.buurman.jooq.generated.Tables.PROPERTIES;
import static com.buurman.jooq.generated.Tables.TENANTS;
import static org.jooq.impl.DSL.count;
import static org.jooq.impl.DSL.sum;

import java.math.BigDecimal;
import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.dto.response.backoffice.BackofficeTeamDetailResponse.DataCounts;
import com.buurman.util.MoneyAmount;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class BackofficeTeamStatsRepository {

  private final DSLContext dsl;

  public DataCounts countEntitiesForTeam(UUID teamId) {
    long properties =
        fetchCount(
            dsl.selectCount()
                .from(PROPERTIES)
                .where(PROPERTIES.TEAM_ID.eq(teamId).and(PROPERTIES.DELETED_AT.isNull())));
    long tenants =
        fetchCount(
            dsl.selectCount()
                .from(TENANTS)
                .where(TENANTS.TEAM_ID.eq(teamId).and(TENANTS.DELETED_AT.isNull())));
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
    return new DataCounts(properties, tenants, contracts, expenses, payments, documents);
  }

  private static long fetchCount(org.jooq.SelectConditionStep<?> query) {
    Long result = query.fetchOne(0, Long.class);
    return result != null ? result : 0L;
  }

  /**
   * Sums active rent grouped by currency. Returns the total for the most-used currency along with
   * that currency code. Returns (ZERO, null) when no active contracts exist.
   */
  public Map.Entry<BigDecimal, String> sumActiveRentForTeam(UUID teamId) {
    var rows =
        dsl.select(CONTRACTS.RENT_AMOUNT_CURRENCY, sum(CONTRACTS.RENT_AMOUNT))
            .from(CONTRACTS)
            .where(
                CONTRACTS
                    .TEAM_ID
                    .eq(teamId)
                    .and(CONTRACTS.STATUS.eq("ACTIVE"))
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

  public Map<String, Long> propertyStatusDistribution(UUID teamId) {
    Map<String, Long> result = new LinkedHashMap<>();
    dsl.select(PROPERTIES.STATUS, count())
        .from(PROPERTIES)
        .where(PROPERTIES.TEAM_ID.eq(teamId).and(PROPERTIES.DELETED_AT.isNull()))
        .groupBy(PROPERTIES.STATUS)
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
