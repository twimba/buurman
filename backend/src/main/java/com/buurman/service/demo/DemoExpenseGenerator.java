package com.buurman.service.demo;

import static com.buurman.jooq.generated.Tables.EXPENSES;
import static com.buurman.util.SidGenerator.newExpenseId;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import com.buurman.util.CurrencyUtils;

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

  private static final String[] CATEGORIES = {
    "MAINTENANCE",
    "REPAIR",
    "UTILITY",
    "TAX",
    "INSURANCE",
    "LEGAL",
    "CLEANING",
    "LANDSCAPING",
    "PROPERTY_MANAGEMENT"
  };

  // Category -> [description template, min amount, max amount]
  private static final Map<String, ExpenseTemplate> TEMPLATES =
      Map.of(
          "MAINTENANCE", new ExpenseTemplate("Annual maintenance inspection", 150, 500),
          "REPAIR", new ExpenseTemplate("Plumbing repair", 200, 1500),
          "UTILITY", new ExpenseTemplate("Water supply quarterly bill", 100, 400),
          "TAX", new ExpenseTemplate("Property tax payment", 500, 2000),
          "INSURANCE", new ExpenseTemplate("Building insurance premium", 300, 1200),
          "LEGAL", new ExpenseTemplate("Legal consultation fee", 200, 800),
          "CLEANING", new ExpenseTemplate("Professional deep cleaning", 100, 350),
          "LANDSCAPING", new ExpenseTemplate("Garden maintenance service", 80, 300),
          "PROPERTY_MANAGEMENT", new ExpenseTemplate("Property management monthly fee", 150, 500));

  private record ExpenseTemplate(String description, int minAmount, int maxAmount) {}

  public void generate(DemoDataContext ctx) {
    LocalDateTime now = LocalDateTime.now(clock);
    LocalDate today = LocalDate.now(clock);

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
        int expenseCount = random.nextInt(3, 6);

        for (int i = 0; i < expenseCount; i++) {
          String category = CATEGORIES[random.nextInt(CATEGORIES.length)];
          ExpenseTemplate template = TEMPLATES.get(category);
          if (template == null) {
            continue;
          }

          BigDecimal amount =
              BigDecimal.valueOf(random.nextInt(template.minAmount(), template.maxAmount() + 1));
          LocalDate expenseDate = today.minusDays(random.nextInt(30, 1095)); // up to 3 years ago

          String description = template.description();
          // Add some variation
          if (random.nextBoolean()) {
            description =
                switch (category) {
                  case "REPAIR" ->
                      pick(
                          "Plumbing repair",
                          "Electrical repair",
                          "Roof leak fix",
                          "Window replacement",
                          "Door lock replacement",
                          "Boiler repair");
                  case "MAINTENANCE" ->
                      pick(
                          "Annual inspection",
                          "HVAC servicing",
                          "Gutter cleaning",
                          "Chimney sweep",
                          "Fire safety check");
                  case "UTILITY" ->
                      pick(
                          "Water bill",
                          "Electricity bill (common areas)",
                          "Gas bill",
                          "Internet service");
                  case "CLEANING" ->
                      pick(
                          "End-of-tenancy cleaning",
                          "Common area cleaning",
                          "Window cleaning",
                          "Carpet cleaning");
                  default -> description;
                };
          }

          dsl.insertInto(EXPENSES)
              .set(EXPENSES.ID, UUID.randomUUID())
              .set(EXPENSES.IDENTIFIER, newExpenseId())
              .set(EXPENSES.TEAM_ID, teamId)
              .set(EXPENSES.PROPERTY_ID, propertyId)
              .set(EXPENSES.CATEGORY, category)
              .set(EXPENSES.AMOUNT, CurrencyUtils.toMinorUnits(amount, currency))
              .set(EXPENSES.CURRENCY, currency)
              .set(EXPENSES.EXPENSE_DATE, expenseDate)
              .set(EXPENSES.DESCRIPTION, description)
              .set(EXPENSES.NOTES, random.nextInt(3) == 0 ? "Vendor invoice attached" : null)
              .set(EXPENSES.CREATED_AT, now.minusDays(random.nextInt(1, 30)))
              .set(EXPENSES.UPDATED_AT, now)
              .set(EXPENSES.CREATED_BY, createdBy)
              .set(EXPENSES.UPDATED_BY, createdBy)
              .execute();

          teamExpenses++;
          ctx.incrementExpenses();
        }
      }

      log.info("Created {} expenses for team {}", teamExpenses, teamKey);
    }
  }

  private String pick(String... options) {
    return options[random.nextInt(options.length)];
  }
}
