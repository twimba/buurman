package com.buurman.service.demo;

import static com.buurman.jooq.generated.Tables.AUDIT_LOG;
import static com.buurman.jooq.generated.Tables.CONTRACTS;
import static com.buurman.jooq.generated.Tables.EXPENSES;
import static com.buurman.jooq.generated.Tables.PAYMENTS;
import static com.buurman.jooq.generated.Tables.PROPERTIES;
import static com.buurman.jooq.generated.Tables.CONTACTS;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
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

      total += generatePropertyAuditLogs(teamId);
      total += generateContactAuditLogs(teamId);
      total += generateContractAuditLogs(teamId, currency);
      total += generatePaymentAuditLogs(teamId, currency);
      total += generateExpenseAuditLogs(teamId, currency);
    }

    log.info("Created {} audit log entries", total);
  }

  private int generatePropertyAuditLogs(UUID teamId) {
    var records =
        dsl.select(
                PROPERTIES.ID,
                PROPERTIES.STREET,
                PROPERTIES.CITY,
                PROPERTIES.POSTAL_CODE,
                PROPERTIES.COUNTRY_CODE,
                PROPERTIES.PROPERTY_TYPE,
                PROPERTIES.PROPERTY_CATEGORY,
                PROPERTIES.STATUS,
                PROPERTIES.AREA_VALUE,
                PROPERTIES.AREA_UNIT,
                PROPERTIES.YEAR_BUILT,
                PROPERTIES.HEATING_TYPE,
                PROPERTIES.ENERGY_EFFICIENCY_RATING,
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
      values.put("status", r.get(PROPERTIES.STATUS));
      values.put("area", r.get(PROPERTIES.AREA_VALUE));
      values.put("areaUnit", r.get(PROPERTIES.AREA_UNIT));
      values.put("yearBuilt", r.get(PROPERTIES.YEAR_BUILT));
      values.put("heatingType", r.get(PROPERTIES.HEATING_TYPE));
      values.put("energyEfficiencyRating", r.get(PROPERTIES.ENERGY_EFFICIENCY_RATING));

      insertAuditLog(
          teamId,
          "PROPERTY",
          r.get(PROPERTIES.ID),
          r.get(PROPERTIES.CREATED_BY),
          r.get(PROPERTIES.CREATED_AT),
          values);
    }

    return records.size();
  }

  private int generateContactAuditLogs(UUID teamId) {
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

      insertAuditLog(
          teamId,
          "CONTACT",
          r.get(CONTACTS.ID),
          r.get(CONTACTS.CREATED_BY),
          r.get(CONTACTS.CREATED_AT),
          values);
    }

    return records.size();
  }

  private int generateContractAuditLogs(UUID teamId, String teamCurrency) {
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

      insertAuditLog(
          teamId,
          "CONTRACT",
          r.get(CONTRACTS.ID),
          r.get(CONTRACTS.CREATED_BY),
          r.get(CONTRACTS.CREATED_AT),
          values);
    }

    return records.size();
  }

  private int generatePaymentAuditLogs(UUID teamId, String teamCurrency) {
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

      insertAuditLog(
          teamId,
          "PAYMENT",
          r.get(PAYMENTS.ID),
          r.get(PAYMENTS.CREATED_BY),
          r.get(PAYMENTS.CREATED_AT),
          values);
    }

    return records.size();
  }

  private int generateExpenseAuditLogs(UUID teamId, String teamCurrency) {
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

      insertAuditLog(
          teamId,
          "EXPENSE",
          r.get(EXPENSES.ID),
          r.get(EXPENSES.CREATED_BY),
          r.get(EXPENSES.CREATED_AT),
          values);
    }

    return records.size();
  }

  private void insertAuditLog(
      UUID teamId,
      String entityType,
      UUID entityId,
      UUID userId,
      LocalDateTime timestamp,
      Map<String, Object> newValues) {
    try {
      JSONB newValuesJsonb = JSONB.valueOf(objectMapper.writeValueAsString(newValues));

      dsl.insertInto(AUDIT_LOG)
          .set(AUDIT_LOG.ID, UUID.randomUUID())
          .set(AUDIT_LOG.TEAM_ID, teamId)
          .set(AUDIT_LOG.ENTITY_TYPE, entityType)
          .set(AUDIT_LOG.ENTITY_ID, entityId)
          .set(AUDIT_LOG.ACTION, "CREATE")
          .set(AUDIT_LOG.NEW_VALUES, newValuesJsonb)
          .set(AUDIT_LOG.USER_ID, userId)
          .set(AUDIT_LOG.TIMESTAMP, timestamp)
          .execute();
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize audit log values for {} {}", entityType, entityId, e);
    }
  }
}
