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
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
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

  public static final int PROPERTIES_PER_TEAM = 12;

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
                  "BY",
                  "Cologne",
                  "BY",
                  "Stuttgart",
                  "BY",
                  "Düsseldorf",
                  "BY",
                  "Dresden",
                  "BE")),
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
                  "NY",
                  "Houston",
                  "CA",
                  "Miami",
                  "NY",
                  "Seattle",
                  "OR")),
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

        // Insert financial data into dedicated tables (all properties)
        insertFinancialData(
            propertyId, teamId, createdBy, currency, propertyCategory, i, yearBuilt, now, ctx);

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
      LocalDateTime now,
      DemoDataContext ctx) {

    long purchasePrice = purchasePriceForCategory(propertyCategory, i);
    long marketValue = marketValueForCategory(propertyCategory, i);
    LocalDate acquisitionDate = LocalDate.of(2015 + (i % 9), 1 + (i % 12), 15);

    // === ACQUISITION (all 12 properties) ===
    String acquisitionType = i == 10 ? "INHERITANCE" : i == 11 ? "AUCTION" : "PURCHASE";
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

    // === VALUATIONS (all 12, 2-3 per property) ===
    // MARKET valuation
    dsl.insertInto(PROPERTY_VALUATIONS)
        .set(PROPERTY_VALUATIONS.ID, UUID.randomUUID())
        .set(PROPERTY_VALUATIONS.IDENTIFIER, newValuationId())
        .set(PROPERTY_VALUATIONS.PROPERTY_ID, propertyId)
        .set(PROPERTY_VALUATIONS.TEAM_ID, teamId)
        .set(PROPERTY_VALUATIONS.VALUATION_TYPE, "MARKET")
        .set(
            PROPERTY_VALUATIONS.VALUATION_DATE,
            LocalDate.now(clock).minusMonths(random.nextInt(1, 12)))
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

    // APPRAISAL valuation (90-95% of market, first 6 properties)
    if (i < 6) {
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

    // === FINANCING (props 1-9, prop 0 = paid off, 10 = inherited, 11 = auction) ===
    if (i >= 1 && i <= 9) {
      String rateType;
      int rateRoll = random.nextInt(10);
      if (rateRoll < 7) {
        rateType = "FIXED";
      } else if (rateRoll < 9) {
        rateType = "VARIABLE";
      } else {
        rateType = "HYBRID";
      }

      BigDecimal interestRate = BigDecimal.valueOf(random.nextInt(250, 550), 2);
      int termMonths =
          "COMMERCIAL".equals(propertyCategory) || "INDUSTRIAL".equals(propertyCategory)
              ? 240
              : random.nextBoolean() ? 300 : 360;
      long originalAmount = (long) (purchasePrice * 0.80);
      long monthlyPayment =
          calculateMonthlyPayment(originalAmount, interestRate.doubleValue(), termMonths);
      long currentBalance = (long) (originalAmount * (0.70 + random.nextDouble() * 0.25));
      String loanNumber =
          "MTG-" + (2015 + (i % 9)) + "-" + String.format("%04d", random.nextInt(1000, 9999));
      String financingNotes = null;
      if (random.nextInt(10) < 3) {
        financingNotes = "Fixed rate locked until " + (2025 + random.nextInt(1, 10));
      }

      UUID financingId = UUID.randomUUID();
      boolean isRefinanced = (i == 5);

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
          .set(PROPERTY_FINANCINGS.END_DATE, acquisitionDate.plusMonths(termMonths))
          .set(PROPERTY_FINANCINGS.TERM_MONTHS, termMonths)
          .set(PROPERTY_FINANCINGS.STATUS, isRefinanced ? "REFINANCED" : "ACTIVE")
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
                BigDecimal.valueOf((long) (currentBalance * 0.95)))
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

    // === INSURANCE (all 12, 1-3 policies each) ===
    String provider = INSURANCE_PROVIDERS[i % INSURANCE_PROVIDERS.length];
    long buildingPremium = (long) (marketValue * 0.002) + random.nextInt(100_00, 300_00);
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

    // LIABILITY insurance (props 0-7)
    if (i <= 7) {
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
          .set(
              PROPERTY_INSURANCES.ANNUAL_PREMIUM,
              BigDecimal.valueOf(random.nextInt(200_00, 600_00)))
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

    // RENT_GUARANTEE insurance (props 3-6)
    if (i >= 3 && i <= 6) {
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
          .set(
              PROPERTY_INSURANCES.ANNUAL_PREMIUM,
              BigDecimal.valueOf(random.nextInt(300_00, 800_00)))
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
    long taxAmount =
        switch (propertyCategory) {
          case "COMMERCIAL" -> random.nextInt(2000_00, 5000_00);
          case "INDUSTRIAL" -> random.nextInt(3000_00, 8000_00);
          case "AGRICULTURAL" -> random.nextInt(500_00, 2000_00);
          case "MIXED_USE" -> random.nextInt(2500_00, 6000_00);
          default -> random.nextInt(1200_00, 3000_00);
        };

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
          .set(PROPERTY_TAXES.ANNUAL_AMOUNT, BigDecimal.valueOf(random.nextInt(300_00, 1000_00)))
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
          .set(PROPERTY_TAXES.ANNUAL_AMOUNT, BigDecimal.valueOf(random.nextInt(200_00, 800_00)))
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

    // === FEES (all 12, category-appropriate, 2-4 each) ===
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
              random.nextInt(600_00, 1800_00),
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
            random.nextInt(200_00, 500_00),
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
            random.nextInt(300_00, 800_00),
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
            random.nextInt(2400_00, 6000_00),
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
            random.nextInt(1200_00, 3600_00),
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
            random.nextInt(1800_00, 4200_00),
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
            random.nextInt(600_00, 1500_00),
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
            random.nextInt(3600_00, 9000_00),
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
            random.nextInt(2400_00, 6000_00),
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
            random.nextInt(1200_00, 3000_00),
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
            random.nextInt(1800_00, 4800_00),
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
            random.nextInt(400_00, 1200_00),
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
            random.nextInt(500_00, 1500_00),
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
              random.nextInt(800_00, 2400_00),
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
            random.nextInt(2000_00, 5000_00),
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
            random.nextInt(400_00, 1000_00),
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
              random.nextInt(500_00, 1000_00),
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
