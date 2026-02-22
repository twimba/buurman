package com.buurman.service.demo;

import static com.buurman.jooq.generated.Tables.PROPERTIES;
import static com.buurman.jooq.generated.Tables.PROPERTY_AGRICULTURAL_DETAILS;
import static com.buurman.jooq.generated.Tables.PROPERTY_COMMERCIAL_DETAILS;
import static com.buurman.jooq.generated.Tables.PROPERTY_INDUSTRIAL_DETAILS;
import static com.buurman.jooq.generated.Tables.PROPERTY_OUTDOOR_AREAS;
import static com.buurman.jooq.generated.Tables.PROPERTY_RESIDENTIAL_DETAILS;
import static com.buurman.util.UlidGenerator.newPropertyId;
import static com.buurman.util.UlidGenerator.newPropertyOutdoorAreaId;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class DemoPropertyGenerator {

  private final DSLContext dsl;
  private final Random random = new Random(42);

  private static final String[] CONSTRUCTION_TYPES = {"BRICK", "CONCRETE", "WOOD", "MIXED"};
  private static final String[] FOUNDATION_TYPES = {
    "CONCRETE_SLAB", "CRAWL_SPACE", "BASEMENT", "PILE"
  };
  private static final String[] ROOF_TYPES = {"FLAT", "PITCHED", "HIP", "GABLE"};
  private static final String[] FLOORING_TYPES = {"HARDWOOD", "LAMINATE", "TILE", "VINYL"};
  private static final String[] WINDOW_TYPES = {"SINGLE_PANE", "DOUBLE_PANE", "TRIPLE_PANE"};
  private static final String[] HEATING_TYPES = {"CENTRAL", "DISTRICT", "HEAT_PUMP", "GAS"};
  private static final String[] ENERGY_RATINGS = {"A+", "A", "B", "C", "D"};
  private static final String[] INTERNET_TYPES = {"FIBER", "CABLE", "DSL"};
  private static final String[] PARKING_TYPES = {"GARAGE", "STREET", "UNDERGROUND", "NONE"};
  private static final String[] OUTDOOR_TYPES = {"BALCONY", "TERRACE", "GARDEN"};

  public static final int PROPERTIES_PER_TEAM = 6;

  // Country data with category-specific street pools
  private static final List<CountryData> COUNTRIES =
      List.of(
          new CountryData(
              "Netherlands",
              new String[] {
                "Keizersgracht", "Prinsengracht", "Herengracht", "Vondelstraat",
                "Beethovenstraat", "Apollolaan", "Singel", "Overtoom"
              },
              new String[] {"Zuidas", "Amstelplein", "WTC Boulevard", "Strawinskylaan"},
              new String[] {"Westpoort", "Schiphol-Rijk", "Havenweg", "Industrieweg"},
              new String[] {"Beemsterweg", "Polderweg", "Boerderijlaan", "Weideland"},
              new String[] {
                "Amsterdam", "Rotterdam", "Den Haag", "Utrecht",
                "Eindhoven", "Leiden", "Haarlem", "Delft"
              },
              "####_AA",
              51.8,
              53.0,
              4.0,
              6.0),
          new CountryData(
              "Germany",
              new String[] {
                "Friedrichstraße", "Kurfürstendamm", "Schillerstraße", "Goethestraße",
                "Berliner Straße", "Hauptstraße", "Bahnhofstraße", "Mozartstraße"
              },
              new String[] {"Potsdamer Platz", "Bankenviertel", "Geschäftsstraße", "Büropark"},
              new String[] {"Industriegebiet", "Gewerbepark", "Logistikring", "Werkstraße"},
              new String[] {"Ackerweg", "Hofstraße", "Feldmark", "Gutshof"},
              new String[] {
                "Berlin", "Munich", "Hamburg", "Frankfurt",
                "Cologne", "Stuttgart", "Düsseldorf", "Dresden"
              },
              "#####",
              48.0,
              54.0,
              6.0,
              14.0),
          new CountryData(
              "United Kingdom",
              new String[] {
                "Baker Street", "King's Road", "Church Lane", "High Street",
                "Park Avenue", "Victoria Road", "Station Road", "Mill Lane"
              },
              new String[] {"Canary Wharf", "Fenchurch Street", "Bishopsgate", "Fleet Street"},
              new String[] {"Trading Estate", "Industrial Park", "Enterprise Way", "Dock Road"},
              new String[] {"Manor Farm Road", "The Green", "Orchard Lane", "Meadow Drive"},
              new String[] {
                "London", "Manchester", "Birmingham", "Edinburgh",
                "Bristol", "Liverpool", "Oxford", "Cambridge"
              },
              "AA## #AA",
              51.0,
              56.0,
              -4.0,
              1.5),
          new CountryData(
              "France",
              new String[] {
                "Rue de Rivoli", "Avenue des Champs-Élysées", "Boulevard Saint-Germain",
                "Rue de la Paix", "Avenue Montaigne", "Rue du Faubourg",
                "Place Vendôme", "Rue de Seine"
              },
              new String[] {
                "Quartier des Affaires", "La Défense", "Rue du Commerce", "Avenue de l'Opéra"
              },
              new String[] {
                "Zone Industrielle", "Parc d'Activités", "Rue de l'Usine", "Route du Port"
              },
              new String[] {
                "Chemin du Vignoble", "Route des Champs", "Lieu-dit La Ferme", "Allée des Vergers"
              },
              new String[] {
                "Paris", "Lyon", "Marseille", "Bordeaux", "Nice", "Toulouse", "Strasbourg", "Nantes"
              },
              "#####",
              43.0,
              49.0,
              -1.0,
              7.0),
          new CountryData(
              "Spain",
              new String[] {
                "Calle Gran Vía", "Paseo de la Castellana", "Avenida Diagonal", "Calle Mayor",
                "Calle de Alcalá", "Rambla de Catalunya", "Calle Serrano", "Paseo del Prado"
              },
              new String[] {
                "Paseo de la Castellana",
                "Calle de Serrano",
                "Avenida de la Constitución",
                "Plaza de España"
              },
              new String[] {
                "Polígono Industrial", "Zona Franca", "Calle de la Industria", "Avenida del Puerto"
              },
              new String[] {
                "Camino de la Huerta",
                "Finca El Olivar",
                "Carretera de los Viñedos",
                "Calle del Campo"
              },
              new String[] {
                "Madrid", "Barcelona", "Valencia", "Seville",
                "Málaga", "Bilbao", "Granada", "San Sebastián"
              },
              "#####",
              36.0,
              43.5,
              -6.0,
              3.0),
          new CountryData(
              "Portugal",
              new String[] {
                "Rua Augusta", "Avenida da Liberdade", "Rua de Santa Catarina",
                "Rua do Carmo", "Praça do Comércio", "Rua dos Clérigos",
                "Avenida dos Aliados", "Rua da Prata"
              },
              new String[] {
                "Avenida da República", "Rua do Comércio", "Praça do Município", "Parque das Nações"
              },
              new String[] {
                "Zona Industrial", "Parque Empresarial", "Rua da Fábrica", "Estrada do Porto"
              },
              new String[] {
                "Estrada das Quintas", "Caminho do Vinhedo", "Rua da Herdade", "Largo do Olival"
              },
              new String[] {
                "Lisbon", "Porto", "Faro", "Coimbra", "Braga", "Funchal", "Aveiro", "Évora"
              },
              "####-###",
              37.0,
              42.0,
              -9.5,
              -6.0));

  record CountryData(
      String name,
      String[] residentialStreets,
      String[] commercialStreets,
      String[] industrialStreets,
      String[] agriculturalStreets,
      String[] cities,
      String postalFormat,
      double latMin,
      double latMax,
      double lonMin,
      double lonMax) {}

  private final Clock clock;

  public void generate(DemoDataContext ctx) {
    LocalDateTime now = LocalDateTime.now(clock);

    for (var teamEntry : ctx.getTeamIds().entrySet()) {
      String teamKey = teamEntry.getKey();
      UUID teamId = teamEntry.getValue();
      UUID createdBy = ctx.getAdminUserForTeam(teamKey);
      String currency = ctx.getCurrencyForTeam(teamKey);
      List<UUID> propertyIds = new ArrayList<>();

      List<String> categories = generateCategoryMix(PROPERTIES_PER_TEAM);

      for (int i = 0; i < PROPERTIES_PER_TEAM; i++) {
        UUID propertyId = UUID.randomUUID();
        String propertyCategory = categories.get(i);
        String propertyType = randomTypeForCategory(propertyCategory);
        String status = i < PROPERTIES_PER_TEAM - 1 ? "OCCUPIED" : "VACANT";

        CountryData country = COUNTRIES.get(i % COUNTRIES.size());
        String[] streets = streetsForCategory(country, propertyCategory);

        int houseNumber = random.nextInt(1, 200);
        String street = pick(streets) + " " + houseNumber;
        String city = pick(country.cities());
        String postalCode = generatePostalCode(country.postalFormat());

        double lat = country.latMin() + random.nextDouble() * (country.latMax() - country.latMin());
        double lon = country.lonMin() + random.nextDouble() * (country.lonMax() - country.lonMin());

        int yearBuilt = random.nextInt(1920, 2020);
        BigDecimal area = areaForCategory(propertyCategory);

        int bedrooms =
            switch (propertyType) {
              case "STUDIO" -> 1;
              case "APARTMENT" -> random.nextInt(1, 4);
              case "HOUSE", "VILLA", "TOWNHOUSE" -> random.nextInt(2, 6);
              default -> 0;
            };
        int bathrooms = Math.max(1, bedrooms / 2 + 1);

        // Category-specific construction attributes
        String constructionType = constructionTypeForCategory(propertyCategory);
        String foundationType = foundationTypeForCategory(propertyCategory);
        String roofType = roofTypeForCategory(propertyCategory);
        String flooringType = flooringTypeForCategory(propertyCategory);
        String heatingType = heatingTypeForCategory(propertyCategory);
        String coolingType = coolingTypeForCategory(propertyCategory);
        int floors = floorsForCategory(propertyCategory);

        String propertyIdentifier = newPropertyId().value();
        dsl.insertInto(PROPERTIES)
            .set(PROPERTIES.ID, propertyId)
            .set(PROPERTIES.IDENTIFIER, propertyIdentifier)
            .set(PROPERTIES.TEAM_ID, teamId)
            .set(PROPERTIES.STREET, street)
            .set(PROPERTIES.CITY, city)
            .set(PROPERTIES.POSTAL_CODE, postalCode)
            .set(PROPERTIES.COUNTRY, country.name())
            .set(PROPERTIES.LATITUDE, BigDecimal.valueOf(lat))
            .set(PROPERTIES.LONGITUDE, BigDecimal.valueOf(lon))
            .set(PROPERTIES.PROPERTY_CATEGORY, propertyCategory)
            .set(PROPERTIES.AREA_VALUE, area)
            .set(PROPERTIES.AREA_UNIT, "sqm")
            .set(PROPERTIES.PROPERTY_TYPE, propertyType)
            .set(PROPERTIES.STATUS, status)
            .set(PROPERTIES.YEAR_BUILT, yearBuilt)
            .set(
                PROPERTIES.YEAR_LAST_RENOVATED,
                yearBuilt < 2000 ? yearBuilt + random.nextInt(5, 30) : null)
            .set(PROPERTIES.CONSTRUCTION_TYPE, constructionType)
            .set(PROPERTIES.FOUNDATION_TYPE, foundationType)
            .set(PROPERTIES.ROOF_TYPE, roofType)
            .set(PROPERTIES.FLOORING_TYPE, flooringType)
            .set(PROPERTIES.WINDOW_TYPE, pick(WINDOW_TYPES))
            .set(PROPERTIES.NUMBER_OF_FLOORS, floors)
            .set(PROPERTIES.ENERGY_EFFICIENCY_RATING, pick(ENERGY_RATINGS))
            .set(
                PROPERTIES.ENERGY_CERTIFICATE_EXPIRY_DATE,
                LocalDate.now(clock).plusYears(random.nextInt(1, 5)))
            .set(PROPERTIES.HEATING_TYPE, heatingType)
            .set(PROPERTIES.COOLING_TYPE, coolingType)
            .set(PROPERTIES.HOT_WATER_SYSTEM, "BOILER")
            .set(PROPERTIES.ELECTRICITY_CONNECTION_TYPE, "MUNICIPAL")
            .set(
                PROPERTIES.ELECTRICITY_CAPACITY_AMPS,
                "INDUSTRIAL".equals(propertyCategory) ? 63 : random.nextBoolean() ? 25 : 35)
            .set(PROPERTIES.WATER_CONNECTION_TYPE, "MUNICIPAL")
            .set(
                PROPERTIES.HAS_GAS_CONNECTION,
                !"AGRICULTURAL".equals(propertyCategory) && random.nextBoolean())
            .set(
                PROPERTIES.SEWAGE_TYPE,
                "AGRICULTURAL".equals(propertyCategory) ? "SEPTIC" : "MUNICIPAL")
            .set(PROPERTIES.INTERNET_CONNECTION_TYPE, pick(INTERNET_TYPES))
            .set(PROPERTIES.INTERNET_MAX_SPEED_MBPS, random.nextBoolean() ? 500 : 1000)
            .set(PROPERTIES.INTERNET_STATUS, "ACTIVE")
            .set(
                PROPERTIES.PARKING_SPACES,
                "INDUSTRIAL".equals(propertyCategory)
                    ? random.nextInt(5, 20)
                    : random.nextInt(0, 3))
            .set(
                PROPERTIES.PARKING_TYPE,
                "AGRICULTURAL".equals(propertyCategory) ? "NONE" : pick(PARKING_TYPES))
            .set(PROPERTIES.HAS_SMOKE_DETECTORS, true)
            .set(PROPERTIES.HAS_CO_DETECTORS, random.nextBoolean())
            .set(PROPERTIES.HAS_FIRE_EXTINGUISHER, !"AGRICULTURAL".equals(propertyCategory))
            .set(
                PROPERTIES.HAS_SPRINKLER_SYSTEM,
                "INDUSTRIAL".equals(propertyCategory) && random.nextBoolean())
            .set(
                PROPERTIES.HAS_ALARM_SYSTEM,
                !"AGRICULTURAL".equals(propertyCategory) && random.nextBoolean())
            .set(
                PROPERTIES.HAS_SECURITY_CAMERAS,
                "INDUSTRIAL".equals(propertyCategory) || "COMMERCIAL".equals(propertyCategory))
            .set(
                PROPERTIES.HAS_SECURE_ENTRY,
                !"AGRICULTURAL".equals(propertyCategory) && random.nextBoolean())
            .set(
                PROPERTIES.IS_WHEELCHAIR_ACCESSIBLE,
                "COMMERCIAL".equals(propertyCategory) || random.nextInt(5) == 0)
            .set(PROPERTIES.HAS_ELEVATOR, "APARTMENT".equals(propertyType) && random.nextBoolean())
            .set(
                PROPERTIES.HAS_STEP_FREE_ENTRANCE,
                "COMMERCIAL".equals(propertyCategory) || random.nextInt(3) == 0)
            .set(PROPERTIES.HAS_ADAPTED_BATHROOM, false)
            // Financial data — varied scenarios for first 4 properties
            .set(
                PROPERTIES.PURCHASE_PRICE,
                i < 4 ? purchasePriceForCategory(propertyCategory, i) : null)
            .set(PROPERTIES.PURCHASE_PRICE_CURRENCY, i < 4 ? currency : null)
            .set(PROPERTIES.PURCHASE_DATE, i < 4 ? LocalDate.of(2020 + i, 3, 15) : null)
            .set(
                PROPERTIES.CURRENT_MARKET_VALUE,
                i < 4 ? marketValueForCategory(propertyCategory, i) : null)
            .set(PROPERTIES.CURRENT_MARKET_VALUE_CURRENCY, i < 4 ? currency : null)
            .set(PROPERTIES.MARKET_VALUE_DATE, i < 4 ? LocalDate.of(2025, 12, 1) : null)
            .set(PROPERTIES.MORTGAGE_TYPE, i == 0 ? "NONE" : i < 4 ? "FIXED_RATE" : null)
            .set(
                PROPERTIES.MORTGAGE_AMOUNT,
                i > 0 && i < 4 ? Long.valueOf((150_000 + i * 50_000) * 100L) : null)
            .set(PROPERTIES.MORTGAGE_AMOUNT_CURRENCY, i > 0 && i < 4 ? currency : null)
            .set(
                PROPERTIES.MORTGAGE_INTEREST_RATE,
                i > 0 && i < 4 ? new BigDecimal("3." + (i * 2) + "50") : null)
            .set(
                PROPERTIES.MORTGAGE_START_DATE,
                i > 0 && i < 4 ? LocalDate.of(2020 + i, 4, 1) : null)
            .set(
                PROPERTIES.MORTGAGE_END_DATE, i > 0 && i < 4 ? LocalDate.of(2050 + i, 3, 31) : null)
            .set(
                PROPERTIES.MONTHLY_MORTGAGE_PAYMENT,
                i > 0 && i < 4 ? Long.valueOf((600 + i * 150) * 100L) : null)
            .set(PROPERTIES.MONTHLY_MORTGAGE_PAYMENT_CURRENCY, i > 0 && i < 4 ? currency : null)
            .set(
                PROPERTIES.ANNUAL_PROPERTY_TAX,
                i < 4 ? Long.valueOf((1200 + i * 300) * 100L) : null)
            .set(PROPERTIES.ANNUAL_PROPERTY_TAX_CURRENCY, i < 4 ? currency : null)
            .set(PROPERTIES.ANNUAL_INSURANCE, i < 4 ? Long.valueOf((400 + i * 100) * 100L) : null)
            .set(PROPERTIES.ANNUAL_INSURANCE_CURRENCY, i < 4 ? currency : null)
            .set(PROPERTIES.ANNUAL_HOA_FEE, i < 3 ? Long.valueOf((600 + i * 200) * 100L) : null)
            .set(PROPERTIES.ANNUAL_HOA_FEE_CURRENCY, i < 3 ? currency : null)
            .set(
                PROPERTIES.ANNUAL_MANAGEMENT_FEE,
                i < 2 ? Long.valueOf((1800 + i * 600) * 100L) : null)
            .set(PROPERTIES.ANNUAL_MANAGEMENT_FEE_CURRENCY, i < 2 ? currency : null)
            .set(PROPERTIES.ANNUAL_MAINTENANCE_RESERVE, i < 4 ? Long.valueOf(500_00L) : null)
            .set(PROPERTIES.ANNUAL_MAINTENANCE_RESERVE_CURRENCY, i < 4 ? currency : null)
            .set(
                PROPERTIES.ANNUAL_PROPERTY_TAX_DUE_MONTH,
                i < 4 ? String.valueOf(new int[] {1, 4, 7, 10}[i % 4]) : null)
            .set(PROPERTIES.ANNUAL_INSURANCE_DUE_MONTH, i < 4 ? "1" : null)
            .set(PROPERTIES.ANNUAL_HOA_FEE_DUE_MONTH, i < 3 ? "6" : null)
            .set(PROPERTIES.ANNUAL_MANAGEMENT_FEE_DUE_MONTH, i < 2 ? "3" : null)
            .set(PROPERTIES.ANNUAL_MAINTENANCE_RESERVE_DUE_MONTH, i < 4 ? "9" : null)
            .set(PROPERTIES.DEPRECIATION_METHOD, i < 3 ? "STRAIGHT_LINE" : null)
            .set(PROPERTIES.DEPRECIATION_YEARS, i < 3 ? 30 : null)
            .set(PROPERTIES.LAND_VALUE, i < 3 ? Long.valueOf((80_000 + i * 20_000) * 100L) : null)
            .set(PROPERTIES.LAND_VALUE_CURRENCY, i < 3 ? currency : null)
            .set(PROPERTIES.CREATED_AT, now.minusDays(random.nextInt(30, 365)))
            .set(PROPERTIES.UPDATED_AT, now)
            .set(PROPERTIES.CREATED_BY, createdBy)
            .set(PROPERTIES.UPDATED_BY, createdBy)
            .execute();

        // Insert category-specific details
        insertCategoryDetails(
            propertyCategory,
            propertyType,
            propertyId,
            teamId,
            area,
            bedrooms,
            bathrooms,
            createdBy,
            now);

        propertyIds.add(propertyId);
        ctx.putIdentifier(propertyId, propertyIdentifier);
        ctx.putPropertyCategory(propertyId, propertyCategory);
        ctx.incrementProperties();

        // Add outdoor areas for RESIDENTIAL and MIXED_USE properties only
        if (("RESIDENTIAL".equals(propertyCategory) || "MIXED_USE".equals(propertyCategory))
            && random.nextBoolean()) {
          int outdoorCount = random.nextInt(1, 3);
          Set<String> usedTypes = new HashSet<>();
          for (int j = 0; j < outdoorCount; j++) {
            String outdoorType = pick(OUTDOOR_TYPES);
            if (usedTypes.add(outdoorType)) {
              dsl.insertInto(PROPERTY_OUTDOOR_AREAS)
                  .set(PROPERTY_OUTDOOR_AREAS.ID, UUID.randomUUID())
                  .set(PROPERTY_OUTDOOR_AREAS.IDENTIFIER, newPropertyOutdoorAreaId().value())
                  .set(PROPERTY_OUTDOOR_AREAS.PROPERTY_ID, propertyId)
                  .set(PROPERTY_OUTDOOR_AREAS.TEAM_ID, teamId)
                  .set(PROPERTY_OUTDOOR_AREAS.TYPE, outdoorType)
                  .set(PROPERTY_OUTDOOR_AREAS.AREA_VALUE, BigDecimal.valueOf(random.nextInt(5, 50)))
                  .set(PROPERTY_OUTDOOR_AREAS.AREA_UNIT, "sqm")
                  .set(PROPERTY_OUTDOOR_AREAS.CREATED_AT, now)
                  .set(PROPERTY_OUTDOOR_AREAS.UPDATED_AT, now)
                  .set(PROPERTY_OUTDOOR_AREAS.CREATED_BY, createdBy)
                  .set(PROPERTY_OUTDOOR_AREAS.UPDATED_BY, createdBy)
                  .execute();
            }
          }
        }
      }

      ctx.getPropertyIdsByTeam().put(teamId, propertyIds);
      log.info("Created {} properties for team {}", PROPERTIES_PER_TEAM, teamKey);
    }
  }

  // --- Category mix generation ---

  private List<String> generateCategoryMix(int count) {
    List<String> mix = new ArrayList<>();
    mix.add("RESIDENTIAL");
    mix.add("RESIDENTIAL");
    mix.add("COMMERCIAL");
    mix.add("INDUSTRIAL");
    String[] extras = {"AGRICULTURAL", "MIXED_USE", "COMMERCIAL", "RESIDENTIAL"};
    while (mix.size() < count) {
      mix.add(extras[random.nextInt(extras.length)]);
    }
    Collections.shuffle(mix, random);
    return mix;
  }

  private String randomTypeForCategory(String category) {
    return switch (category) {
      case "RESIDENTIAL" ->
          pick(new String[] {"APARTMENT", "HOUSE", "STUDIO", "VILLA", "TOWNHOUSE"});
      case "COMMERCIAL" -> pick(new String[] {"OFFICE", "RETAIL", "RESTAURANT", "CAFE"});
      case "INDUSTRIAL" -> pick(new String[] {"WAREHOUSE", "WORKSHOP", "FACTORY", "GARAGE"});
      case "AGRICULTURAL" -> pick(new String[] {"FARMLAND", "GREENHOUSE", "ORCHARD", "VINEYARD"});
      case "MIXED_USE" -> "MIXED_USE";
      default -> "APARTMENT";
    };
  }

  // --- Category-specific addresses ---

  private String[] streetsForCategory(CountryData country, String category) {
    return switch (category) {
      case "COMMERCIAL", "MIXED_USE" -> country.commercialStreets();
      case "INDUSTRIAL" -> country.industrialStreets();
      case "AGRICULTURAL" -> country.agriculturalStreets();
      default -> country.residentialStreets();
    };
  }

  // --- Category-specific area ---

  private BigDecimal areaForCategory(String category) {
    return BigDecimal.valueOf(
        switch (category) {
          case "RESIDENTIAL" -> random.nextInt(25, 250);
          case "COMMERCIAL" -> random.nextInt(50, 500);
          case "INDUSTRIAL" -> random.nextInt(200, 5000);
          case "AGRICULTURAL" -> random.nextInt(5000, 50000);
          case "MIXED_USE" -> random.nextInt(100, 800);
          default -> 75;
        });
  }

  // --- Category-specific construction ---

  private String constructionTypeForCategory(String category) {
    return switch (category) {
      case "INDUSTRIAL" -> pick(new String[] {"CONCRETE", "MIXED"});
      case "AGRICULTURAL" -> pick(new String[] {"WOOD", "MIXED"});
      default -> pick(CONSTRUCTION_TYPES);
    };
  }

  private String foundationTypeForCategory(String category) {
    return switch (category) {
      case "INDUSTRIAL" -> "CONCRETE_SLAB";
      case "AGRICULTURAL" -> pick(new String[] {"CONCRETE_SLAB", "PILE"});
      default -> pick(FOUNDATION_TYPES);
    };
  }

  private String roofTypeForCategory(String category) {
    return switch (category) {
      case "INDUSTRIAL" -> "FLAT";
      case "AGRICULTURAL" -> pick(new String[] {"PITCHED", "GABLE"});
      default -> pick(ROOF_TYPES);
    };
  }

  private String flooringTypeForCategory(String category) {
    return switch (category) {
      case "INDUSTRIAL" -> "CONCRETE";
      case "COMMERCIAL" -> pick(new String[] {"TILE", "LAMINATE", "VINYL"});
      default -> pick(FLOORING_TYPES);
    };
  }

  private String heatingTypeForCategory(String category) {
    return switch (category) {
      case "INDUSTRIAL", "AGRICULTURAL" -> "NONE";
      default -> pick(HEATING_TYPES);
    };
  }

  private String coolingTypeForCategory(String category) {
    return switch (category) {
      case "COMMERCIAL" -> "CENTRAL_AC";
      case "INDUSTRIAL", "AGRICULTURAL" -> "NONE";
      default -> random.nextBoolean() ? "CENTRAL_AC" : "NONE";
    };
  }

  private int floorsForCategory(String category) {
    return switch (category) {
      case "INDUSTRIAL" -> 1;
      case "AGRICULTURAL" -> 1;
      case "COMMERCIAL" -> random.nextInt(1, 6);
      default -> random.nextInt(1, 4);
    };
  }

  // --- Category-specific financial ---

  private Long purchasePriceForCategory(String category, int i) {
    long base =
        switch (category) {
          case "COMMERCIAL" -> 400_000 + i * 100_000L;
          case "INDUSTRIAL" -> 500_000 + i * 150_000L;
          case "AGRICULTURAL" -> 150_000 + i * 50_000L;
          case "MIXED_USE" -> 350_000 + i * 80_000L;
          default -> 200_000 + i * 75_000L;
        };
    return base * 100L;
  }

  private Long marketValueForCategory(String category, int i) {
    long base =
        switch (category) {
          case "COMMERCIAL" -> 440_000 + i * 110_000L;
          case "INDUSTRIAL" -> 550_000 + i * 160_000L;
          case "AGRICULTURAL" -> 170_000 + i * 55_000L;
          case "MIXED_USE" -> 385_000 + i * 90_000L;
          default -> 220_000 + i * 80_000L;
        };
    return base * 100L;
  }

  // --- Category-specific details insertion ---

  private void insertCategoryDetails(
      String category,
      String propertyType,
      UUID propertyId,
      UUID teamId,
      BigDecimal area,
      int bedrooms,
      int bathrooms,
      @Nullable UUID createdBy,
      LocalDateTime now) {
    switch (category) {
      case "RESIDENTIAL" -> {
        if (bedrooms > 0) {
          dsl.insertInto(PROPERTY_RESIDENTIAL_DETAILS)
              .set(PROPERTY_RESIDENTIAL_DETAILS.ID, UUID.randomUUID())
              .set(PROPERTY_RESIDENTIAL_DETAILS.PROPERTY_ID, propertyId)
              .set(PROPERTY_RESIDENTIAL_DETAILS.TEAM_ID, teamId)
              .set(PROPERTY_RESIDENTIAL_DETAILS.BEDROOMS, bedrooms)
              .set(PROPERTY_RESIDENTIAL_DETAILS.BATHROOMS, bathrooms)
              .set(PROPERTY_RESIDENTIAL_DETAILS.FURNISHED, random.nextBoolean())
              .set(
                  PROPERTY_RESIDENTIAL_DETAILS.PET_POLICY,
                  random.nextBoolean() ? "ALLOWED" : "NOT_ALLOWED")
              .set(PROPERTY_RESIDENTIAL_DETAILS.CREATED_AT, now)
              .set(PROPERTY_RESIDENTIAL_DETAILS.UPDATED_AT, now)
              .set(PROPERTY_RESIDENTIAL_DETAILS.CREATED_BY, createdBy)
              .set(PROPERTY_RESIDENTIAL_DETAILS.UPDATED_BY, createdBy)
              .execute();
        }
      }
      case "COMMERCIAL" -> {
        BigDecimal usable = area.multiply(BigDecimal.valueOf(0.85));
        BigDecimal common = area.subtract(usable);
        dsl.insertInto(PROPERTY_COMMERCIAL_DETAILS)
            .set(PROPERTY_COMMERCIAL_DETAILS.ID, UUID.randomUUID())
            .set(PROPERTY_COMMERCIAL_DETAILS.PROPERTY_ID, propertyId)
            .set(PROPERTY_COMMERCIAL_DETAILS.TEAM_ID, teamId)
            .set(PROPERTY_COMMERCIAL_DETAILS.USABLE_AREA_VALUE, usable)
            .set(PROPERTY_COMMERCIAL_DETAILS.USABLE_AREA_UNIT, "sqm")
            .set(PROPERTY_COMMERCIAL_DETAILS.COMMON_AREA_VALUE, common)
            .set(PROPERTY_COMMERCIAL_DETAILS.COMMON_AREA_UNIT, "sqm")
            .set(PROPERTY_COMMERCIAL_DETAILS.FLOOR_LEVEL, random.nextInt(0, 5))
            .set(
                PROPERTY_COMMERCIAL_DETAILS.CEILING_HEIGHT_M,
                BigDecimal.valueOf(2.7 + random.nextDouble() * 1.3))
            .set(
                PROPERTY_COMMERCIAL_DETAILS.HAS_STOREFRONT,
                "RETAIL".equals(propertyType)
                    || "RESTAURANT".equals(propertyType)
                    || "CAFE".equals(propertyType))
            .set(PROPERTY_COMMERCIAL_DETAILS.HAS_SIGNAGE_RIGHTS, random.nextBoolean())
            .set(PROPERTY_COMMERCIAL_DETAILS.ZONING_CLASSIFICATION, "COMMERCIAL")
            .set(PROPERTY_COMMERCIAL_DETAILS.MAX_OCCUPANCY, random.nextInt(10, 100))
            .set(PROPERTY_COMMERCIAL_DETAILS.RESTROOM_COUNT, random.nextInt(1, 4))
            .set(
                PROPERTY_COMMERCIAL_DETAILS.HAS_KITCHEN_FACILITY,
                "RESTAURANT".equals(propertyType) || "CAFE".equals(propertyType))
            .set(PROPERTY_COMMERCIAL_DETAILS.ACCESSIBILITY_COMPLIANT, true)
            .set(PROPERTY_COMMERCIAL_DETAILS.CREATED_AT, now)
            .set(PROPERTY_COMMERCIAL_DETAILS.UPDATED_AT, now)
            .set(PROPERTY_COMMERCIAL_DETAILS.CREATED_BY, createdBy)
            .set(PROPERTY_COMMERCIAL_DETAILS.UPDATED_BY, createdBy)
            .execute();
      }
      case "INDUSTRIAL" -> {
        dsl.insertInto(PROPERTY_INDUSTRIAL_DETAILS)
            .set(PROPERTY_INDUSTRIAL_DETAILS.ID, UUID.randomUUID())
            .set(PROPERTY_INDUSTRIAL_DETAILS.PROPERTY_ID, propertyId)
            .set(PROPERTY_INDUSTRIAL_DETAILS.TEAM_ID, teamId)
            .set(
                PROPERTY_INDUSTRIAL_DETAILS.CLEAR_HEIGHT_M,
                BigDecimal.valueOf(4.0 + random.nextDouble() * 8.0))
            .set(PROPERTY_INDUSTRIAL_DETAILS.LOADING_DOCKS, random.nextInt(1, 6))
            .set(PROPERTY_INDUSTRIAL_DETAILS.DRIVE_IN_DOORS, random.nextInt(1, 4))
            .set(
                PROPERTY_INDUSTRIAL_DETAILS.FLOOR_LOAD_CAPACITY_KG_SQM,
                BigDecimal.valueOf(1000 + random.nextInt(4000)))
            .set(PROPERTY_INDUSTRIAL_DETAILS.POWER_CAPACITY_KVA, random.nextInt(50, 500))
            .set(PROPERTY_INDUSTRIAL_DETAILS.HAS_THREE_PHASE_POWER, true)
            .set(PROPERTY_INDUSTRIAL_DETAILS.HAS_CRANE, random.nextInt(3) == 0)
            .set(
                PROPERTY_INDUSTRIAL_DETAILS.CRANE_CAPACITY_TONS,
                random.nextInt(3) == 0 ? BigDecimal.valueOf(5 + random.nextInt(20)) : null)
            .set(PROPERTY_INDUSTRIAL_DETAILS.HAS_HAZMAT_CERTIFICATION, random.nextInt(4) == 0)
            .set(PROPERTY_INDUSTRIAL_DETAILS.HAS_VENTILATION_SYSTEM, true)
            .set(PROPERTY_INDUSTRIAL_DETAILS.HAS_CLIMATE_CONTROL, random.nextBoolean())
            .set(
                PROPERTY_INDUSTRIAL_DETAILS.YARD_AREA_VALUE,
                BigDecimal.valueOf(random.nextInt(500, 5000)))
            .set(PROPERTY_INDUSTRIAL_DETAILS.YARD_AREA_UNIT, "sqm")
            .set(PROPERTY_INDUSTRIAL_DETAILS.ZONING_CLASSIFICATION, "INDUSTRIAL")
            .set(PROPERTY_INDUSTRIAL_DETAILS.CREATED_AT, now)
            .set(PROPERTY_INDUSTRIAL_DETAILS.UPDATED_AT, now)
            .set(PROPERTY_INDUSTRIAL_DETAILS.CREATED_BY, createdBy)
            .set(PROPERTY_INDUSTRIAL_DETAILS.UPDATED_BY, createdBy)
            .execute();
      }
      case "AGRICULTURAL" -> {
        BigDecimal totalLand = area;
        BigDecimal arableLand =
            totalLand.multiply(BigDecimal.valueOf(0.6 + random.nextDouble() * 0.3));
        String[] soilTypes = {"CLAY", "LOAM", "SANDY", "PEAT", "CHALK"};
        String[] waterSources = {"WELL", "CANAL", "RIVER", "MUNICIPAL"};
        String[] irrigationTypes = {"DRIP", "SPRINKLER", "FLOOD", "NONE"};
        String[] fencingTypes = {"WIRE", "WOODEN", "HEDGE", "NONE"};
        String[] currentUses = {
          "ARABLE_FARMING", "LIVESTOCK", "HORTICULTURE", "VITICULTURE", "MIXED"
        };

        dsl.insertInto(PROPERTY_AGRICULTURAL_DETAILS)
            .set(PROPERTY_AGRICULTURAL_DETAILS.ID, UUID.randomUUID())
            .set(PROPERTY_AGRICULTURAL_DETAILS.PROPERTY_ID, propertyId)
            .set(PROPERTY_AGRICULTURAL_DETAILS.TEAM_ID, teamId)
            .set(PROPERTY_AGRICULTURAL_DETAILS.TOTAL_LAND_AREA_VALUE, totalLand)
            .set(PROPERTY_AGRICULTURAL_DETAILS.TOTAL_LAND_AREA_UNIT, "sqm")
            .set(PROPERTY_AGRICULTURAL_DETAILS.ARABLE_AREA_VALUE, arableLand)
            .set(PROPERTY_AGRICULTURAL_DETAILS.ARABLE_AREA_UNIT, "sqm")
            .set(PROPERTY_AGRICULTURAL_DETAILS.SOIL_TYPE, pick(soilTypes))
            .set(PROPERTY_AGRICULTURAL_DETAILS.HAS_WATER_RIGHTS, random.nextBoolean())
            .set(PROPERTY_AGRICULTURAL_DETAILS.WATER_SOURCE, pick(waterSources))
            .set(PROPERTY_AGRICULTURAL_DETAILS.IRRIGATION_TYPE, pick(irrigationTypes))
            .set(PROPERTY_AGRICULTURAL_DETAILS.FENCING_TYPE, pick(fencingTypes))
            .set(PROPERTY_AGRICULTURAL_DETAILS.HAS_OUTBUILDINGS, random.nextBoolean())
            .set(
                PROPERTY_AGRICULTURAL_DETAILS.OUTBUILDING_DETAILS,
                random.nextBoolean() ? "Barn, tool shed" : null)
            .set(PROPERTY_AGRICULTURAL_DETAILS.CURRENT_USE, pick(currentUses))
            .set(PROPERTY_AGRICULTURAL_DETAILS.ZONING_CLASSIFICATION, "AGRICULTURAL")
            .set(PROPERTY_AGRICULTURAL_DETAILS.CREATED_AT, now)
            .set(PROPERTY_AGRICULTURAL_DETAILS.UPDATED_AT, now)
            .set(PROPERTY_AGRICULTURAL_DETAILS.CREATED_BY, createdBy)
            .set(PROPERTY_AGRICULTURAL_DETAILS.UPDATED_BY, createdBy)
            .execute();
      }
      default -> {
        // MIXED_USE — no specific detail table
      }
    }
  }

  // --- Helpers ---

  private String generatePostalCode(String format) {
    StringBuilder sb = new StringBuilder();
    for (char c : format.toCharArray()) {
      if (c == '#') {
        sb.append(random.nextInt(0, 10));
      } else if (c == 'A') {
        sb.append((char) ('A' + random.nextInt(26)));
      } else if (c == '_') {
        sb.append(' ');
      } else {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  private String pick(String[] array) {
    return array[random.nextInt(array.length)];
  }
}
