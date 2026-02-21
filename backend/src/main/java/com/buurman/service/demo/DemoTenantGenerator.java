package com.buurman.service.demo;

import static com.buurman.jooq.generated.Tables.TENANTS;
import static com.buurman.jooq.generated.Tables.TENANT_ADDRESSES;
import static com.buurman.util.UlidGenerator.newTenantAddressId;
import static com.buurman.util.UlidGenerator.newTenantId;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
public class DemoTenantGenerator {

  private final DSLContext dsl;
  private final Clock clock;
  private final Faker faker = new Faker(Locale.ENGLISH, new Random(42));
  private final Random random = new Random(42);

  public static final int TENANTS_PER_TEAM = 8;

  private static final String[] DUTCH_CITIES = {
    "Amsterdam", "Rotterdam", "Den Haag", "Utrecht", "Eindhoven",
    "Tilburg", "Groningen", "Almere", "Breda", "Nijmegen"
  };

  // Business company names per country
  private static final String[][] BUSINESS_NAMES = {
    // NL
    {
      "TechVentures B.V.",
      "Van der Berg Logistics B.V.",
      "Bakkerij De Gouden Oven B.V.",
      "Noord-Holland Consultancy B.V.",
      "Groen Energie B.V."
    },
    // DE
    {
      "Müller Maschinenbau GmbH",
      "Schmidt & Partners GmbH",
      "Brauereigesellschaft GmbH",
      "Berliner Tech Solutions GmbH",
      "Rhein Logistik GmbH"
    },
    // UK
    {
      "Hartley & Sons Ltd",
      "Crown Industrial Services Ltd",
      "The Old Mill Trading Co. Ltd",
      "Brighton Digital Solutions Ltd",
      "Thames Warehousing Ltd"
    },
    // FR
    {
      "Boulangerie Martin SARL",
      "Groupe Industriel Dupont SARL",
      "Vignobles du Sud SARL",
      "Paris Consulting SARL",
      "Lyon Distribution SARL"
    },
    // ES
    {
      "Construcciones García S.L.",
      "Viñedos del Sur S.A.",
      "Olivares de Andalucía S.L.",
      "Barcelona Tech S.L.",
      "Transportes Madrid S.A."
    },
    // PT
    {
      "Vinhos do Douro Lda.",
      "Oliveira & Filhos Lda.",
      "Porto Logística Lda.",
      "Lisboa Digital Lda.",
      "Algarve Imobiliária Lda."
    }
  };

  private static final String[] BUSINESS_DOMAINS = {".nl", ".de", ".co.uk", ".fr", ".es", ".pt"};

  public void generate(DemoDataContext ctx) {
    LocalDateTime now = LocalDateTime.now(clock);

    for (var teamEntry : ctx.getTeamIds().entrySet()) {
      String teamKey = teamEntry.getKey();
      UUID teamId = teamEntry.getValue();
      UUID createdBy = ctx.getAdminUserForTeam(teamKey);
      List<UUID> tenantIds = new ArrayList<>();

      int businessStart = (int) (TENANTS_PER_TEAM * 0.6); // first 60% individual, rest business

      for (int i = 0; i < TENANTS_PER_TEAM; i++) {
        UUID tenantId = UUID.randomUUID();
        boolean isBusiness = i >= businessStart;

        String firstName;
        String lastName;
        String email;
        String phone;
        String taxNumber;
        String additionalInfo = null;

        if (isBusiness) {
          int countryIdx = (i - businessStart) % BUSINESS_NAMES.length;
          String[] names = BUSINESS_NAMES[countryIdx];
          String companyName = names[random.nextInt(names.length)];
          String domain = BUSINESS_DOMAINS[countryIdx];

          firstName = faker.name().firstName(); // contact person
          lastName = companyName;
          String slug =
              companyName
                  .toLowerCase()
                  .replaceAll("[^a-z0-9]+", "")
                  .substring(0, Math.min(15, companyName.replaceAll("[^a-z0-9]+", "").length()));
          email = "info@" + slug + domain;
          // Landline format
          phone = "+3120" + String.format("%07d", random.nextInt(1000000, 9999999));
          taxNumber = "NL" + String.format("%09d", random.nextInt(100000000, 999999999)) + "B01";
          additionalInfo = "Business tenant - " + companyName;
        } else {
          firstName = faker.name().firstName();
          lastName = faker.name().lastName();
          email =
              (firstName.toLowerCase()
                      + "."
                      + lastName.toLowerCase()
                      + "."
                      + teamKey.replace("-", "")
                      + i
                      + "@example.com")
                  .replaceAll("[^a-z0-9.@]", "");
          phone = "+316" + String.format("%08d", random.nextInt(10000000, 99999999));
          taxNumber = "NL" + String.format("%09d", random.nextInt(100000000, 999999999)) + "B01";
        }

        String tenantIdentifier = newTenantId().value();
        dsl.insertInto(TENANTS)
            .set(TENANTS.ID, tenantId)
            .set(TENANTS.IDENTIFIER, tenantIdentifier)
            .set(TENANTS.TEAM_ID, teamId)
            .set(TENANTS.FIRST_NAME, firstName)
            .set(TENANTS.LAST_NAME, lastName)
            .set(TENANTS.EMAIL, email)
            .set(TENANTS.PHONE, phone)
            .set(TENANTS.TAX_NUMBER, taxNumber)
            .set(TENANTS.ID_NUMBER, String.format("%09d", random.nextInt(100000000, 999999999)))
            .set(TENANTS.ADDITIONAL_INFO, additionalInfo)
            .set(TENANTS.CREATED_AT, now.minusDays(random.nextInt(30, 365)))
            .set(TENANTS.UPDATED_AT, now)
            .set(TENANTS.CREATED_BY, createdBy)
            .set(TENANTS.UPDATED_BY, createdBy)
            .execute();

        // Add CURRENT address for each tenant (one active per tenant allowed)
        String city = DUTCH_CITIES[random.nextInt(DUTCH_CITIES.length)];
        int houseNum = random.nextInt(1, 200);
        String postalCode =
            String.format(
                "%04d %s",
                random.nextInt(1000, 9999),
                "" + (char) ('A' + random.nextInt(26)) + (char) ('A' + random.nextInt(26)));

        dsl.insertInto(TENANT_ADDRESSES)
            .set(TENANT_ADDRESSES.ID, UUID.randomUUID())
            .set(TENANT_ADDRESSES.IDENTIFIER, newTenantAddressId().value())
            .set(TENANT_ADDRESSES.TENANT_ID, tenantId)
            .set(TENANT_ADDRESSES.TEAM_ID, teamId)
            .set(TENANT_ADDRESSES.STREET, faker.address().streetName() + " " + houseNum)
            .set(TENANT_ADDRESSES.CITY, city)
            .set(TENANT_ADDRESSES.POSTAL_CODE, postalCode)
            .set(TENANT_ADDRESSES.COUNTRY, "Netherlands")
            .set(TENANT_ADDRESSES.ADDRESS_TYPE, "CURRENT")
            .set(TENANT_ADDRESSES.STATUS, "ACTIVE")
            .set(TENANT_ADDRESSES.LATITUDE, BigDecimal.valueOf(51.8 + random.nextDouble() * 1.2))
            .set(TENANT_ADDRESSES.LONGITUDE, BigDecimal.valueOf(4.0 + random.nextDouble() * 2.0))
            .set(TENANT_ADDRESSES.CREATED_AT, now)
            .set(TENANT_ADDRESSES.UPDATED_AT, now)
            .set(TENANT_ADDRESSES.CREATED_BY, createdBy)
            .set(TENANT_ADDRESSES.UPDATED_BY, createdBy)
            .execute();

        tenantIds.add(tenantId);
        ctx.putIdentifier(tenantId, tenantIdentifier);
        ctx.putBusinessTenantFlag(tenantId, isBusiness);
        ctx.incrementTenants();
      }

      ctx.getTenantIdsByTeam().put(teamId, tenantIds);
      log.info("Created {} tenants for team {}", TENANTS_PER_TEAM, teamKey);
    }
  }
}
