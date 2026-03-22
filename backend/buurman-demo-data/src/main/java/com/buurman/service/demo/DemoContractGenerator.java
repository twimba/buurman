package com.buurman.service.demo;

import static com.buurman.jooq.generated.Tables.CONTRACTS;
import static com.buurman.jooq.generated.Tables.PROPERTIES;
import static com.buurman.util.SidGenerator.newContractId;
import static com.buurman.util.SidGenerator.newContractPartyId;
import static com.buurman.util.SidGenerator.newContractRentPeriodId;
import static com.buurman.util.SidGenerator.newRentComponentId;
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

  @SuppressWarnings("NullAway")
  public void generate(DemoDataContext ctx) {
    LocalDateTime now = LocalDateTime.now(clock);
    LocalDate today = LocalDate.now(clock);

    for (var teamEntry : ctx.getTeamIds().entrySet()) {
      String teamKey = teamEntry.getKey();
      UUID teamId = teamEntry.getValue();
      UUID createdBy = ctx.getAdminUserForTeam(teamKey).orElse(null);
      String currency = ctx.getCurrencyForTeam(teamKey);
      List<UUID> propertyIds = ctx.getPropertyIdsByTeam().get(teamId);
      List<UUID> contactIds = ctx.getContactIdsByTeam().get(teamId);
      List<UUID> contractIds = new ArrayList<>();

      if (propertyIds == null || contactIds == null) {
        continue;
      }

      // Sort contacts: individuals first, business last
      List<UUID> sortedContacts = new ArrayList<>(contactIds);
      sortedContacts.sort(
          (a, b) -> {
            boolean aBiz = ctx.isBusinessContact(a);
            boolean bBiz = ctx.isBusinessContact(b);
            return Boolean.compare(aBiz, bBiz);
          });

      // Sort properties: residential first, non-residential last
      List<UUID> sortedProperties = new ArrayList<>(propertyIds);
      sortedProperties.sort(
          (a, b) -> {
            boolean aRes = "RESIDENTIAL".equals(ctx.getPropertyCategory(a));
            boolean bRes = "RESIDENTIAL".equals(ctx.getPropertyCategory(b));
            return Boolean.compare(!aRes, !bRes);
          });

      // Global contact index across all properties in this team
      int contactIndex = 0;
      int totalContracts = 0;

      for (int propIdx = 0; propIdx < sortedProperties.size(); propIdx++) {
        UUID propertyId = sortedProperties.get(propIdx);
        String propertyCategory = ctx.getPropertyCategory(propertyId);
        LocalDate acquisitionDate = ctx.getPropertyAcquisitionDate(propertyId);
        BigDecimal rentBaseline = ctx.getPropertyRentBaseline(propertyId);
        String propertyCountry = ctx.getPropertyCountryCode(propertyId);
        String countryCode = CountryMetadataRegistry.normalizeCountryCode(propertyCountry);

        long yearsOwned = ChronoUnit.YEARS.between(acquisitionDate, today);
        int chainLength = computeChainLength(yearsOwned);

        if (chainLength == 0) {
          continue;
        }

        // Divide time from acquisition to today into roughly equal segments
        long totalDays = ChronoUnit.DAYS.between(acquisitionDate, today);
        long segmentDays = chainLength > 0 ? totalDays / chainLength : totalDays;

        BigDecimal previousRent = null;

        for (int c = 0; c < chainLength; c++) {
          boolean isLastContract = (c == chainLength - 1);

          // Determine contact (wrap around if we exceed the pool)
          UUID contactId = sortedContacts.get(contactIndex % sortedContacts.size());
          contactIndex++;

          // Compute start date
          LocalDate startDate;
          if (c == 0) {
            // First contract: acquisition date + 1-6 months
            startDate = acquisitionDate.plusMonths(random.nextInt(1, 7));
          } else {
            // Subsequent contracts: previous segment end + 0-3 months gap
            LocalDate segmentEnd = acquisitionDate.plusDays(segmentDays * c);
            startDate = segmentEnd.plusMonths(random.nextInt(0, 4));
          }

          // Clamp start date to not be in the future for non-last contracts
          if (!isLastContract && startDate.isAfter(today)) {
            startDate = today.minusMonths(1);
          }

          // Compute contract term based on category
          int termMonths = contractTermMonthsForCategory(propertyCategory);

          // Compute end date
          LocalDate endDate;
          if (isLastContract) {
            endDate =
                computeLastContractEndDate(
                    propIdx, sortedProperties.size(), startDate, termMonths, today);
          } else {
            // Historical contracts: end at roughly the segment boundary
            LocalDate segmentEnd = acquisitionDate.plusDays(segmentDays * (c + 1));
            endDate = segmentEnd;
            if (!endDate.isAfter(startDate)) {
              endDate = startDate.plusMonths(termMonths);
            }
          }

          // Determine status
          String status =
              computeStatus(propIdx, sortedProperties.size(), isLastContract, endDate, today);

          // Compute rent for this contract
          int contractYear = startDate.getYear();
          int currentYear = today.getYear();
          BigDecimal rent;
          if (previousRent != null) {
            // New contract in chain: 5-15% bump above previous (market rate reset)
            double bump = 1.05 + random.nextDouble() * 0.10;
            rent =
                previousRent.multiply(BigDecimal.valueOf(bump)).setScale(0, RoundingMode.HALF_UP);
          } else {
            rent = rentForYear(rentBaseline, contractYear, currentYear);
          }
          previousRent = rent;

          BigDecimal deposit = rent.multiply(depositMultiplierForCategory(propertyCategory));
          String paymentFrequency = paymentFrequencyForCategory(propertyCategory);
          int terminationNoticeDays = terminationNoticeForCategory(propertyCategory);

          // Contract type
          String contractType;
          if (isLastContract && endDate == null) {
            contractType = "INDEFINITE";
          } else {
            contractType = CONTRACT_TYPES[totalContracts % CONTRACT_TYPES.length];
            if (endDate == null) {
              contractType = "INDEFINITE";
            }
          }

          // Signed date
          LocalDate signedDate;
          if ("DRAFT".equals(status)) {
            signedDate = null;
          } else {
            signedDate = startDate.minusDays(random.nextInt(7, 30));
          }

          // Renewal mode distribution: ~40% AUTOMATIC, ~20% MANUAL, ~40% NONE
          String renewalMode;
          int renewalTermMonths = 12;
          Integer maxRenewals = null;
          int landlordNoticeDays;
          int tenantNoticeDays;
          boolean requiresTenantConfirmation;
          String rentAdjType;
          BigDecimal rentAdjValue;

          int renewalBucket = totalContracts % 5;
          if (renewalBucket < 2) {
            renewalMode = "AUTOMATIC";
            landlordNoticeDays = 90;
            tenantNoticeDays = 30;
            requiresTenantConfirmation = false;
            rentAdjType = "FIXED_PERCENTAGE";
            rentAdjValue =
                BigDecimal.valueOf(2 + random.nextDouble() * 3).setScale(4, RoundingMode.HALF_UP);
          } else if (renewalBucket == 2) {
            renewalMode = "MANUAL";
            landlordNoticeDays = 90;
            tenantNoticeDays = 30;
            requiresTenantConfirmation = true;
            maxRenewals = random.nextInt(2, 6);
            rentAdjType = "MANUAL";
            rentAdjValue = null;
          } else {
            renewalMode = "NONE";
            landlordNoticeDays = 30;
            tenantNoticeDays = 30;
            requiresTenantConfirmation = false;
            rentAdjType = "NONE";
            rentAdjValue = null;
          }

          // Country metadata
          ContractCountryMetadata metadata =
              countryCode != null ? buildDemoMetadata(countryCode, currency) : null;

          // Compute realistic created_at based on contract start
          LocalDateTime createdAt;
          if (signedDate != null) {
            createdAt = signedDate.atStartOfDay().minusDays(random.nextInt(1, 14));
          } else {
            createdAt = now.minusDays(random.nextInt(1, 30));
          }

          // Mark property as occupied if last contract is ACTIVE
          if (isLastContract && "ACTIVE".equals(status)) {
            dsl.update(PROPERTIES)
                .set(PROPERTIES.STATUS, "OCCUPIED")
                .where(PROPERTIES.ID.eq(propertyId))
                .execute();
          }

          UUID contractId = UUID.randomUUID();
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
              .set(CONTRACTS.RENT_AMOUNT, rent)
              .set(CONTRACTS.DEPOSIT_AMOUNT, deposit)
              .set(CONTRACTS.SECURITY_DEPOSIT, rent)
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
              .set(
                  CONTRACTS.NOTES,
                  isLastContract
                      ? "Demo contract for testing purposes"
                      : "Historical contract (contract " + (c + 1) + " of " + chainLength + ")")
              .set(field("country_code", String.class), countryCode)
              .set(
                  field("country_metadata", JSONB.class),
                  metadata != null
                      ? JSONB.jsonb(countryMetadataSerializer.serialize(metadata))
                      : null)
              .set(CONTRACTS.CREATED_AT, createdAt)
              .set(CONTRACTS.UPDATED_AT, now)
              .set(CONTRACTS.CREATED_BY, createdBy)
              .set(CONTRACTS.UPDATED_BY, createdBy)
              .execute();

          // Insert primary contact into contract_parties
          dsl.insertInto(table("contract_parties"))
              .set(field("id", UUID.class), UUID.randomUUID())
              .set(field("identifier", String.class), newContractPartyId().value())
              .set(field("team_id", UUID.class), teamId)
              .set(field("contract_id", UUID.class), contractId)
              .set(field("contact_id", UUID.class), contactId)
              .set(field("role", String.class), "PRIMARY_TENANT")
              .set(field("created_at", LocalDateTime.class), now)
              .set(field("updated_at", LocalDateTime.class), now)
              .set(field("created_by", UUID.class), createdBy)
              .set(field("updated_by", UUID.class), createdBy)
              .execute();

          // Insert initial rent period
          UUID rentPeriodId = UUID.randomUUID();
          dsl.insertInto(table("contract_rent_periods"))
              .set(field("id", UUID.class), rentPeriodId)
              .set(field("identifier", String.class), newContractRentPeriodId().value())
              .set(field("team_id", UUID.class), teamId)
              .set(field("contract_id", UUID.class), contractId)
              .set(field("rent_amount", Long.class), rent.movePointRight(2).longValueExact())
              .set(field("currency", String.class), currency)
              .set(field("effective_from", java.sql.Date.class), java.sql.Date.valueOf(startDate))
              .set(field("created_at", LocalDateTime.class), createdAt)
              .set(field("updated_at", LocalDateTime.class), now)
              .set(field("created_by", UUID.class), createdBy)
              .set(field("updated_by", UUID.class), createdBy)
              .execute();

          // Insert rent components (~70% of contracts get a breakdown)
          if (totalContracts % 3 != 2) {
            insertRentComponents(
                contractId, rentPeriodId, teamId, createdBy, rent, currency, propertyCategory, now);
          }

          contractIds.add(contractId);
          ctx.putIdentifier(contractId, contractIdentifier);
          ctx.incrementContracts();
          totalContracts++;
        }
      }

      ctx.getContractIdsByTeam().put(teamId, contractIds);
      log.info(
          "Created {} contracts across {} properties for team {}",
          totalContracts,
          sortedProperties.size(),
          teamKey);
    }
  }

  /** Determines how many contracts a property should have based on years owned. */
  private int computeChainLength(long yearsOwned) {
    if (yearsOwned >= 20) {
      return random.nextInt(4, 6);
    } else if (yearsOwned >= 15) {
      return random.nextInt(3, 5);
    } else if (yearsOwned >= 10) {
      return random.nextInt(2, 4);
    } else if (yearsOwned >= 5) {
      return random.nextInt(1, 3);
    } else if (yearsOwned >= 2) {
      return 1;
    } else {
      return random.nextBoolean() ? 1 : 0;
    }
  }

  /**
   * Computes the end date for the last contract in a chain. Properties 27, 28 get
   * TERMINATED/EXPIRED (recent end). Property 29 gets DRAFT. Properties 0-26 get ACTIVE with no end
   * date (INDEFINITE) or future end.
   */
  @SuppressWarnings("NullAway")
  private @org.jspecify.annotations.Nullable LocalDate computeLastContractEndDate(
      int propIdx, int totalProperties, LocalDate startDate, int termMonths, LocalDate today) {
    if (propIdx == totalProperties - 1) {
      // DRAFT: future dates
      return startDate.plusMonths(termMonths);
    } else if (propIdx >= totalProperties - 3) {
      // TERMINATED/EXPIRED: recently ended
      return today.minusMonths(random.nextInt(1, 4));
    } else {
      // ACTIVE: ~50% INDEFINITE (null end), ~50% future end date
      if (random.nextBoolean()) {
        return null;
      } else {
        return today.plusMonths(random.nextInt(6, 36));
      }
    }
  }

  /** Computes the status for a contract based on property index and position in chain. */
  private String computeStatus(
      int propIdx,
      int totalProperties,
      boolean isLastContract,
      LocalDate endDate,
      LocalDate today) {
    if (!isLastContract) {
      return "EXPIRED";
    }

    // Last contract in the chain
    if (propIdx == totalProperties - 1) {
      return "DRAFT";
    } else if (propIdx == totalProperties - 2) {
      return "TERMINATED";
    } else if (propIdx == totalProperties - 3) {
      return "EXPIRED";
    } else {
      return "ACTIVE";
    }
  }

  /**
   * Deflates the current (2025) rent baseline backwards to compute the rent for a given year. Uses
   * ~3% annual appreciation.
   */
  private BigDecimal rentForYear(BigDecimal currentBaseline, int contractYear, int currentYear) {
    int yearsBack = currentYear - contractYear;
    double deflationFactor = Math.pow(0.97, yearsBack);
    return currentBaseline
        .multiply(BigDecimal.valueOf(deflationFactor))
        .setScale(0, RoundingMode.HALF_UP);
  }

  /** Returns a contract term in months based on category. */
  private int contractTermMonthsForCategory(String category) {
    return switch (category) {
      case "COMMERCIAL" -> random.nextInt(36, 121); // 3-10 years
      case "INDUSTRIAL" -> random.nextInt(60, 181); // 5-15 years
      case "AGRICULTURAL" -> random.nextInt(12, 61); // 1-5 years
      default -> random.nextInt(12, 61); // RESIDENTIAL: 1-5 years
    };
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

  @SuppressWarnings("NullAway")
  private void insertRentComponents(
      UUID contractId,
      UUID rentPeriodId,
      UUID teamId,
      @org.jspecify.annotations.Nullable UUID createdBy,
      BigDecimal rentAmount,
      String currency,
      String propertyCategory,
      LocalDateTime now) {
    // BASE_RENT: ~70% of total
    BigDecimal baseRent =
        rentAmount.multiply(BigDecimal.valueOf(0.70)).setScale(2, RoundingMode.HALF_UP);
    // UTILITIES_ADVANCE: ~15%
    BigDecimal utilities =
        rentAmount.multiply(BigDecimal.valueOf(0.15)).setScale(2, RoundingMode.HALF_UP);
    // SERVICE_COSTS: remainder
    BigDecimal serviceCosts = rentAmount.subtract(baseRent).subtract(utilities);

    int sortOrder = 0;
    insertComponent(
        contractId,
        rentPeriodId,
        teamId,
        createdBy,
        "BASE_RENT",
        baseRent,
        currency,
        null,
        sortOrder++,
        now);
    insertComponent(
        contractId,
        rentPeriodId,
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
        rentPeriodId,
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
          contractId,
          rentPeriodId,
          teamId,
          createdBy,
          "PARKING",
          parking,
          currency,
          null,
          sortOrder++,
          now);
    }
  }

  @SuppressWarnings("NullAway")
  private void insertComponent(
      UUID contractId,
      UUID rentPeriodId,
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
        .set(field("rent_period_id", UUID.class), rentPeriodId)
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

  private ContractCountryMetadata buildDemoMetadata(String countryCode, String currency) {
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
              MoneyAmount.of(BigDecimal.valueOf(150), currency),
              false,
              MoneyAmount.of(BigDecimal.valueOf(879), currency),
              "C");
      case "DE" ->
          new DeContractMetadata(
              "Mietspiegel Berlin 2024",
              true,
              "STANDARD",
              false,
              MoneyAmount.of(BigDecimal.valueOf(200), currency),
              MoneyAmount.of(BigDecimal.valueOf(2100), currency),
              3,
              "VERBRAUCH",
              "C",
              BigDecimal.valueOf(125));
      case "FR" ->
          new FrContractMetadata(
              MoneyAmount.of(BigDecimal.valueOf(25, 1), currency),
              MoneyAmount.of(BigDecimal.valueOf(30), currency),
              true,
              true,
              "C",
              false,
              false,
              true,
              true,
              true,
              true,
              MoneyAmount.of(BigDecimal.valueOf(900), currency),
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
              "AST",
              "DPS",
              "C",
              true,
              true,
              true,
              MoneyAmount.of(BigDecimal.valueOf(1500), currency));
      case "US" ->
          new UsContractMetadata(
              "NY",
              true,
              "New York City",
              false,
              true,
              MoneyAmount.of(BigDecimal.valueOf(5000), currency),
              2);
      case "ES" ->
          new EsContractMetadata(
              true,
              false,
              BigDecimal.valueOf(1200),
              MoneyAmount.of(BigDecimal.valueOf(800), currency),
              1,
              "D",
              MoneyAmount.of(BigDecimal.valueOf(1600), currency),
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
              MoneyAmount.of(BigDecimal.valueOf(2400), currency),
              3);
      case "AT" ->
          new AtContractMetadata(
              "MRG",
              "B",
              MoneyAmount.of(BigDecimal.valueOf(180), currency),
              3,
              MoneyAmount.of(BigDecimal.valueOf(2400), currency),
              true,
              MoneyAmount.of(BigDecimal.valueOf(6.5), currency),
              "EA-2024-AT-001");
      case "CH" ->
          new ChContractMetadata(
              "ZH",
              "Zurich",
              MoneyAmount.of(BigDecimal.valueOf(250), currency),
              3,
              MoneyAmount.of(BigDecimal.valueOf(4500), currency),
              true,
              BigDecimal.valueOf(1.5));
      case "DK" ->
          new DkContractMetadata(
              "PRIVATE",
              "C",
              MoneyAmount.of(BigDecimal.valueOf(30000), currency),
              3,
              MoneyAmount.of(BigDecimal.valueOf(30000), currency),
              3,
              true);
      case "SE" ->
          new SeContractMetadata(
              "PRIVATE", true, "C", MoneyAmount.of(BigDecimal.valueOf(25000), currency), 3, true);
      case "FI" ->
          new FiContractMetadata(
              "INDEFINITE", "C", MoneyAmount.of(BigDecimal.valueOf(1500), currency), 2, false);
      case "NO" ->
          new NoContractMetadata(
              "RESIDENTIAL",
              "C",
              MoneyAmount.of(BigDecimal.valueOf(30000), currency),
              3,
              true,
              false);
      case "IE" ->
          new IeContractMetadata(
              "PART4",
              "B2",
              MoneyAmount.of(BigDecimal.valueOf(2000), currency),
              1,
              true,
              true,
              MoneyAmount.of(BigDecimal.valueOf(1800), currency),
              "BER-2024-IE-001");
      case "PL" ->
          new PlContractMetadata(
              "ZWYKLY",
              "C",
              MoneyAmount.of(BigDecimal.valueOf(4000), currency),
              2,
              true,
              "EC-2024-PL-001",
              true);
      case "CZ" ->
          new CzContractMetadata(
              "INDEFINITE",
              "C",
              MoneyAmount.of(BigDecimal.valueOf(30000), currency),
              3,
              MoneyAmount.of(BigDecimal.valueOf(5000), currency),
              false);
      case "HU" ->
          new HuContractMetadata(
              "DEFINITE",
              "CC",
              MoneyAmount.of(BigDecimal.valueOf(300000), currency),
              2,
              MoneyAmount.of(BigDecimal.valueOf(25000), currency),
              false);
      case "RO" ->
          new RoContractMetadata(
              "DEFINITE",
              "C",
              MoneyAmount.of(BigDecimal.valueOf(3000), currency),
              2,
              true,
              MoneyAmount.of(BigDecimal.valueOf(500), currency));
      case "GR" ->
          new GrContractMetadata(
              "RESIDENTIAL",
              "C",
              MoneyAmount.of(BigDecimal.valueOf(1500), currency),
              2,
              false,
              MoneyAmount.of(BigDecimal.valueOf(80), currency),
              "TAX-2024-GR-001");
      case "BR" ->
          new BrContractMetadata(
              "RESIDENCIAL",
              MoneyAmount.of(BigDecimal.valueOf(5000), currency),
              3,
              false,
              MoneyAmount.of(BigDecimal.valueOf(800), currency),
              "MAT-2024-BR-001",
              false);
      case "CA" ->
          new CaContractMetadata(
              "ON", true, MoneyAmount.of(BigDecimal.valueOf(2000), currency), 2, true, null);
      case "MX" ->
          new MxContractMetadata(
              "CDMX",
              "DEFINITE",
              MoneyAmount.of(BigDecimal.valueOf(15000), currency),
              1,
              false,
              MoneyAmount.of(BigDecimal.valueOf(2000), currency));
      default -> new GenericContractMetadata(null, null, null, null, null, "Demo generic metadata");
    };
  }
}
