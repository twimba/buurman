package com.buurman.service.demo;

import static com.buurman.jooq.generated.Tables.CONTRACTS;
import static com.buurman.jooq.generated.Tables.PROPERTIES;
import static com.buurman.util.SidGenerator.newContractId;
import static com.buurman.util.SidGenerator.newContractPartyId;
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
import com.buurman.util.CurrencyUtils;

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

  private static final String[] CONTRACT_TYPES = {
    "FIXED_TERM", "INDEFINITE", "FURNISHED", "UNFURNISHED"
  };

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

        BigDecimal rentAmount = rentAmountForCategory(propertyCategory);
        BigDecimal deposit = rentAmount.multiply(depositMultiplierForCategory(propertyCategory));
        BigDecimal securityDeposit = rentAmount;
        String paymentFrequency = paymentFrequencyForCategory(propertyCategory);
        int terminationNoticeDays = terminationNoticeForCategory(propertyCategory);

        // Resolve country from property
        String propertyCountry =
            dsl.select(PROPERTIES.COUNTRY)
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
            .set(CONTRACTS.RENT_AMOUNT, CurrencyUtils.toMinorUnits(rentAmount, currency))
            .set(CONTRACTS.DEPOSIT_AMOUNT, CurrencyUtils.toMinorUnits(deposit, currency))
            .set(CONTRACTS.SECURITY_DEPOSIT, CurrencyUtils.toMinorUnits(securityDeposit, currency))
            .set(CONTRACTS.RENT_AMOUNT_CURRENCY, currency)
            .set(CONTRACTS.DEPOSIT_AMOUNT_CURRENCY, currency)
            .set(CONTRACTS.SECURITY_DEPOSIT_CURRENCY, currency)
            .set(CONTRACTS.PAYMENT_FREQUENCY, paymentFrequency)
            .set(CONTRACTS.PAYMENT_DUE_DAY, 1)
            .set(CONTRACTS.AUTO_RENEWAL, "INDEFINITE".equals(contractType))
            .set(CONTRACTS.RENEWAL_NOTICE_DAYS, 30)
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

        contractIds.add(contractId);
        ctx.putIdentifier(contractId, contractIdentifier);
        ctx.incrementContracts();
      }

      ctx.getContractIdsByTeam().put(teamId, contractIds);
      log.info("Created {} contracts for team {}", contractCount, teamKey);
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
              BigDecimal.valueOf(150),
              false,
              BigDecimal.valueOf(879),
              "C");
      case "DE" ->
          new DeContractMetadata(
              "Mietspiegel Berlin 2024",
              true,
              "STANDARD",
              false,
              BigDecimal.valueOf(200),
              BigDecimal.valueOf(2100),
              3,
              "VERBRAUCH",
              "C",
              BigDecimal.valueOf(125));
      case "FR" ->
          new FrContractMetadata(
              BigDecimal.valueOf(25, 1),
              BigDecimal.valueOf(30),
              true,
              true,
              "C",
              false,
              false,
              true,
              true,
              true,
              true,
              BigDecimal.valueOf(900),
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
          new UkContractMetadata("AST", "DPS", "C", true, true, true, BigDecimal.valueOf(1500));
      case "US" ->
          new UsContractMetadata(
              "NY", true, "New York City", false, true, BigDecimal.valueOf(5000), 2);
      case "ES" ->
          new EsContractMetadata(
              true,
              false,
              BigDecimal.valueOf(1200),
              BigDecimal.valueOf(800),
              1,
              "D",
              BigDecimal.valueOf(1600),
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
              BigDecimal.valueOf(2400),
              3);
      case "AT" ->
          new AtContractMetadata(
              "MRG",
              "B",
              BigDecimal.valueOf(180),
              3,
              BigDecimal.valueOf(2400),
              true,
              BigDecimal.valueOf(6.5),
              "EA-2024-AT-001");
      case "CH" ->
          new ChContractMetadata(
              "ZH",
              "Zurich",
              BigDecimal.valueOf(250),
              3,
              BigDecimal.valueOf(4500),
              true,
              BigDecimal.valueOf(1.5));
      case "DK" ->
          new DkContractMetadata(
              "PRIVATE", "C", BigDecimal.valueOf(30000), 3, BigDecimal.valueOf(30000), 3, true);
      case "SE" -> new SeContractMetadata("PRIVATE", true, "C", BigDecimal.valueOf(25000), 3, true);
      case "FI" -> new FiContractMetadata("INDEFINITE", "C", BigDecimal.valueOf(1500), 2, false);
      case "NO" ->
          new NoContractMetadata("RESIDENTIAL", "C", BigDecimal.valueOf(30000), 3, true, false);
      case "IE" ->
          new IeContractMetadata(
              "PART4",
              "B2",
              BigDecimal.valueOf(2000),
              1,
              true,
              true,
              BigDecimal.valueOf(1800),
              "BER-2024-IE-001");
      case "PL" ->
          new PlContractMetadata(
              "ZWYKLY", "C", BigDecimal.valueOf(4000), 2, true, "EC-2024-PL-001", true);
      case "CZ" ->
          new CzContractMetadata(
              "INDEFINITE", "C", BigDecimal.valueOf(30000), 3, BigDecimal.valueOf(5000), false);
      case "HU" ->
          new HuContractMetadata(
              "DEFINITE", "CC", BigDecimal.valueOf(300000), 2, BigDecimal.valueOf(25000), false);
      case "RO" ->
          new RoContractMetadata(
              "DEFINITE", "C", BigDecimal.valueOf(3000), 2, true, BigDecimal.valueOf(500));
      case "GR" ->
          new GrContractMetadata(
              "RESIDENTIAL",
              "C",
              BigDecimal.valueOf(1500),
              2,
              false,
              BigDecimal.valueOf(80),
              "TAX-2024-GR-001");
      case "BR" ->
          new BrContractMetadata(
              "RESIDENCIAL",
              BigDecimal.valueOf(5000),
              3,
              false,
              BigDecimal.valueOf(800),
              "MAT-2024-BR-001",
              false);
      case "CA" -> new CaContractMetadata("ON", true, BigDecimal.valueOf(2000), 2, true, null);
      case "MX" ->
          new MxContractMetadata(
              "CDMX", "DEFINITE", BigDecimal.valueOf(15000), 1, false, BigDecimal.valueOf(2000));
      default -> new GenericContractMetadata(null, null, null, null, null, "Demo generic metadata");
    };
  }
}
