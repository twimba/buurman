package com.buurman.service.demo;

import static com.buurman.jooq.generated.Tables.CONTRACTS;
import static com.buurman.jooq.generated.Tables.PAYMENTS;
import static com.buurman.jooq.generated.Tables.PAYMENT_RECEIVALS;
import static com.buurman.util.SidGenerator.newPaymentId;
import static com.buurman.util.SidGenerator.newPaymentReceivalId;

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

      List<Object[]> paymentRecords = new ArrayList<>();
      List<Object[]> receivalRecords = new ArrayList<>();
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
        BigDecimal rentAmount = contract.get(CONTRACTS.RENT_AMOUNT);
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
            long monthsAgo =
                ChronoUnit.MONTHS.between(dueDate.atStartOfDay(), today.atStartOfDay());
            int roll = random.nextInt(100);
            if (monthsAgo > 60) {
              // > 5 years: nearly all paid
              paymentStatus = roll < 98 ? "PAID" : "LATE";
              if ("LATE".equals(paymentStatus)) {
                paymentDate = dueDate.plusDays(random.nextInt(5, 30));
              }
            } else if (monthsAgo > 12) {
              // 1-5 years: mostly paid
              if (roll < 92) {
                paymentStatus = "PAID";
              } else if (roll < 97) {
                paymentStatus = "LATE";
                paymentDate = dueDate.plusDays(random.nextInt(5, 30));
              } else if (roll < 99) {
                paymentStatus = "OVERDUE";
              } else {
                paymentStatus = "PARTIALLY_PAID";
              }
            } else {
              // Last year
              if (roll < 85) {
                paymentStatus = "PAID";
              } else if (roll < 93) {
                paymentStatus = "LATE";
                paymentDate = dueDate.plusDays(random.nextInt(3, 15));
              } else if (roll < 98) {
                paymentStatus = "OVERDUE";
              } else {
                paymentStatus = "PARTIALLY_PAID";
              }
            }

            if ("PAID".equals(paymentStatus) && paymentDate == null) {
              paymentDate = dueDate.plusDays(random.nextInt(0, 5));
            }
            if ("PARTIALLY_PAID".equals(paymentStatus)) {
              paymentDate = dueDate.plusDays(random.nextInt(0, 10));
            }
            // EXPIRED contracts: override all to PAID (contact fulfilled obligations)
            if ("EXPIRED".equals(status)) {
              paymentStatus = "PAID";
              paymentDate = dueDate.plusDays(random.nextInt(0, 3));
            }
          } else if (isCurrentMonth) {
            paymentStatus = random.nextInt(3) == 0 ? "PENDING" : "PAID";
            if ("PAID".equals(paymentStatus)) {
              paymentDate = dueDate.plusDays(random.nextInt(0, 3));
            }
          } else {
            paymentStatus = "PENDING";
          }

          LocalDateTime paymentCreatedAt = dueDate.atStartOfDay().minusDays(random.nextInt(0, 7));
          if (paymentCreatedAt.isAfter(now)) {
            paymentCreatedAt = now;
          }

          paymentRecords.add(
              new Object[] {
                paymentId,
                newPaymentId(),
                teamId,
                contractId,
                rentAmount,
                currency,
                dueDate,
                paymentDate,
                paymentStatus,
                true,
                paymentCreatedAt,
                now,
                createdBy,
                createdBy
              });

          paymentIds.add(paymentId);
          ctx.incrementPayments();
          teamPayments++;

          // Collect payment receival for PAID, PARTIALLY_PAID, or LATE payments
          if ("PAID".equals(paymentStatus)) {
            receivalRecords.add(
                new Object[] {
                  UUID.randomUUID(),
                  newPaymentReceivalId(),
                  teamId,
                  paymentId,
                  rentAmount,
                  currency,
                  paymentDate,
                  now,
                  now,
                  createdBy,
                  createdBy
                });
          } else if ("PARTIALLY_PAID".equals(paymentStatus)) {
            BigDecimal partialAmount =
                rentAmount.multiply(new BigDecimal("0.6")).setScale(0, RoundingMode.HALF_UP);
            receivalRecords.add(
                new Object[] {
                  UUID.randomUUID(),
                  newPaymentReceivalId(),
                  teamId,
                  paymentId,
                  partialAmount,
                  currency,
                  paymentDate,
                  now,
                  now,
                  createdBy,
                  createdBy
                });
          } else if ("LATE".equals(paymentStatus) && paymentDate != null) {
            receivalRecords.add(
                new Object[] {
                  UUID.randomUUID(),
                  newPaymentReceivalId(),
                  teamId,
                  paymentId,
                  rentAmount,
                  currency,
                  paymentDate,
                  now,
                  now,
                  createdBy,
                  createdBy
                });
          }

          dueDate = dueDate.plusMonths(periodMonths);
        }

        ctx.getPaymentIdsByContract().put(contractId, paymentIds);
      }

      // Batch insert all payments for this team
      if (!paymentRecords.isEmpty()) {
        var insert =
            dsl.insertInto(PAYMENTS)
                .columns(
                    PAYMENTS.ID,
                    PAYMENTS.IDENTIFIER,
                    PAYMENTS.TEAM_ID,
                    PAYMENTS.CONTRACT_ID,
                    PAYMENTS.AMOUNT,
                    PAYMENTS.CURRENCY,
                    PAYMENTS.DUE_DATE,
                    PAYMENTS.PAYMENT_DATE,
                    PAYMENTS.STATUS,
                    PAYMENTS.AUTO_GENERATED,
                    PAYMENTS.CREATED_AT,
                    PAYMENTS.UPDATED_AT,
                    PAYMENTS.CREATED_BY,
                    PAYMENTS.UPDATED_BY)
                .values(
                    (UUID) null, null, null, null, null, null, null, null, null, null, null, null,
                    null, null);
        var batch = dsl.batch(insert);
        for (Object[] r : paymentRecords) {
          batch = batch.bind(r);
        }
        batch.execute();
      }

      // Batch insert all payment receivals for this team
      if (!receivalRecords.isEmpty()) {
        var insert =
            dsl.insertInto(PAYMENT_RECEIVALS)
                .columns(
                    PAYMENT_RECEIVALS.ID,
                    PAYMENT_RECEIVALS.IDENTIFIER,
                    PAYMENT_RECEIVALS.TEAM_ID,
                    PAYMENT_RECEIVALS.PAYMENT_ID,
                    PAYMENT_RECEIVALS.AMOUNT,
                    PAYMENT_RECEIVALS.CURRENCY,
                    PAYMENT_RECEIVALS.RECEIVAL_DATE,
                    PAYMENT_RECEIVALS.CREATED_AT,
                    PAYMENT_RECEIVALS.UPDATED_AT,
                    PAYMENT_RECEIVALS.CREATED_BY,
                    PAYMENT_RECEIVALS.UPDATED_BY)
                .values(
                    (UUID) null, null, null, null, null, null, null, null, null, null, null);
        var batch = dsl.batch(insert);
        for (Object[] r : receivalRecords) {
          batch = batch.bind(r);
        }
        batch.execute();
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
