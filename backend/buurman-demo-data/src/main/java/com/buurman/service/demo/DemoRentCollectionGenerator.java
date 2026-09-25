package com.buurman.service.demo;

import static com.buurman.jooq.generated.Tables.CONTACTS;
import static com.buurman.jooq.generated.Tables.CONTACT_CREDITS;
import static com.buurman.jooq.generated.Tables.CONTRACTS;
import static com.buurman.jooq.generated.Tables.CONTRACT_PARTIES;
import static com.buurman.jooq.generated.Tables.DEPOSITS;
import static com.buurman.jooq.generated.Tables.DEPOSIT_DEDUCTIONS;
import static com.buurman.jooq.generated.Tables.PAYMENTS;
import static com.buurman.jooq.generated.Tables.PAYMENT_PLANS;
import static com.buurman.jooq.generated.Tables.PAYMENT_RECEIVALS;
import static com.buurman.jooq.generated.Tables.PAYMENT_REMINDERS;
import static com.buurman.jooq.generated.Tables.RENT_REGULATION_COUNTRIES;
import static com.buurman.util.SidGenerator.newContactCreditId;
import static com.buurman.util.SidGenerator.newDepositDeductionId;
import static com.buurman.util.SidGenerator.newDepositId;
import static com.buurman.util.SidGenerator.newPaymentId;
import static com.buurman.util.SidGenerator.newPaymentPlanId;
import static com.buurman.util.SidGenerator.newPaymentReceivalId;
import static com.buurman.util.SidGenerator.newPaymentReminderId;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.buurman.util.CurrencyUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Demo data for the rent collection features: tenant reminder opt-in, the dunning ladder, late
 * fees, deposits with deductions, payment plans and tenant credits.
 *
 * <p>Late fees are only switched on for contracts whose country actually permits them, so the demo
 * also shows the regulation guard working rather than charging fees where they would be void.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DemoRentCollectionGenerator {

  /** Statuses that mean a payment is still owed. */
  private static final List<String> OPEN = List.of("PENDING", "PARTIALLY_PAID", "OVERDUE");

  private final DSLContext dsl;
  private final Clock clock;
  private final Random random = new Random(4242);

  public void generate(DemoDataContext ctx) {
    LocalDate today = LocalDate.now(clock);
    LocalDateTime now = LocalDateTime.now(clock);
    Map<String, LateFeeRule> rules = loadLateFeeRules();

    for (var teamEntry : ctx.getTeamIds().entrySet()) {
      String teamKey = teamEntry.getKey();
      UUID teamId = teamEntry.getValue();
      UUID actor = ctx.getAdminUserForTeam(teamKey).orElse(null);
      if (actor == null) {
        continue;
      }
      String currency = ctx.getCurrencyForTeam(teamKey);

      List<Record> contracts =
          dsl.select(
                  CONTRACTS.ID,
                  CONTRACTS.STATUS,
                  CONTRACTS.COUNTRY_CODE,
                  CONTRACTS.RENT_AMOUNT,
                  CONTRACTS.SECURITY_DEPOSIT,
                  CONTRACTS.START_DATE,
                  CONTRACTS.END_DATE)
              .from(CONTRACTS)
              .where(CONTRACTS.TEAM_ID.eq(teamId).and(CONTRACTS.DELETED_AT.isNull()))
              .fetch()
              .map(r -> (Record) r);
      if (contracts.isEmpty()) {
        continue;
      }

      optInTenantsToReminders(teamId, actor, now);
      Counts counts = new Counts();

      for (int i = 0; i < contracts.size(); i++) {
        Record contract = contracts.get(i);
        UUID contractId = contract.get(CONTRACTS.ID);
        String status = contract.get(CONTRACTS.STATUS);
        BigDecimal rent = contract.get(CONTRACTS.RENT_AMOUNT);
        if ("DRAFT".equals(status) || rent == null) {
          continue;
        }
        Optional<UUID> primaryContact = findPrimaryContact(contractId, teamId);

        LateFeeRule rule = rules.get(contract.get(CONTRACTS.COUNTRY_CODE));
        boolean lateFeesOn = enableLateFees(contractId, rule, i, actor, now);
        boolean remindersOn = enableTenantReminders(contractId, status, i, actor, now);

        // Every third active contract carries a deposit; the rest show the "not recorded" state.
        if (i % 3 == 0) {
          createDeposit(
              teamId, contractId, primaryContact, contract, currency, actor, today, now, counts);
        }

        List<Record> openPayments = openRentPayments(contractId, teamId, today);
        if (lateFeesOn) {
          counts.lateFees +=
              chargeLateFees(
                  teamId, contractId, openPayments, rule, rent, currency, actor, today, now);
        }
        if (remindersOn && primaryContact.isPresent()) {
          counts.reminders +=
              sendReminders(
                  teamId, openPayments, primaryContact.get(), currency, actor, today, now);
        }
        // One contract per team gets a payment plan over its oldest arrears.
        if (i == 1 && openPayments.size() >= 3) {
          counts.plans +=
              createPaymentPlan(
                  teamId, contractId, primaryContact, openPayments, currency, actor, today, now);
        }
        // And one gets a pair of tenant credits.
        if (i == 2 && primaryContact.isPresent()) {
          counts.credits +=
              createCredits(teamId, contractId, primaryContact.get(), rent, currency, actor, now);
        }
      }

      log.info(
          "Created rent-collection demo data for team {} — {} deposits, {} late fees, {} reminders,"
              + " {} plans, {} credits",
          teamKey,
          counts.deposits,
          counts.lateFees,
          counts.reminders,
          counts.plans,
          counts.credits);
    }
  }

  // ── Feature opt-in ──────────────────────────────────────────────────────

  /** Most tenants consent to reminders; the rest exercise the opted-out path. */
  private void optInTenantsToReminders(UUID teamId, UUID actor, LocalDateTime now) {
    List<UUID> contactIds =
        dsl.select(CONTACTS.ID)
            .from(CONTACTS)
            .where(CONTACTS.TEAM_ID.eq(teamId).and(CONTACTS.DELETED_AT.isNull()))
            .fetch(CONTACTS.ID);
    for (int i = 0; i < contactIds.size(); i++) {
      if (i % 10 < 7) {
        dsl.update(CONTACTS)
            .set(CONTACTS.PAYMENT_REMINDERS_ENABLED, true)
            .set(CONTACTS.UPDATED_AT, now)
            .set(CONTACTS.UPDATED_BY, actor)
            .where(CONTACTS.ID.eq(contactIds.get(i)))
            .execute();
      }
    }
  }

  /** Enables late fees only where the jurisdiction allows them, clamped to any statutory cap. */
  private boolean enableLateFees(
      UUID contractId, @Nullable LateFeeRule rule, int index, UUID actor, LocalDateTime now) {
    if (rule == null || !rule.permitsFee() || index % 2 != 0) {
      return false;
    }
    BigDecimal percentage = rule.clamp(new BigDecimal("5.00"));
    dsl.update(CONTRACTS)
        .set(CONTRACTS.LATE_FEE_ENABLED, true)
        .set(CONTRACTS.LATE_FEE_PERCENTAGE, percentage)
        .set(CONTRACTS.LATE_FEE_GRACE_DAYS, 5)
        .set(CONTRACTS.UPDATED_AT, now)
        .set(CONTRACTS.UPDATED_BY, actor)
        .where(CONTRACTS.ID.eq(contractId))
        .execute();
    return true;
  }

  private boolean enableTenantReminders(
      UUID contractId, String status, int index, UUID actor, LocalDateTime now) {
    if (!"ACTIVE".equals(status) || index % 10 >= 6) {
      return false;
    }
    dsl.update(CONTRACTS)
        .set(CONTRACTS.TENANT_REMINDERS_ENABLED, true)
        .set(CONTRACTS.UPDATED_AT, now)
        .set(CONTRACTS.UPDATED_BY, actor)
        .where(CONTRACTS.ID.eq(contractId))
        .execute();
    return true;
  }

  // ── Deposits ────────────────────────────────────────────────────────────

  private void createDeposit(
      UUID teamId,
      UUID contractId,
      Optional<UUID> contactId,
      Record contract,
      String currency,
      UUID actor,
      LocalDate today,
      LocalDateTime now,
      Counts counts) {
    BigDecimal agreed =
        Optional.ofNullable(contract.get(CONTRACTS.SECURITY_DEPOSIT))
            .filter(d -> d.signum() > 0)
            .orElseGet(() -> contract.get(CONTRACTS.RENT_AMOUNT).multiply(new BigDecimal("2")));
    LocalDate start = contract.get(CONTRACTS.START_DATE);
    LocalDate end = contract.get(CONTRACTS.END_DATE);
    boolean tenancyOver = end != null && end.isBefore(today);

    String status;
    BigDecimal returned = BigDecimal.ZERO;
    LocalDate returnedDate = null;
    if (!tenancyOver) {
      status = "HELD";
    } else if (random.nextInt(10) == 0) {
      status = "FORFEITED";
    } else {
      status = "RETURNED";
      returnedDate = end.plusDays(random.nextInt(5, 25));
    }

    UUID depositId = UUID.randomUUID();
    // A third of deposits carry a deduction, so the statement PDF has something to itemise.
    List<Object[]> deductions = new ArrayList<>();
    if (random.nextInt(3) == 0) {
      BigDecimal amount =
          agreed.multiply(new BigDecimal("0.15")).setScale(scale(currency), RoundingMode.HALF_UP);
      deductions.add(
          new Object[] {
            amount, deductionReason(), (tenancyOver ? end : today).minusDays(random.nextInt(1, 20))
          });
    }
    BigDecimal deducted =
        deductions.stream().map(d -> (BigDecimal) d[0]).reduce(BigDecimal.ZERO, BigDecimal::add);
    if ("RETURNED".equals(status)) {
      returned = agreed.subtract(deducted).max(BigDecimal.ZERO);
    }

    dsl.insertInto(DEPOSITS)
        .set(DEPOSITS.ID, depositId)
        .set(DEPOSITS.IDENTIFIER, newDepositId())
        .set(DEPOSITS.TEAM_ID, teamId)
        .set(DEPOSITS.CONTRACT_ID, contractId)
        .set(DEPOSITS.CONTACT_ID, contactId.orElse(null))
        .set(DEPOSITS.AMOUNT, agreed)
        .set(DEPOSITS.CURRENCY, currency)
        .set(DEPOSITS.RECEIVED_DATE, start)
        .set(DEPOSITS.HELD_WHERE, heldWhere())
        .set(DEPOSITS.STATUS, status)
        .set(DEPOSITS.RETURN_DUE_DATE, end != null ? end.plusDays(30) : null)
        .set(DEPOSITS.RETURNED_DATE, returnedDate)
        .set(DEPOSITS.RETURNED_AMOUNT, minor(returned, currency))
        .set(DEPOSITS.CREATED_AT, now)
        .set(DEPOSITS.UPDATED_AT, now)
        .set(DEPOSITS.CREATED_BY, actor)
        .set(DEPOSITS.UPDATED_BY, actor)
        .execute();
    counts.deposits++;

    for (Object[] d : deductions) {
      dsl.insertInto(DEPOSIT_DEDUCTIONS)
          .set(DEPOSIT_DEDUCTIONS.ID, UUID.randomUUID())
          .set(DEPOSIT_DEDUCTIONS.IDENTIFIER, newDepositDeductionId())
          .set(DEPOSIT_DEDUCTIONS.TEAM_ID, teamId)
          .set(DEPOSIT_DEDUCTIONS.DEPOSIT_ID, depositId)
          .set(DEPOSIT_DEDUCTIONS.AMOUNT, (BigDecimal) d[0])
          .set(DEPOSIT_DEDUCTIONS.CURRENCY, currency)
          .set(DEPOSIT_DEDUCTIONS.REASON, (String) d[1])
          .set(DEPOSIT_DEDUCTIONS.DEDUCTION_DATE, (LocalDate) d[2])
          .set(DEPOSIT_DEDUCTIONS.CREATED_AT, now)
          .set(DEPOSIT_DEDUCTIONS.UPDATED_AT, now)
          .set(DEPOSIT_DEDUCTIONS.CREATED_BY, actor)
          .set(DEPOSIT_DEDUCTIONS.UPDATED_BY, actor)
          .execute();
    }
  }

  // ── Late fees ───────────────────────────────────────────────────────────

  private int chargeLateFees(
      UUID teamId,
      UUID contractId,
      List<Record> openPayments,
      @Nullable LateFeeRule rule,
      BigDecimal rent,
      String currency,
      UUID actor,
      LocalDate today,
      LocalDateTime now) {
    int charged = 0;
    for (Record payment : openPayments) {
      LocalDate due = payment.get(PAYMENTS.DUE_DATE);
      if (!due.plusDays(5).isBefore(today)) {
        continue; // still inside the grace period
      }
      BigDecimal percentage =
          rule == null ? new BigDecimal("5.00") : rule.clamp(new BigDecimal("5.00"));
      BigDecimal fee =
          rent.multiply(percentage)
              .divide(new BigDecimal("100"), scale(currency), RoundingMode.HALF_UP);
      if (fee.signum() <= 0) {
        continue;
      }
      LocalDate feeDue = due.plusDays(6);
      dsl.insertInto(PAYMENTS)
          .set(PAYMENTS.ID, UUID.randomUUID())
          .set(PAYMENTS.IDENTIFIER, newPaymentId())
          .set(PAYMENTS.TEAM_ID, teamId)
          .set(PAYMENTS.CONTRACT_ID, contractId)
          .set(PAYMENTS.CONTACT_ID, payment.get(PAYMENTS.CONTACT_ID))
          .set(PAYMENTS.AMOUNT, fee)
          .set(PAYMENTS.CURRENCY, currency)
          .set(PAYMENTS.DUE_DATE, feeDue)
          .set(PAYMENTS.STATUS, "OVERDUE")
          .set(PAYMENTS.PAYMENT_TYPE, "LATE_FEE")
          .set(PAYMENTS.PARENT_PAYMENT_ID, payment.get(PAYMENTS.ID))
          .set(PAYMENTS.AUTO_GENERATED, true)
          .set(PAYMENTS.CREATED_AT, feeDue.atStartOfDay())
          .set(PAYMENTS.UPDATED_AT, now)
          .set(PAYMENTS.CREATED_BY, actor)
          .set(PAYMENTS.UPDATED_BY, actor)
          .execute();
      charged++;
      if (charged == 2) {
        break; // a couple per contract is enough to demonstrate
      }
    }
    return charged;
  }

  // ── Reminders ───────────────────────────────────────────────────────────

  /** A friendly nudge, then a firm one, then a final notice as arrears age. */
  private int sendReminders(
      UUID teamId,
      List<Record> openPayments,
      UUID contactId,
      String currency,
      UUID actor,
      LocalDate today,
      LocalDateTime now) {
    String email =
        dsl.select(CONTACTS.EMAIL)
            .from(CONTACTS)
            .where(CONTACTS.ID.eq(contactId))
            .fetchOptional(CONTACTS.EMAIL)
            .orElse(null);
    if (email == null) {
      return 0;
    }
    int sent = 0;
    for (Record payment : openPayments) {
      LocalDate due = payment.get(PAYMENTS.DUE_DATE);
      int daysOverdue = (int) Math.max(0, ChronoUnit.DAYS.between(due, today));
      if (daysOverdue < 3) {
        continue;
      }
      BigDecimal outstanding = payment.get(PAYMENTS.AMOUNT);
      // Ladder steps reached so far: day 0 friendly, day 7 firm, day 21 final.
      List<int[]> steps = new ArrayList<>();
      steps.add(new int[] {0});
      if (daysOverdue >= 7) {
        steps.add(new int[] {7});
      }
      if (daysOverdue >= 21) {
        steps.add(new int[] {21});
      }
      for (int[] step : steps) {
        int offset = step[0];
        String tone = offset >= 21 ? "FINAL" : offset >= 7 ? "FIRM" : "FRIENDLY";
        LocalDate sentOn = due.plusDays(offset);
        dsl.insertInto(PAYMENT_REMINDERS)
            .set(PAYMENT_REMINDERS.ID, UUID.randomUUID())
            .set(PAYMENT_REMINDERS.IDENTIFIER, newPaymentReminderId())
            .set(PAYMENT_REMINDERS.TEAM_ID, teamId)
            .set(PAYMENT_REMINDERS.PAYMENT_ID, payment.get(PAYMENTS.ID))
            .set(PAYMENT_REMINDERS.CONTACT_ID, contactId)
            .set(PAYMENT_REMINDERS.REMINDER_TYPE, "AUTOMATIC")
            .set(PAYMENT_REMINDERS.CHANNEL, "EMAIL")
            .set(PAYMENT_REMINDERS.RECIPIENT_EMAIL, email)
            .set(PAYMENT_REMINDERS.DAYS_OVERDUE, offset)
            .set(PAYMENT_REMINDERS.OUTSTANDING_AMOUNT, minor(outstanding, currency))
            .set(PAYMENT_REMINDERS.CURRENCY, currency)
            .set(PAYMENT_REMINDERS.STEP_OFFSET_DAYS, offset)
            .set(PAYMENT_REMINDERS.TONE, tone)
            .set(PAYMENT_REMINDERS.SENT_AT, sentOn.atTime(8, 30))
            .set(PAYMENT_REMINDERS.CREATED_AT, sentOn.atTime(8, 30))
            .set(PAYMENT_REMINDERS.UPDATED_AT, now)
            .set(PAYMENT_REMINDERS.CREATED_BY, actor)
            .set(PAYMENT_REMINDERS.UPDATED_BY, actor)
            .execute();
        sent++;
      }
      if (sent >= 4) {
        break;
      }
    }
    return sent;
  }

  // ── Payment plans ───────────────────────────────────────────────────────

  private int createPaymentPlan(
      UUID teamId,
      UUID contractId,
      Optional<UUID> contactId,
      List<Record> openPayments,
      String currency,
      UUID actor,
      LocalDate today,
      LocalDateTime now) {
    List<Record> covered = openPayments.subList(0, Math.min(2, openPayments.size()));
    BigDecimal total =
        covered.stream().map(p -> p.get(PAYMENTS.AMOUNT)).reduce(BigDecimal.ZERO, BigDecimal::add);
    if (total.signum() <= 0) {
      return 0;
    }
    int instalments = 3;
    LocalDate start = today.withDayOfMonth(1).plusMonths(1);
    UUID planId = UUID.randomUUID();
    dsl.insertInto(PAYMENT_PLANS)
        .set(PAYMENT_PLANS.ID, planId)
        .set(PAYMENT_PLANS.IDENTIFIER, newPaymentPlanId())
        .set(PAYMENT_PLANS.TEAM_ID, teamId)
        .set(PAYMENT_PLANS.CONTRACT_ID, contractId)
        .set(PAYMENT_PLANS.CONTACT_ID, contactId.orElse(null))
        .set(PAYMENT_PLANS.TOTAL_AMOUNT, total)
        .set(PAYMENT_PLANS.CURRENCY, currency)
        .set(PAYMENT_PLANS.INSTALMENT_COUNT, instalments)
        .set(PAYMENT_PLANS.START_DATE, start)
        .set(PAYMENT_PLANS.FREQUENCY, "MONTHLY")
        .set(PAYMENT_PLANS.STATUS, "ACTIVE")
        .set(PAYMENT_PLANS.NOTES, "Arrears spread over three months after a call with the tenant")
        .set(PAYMENT_PLANS.CREATED_AT, now)
        .set(PAYMENT_PLANS.UPDATED_AT, now)
        .set(PAYMENT_PLANS.CREATED_BY, actor)
        .set(PAYMENT_PLANS.UPDATED_BY, actor)
        .execute();

    // Settle the covered arrears with PLAN receivals, exactly as PaymentPlanService does, so the
    // debt lives on the instalments instead of being counted twice.
    for (Record payment : covered) {
      dsl.insertInto(PAYMENT_RECEIVALS)
          .set(PAYMENT_RECEIVALS.ID, UUID.randomUUID())
          .set(PAYMENT_RECEIVALS.IDENTIFIER, newPaymentReceivalId())
          .set(PAYMENT_RECEIVALS.TEAM_ID, teamId)
          .set(PAYMENT_RECEIVALS.PAYMENT_ID, payment.get(PAYMENTS.ID))
          .set(PAYMENT_RECEIVALS.AMOUNT, payment.get(PAYMENTS.AMOUNT))
          .set(PAYMENT_RECEIVALS.CURRENCY, currency)
          .set(PAYMENT_RECEIVALS.RECEIVAL_DATE, today)
          .set(PAYMENT_RECEIVALS.RECEIVAL_TYPE, "PLAN")
          .set(PAYMENT_RECEIVALS.PAYMENT_PLAN_ID, planId)
          .set(PAYMENT_RECEIVALS.NOTES, "Moved into a payment plan")
          .set(PAYMENT_RECEIVALS.CREATED_AT, now)
          .set(PAYMENT_RECEIVALS.UPDATED_AT, now)
          .set(PAYMENT_RECEIVALS.CREATED_BY, actor)
          .set(PAYMENT_RECEIVALS.UPDATED_BY, actor)
          .execute();
      dsl.update(PAYMENTS)
          .set(PAYMENTS.STATUS, "PAID")
          .set(PAYMENTS.PAYMENT_DATE, today)
          .set(PAYMENTS.UPDATED_AT, now)
          .set(PAYMENTS.UPDATED_BY, actor)
          .where(PAYMENTS.ID.eq(payment.get(PAYMENTS.ID)))
          .execute();
    }

    BigDecimal each = total.divide(new BigDecimal(instalments), scale(currency), RoundingMode.DOWN);
    BigDecimal remainder = total.subtract(each.multiply(new BigDecimal(instalments)));
    for (int n = 0; n < instalments; n++) {
      BigDecimal amount = n == instalments - 1 ? each.add(remainder) : each;
      dsl.insertInto(PAYMENTS)
          .set(PAYMENTS.ID, UUID.randomUUID())
          .set(PAYMENTS.IDENTIFIER, newPaymentId())
          .set(PAYMENTS.TEAM_ID, teamId)
          .set(PAYMENTS.CONTRACT_ID, contractId)
          .set(PAYMENTS.CONTACT_ID, contactId.orElse(null))
          .set(PAYMENTS.AMOUNT, amount)
          .set(PAYMENTS.CURRENCY, currency)
          .set(PAYMENTS.DUE_DATE, start.plusMonths(n))
          .set(PAYMENTS.STATUS, "PENDING")
          .set(PAYMENTS.PAYMENT_TYPE, "INSTALMENT")
          .set(PAYMENTS.PAYMENT_PLAN_ID, planId)
          .set(PAYMENTS.AUTO_GENERATED, true)
          .set(PAYMENTS.CREATED_AT, now)
          .set(PAYMENTS.UPDATED_AT, now)
          .set(PAYMENTS.CREATED_BY, actor)
          .set(PAYMENTS.UPDATED_BY, actor)
          .execute();
    }
    return 1;
  }

  // ── Credits ─────────────────────────────────────────────────────────────

  private int createCredits(
      UUID teamId,
      UUID contractId,
      UUID contactId,
      BigDecimal rent,
      String currency,
      UUID actor,
      LocalDateTime now) {
    BigDecimal overpayment =
        rent.multiply(new BigDecimal("0.20")).setScale(scale(currency), RoundingMode.HALF_UP);
    insertCredit(
        teamId,
        contractId,
        contactId,
        overpayment,
        overpayment,
        "OVERPAYMENT",
        "Tenant transferred more than the rent due",
        currency,
        actor,
        now);
    BigDecimal goodwill =
        rent.multiply(new BigDecimal("0.10")).setScale(scale(currency), RoundingMode.HALF_UP);
    insertCredit(
        teamId,
        contractId,
        contactId,
        goodwill,
        BigDecimal.ZERO,
        "CREDIT_NOTE",
        "Goodwill for the boiler outage in the first week",
        currency,
        actor,
        now);
    return 2;
  }

  private void insertCredit(
      UUID teamId,
      UUID contractId,
      UUID contactId,
      BigDecimal amount,
      BigDecimal remaining,
      String source,
      String reason,
      String currency,
      UUID actor,
      LocalDateTime now) {
    dsl.insertInto(CONTACT_CREDITS)
        .set(CONTACT_CREDITS.ID, UUID.randomUUID())
        .set(CONTACT_CREDITS.IDENTIFIER, newContactCreditId())
        .set(CONTACT_CREDITS.TEAM_ID, teamId)
        .set(CONTACT_CREDITS.CONTACT_ID, contactId)
        .set(CONTACT_CREDITS.CONTRACT_ID, contractId)
        .set(CONTACT_CREDITS.AMOUNT, amount)
        .set(CONTACT_CREDITS.REMAINING_AMOUNT, minor(remaining, currency))
        .set(CONTACT_CREDITS.CURRENCY, currency)
        .set(CONTACT_CREDITS.SOURCE, source)
        .set(CONTACT_CREDITS.REASON, reason)
        .set(CONTACT_CREDITS.CREATED_AT, now)
        .set(CONTACT_CREDITS.UPDATED_AT, now)
        .set(CONTACT_CREDITS.CREATED_BY, actor)
        .set(CONTACT_CREDITS.UPDATED_BY, actor)
        .execute();
  }

  // ── Helpers ─────────────────────────────────────────────────────────────

  private List<Record> openRentPayments(UUID contractId, UUID teamId, LocalDate today) {
    return dsl.select(PAYMENTS.ID, PAYMENTS.DUE_DATE, PAYMENTS.AMOUNT, PAYMENTS.CONTACT_ID)
        .from(PAYMENTS)
        .where(
            PAYMENTS
                .CONTRACT_ID
                .eq(contractId)
                .and(PAYMENTS.TEAM_ID.eq(teamId))
                .and(PAYMENTS.STATUS.in(OPEN))
                .and(PAYMENTS.PAYMENT_TYPE.eq("RENT"))
                .and(PAYMENTS.DUE_DATE.lt(today))
                .and(PAYMENTS.DELETED_AT.isNull()))
        .orderBy(PAYMENTS.DUE_DATE.asc())
        .fetch()
        .map(r -> (Record) r);
  }

  private Optional<UUID> findPrimaryContact(UUID contractId, UUID teamId) {
    return dsl.select(CONTRACT_PARTIES.CONTACT_ID)
        .from(CONTRACT_PARTIES)
        .where(
            CONTRACT_PARTIES
                .CONTRACT_ID
                .eq(contractId)
                .and(CONTRACT_PARTIES.TEAM_ID.eq(teamId))
                .and(CONTRACT_PARTIES.ROLE.eq("PRIMARY_TENANT"))
                .and(CONTRACT_PARTIES.DELETED_AT.isNull()))
        .limit(1)
        .fetchOptional(CONTRACT_PARTIES.CONTACT_ID);
  }

  /** The catalogue's late-fee regime per country, so demo contracts respect local law. */
  private Map<String, LateFeeRule> loadLateFeeRules() {
    Map<String, LateFeeRule> rules = new HashMap<>();
    dsl.select(
            RENT_REGULATION_COUNTRIES.COUNTRY_CODE,
            RENT_REGULATION_COUNTRIES.LATE_FEE_POLICY,
            RENT_REGULATION_COUNTRIES.LATE_FEE_MAX_PERCENTAGE)
        .from(RENT_REGULATION_COUNTRIES)
        .forEach(
            r ->
                rules.put(
                    r.value1(), new LateFeeRule(r.value2(), Optional.ofNullable(r.value3()))));
    return rules;
  }

  private static int scale(String currency) {
    return CurrencyUtils.getFractionalDigits(currency);
  }

  private static long minor(BigDecimal amount, String currency) {
    return amount
        .movePointRight(scale(currency))
        .setScale(0, RoundingMode.HALF_UP)
        .longValueExact();
  }

  private String heldWhere() {
    return switch (random.nextInt(3)) {
      case 0 -> "Separate escrow account";
      case 1 -> "Deposit protection scheme";
      default -> "Business account, ring-fenced";
    };
  }

  private String deductionReason() {
    return switch (random.nextInt(4)) {
      case 0 -> "Repainting the living room wall";
      case 1 -> "Professional cleaning after hand-back";
      case 2 -> "Replacing two broken window blinds";
      default -> "Unpaid final utility bill";
    };
  }

  /** A country's late-fee regime as recorded in the rent regulation catalogue. */
  private record LateFeeRule(String policy, Optional<BigDecimal> maxPercentage) {
    boolean permitsFee() {
      return "ALLOWED".equals(policy) || "CAPPED".equals(policy);
    }

    BigDecimal clamp(BigDecimal desired) {
      return maxPercentage.map(desired::min).orElse(desired);
    }
  }

  private static final class Counts {
    private int deposits;
    private int lateFees;
    private int reminders;
    private int plans;
    private int credits;
  }
}
