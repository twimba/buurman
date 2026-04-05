package com.buurman.service.demo;

import static com.buurman.jooq.generated.Tables.CONTRACTS;
import static com.buurman.util.SidGenerator.newContractExtensionId;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class ContractExtensionDemoDataGenerator {

  private final DSLContext dsl;
  private final Clock clock;
  private final Random random = new Random(42);

  @SuppressWarnings("NullAway")
  public void generate(DemoDataContext ctx) {
    LocalDateTime now = LocalDateTime.now(clock);
    LocalDate today = LocalDate.now(clock);

    for (var teamEntry : ctx.getTeamIds().entrySet()) {
      String teamKey = teamEntry.getKey();
      UUID teamId = teamEntry.getValue();
      UUID createdBy = ctx.getAdminUserForTeam(teamKey).orElse(null);
      List<UUID> contractIds = ctx.getContractIdsByTeam().get(teamId);

      if (contractIds == null || contractIds.isEmpty()) {
        continue;
      }

      int extensionsCreated = 0;
      List<Object[]> records = new ArrayList<>();

      // Find contracts with AUTOMATIC or MANUAL renewal_mode
      var renewableContracts =
          dsl.select(
                  CONTRACTS.ID,
                  CONTRACTS.RENT_AMOUNT,
                  CONTRACTS.RENT_AMOUNT_CURRENCY,
                  CONTRACTS.START_DATE,
                  CONTRACTS.END_DATE,
                  CONTRACTS.STATUS,
                  field("renewal_mode", String.class))
              .from(CONTRACTS)
              .where(CONTRACTS.TEAM_ID.eq(teamId))
              .and(CONTRACTS.DELETED_AT.isNull())
              .and(field("renewal_mode", String.class).in("AUTOMATIC", "MANUAL"))
              .and(CONTRACTS.STATUS.in("ACTIVE", "EXPIRED"))
              .fetch();

      for (Record contract : renewableContracts) {
        UUID contractId = contract.get(CONTRACTS.ID);
        BigDecimal rentAmount = contract.get(CONTRACTS.RENT_AMOUNT);
        String currency = contract.get(CONTRACTS.RENT_AMOUNT_CURRENCY);
        LocalDate endDate = contract.get(CONTRACTS.END_DATE);
        String contractStatus = contract.get(CONTRACTS.STATUS);
        String renewalMode = contract.get(field("renewal_mode", String.class));

        LocalDate startDate = contract.get(CONTRACTS.START_DATE);

        // Skip contracts without start or end date (indefinite)
        if (startDate == null || endDate == null) {
          continue;
        }

        String triggerType = "AUTOMATIC".equals(renewalMode) ? "AUTO" : "MANUAL";

        // Scale extensions with contract duration
        long contractYears = ChronoUnit.YEARS.between(startDate, endDate);
        int extensionCount;
        if (contractYears >= 10) {
          extensionCount = random.nextInt(4, 7); // 4-6 extensions
        } else if (contractYears >= 5) {
          extensionCount = random.nextInt(2, 5); // 2-4 extensions
        } else {
          extensionCount = random.nextInt(1, 3); // 1-2 extensions
        }
        LocalDate previousEndDate = endDate;
        // rentAmount comes through MoneyMinorUnitConverter, so it's already in major units
        BigDecimal currentRentMajor = rentAmount;

        for (int extNum = 1; extNum <= extensionCount; extNum++) {
          // 12-month extension term
          LocalDate newEndDate = previousEndDate.plusMonths(12);

          // Rent increase varies: 1.5-3% for older periods, 2-5% for recent
          double increaseBase;
          if (extNum <= extensionCount / 2) {
            increaseBase = 1.5 + random.nextDouble() * 1.5; // 1.5-3%
          } else {
            increaseBase = 2.0 + random.nextDouble() * 3.0; // 2-5%
          }
          BigDecimal increasePercent =
              BigDecimal.valueOf(increaseBase).setScale(4, RoundingMode.HALF_UP);
          BigDecimal newRentMajor =
              currentRentMajor
                  .multiply(
                      BigDecimal.ONE.add(
                          increasePercent.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)))
                  .setScale(2, RoundingMode.HALF_UP);

          // Convert to minor units (cents) for BIGINT storage
          long previousRentMinor = currentRentMajor.movePointRight(2).longValue();
          long newRentMinor = newRentMajor.movePointRight(2).longValue();

          // Determine status based on extension number and contract status
          String status;
          LocalDateTime activatedAt = null;
          UUID activatedBy = null;
          LocalDateTime supersededAt = null;

          if (extNum < extensionCount) {
            // Earlier extensions are SUPERSEDED
            status = "SUPERSEDED";
            activatedAt = now.minusMonths((long) (extensionCount - extNum) * 12 + 6);
            activatedBy = createdBy;
            supersededAt = now.minusMonths((long) (extensionCount - extNum) * 12);
          } else if ("ACTIVE".equals(contractStatus)) {
            // Latest extension on active contract — pick ACTIVE or DRAFT
            if (random.nextBoolean()) {
              status = "ACTIVE";
              activatedAt = now.minusMonths(random.nextInt(1, 6));
              activatedBy = createdBy;
            } else {
              status = "DRAFT";
            }
          } else {
            // Latest extension on expired contract — DECLINED or SUPERSEDED
            if (random.nextBoolean()) {
              status = "DECLINED";
            } else {
              status = "SUPERSEDED";
              activatedAt = now.minusMonths(random.nextInt(12, 24));
              activatedBy = createdBy;
              supersededAt = now.minusMonths(random.nextInt(1, 12));
            }
          }

          String notes = buildNotes(extNum, increasePercent, status, previousEndDate);
          String declinedReason =
              "DECLINED".equals(status) ? "Tenant declined the proposed rent increase" : null;
          LocalDateTime createdAt = now.minusDays(random.nextInt(30, 365));

          records.add(
              new Object[] {
                UUID.randomUUID(),
                newContractExtensionId().value(),
                teamId,
                contractId,
                extNum,
                previousEndDate,
                newEndDate,
                previousRentMinor,
                currency,
                newRentMinor,
                currency,
                "FIXED_PERCENTAGE",
                increasePercent,
                status,
                triggerType,
                notes,
                declinedReason,
                activatedAt,
                activatedBy,
                supersededAt,
                createdAt,
                now,
                createdBy,
                createdBy
              });

          extensionsCreated++;

          // Advance for next extension
          previousEndDate = newEndDate;
          currentRentMajor = newRentMajor;
        }
      }

      if (!records.isEmpty()) {
        var insert =
            dsl.insertInto(table("contract_extensions"))
                .columns(
                    field("id", UUID.class),
                    field("identifier", String.class),
                    field("team_id", UUID.class),
                    field("contract_id", UUID.class),
                    field("extension_number", Integer.class),
                    field("previous_end_date", LocalDate.class),
                    field("new_end_date", LocalDate.class),
                    field("previous_rent_amount", Long.class),
                    field("previous_rent_currency", String.class),
                    field("new_rent_amount", Long.class),
                    field("new_rent_currency", String.class),
                    field("rent_adjustment_type", String.class),
                    field("rent_adjustment_value", BigDecimal.class),
                    field("status", String.class),
                    field("trigger_type", String.class),
                    field("notes", String.class),
                    field("declined_reason", String.class),
                    field("activated_at", LocalDateTime.class),
                    field("activated_by", UUID.class),
                    field("superseded_at", LocalDateTime.class),
                    field("created_at", LocalDateTime.class),
                    field("updated_at", LocalDateTime.class),
                    field("created_by", UUID.class),
                    field("updated_by", UUID.class))
                .values(
                    (UUID) null, (String) null, (UUID) null, (UUID) null, (Integer) null,
                    (LocalDate) null, (LocalDate) null, (Long) null, (String) null, (Long) null,
                    (String) null, (String) null, (BigDecimal) null, (String) null, (String) null,
                    (String) null, (String) null, (LocalDateTime) null, (UUID) null,
                    (LocalDateTime) null, (LocalDateTime) null, (LocalDateTime) null, (UUID) null,
                    (UUID) null);
        var batch = dsl.batch(insert);
        for (Object[] r : records) {
          batch = batch.bind(r);
        }
        batch.execute();
      }

      log.info("Created {} contract extensions for team {}", extensionsCreated, teamKey);
    }
  }

  private String buildNotes(
      int extensionNumber, BigDecimal increasePercent, String status, LocalDate previousEndDate) {
    String yearContext = previousEndDate != null ? " (" + previousEndDate.getYear() + ")" : "";
    return switch (status) {
      case "ACTIVE" ->
          String.format(
              "Extension #%d%s — rent increased by %s%%",
              extensionNumber, yearContext, increasePercent.setScale(2, RoundingMode.HALF_UP));
      case "SUPERSEDED" ->
          String.format(
              "Extension #%d%s — superseded by subsequent renewal", extensionNumber, yearContext);
      case "DRAFT" ->
          String.format(
              "Extension #%d%s — pending approval, proposed %s%% increase",
              extensionNumber, yearContext, increasePercent.setScale(2, RoundingMode.HALF_UP));
      case "DECLINED" ->
          String.format("Extension #%d%s — declined by tenant", extensionNumber, yearContext);
      default -> String.format("Extension #%d%s", extensionNumber, yearContext);
    };
  }
}
