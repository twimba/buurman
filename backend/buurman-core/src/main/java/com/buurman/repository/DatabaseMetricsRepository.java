package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.CONTACTS;
import static com.buurman.jooq.generated.Tables.CONTRACTS;
import static com.buurman.jooq.generated.Tables.EXPENSES;
import static com.buurman.jooq.generated.Tables.PAYMENTS;
import static com.buurman.jooq.generated.Tables.PROPERTIES;
import static com.buurman.jooq.generated.Tables.TEAMS;
import static com.buurman.jooq.generated.Tables.UNITS;
import static org.jooq.impl.DSL.count;
import static org.jooq.impl.DSL.select;

import java.util.List;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.LabelCount;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class DatabaseMetricsRepository {

  private final DSLContext dsl;

  private static final Condition NOT_DEMO_TEAM = TEAMS.DEMO.isFalse();

  public long countProperties() {
    var nonDemoTeamIds = select(TEAMS.ID).from(TEAMS).where(NOT_DEMO_TEAM);
    return dsl.fetchCount(
        PROPERTIES, PROPERTIES.DELETED_AT.isNull().and(PROPERTIES.TEAM_ID.in(nonDemoTeamIds)));
  }

  public long countContracts() {
    var nonDemoTeamIds = select(TEAMS.ID).from(TEAMS).where(NOT_DEMO_TEAM);
    return dsl.fetchCount(
        CONTRACTS, CONTRACTS.DELETED_AT.isNull().and(CONTRACTS.TEAM_ID.in(nonDemoTeamIds)));
  }

  public long countContacts() {
    var nonDemoTeamIds = select(TEAMS.ID).from(TEAMS).where(NOT_DEMO_TEAM);
    return dsl.fetchCount(
        CONTACTS, CONTACTS.DELETED_AT.isNull().and(CONTACTS.TEAM_ID.in(nonDemoTeamIds)));
  }

  public long countPayments() {
    var nonDemoTeamIds = select(TEAMS.ID).from(TEAMS).where(NOT_DEMO_TEAM);
    return dsl.fetchCount(
        PAYMENTS, PAYMENTS.DELETED_AT.isNull().and(PAYMENTS.TEAM_ID.in(nonDemoTeamIds)));
  }

  public long countTeams() {
    return dsl.fetchCount(TEAMS, NOT_DEMO_TEAM);
  }

  public long countExpenses() {
    var nonDemoTeamIds = select(TEAMS.ID).from(TEAMS).where(NOT_DEMO_TEAM);
    return dsl.fetchCount(
        EXPENSES, EXPENSES.DELETED_AT.isNull().and(EXPENSES.TEAM_ID.in(nonDemoTeamIds)));
  }

  public List<LabelCount> countContractsByStatus() {
    var nonDemoTeamIds = select(TEAMS.ID).from(TEAMS).where(NOT_DEMO_TEAM);
    return dsl.select(CONTRACTS.STATUS, count())
        .from(CONTRACTS)
        .where(CONTRACTS.DELETED_AT.isNull().and(CONTRACTS.TEAM_ID.in(nonDemoTeamIds)))
        .groupBy(CONTRACTS.STATUS)
        .fetch()
        .map(r -> new LabelCount(r.value1(), r.value2()));
  }

  public List<LabelCount> countPaymentsByStatus() {
    var nonDemoTeamIds = select(TEAMS.ID).from(TEAMS).where(NOT_DEMO_TEAM);
    return dsl.select(PAYMENTS.STATUS, count())
        .from(PAYMENTS)
        .where(PAYMENTS.DELETED_AT.isNull().and(PAYMENTS.TEAM_ID.in(nonDemoTeamIds)))
        .groupBy(PAYMENTS.STATUS)
        .fetch()
        .map(r -> new LabelCount(r.value1(), r.value2()));
  }

  /**
   * Kept the name (and the Prometheus gauge it feeds) for continuity, but the breakdown is now by
   * unit status, not property status — {@code properties.status} was dropped in V068 in favor of
   * per-unit status.
   */
  public List<LabelCount> countPropertiesByStatus() {
    var nonDemoTeamIds = select(TEAMS.ID).from(TEAMS).where(NOT_DEMO_TEAM);
    return dsl.select(UNITS.STATUS, count())
        .from(UNITS)
        .join(PROPERTIES)
        .on(PROPERTIES.ID.eq(UNITS.PROPERTY_ID))
        .where(UnitScope.active().and(PROPERTIES.TEAM_ID.in(nonDemoTeamIds)))
        .groupBy(UNITS.STATUS)
        .fetch()
        .map(r -> new LabelCount(r.value1(), r.value2()));
  }
}
