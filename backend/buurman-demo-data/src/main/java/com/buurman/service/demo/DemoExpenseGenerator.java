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
    "PROPERTY_MANAGEMENT",
    "MARKETING",
    "FEES",
    "PROPERTY_TAX",
    "OTHER"
  };

  private static final Map<String, ExpenseTemplate> TEMPLATES =
      Map.ofEntries(
          Map.entry("MAINTENANCE", new ExpenseTemplate(150, 500)),
          Map.entry("REPAIR", new ExpenseTemplate(200, 1500)),
          Map.entry("UTILITY", new ExpenseTemplate(100, 400)),
          Map.entry("TAX", new ExpenseTemplate(500, 2000)),
          Map.entry("INSURANCE", new ExpenseTemplate(300, 1200)),
          Map.entry("LEGAL", new ExpenseTemplate(200, 800)),
          Map.entry("CLEANING", new ExpenseTemplate(100, 350)),
          Map.entry("LANDSCAPING", new ExpenseTemplate(80, 300)),
          Map.entry("PROPERTY_MANAGEMENT", new ExpenseTemplate(150, 500)),
          Map.entry("MARKETING", new ExpenseTemplate(200, 1500)),
          Map.entry("FEES", new ExpenseTemplate(50, 400)),
          Map.entry("PROPERTY_TAX", new ExpenseTemplate(800, 3000)),
          Map.entry("OTHER", new ExpenseTemplate(50, 500)));

  private record ExpenseTemplate(int minAmount, int maxAmount) {}

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
        String propCategory = ctx.getPropertyCategory(propertyId);
        double categoryMultiplier =
            "COMMERCIAL".equals(propCategory) || "INDUSTRIAL".equals(propCategory) ? 2.5 : 1.0;
        int expenseCount = random.nextInt(6, 13);

        for (int i = 0; i < expenseCount; i++) {
          String category = CATEGORIES[random.nextInt(CATEGORIES.length)];
          ExpenseTemplate template = TEMPLATES.get(category);
          if (template == null) {
            continue;
          }

          int baseAmount = random.nextInt(template.minAmount(), template.maxAmount() + 1);
          BigDecimal amount = BigDecimal.valueOf((long) (baseAmount * categoryMultiplier));
          LocalDate expenseDate = today.minusDays(random.nextInt(30, 1095));

          String description = descriptionForCategory(category);

          String notes = null;
          if (random.nextInt(10) < 4) {
            notes =
                pick(
                    "Vendor: " + faker.company().name(),
                    "Invoice #INV-" + String.format("%06d", random.nextInt(100000, 999999)),
                    "Annual service contract",
                    "Emergency call-out",
                    "Paid via bank transfer",
                    "Receipt on file");
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
              .set(EXPENSES.CREATED_AT, now.minusDays(random.nextInt(1, 30)))
              .set(EXPENSES.UPDATED_AT, now)
              .set(EXPENSES.CREATED_BY, createdBy)
              .set(EXPENSES.UPDATED_BY, createdBy)
              .execute();

          teamExpenses++;
          ctx.incrementExpenses();
        }

        // Large repair event: 20% of properties get 1 major expense
        if (random.nextInt(5) == 0) {
          BigDecimal majorAmount =
              BigDecimal.valueOf(random.nextInt(2000, 8001))
                  .multiply(BigDecimal.valueOf(categoryMultiplier > 1 ? 2 : 1));
          dsl.insertInto(EXPENSES)
              .set(EXPENSES.ID, UUID.randomUUID())
              .set(EXPENSES.IDENTIFIER, newExpenseId())
              .set(EXPENSES.TEAM_ID, teamId)
              .set(EXPENSES.PROPERTY_ID, propertyId)
              .set(EXPENSES.CATEGORY, "REPAIR")
              .set(EXPENSES.AMOUNT, majorAmount)
              .set(EXPENSES.CURRENCY, currency)
              .set(EXPENSES.EXPENSE_DATE, today.minusDays(random.nextInt(30, 365)))
              .set(
                  EXPENSES.DESCRIPTION,
                  pick(
                      "Major roof replacement",
                      "Complete HVAC system overhaul",
                      "Foundation repair work",
                      "Full electrical rewiring",
                      "Extensive water damage restoration",
                      "Structural reinforcement"))
              .set(EXPENSES.NOTES, "Major repair - contractor: " + faker.company().name())
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

  private String descriptionForCategory(String category) {
    return switch (category) {
      case "REPAIR" ->
          pick(
              "Plumbing repair",
              "Electrical repair",
              "Roof leak fix",
              "Window replacement",
              "Door lock replacement",
              "Boiler repair",
              "Pipe burst repair",
              "Damp treatment");
      case "MAINTENANCE" ->
          pick(
              "Annual inspection",
              "HVAC servicing",
              "Gutter cleaning",
              "Chimney sweep",
              "Fire safety check",
              "Boiler annual service",
              "Pest control treatment",
              "Fire alarm testing");
      case "UTILITY" ->
          pick(
              "Water bill",
              "Electricity bill (common areas)",
              "Gas bill",
              "Internet service",
              "Waste water treatment",
              "District heating charge");
      case "CLEANING" ->
          pick(
              "End-of-tenancy cleaning",
              "Common area cleaning",
              "Window cleaning",
              "Carpet cleaning",
              "Deep clean after renovation",
              "Pressure washing exterior");
      case "LANDSCAPING" ->
          pick(
              "Garden maintenance",
              "Tree pruning",
              "Lawn mowing service",
              "Snow removal",
              "Hedge trimming",
              "Seasonal planting");
      case "INSURANCE" ->
          pick(
              "Building insurance premium", "Liability insurance renewal",
              "Contents insurance", "Flood insurance supplement");
      case "TAX" ->
          pick(
              "Property tax payment", "Municipal tax levy",
              "Land tax assessment", "Waste collection tax");
      case "LEGAL" ->
          pick(
              "Legal consultation fee", "Lease review by solicitor",
              "Eviction proceedings", "Contract drafting fee",
              "Dispute mediation", "Regulatory compliance review");
      case "PROPERTY_MANAGEMENT" ->
          pick(
              "Monthly management fee", "Tenant screening service",
              "Property inspection report", "Key management service");
      case "MARKETING" ->
          pick(
              "Property listing fee", "Professional photography",
              "Virtual tour creation", "Advertising placement",
              "Signage and brochures", "Online portal subscription");
      case "FEES" ->
          pick(
              "Building permit fee", "Certificate of compliance",
              "Energy audit fee", "Bank transfer fee",
              "Government registration fee", "Notary fee");
      case "PROPERTY_TAX" ->
          pick(
              "Annual property tax", "Supplemental tax bill",
              "Special assessment levy", "Municipal surcharge");
      default ->
          pick(
              "Miscellaneous expense", "Sundry costs",
              "Administrative expense", "Contingency payment");
    };
  }

  private String pick(String... options) {
    return options[random.nextInt(options.length)];
  }
}
