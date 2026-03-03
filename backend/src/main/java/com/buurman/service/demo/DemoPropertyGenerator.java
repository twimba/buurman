package com.buurman.service.demo;

import static com.buurman.jooq.generated.Tables.PROPERTIES;
import static com.buurman.jooq.generated.Tables.PROPERTY_ACQUISITIONS;
import static com.buurman.jooq.generated.Tables.PROPERTY_AGRICULTURAL_DETAILS;
import static com.buurman.jooq.generated.Tables.PROPERTY_COMMERCIAL_DETAILS;
import static com.buurman.jooq.generated.Tables.PROPERTY_FEES;
import static com.buurman.jooq.generated.Tables.PROPERTY_FINANCINGS;
import static com.buurman.jooq.generated.Tables.PROPERTY_INDUSTRIAL_DETAILS;
import static com.buurman.jooq.generated.Tables.PROPERTY_INSURANCES;
import static com.buurman.jooq.generated.Tables.PROPERTY_OUTDOOR_AREAS;
import static com.buurman.jooq.generated.Tables.PROPERTY_RESIDENTIAL_DETAILS;
import static com.buurman.jooq.generated.Tables.PROPERTY_TAXES;
import static com.buurman.jooq.generated.Tables.PROPERTY_VALUATIONS;
import static com.buurman.util.UlidGenerator.newAcquisitionId;
import static com.buurman.util.UlidGenerator.newFinancingId;
import static com.buurman.util.UlidGenerator.newInsuranceId;
import static com.buurman.util.UlidGenerator.newPropertyFeeId;
import static com.buurman.util.UlidGenerator.newPropertyId;
import static com.buurman.util.UlidGenerator.newPropertyOutdoorAreaId;
import static com.buurman.util.UlidGenerator.newPropertyTaxId;
import static com.buurman.util.UlidGenerator.newValuationId;

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
              List.of(
                  "Keizersgracht",
                  "Prinsengracht",
                  "Herengracht",
                  "Vondelstraat",
                  "Beethovenstraat",
                  "Apollolaan",
                  "Singel",
                  "Overtoom"),
              List.of("Zuidas", "Amstelplein", "WTC Boulevard", "Strawinskylaan"),
              List.of("Westpoort", "Schiphol-Rijk", "Havenweg", "Industrieweg"),
              List.of("Beemsterweg", "Polderweg", "Boerderijlaan", "Weideland"),
              List.of(
                  "Amsterdam",
                  "Rotterdam",
                  "Den Haag",
                  "Utrecht",
                  "Eindhoven",
                  "Leiden",
                  "Haarlem",
                  "Delft"),
              "####_AA",
              51.8,
              53.0,
              4.0,
              6.0),
          new CountryData(
              "Germany",
              List.of(
                  "Friedrichstraße",
                  "Kurfürstendamm",
                  "Schillerstraße",
                  "Goethestraße",
                  "Berliner Straße",
                  "Hauptstraße",
                  "Bahnhofstraße",
                  "Mozartstraße"),
              List.of("Potsdamer Platz", "Bankenviertel", "Geschäftsstraße", "Büropark"),
              List.of("Industriegebiet", "Gewerbepark", "Logistikring", "Werkstraße"),
              List.of("Ackerweg", "Hofstraße", "Feldmark", "Gutshof"),
              List.of(
                  "Berlin",
                  "Munich",
                  "Hamburg",
                  "Frankfurt",
                  "Cologne",
                  "Stuttgart",
                  "Düsseldorf",
                  "Dresden"),
              "#####",
              48.0,
              54.0,
              6.0,
              14.0),
          new CountryData(
              "United Kingdom",
              List.of(
                  "Baker Street",
                  "King's Road",
                  "Church Lane",
                  "High Street",
                  "Park Avenue",
                  "Victoria Road",
                  "Station Road",
                  "Mill Lane"),
              List.of("Canary Wharf", "Fenchurch Street", "Bishopsgate", "Fleet Street"),
              List.of("Trading Estate", "Industrial Park", "Enterprise Way", "Dock Road"),
              List.of("Manor Farm Road", "The Green", "Orchard Lane", "Meadow Drive"),
              List.of(
                  "London",
                  "Manchester",
                  "Birmingham",
                  "Edinburgh",
                  "Bristol",
                  "Liverpool",
                  "Oxford",
                  "Cambridge"),
              "AA## #AA",
              51.0,
              56.0,
              -4.0,
              1.5),
          new CountryData(
              "France",
              List.of(
                  "Rue de Rivoli",
                  "Avenue des Champs-Élysées",
                  "Boulevard Saint-Germain",
                  "Rue de la Paix",
                  "Avenue Montaigne",
                  "Rue du Faubourg",
                  "Place Vendôme",
                  "Rue de Seine"),
              List.of(
                  "Quartier des Affaires", "La Défense", "Rue du Commerce", "Avenue de l'Opéra"),
              List.of("Zone Industrielle", "Parc d'Activités", "Rue de l'Usine", "Route du Port"),
              List.of(
                  "Chemin du Vignoble",
                  "Route des Champs",
                  "Lieu-dit La Ferme",
                  "Allée des Vergers"),
              List.of(
                  "Paris",
                  "Lyon",
                  "Marseille",
                  "Bordeaux",
                  "Nice",
                  "Toulouse",
                  "Strasbourg",
                  "Nantes"),
              "#####",
              43.0,
              49.0,
              -1.0,
              7.0),
          new CountryData(
              "Spain",
              List.of(
                  "Calle Gran Vía",
                  "Paseo de la Castellana",
                  "Avenida Diagonal",
                  "Calle Mayor",
                  "Calle de Alcalá",
                  "Rambla de Catalunya",
                  "Calle Serrano",
                  "Paseo del Prado"),
              List.of(
                  "Paseo de la Castellana",
                  "Calle de Serrano",
                  "Avenida de la Constitución",
                  "Plaza de España"),
              List.of(
                  "Polígono Industrial",
                  "Zona Franca",
                  "Calle de la Industria",
                  "Avenida del Puerto"),
              List.of(
                  "Camino de la Huerta",
                  "Finca El Olivar",
                  "Carretera de los Viñedos",
                  "Calle del Campo"),
              List.of(
                  "Madrid",
                  "Barcelona",
                  "Valencia",
                  "Seville",
                  "Málaga",
                  "Bilbao",
                  "Granada",
                  "San Sebastián"),
              "#####",
              36.0,
              43.5,
              -6.0,
              3.0),
          new CountryData(
              "Portugal",
              List.of(
                  "Rua Augusta",
                  "Avenida da Liberdade",
                  "Rua de Santa Catarina",
                  "Rua do Carmo",
                  "Praça do Comércio",
                  "Rua dos Clérigos",
                  "Avenida dos Aliados",
                  "Rua da Prata"),
              List.of(
                  "Avenida da República",
                  "Rua do Comércio",
                  "Praça do Município",
                  "Parque das Nações"),
              List.of(
                  "Zona Industrial", "Parque Empresarial", "Rua da Fábrica", "Estrada do Porto"),
              List.of(
                  "Estrada das Quintas", "Caminho do Vinhedo", "Rua da Herdade", "Largo do Olival"),
              List.of("Lisbon", "Porto", "Faro", "Coimbra", "Braga", "Funchal", "Aveiro", "Évora"),
              "####-###",
              37.0,
              42.0,
              -9.5,
              -6.0));

  record CountryData(
      String name,
      List<String> residentialStreets,
      List<String> commercialStreets,
      List<String> industrialStreets,
      List<String> agriculturalStreets,
      List<String> cities,
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
      UUID createdBy = ctx.getAdminUserForTeam(teamKey).orElse(null);
      String currency = ctx.getCurrencyForTeam(teamKey);
      List<UUID> propertyIds = new ArrayList<>();

      List<String> categories = generateCategoryMix(PROPERTIES_PER_TEAM);

      for (int i = 0; i < PROPERTIES_PER_TEAM; i++) {
        UUID propertyId = UUID.randomUUID();
        String propertyCategory = categories.get(i);
        String propertyType = randomTypeForCategory(propertyCategory);
        String status = i < PROPERTIES_PER_TEAM - 1 ? "OCCUPIED" : "VACANT";

        CountryData country = COUNTRIES.get(i % COUNTRIES.size());
        List<String> streets = streetsForCategory(country, propertyCategory);

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
                PROPERTIES.ELECTRICITY_CAPACITY_VALUE,
                "INDUSTRIAL".equals(propertyCategory) ? 63 : random.nextBoolean() ? 25 : 35)
            .set(PROPERTIES.ELECTRICITY_CAPACITY_UNIT, "a")
            .set(PROPERTIES.WATER_CONNECTION_TYPE, "MUNICIPAL")
            .set(
                PROPERTIES.HAS_GAS_CONNECTION,
                !"AGRICULTURAL".equals(propertyCategory) && random.nextBoolean())
            .set(
                PROPERTIES.SEWAGE_TYPE,
                "AGRICULTURAL".equals(propertyCategory) ? "SEPTIC" : "MUNICIPAL")
            .set(PROPERTIES.INTERNET_CONNECTION_TYPE, pick(INTERNET_TYPES))
            .set(PROPERTIES.INTERNET_MAX_SPEED_VALUE, random.nextBoolean() ? 500 : 1000)
            .set(PROPERTIES.INTERNET_MAX_SPEED_UNIT, "mbps")
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

        // Insert financial data into new dedicated tables (first 4 properties)
        insertFinancialData(propertyId, teamId, createdBy, currency, propertyCategory, i, now);

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

  private List<String> streetsForCategory(CountryData country, String category) {
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
                PROPERTY_COMMERCIAL_DETAILS.CEILING_HEIGHT_VALUE,
                BigDecimal.valueOf(2.7 + random.nextDouble() * 1.3))
            .set(PROPERTY_COMMERCIAL_DETAILS.CEILING_HEIGHT_UNIT, "m")
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
      case "INDUSTRIAL" ->
          dsl.insertInto(PROPERTY_INDUSTRIAL_DETAILS)
              .set(PROPERTY_INDUSTRIAL_DETAILS.ID, UUID.randomUUID())
              .set(PROPERTY_INDUSTRIAL_DETAILS.PROPERTY_ID, propertyId)
              .set(PROPERTY_INDUSTRIAL_DETAILS.TEAM_ID, teamId)
              .set(
                  PROPERTY_INDUSTRIAL_DETAILS.CLEAR_HEIGHT_VALUE,
                  BigDecimal.valueOf(4.0 + random.nextDouble() * 8.0))
              .set(PROPERTY_INDUSTRIAL_DETAILS.CLEAR_HEIGHT_UNIT, "m")
              .set(PROPERTY_INDUSTRIAL_DETAILS.LOADING_DOCKS, random.nextInt(1, 6))
              .set(PROPERTY_INDUSTRIAL_DETAILS.DRIVE_IN_DOORS, random.nextInt(1, 4))
              .set(
                  PROPERTY_INDUSTRIAL_DETAILS.FLOOR_LOAD_CAPACITY_VALUE,
                  BigDecimal.valueOf(1000 + random.nextInt(4000)))
              .set(PROPERTY_INDUSTRIAL_DETAILS.FLOOR_LOAD_CAPACITY_UNIT, "kg_sqm")
              .set(PROPERTY_INDUSTRIAL_DETAILS.POWER_CAPACITY_VALUE, random.nextInt(50, 500))
              .set(PROPERTY_INDUSTRIAL_DETAILS.POWER_CAPACITY_UNIT, "kva")
              .set(PROPERTY_INDUSTRIAL_DETAILS.HAS_THREE_PHASE_POWER, true)
              .set(PROPERTY_INDUSTRIAL_DETAILS.HAS_CRANE, random.nextInt(3) == 0)
              .set(
                  PROPERTY_INDUSTRIAL_DETAILS.CRANE_CAPACITY_VALUE,
                  random.nextInt(3) == 0 ? BigDecimal.valueOf(5 + random.nextInt(20)) : null)
              .set(
                  PROPERTY_INDUSTRIAL_DETAILS.CRANE_CAPACITY_UNIT,
                  random.nextInt(3) == 0 ? "metric_tons" : null)
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

  // --- Financial data insertion ---

  private void insertFinancialData(
      UUID propertyId,
      UUID teamId,
      @Nullable UUID createdBy,
      String currency,
      String propertyCategory,
      int i,
      LocalDateTime now) {

    // Acquisition (first 4 properties)
    if (i < 4) {
      long purchasePrice = purchasePriceForCategory(propertyCategory, i);
      dsl.insertInto(PROPERTY_ACQUISITIONS)
          .set(PROPERTY_ACQUISITIONS.ID, UUID.randomUUID())
          .set(PROPERTY_ACQUISITIONS.IDENTIFIER, newAcquisitionId().value())
          .set(PROPERTY_ACQUISITIONS.PROPERTY_ID, propertyId)
          .set(PROPERTY_ACQUISITIONS.TEAM_ID, teamId)
          .set(PROPERTY_ACQUISITIONS.ACQUISITION_TYPE, "PURCHASE")
          .set(PROPERTY_ACQUISITIONS.ACQUISITION_DATE, LocalDate.of(2020 + i, 3, 15))
          .set(PROPERTY_ACQUISITIONS.PURCHASE_PRICE, purchasePrice)
          .set(PROPERTY_ACQUISITIONS.PURCHASE_PRICE_CURRENCY, currency)
          .set(PROPERTY_ACQUISITIONS.CLOSING_COSTS, (long) (purchasePrice * 0.03))
          .set(PROPERTY_ACQUISITIONS.CLOSING_COSTS_CURRENCY, currency)
          .set(PROPERTY_ACQUISITIONS.LAND_VALUE, i < 3 ? (80_000L + i * 20_000L) * 100L : null)
          .set(PROPERTY_ACQUISITIONS.LAND_VALUE_CURRENCY, i < 3 ? currency : null)
          .set(PROPERTY_ACQUISITIONS.DEPRECIATION_METHOD, i < 3 ? "STRAIGHT_LINE" : null)
          .set(PROPERTY_ACQUISITIONS.DEPRECIATION_YEARS, i < 3 ? 30 : null)
          .set(PROPERTY_ACQUISITIONS.CREATED_AT, now)
          .set(PROPERTY_ACQUISITIONS.UPDATED_AT, now)
          .set(PROPERTY_ACQUISITIONS.CREATED_BY, createdBy)
          .set(PROPERTY_ACQUISITIONS.UPDATED_BY, createdBy)
          .execute();
    }

    // Valuation (first 4 properties)
    if (i < 4) {
      dsl.insertInto(PROPERTY_VALUATIONS)
          .set(PROPERTY_VALUATIONS.ID, UUID.randomUUID())
          .set(PROPERTY_VALUATIONS.IDENTIFIER, newValuationId().value())
          .set(PROPERTY_VALUATIONS.PROPERTY_ID, propertyId)
          .set(PROPERTY_VALUATIONS.TEAM_ID, teamId)
          .set(PROPERTY_VALUATIONS.VALUATION_TYPE, "MARKET")
          .set(PROPERTY_VALUATIONS.VALUATION_DATE, LocalDate.of(2025, 12, 1))
          .set(PROPERTY_VALUATIONS.AMOUNT, marketValueForCategory(propertyCategory, i))
          .set(PROPERTY_VALUATIONS.CURRENCY, currency)
          .set(PROPERTY_VALUATIONS.SOURCE, "Appraiser")
          .set(PROPERTY_VALUATIONS.CREATED_AT, now)
          .set(PROPERTY_VALUATIONS.UPDATED_AT, now)
          .set(PROPERTY_VALUATIONS.CREATED_BY, createdBy)
          .set(PROPERTY_VALUATIONS.UPDATED_BY, createdBy)
          .execute();
    }

    // Financing / Mortgage (properties 1-3, not property 0 which has NONE)
    if (i > 0 && i < 4) {
      dsl.insertInto(PROPERTY_FINANCINGS)
          .set(PROPERTY_FINANCINGS.ID, UUID.randomUUID())
          .set(PROPERTY_FINANCINGS.IDENTIFIER, newFinancingId().value())
          .set(PROPERTY_FINANCINGS.PROPERTY_ID, propertyId)
          .set(PROPERTY_FINANCINGS.TEAM_ID, teamId)
          .set(PROPERTY_FINANCINGS.FINANCING_TYPE, "MORTGAGE")
          .set(PROPERTY_FINANCINGS.RATE_TYPE, "FIXED")
          .set(PROPERTY_FINANCINGS.LENDER_NAME, "Demo Bank " + i)
          .set(PROPERTY_FINANCINGS.ORIGINAL_AMOUNT, (150_000L + i * 50_000L) * 100L)
          .set(PROPERTY_FINANCINGS.ORIGINAL_AMOUNT_CURRENCY, currency)
          .set(PROPERTY_FINANCINGS.INTEREST_RATE, new BigDecimal("3." + (i * 2) + "50"))
          .set(PROPERTY_FINANCINGS.MONTHLY_PAYMENT, (600L + i * 150L) * 100L)
          .set(PROPERTY_FINANCINGS.MONTHLY_PAYMENT_CURRENCY, currency)
          .set(PROPERTY_FINANCINGS.PAYMENT_VARIABLE, false)
          .set(PROPERTY_FINANCINGS.START_DATE, LocalDate.of(2020 + i, 4, 1))
          .set(PROPERTY_FINANCINGS.END_DATE, LocalDate.of(2050 + i, 3, 31))
          .set(PROPERTY_FINANCINGS.TERM_MONTHS, 360)
          .set(PROPERTY_FINANCINGS.STATUS, "ACTIVE")
          .set(PROPERTY_FINANCINGS.CREATED_AT, now)
          .set(PROPERTY_FINANCINGS.UPDATED_AT, now)
          .set(PROPERTY_FINANCINGS.CREATED_BY, createdBy)
          .set(PROPERTY_FINANCINGS.UPDATED_BY, createdBy)
          .execute();
    }

    // Insurance (first 4 properties)
    if (i < 4) {
      dsl.insertInto(PROPERTY_INSURANCES)
          .set(PROPERTY_INSURANCES.ID, UUID.randomUUID())
          .set(PROPERTY_INSURANCES.IDENTIFIER, newInsuranceId().value())
          .set(PROPERTY_INSURANCES.PROPERTY_ID, propertyId)
          .set(PROPERTY_INSURANCES.TEAM_ID, teamId)
          .set(PROPERTY_INSURANCES.INSURANCE_TYPE, "BUILDING")
          .set(PROPERTY_INSURANCES.PROVIDER, "Demo Insurance Co.")
          .set(PROPERTY_INSURANCES.ANNUAL_PREMIUM, (400L + i * 100L) * 100L)
          .set(PROPERTY_INSURANCES.ANNUAL_PREMIUM_CURRENCY, currency)
          .set(PROPERTY_INSURANCES.PAYMENT_FREQUENCY, "ANNUALLY")
          .set(PROPERTY_INSURANCES.START_DATE, LocalDate.of(2020 + i, 1, 1))
          .set(PROPERTY_INSURANCES.END_DATE, LocalDate.of(2026 + i, 12, 31))
          .set(PROPERTY_INSURANCES.STATUS, "ACTIVE")
          .set(PROPERTY_INSURANCES.CREATED_AT, now)
          .set(PROPERTY_INSURANCES.UPDATED_AT, now)
          .set(PROPERTY_INSURANCES.CREATED_BY, createdBy)
          .set(PROPERTY_INSURANCES.UPDATED_BY, createdBy)
          .execute();
    }

    // Property Tax (first 4 properties)
    if (i < 4) {
      String taxDueMonth = String.valueOf(new int[] {1, 4, 7, 10}[i % 4]);
      dsl.insertInto(PROPERTY_TAXES)
          .set(PROPERTY_TAXES.ID, UUID.randomUUID())
          .set(PROPERTY_TAXES.IDENTIFIER, newPropertyTaxId().value())
          .set(PROPERTY_TAXES.PROPERTY_ID, propertyId)
          .set(PROPERTY_TAXES.TEAM_ID, teamId)
          .set(PROPERTY_TAXES.TAX_TYPE, "PROPERTY")
          .set(PROPERTY_TAXES.ANNUAL_AMOUNT, (1200L + i * 300L) * 100L)
          .set(PROPERTY_TAXES.CURRENCY, currency)
          .set(PROPERTY_TAXES.PAYMENT_FREQUENCY, "ANNUALLY")
          .set(PROPERTY_TAXES.DUE_MONTHS, taxDueMonth)
          .set(PROPERTY_TAXES.STATUS, "ACTIVE")
          .set(PROPERTY_TAXES.CREATED_AT, now)
          .set(PROPERTY_TAXES.UPDATED_AT, now)
          .set(PROPERTY_TAXES.CREATED_BY, createdBy)
          .set(PROPERTY_TAXES.UPDATED_BY, createdBy)
          .execute();
    }

    // HOA Fee (first 3 properties)
    if (i < 3) {
      dsl.insertInto(PROPERTY_FEES)
          .set(PROPERTY_FEES.ID, UUID.randomUUID())
          .set(PROPERTY_FEES.IDENTIFIER, newPropertyFeeId().value())
          .set(PROPERTY_FEES.PROPERTY_ID, propertyId)
          .set(PROPERTY_FEES.TEAM_ID, teamId)
          .set(PROPERTY_FEES.FEE_TYPE, "HOA")
          .set(PROPERTY_FEES.ANNUAL_AMOUNT, (600L + i * 200L) * 100L)
          .set(PROPERTY_FEES.CURRENCY, currency)
          .set(PROPERTY_FEES.PAYMENT_FREQUENCY, "MONTHLY")
          .set(PROPERTY_FEES.STATUS, "ACTIVE")
          .set(PROPERTY_FEES.CREATED_AT, now)
          .set(PROPERTY_FEES.UPDATED_AT, now)
          .set(PROPERTY_FEES.CREATED_BY, createdBy)
          .set(PROPERTY_FEES.UPDATED_BY, createdBy)
          .execute();
    }

    // Management Fee (first 2 properties)
    if (i < 2) {
      dsl.insertInto(PROPERTY_FEES)
          .set(PROPERTY_FEES.ID, UUID.randomUUID())
          .set(PROPERTY_FEES.IDENTIFIER, newPropertyFeeId().value())
          .set(PROPERTY_FEES.PROPERTY_ID, propertyId)
          .set(PROPERTY_FEES.TEAM_ID, teamId)
          .set(PROPERTY_FEES.FEE_TYPE, "MANAGEMENT")
          .set(PROPERTY_FEES.NAME, "Property Management")
          .set(PROPERTY_FEES.ANNUAL_AMOUNT, (1800L + i * 600L) * 100L)
          .set(PROPERTY_FEES.CURRENCY, currency)
          .set(PROPERTY_FEES.PAYMENT_FREQUENCY, "QUARTERLY")
          .set(PROPERTY_FEES.DUE_MONTHS, "3,6,9,12")
          .set(PROPERTY_FEES.STATUS, "ACTIVE")
          .set(PROPERTY_FEES.CREATED_AT, now)
          .set(PROPERTY_FEES.UPDATED_AT, now)
          .set(PROPERTY_FEES.CREATED_BY, createdBy)
          .set(PROPERTY_FEES.UPDATED_BY, createdBy)
          .execute();
    }

    // Maintenance Reserve Fee (first 4 properties)
    if (i < 4) {
      dsl.insertInto(PROPERTY_FEES)
          .set(PROPERTY_FEES.ID, UUID.randomUUID())
          .set(PROPERTY_FEES.IDENTIFIER, newPropertyFeeId().value())
          .set(PROPERTY_FEES.PROPERTY_ID, propertyId)
          .set(PROPERTY_FEES.TEAM_ID, teamId)
          .set(PROPERTY_FEES.FEE_TYPE, "MAINTENANCE_RESERVE")
          .set(PROPERTY_FEES.NAME, "Maintenance Reserve")
          .set(PROPERTY_FEES.ANNUAL_AMOUNT, 500_00L)
          .set(PROPERTY_FEES.CURRENCY, currency)
          .set(PROPERTY_FEES.PAYMENT_FREQUENCY, "ANNUALLY")
          .set(PROPERTY_FEES.DUE_MONTHS, "9")
          .set(PROPERTY_FEES.STATUS, "ACTIVE")
          .set(PROPERTY_FEES.CREATED_AT, now)
          .set(PROPERTY_FEES.UPDATED_AT, now)
          .set(PROPERTY_FEES.CREATED_BY, createdBy)
          .set(PROPERTY_FEES.UPDATED_BY, createdBy)
          .execute();
    }
  }

  // --- Helpers ---

  private String generatePostalCode(String format) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < format.length(); i++) {
      char c = format.charAt(i);
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

  private String pick(List<String> list) {
    return list.get(random.nextInt(list.size()));
  }

  private String pick(String[] array) {
    return array[random.nextInt(array.length)];
  }
}
