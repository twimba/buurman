package com.buurman.service.demo;

import static com.buurman.jooq.generated.Tables.TENANTS;
import static com.buurman.jooq.generated.Tables.TENANT_ADDRESSES;
import static com.buurman.util.SidGenerator.newTenantAddressId;
import static com.buurman.util.SidGenerator.newTenantId;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import com.buurman.domain.Sid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;

@Component
@Slf4j
@RequiredArgsConstructor
public class DemoTenantGenerator {

  private final DSLContext dsl;
  private final Clock clock;
  private final Random random = new Random(42);

  public static final int TENANTS_PER_TEAM = 55;

  private static final String[] COUNTRY_CODES = {
    "NL", "DE", "GB", "FR", "ES", "PT", "BE", "IT", "AT", "CH", "US", "IE"
  };

  private static final Map<String, Faker> COUNTRY_FAKERS =
      Map.ofEntries(
          Map.entry("NL", new Faker(Locale.of("nl"), new Random(42))),
          Map.entry("DE", new Faker(Locale.GERMAN, new Random(42))),
          Map.entry("GB", new Faker(Locale.UK, new Random(42))),
          Map.entry("FR", new Faker(Locale.FRENCH, new Random(42))),
          Map.entry("ES", new Faker(Locale.of("es"), new Random(42))),
          Map.entry("PT", new Faker(Locale.of("pt"), new Random(42))),
          Map.entry("BE", new Faker(Locale.FRENCH, new Random(42))),
          Map.entry("IT", new Faker(Locale.ITALIAN, new Random(42))),
          Map.entry("AT", new Faker(Locale.GERMAN, new Random(42))),
          Map.entry("CH", new Faker(Locale.GERMAN, new Random(42))),
          Map.entry("US", new Faker(Locale.US, new Random(42))),
          Map.entry("IE", new Faker(Locale.UK, new Random(42))));

  private static final Map<String, String[]> COUNTRY_CITIES =
      Map.ofEntries(
          Map.entry(
              "NL",
              new String[] {
                "Amsterdam",
                "Rotterdam",
                "Den Haag",
                "Utrecht",
                "Eindhoven",
                "Leiden",
                "Haarlem",
                "Delft"
              }),
          Map.entry(
              "DE",
              new String[] {
                "Berlin",
                "Munich",
                "Hamburg",
                "Frankfurt",
                "Cologne",
                "Stuttgart",
                "Düsseldorf",
                "Dresden"
              }),
          Map.entry(
              "GB",
              new String[] {
                "London",
                "Manchester",
                "Birmingham",
                "Edinburgh",
                "Bristol",
                "Liverpool",
                "Leeds",
                "Oxford"
              }),
          Map.entry(
              "FR",
              new String[] {
                "Paris", "Lyon", "Marseille", "Toulouse", "Nice", "Bordeaux", "Strasbourg", "Nantes"
              }),
          Map.entry(
              "ES",
              new String[] {
                "Madrid",
                "Barcelona",
                "Valencia",
                "Seville",
                "Málaga",
                "Bilbao",
                "Zaragoza",
                "Palma"
              }),
          Map.entry(
              "PT",
              new String[] {
                "Lisbon", "Porto", "Braga", "Coimbra", "Faro", "Funchal", "Aveiro", "Évora"
              }),
          Map.entry(
              "BE",
              new String[] {
                "Brussels", "Antwerp", "Ghent", "Bruges", "Liège", "Namur", "Leuven", "Mechelen"
              }),
          Map.entry(
              "IT",
              new String[] {
                "Rome", "Milan", "Florence", "Naples", "Turin", "Bologna", "Venice", "Genoa"
              }),
          Map.entry(
              "AT",
              new String[] {
                "Vienna", "Graz", "Linz", "Salzburg", "Innsbruck", "Klagenfurt", "Villach", "Wels"
              }),
          Map.entry(
              "CH",
              new String[] {
                "Zurich",
                "Geneva",
                "Bern",
                "Basel",
                "Lausanne",
                "Lucerne",
                "Winterthur",
                "St. Gallen"
              }),
          Map.entry(
              "US",
              new String[] {
                "New York",
                "Los Angeles",
                "Chicago",
                "Houston",
                "Phoenix",
                "San Francisco",
                "Seattle",
                "Boston"
              }),
          Map.entry(
              "IE",
              new String[] {
                "Dublin",
                "Cork",
                "Galway",
                "Limerick",
                "Waterford",
                "Kilkenny",
                "Dundalk",
                "Drogheda"
              }));

  // Business company names per country (indexed by COUNTRY_CODES order)
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
    // GB
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
    },
    // BE
    {
      "Chocolaterie Belge SPRL",
      "Bruxelles Consulting SPRL",
      "Antwerp Logistics NV",
      "Flanders Textiles NV",
      "Ardennes Tourism SPRL"
    },
    // IT
    {
      "Ristorante Bella Vita S.r.l.",
      "Milano Fashion S.r.l.",
      "Toscana Vini S.r.l.",
      "Roma Digital S.r.l.",
      "Napoli Shipping S.r.l."
    },
    // AT
    {
      "Wiener Kaffeehaus GmbH",
      "Alpen Tourismus GmbH",
      "Salzburger Festspiele GmbH",
      "Graz Technologie GmbH",
      "Innsbruck Ski GmbH"
    },
    // CH
    {
      "Zürich Finanz AG",
      "Genève Horlogerie SA",
      "Bern Consulting AG",
      "Basel Pharma AG",
      "Lugano Immobilien AG"
    },
    // US
    {
      "Manhattan Properties LLC",
      "Pacific Coast Ventures LLC",
      "Midwest Industrial Inc.",
      "Southern Hospitality LLC",
      "Great Lakes Trading LLC"
    },
    // IE
    {
      "Dublin Tech Solutions Ltd",
      "Cork Harbour Trading Ltd",
      "Galway Bay Fisheries Ltd",
      "Shamrock Consulting Ltd",
      "Celtic Properties Ltd"
    }
  };

  private static final String[] BUSINESS_DOMAINS = {
    ".nl", ".de", ".co.uk", ".fr", ".es", ".pt", ".be", ".it", ".at", ".ch", ".com", ".ie"
  };

  public void generate(DemoDataContext ctx) {
    LocalDateTime now = LocalDateTime.now(clock);

    for (var teamEntry : ctx.getTeamIds().entrySet()) {
      String teamKey = teamEntry.getKey();
      UUID teamId = teamEntry.getValue();
      UUID createdBy = ctx.getAdminUserForTeam(teamKey).orElse(null);
      List<UUID> tenantIds = new ArrayList<>();

      int businessStart = (int) (TENANTS_PER_TEAM * 0.6); // first 60% individual, rest business

      for (int i = 0; i < TENANTS_PER_TEAM; i++) {
        UUID tenantId = UUID.randomUUID();
        boolean isBusiness = i >= businessStart;
        String country = COUNTRY_CODES[i % COUNTRY_CODES.length];
        Faker countryFaker = Objects.requireNonNull(COUNTRY_FAKERS.get(country));

        String firstName;
        String lastName;
        String email;
        String phone;
        String taxNumber;
        String additionalInfo = null;

        if (isBusiness) {
          int countryIdx = indexOf(country);
          String[] names = BUSINESS_NAMES[countryIdx];
          String companyName = names[random.nextInt(names.length)];
          String domain = BUSINESS_DOMAINS[countryIdx];

          firstName = countryFaker.name().firstName(); // contact person
          lastName = companyName;
          String slug =
              companyName
                  .toLowerCase(Locale.ROOT)
                  .replaceAll("[^a-z0-9]+", "")
                  .substring(0, Math.min(15, companyName.replaceAll("[^a-z0-9]+", "").length()));
          String teamSlug = teamKey.replace("-", "");
          email = "info." + teamSlug + "." + i + "@" + slug + domain;
          phone = phoneForCountry(country, random);
          taxNumber = taxIdForCountry(country, random);
          additionalInfo = "Business tenant - " + companyName;
        } else {
          firstName = countryFaker.name().firstName();
          lastName = countryFaker.name().lastName();
          email =
              (firstName.toLowerCase(Locale.ROOT)
                      + "."
                      + lastName.toLowerCase(Locale.ROOT)
                      + "."
                      + teamKey.replace("-", "")
                      + i
                      + "@example.com")
                  .replaceAll("[^a-z0-9.@]", "");
          phone = phoneForCountry(country, random);
          taxNumber = taxIdForCountry(country, random);
        }

        Sid tenantIdentifier = newTenantId();
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
            .set(TENANTS.CREATED_AT, now.minusDays(random.nextInt(30, 3650)))
            .set(TENANTS.UPDATED_AT, now)
            .set(TENANTS.CREATED_BY, createdBy)
            .set(TENANTS.UPDATED_BY, createdBy)
            .execute();

        // Add CURRENT address for each tenant (one active per tenant allowed)
        String[] cities = Objects.requireNonNull(COUNTRY_CITIES.get(country));
        String city = cities[random.nextInt(cities.length)];
        int houseNum = random.nextInt(1, 200);
        String postalCode = postalCodeForCountry(country, random);
        BigDecimal[] latLon = latLonForCountry(country, random);

        dsl.insertInto(TENANT_ADDRESSES)
            .set(TENANT_ADDRESSES.ID, UUID.randomUUID())
            .set(TENANT_ADDRESSES.IDENTIFIER, newTenantAddressId())
            .set(TENANT_ADDRESSES.TENANT_ID, tenantId)
            .set(TENANT_ADDRESSES.TEAM_ID, teamId)
            .set(TENANT_ADDRESSES.STREET, countryFaker.address().streetName() + " " + houseNum)
            .set(TENANT_ADDRESSES.CITY, city)
            .set(TENANT_ADDRESSES.POSTAL_CODE, postalCode)
            .set(TENANT_ADDRESSES.COUNTRY_CODE, country)
            .set(TENANT_ADDRESSES.ADDRESS_TYPE, "CURRENT")
            .set(TENANT_ADDRESSES.STATUS, "ACTIVE")
            .set(TENANT_ADDRESSES.LATITUDE, latLon[0])
            .set(TENANT_ADDRESSES.LONGITUDE, latLon[1])
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

  private static int indexOf(String country) {
    for (int i = 0; i < COUNTRY_CODES.length; i++) {
      if (COUNTRY_CODES[i].equals(country)) {
        return i;
      }
    }
    return 0;
  }

  private static String phoneForCountry(String country, Random random) {
    return switch (country) {
      case "NL" -> "+316" + String.format("%08d", random.nextInt(10000000, 99999999));
      case "DE" -> "+491" + String.format("%09d", random.nextInt(100000000, 999999999));
      case "GB" -> "+447" + String.format("%09d", random.nextInt(100000000, 999999999));
      case "FR" -> "+336" + String.format("%08d", random.nextInt(10000000, 99999999));
      case "ES" -> "+346" + String.format("%08d", random.nextInt(10000000, 99999999));
      case "PT" -> "+3519" + String.format("%08d", random.nextInt(10000000, 99999999));
      case "BE" -> "+324" + String.format("%08d", random.nextInt(10000000, 99999999));
      case "IT" -> "+393" + String.format("%09d", random.nextInt(100000000, 999999999));
      case "AT" -> "+436" + String.format("%08d", random.nextInt(10000000, 99999999));
      case "CH" -> "+417" + String.format("%08d", random.nextInt(10000000, 99999999));
      case "US" -> "+1" + String.format("%010d", random.nextLong(2000000000L, 9999999999L));
      case "IE" -> "+3538" + String.format("%08d", random.nextInt(10000000, 99999999));
      default -> "+316" + String.format("%08d", random.nextInt(10000000, 99999999));
    };
  }

  private static String taxIdForCountry(String country, Random random) {
    return switch (country) {
      case "NL" -> "NL" + String.format("%09d", random.nextInt(100000000, 999999999)) + "B01";
      case "DE" -> String.format("%011d", random.nextLong(10000000000L, 99999999999L));
      case "GB" -> String.format("%09d", random.nextInt(100000000, 999999999)); // UTR
      case "FR" -> String.format("%013d", random.nextLong(1000000000000L, 9999999999999L));
      case "ES" ->
          (char) ('A' + random.nextInt(26))
              + String.format("%08d", random.nextInt(10000000, 99999999));
      case "PT" -> String.format("%09d", random.nextInt(100000000, 999999999));
      case "BE" -> "BE0" + String.format("%09d", random.nextInt(100000000, 999999999));
      case "IT" -> "IT" + String.format("%011d", random.nextLong(10000000000L, 99999999999L));
      case "AT" -> "ATU" + String.format("%08d", random.nextInt(10000000, 99999999));
      case "CH" ->
          "CHE-"
              + String.format("%03d", random.nextInt(100, 999))
              + "."
              + String.format("%03d", random.nextInt(100, 999))
              + "."
              + String.format("%03d", random.nextInt(100, 999));
      case "US" ->
          String.format("%03d", random.nextInt(100, 999))
              + "-"
              + String.format("%02d", random.nextInt(10, 99))
              + "-"
              + String.format("%04d", random.nextInt(1000, 9999));
      case "IE" ->
          String.format("%07d", random.nextInt(1000000, 9999999))
              + (char) ('A' + random.nextInt(26));
      default -> String.format("%09d", random.nextInt(100000000, 999999999));
    };
  }

  private static String postalCodeForCountry(String country, Random random) {
    return switch (country) {
      case "NL" ->
          String.format("%04d", random.nextInt(1000, 9999))
              + " "
              + (char) ('A' + random.nextInt(26))
              + (char) ('A' + random.nextInt(26));
      case "DE" -> String.format("%05d", random.nextInt(10000, 99999));
      case "GB" ->
          ""
              + (char) ('A' + random.nextInt(26))
              + (char) ('A' + random.nextInt(26))
              + random.nextInt(1, 9)
              + " "
              + random.nextInt(1, 9)
              + (char) ('A' + random.nextInt(26))
              + (char) ('A' + random.nextInt(26));
      case "FR" -> String.format("%05d", random.nextInt(10000, 99999));
      case "ES" -> String.format("%05d", random.nextInt(10000, 52999));
      case "PT" ->
          String.format("%04d", random.nextInt(1000, 9999))
              + "-"
              + String.format("%03d", random.nextInt(100, 999));
      case "BE" -> String.format("%04d", random.nextInt(1000, 9999));
      case "IT" -> String.format("%05d", random.nextInt(10000, 99999));
      case "AT" -> String.format("%04d", random.nextInt(1000, 9999));
      case "CH" -> String.format("%04d", random.nextInt(1000, 9999));
      case "US" -> String.format("%05d", random.nextInt(10000, 99999));
      case "IE" ->
          ""
              + (char) ('A' + random.nextInt(26))
              + String.format("%02d", random.nextInt(10, 99))
              + " "
              + (char) ('A' + random.nextInt(26))
              + (char) ('A' + random.nextInt(26))
              + random.nextInt(1, 9)
              + random.nextInt(1, 9);
      default -> String.format("%05d", random.nextInt(10000, 99999));
    };
  }

  private static BigDecimal[] latLonForCountry(String country, Random random) {
    return switch (country) {
      case "NL" ->
          new BigDecimal[] {
            BigDecimal.valueOf(51.8 + random.nextDouble() * 1.2),
            BigDecimal.valueOf(4.0 + random.nextDouble() * 2.0)
          };
      case "DE" ->
          new BigDecimal[] {
            BigDecimal.valueOf(48.0 + random.nextDouble() * 6.0),
            BigDecimal.valueOf(6.0 + random.nextDouble() * 8.0)
          };
      case "GB" ->
          new BigDecimal[] {
            BigDecimal.valueOf(51.0 + random.nextDouble() * 4.0),
            BigDecimal.valueOf(-3.0 + random.nextDouble() * 3.5)
          };
      case "FR" ->
          new BigDecimal[] {
            BigDecimal.valueOf(43.0 + random.nextDouble() * 6.0),
            BigDecimal.valueOf(-1.0 + random.nextDouble() * 8.0)
          };
      case "ES" ->
          new BigDecimal[] {
            BigDecimal.valueOf(36.0 + random.nextDouble() * 7.0),
            BigDecimal.valueOf(-6.0 + random.nextDouble() * 10.0)
          };
      case "PT" ->
          new BigDecimal[] {
            BigDecimal.valueOf(37.0 + random.nextDouble() * 4.0),
            BigDecimal.valueOf(-9.5 + random.nextDouble() * 2.5)
          };
      case "BE" ->
          new BigDecimal[] {
            BigDecimal.valueOf(50.0 + random.nextDouble() * 1.5),
            BigDecimal.valueOf(3.0 + random.nextDouble() * 3.0)
          };
      case "IT" ->
          new BigDecimal[] {
            BigDecimal.valueOf(38.0 + random.nextDouble() * 8.0),
            BigDecimal.valueOf(8.0 + random.nextDouble() * 10.0)
          };
      case "AT" ->
          new BigDecimal[] {
            BigDecimal.valueOf(46.5 + random.nextDouble() * 2.0),
            BigDecimal.valueOf(10.0 + random.nextDouble() * 7.0)
          };
      case "CH" ->
          new BigDecimal[] {
            BigDecimal.valueOf(46.0 + random.nextDouble() * 2.0),
            BigDecimal.valueOf(6.0 + random.nextDouble() * 4.0)
          };
      case "US" ->
          new BigDecimal[] {
            BigDecimal.valueOf(34.0 + random.nextDouble() * 8.0),
            BigDecimal.valueOf(-122.0 + random.nextDouble() * 45.0)
          };
      case "IE" ->
          new BigDecimal[] {
            BigDecimal.valueOf(51.5 + random.nextDouble() * 2.5),
            BigDecimal.valueOf(-10.0 + random.nextDouble() * 4.0)
          };
      default -> new BigDecimal[] {BigDecimal.valueOf(52.0), BigDecimal.valueOf(5.0)};
    };
  }
}
