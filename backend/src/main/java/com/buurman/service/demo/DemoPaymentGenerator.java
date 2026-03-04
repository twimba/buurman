package com.buurman.service.demo;

import static com.buurman.jooq.generated.Tables.CONTRACTS;
import static com.buurman.jooq.generated.Tables.PAYMENTS;
import static com.buurman.jooq.generated.Tables.PAYMENT_RECEIVALS;
import static com.buurman.util.SidGenerator.newPaymentId;
import static com.buurman.util.SidGenerator.newPaymentReceivalId;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
public class DemoPaymentGenerator {

  private final DSLContext dsl;
  private final Clock clock;
  private final Random random = new Random(42);

  public void generate(DemoDataContext ctx) {
    LocalDateTime now = LocalDateTime.now(clock);
    LocalDate today = LocalDate.now(clock);

    for (var teamEntry : ctx.getTeamIds().entrySet()) {
      String teamKey = teamEntry.getKey();
      UUID teamId = teamEntry.getValue();
      UUID createdBy = ctx.getAdminUserForTeam(teamKey).orElse(null);
      String currency = ctx.getCurrencyForTeam(teamKey);
      List<UUID> contractIds = ctx.getContractIdsByTeam().get(teamId);

      if (contractIds == null) {
        continue;
      }

      int teamPayments = 0;

      for (UUID contractId : contractIds) {
        // Fetch contract details
        Record contract = dsl.selectFrom(CONTRACTS).where(CONTRACTS.ID.eq(contractId)).fetchOne();

        if (contract == null) {
          continue;
        }

        String status = contract.get(CONTRACTS.STATUS);
        if ("DRAFT".equals(status)) {
          continue; // No payments for drafts
        }

        LocalDate startDate = contract.get(CONTRACTS.START_DATE);
        Long rentAmount = contract.get(CONTRACTS.RENT_AMOUNT);
        String frequency = contract.get(CONTRACTS.PAYMENT_FREQUENCY);
        int periodMonths = periodMonthsForFrequency(frequency);

        // Determine end date for payment generation
        LocalDate paymentEndDate;
        if ("EXPIRED".equals(status) || "TERMINATED".equals(status)) {
          paymentEndDate = contract.get(CONTRACTS.END_DATE);
          if (paymentEndDate == null) {
            paymentEndDate = today;
          }
        } else {
          // ACTIVE: generate payments up to 3 months in the future
          paymentEndDate = today.plusMonths(3);
          LocalDate contractEnd = contract.get(CONTRACTS.END_DATE);
          if (contractEnd != null && contractEnd.isBefore(paymentEndDate)) {
            paymentEndDate = contractEnd;
          }
        }

        // Generate payments at the correct frequency
        LocalDate dueDate = startDate.withDayOfMonth(1);
        if (dueDate.isBefore(startDate)) {
          dueDate = dueDate.plusMonths(1);
        }

        List<UUID> paymentIds = new ArrayList<>();

        while (!dueDate.isAfter(paymentEndDate)) {
          UUID paymentId = UUID.randomUUID();
          boolean isPast = dueDate.isBefore(today);
          boolean isCurrentMonth =
              dueDate.getMonth() == today.getMonth() && dueDate.getYear() == today.getYear();

          String paymentStatus;
          LocalDate paymentDate = null;

          if (isPast && !isCurrentMonth) {
            // Past payments
            if ("EXPIRED".equals(status)) {
              paymentStatus = "PAID";
            } else {
              int roll = random.nextInt(100);
              if (roll < 80) {
                paymentStatus = "PAID";
              } else if (roll < 95) {
                paymentStatus = "OVERDUE";
              } else {
                paymentStatus = "PARTIALLY_PAID";
              }
            }
            if ("PAID".equals(paymentStatus) || "PARTIALLY_PAID".equals(paymentStatus)) {
              paymentDate = dueDate.plusDays(random.nextInt(0, 5));
            }
          } else if (isCurrentMonth) {
            paymentStatus = random.nextInt(3) == 0 ? "PENDING" : "PAID";
            if ("PAID".equals(paymentStatus)) {
              paymentDate = dueDate.plusDays(random.nextInt(0, 3));
            }
          } else {
            paymentStatus = "PENDING";
          }

          dsl.insertInto(PAYMENTS)
              .set(PAYMENTS.ID, paymentId)
              .set(PAYMENTS.IDENTIFIER, newPaymentId())
              .set(PAYMENTS.TEAM_ID, teamId)
              .set(PAYMENTS.CONTRACT_ID, contractId)
              .set(PAYMENTS.AMOUNT, rentAmount)
              .set(PAYMENTS.CURRENCY, currency)
              .set(PAYMENTS.DUE_DATE, dueDate)
              .set(PAYMENTS.PAYMENT_DATE, paymentDate)
              .set(PAYMENTS.STATUS, paymentStatus)
              .set(PAYMENTS.AUTO_GENERATED, true)
              .set(PAYMENTS.CREATED_AT, now.minusDays(random.nextInt(1, 30)))
              .set(PAYMENTS.UPDATED_AT, now)
              .set(PAYMENTS.CREATED_BY, createdBy)
              .set(PAYMENTS.UPDATED_BY, createdBy)
              .execute();

          paymentIds.add(paymentId);
          ctx.incrementPayments();
          teamPayments++;

          // Create payment receival for PAID payments
          if ("PAID".equals(paymentStatus)) {
            dsl.insertInto(PAYMENT_RECEIVALS)
                .set(PAYMENT_RECEIVALS.ID, UUID.randomUUID())
                .set(PAYMENT_RECEIVALS.IDENTIFIER, newPaymentReceivalId())
                .set(PAYMENT_RECEIVALS.TEAM_ID, teamId)
                .set(PAYMENT_RECEIVALS.PAYMENT_ID, paymentId)
                .set(PAYMENT_RECEIVALS.AMOUNT, rentAmount)
                .set(PAYMENT_RECEIVALS.CURRENCY, currency)
                .set(PAYMENT_RECEIVALS.RECEIVAL_DATE, paymentDate)
                .set(PAYMENT_RECEIVALS.CREATED_AT, now)
                .set(PAYMENT_RECEIVALS.UPDATED_AT, now)
                .set(PAYMENT_RECEIVALS.CREATED_BY, createdBy)
                .set(PAYMENT_RECEIVALS.UPDATED_BY, createdBy)
                .execute();
          } else if ("PARTIALLY_PAID".equals(paymentStatus)) {
            long partialAmount = (long) (rentAmount * 0.6);
            dsl.insertInto(PAYMENT_RECEIVALS)
                .set(PAYMENT_RECEIVALS.ID, UUID.randomUUID())
                .set(PAYMENT_RECEIVALS.IDENTIFIER, newPaymentReceivalId())
                .set(PAYMENT_RECEIVALS.TEAM_ID, teamId)
                .set(PAYMENT_RECEIVALS.PAYMENT_ID, paymentId)
                .set(PAYMENT_RECEIVALS.AMOUNT, partialAmount)
                .set(PAYMENT_RECEIVALS.CURRENCY, currency)
                .set(PAYMENT_RECEIVALS.RECEIVAL_DATE, paymentDate)
                .set(PAYMENT_RECEIVALS.CREATED_AT, now)
                .set(PAYMENT_RECEIVALS.UPDATED_AT, now)
                .set(PAYMENT_RECEIVALS.CREATED_BY, createdBy)
                .set(PAYMENT_RECEIVALS.UPDATED_BY, createdBy)
                .execute();
          }

          dueDate = dueDate.plusMonths(periodMonths);
        }

        ctx.getPaymentIdsByContract().put(contractId, paymentIds);
      }

      log.info("Created {} payments for team {}", teamPayments, teamKey);
    }
  }

  private int periodMonthsForFrequency(String frequency) {
    if (frequency == null) {
      return 1;
    }
    return switch (frequency) {
      case "QUARTERLY" -> 3;
      case "ANNUALLY" -> 12;
      default -> 1; // MONTHLY
    };
  }
}
