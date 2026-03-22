package com.buurman.service.demo;

import static com.buurman.jooq.generated.Tables.EXPENSES;
import static com.buurman.util.SidGenerator.newExpenseId;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;

@Component
@Slf4j
@RequiredArgsConstructor
public class DemoExpenseGenerator {

  private final DSLContext dsl;
  private final Clock clock;
  private final Faker faker = new Faker(Locale.ENGLISH, new Random(42));
  private final Random random = new Random(42);

  public void generate(DemoDataContext ctx) {
    LocalDateTime now = LocalDateTime.now(clock);
    LocalDate today = LocalDate.now(clock);
    int currentYear = today.getYear();

    for (var teamEntry : ctx.getTeamIds().entrySet()) {
      String teamKey = teamEntry.getKey();
      UUID teamId = teamEntry.getValue();
      UUID createdBy = ctx.getAdminUserForTeam(teamKey).orElse(null);
      String currency = ctx.getCurrencyForTeam(teamKey);
      List<UUID> propertyIds = ctx.getPropertyIdsByTeam().get(teamId);
      if (propertyIds == null) {
        continue;
      }

      int teamExpenses = 0;

      for (UUID propertyId : propertyIds) {
        String propCategory = ctx.getPropertyCategory(propertyId);
        LocalDate acquisitionDate = ctx.getPropertyAcquisitionDate(propertyId);
        int startYear = acquisitionDate.getYear();
        double countryMultiplier = ctx.getPropertyCountryRentMultiplier(propertyId);
        double categoryMultiplier =
            ("COMMERCIAL".equals(propCategory) || "INDUSTRIAL".equals(propCategory)) ? 2.0 : 1.0;

        // Generate expenses for each COMPLETED year of ownership.
        // For the current (partial) year, only generate recurring annual expenses (insurance, tax)
        // to avoid concentrating a full year of expenses into just a few months.
        int lastFullYear = currentYear - 1;
        for (int year = startYear; year <= currentYear; year++) {
          boolean isPartialYear = (year == currentYear);
          int propertyAge = year - startYear;
          double inflationFactor = Math.pow(1.025, currentYear - year);
          double baseMultiplier = categoryMultiplier * countryMultiplier / inflationFactor;

          // === RECURRING ANNUAL EXPENSES ===

          // 1. Insurance premium (every year)
          insertExpense(
              propertyId,
              teamId,
              createdBy,
              currency,
              "INSURANCE",
              randomAmount(300, 1200, baseMultiplier),
              randomDateInYear(year),
              "Annual building insurance premium",
              "Annual renewal - " + year,
              now);
          teamExpenses++;
          ctx.incrementExpenses();

          // 2. Property tax (every year)
          insertExpense(
              propertyId,
              teamId,
              createdBy,
              currency,
              "PROPERTY_TAX",
              randomAmount(800, 3000, baseMultiplier),
              LocalDate.of(year, random.nextInt(1, 4), random.nextInt(1, 28)),
              "Annual property tax - " + year,
              null,
              now);
          teamExpenses++;
          ctx.incrementExpenses();

          // Skip random/periodic expenses for the current partial year
          // to avoid concentrating a full year of costs into a few months
          if (isPartialYear) {
            continue;
          }

          // 3. Annual maintenance/inspection
          insertExpense(
              propertyId,
              teamId,
              createdBy,
              currency,
              "MAINTENANCE",
              randomAmount(150, 500, baseMultiplier),
              randomDateInYear(year),
              pick(
                  "Annual inspection",
                  "HVAC servicing",
                  "Boiler annual service",
                  "Fire safety check"),
              null,
              now);
          teamExpenses++;
          ctx.incrementExpenses();

          // === PERIODIC EXPENSES ===

          // 4. Cleaning (commercial: 2x/year, residential: 0-1x/year)
          int cleaningCount =
              "COMMERCIAL".equals(propCategory) ? 2 : (random.nextInt(3) == 0 ? 1 : 0);
          for (int c = 0; c < cleaningCount; c++) {
            insertExpense(
                propertyId,
                teamId,
                createdBy,
                currency,
                "CLEANING",
                randomAmount(100, 350, baseMultiplier),
                randomDateInYear(year),
                pick(
                    "Common area cleaning",
                    "Window cleaning",
                    "End-of-tenancy cleaning",
                    "Deep clean"),
                null,
                now);
            teamExpenses++;
            ctx.incrementExpenses();
          }

          // 5. Landscaping (if residential/agricultural, ~50% of years)
          if (("RESIDENTIAL".equals(propCategory) || "AGRICULTURAL".equals(propCategory))
              && random.nextBoolean()) {
            insertExpense(
                propertyId,
                teamId,
                createdBy,
                currency,
                "LANDSCAPING",
                randomAmount(80, 300, baseMultiplier),
                randomDateInYear(year),
                pick(
                    "Garden maintenance",
                    "Tree pruning",
                    "Lawn mowing",
                    "Hedge trimming",
                    "Snow removal"),
                null,
                now);
            teamExpenses++;
            ctx.incrementExpenses();
          }

          // 6. Random repairs (0-2 per year, more for older properties)
          int repairChance = propertyAge > 15 ? 60 : (propertyAge > 5 ? 40 : 25);
          int repairCount = 0;
          if (random.nextInt(100) < repairChance) {
            repairCount++;
          }
          if (propertyAge > 10 && random.nextInt(100) < 30) {
            repairCount++;
          }
          for (int r = 0; r < repairCount; r++) {
            insertExpense(
                propertyId,
                teamId,
                createdBy,
                currency,
                "REPAIR",
                randomAmount(200, 1500, baseMultiplier),
                randomDateInYear(year),
                pick(
                    "Plumbing repair",
                    "Electrical repair",
                    "Roof leak fix",
                    "Window replacement",
                    "Door lock replacement",
                    "Boiler repair",
                    "Pipe burst repair",
                    "Damp treatment"),
                "Vendor: " + faker.company().name(),
                now);
            teamExpenses++;
            ctx.incrementExpenses();
          }

          // 7. Major repair every ~7 years (for properties older than 7 years)
          if (propertyAge > 0 && propertyAge % 7 == 0) {
            insertExpense(
                propertyId,
                teamId,
                createdBy,
                currency,
                "REPAIR",
                randomAmount(3000, 15000, baseMultiplier),
                randomDateInYear(year),
                pick(
                    "Major roof replacement",
                    "Complete HVAC system overhaul",
                    "Foundation repair work",
                    "Full electrical rewiring",
                    "Extensive water damage restoration",
                    "Structural reinforcement",
                    "Complete bathroom renovation",
                    "Kitchen overhaul"),
                "Major repair - contractor: " + faker.company().name(),
                now);
            teamExpenses++;
            ctx.incrementExpenses();
          }

          // 8. Utility expenses (50% of years for residential, always for commercial)
          if ("COMMERCIAL".equals(propCategory)
              || "INDUSTRIAL".equals(propCategory)
              || random.nextBoolean()) {
            insertExpense(
                propertyId,
                teamId,
                createdBy,
                currency,
                "UTILITY",
                randomAmount(100, 400, baseMultiplier),
                randomDateInYear(year),
                pick("Water bill", "Electricity (common areas)", "Gas bill", "Waste collection"),
                null,
                now);
            teamExpenses++;
            ctx.incrementExpenses();
          }

          // 9. Legal/fees (occasional, ~15% of years)
          if (random.nextInt(100) < 15) {
            insertExpense(
                propertyId,
                teamId,
                createdBy,
                currency,
                random.nextBoolean() ? "LEGAL" : "FEES",
                randomAmount(200, 800, baseMultiplier),
                randomDateInYear(year),
                pick(
                    "Legal consultation fee",
                    "Lease review",
                    "Contract drafting fee",
                    "Building permit fee",
                    "Energy audit fee",
                    "Notary fee"),
                null,
                now);
            teamExpenses++;
            ctx.incrementExpenses();
          }

          // 10. Property management fee (~40% of years)
          if (random.nextInt(100) < 40) {
            insertExpense(
                propertyId,
                teamId,
                createdBy,
                currency,
                "PROPERTY_MANAGEMENT",
                randomAmount(150, 500, baseMultiplier),
                randomDateInYear(year),
                pick(
                    "Monthly management fee",
                    "Property inspection report",
                    "Tenant screening service"),
                null,
                now);
            teamExpenses++;
            ctx.incrementExpenses();
          }
        }
      }
      log.info("Created {} expenses for team {}", teamExpenses, teamKey);
    }
  }

  private BigDecimal randomAmount(int min, int max, double multiplier) {
    int base = random.nextInt(min, max + 1);
    return BigDecimal.valueOf((long) (base * multiplier));
  }

  private LocalDate randomDateInYear(int year) {
    LocalDate today = LocalDate.now(clock);
    int maxMonth = (year == today.getYear()) ? today.getMonthValue() : 12;
    int month = random.nextInt(1, maxMonth + 1);
    int maxDay = YearMonth.of(year, month).lengthOfMonth();
    if (year == today.getYear() && month == today.getMonthValue()) {
      maxDay = Math.min(maxDay, today.getDayOfMonth());
    }
    int day = random.nextInt(1, maxDay + 1);
    return LocalDate.of(year, month, day);
  }

  @SuppressWarnings("NullAway")
  private void insertExpense(
      UUID propertyId,
      UUID teamId,
      @Nullable UUID createdBy,
      String currency,
      String category,
      BigDecimal amount,
      LocalDate expenseDate,
      String description,
      @Nullable String notes,
      LocalDateTime now) {

    LocalDateTime createdAt = expenseDate.atStartOfDay().plusDays(random.nextInt(0, 7));
    if (createdAt.isAfter(now)) {
      createdAt = now;
    }

    dsl.insertInto(EXPENSES)
        .set(EXPENSES.ID, UUID.randomUUID())
        .set(EXPENSES.IDENTIFIER, newExpenseId())
        .set(EXPENSES.TEAM_ID, teamId)
        .set(EXPENSES.PROPERTY_ID, propertyId)
        .set(EXPENSES.CATEGORY, category)
        .set(EXPENSES.AMOUNT, amount)
        .set(EXPENSES.CURRENCY, currency)
        .set(EXPENSES.EXPENSE_DATE, expenseDate)
        .set(EXPENSES.DESCRIPTION, description)
        .set(EXPENSES.NOTES, notes)
        .set(EXPENSES.CREATED_AT, createdAt)
        .set(EXPENSES.UPDATED_AT, now)
        .set(EXPENSES.CREATED_BY, createdBy)
        .set(EXPENSES.UPDATED_BY, createdBy)
        .execute();
  }

  private String pick(String... options) {
    return options[random.nextInt(options.length)];
  }
}
