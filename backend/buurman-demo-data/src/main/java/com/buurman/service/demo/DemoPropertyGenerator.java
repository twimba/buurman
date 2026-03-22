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
import static com.buurman.util.SidGenerator.newAcquisitionId;
import static com.buurman.util.SidGenerator.newFinancingId;
import static com.buurman.util.SidGenerator.newInsuranceId;
import static com.buurman.util.SidGenerator.newPropertyFeeId;
import static com.buurman.util.SidGenerator.newPropertyId;
import static com.buurman.util.SidGenerator.newPropertyOutdoorAreaId;
import static com.buurman.util.SidGenerator.newPropertyTaxId;
import static com.buurman.util.SidGenerator.newValuationId;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.buurman.domain.Sid;

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

  // Country-specific providers (indexed by property i, 12 countries)
  private static final String[] INSURANCE_PROVIDERS = {
    "Nationale-Nederlanden", "Allianz", "Aviva", "AXA France",
    "Mapfre", "Fidelidade", "AG Insurance", "Generali",
    "Wiener Städtische", "Helvetia", "State Farm", "Zurich Ireland"
  };
  private static final String[] TAX_AUTHORITIES = {
    "Gemeente Amsterdam",
    "Finanzamt Berlin",
    "HM Revenue & Customs",
    "Centre des Impôts",
    "Hacienda Pública",
    "Autoridade Tributária",
    "SPF Finances",
    "Agenzia delle Entrate",
    "Finanzamt Wien",
    "Kantonale Steuerverwaltung",
    "County Assessor's Office",
    "Revenue Commissioners"
  };
  private static final String[] APPRAISER_NAMES = {
    "Makelaardij Van der Berg", "Immobilienbewertung Schmidt",
    "RICS Chartered Surveyors", "Cabinet d'Expertise Dupont",
    "Sociedad de Tasación", "Avaliadores Certificados",
    "Expertise Immobilière", "Perizie Immobiliari",
    "Immobilienbewertung Wien", "Schweizer Immobiliengutachter",
    "National Appraisal Group", "Irish Property Valuers"
  };
  private static final String[] LENDER_NAMES = {
    "ABN AMRO", "Deutsche Bank", "Barclays", "BNP Paribas",
    "Banco Santander", "Millennium BCP", "KBC Bank", "UniCredit",
    "Erste Bank", "Credit Suisse", "JPMorgan Chase", "Bank of Ireland"
  };

  public static final int PROPERTIES_PER_TEAM = 30;

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
              6.0,
              Map.of()),
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
              14.0,
              Map.of(
                  "Berlin",
                  "BE",
                  "Munich",
                  "BY",
                  "Hamburg",
                  "HH",
                  "Frankfurt",
                  "HE",
                  "Cologne",
                  "NW",
                  "Stuttgart",
                  "BW",
                  "Düsseldorf",
                  "NW",
                  "Dresden",
                  "SN")),
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
              1.5,
              Map.of(
                  "London",
                  "ENG",
                  "Manchester",
                  "ENG",
                  "Birmingham",
                  "ENG",
                  "Edinburgh",
                  "SCT",
                  "Bristol",
                  "ENG",
                  "Liverpool",
                  "ENG",
                  "Oxford",
                  "ENG",
                  "Cambridge",
                  "ENG")),
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
              7.0,
              Map.of()),
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
              3.0,
              Map.of()),
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
              -6.0,
              Map.of()),
          new CountryData(
              "Belgium",
              List.of(
                  "Rue de la Loi",
                  "Avenue Louise",
                  "Meir",
                  "Veldstraat",
                  "Rue Neuve",
                  "Lange Gasthuisstraat",
                  "Boulevard Anspach",
                  "Koningstraat"),
              List.of("Avenue de Tervueren", "Louizalaan", "Frankrijklei", "Rue de la Régence"),
              List.of("Industriezone", "Haven van Antwerpen", "Ambachtsstraat", "Zeehavenlaan"),
              List.of("Polderweg", "Kasteeldreef", "Hoeveland", "Veldweg"),
              List.of(
                  "Brussels", "Antwerp", "Ghent", "Bruges", "Liège", "Namur", "Leuven", "Mechelen"),
              "####",
              49.5,
              51.5,
              2.5,
              6.4,
              Map.of(
                  "Brussels",
                  "BRU",
                  "Antwerp",
                  "VLG",
                  "Ghent",
                  "VLG",
                  "Bruges",
                  "VLG",
                  "Liège",
                  "WAL",
                  "Namur",
                  "WAL",
                  "Leuven",
                  "VLG",
                  "Mechelen",
                  "VLG")),
          new CountryData(
              "Italy",
              List.of(
                  "Via Roma",
                  "Via Garibaldi",
                  "Corso Vittorio Emanuele",
                  "Via Nazionale",
                  "Via del Corso",
                  "Via Manzoni",
                  "Via Torino",
                  "Via Dante"),
              List.of("Piazza degli Affari", "Via Montenapoleone", "Corso Europa", "Via Veneto"),
              List.of(
                  "Zona Industriale", "Via dell'Industria", "Strada Provinciale", "Porto Fluviale"),
              List.of(
                  "Via dei Campi", "Contrada Vigneto", "Strada del Frantoio", "Podere San Marco"),
              List.of("Rome", "Milan", "Florence", "Naples", "Venice", "Turin", "Bologna", "Genoa"),
              "#####",
              36.6,
              47.1,
              6.6,
              18.5,
              Map.of()),
          new CountryData(
              "Austria",
              List.of(
                  "Ringstraße",
                  "Mariahilfer Straße",
                  "Kärntner Straße",
                  "Graben",
                  "Getreidegasse",
                  "Landstraße",
                  "Herrengasse",
                  "Linzer Gasse"),
              List.of("Donau City", "Wienerbergstraße", "Lassallestraße", "Europaplatz"),
              List.of("Industriestraße", "Gewerbepark", "Hafenstraße", "Werkstraße"),
              List.of("Weinbergweg", "Almstraße", "Hofgasse", "Feldweg"),
              List.of(
                  "Vienna",
                  "Graz",
                  "Linz",
                  "Salzburg",
                  "Innsbruck",
                  "Klagenfurt",
                  "Villach",
                  "Wels"),
              "####",
              46.4,
              48.9,
              9.5,
              17.2,
              Map.of()),
          new CountryData(
              "Switzerland",
              List.of(
                  "Bahnhofstrasse",
                  "Rue du Rhône",
                  "Freie Strasse",
                  "Kramgasse",
                  "Limmatquai",
                  "Marktgasse",
                  "Spiegelgasse",
                  "Münstergasse"),
              List.of("Paradeplatz", "Rue du Marché", "Aeschenvorstadt", "Place de la Fusterie"),
              List.of("Industriestrasse", "Gewerbepark", "Hafenweg", "Route de l'Usine"),
              List.of("Rebbergweg", "Weidstrasse", "Alpweg", "Route du Vignoble"),
              List.of(
                  "Zurich",
                  "Geneva",
                  "Basel",
                  "Bern",
                  "Lausanne",
                  "Lucerne",
                  "St. Gallen",
                  "Winterthur"),
              "####",
              45.8,
              47.8,
              5.9,
              10.5,
              Map.of()),
          new CountryData(
              "United States",
              List.of(
                  "Broadway",
                  "Main Street",
                  "Park Avenue",
                  "Elm Street",
                  "Oak Drive",
                  "Maple Avenue",
                  "Washington Street",
                  "Lincoln Boulevard"),
              List.of("Wall Street", "Market Street", "Commerce Drive", "Corporate Boulevard"),
              List.of("Industrial Boulevard", "Warehouse Row", "Factory Lane", "Port Road"),
              List.of("Ranch Road", "Farm Lane", "County Road", "Orchard Drive"),
              List.of(
                  "New York",
                  "Los Angeles",
                  "San Francisco",
                  "Portland",
                  "Chicago",
                  "Houston",
                  "Miami",
                  "Seattle"),
              "#####",
              25.0,
              49.0,
              -125.0,
              -67.0,
              Map.of(
                  "New York",
                  "NY",
                  "Los Angeles",
                  "CA",
                  "San Francisco",
                  "CA",
                  "Portland",
                  "OR",
                  "Chicago",
                  "IL",
                  "Houston",
                  "TX",
                  "Miami",
                  "FL",
                  "Seattle",
                  "WA")),
          new CountryData(
              "Ireland",
              List.of(
                  "O'Connell Street",
                  "Grafton Street",
                  "Patrick Street",
                  "Shop Street",
                  "Quay Street",
                  "Henry Street",
                  "Dame Street",
                  "Barrack Street"),
              List.of("Grand Canal Dock", "IFSC", "Burlington Road", "Mespil Road"),
              List.of("Industrial Estate", "Business Park", "Dock Road", "Shannon Free Zone"),
              List.of("Boreen Lane", "Greenfield Road", "Meadow Drive", "Farm Road"),
              List.of(
                  "Dublin",
                  "Cork",
                  "Galway",
                  "Limerick",
                  "Waterford",
                  "Kilkenny",
                  "Wexford",
                  "Dundalk"),
              "A##_A#A#",
              51.4,
              55.4,
              -10.5,
              -5.9,
              Map.of()));

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
      double lonMax,
      Map<String, String> cityToRegion) {}

  private final Clock clock;

  // Property type cycling arrays per category
  private static final String[] RESIDENTIAL_TYPES = {
    "APARTMENT", "HOUSE", "STUDIO", "TOWNHOUSE", "VILLA", "APARTMENT", "HOUSE", "APARTMENT"
  };
  private static final String[] COMMERCIAL_TYPES = {
    "OFFICE", "RETAIL", "RESTAURANT", "OFFICE", "RETAIL", "OFFICE"
  };
  private static final String[] INDUSTRIAL_TYPES = {"WAREHOUSE", "FACTORY", "WORKSHOP"};
  private static final String[] AGRICULTURAL_TYPES = {"FARMLAND", "GREENHOUSE", "FARMLAND"};

  public void generate(DemoDataContext ctx) {
    LocalDateTime now = LocalDateTime.now(clock);

    for (var teamEntry : ctx.getTeamIds().entrySet()) {
      String teamKey = teamEntry.getKey();
      UUID teamId = teamEntry.getValue();
      UUID createdBy = ctx.getAdminUserForTeam(teamKey).orElse(null);
      String currency = ctx.getCurrencyForTeam(teamKey);
      List<UUID> propertyIds = new ArrayList<>();

      for (int i = 0; i < PROPERTIES_PER_TEAM; i++) {
        UUID propertyId = UUID.randomUUID();

        // Category distribution
        String propertyCategory = categoryForIndex(i);

        // Property type cycling within category
        String propertyType = typeForIndex(propertyCategory, i);

        // Default to VACANT; contract generator sets OCCUPIED for properties with ACTIVE contracts
        String status = "VACANT";

        // Country distribution: cycle through 12 countries
        CountryData country = COUNTRIES.get(i % COUNTRIES.size());
        List<String> streets = streetsForCategory(country, propertyCategory);

        int houseNumber = random.nextInt(1, 200);
        String street = pick(streets) + " " + houseNumber;
        String city = pick(country.cities());
        String postalCode = generatePostalCode(country.postalFormat());

        double lat = country.latMin() + random.nextDouble() * (country.latMax() - country.latMin());
        double lon = country.lonMin() + random.nextDouble() * (country.lonMax() - country.lonMin());

        // Acquisition date based on index band
        LocalDate acquisitionDate = acquisitionDateForIndex(i);

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

        // created_at is around the acquisition date (property was "added" when acquired)
        LocalDateTime createdAt = acquisitionDate.atStartOfDay().plusDays(random.nextInt(0, 30));

        // Rent baseline for this property
        BigDecimal rentBaseline =
            rentBaselineForProperty(country.name(), propertyType, propertyCategory);

        Sid propertyIdentifier = newPropertyId();
        dsl.insertInto(PROPERTIES)
            .set(PROPERTIES.ID, propertyId)
            .set(PROPERTIES.IDENTIFIER, propertyIdentifier)
            .set(PROPERTIES.TEAM_ID, teamId)
            .set(PROPERTIES.STREET, street)
            .set(PROPERTIES.CITY, city)
            .set(PROPERTIES.POSTAL_CODE, postalCode)
            .set(PROPERTIES.COUNTRY_CODE, country.name())
            .set(PROPERTIES.REGION_CODE, country.cityToRegion().get(city))
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
            .set(PROPERTIES.CREATED_AT, createdAt)
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

        // Insert financial data into dedicated tables (all properties)
        insertFinancialData(
            propertyId,
            teamId,
            createdBy,
            currency,
            propertyCategory,
            i,
            yearBuilt,
            acquisitionDate,
            rentBaseline,
            now,
            ctx);

        propertyIds.add(propertyId);
        ctx.putIdentifier(propertyId, propertyIdentifier);
        ctx.putPropertyCategory(propertyId, propertyCategory);
        ctx.putPropertyAcquisitionDate(propertyId, acquisitionDate);
        ctx.putPropertyCountryCode(propertyId, country.name());
        ctx.putPropertyRentBaseline(propertyId, rentBaseline);
        ctx.putPropertyType(propertyId, propertyType);
        ctx.putPropertyCountryRentMultiplier(propertyId, countryRentMultiplier(country.name()));
        ctx.incrementProperties();

        // Add outdoor areas for RESIDENTIAL properties only
        if ("RESIDENTIAL".equals(propertyCategory) && random.nextBoolean()) {
          int outdoorCount = random.nextInt(1, 3);
          Set<String> usedTypes = new HashSet<>();
          for (int j = 0; j < outdoorCount; j++) {
            String outdoorType = pick(OUTDOOR_TYPES);
            if (usedTypes.add(outdoorType)) {
              dsl.insertInto(PROPERTY_OUTDOOR_AREAS)
                  .set(PROPERTY_OUTDOOR_AREAS.ID, UUID.randomUUID())
                  .set(PROPERTY_OUTDOOR_AREAS.IDENTIFIER, newPropertyOutdoorAreaId())
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

  // --- Category and type distribution ---

  private String categoryForIndex(int i) {
    if (i <= 17) {
      return "RESIDENTIAL";
    } else if (i <= 23) {
      return "COMMERCIAL";
    } else if (i <= 26) {
      return "INDUSTRIAL";
    } else {
      return "AGRICULTURAL";
    }
  }

  private String typeForIndex(String category, int i) {
    return switch (category) {
      case "RESIDENTIAL" -> RESIDENTIAL_TYPES[i % RESIDENTIAL_TYPES.length];
      case "COMMERCIAL" -> COMMERCIAL_TYPES[(i - 18) % COMMERCIAL_TYPES.length];
      case "INDUSTRIAL" -> INDUSTRIAL_TYPES[(i - 24) % INDUSTRIAL_TYPES.length];
      case "AGRICULTURAL" -> AGRICULTURAL_TYPES[(i - 27) % AGRICULTURAL_TYPES.length];
      default -> "APARTMENT";
    };
  }

  // --- Acquisition date based on index band ---

  private LocalDate acquisitionDateForIndex(int i) {
    int startYear;
    int endYear;
    int endMonth;
    int endDay;
    if (i <= 4) {
      startYear = 2000;
      endYear = 2004;
      endMonth = 12;
      endDay = 31;
    } else if (i <= 9) {
      startYear = 2005;
      endYear = 2009;
      endMonth = 12;
      endDay = 31;
    } else if (i <= 14) {
      startYear = 2010;
      endYear = 2014;
      endMonth = 12;
      endDay = 31;
    } else if (i <= 19) {
      startYear = 2015;
      endYear = 2019;
      endMonth = 12;
      endDay = 31;
    } else if (i <= 24) {
      startYear = 2020;
      endYear = 2023;
      endMonth = 12;
      endDay = 31;
    } else {
      startYear = 2024;
      endYear = 2025;
      endMonth = 6;
      endDay = 30;
    }
    LocalDate rangeStart = LocalDate.of(startYear, 1, 1);
    LocalDate rangeEnd = LocalDate.of(endYear, endMonth, endDay);
    long daysBetween = ChronoUnit.DAYS.between(rangeStart, rangeEnd);
    return rangeStart.plusDays(random.nextLong(0, daysBetween + 1));
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

  // --- Category-specific financial (era-aware) ---

  /**
   * Derives purchase price from the property's monthly rent baseline and a target gross yield.
   * This ensures all properties have realistic price-to-rent ratios regardless of country or type,
   * preventing perpetually cash-flow-negative properties. Target yield: 5.0-7.5%.
   */
  private long acquisitionPriceFromRent(BigDecimal monthlyRent, int i, int acquisitionYear) {
    // Target gross yield varies by property index (5.0%-7.25%) for realistic diversity
    double targetYield = 0.050 + (i % 10) * 0.0025;
    long annualRent = monthlyRent.longValue() * 12;
    long baseline2024 = Math.round(annualRent / targetYield);
    // Deflate by 3% per year from 2024
    int yearsBack = 2024 - acquisitionYear;
    double deflator = Math.pow(1.0 / 1.03, yearsBack);
    return Math.round(baseline2024 * deflator);
  }

  /**
   * Returns the current (2025) market value in minor units (cents). Appreciation from acquisition
   * at ~3% compound per year.
   */
  private long currentMarketValue(long acquisitionPriceMinor, int acquisitionYear) {
    int yearsSince = 2025 - acquisitionYear;
    double appreciation = Math.pow(1.03, yearsSince);
    return Math.round(acquisitionPriceMinor * appreciation);
  }

  // --- Rent baseline calculation ---

  private BigDecimal rentBaselineForProperty(
      String countryName, String propertyType, String category) {
    double base = 1200.0;
    double countryMul = countryRentMultiplier(countryName);
    double typeMul = typeRentMultiplier(propertyType, category);
    return BigDecimal.valueOf(base * countryMul * typeMul).setScale(0, RoundingMode.HALF_UP);
  }

  private double countryRentMultiplier(String countryName) {
    return switch (countryName) {
      case "Netherlands" -> 1.0;
      case "Germany" -> 0.85;
      case "United Kingdom" -> 1.4;
      case "France" -> 0.95;
      case "Spain" -> 0.65;
      case "Portugal" -> 0.55;
      case "Belgium" -> 0.9;
      case "Italy" -> 0.8;
      case "Austria" -> 0.9;
      case "Switzerland" -> 1.5;
      case "United States" -> 1.3;
      case "Ireland" -> 1.2;
      default -> 1.0;
    };
  }

  private double typeRentMultiplier(String propertyType, String category) {
    return switch (category) {
      case "RESIDENTIAL" ->
          switch (propertyType) {
            case "STUDIO" -> 0.65;
            case "APARTMENT" -> 1.0;
            case "HOUSE" -> 1.3;
            case "TOWNHOUSE" -> 1.1;
            case "VILLA" -> 2.0;
            default -> 1.0;
          };
      case "COMMERCIAL" ->
          switch (propertyType) {
            case "OFFICE" -> 1.8;
            case "RETAIL" -> 2.2;
            case "RESTAURANT" -> 2.5;
            default -> 1.8;
          };
      case "INDUSTRIAL" ->
          switch (propertyType) {
            case "WAREHOUSE" -> 2.5;
            case "FACTORY" -> 3.5;
            case "WORKSHOP" -> 2.0;
            default -> 2.5;
          };
      case "AGRICULTURAL" ->
          switch (propertyType) {
            case "FARMLAND" -> 1.5;
            case "GREENHOUSE" -> 2.0;
            default -> 1.5;
          };
      default -> 1.0;
    };
  }

  // --- Historical interest rate by acquisition year ---

  private BigDecimal historicalInterestRate(int acquisitionYear) {
    if (acquisitionYear <= 2005) {
      // Pre-crisis: ECB rates ~4-5%, mortgage rates ~5-6%
      return BigDecimal.valueOf(4.5 + random.nextDouble() * 1.5).setScale(2, RoundingMode.HALF_UP);
    } else if (acquisitionYear <= 2010) {
      // Financial crisis / recovery: rates dipping
      return BigDecimal.valueOf(3.5 + random.nextDouble() * 1.5).setScale(2, RoundingMode.HALF_UP);
    } else if (acquisitionYear <= 2015) {
      // Post-crisis low rates
      return BigDecimal.valueOf(2.5 + random.nextDouble() * 1.5).setScale(2, RoundingMode.HALF_UP);
    } else if (acquisitionYear <= 2019) {
      // ECB low-rate era: historic lows
      return BigDecimal.valueOf(1.5 + random.nextDouble() * 1.0).setScale(2, RoundingMode.HALF_UP);
    } else if (acquisitionYear <= 2021) {
      // Pandemic lows
      return BigDecimal.valueOf(1.2 + random.nextDouble() * 0.8).setScale(2, RoundingMode.HALF_UP);
    } else if (acquisitionYear == 2022) {
      // Rate hike cycle begins
      return BigDecimal.valueOf(2.5 + random.nextDouble() * 1.5).setScale(2, RoundingMode.HALF_UP);
    } else {
      // 2023+: elevated rates
      return BigDecimal.valueOf(3.5 + random.nextDouble() * 1.5).setScale(2, RoundingMode.HALF_UP);
    }
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
        BigDecimal usable =
            area.multiply(BigDecimal.valueOf(0.85)).setScale(2, java.math.RoundingMode.HALF_UP);
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
                BigDecimal.valueOf(2.7 + random.nextDouble() * 1.3)
                    .setScale(2, java.math.RoundingMode.HALF_UP))
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
                  BigDecimal.valueOf(4.0 + random.nextDouble() * 8.0)
                      .setScale(2, java.math.RoundingMode.HALF_UP))
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
        BigDecimal arableLand =
            area.multiply(BigDecimal.valueOf(0.6 + random.nextDouble() * 0.3))
                .setScale(2, java.math.RoundingMode.HALF_UP);
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
            .set(PROPERTY_AGRICULTURAL_DETAILS.TOTAL_LAND_AREA_VALUE, area)
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
      int yearBuilt,
      LocalDate acquisitionDate,
      BigDecimal rentBaseline,
      LocalDateTime now,
      DemoDataContext ctx) {

    int acquisitionYear = acquisitionDate.getYear();
    long purchasePrice = acquisitionPriceFromRent(rentBaseline, i, acquisitionYear);
    long marketValue = currentMarketValue(purchasePrice, acquisitionYear);

    // === ACQUISITION (all 30 properties) ===
    String acquisitionType;
    if (i % 15 == 10) {
      acquisitionType = "INHERITANCE";
    } else if (i % 15 == 11) {
      acquisitionType = "AUCTION";
    } else {
      acquisitionType = "PURCHASE";
    }
    Long renovationCosts = null;
    String renovationCurrency = null;
    if (yearBuilt < 2000 && random.nextInt(10) < 4) {
      renovationCosts = (long) (purchasePrice * (0.05 + random.nextDouble() * 0.10));
      renovationCurrency = currency;
    }
    long closingCosts = (long) (purchasePrice * (0.02 + random.nextDouble() * 0.03));
    double landPct =
        "AGRICULTURAL".equals(propertyCategory)
            ? 0.50 + random.nextDouble() * 0.10
            : 0.30 + random.nextDouble() * 0.10;
    long landValue = (long) (purchasePrice * landPct);
    String depreciationMethod;
    int depreciationYears;
    if ("COMMERCIAL".equals(propertyCategory) || "INDUSTRIAL".equals(propertyCategory)) {
      depreciationMethod = "DECLINING_BALANCE";
      depreciationYears = "INDUSTRIAL".equals(propertyCategory) ? 20 : 40;
    } else {
      depreciationMethod = "STRAIGHT_LINE";
      depreciationYears = 30;
    }
    String acquisitionNotes = null;
    if (random.nextInt(10) < 3) {
      acquisitionNotes =
          switch (acquisitionType) {
            case "INHERITANCE" -> "Inherited from family estate, no transfer tax applied";
            case "AUCTION" -> "Won at municipal auction, below market value";
            default -> "Standard purchase through certified broker";
          };
    }
    dsl.insertInto(PROPERTY_ACQUISITIONS)
        .set(PROPERTY_ACQUISITIONS.ID, UUID.randomUUID())
        .set(PROPERTY_ACQUISITIONS.IDENTIFIER, newAcquisitionId())
        .set(PROPERTY_ACQUISITIONS.PROPERTY_ID, propertyId)
        .set(PROPERTY_ACQUISITIONS.TEAM_ID, teamId)
        .set(PROPERTY_ACQUISITIONS.ACQUISITION_TYPE, acquisitionType)
        .set(PROPERTY_ACQUISITIONS.ACQUISITION_DATE, acquisitionDate)
        .set(PROPERTY_ACQUISITIONS.PURCHASE_PRICE, BigDecimal.valueOf(purchasePrice))
        .set(PROPERTY_ACQUISITIONS.PURCHASE_PRICE_CURRENCY, currency)
        .set(PROPERTY_ACQUISITIONS.CLOSING_COSTS, BigDecimal.valueOf(closingCosts))
        .set(PROPERTY_ACQUISITIONS.CLOSING_COSTS_CURRENCY, currency)
        .set(
            PROPERTY_ACQUISITIONS.RENOVATION_COSTS,
            renovationCosts != null ? BigDecimal.valueOf(renovationCosts) : null)
        .set(PROPERTY_ACQUISITIONS.RENOVATION_COSTS_CURRENCY, renovationCurrency)
        .set(PROPERTY_ACQUISITIONS.LAND_VALUE, BigDecimal.valueOf(landValue))
        .set(PROPERTY_ACQUISITIONS.LAND_VALUE_CURRENCY, currency)
        .set(PROPERTY_ACQUISITIONS.DEPRECIATION_METHOD, depreciationMethod)
        .set(PROPERTY_ACQUISITIONS.DEPRECIATION_YEARS, depreciationYears)
        .set(PROPERTY_ACQUISITIONS.NOTES, acquisitionNotes)
        .set(PROPERTY_ACQUISITIONS.CREATED_AT, now)
        .set(PROPERTY_ACQUISITIONS.UPDATED_AT, now)
        .set(PROPERTY_ACQUISITIONS.CREATED_BY, createdBy)
        .set(PROPERTY_ACQUISITIONS.UPDATED_BY, createdBy)
        .execute();

    // === VALUATIONS (all 30, 2-3 per property) ===
    // MARKET valuation — recent date, current market value
    dsl.insertInto(PROPERTY_VALUATIONS)
        .set(PROPERTY_VALUATIONS.ID, UUID.randomUUID())
        .set(PROPERTY_VALUATIONS.IDENTIFIER, newValuationId())
        .set(PROPERTY_VALUATIONS.PROPERTY_ID, propertyId)
        .set(PROPERTY_VALUATIONS.TEAM_ID, teamId)
        .set(PROPERTY_VALUATIONS.VALUATION_TYPE, "MARKET")
        .set(
            PROPERTY_VALUATIONS.VALUATION_DATE,
            LocalDate.now(clock).minusMonths(random.nextInt(1, 24)))
        .set(PROPERTY_VALUATIONS.AMOUNT, BigDecimal.valueOf(marketValue))
        .set(PROPERTY_VALUATIONS.CURRENCY, currency)
        .set(PROPERTY_VALUATIONS.SOURCE, APPRAISER_NAMES[i % APPRAISER_NAMES.length])
        .set(PROPERTY_VALUATIONS.NOTES, random.nextInt(4) == 0 ? "Annual market assessment" : null)
        .set(PROPERTY_VALUATIONS.CREATED_AT, now)
        .set(PROPERTY_VALUATIONS.UPDATED_AT, now)
        .set(PROPERTY_VALUATIONS.CREATED_BY, createdBy)
        .set(PROPERTY_VALUATIONS.UPDATED_BY, createdBy)
        .execute();

    // TAX_ASSESSED valuation (70-85% of market)
    long taxAssessedValue = (long) (marketValue * (0.70 + random.nextDouble() * 0.15));
    dsl.insertInto(PROPERTY_VALUATIONS)
        .set(PROPERTY_VALUATIONS.ID, UUID.randomUUID())
        .set(PROPERTY_VALUATIONS.IDENTIFIER, newValuationId())
        .set(PROPERTY_VALUATIONS.PROPERTY_ID, propertyId)
        .set(PROPERTY_VALUATIONS.TEAM_ID, teamId)
        .set(PROPERTY_VALUATIONS.VALUATION_TYPE, "TAX_ASSESSED")
        .set(
            PROPERTY_VALUATIONS.VALUATION_DATE,
            LocalDate.now(clock).minusMonths(random.nextInt(4, 10)))
        .set(PROPERTY_VALUATIONS.AMOUNT, BigDecimal.valueOf(taxAssessedValue))
        .set(PROPERTY_VALUATIONS.CURRENCY, currency)
        .set(PROPERTY_VALUATIONS.SOURCE, TAX_AUTHORITIES[i % TAX_AUTHORITIES.length])
        .set(PROPERTY_VALUATIONS.CREATED_AT, now)
        .set(PROPERTY_VALUATIONS.UPDATED_AT, now)
        .set(PROPERTY_VALUATIONS.CREATED_BY, createdBy)
        .set(PROPERTY_VALUATIONS.UPDATED_BY, createdBy)
        .execute();

    // APPRAISAL valuation (90-95% of market, first 10 properties)
    if (i < 10) {
      long appraisalValue = (long) (marketValue * (0.90 + random.nextDouble() * 0.05));
      dsl.insertInto(PROPERTY_VALUATIONS)
          .set(PROPERTY_VALUATIONS.ID, UUID.randomUUID())
          .set(PROPERTY_VALUATIONS.IDENTIFIER, newValuationId())
          .set(PROPERTY_VALUATIONS.PROPERTY_ID, propertyId)
          .set(PROPERTY_VALUATIONS.TEAM_ID, teamId)
          .set(PROPERTY_VALUATIONS.VALUATION_TYPE, "APPRAISAL")
          .set(
              PROPERTY_VALUATIONS.VALUATION_DATE,
              LocalDate.now(clock).minusMonths(random.nextInt(12, 24)))
          .set(PROPERTY_VALUATIONS.AMOUNT, BigDecimal.valueOf(appraisalValue))
          .set(PROPERTY_VALUATIONS.CURRENCY, currency)
          .set(PROPERTY_VALUATIONS.SOURCE, APPRAISER_NAMES[i % APPRAISER_NAMES.length])
          .set(PROPERTY_VALUATIONS.NOTES, "Independent appraisal for refinancing")
          .set(PROPERTY_VALUATIONS.CREATED_AT, now)
          .set(PROPERTY_VALUATIONS.UPDATED_AT, now)
          .set(PROPERTY_VALUATIONS.CREATED_BY, createdBy)
          .set(PROPERTY_VALUATIONS.UPDATED_BY, createdBy)
          .execute();
    }

    // === FINANCING (most properties, except inherited/auction special indices and first = paid
    // off)
    // Props with financing: indices 1-9, 12-24 (excluding 10,11,25,26 which are inheritance/auction
    // or very recent). Prop 0 = paid off.
    boolean hasFinancing =
        (i >= 1 && i <= 24)
            && !"INHERITANCE".equals(acquisitionType)
            && !"AUCTION".equals(acquisitionType);
    if (hasFinancing) {
      String rateType;
      int rateRoll = random.nextInt(10);
      if (rateRoll < 7) {
        rateType = "FIXED";
      } else if (rateRoll < 9) {
        rateType = "VARIABLE";
      } else {
        rateType = "HYBRID";
      }

      BigDecimal interestRate = historicalInterestRate(acquisitionYear);
      int termYears = 25 + random.nextInt(0, 6); // 25-30 years
      int termMonths = termYears * 12;

      // LTV ratio 55-75% (typical for investment properties)
      double ltv = 0.55 + random.nextDouble() * 0.20;
      long originalAmount = Math.round(purchasePrice * ltv);

      long monthlyPayment =
          calculateMonthlyPayment(originalAmount, interestRate.doubleValue(), termMonths);

      // Current balance: remaining fraction based on years elapsed
      int yearsElapsed = 2025 - acquisitionYear;
      int totalYears = termYears;
      double remainingFraction = Math.max(0.0, 1.0 - ((double) yearsElapsed / totalYears));
      // Slightly adjust for amortization curve (early years pay mostly interest)
      remainingFraction = Math.pow(remainingFraction, 0.7);
      long currentBalance = Math.round(originalAmount * remainingFraction);

      String loanNumber =
          "MTG-" + acquisitionYear + "-" + String.format("%04d", random.nextInt(1000, 9999));
      String financingNotes = null;
      if (random.nextInt(10) < 3) {
        financingNotes = "Fixed rate locked until " + (acquisitionYear + random.nextInt(5, 15));
      }

      UUID financingId = UUID.randomUUID();
      boolean isRefinanced = (i == 5);

      // Determine status: if the loan end date is in the past, it's COMPLETED
      LocalDate loanEndDate = acquisitionDate.plusMonths(termMonths);
      String financingStatus;
      if (isRefinanced) {
        financingStatus = "REFINANCED";
      } else if (loanEndDate.isBefore(LocalDate.now(clock))) {
        financingStatus = "COMPLETED";
      } else {
        financingStatus = "ACTIVE";
      }

      dsl.insertInto(PROPERTY_FINANCINGS)
          .set(PROPERTY_FINANCINGS.ID, financingId)
          .set(PROPERTY_FINANCINGS.IDENTIFIER, newFinancingId())
          .set(PROPERTY_FINANCINGS.PROPERTY_ID, propertyId)
          .set(PROPERTY_FINANCINGS.TEAM_ID, teamId)
          .set(PROPERTY_FINANCINGS.FINANCING_TYPE, "MORTGAGE")
          .set(PROPERTY_FINANCINGS.RATE_TYPE, rateType)
          .set(PROPERTY_FINANCINGS.LENDER_NAME, LENDER_NAMES[i % LENDER_NAMES.length])
          .set(PROPERTY_FINANCINGS.LOAN_NUMBER, loanNumber)
          .set(PROPERTY_FINANCINGS.ORIGINAL_AMOUNT, BigDecimal.valueOf(originalAmount))
          .set(PROPERTY_FINANCINGS.ORIGINAL_AMOUNT_CURRENCY, currency)
          .set(PROPERTY_FINANCINGS.CURRENT_BALANCE, BigDecimal.valueOf(currentBalance))
          .set(PROPERTY_FINANCINGS.CURRENT_BALANCE_CURRENCY, currency)
          .set(PROPERTY_FINANCINGS.INTEREST_RATE, interestRate)
          .set(PROPERTY_FINANCINGS.MONTHLY_PAYMENT, BigDecimal.valueOf(monthlyPayment))
          .set(PROPERTY_FINANCINGS.MONTHLY_PAYMENT_CURRENCY, currency)
          .set(PROPERTY_FINANCINGS.PAYMENT_VARIABLE, "VARIABLE".equals(rateType))
          .set(PROPERTY_FINANCINGS.START_DATE, acquisitionDate.plusDays(15))
          .set(PROPERTY_FINANCINGS.END_DATE, loanEndDate)
          .set(PROPERTY_FINANCINGS.TERM_MONTHS, termMonths)
          .set(PROPERTY_FINANCINGS.STATUS, financingStatus)
          .set(PROPERTY_FINANCINGS.NOTES, financingNotes)
          .set(PROPERTY_FINANCINGS.CREATED_AT, now)
          .set(PROPERTY_FINANCINGS.UPDATED_AT, now)
          .set(PROPERTY_FINANCINGS.CREATED_BY, createdBy)
          .set(PROPERTY_FINANCINGS.UPDATED_BY, createdBy)
          .execute();

      ctx.getFinancingIdsByTeam().computeIfAbsent(teamId, k -> new ArrayList<>()).add(financingId);

      // Prop 5: refinanced — add second financing at lower rate
      if (isRefinanced) {
        UUID refinancedId = UUID.randomUUID();
        BigDecimal lowerRate =
            interestRate.subtract(BigDecimal.ONE).max(BigDecimal.valueOf(150, 2));
        long refinancedPayment =
            calculateMonthlyPayment(currentBalance, lowerRate.doubleValue(), 300);

        dsl.insertInto(PROPERTY_FINANCINGS)
            .set(PROPERTY_FINANCINGS.ID, refinancedId)
            .set(PROPERTY_FINANCINGS.IDENTIFIER, newFinancingId())
            .set(PROPERTY_FINANCINGS.PROPERTY_ID, propertyId)
            .set(PROPERTY_FINANCINGS.TEAM_ID, teamId)
            .set(PROPERTY_FINANCINGS.FINANCING_TYPE, "MORTGAGE")
            .set(PROPERTY_FINANCINGS.RATE_TYPE, "FIXED")
            .set(PROPERTY_FINANCINGS.LENDER_NAME, LENDER_NAMES[(i + 1) % LENDER_NAMES.length])
            .set(
                PROPERTY_FINANCINGS.LOAN_NUMBER,
                "MTG-REFI-" + String.format("%04d", random.nextInt(1000, 9999)))
            .set(PROPERTY_FINANCINGS.ORIGINAL_AMOUNT, BigDecimal.valueOf(currentBalance))
            .set(PROPERTY_FINANCINGS.ORIGINAL_AMOUNT_CURRENCY, currency)
            .set(
                PROPERTY_FINANCINGS.CURRENT_BALANCE,
                BigDecimal.valueOf(Math.round(currentBalance * 0.95)))
            .set(PROPERTY_FINANCINGS.CURRENT_BALANCE_CURRENCY, currency)
            .set(PROPERTY_FINANCINGS.INTEREST_RATE, lowerRate)
            .set(PROPERTY_FINANCINGS.MONTHLY_PAYMENT, BigDecimal.valueOf(refinancedPayment))
            .set(PROPERTY_FINANCINGS.MONTHLY_PAYMENT_CURRENCY, currency)
            .set(PROPERTY_FINANCINGS.PAYMENT_VARIABLE, false)
            .set(PROPERTY_FINANCINGS.START_DATE, LocalDate.now(clock).minusYears(1))
            .set(PROPERTY_FINANCINGS.END_DATE, LocalDate.now(clock).minusYears(1).plusMonths(300))
            .set(PROPERTY_FINANCINGS.TERM_MONTHS, 300)
            .set(PROPERTY_FINANCINGS.STATUS, "ACTIVE")
            .set(PROPERTY_FINANCINGS.NOTES, "Refinanced at lower rate")
            .set(PROPERTY_FINANCINGS.CREATED_AT, now)
            .set(PROPERTY_FINANCINGS.UPDATED_AT, now)
            .set(PROPERTY_FINANCINGS.CREATED_BY, createdBy)
            .set(PROPERTY_FINANCINGS.UPDATED_BY, createdBy)
            .execute();

        ctx.getFinancingIdsByTeam().get(teamId).add(refinancedId);
      }
    }

    // === INSURANCE (all 30, 1-3 policies each) ===
    String provider = INSURANCE_PROVIDERS[i % INSURANCE_PROVIDERS.length];
    long buildingPremium = (long) (marketValue * 0.002) + random.nextInt(100, 300);
    long coverageAmount = (long) (marketValue * (0.80 + random.nextDouble() * 0.20));

    // BUILDING insurance for all
    dsl.insertInto(PROPERTY_INSURANCES)
        .set(PROPERTY_INSURANCES.ID, UUID.randomUUID())
        .set(PROPERTY_INSURANCES.IDENTIFIER, newInsuranceId())
        .set(PROPERTY_INSURANCES.PROPERTY_ID, propertyId)
        .set(PROPERTY_INSURANCES.TEAM_ID, teamId)
        .set(PROPERTY_INSURANCES.INSURANCE_TYPE, "BUILDING")
        .set(PROPERTY_INSURANCES.PROVIDER, provider)
        .set(
            PROPERTY_INSURANCES.POLICY_NUMBER,
            "BLD-" + String.format("%06d", random.nextInt(100000, 999999)))
        .set(PROPERTY_INSURANCES.COVERAGE_AMOUNT, BigDecimal.valueOf(coverageAmount))
        .set(PROPERTY_INSURANCES.COVERAGE_AMOUNT_CURRENCY, currency)
        .set(PROPERTY_INSURANCES.ANNUAL_PREMIUM, BigDecimal.valueOf(buildingPremium))
        .set(PROPERTY_INSURANCES.ANNUAL_PREMIUM_CURRENCY, currency)
        .set(PROPERTY_INSURANCES.PAYMENT_FREQUENCY, "ANNUALLY")
        .set(PROPERTY_INSURANCES.START_DATE, acquisitionDate)
        .set(PROPERTY_INSURANCES.END_DATE, acquisitionDate.plusYears(10))
        .set(PROPERTY_INSURANCES.STATUS, "ACTIVE")
        .set(
            PROPERTY_INSURANCES.NOTES,
            random.nextInt(5) == 0 ? "Comprehensive building coverage" : null)
        .set(PROPERTY_INSURANCES.CREATED_AT, now)
        .set(PROPERTY_INSURANCES.UPDATED_AT, now)
        .set(PROPERTY_INSURANCES.CREATED_BY, createdBy)
        .set(PROPERTY_INSURANCES.UPDATED_BY, createdBy)
        .execute();

    // LIABILITY insurance (props 0-19)
    if (i <= 19) {
      dsl.insertInto(PROPERTY_INSURANCES)
          .set(PROPERTY_INSURANCES.ID, UUID.randomUUID())
          .set(PROPERTY_INSURANCES.IDENTIFIER, newInsuranceId())
          .set(PROPERTY_INSURANCES.PROPERTY_ID, propertyId)
          .set(PROPERTY_INSURANCES.TEAM_ID, teamId)
          .set(PROPERTY_INSURANCES.INSURANCE_TYPE, "LIABILITY")
          .set(PROPERTY_INSURANCES.PROVIDER, provider)
          .set(
              PROPERTY_INSURANCES.POLICY_NUMBER,
              "LIB-" + String.format("%06d", random.nextInt(100000, 999999)))
          .set(PROPERTY_INSURANCES.COVERAGE_AMOUNT, BigDecimal.valueOf(500_000_00L))
          .set(PROPERTY_INSURANCES.COVERAGE_AMOUNT_CURRENCY, currency)
          .set(PROPERTY_INSURANCES.ANNUAL_PREMIUM, BigDecimal.valueOf(random.nextInt(200, 600)))
          .set(PROPERTY_INSURANCES.ANNUAL_PREMIUM_CURRENCY, currency)
          .set(PROPERTY_INSURANCES.PAYMENT_FREQUENCY, "ANNUALLY")
          .set(PROPERTY_INSURANCES.START_DATE, acquisitionDate)
          .set(PROPERTY_INSURANCES.END_DATE, acquisitionDate.plusYears(5))
          .set(PROPERTY_INSURANCES.STATUS, "ACTIVE")
          .set(PROPERTY_INSURANCES.CREATED_AT, now)
          .set(PROPERTY_INSURANCES.UPDATED_AT, now)
          .set(PROPERTY_INSURANCES.CREATED_BY, createdBy)
          .set(PROPERTY_INSURANCES.UPDATED_BY, createdBy)
          .execute();
    }

    // RENT_GUARANTEE insurance (residential props 3-14)
    if (i >= 3 && i <= 14 && "RESIDENTIAL".equals(propertyCategory)) {
      dsl.insertInto(PROPERTY_INSURANCES)
          .set(PROPERTY_INSURANCES.ID, UUID.randomUUID())
          .set(PROPERTY_INSURANCES.IDENTIFIER, newInsuranceId())
          .set(PROPERTY_INSURANCES.PROPERTY_ID, propertyId)
          .set(PROPERTY_INSURANCES.TEAM_ID, teamId)
          .set(PROPERTY_INSURANCES.INSURANCE_TYPE, "RENT_GUARANTEE")
          .set(PROPERTY_INSURANCES.PROVIDER, provider)
          .set(
              PROPERTY_INSURANCES.POLICY_NUMBER,
              "RGT-" + String.format("%06d", random.nextInt(100000, 999999)))
          .set(PROPERTY_INSURANCES.ANNUAL_PREMIUM, BigDecimal.valueOf(random.nextInt(300, 800)))
          .set(PROPERTY_INSURANCES.ANNUAL_PREMIUM_CURRENCY, currency)
          .set(PROPERTY_INSURANCES.PAYMENT_FREQUENCY, "ANNUALLY")
          .set(PROPERTY_INSURANCES.START_DATE, acquisitionDate.plusMonths(1))
          .set(PROPERTY_INSURANCES.END_DATE, acquisitionDate.plusYears(3))
          .set(PROPERTY_INSURANCES.STATUS, "ACTIVE")
          .set(PROPERTY_INSURANCES.NOTES, "Covers up to 12 months unpaid rent")
          .set(PROPERTY_INSURANCES.CREATED_AT, now)
          .set(PROPERTY_INSURANCES.UPDATED_AT, now)
          .set(PROPERTY_INSURANCES.CREATED_BY, createdBy)
          .set(PROPERTY_INSURANCES.UPDATED_BY, createdBy)
          .execute();
    }

    // === TAXES (all 12, with full detail fields) ===
    String authority = TAX_AUTHORITIES[i % TAX_AUTHORITIES.length];
    String taxFrequency;
    String dueMonths;
    if (i % 3 == 0) {
      taxFrequency = "QUARTERLY";
      dueMonths = "3,6,9,12";
    } else if (i % 3 == 1) {
      taxFrequency = "SEMI_ANNUALLY";
      dueMonths = "6,12";
    } else {
      taxFrequency = "ANNUALLY";
      dueMonths = String.valueOf(1 + (i % 12));
    }
    // Tax proportional to market value (0.3-1.2% of assessed value)
    double taxRatePct =
        switch (propertyCategory) {
          case "COMMERCIAL" -> 0.006 + random.nextDouble() * 0.006;
          case "INDUSTRIAL" -> 0.008 + random.nextDouble() * 0.007;
          case "AGRICULTURAL" -> 0.002 + random.nextDouble() * 0.003;
          default -> 0.004 + random.nextDouble() * 0.006;
        };
    long taxAmount = Math.max(300, Math.round(marketValue * taxRatePct));

    dsl.insertInto(PROPERTY_TAXES)
        .set(PROPERTY_TAXES.ID, UUID.randomUUID())
        .set(PROPERTY_TAXES.IDENTIFIER, newPropertyTaxId())
        .set(PROPERTY_TAXES.PROPERTY_ID, propertyId)
        .set(PROPERTY_TAXES.TEAM_ID, teamId)
        .set(PROPERTY_TAXES.TAX_TYPE, "PROPERTY")
        .set(PROPERTY_TAXES.AUTHORITY, authority)
        .set(PROPERTY_TAXES.ANNUAL_AMOUNT, BigDecimal.valueOf(taxAmount))
        .set(PROPERTY_TAXES.CURRENCY, currency)
        .set(PROPERTY_TAXES.PAYMENT_FREQUENCY, taxFrequency)
        .set(PROPERTY_TAXES.DUE_MONTHS, dueMonths)
        .set(PROPERTY_TAXES.TAX_YEAR, 2026)
        .set(PROPERTY_TAXES.START_DATE, LocalDate.of(2026, 1, 1))
        .set(PROPERTY_TAXES.END_DATE, LocalDate.of(2026, 12, 31))
        .set(PROPERTY_TAXES.STATUS, "ACTIVE")
        .set(
            PROPERTY_TAXES.NOTES,
            random.nextInt(4) == 0 ? "Assessment based on 2026 property valuation" : null)
        .set(PROPERTY_TAXES.CREATED_AT, now)
        .set(PROPERTY_TAXES.UPDATED_AT, now)
        .set(PROPERTY_TAXES.CREATED_BY, createdBy)
        .set(PROPERTY_TAXES.UPDATED_BY, createdBy)
        .execute();

    // Second tax: LAND for agricultural, MUNICIPAL for every 3rd property
    if ("AGRICULTURAL".equals(propertyCategory)) {
      dsl.insertInto(PROPERTY_TAXES)
          .set(PROPERTY_TAXES.ID, UUID.randomUUID())
          .set(PROPERTY_TAXES.IDENTIFIER, newPropertyTaxId())
          .set(PROPERTY_TAXES.PROPERTY_ID, propertyId)
          .set(PROPERTY_TAXES.TEAM_ID, teamId)
          .set(PROPERTY_TAXES.TAX_TYPE, "LAND")
          .set(PROPERTY_TAXES.AUTHORITY, authority)
          .set(
              PROPERTY_TAXES.ANNUAL_AMOUNT,
              BigDecimal.valueOf(Math.max(200, Math.round(marketValue * 0.002))))
          .set(PROPERTY_TAXES.CURRENCY, currency)
          .set(PROPERTY_TAXES.PAYMENT_FREQUENCY, "ANNUALLY")
          .set(PROPERTY_TAXES.DUE_MONTHS, "3")
          .set(PROPERTY_TAXES.TAX_YEAR, 2026)
          .set(PROPERTY_TAXES.START_DATE, LocalDate.of(2026, 1, 1))
          .set(PROPERTY_TAXES.END_DATE, LocalDate.of(2026, 12, 31))
          .set(PROPERTY_TAXES.STATUS, "ACTIVE")
          .set(PROPERTY_TAXES.CREATED_AT, now)
          .set(PROPERTY_TAXES.UPDATED_AT, now)
          .set(PROPERTY_TAXES.CREATED_BY, createdBy)
          .set(PROPERTY_TAXES.UPDATED_BY, createdBy)
          .execute();
    } else if (i % 3 == 0) {
      dsl.insertInto(PROPERTY_TAXES)
          .set(PROPERTY_TAXES.ID, UUID.randomUUID())
          .set(PROPERTY_TAXES.IDENTIFIER, newPropertyTaxId())
          .set(PROPERTY_TAXES.PROPERTY_ID, propertyId)
          .set(PROPERTY_TAXES.TEAM_ID, teamId)
          .set(PROPERTY_TAXES.TAX_TYPE, "MUNICIPAL")
          .set(PROPERTY_TAXES.AUTHORITY, authority)
          .set(
              PROPERTY_TAXES.ANNUAL_AMOUNT,
              BigDecimal.valueOf(Math.max(100, Math.round(marketValue * 0.001))))
          .set(PROPERTY_TAXES.CURRENCY, currency)
          .set(PROPERTY_TAXES.PAYMENT_FREQUENCY, "ANNUALLY")
          .set(PROPERTY_TAXES.DUE_MONTHS, "9")
          .set(PROPERTY_TAXES.TAX_YEAR, 2026)
          .set(PROPERTY_TAXES.START_DATE, LocalDate.of(2026, 1, 1))
          .set(PROPERTY_TAXES.END_DATE, LocalDate.of(2026, 12, 31))
          .set(PROPERTY_TAXES.STATUS, "ACTIVE")
          .set(PROPERTY_TAXES.CREATED_AT, now)
          .set(PROPERTY_TAXES.UPDATED_AT, now)
          .set(PROPERTY_TAXES.CREATED_BY, createdBy)
          .set(PROPERTY_TAXES.UPDATED_BY, createdBy)
          .execute();
    }

    // === FEES (category-appropriate, proportional to annual rent for realistic cash flow) ===
    long annualRent = rentBaseline.longValue() * 12;
    switch (propertyCategory) {
      case "RESIDENTIAL" -> {
        if (random.nextInt(3) < 2) {
          insertFee(
              propertyId,
              teamId,
              createdBy,
              currency,
              "HOA",
              "Monthly HOA Dues",
              Math.round(annualRent * (0.04 + random.nextDouble() * 0.06)),
              "MONTHLY",
              "1,2,3,4,5,6,7,8,9,10,11,12",
              acquisitionDate,
              now);
        }
        insertFee(
            propertyId,
            teamId,
            createdBy,
            currency,
            "WASTE_MANAGEMENT",
            "Waste Collection Service",
            Math.round(annualRent * (0.015 + random.nextDouble() * 0.015)),
            "QUARTERLY",
            "3,6,9,12",
            acquisitionDate,
            now);
        insertFee(
            propertyId,
            teamId,
            createdBy,
            currency,
            "WATER",
            "Water & Sewage",
            Math.round(annualRent * (0.02 + random.nextDouble() * 0.02)),
            "QUARTERLY",
            "3,6,9,12",
            acquisitionDate,
            now);
      }
      case "COMMERCIAL" -> {
        insertFee(
            propertyId,
            teamId,
            createdBy,
            currency,
            "MANAGEMENT",
            "Commercial Property Management",
            Math.round(annualRent * (0.06 + random.nextDouble() * 0.04)),
            "MONTHLY",
            "1,2,3,4,5,6,7,8,9,10,11,12",
            acquisitionDate,
            now);
        insertFee(
            propertyId,
            teamId,
            createdBy,
            currency,
            "SECURITY",
            "Security Service",
            Math.round(annualRent * (0.03 + random.nextDouble() * 0.03)),
            "MONTHLY",
            "1,2,3,4,5,6,7,8,9,10,11,12",
            acquisitionDate,
            now);
        insertFee(
            propertyId,
            teamId,
            createdBy,
            currency,
            "CLEANING",
            "Professional Cleaning",
            Math.round(annualRent * (0.04 + random.nextDouble() * 0.04)),
            "MONTHLY",
            "1,2,3,4,5,6,7,8,9,10,11,12",
            acquisitionDate,
            now);
        insertFee(
            propertyId,
            teamId,
            createdBy,
            currency,
            "WASTE_MANAGEMENT",
            "Commercial Waste Disposal",
            Math.round(annualRent * (0.02 + random.nextDouble() * 0.02)),
            "QUARTERLY",
            "3,6,9,12",
            acquisitionDate,
            now);
      }
      case "INDUSTRIAL" -> {
        insertFee(
            propertyId,
            teamId,
            createdBy,
            currency,
            "MANAGEMENT",
            "Industrial Site Management",
            Math.round(annualRent * (0.06 + random.nextDouble() * 0.04)),
            "MONTHLY",
            "1,2,3,4,5,6,7,8,9,10,11,12",
            acquisitionDate,
            now);
        insertFee(
            propertyId,
            teamId,
            createdBy,
            currency,
            "SECURITY",
            "24/7 Security Service",
            Math.round(annualRent * (0.04 + random.nextDouble() * 0.04)),
            "MONTHLY",
            "1,2,3,4,5,6,7,8,9,10,11,12",
            acquisitionDate,
            now);
        insertFee(
            propertyId,
            teamId,
            createdBy,
            currency,
            "WASTE_MANAGEMENT",
            "Industrial Waste Removal",
            Math.round(annualRent * (0.02 + random.nextDouble() * 0.02)),
            "MONTHLY",
            "1,2,3,4,5,6,7,8,9,10,11,12",
            acquisitionDate,
            now);
        insertFee(
            propertyId,
            teamId,
            createdBy,
            currency,
            "UTILITIES",
            "Common Area Utilities",
            Math.round(annualRent * (0.03 + random.nextDouble() * 0.04)),
            "QUARTERLY",
            "3,6,9,12",
            acquisitionDate,
            now);
      }
      case "AGRICULTURAL" -> {
        insertFee(
            propertyId,
            teamId,
            createdBy,
            currency,
            "WATER",
            "Irrigation Water Supply",
            Math.round(annualRent * (0.03 + random.nextDouble() * 0.04)),
            "SEMI_ANNUALLY",
            "4,10",
            acquisitionDate,
            now);
        insertFee(
            propertyId,
            teamId,
            createdBy,
            currency,
            "MAINTENANCE_RESERVE",
            "Farm Maintenance Reserve",
            Math.round(annualRent * (0.03 + random.nextDouble() * 0.05)),
            "ANNUALLY",
            "9",
            acquisitionDate,
            now);
      }
      case "MIXED_USE" -> {
        if (random.nextBoolean()) {
          insertFee(
              propertyId,
              teamId,
              createdBy,
              currency,
              "HOA",
              "Building Association Fees",
              Math.round(annualRent * (0.04 + random.nextDouble() * 0.06)),
              "MONTHLY",
              "1,2,3,4,5,6,7,8,9,10,11,12",
              acquisitionDate,
              now);
        }
        insertFee(
            propertyId,
            teamId,
            createdBy,
            currency,
            "MANAGEMENT",
            "Mixed-Use Property Management",
            Math.round(annualRent * (0.06 + random.nextDouble() * 0.04)),
            "MONTHLY",
            "1,2,3,4,5,6,7,8,9,10,11,12",
            acquisitionDate,
            now);
        insertFee(
            propertyId,
            teamId,
            createdBy,
            currency,
            "WASTE_MANAGEMENT",
            "Waste Collection Service",
            Math.round(annualRent * (0.02 + random.nextDouble() * 0.02)),
            "QUARTERLY",
            "3,6,9,12",
            acquisitionDate,
            now);
      }
      default ->
          insertFee(
              propertyId,
              teamId,
              createdBy,
              currency,
              "MAINTENANCE_RESERVE",
              "Maintenance Reserve",
              Math.round(annualRent * (0.03 + random.nextDouble() * 0.04)),
              "ANNUALLY",
              "9",
              acquisitionDate,
              now);
    }
  }

  private void insertFee(
      UUID propertyId,
      UUID teamId,
      @Nullable UUID createdBy,
      String currency,
      String feeType,
      String name,
      long annualAmount,
      String frequency,
      String dueMths,
      LocalDate startDate,
      LocalDateTime now) {
    dsl.insertInto(PROPERTY_FEES)
        .set(PROPERTY_FEES.ID, UUID.randomUUID())
        .set(PROPERTY_FEES.IDENTIFIER, newPropertyFeeId())
        .set(PROPERTY_FEES.PROPERTY_ID, propertyId)
        .set(PROPERTY_FEES.TEAM_ID, teamId)
        .set(PROPERTY_FEES.FEE_TYPE, feeType)
        .set(PROPERTY_FEES.NAME, name)
        .set(PROPERTY_FEES.ANNUAL_AMOUNT, BigDecimal.valueOf(annualAmount))
        .set(PROPERTY_FEES.CURRENCY, currency)
        .set(PROPERTY_FEES.PAYMENT_FREQUENCY, frequency)
        .set(PROPERTY_FEES.DUE_MONTHS, dueMths)
        .set(PROPERTY_FEES.START_DATE, startDate)
        .set(PROPERTY_FEES.STATUS, "ACTIVE")
        .set(PROPERTY_FEES.NOTES, random.nextInt(5) == 0 ? "Annual rate subject to review" : null)
        .set(PROPERTY_FEES.CREATED_AT, now)
        .set(PROPERTY_FEES.UPDATED_AT, now)
        .set(PROPERTY_FEES.CREATED_BY, createdBy)
        .set(PROPERTY_FEES.UPDATED_BY, createdBy)
        .execute();
  }

  private long calculateMonthlyPayment(long principal, double annualRate, int termMonths) {
    double monthlyRate = annualRate / 100.0 / 12.0;
    if (monthlyRate == 0) {
      return principal / termMonths;
    }
    double payment =
        principal
            * (monthlyRate * Math.pow(1 + monthlyRate, termMonths))
            / (Math.pow(1 + monthlyRate, termMonths) - 1);
    return Math.round(payment);
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
