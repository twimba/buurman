package com.buurman.service.demo;

import static com.buurman.jooq.generated.Tables.CONTRACTS;
import static com.buurman.jooq.generated.Tables.PROPERTIES;
import static com.buurman.util.SidGenerator.newContractId;
import static com.buurman.util.SidGenerator.newContractPartyId;
import static com.buurman.util.SidGenerator.newRentComponentId;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.springframework.stereotype.Component;

import com.buurman.domain.Sid;
import com.buurman.domain.metadata.AtContractMetadata;
import com.buurman.domain.metadata.BeContractMetadata;
import com.buurman.domain.metadata.BrContractMetadata;
import com.buurman.domain.metadata.CaContractMetadata;
import com.buurman.domain.metadata.ChContractMetadata;
import com.buurman.domain.metadata.ContractCountryMetadata;
import com.buurman.domain.metadata.CountryMetadataRegistry;
import com.buurman.domain.metadata.CountryMetadataSerializer;
import com.buurman.domain.metadata.CzContractMetadata;
import com.buurman.domain.metadata.DeContractMetadata;
import com.buurman.domain.metadata.DkContractMetadata;
import com.buurman.domain.metadata.EsContractMetadata;
import com.buurman.domain.metadata.FiContractMetadata;
import com.buurman.domain.metadata.FrContractMetadata;
import com.buurman.domain.metadata.GenericContractMetadata;
import com.buurman.domain.metadata.GrContractMetadata;
import com.buurman.domain.metadata.HuContractMetadata;
import com.buurman.domain.metadata.IeContractMetadata;
import com.buurman.domain.metadata.ItContractMetadata;
import com.buurman.domain.metadata.MxContractMetadata;
import com.buurman.domain.metadata.NlContractMetadata;
import com.buurman.domain.metadata.NoContractMetadata;
import com.buurman.domain.metadata.PlContractMetadata;
import com.buurman.domain.metadata.PtContractMetadata;
import com.buurman.domain.metadata.RoContractMetadata;
import com.buurman.domain.metadata.SeContractMetadata;
import com.buurman.domain.metadata.UkContractMetadata;
import com.buurman.domain.metadata.UsContractMetadata;
import com.buurman.util.MoneyAmount;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class DemoContractGenerator {

  private final DSLContext dsl;
  private final CountryMetadataSerializer countryMetadataSerializer;
  private final Clock clock;
  private final Random random = new Random(42);

  private static final String[] CONTRACT_TYPES = {"FIXED_TERM", "INDEFINITE"};

  public void generate(DemoDataContext ctx) {
    LocalDateTime now = LocalDateTime.now(clock);
    LocalDate today = LocalDate.now(clock);

    for (var teamEntry : ctx.getTeamIds().entrySet()) {
      String teamKey = teamEntry.getKey();
      UUID teamId = teamEntry.getValue();
      UUID createdBy = ctx.getAdminUserForTeam(teamKey).orElse(null);
      String currency = ctx.getCurrencyForTeam(teamKey);
      List<UUID> propertyIds = ctx.getPropertyIdsByTeam().get(teamId);
      List<UUID> tenantIds = ctx.getTenantIdsByTeam().get(teamId);
      List<UUID> contractIds = new ArrayList<>();

      if (propertyIds == null || tenantIds == null) {
        continue;
      }

      int contractCount = Math.min(propertyIds.size(), tenantIds.size());

      // Sort tenants: put business tenants at end so they can match non-residential properties
      List<UUID> sortedTenants = new ArrayList<>(tenantIds);
      sortedTenants.sort(
          (a, b) -> {
            boolean aBiz = ctx.isBusinessTenant(a);
            boolean bBiz = ctx.isBusinessTenant(b);
            return Boolean.compare(aBiz, bBiz); // individuals first, business last
          });

      // Sort properties: put non-residential at end to match business tenants
      List<UUID> sortedProperties = new ArrayList<>(propertyIds);
      sortedProperties.sort(
          (a, b) -> {
            boolean aRes = "RESIDENTIAL".equals(ctx.getPropertyCategory(a));
            boolean bRes = "RESIDENTIAL".equals(ctx.getPropertyCategory(b));
            return Boolean.compare(
                aRes, bRes); // non-residential first (false < true), then residential
          });

      // Now reverse: we want residential first, non-residential last to match with business tenants
      // Actually we want: individual tenants matched to residential, business to non-residential
      // So pair from both ends: first individual+residential, then business+non-residential
      // Simpler: just sort both so residential+individual come first
      sortedProperties.sort(
          (a, b) -> {
            boolean aRes = "RESIDENTIAL".equals(ctx.getPropertyCategory(a));
            boolean bRes = "RESIDENTIAL".equals(ctx.getPropertyCategory(b));
            return Boolean.compare(!aRes, !bRes); // residential first
          });

      for (int i = 0; i < contractCount; i++) {
        UUID contractId = UUID.randomUUID();
        UUID propertyId = sortedProperties.get(i);
        UUID tenantId = sortedTenants.get(i);
        String propertyCategory = ctx.getPropertyCategory(propertyId);

        // Determine contract scenario
        String status;
        LocalDate startDate;
        LocalDate endDate;
        LocalDate signedDate;

        if (i < 2) {
          // EXPIRED contracts (past)
          status = "EXPIRED";
          startDate = today.minusYears(3).minusMonths(random.nextInt(0, 6));
          endDate = today.minusYears(1).minusMonths(random.nextInt(0, 12));
          signedDate = startDate.minusDays(random.nextInt(7, 30));
        } else if (i < contractCount - 2) {
          // ACTIVE contracts (current)
          status = "ACTIVE";
          startDate = today.minusMonths(random.nextInt(6, 18));
          if (random.nextBoolean()) {
            endDate = today.plusMonths(endDateMonthsForCategory(propertyCategory));
          } else {
            endDate = null;
          }
          signedDate = startDate.minusDays(random.nextInt(7, 30));

          // Mark property as occupied
          dsl.update(PROPERTIES)
              .set(PROPERTIES.STATUS, "OCCUPIED")
              .where(PROPERTIES.ID.eq(propertyId))
              .execute();
        } else if (i == contractCount - 2) {
          // TERMINATED
          status = "TERMINATED";
          startDate = today.minusYears(2);
          endDate = today.minusMonths(random.nextInt(1, 6));
          signedDate = startDate.minusDays(14);
        } else {
          // DRAFT (future)
          status = "DRAFT";
          startDate = today.plusMonths(1);
          endDate = today.plusYears(1).plusMonths(1);
          signedDate = null;
        }

        String contractType = CONTRACT_TYPES[i % CONTRACT_TYPES.length];
        if (endDate == null) {
          contractType = "INDEFINITE";
        }

        // Renewal mode: ~40% AUTOMATIC, ~20% MANUAL, ~40% NONE
        String renewalMode;
        int renewalTermMonths = 12;
        Integer maxRenewals = null;
        int landlordNoticeDays;
        int tenantNoticeDays;
        boolean requiresTenantConfirmation;
        String rentAdjType;
        BigDecimal rentAdjValue;

        int renewalBucket = i % 5;
        if (renewalBucket < 2) {
          // 40% AUTOMATIC
          renewalMode = "AUTOMATIC";
          landlordNoticeDays = 90;
          tenantNoticeDays = 30;
          requiresTenantConfirmation = false;
          rentAdjType = "FIXED_PERCENTAGE";
          rentAdjValue =
              BigDecimal.valueOf(2 + random.nextDouble() * 3)
                  .setScale(4, java.math.RoundingMode.HALF_UP);
        } else if (renewalBucket == 2) {
          // 20% MANUAL
          renewalMode = "MANUAL";
          landlordNoticeDays = 90;
          tenantNoticeDays = 30;
          requiresTenantConfirmation = true;
          maxRenewals = random.nextInt(2, 6);
          rentAdjType = "MANUAL";
          rentAdjValue = null;
        } else {
          // 40% NONE
          renewalMode = "NONE";
          landlordNoticeDays = 30;
          tenantNoticeDays = 30;
          requiresTenantConfirmation = false;
          rentAdjType = "NONE";
          rentAdjValue = null;
        }

        BigDecimal rentAmount = rentAmountForCategory(propertyCategory);
        BigDecimal deposit = rentAmount.multiply(depositMultiplierForCategory(propertyCategory));
        String paymentFrequency = paymentFrequencyForCategory(propertyCategory);
        int terminationNoticeDays = terminationNoticeForCategory(propertyCategory);

        // Resolve country from property
        String propertyCountry =
            dsl.select(PROPERTIES.COUNTRY_CODE)
                .from(PROPERTIES)
                .where(PROPERTIES.ID.eq(propertyId))
                .fetchOneInto(String.class);
        String countryCode = CountryMetadataRegistry.normalizeCountryCode(propertyCountry);
        ContractCountryMetadata metadata =
            countryCode != null ? buildDemoMetadata(countryCode) : null;

        Sid contractIdentifier = newContractId();
        dsl.insertInto(CONTRACTS)
            .set(CONTRACTS.ID, contractId)
            .set(CONTRACTS.IDENTIFIER, contractIdentifier)
            .set(CONTRACTS.TEAM_ID, teamId)
            .set(CONTRACTS.PROPERTY_ID, propertyId)
            .set(CONTRACTS.CONTRACT_TYPE, contractType)
            .set(CONTRACTS.START_DATE, startDate)
            .set(CONTRACTS.END_DATE, endDate)
            .set(CONTRACTS.SIGNED_DATE, signedDate)
            .set(CONTRACTS.RENT_AMOUNT, rentAmount)
            .set(CONTRACTS.DEPOSIT_AMOUNT, deposit)
            .set(CONTRACTS.SECURITY_DEPOSIT, rentAmount)
            .set(CONTRACTS.RENT_AMOUNT_CURRENCY, currency)
            .set(CONTRACTS.DEPOSIT_AMOUNT_CURRENCY, currency)
            .set(CONTRACTS.SECURITY_DEPOSIT_CURRENCY, currency)
            .set(CONTRACTS.PAYMENT_FREQUENCY, paymentFrequency)
            .set(CONTRACTS.PAYMENT_DUE_DAY, 1)
            .set(field("renewal_mode", String.class), renewalMode)
            .set(field("renewal_term_months", Integer.class), renewalTermMonths)
            .set(field("max_renewals", Integer.class), maxRenewals)
            .set(field("landlord_notice_days", Integer.class), landlordNoticeDays)
            .set(field("tenant_notice_days", Integer.class), tenantNoticeDays)
            .set(field("requires_tenant_confirmation", Boolean.class), requiresTenantConfirmation)
            .set(field("rent_adjustment_type", String.class), rentAdjType)
            .set(field("rent_adjustment_value", BigDecimal.class), rentAdjValue)
            .set(CONTRACTS.TERMINATION_NOTICE_DAYS, terminationNoticeDays)
            .set(CONTRACTS.LATE_FEE_PERCENTAGE, BigDecimal.valueOf(2))
            .set(CONTRACTS.STATUS, status)
            .set(CONTRACTS.NOTES, "Demo contract for testing purposes")
            .set(field("country_code", String.class), countryCode)
            .set(
                field("country_metadata", JSONB.class),
                metadata != null
                    ? JSONB.jsonb(countryMetadataSerializer.serialize(metadata))
                    : null)
            .set(CONTRACTS.CREATED_AT, now.minusDays(random.nextInt(30, 365)))
            .set(CONTRACTS.UPDATED_AT, now)
            .set(CONTRACTS.CREATED_BY, createdBy)
            .set(CONTRACTS.UPDATED_BY, createdBy)
            .execute();

        // Insert primary tenant into contract_parties
        dsl.insertInto(table("contract_parties"))
            .set(field("id", UUID.class), UUID.randomUUID())
            .set(field("identifier", String.class), newContractPartyId().value())
            .set(field("team_id", UUID.class), teamId)
            .set(field("contract_id", UUID.class), contractId)
            .set(field("tenant_id", UUID.class), tenantId)
            .set(field("role", String.class), "PRIMARY_TENANT")
            .set(field("created_at", LocalDateTime.class), now)
            .set(field("updated_at", LocalDateTime.class), now)
            .set(field("created_by", UUID.class), createdBy)
            .set(field("updated_by", UUID.class), createdBy)
            .execute();

        // Insert rent components (~70% of contracts get a breakdown)
        if (i % 3 != 2) {
          insertRentComponents(
              contractId, teamId, createdBy, rentAmount, currency, propertyCategory, now);
        }

        contractIds.add(contractId);
        ctx.putIdentifier(contractId, contractIdentifier);
        ctx.incrementContracts();
      }

      // === HISTORICAL EXPIRED CONTRACTS (4-6 per team) ===
      int historicalCount = random.nextInt(4, 7);
      int extraTenantStart = 12; // indices 12-17 are extra tenants
      int[] historicalPropertyIndices = {0, 2, 4, 6, 1, 3};

      for (int h = 0; h < historicalCount && h < historicalPropertyIndices.length; h++) {
        int propIdx = historicalPropertyIndices[h];
        if (propIdx >= sortedProperties.size()) {
          continue;
        }
        int tenantIdx = extraTenantStart + h;
        if (tenantIdx >= sortedTenants.size()) {
          continue;
        }

        UUID historicalContractId = UUID.randomUUID();
        UUID historicalPropertyId = sortedProperties.get(propIdx);
        UUID historicalTenantId = sortedTenants.get(tenantIdx);
        String historicalCategory = ctx.getPropertyCategory(historicalPropertyId);

        // Start 3-5 years ago, end 1-3 years ago (before current contracts)
        LocalDate histStart =
            today.minusYears(random.nextInt(3, 6)).minusMonths(random.nextInt(0, 6));
        LocalDate histEnd =
            today.minusYears(random.nextInt(1, 3)).minusMonths(random.nextInt(0, 6));
        if (!histEnd.isAfter(histStart)) {
          histEnd = histStart.plusYears(1);
        }
        LocalDate histSigned = histStart.minusDays(random.nextInt(7, 30));

        // Rent 80-90% of current (shows rent increase over time)
        BigDecimal histRent =
            rentAmountForCategory(historicalCategory)
                .multiply(BigDecimal.valueOf(0.80 + random.nextDouble() * 0.10))
                .setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal histDeposit = histRent.multiply(BigDecimal.valueOf(2));

        String histContractType = CONTRACT_TYPES[h % CONTRACT_TYPES.length];

        // Resolve country from property
        String histCountry =
            dsl.select(PROPERTIES.COUNTRY_CODE)
                .from(PROPERTIES)
                .where(PROPERTIES.ID.eq(historicalPropertyId))
                .fetchOneInto(String.class);
        String histCountryCode = CountryMetadataRegistry.normalizeCountryCode(histCountry);
        ContractCountryMetadata histMetadata =
            histCountryCode != null ? buildDemoMetadata(histCountryCode) : null;

        Sid histContractIdentifier = newContractId();
        dsl.insertInto(CONTRACTS)
            .set(CONTRACTS.ID, historicalContractId)
            .set(CONTRACTS.IDENTIFIER, histContractIdentifier)
            .set(CONTRACTS.TEAM_ID, teamId)
            .set(CONTRACTS.PROPERTY_ID, historicalPropertyId)
            .set(CONTRACTS.CONTRACT_TYPE, histContractType)
            .set(CONTRACTS.START_DATE, histStart)
            .set(CONTRACTS.END_DATE, histEnd)
            .set(CONTRACTS.SIGNED_DATE, histSigned)
            .set(CONTRACTS.RENT_AMOUNT, histRent)
            .set(CONTRACTS.DEPOSIT_AMOUNT, histDeposit)
            .set(CONTRACTS.SECURITY_DEPOSIT, histRent)
            .set(CONTRACTS.RENT_AMOUNT_CURRENCY, currency)
            .set(CONTRACTS.DEPOSIT_AMOUNT_CURRENCY, currency)
            .set(CONTRACTS.SECURITY_DEPOSIT_CURRENCY, currency)
            .set(CONTRACTS.PAYMENT_FREQUENCY, paymentFrequencyForCategory(historicalCategory))
            .set(CONTRACTS.PAYMENT_DUE_DAY, 1)
            .set(field("renewal_mode", String.class), "NONE")
            .set(field("landlord_notice_days", Integer.class), 30)
            .set(field("tenant_notice_days", Integer.class), 30)
            .set(field("requires_tenant_confirmation", Boolean.class), false)
            .set(field("rent_adjustment_type", String.class), "NONE")
            .set(
                CONTRACTS.TERMINATION_NOTICE_DAYS, terminationNoticeForCategory(historicalCategory))
            .set(CONTRACTS.LATE_FEE_PERCENTAGE, BigDecimal.valueOf(2))
            .set(CONTRACTS.STATUS, "EXPIRED")
            .set(CONTRACTS.NOTES, "Historical contract - expired " + histEnd)
            .set(field("country_code", String.class), histCountryCode)
            .set(
                field("country_metadata", JSONB.class),
                histMetadata != null
                    ? JSONB.jsonb(countryMetadataSerializer.serialize(histMetadata))
                    : null)
            .set(CONTRACTS.CREATED_AT, now.minusDays(random.nextInt(365, 1800)))
            .set(CONTRACTS.UPDATED_AT, now.minusDays(random.nextInt(30, 365)))
            .set(CONTRACTS.CREATED_BY, createdBy)
            .set(CONTRACTS.UPDATED_BY, createdBy)
            .execute();

        // Insert historical contract party
        dsl.insertInto(table("contract_parties"))
            .set(field("id", UUID.class), UUID.randomUUID())
            .set(field("identifier", String.class), newContractPartyId().value())
            .set(field("team_id", UUID.class), teamId)
            .set(field("contract_id", UUID.class), historicalContractId)
            .set(field("tenant_id", UUID.class), historicalTenantId)
            .set(field("role", String.class), "PRIMARY_TENANT")
            .set(field("created_at", LocalDateTime.class), now)
            .set(field("updated_at", LocalDateTime.class), now)
            .set(field("created_by", UUID.class), createdBy)
            .set(field("updated_by", UUID.class), createdBy)
            .execute();

        contractIds.add(historicalContractId);
        ctx.putIdentifier(historicalContractId, histContractIdentifier);
        ctx.incrementContracts();
      }

      ctx.getContractIdsByTeam().put(teamId, contractIds);
      log.info(
          "Created {} contracts ({} current + {} historical) for team {}",
          contractIds.size(),
          contractCount,
          contractIds.size() - contractCount,
          teamKey);
    }
  }

  private BigDecimal rentAmountForCategory(String category) {
    return BigDecimal.valueOf(
        switch (category) {
          case "COMMERCIAL" -> random.nextInt(1500, 8000);
          case "INDUSTRIAL" -> random.nextInt(2000, 15000);
          case "AGRICULTURAL" -> random.nextInt(500, 5000);
          case "MIXED_USE" -> random.nextInt(2000, 10000);
          default -> random.nextInt(800, 3000); // RESIDENTIAL
        });
  }

  private BigDecimal depositMultiplierForCategory(String category) {
    return switch (category) {
      case "COMMERCIAL", "INDUSTRIAL" -> BigDecimal.valueOf(random.nextInt(3, 7));
      default -> BigDecimal.valueOf(2);
    };
  }

  private String paymentFrequencyForCategory(String category) {
    return switch (category) {
      case "COMMERCIAL", "INDUSTRIAL" -> random.nextInt(3) == 0 ? "QUARTERLY" : "MONTHLY";
      case "AGRICULTURAL" -> random.nextInt(3) == 0 ? "ANNUALLY" : "QUARTERLY";
      default -> "MONTHLY";
    };
  }

  private int endDateMonthsForCategory(String category) {
    return switch (category) {
      case "COMMERCIAL" -> random.nextInt(36, 120); // 3-10 years
      case "INDUSTRIAL" -> random.nextInt(60, 180); // 5-15 years
      case "AGRICULTURAL" -> random.nextInt(12, 60); // 1-5 years
      default -> random.nextInt(6, 24); // residential
    };
  }

  @SuppressWarnings("NullAway")
  private void insertRentComponents(
      UUID contractId,
      UUID teamId,
      @org.jspecify.annotations.Nullable UUID createdBy,
      BigDecimal rentAmount,
      String currency,
      String propertyCategory,
      LocalDateTime now) {
    // BASE_RENT: ~70% of total
    BigDecimal baseRent =
        rentAmount.multiply(BigDecimal.valueOf(0.70)).setScale(2, java.math.RoundingMode.HALF_UP);
    // UTILITIES_ADVANCE: ~15%
    BigDecimal utilities =
        rentAmount.multiply(BigDecimal.valueOf(0.15)).setScale(2, java.math.RoundingMode.HALF_UP);
    // SERVICE_COSTS: remainder
    BigDecimal serviceCosts = rentAmount.subtract(baseRent).subtract(utilities);

    int sortOrder = 0;
    insertComponent(
        contractId, teamId, createdBy, "BASE_RENT", baseRent, currency, null, sortOrder++, now);
    insertComponent(
        contractId,
        teamId,
        createdBy,
        "UTILITIES_ADVANCE",
        utilities,
        currency,
        null,
        sortOrder++,
        now);
    insertComponent(
        contractId,
        teamId,
        createdBy,
        "SERVICE_COSTS",
        serviceCosts,
        currency,
        null,
        sortOrder++,
        now);

    // Add PARKING for commercial/industrial (~10% chance for residential)
    if ("COMMERCIAL".equals(propertyCategory)
        || "INDUSTRIAL".equals(propertyCategory)
        || random.nextInt(10) == 0) {
      BigDecimal parking = BigDecimal.valueOf(random.nextInt(50, 200));
      insertComponent(
          contractId, teamId, createdBy, "PARKING", parking, currency, null, sortOrder++, now);
    }
  }

  @SuppressWarnings("NullAway")
  private void insertComponent(
      UUID contractId,
      UUID teamId,
      UUID createdBy,
      String componentType,
      BigDecimal amountMajor,
      String currency,
      @org.jspecify.annotations.Nullable String description,
      int sortOrder,
      LocalDateTime now) {
    int digits = com.buurman.util.CurrencyUtils.getFractionalDigits(currency);
    long minorUnits = amountMajor.movePointRight(digits).longValueExact();
    dsl.insertInto(table("contract_rent_components"))
        .set(field("id", UUID.class), UUID.randomUUID())
        .set(field("identifier", String.class), newRentComponentId().value())
        .set(field("team_id", UUID.class), teamId)
        .set(field("contract_id", UUID.class), contractId)
        .set(field("component_type", String.class), componentType)
        .set(field("amount", Long.class), minorUnits)
        .set(field("currency", String.class), currency)
        .set(field("description", String.class), description)
        .set(field("sort_order", Integer.class), sortOrder)
        .set(field("created_at", LocalDateTime.class), now)
        .set(field("updated_at", LocalDateTime.class), now)
        .set(field("created_by", UUID.class), createdBy)
        .set(field("updated_by", UUID.class), createdBy)
        .execute();
  }

  private int terminationNoticeForCategory(String category) {
    return switch (category) {
      case "COMMERCIAL", "INDUSTRIAL" -> 90;
      case "AGRICULTURAL" -> 60;
      default -> 30;
    };
  }

  private ContractCountryMetadata buildDemoMetadata(String countryCode) {
    return switch (countryCode) {
      case "NL" ->
          new NlContractMetadata(
              random.nextBoolean() ? "VRIJE_SECTOR" : "GEREGULEERD",
              random.nextInt(100, 250),
              true,
              false,
              true,
              true,
              false,
              false,
              false,
              MoneyAmount.of(BigDecimal.valueOf(150), "EUR"),
              false,
              MoneyAmount.of(BigDecimal.valueOf(879), "EUR"),
              "C");
      case "DE" ->
          new DeContractMetadata(
              "Mietspiegel Berlin 2024",
              true,
              "STANDARD",
              false,
              MoneyAmount.of(BigDecimal.valueOf(200), "EUR"),
              MoneyAmount.of(BigDecimal.valueOf(2100), "EUR"),
              3,
              "VERBRAUCH",
              "C",
              BigDecimal.valueOf(125));
      case "FR" ->
          new FrContractMetadata(
              MoneyAmount.of(BigDecimal.valueOf(25, 1), "EUR"),
              MoneyAmount.of(BigDecimal.valueOf(30), "EUR"),
              true,
              true,
              "C",
              false,
              false,
              true,
              true,
              true,
              true,
              MoneyAmount.of(BigDecimal.valueOf(900), "EUR"),
              1);
      case "BE" ->
          new BeContractMetadata(
              "BRUSSELS",
              BigDecimal.valueOf(110, 1),
              "B",
              "EPC-2024-12345",
              "REG-FOD-67890",
              2,
              "BLOCKED_ACCOUNT",
              "2024-01");
      case "GB" ->
          new UkContractMetadata(
              "AST", "DPS", "C", true, true, true, MoneyAmount.of(BigDecimal.valueOf(1500), "GBP"));
      case "US" ->
          new UsContractMetadata(
              "NY",
              true,
              "New York City",
              false,
              true,
              MoneyAmount.of(BigDecimal.valueOf(5000), "USD"),
              2);
      case "ES" ->
          new EsContractMetadata(
              true,
              false,
              BigDecimal.valueOf(1200),
              MoneyAmount.of(BigDecimal.valueOf(800), "EUR"),
              1,
              "D",
              MoneyAmount.of(BigDecimal.valueOf(1600), "EUR"),
              2);
      case "PT" ->
          new PtContractMetadata("NRAU", false, BigDecimal.valueOf(1.0154), "IMI-2024-98765", "B");
      case "IT" ->
          new ItContractMetadata(
              "LIBERO",
              true,
              BigDecimal.valueOf(21),
              "REG-2024-MI-12345",
              "B",
              MoneyAmount.of(BigDecimal.valueOf(2400), "EUR"),
              3);
      case "AT" ->
          new AtContractMetadata(
              "MRG",
              "B",
              MoneyAmount.of(BigDecimal.valueOf(180), "EUR"),
              3,
              MoneyAmount.of(BigDecimal.valueOf(2400), "EUR"),
              true,
              MoneyAmount.of(BigDecimal.valueOf(6.5), "EUR"),
              "EA-2024-AT-001");
      case "CH" ->
          new ChContractMetadata(
              "ZH",
              "Zurich",
              MoneyAmount.of(BigDecimal.valueOf(250), "CHF"),
              3,
              MoneyAmount.of(BigDecimal.valueOf(4500), "CHF"),
              true,
              BigDecimal.valueOf(1.5));
      case "DK" ->
          new DkContractMetadata(
              "PRIVATE",
              "C",
              MoneyAmount.of(BigDecimal.valueOf(30000), "DKK"),
              3,
              MoneyAmount.of(BigDecimal.valueOf(30000), "DKK"),
              3,
              true);
      case "SE" ->
          new SeContractMetadata(
              "PRIVATE", true, "C", MoneyAmount.of(BigDecimal.valueOf(25000), "SEK"), 3, true);
      case "FI" ->
          new FiContractMetadata(
              "INDEFINITE", "C", MoneyAmount.of(BigDecimal.valueOf(1500), "EUR"), 2, false);
      case "NO" ->
          new NoContractMetadata(
              "RESIDENTIAL", "C", MoneyAmount.of(BigDecimal.valueOf(30000), "NOK"), 3, true, false);
      case "IE" ->
          new IeContractMetadata(
              "PART4",
              "B2",
              MoneyAmount.of(BigDecimal.valueOf(2000), "EUR"),
              1,
              true,
              true,
              MoneyAmount.of(BigDecimal.valueOf(1800), "EUR"),
              "BER-2024-IE-001");
      case "PL" ->
          new PlContractMetadata(
              "ZWYKLY",
              "C",
              MoneyAmount.of(BigDecimal.valueOf(4000), "PLN"),
              2,
              true,
              "EC-2024-PL-001",
              true);
      case "CZ" ->
          new CzContractMetadata(
              "INDEFINITE",
              "C",
              MoneyAmount.of(BigDecimal.valueOf(30000), "CZK"),
              3,
              MoneyAmount.of(BigDecimal.valueOf(5000), "CZK"),
              false);
      case "HU" ->
          new HuContractMetadata(
              "DEFINITE",
              "CC",
              MoneyAmount.of(BigDecimal.valueOf(300000), "HUF"),
              2,
              MoneyAmount.of(BigDecimal.valueOf(25000), "HUF"),
              false);
      case "RO" ->
          new RoContractMetadata(
              "DEFINITE",
              "C",
              MoneyAmount.of(BigDecimal.valueOf(3000), "RON"),
              2,
              true,
              MoneyAmount.of(BigDecimal.valueOf(500), "RON"));
      case "GR" ->
          new GrContractMetadata(
              "RESIDENTIAL",
              "C",
              MoneyAmount.of(BigDecimal.valueOf(1500), "EUR"),
              2,
              false,
              MoneyAmount.of(BigDecimal.valueOf(80), "EUR"),
              "TAX-2024-GR-001");
      case "BR" ->
          new BrContractMetadata(
              "RESIDENCIAL",
              MoneyAmount.of(BigDecimal.valueOf(5000), "BRL"),
              3,
              false,
              MoneyAmount.of(BigDecimal.valueOf(800), "BRL"),
              "MAT-2024-BR-001",
              false);
      case "CA" ->
          new CaContractMetadata(
              "ON", true, MoneyAmount.of(BigDecimal.valueOf(2000), "CAD"), 2, true, null);
      case "MX" ->
          new MxContractMetadata(
              "CDMX",
              "DEFINITE",
              MoneyAmount.of(BigDecimal.valueOf(15000), "MXN"),
              1,
              false,
              MoneyAmount.of(BigDecimal.valueOf(2000), "MXN"));
      default -> new GenericContractMetadata(null, null, null, null, null, "Demo generic metadata");
    };
  }
}
