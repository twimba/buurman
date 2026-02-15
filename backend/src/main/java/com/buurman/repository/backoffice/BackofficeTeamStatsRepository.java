package com.buurman.repository.backoffice;

import com.buurman.dto.response.backoffice.BackofficeTeamDetailResponse.DataCounts;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static com.buurman.jooq.generated.Tables.CONTRACTS;
import static com.buurman.jooq.generated.Tables.DOCUMENTS;
import static com.buurman.jooq.generated.Tables.EXPENSES;
import static com.buurman.jooq.generated.Tables.PAYMENTS;
import static com.buurman.jooq.generated.Tables.PROPERTIES;
import static com.buurman.jooq.generated.Tables.TENANTS;
import static org.jooq.impl.DSL.count;
import static org.jooq.impl.DSL.sum;

@Repository
@RequiredArgsConstructor
public class BackofficeTeamStatsRepository {

    private final DSLContext dsl;

    public DataCounts countEntitiesForTeam(UUID teamId) {
        long properties = dsl.selectCount().from(PROPERTIES)
                .where(PROPERTIES.TEAM_ID.eq(teamId).and(PROPERTIES.DELETED_AT.isNull()))
                .fetchOne(0, long.class);
        long tenants = dsl.selectCount().from(TENANTS)
                .where(TENANTS.TEAM_ID.eq(teamId).and(TENANTS.DELETED_AT.isNull()))
                .fetchOne(0, long.class);
        long contracts = dsl.selectCount().from(CONTRACTS)
                .where(CONTRACTS.TEAM_ID.eq(teamId).and(CONTRACTS.DELETED_AT.isNull()))
                .fetchOne(0, long.class);
        long expenses = dsl.selectCount().from(EXPENSES)
                .where(EXPENSES.TEAM_ID.eq(teamId).and(EXPENSES.DELETED_AT.isNull()))
                .fetchOne(0, long.class);
        long payments = dsl.selectCount().from(PAYMENTS)
                .where(PAYMENTS.TEAM_ID.eq(teamId).and(PAYMENTS.DELETED_AT.isNull()))
                .fetchOne(0, long.class);
        long documents = dsl.selectCount().from(DOCUMENTS)
                .where(DOCUMENTS.TEAM_ID.eq(teamId).and(DOCUMENTS.DELETED_AT.isNull()))
                .fetchOne(0, long.class);
        return new DataCounts(properties, tenants, contracts, expenses, payments, documents);
    }

    public BigDecimal sumActiveRentForTeam(UUID teamId) {
        BigDecimal result = dsl.select(sum(CONTRACTS.RENT_AMOUNT))
                .from(CONTRACTS)
                .where(CONTRACTS.TEAM_ID.eq(teamId)
                        .and(CONTRACTS.STATUS.eq("ACTIVE"))
                        .and(CONTRACTS.DELETED_AT.isNull()))
                .fetchOne(0, BigDecimal.class);
        return result != null ? result : BigDecimal.ZERO;
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
}
