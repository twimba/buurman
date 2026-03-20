package com.buurman.service.demo;

import static com.buurman.jooq.generated.Tables.FINANCING_PAYMENTS;
import static com.buurman.jooq.generated.Tables.PROPERTY_FINANCINGS;
import static com.buurman.util.SidGenerator.newFinancingPaymentId;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
public class DemoFinancingPaymentGenerator {

  private final DSLContext dsl;
  private final Clock clock;
  private final Random random = new Random(42);

  public void generate(DemoDataContext ctx) {
    LocalDateTime now = LocalDateTime.now(clock);
    LocalDate today = LocalDate.now(clock);
    int totalPayments = 0;

    for (var teamEntry : ctx.getTeamIds().entrySet()) {
      String teamKey = teamEntry.getKey();
      UUID teamId = teamEntry.getValue();
      UUID createdBy = ctx.getAdminUserForTeam(teamKey).orElse(null);

      List<UUID> financingIds = ctx.getFinancingIdsByTeam().get(teamId);
      if (financingIds == null || financingIds.isEmpty()) {
        continue;
      }

      for (UUID financingId : financingIds) {
        Record financing =
            dsl.select(
                    PROPERTY_FINANCINGS.INTEREST_RATE,
                    PROPERTY_FINANCINGS.MONTHLY_PAYMENT,
                    PROPERTY_FINANCINGS.ORIGINAL_AMOUNT_CURRENCY,
                    PROPERTY_FINANCINGS.CURRENT_BALANCE,
                    PROPERTY_FINANCINGS.ORIGINAL_AMOUNT,
                    PROPERTY_FINANCINGS.STATUS,
                    PROPERTY_FINANCINGS.START_DATE)
                .from(PROPERTY_FINANCINGS)
                .where(PROPERTY_FINANCINGS.ID.eq(financingId))
                .fetchOne();

        if (financing == null) {
          continue;
        }

        String status = financing.get(PROPERTY_FINANCINGS.STATUS);
        if ("REFINANCED".equals(status)) {
          // Skip refinanced financings — payments belong to the new one
          continue;
        }
        if ("COMPLETED".equals(status)) {
          // Skip completed financings — mortgage is fully paid off
          continue;
        }

        BigDecimal interestRateBd = financing.get(PROPERTY_FINANCINGS.INTEREST_RATE);
        BigDecimal monthlyPaymentBd = financing.get(PROPERTY_FINANCINGS.MONTHLY_PAYMENT);
        String paymentCurrency = financing.get(PROPERTY_FINANCINGS.ORIGINAL_AMOUNT_CURRENCY);
        BigDecimal currentBalanceBd = financing.get(PROPERTY_FINANCINGS.CURRENT_BALANCE);

        double annualRate = interestRateBd != null ? interestRateBd.doubleValue() : 3.5;
        double monthlyRate = annualRate / 100.0 / 12.0;
        long monthlyPayment = monthlyPaymentBd != null ? monthlyPaymentBd.longValue() : 100_000L;

        // Use original amount as initial balance for full history
        BigDecimal originalAmountBd = financing.get(PROPERTY_FINANCINGS.ORIGINAL_AMOUNT);
        long balance =
            originalAmountBd != null ? originalAmountBd.longValue() : monthlyPayment * 300L;

        // Calculate months from financing start date to today + 2 future months
        LocalDate startDate = financing.get(PROPERTY_FINANCINGS.START_DATE);
        if (startDate == null) {
          startDate = today.minusMonths(24);
        }
        long totalMonths = ChronoUnit.MONTHS.between(startDate, today) + 2;

        // Generate full payment history from start date
        int teamPayments = 0;
        for (long m = 0; m <= totalMonths; m++) {
          // Stop generating once the loan is fully paid off
          if (balance <= 0 && m > 0) {
            break;
          }

          LocalDate paymentDate = startDate.plusMonths(m).withDayOfMonth(1);
          boolean isFuture = paymentDate.isAfter(today);

          long interestAmount = (long) (balance * monthlyRate);
          long principalAmount = monthlyPayment - interestAmount;
          if (principalAmount < 0) {
            principalAmount = 0;
          }

          String paymentStatus;
          String notes = null;
          boolean balanceDeducted = false;

          if (isFuture) {
            paymentStatus = "SCHEDULED";
          } else {
            int roll = random.nextInt(100);
            if (roll < 95) {
              paymentStatus = "COMPLETED";
              balanceDeducted = true;
              balance = Math.max(0, balance - principalAmount);
            } else if (roll < 98) {
              paymentStatus = "LATE";
              balanceDeducted = true;
              balance = Math.max(0, balance - principalAmount);
              notes = "Payment received " + (5 + random.nextInt(11)) + " days late";
            } else {
              paymentStatus = "MISSED";
              notes = "Payment not received, follow-up sent";
              principalAmount = 0;
              interestAmount = 0;
            }
          }

          long totalAmount =
              "MISSED".equals(paymentStatus) ? monthlyPayment : principalAmount + interestAmount;

          dsl.insertInto(FINANCING_PAYMENTS)
              .set(FINANCING_PAYMENTS.ID, UUID.randomUUID())
              .set(FINANCING_PAYMENTS.IDENTIFIER, newFinancingPaymentId())
              .set(FINANCING_PAYMENTS.FINANCING_ID, financingId)
              .set(FINANCING_PAYMENTS.TEAM_ID, teamId)
              .set(FINANCING_PAYMENTS.PAYMENT_DATE, paymentDate)
              .set(FINANCING_PAYMENTS.TOTAL_AMOUNT, BigDecimal.valueOf(totalAmount))
              .set(FINANCING_PAYMENTS.PRINCIPAL_AMOUNT, BigDecimal.valueOf(principalAmount))
              .set(FINANCING_PAYMENTS.INTEREST_AMOUNT, BigDecimal.valueOf(interestAmount))
              .set(FINANCING_PAYMENTS.CURRENCY, paymentCurrency)
              .set(FINANCING_PAYMENTS.STATUS, paymentStatus)
              .set(FINANCING_PAYMENTS.BALANCE_DEDUCTED, balanceDeducted)
              .set(FINANCING_PAYMENTS.NOTES, notes)
              .set(FINANCING_PAYMENTS.CREATED_AT, now)
              .set(FINANCING_PAYMENTS.UPDATED_AT, now)
              .set(FINANCING_PAYMENTS.CREATED_BY, createdBy)
              .set(FINANCING_PAYMENTS.UPDATED_BY, createdBy)
              .execute();

          teamPayments++;
        }

        // Update financing current_balance to reflect actual amortization
        dsl.update(PROPERTY_FINANCINGS)
            .set(PROPERTY_FINANCINGS.CURRENT_BALANCE, BigDecimal.valueOf(balance))
            .where(PROPERTY_FINANCINGS.ID.eq(financingId))
            .execute();

        totalPayments += teamPayments;
      }

      log.info("Created financing payments for team {}", teamKey);
    }

    log.info("Created {} total financing payments", totalPayments);
  }
}
