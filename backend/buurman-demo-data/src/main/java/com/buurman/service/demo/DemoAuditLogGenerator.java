package com.buurman.service.demo;

import static com.buurman.jooq.generated.Tables.AUDIT_LOG;
import static com.buurman.jooq.generated.Tables.CONTACTS;
import static com.buurman.jooq.generated.Tables.CONTRACTS;
import static com.buurman.jooq.generated.Tables.EXPENSES;
import static com.buurman.jooq.generated.Tables.PAYMENTS;
import static com.buurman.jooq.generated.Tables.PROPERTIES;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class DemoAuditLogGenerator {

  private final DSLContext dsl;
  private final ObjectMapper objectMapper;

  public void generate(DemoDataContext ctx) {
    int total = 0;

    for (var teamEntry : ctx.getTeamIds().entrySet()) {
      String teamKey = teamEntry.getKey();
      UUID teamId = teamEntry.getValue();
      String currency = ctx.getCurrencyForTeam(teamKey);

      List<Object[]> auditRecords = new ArrayList<>();

      collectPropertyAuditLogs(teamId, auditRecords);
      collectContactAuditLogs(teamId, auditRecords);
      collectContractAuditLogs(teamId, currency, auditRecords);
      collectPaymentAuditLogs(teamId, currency, auditRecords);
      collectExpenseAuditLogs(teamId, currency, auditRecords);

      if (!auditRecords.isEmpty()) {
        var insert =
            dsl.insertInto(AUDIT_LOG)
                .columns(
                    AUDIT_LOG.ID,
                    AUDIT_LOG.TEAM_ID,
                    AUDIT_LOG.ENTITY_TYPE,
                    AUDIT_LOG.ENTITY_ID,
                    AUDIT_LOG.ACTION,
                    AUDIT_LOG.NEW_VALUES,
                    AUDIT_LOG.USER_ID,
                    AUDIT_LOG.TIMESTAMP)
                .values((UUID) null, null, null, null, null, null, null, null);
        var batch = dsl.batch(insert);
        for (Object[] r : auditRecords) {
          batch = batch.bind(r);
        }
        batch.execute();
      }

      total += auditRecords.size();
    }

    log.info("Created {} audit log entries", total);
  }

  private void collectPropertyAuditLogs(UUID teamId, List<Object[]> auditRecords) {
    // Status/area/heating/energy fields moved from properties to units in V070 (BUUR-106);
    // dropped from the demo audit payload rather than faking values here.
    var records =
        dsl.select(
                PROPERTIES.ID,
                PROPERTIES.STREET,
                PROPERTIES.CITY,
                PROPERTIES.POSTAL_CODE,
                PROPERTIES.COUNTRY_CODE,
                PROPERTIES.PROPERTY_TYPE,
                PROPERTIES.PROPERTY_CATEGORY,
                PROPERTIES.YEAR_BUILT,
                PROPERTIES.CREATED_AT,
                PROPERTIES.CREATED_BY)
            .from(PROPERTIES)
            .where(PROPERTIES.TEAM_ID.eq(teamId))
            .fetch();

    for (var r : records) {
      Map<String, Object> values = new LinkedHashMap<>();
      values.put("street", r.get(PROPERTIES.STREET));
      values.put("city", r.get(PROPERTIES.CITY));
      values.put("postalCode", r.get(PROPERTIES.POSTAL_CODE));
      values.put("country", r.get(PROPERTIES.COUNTRY_CODE));
      values.put("propertyType", r.get(PROPERTIES.PROPERTY_TYPE));
      values.put("propertyCategory", r.get(PROPERTIES.PROPERTY_CATEGORY));
      values.put("yearBuilt", r.get(PROPERTIES.YEAR_BUILT));

      collectAuditLog(
          auditRecords,
          teamId,
          "PROPERTY",
          r.get(PROPERTIES.ID),
          r.get(PROPERTIES.CREATED_BY),
          r.get(PROPERTIES.CREATED_AT),
          values);
    }
  }

  private void collectContactAuditLogs(UUID teamId, List<Object[]> auditRecords) {
    var records =
        dsl.select(
                CONTACTS.ID,
                CONTACTS.FIRST_NAME,
                CONTACTS.LAST_NAME,
                CONTACTS.EMAIL,
                CONTACTS.PHONE,
                CONTACTS.CREATED_AT,
                CONTACTS.CREATED_BY)
            .from(CONTACTS)
            .where(CONTACTS.TEAM_ID.eq(teamId))
            .fetch();

    for (var r : records) {
      Map<String, Object> values = new LinkedHashMap<>();
      values.put("firstName", r.get(CONTACTS.FIRST_NAME));
      values.put("lastName", r.get(CONTACTS.LAST_NAME));
      values.put("email", r.get(CONTACTS.EMAIL));
      values.put("phone", r.get(CONTACTS.PHONE));

      collectAuditLog(
          auditRecords,
          teamId,
          "CONTACT",
          r.get(CONTACTS.ID),
          r.get(CONTACTS.CREATED_BY),
          r.get(CONTACTS.CREATED_AT),
          values);
    }
  }

  private void collectContractAuditLogs(
      UUID teamId, String teamCurrency, List<Object[]> auditRecords) {
    var records =
        dsl.select(
                CONTRACTS.ID,
                CONTRACTS.CONTRACT_TYPE,
                CONTRACTS.STATUS,
                CONTRACTS.START_DATE,
                CONTRACTS.END_DATE,
                CONTRACTS.RENT_AMOUNT,
                CONTRACTS.RENT_AMOUNT_CURRENCY,
                CONTRACTS.PAYMENT_FREQUENCY,
                CONTRACTS.DEPOSIT_AMOUNT,
                CONTRACTS.CREATED_AT,
                CONTRACTS.CREATED_BY)
            .from(CONTRACTS)
            .where(CONTRACTS.TEAM_ID.eq(teamId))
            .fetch();

    for (var r : records) {
      Map<String, Object> values = new LinkedHashMap<>();
      values.put("contractType", r.get(CONTRACTS.CONTRACT_TYPE));
      values.put("status", r.get(CONTRACTS.STATUS));
      values.put("startDate", Objects.toString(r.get(CONTRACTS.START_DATE), null));
      values.put("endDate", Objects.toString(r.get(CONTRACTS.END_DATE), null));
      String currency =
          r.get(CONTRACTS.RENT_AMOUNT_CURRENCY) != null
              ? r.get(CONTRACTS.RENT_AMOUNT_CURRENCY)
              : teamCurrency;
      values.put("rentAmount", r.get(CONTRACTS.RENT_AMOUNT));
      values.put("currency", currency);
      values.put("paymentFrequency", r.get(CONTRACTS.PAYMENT_FREQUENCY));
      values.put("depositAmount", r.get(CONTRACTS.DEPOSIT_AMOUNT));

      collectAuditLog(
          auditRecords,
          teamId,
          "CONTRACT",
          r.get(CONTRACTS.ID),
          r.get(CONTRACTS.CREATED_BY),
          r.get(CONTRACTS.CREATED_AT),
          values);
    }
  }

  private void collectPaymentAuditLogs(
      UUID teamId, String teamCurrency, List<Object[]> auditRecords) {
    var records =
        dsl.select(
                PAYMENTS.ID,
                PAYMENTS.AMOUNT,
                PAYMENTS.CURRENCY,
                PAYMENTS.DUE_DATE,
                PAYMENTS.STATUS,
                PAYMENTS.PAYMENT_DATE,
                PAYMENTS.CREATED_AT,
                PAYMENTS.CREATED_BY)
            .from(PAYMENTS)
            .where(PAYMENTS.TEAM_ID.eq(teamId))
            .fetch();

    for (var r : records) {
      Map<String, Object> values = new LinkedHashMap<>();
      String payCurrency =
          r.get(PAYMENTS.CURRENCY) != null ? r.get(PAYMENTS.CURRENCY) : teamCurrency;
      values.put("amount", r.get(PAYMENTS.AMOUNT));
      values.put("currency", payCurrency);
      values.put("dueDate", Objects.toString(r.get(PAYMENTS.DUE_DATE), null));
      values.put("status", r.get(PAYMENTS.STATUS));
      values.put("paymentDate", Objects.toString(r.get(PAYMENTS.PAYMENT_DATE), null));

      collectAuditLog(
          auditRecords,
          teamId,
          "PAYMENT",
          r.get(PAYMENTS.ID),
          r.get(PAYMENTS.CREATED_BY),
          r.get(PAYMENTS.CREATED_AT),
          values);
    }
  }

  private void collectExpenseAuditLogs(
      UUID teamId, String teamCurrency, List<Object[]> auditRecords) {
    var records =
        dsl.select(
                EXPENSES.ID,
                EXPENSES.CATEGORY,
                EXPENSES.AMOUNT,
                EXPENSES.CURRENCY,
                EXPENSES.EXPENSE_DATE,
                EXPENSES.DESCRIPTION,
                EXPENSES.CREATED_AT,
                EXPENSES.CREATED_BY)
            .from(EXPENSES)
            .where(EXPENSES.TEAM_ID.eq(teamId))
            .fetch();

    for (var r : records) {
      Map<String, Object> values = new LinkedHashMap<>();
      String expCurrency =
          r.get(EXPENSES.CURRENCY) != null ? r.get(EXPENSES.CURRENCY) : teamCurrency;
      values.put("category", r.get(EXPENSES.CATEGORY));
      values.put("amount", r.get(EXPENSES.AMOUNT));
      values.put("currency", expCurrency);
      values.put("expenseDate", Objects.toString(r.get(EXPENSES.EXPENSE_DATE), null));
      values.put("description", r.get(EXPENSES.DESCRIPTION));

      collectAuditLog(
          auditRecords,
          teamId,
          "EXPENSE",
          r.get(EXPENSES.ID),
          r.get(EXPENSES.CREATED_BY),
          r.get(EXPENSES.CREATED_AT),
          values);
    }
  }

  private void collectAuditLog(
      List<Object[]> auditRecords,
      UUID teamId,
      String entityType,
      UUID entityId,
      UUID userId,
      LocalDateTime timestamp,
      Map<String, Object> newValues) {
    try {
      JSONB newValuesJsonb = JSONB.valueOf(objectMapper.writeValueAsString(newValues));

      auditRecords.add(
          new Object[] {
            UUID.randomUUID(),
            teamId,
            entityType,
            entityId,
            "CREATE",
            newValuesJsonb,
            userId,
            timestamp
          });
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize audit log values for {} {}", entityType, entityId, e);
    }
  }
}
