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

import org.jooq.BatchBindStep;
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

      // Batch record lists for all sub-tables
      List<Object[]> propertyRecords = new ArrayList<>();
      List<Object[]> acquisitionRecords = new ArrayList<>();
      List<Object[]> valuationRecords = new ArrayList<>();
      List<Object[]> financingRecords = new ArrayList<>();
      List<Object[]> insuranceRecords = new ArrayList<>();
      List<Object[]> taxRecords = new ArrayList<>();
      List<Object[]> feeRecords = new ArrayList<>();
      List<Object[]> outdoorRecords = new ArrayList<>();
      List<Object[]> residentialRecords = new ArrayList<>();
      List<Object[]> commercialRecords = new ArrayList<>();
      List<Object[]> industrialRecords = new ArrayList<>();
      List<Object[]> agriculturalRecords = new ArrayList<>();

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
        propertyRecords.add(
            new Object[] {
              propertyId,
              propertyIdentifier,
              teamId,
              street,
              city,
              postalCode,
              country.name(),
              country.cityToRegion().get(city),
              BigDecimal.valueOf(lat),
              BigDecimal.valueOf(lon),
              propertyCategory,
              area,
              "sqm",
              propertyType,
              status,
              yearBuilt,
              yearBuilt < 2000 ? yearBuilt + random.nextInt(5, 30) : null,
              constructionType,
              foundationType,
              roofType,
              flooringType,
              pick(WINDOW_TYPES),
              floors,
              pick(ENERGY_RATINGS),
              LocalDate.now(clock).plusYears(random.nextInt(1, 5)),
              heatingType,
              coolingType,
              "BOILER",
              "MUNICIPAL",
              "INDUSTRIAL".equals(propertyCategory) ? 63 : random.nextBoolean() ? 25 : 35,
              "a",
              "MUNICIPAL",
              !"AGRICULTURAL".equals(propertyCategory) && random.nextBoolean(),
              "AGRICULTURAL".equals(propertyCategory) ? "SEPTIC" : "MUNICIPAL",
              pick(INTERNET_TYPES),
              random.nextBoolean() ? 500 : 1000,
              "mbps",
              "ACTIVE",
              "INDUSTRIAL".equals(propertyCategory) ? random.nextInt(5, 20) : random.nextInt(0, 3),
              "AGRICULTURAL".equals(propertyCategory) ? "NONE" : pick(PARKING_TYPES),
              true,
              random.nextBoolean(),
              !"AGRICULTURAL".equals(propertyCategory),
              "INDUSTRIAL".equals(propertyCategory) && random.nextBoolean(),
              !"AGRICULTURAL".equals(propertyCategory) && random.nextBoolean(),
              "INDUSTRIAL".equals(propertyCategory) || "COMMERCIAL".equals(propertyCategory),
              !"AGRICULTURAL".equals(propertyCategory) && random.nextBoolean(),
              "COMMERCIAL".equals(propertyCategory) || random.nextInt(5) == 0,
              "APARTMENT".equals(propertyType) && random.nextBoolean(),
              "COMMERCIAL".equals(propertyCategory) || random.nextInt(3) == 0,
              false,
              createdAt,
              now,
              createdBy,
              createdBy
            });

        // Collect category-specific details
        collectCategoryDetails(
            residentialRecords,
            commercialRecords,
            industrialRecords,
            agriculturalRecords,
            propertyCategory,
            propertyType,
            propertyId,
            teamId,
            area,
            bedrooms,
            bathrooms,
            createdBy,
            now);

        // Collect financial data into dedicated lists
        collectFinancialData(
            acquisitionRecords,
            valuationRecords,
            financingRecords,
            insuranceRecords,
            taxRecords,
            feeRecords,
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
              outdoorRecords.add(
                  new Object[] {
                    UUID.randomUUID(),
                    newPropertyOutdoorAreaId(),
                    propertyId,
                    teamId,
                    outdoorType,
                    BigDecimal.valueOf(random.nextInt(5, 50)),
                    "sqm",
                    now,
                    now,
                    createdBy,
                    createdBy
                  });
            }
          }
        }
      }

      // Execute all batch inserts
      executeBatchProperties(propertyRecords);
      executeBatchAcquisitions(acquisitionRecords);
      executeBatchValuations(valuationRecords);
      executeBatchFinancings(financingRecords);
      executeBatchInsurances(insuranceRecords);
      executeBatchTaxes(taxRecords);
      executeBatchFees(feeRecords);
      executeBatchOutdoors(outdoorRecords);
      executeBatchResidential(residentialRecords);
      executeBatchCommercial(commercialRecords);
      executeBatchIndustrial(industrialRecords);
      executeBatchAgricultural(agriculturalRecords);

      ctx.getPropertyIdsByTeam().put(teamId, propertyIds);
      log.info("Created {} properties for team {}", PROPERTIES_PER_TEAM, teamKey);
    }
  }

  // --- Batch execution methods ---

  private void executeBatchProperties(List<Object[]> records) {
    if (records.isEmpty()) {
      return;
    }
    var insert =
        dsl.insertInto(PROPERTIES)
            .columns(
                PROPERTIES.ID,
                PROPERTIES.IDENTIFIER,
                PROPERTIES.TEAM_ID,
                PROPERTIES.STREET,
                PROPERTIES.CITY,
                PROPERTIES.POSTAL_CODE,
                PROPERTIES.COUNTRY_CODE,
                PROPERTIES.REGION_CODE,
                PROPERTIES.LATITUDE,
                PROPERTIES.LONGITUDE,
                PROPERTIES.PROPERTY_CATEGORY,
                PROPERTIES.AREA_VALUE,
                PROPERTIES.AREA_UNIT,
                PROPERTIES.PROPERTY_TYPE,
                PROPERTIES.STATUS,
                PROPERTIES.YEAR_BUILT,
                PROPERTIES.YEAR_LAST_RENOVATED,
                PROPERTIES.CONSTRUCTION_TYPE,
                PROPERTIES.FOUNDATION_TYPE,
                PROPERTIES.ROOF_TYPE,
                PROPERTIES.FLOORING_TYPE,
                PROPERTIES.WINDOW_TYPE,
                PROPERTIES.NUMBER_OF_FLOORS,
                PROPERTIES.ENERGY_EFFICIENCY_RATING,
                PROPERTIES.ENERGY_CERTIFICATE_EXPIRY_DATE,
                PROPERTIES.HEATING_TYPE,
                PROPERTIES.COOLING_TYPE,
                PROPERTIES.HOT_WATER_SYSTEM,
                PROPERTIES.ELECTRICITY_CONNECTION_TYPE,
                PROPERTIES.ELECTRICITY_CAPACITY_VALUE,
                PROPERTIES.ELECTRICITY_CAPACITY_UNIT,
                PROPERTIES.WATER_CONNECTION_TYPE,
                PROPERTIES.HAS_GAS_CONNECTION,
                PROPERTIES.SEWAGE_TYPE,
                PROPERTIES.INTERNET_CONNECTION_TYPE,
                PROPERTIES.INTERNET_MAX_SPEED_VALUE,
                PROPERTIES.INTERNET_MAX_SPEED_UNIT,
                PROPERTIES.INTERNET_STATUS,
                PROPERTIES.PARKING_SPACES,
                PROPERTIES.PARKING_TYPE,
                PROPERTIES.HAS_SMOKE_DETECTORS,
                PROPERTIES.HAS_CO_DETECTORS,
                PROPERTIES.HAS_FIRE_EXTINGUISHER,
                PROPERTIES.HAS_SPRINKLER_SYSTEM,
                PROPERTIES.HAS_ALARM_SYSTEM,
                PROPERTIES.HAS_SECURITY_CAMERAS,
                PROPERTIES.HAS_SECURE_ENTRY,
                PROPERTIES.IS_WHEELCHAIR_ACCESSIBLE,
                PROPERTIES.HAS_ELEVATOR,
                PROPERTIES.HAS_STEP_FREE_ENTRANCE,
                PROPERTIES.HAS_ADAPTED_BATHROOM,
                PROPERTIES.CREATED_AT,
                PROPERTIES.UPDATED_AT,
                PROPERTIES.CREATED_BY,
                PROPERTIES.UPDATED_BY)
            .values(
                (UUID) null,
                (Sid) null,
                (UUID) null,
                (String) null,
                (String) null,
                (String) null,
                (String) null,
                (String) null,
                (BigDecimal) null,
                (BigDecimal) null,
                (String) null,
                (BigDecimal) null,
                (String) null,
                (String) null,
                (String) null,
                (Integer) null,
                (Integer) null,
                (String) null,
                (String) null,
                (String) null,
                (String) null,
                (String) null,
                (Integer) null,
                (String) null,
                (LocalDate) null,
                (String) null,
                (String) null,
                (String) null,
                (String) null,
                (Integer) null,
                (String) null,
                (String) null,
                (Boolean) null,
                (String) null,
                (String) null,
                (Integer) null,
                (String) null,
                (String) null,
                (Integer) null,
                (String) null,
                (Boolean) null,
                (Boolean) null,
                (Boolean) null,
                (Boolean) null,
                (Boolean) null,
                (Boolean) null,
                (Boolean) null,
                (Boolean) null,
                (Boolean) null,
                (Boolean) null,
                (Boolean) null,
                (LocalDateTime) null,
                (LocalDateTime) null,
                (UUID) null,
                (UUID) null);
    BatchBindStep batch = dsl.batch(insert);
    for (Object[] r : records) {
      batch = batch.bind(r);
    }
    batch.execute();
  }

  private void executeBatchAcquisitions(List<Object[]> records) {
    if (records.isEmpty()) {
      return;
    }
    var insert =
        dsl.insertInto(PROPERTY_ACQUISITIONS)
            .columns(
                PROPERTY_ACQUISITIONS.ID,
                PROPERTY_ACQUISITIONS.IDENTIFIER,
                PROPERTY_ACQUISITIONS.PROPERTY_ID,
                PROPERTY_ACQUISITIONS.TEAM_ID,
                PROPERTY_ACQUISITIONS.ACQUISITION_TYPE,
                PROPERTY_ACQUISITIONS.ACQUISITION_DATE,
                PROPERTY_ACQUISITIONS.PURCHASE_PRICE,
                PROPERTY_ACQUISITIONS.PURCHASE_PRICE_CURRENCY,
                PROPERTY_ACQUISITIONS.CLOSING_COSTS,
                PROPERTY_ACQUISITIONS.CLOSING_COSTS_CURRENCY,
                PROPERTY_ACQUISITIONS.RENOVATION_COSTS,
                PROPERTY_ACQUISITIONS.RENOVATION_COSTS_CURRENCY,
                PROPERTY_ACQUISITIONS.LAND_VALUE,
                PROPERTY_ACQUISITIONS.LAND_VALUE_CURRENCY,
                PROPERTY_ACQUISITIONS.DEPRECIATION_METHOD,
                PROPERTY_ACQUISITIONS.DEPRECIATION_YEARS,
                PROPERTY_ACQUISITIONS.NOTES,
                PROPERTY_ACQUISITIONS.CREATED_AT,
                PROPERTY_ACQUISITIONS.UPDATED_AT,
                PROPERTY_ACQUISITIONS.CREATED_BY,
                PROPERTY_ACQUISITIONS.UPDATED_BY)
            .values(
                (UUID) null,
                (Sid) null,
                (UUID) null,
                (UUID) null,
                (String) null,
                (LocalDate) null,
                (BigDecimal) null,
                (String) null,
                (BigDecimal) null,
                (String) null,
                (BigDecimal) null,
                (String) null,
                (BigDecimal) null,
                (String) null,
                (String) null,
                (Integer) null,
                (String) null,
                (LocalDateTime) null,
                (LocalDateTime) null,
                (UUID) null,
                (UUID) null);
    BatchBindStep batch = dsl.batch(insert);
    for (Object[] r : records) {
      batch = batch.bind(r);
    }
    batch.execute();
  }

  private void executeBatchValuations(List<Object[]> records) {
    if (records.isEmpty()) {
      return;
    }
    var insert =
        dsl.insertInto(PROPERTY_VALUATIONS)
            .columns(
                PROPERTY_VALUATIONS.ID,
                PROPERTY_VALUATIONS.IDENTIFIER,
                PROPERTY_VALUATIONS.PROPERTY_ID,
                PROPERTY_VALUATIONS.TEAM_ID,
                PROPERTY_VALUATIONS.VALUATION_TYPE,
                PROPERTY_VALUATIONS.VALUATION_DATE,
                PROPERTY_VALUATIONS.AMOUNT,
                PROPERTY_VALUATIONS.CURRENCY,
                PROPERTY_VALUATIONS.SOURCE,
                PROPERTY_VALUATIONS.NOTES,
                PROPERTY_VALUATIONS.CREATED_AT,
                PROPERTY_VALUATIONS.UPDATED_AT,
                PROPERTY_VALUATIONS.CREATED_BY,
                PROPERTY_VALUATIONS.UPDATED_BY)
            .values(
                (UUID) null,
                (Sid) null,
                (UUID) null,
                (UUID) null,
                (String) null,
                (LocalDate) null,
                (BigDecimal) null,
                (String) null,
                (String) null,
                (String) null,
                (LocalDateTime) null,
                (LocalDateTime) null,
                (UUID) null,
                (UUID) null);
    BatchBindStep batch = dsl.batch(insert);
    for (Object[] r : records) {
      batch = batch.bind(r);
    }
    batch.execute();
  }

  private void executeBatchFinancings(List<Object[]> records) {
    if (records.isEmpty()) {
      return;
    }
    var insert =
        dsl.insertInto(PROPERTY_FINANCINGS)
            .columns(
                PROPERTY_FINANCINGS.ID,
                PROPERTY_FINANCINGS.IDENTIFIER,
                PROPERTY_FINANCINGS.PROPERTY_ID,
                PROPERTY_FINANCINGS.TEAM_ID,
                PROPERTY_FINANCINGS.FINANCING_TYPE,
                PROPERTY_FINANCINGS.RATE_TYPE,
                PROPERTY_FINANCINGS.LENDER_NAME,
                PROPERTY_FINANCINGS.LOAN_NUMBER,
                PROPERTY_FINANCINGS.ORIGINAL_AMOUNT,
                PROPERTY_FINANCINGS.ORIGINAL_AMOUNT_CURRENCY,
                PROPERTY_FINANCINGS.CURRENT_BALANCE,
                PROPERTY_FINANCINGS.CURRENT_BALANCE_CURRENCY,
                PROPERTY_FINANCINGS.INTEREST_RATE,
                PROPERTY_FINANCINGS.MONTHLY_PAYMENT,
                PROPERTY_FINANCINGS.MONTHLY_PAYMENT_CURRENCY,
                PROPERTY_FINANCINGS.PAYMENT_VARIABLE,
                PROPERTY_FINANCINGS.START_DATE,
                PROPERTY_FINANCINGS.END_DATE,
                PROPERTY_FINANCINGS.TERM_MONTHS,
                PROPERTY_FINANCINGS.STATUS,
                PROPERTY_FINANCINGS.NOTES,
                PROPERTY_FINANCINGS.CREATED_AT,
                PROPERTY_FINANCINGS.UPDATED_AT,
                PROPERTY_FINANCINGS.CREATED_BY,
                PROPERTY_FINANCINGS.UPDATED_BY)
            .values(
                (UUID) null,
                (Sid) null,
                (UUID) null,
                (UUID) null,
                (String) null,
                (String) null,
                (String) null,
                (String) null,
                (BigDecimal) null,
                (String) null,
                (BigDecimal) null,
                (String) null,
                (BigDecimal) null,
                (BigDecimal) null,
                (String) null,
                (Boolean) null,
                (LocalDate) null,
                (LocalDate) null,
                (Integer) null,
                (String) null,
                (String) null,
                (LocalDateTime) null,
                (LocalDateTime) null,
                (UUID) null,
                (UUID) null);
    BatchBindStep batch = dsl.batch(insert);
    for (Object[] r : records) {
      batch = batch.bind(r);
    }
    batch.execute();
  }

  private void executeBatchInsurances(List<Object[]> records) {
    if (records.isEmpty()) {
      return;
    }
    var insert =
        dsl.insertInto(PROPERTY_INSURANCES)
            .columns(
                PROPERTY_INSURANCES.ID,
                PROPERTY_INSURANCES.IDENTIFIER,
                PROPERTY_INSURANCES.PROPERTY_ID,
                PROPERTY_INSURANCES.TEAM_ID,
                PROPERTY_INSURANCES.INSURANCE_TYPE,
                PROPERTY_INSURANCES.PROVIDER,
                PROPERTY_INSURANCES.POLICY_NUMBER,
                PROPERTY_INSURANCES.COVERAGE_AMOUNT,
                PROPERTY_INSURANCES.COVERAGE_AMOUNT_CURRENCY,
                PROPERTY_INSURANCES.ANNUAL_PREMIUM,
                PROPERTY_INSURANCES.ANNUAL_PREMIUM_CURRENCY,
                PROPERTY_INSURANCES.PAYMENT_FREQUENCY,
                PROPERTY_INSURANCES.START_DATE,
                PROPERTY_INSURANCES.END_DATE,
                PROPERTY_INSURANCES.STATUS,
                PROPERTY_INSURANCES.NOTES,
                PROPERTY_INSURANCES.CREATED_AT,
                PROPERTY_INSURANCES.UPDATED_AT,
                PROPERTY_INSURANCES.CREATED_BY,
                PROPERTY_INSURANCES.UPDATED_BY)
            .values(
                (UUID) null,
                (Sid) null,
                (UUID) null,
                (UUID) null,
                (String) null,
                (String) null,
                (String) null,
                (BigDecimal) null,
                (String) null,
                (BigDecimal) null,
                (String) null,
                (String) null,
                (LocalDate) null,
                (LocalDate) null,
                (String) null,
                (String) null,
                (LocalDateTime) null,
                (LocalDateTime) null,
                (UUID) null,
                (UUID) null);
    BatchBindStep batch = dsl.batch(insert);
    for (Object[] r : records) {
      batch = batch.bind(r);
    }
    batch.execute();
  }

  private void executeBatchTaxes(List<Object[]> records) {
    if (records.isEmpty()) {
      return;
    }
    var insert =
        dsl.insertInto(PROPERTY_TAXES)
            .columns(
                PROPERTY_TAXES.ID,
                PROPERTY_TAXES.IDENTIFIER,
                PROPERTY_TAXES.PROPERTY_ID,
                PROPERTY_TAXES.TEAM_ID,
                PROPERTY_TAXES.TAX_TYPE,
                PROPERTY_TAXES.AUTHORITY,
                PROPERTY_TAXES.ANNUAL_AMOUNT,
                PROPERTY_TAXES.CURRENCY,
                PROPERTY_TAXES.PAYMENT_FREQUENCY,
                PROPERTY_TAXES.DUE_MONTHS,
                PROPERTY_TAXES.TAX_YEAR,
                PROPERTY_TAXES.START_DATE,
                PROPERTY_TAXES.END_DATE,
                PROPERTY_TAXES.STATUS,
                PROPERTY_TAXES.NOTES,
                PROPERTY_TAXES.CREATED_AT,
                PROPERTY_TAXES.UPDATED_AT,
                PROPERTY_TAXES.CREATED_BY,
                PROPERTY_TAXES.UPDATED_BY)
            .values(
                (UUID) null,
                (Sid) null,
                (UUID) null,
                (UUID) null,
                (String) null,
                (String) null,
                (BigDecimal) null,
                (String) null,
                (String) null,
                (String) null,
                (Integer) null,
                (LocalDate) null,
                (LocalDate) null,
                (String) null,
                (String) null,
                (LocalDateTime) null,
                (LocalDateTime) null,
                (UUID) null,
                (UUID) null);
    BatchBindStep batch = dsl.batch(insert);
    for (Object[] r : records) {
      batch = batch.bind(r);
    }
    batch.execute();
  }

  private void executeBatchFees(List<Object[]> records) {
    if (records.isEmpty()) {
      return;
    }
    var insert =
        dsl.insertInto(PROPERTY_FEES)
            .columns(
                PROPERTY_FEES.ID,
                PROPERTY_FEES.IDENTIFIER,
                PROPERTY_FEES.PROPERTY_ID,
                PROPERTY_FEES.TEAM_ID,
                PROPERTY_FEES.FEE_TYPE,
                PROPERTY_FEES.NAME,
                PROPERTY_FEES.ANNUAL_AMOUNT,
                PROPERTY_FEES.CURRENCY,
                PROPERTY_FEES.PAYMENT_FREQUENCY,
                PROPERTY_FEES.DUE_MONTHS,
                PROPERTY_FEES.START_DATE,
                PROPERTY_FEES.STATUS,
                PROPERTY_FEES.NOTES,
                PROPERTY_FEES.CREATED_AT,
                PROPERTY_FEES.UPDATED_AT,
                PROPERTY_FEES.CREATED_BY,
                PROPERTY_FEES.UPDATED_BY)
            .values(
                (UUID) null,
                (Sid) null,
                (UUID) null,
                (UUID) null,
                (String) null,
                (String) null,
                (BigDecimal) null,
                (String) null,
                (String) null,
                (String) null,
                (LocalDate) null,
                (String) null,
                (String) null,
                (LocalDateTime) null,
                (LocalDateTime) null,
                (UUID) null,
                (UUID) null);
    BatchBindStep batch = dsl.batch(insert);
    for (Object[] r : records) {
      batch = batch.bind(r);
    }
    batch.execute();
  }

  private void executeBatchOutdoors(List<Object[]> records) {
    if (records.isEmpty()) {
      return;
    }
    var insert =
        dsl.insertInto(PROPERTY_OUTDOOR_AREAS)
            .columns(
                PROPERTY_OUTDOOR_AREAS.ID,
                PROPERTY_OUTDOOR_AREAS.IDENTIFIER,
                PROPERTY_OUTDOOR_AREAS.PROPERTY_ID,
                PROPERTY_OUTDOOR_AREAS.TEAM_ID,
                PROPERTY_OUTDOOR_AREAS.TYPE,
                PROPERTY_OUTDOOR_AREAS.AREA_VALUE,
                PROPERTY_OUTDOOR_AREAS.AREA_UNIT,
                PROPERTY_OUTDOOR_AREAS.CREATED_AT,
                PROPERTY_OUTDOOR_AREAS.UPDATED_AT,
                PROPERTY_OUTDOOR_AREAS.CREATED_BY,
                PROPERTY_OUTDOOR_AREAS.UPDATED_BY)
            .values(
                (UUID) null,
                (Sid) null,
                (UUID) null,
                (UUID) null,
                (String) null,
                (BigDecimal) null,
                (String) null,
                (LocalDateTime) null,
                (LocalDateTime) null,
                (UUID) null,
                (UUID) null);
    BatchBindStep batch = dsl.batch(insert);
    for (Object[] r : records) {
      batch = batch.bind(r);
    }
    batch.execute();
  }

  private void executeBatchResidential(List<Object[]> records) {
    if (records.isEmpty()) {
      return;
    }
    var insert =
        dsl.insertInto(PROPERTY_RESIDENTIAL_DETAILS)
            .columns(
                PROPERTY_RESIDENTIAL_DETAILS.ID,
                PROPERTY_RESIDENTIAL_DETAILS.PROPERTY_ID,
                PROPERTY_RESIDENTIAL_DETAILS.TEAM_ID,
                PROPERTY_RESIDENTIAL_DETAILS.BEDROOMS,
                PROPERTY_RESIDENTIAL_DETAILS.BATHROOMS,
                PROPERTY_RESIDENTIAL_DETAILS.FURNISHED,
                PROPERTY_RESIDENTIAL_DETAILS.PET_POLICY,
                PROPERTY_RESIDENTIAL_DETAILS.CREATED_AT,
                PROPERTY_RESIDENTIAL_DETAILS.UPDATED_AT,
                PROPERTY_RESIDENTIAL_DETAILS.CREATED_BY,
                PROPERTY_RESIDENTIAL_DETAILS.UPDATED_BY)
            .values(
                (UUID) null,
                (UUID) null,
                (UUID) null,
                (Integer) null,
                (Integer) null,
                (Boolean) null,
                (String) null,
                (LocalDateTime) null,
                (LocalDateTime) null,
                (UUID) null,
                (UUID) null);
    BatchBindStep batch = dsl.batch(insert);
    for (Object[] r : records) {
      batch = batch.bind(r);
    }
    batch.execute();
  }

  private void executeBatchCommercial(List<Object[]> records) {
    if (records.isEmpty()) {
      return;
    }
    var insert =
        dsl.insertInto(PROPERTY_COMMERCIAL_DETAILS)
            .columns(
                PROPERTY_COMMERCIAL_DETAILS.ID,
                PROPERTY_COMMERCIAL_DETAILS.PROPERTY_ID,
                PROPERTY_COMMERCIAL_DETAILS.TEAM_ID,
                PROPERTY_COMMERCIAL_DETAILS.USABLE_AREA_VALUE,
                PROPERTY_COMMERCIAL_DETAILS.USABLE_AREA_UNIT,
                PROPERTY_COMMERCIAL_DETAILS.COMMON_AREA_VALUE,
                PROPERTY_COMMERCIAL_DETAILS.COMMON_AREA_UNIT,
                PROPERTY_COMMERCIAL_DETAILS.FLOOR_LEVEL,
                PROPERTY_COMMERCIAL_DETAILS.CEILING_HEIGHT_VALUE,
                PROPERTY_COMMERCIAL_DETAILS.CEILING_HEIGHT_UNIT,
                PROPERTY_COMMERCIAL_DETAILS.HAS_STOREFRONT,
                PROPERTY_COMMERCIAL_DETAILS.HAS_SIGNAGE_RIGHTS,
                PROPERTY_COMMERCIAL_DETAILS.ZONING_CLASSIFICATION,
                PROPERTY_COMMERCIAL_DETAILS.MAX_OCCUPANCY,
                PROPERTY_COMMERCIAL_DETAILS.RESTROOM_COUNT,
                PROPERTY_COMMERCIAL_DETAILS.HAS_KITCHEN_FACILITY,
                PROPERTY_COMMERCIAL_DETAILS.ACCESSIBILITY_COMPLIANT,
                PROPERTY_COMMERCIAL_DETAILS.CREATED_AT,
                PROPERTY_COMMERCIAL_DETAILS.UPDATED_AT,
                PROPERTY_COMMERCIAL_DETAILS.CREATED_BY,
                PROPERTY_COMMERCIAL_DETAILS.UPDATED_BY)
            .values(
                (UUID) null,
                (UUID) null,
                (UUID) null,
                (BigDecimal) null,
                (String) null,
                (BigDecimal) null,
                (String) null,
                (Integer) null,
                (BigDecimal) null,
                (String) null,
                (Boolean) null,
                (Boolean) null,
                (String) null,
                (Integer) null,
                (Integer) null,
                (Boolean) null,
                (Boolean) null,
                (LocalDateTime) null,
                (LocalDateTime) null,
                (UUID) null,
                (UUID) null);
    BatchBindStep batch = dsl.batch(insert);
    for (Object[] r : records) {
      batch = batch.bind(r);
    }
    batch.execute();
  }

  private void executeBatchIndustrial(List<Object[]> records) {
    if (records.isEmpty()) {
      return;
    }
    var insert =
        dsl.insertInto(PROPERTY_INDUSTRIAL_DETAILS)
            .columns(
                PROPERTY_INDUSTRIAL_DETAILS.ID,
                PROPERTY_INDUSTRIAL_DETAILS.PROPERTY_ID,
                PROPERTY_INDUSTRIAL_DETAILS.TEAM_ID,
                PROPERTY_INDUSTRIAL_DETAILS.CLEAR_HEIGHT_VALUE,
                PROPERTY_INDUSTRIAL_DETAILS.CLEAR_HEIGHT_UNIT,
                PROPERTY_INDUSTRIAL_DETAILS.LOADING_DOCKS,
                PROPERTY_INDUSTRIAL_DETAILS.DRIVE_IN_DOORS,
                PROPERTY_INDUSTRIAL_DETAILS.FLOOR_LOAD_CAPACITY_VALUE,
                PROPERTY_INDUSTRIAL_DETAILS.FLOOR_LOAD_CAPACITY_UNIT,
                PROPERTY_INDUSTRIAL_DETAILS.POWER_CAPACITY_VALUE,
                PROPERTY_INDUSTRIAL_DETAILS.POWER_CAPACITY_UNIT,
                PROPERTY_INDUSTRIAL_DETAILS.HAS_THREE_PHASE_POWER,
                PROPERTY_INDUSTRIAL_DETAILS.HAS_CRANE,
                PROPERTY_INDUSTRIAL_DETAILS.CRANE_CAPACITY_VALUE,
                PROPERTY_INDUSTRIAL_DETAILS.CRANE_CAPACITY_UNIT,
                PROPERTY_INDUSTRIAL_DETAILS.HAS_HAZMAT_CERTIFICATION,
                PROPERTY_INDUSTRIAL_DETAILS.HAS_VENTILATION_SYSTEM,
                PROPERTY_INDUSTRIAL_DETAILS.HAS_CLIMATE_CONTROL,
                PROPERTY_INDUSTRIAL_DETAILS.YARD_AREA_VALUE,
                PROPERTY_INDUSTRIAL_DETAILS.YARD_AREA_UNIT,
                PROPERTY_INDUSTRIAL_DETAILS.ZONING_CLASSIFICATION,
                PROPERTY_INDUSTRIAL_DETAILS.CREATED_AT,
                PROPERTY_INDUSTRIAL_DETAILS.UPDATED_AT,
                PROPERTY_INDUSTRIAL_DETAILS.CREATED_BY,
                PROPERTY_INDUSTRIAL_DETAILS.UPDATED_BY)
            .values(
                (UUID) null,
                (UUID) null,
                (UUID) null,
                (BigDecimal) null,
                (String) null,
                (Integer) null,
                (Integer) null,
                (BigDecimal) null,
                (String) null,
                (Integer) null,
                (String) null,
                (Boolean) null,
                (Boolean) null,
                (BigDecimal) null,
                (String) null,
                (Boolean) null,
                (Boolean) null,
                (Boolean) null,
                (BigDecimal) null,
                (String) null,
                (String) null,
                (LocalDateTime) null,
                (LocalDateTime) null,
                (UUID) null,
                (UUID) null);
    BatchBindStep batch = dsl.batch(insert);
    for (Object[] r : records) {
      batch = batch.bind(r);
    }
    batch.execute();
  }

  private void executeBatchAgricultural(List<Object[]> records) {
    if (records.isEmpty()) {
      return;
    }
    var insert =
        dsl.insertInto(PROPERTY_AGRICULTURAL_DETAILS)
            .columns(
                PROPERTY_AGRICULTURAL_DETAILS.ID,
                PROPERTY_AGRICULTURAL_DETAILS.PROPERTY_ID,
                PROPERTY_AGRICULTURAL_DETAILS.TEAM_ID,
                PROPERTY_AGRICULTURAL_DETAILS.TOTAL_LAND_AREA_VALUE,
                PROPERTY_AGRICULTURAL_DETAILS.TOTAL_LAND_AREA_UNIT,
                PROPERTY_AGRICULTURAL_DETAILS.ARABLE_AREA_VALUE,
                PROPERTY_AGRICULTURAL_DETAILS.ARABLE_AREA_UNIT,
                PROPERTY_AGRICULTURAL_DETAILS.SOIL_TYPE,
                PROPERTY_AGRICULTURAL_DETAILS.HAS_WATER_RIGHTS,
                PROPERTY_AGRICULTURAL_DETAILS.WATER_SOURCE,
                PROPERTY_AGRICULTURAL_DETAILS.IRRIGATION_TYPE,
                PROPERTY_AGRICULTURAL_DETAILS.FENCING_TYPE,
                PROPERTY_AGRICULTURAL_DETAILS.HAS_OUTBUILDINGS,
                PROPERTY_AGRICULTURAL_DETAILS.OUTBUILDING_DETAILS,
                PROPERTY_AGRICULTURAL_DETAILS.CURRENT_USE,
                PROPERTY_AGRICULTURAL_DETAILS.ZONING_CLASSIFICATION,
                PROPERTY_AGRICULTURAL_DETAILS.CREATED_AT,
                PROPERTY_AGRICULTURAL_DETAILS.UPDATED_AT,
                PROPERTY_AGRICULTURAL_DETAILS.CREATED_BY,
                PROPERTY_AGRICULTURAL_DETAILS.UPDATED_BY)
            .values(
                (UUID) null,
                (UUID) null,
                (UUID) null,
                (BigDecimal) null,
                (String) null,
                (BigDecimal) null,
                (String) null,
                (String) null,
                (Boolean) null,
                (String) null,
                (String) null,
                (String) null,
                (Boolean) null,
                (String) null,
                (String) null,
                (String) null,
                (LocalDateTime) null,
                (LocalDateTime) null,
                (UUID) null,
                (UUID) null);
    BatchBindStep batch = dsl.batch(insert);
    for (Object[] r : records) {
      batch = batch.bind(r);
    }
    batch.execute();
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

  private long acquisitionPriceFromRent(BigDecimal monthlyRent, int i, int acquisitionYear) {
    double targetYield = 0.050 + (i % 10) * 0.0025;
    long annualRent = monthlyRent.longValue() * 12;
    long baseline2024 = Math.round(annualRent / targetYield);
    int yearsBack = 2024 - acquisitionYear;
    double deflator = Math.pow(1.0 / 1.03, yearsBack);
    return Math.round(baseline2024 * deflator);
  }

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
      return BigDecimal.valueOf(4.5 + random.nextDouble() * 1.5).setScale(2, RoundingMode.HALF_UP);
    } else if (acquisitionYear <= 2010) {
      return BigDecimal.valueOf(3.5 + random.nextDouble() * 1.5).setScale(2, RoundingMode.HALF_UP);
    } else if (acquisitionYear <= 2015) {
      return BigDecimal.valueOf(2.5 + random.nextDouble() * 1.5).setScale(2, RoundingMode.HALF_UP);
    } else if (acquisitionYear <= 2019) {
      return BigDecimal.valueOf(1.5 + random.nextDouble() * 1.0).setScale(2, RoundingMode.HALF_UP);
    } else if (acquisitionYear <= 2021) {
      return BigDecimal.valueOf(1.2 + random.nextDouble() * 0.8).setScale(2, RoundingMode.HALF_UP);
    } else if (acquisitionYear == 2022) {
      return BigDecimal.valueOf(2.5 + random.nextDouble() * 1.5).setScale(2, RoundingMode.HALF_UP);
    } else {
      return BigDecimal.valueOf(3.5 + random.nextDouble() * 1.5).setScale(2, RoundingMode.HALF_UP);
    }
  }

  // --- Category-specific details collection ---

  private void collectCategoryDetails(
      List<Object[]> residentialRecords,
      List<Object[]> commercialRecords,
      List<Object[]> industrialRecords,
      List<Object[]> agriculturalRecords,
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
          residentialRecords.add(
              new Object[] {
                UUID.randomUUID(),
                propertyId,
                teamId,
                bedrooms,
                bathrooms,
                random.nextBoolean(),
                random.nextBoolean() ? "ALLOWED" : "NOT_ALLOWED",
                now,
                now,
                createdBy,
                createdBy
              });
        }
      }
      case "COMMERCIAL" -> {
        BigDecimal usable =
            area.multiply(BigDecimal.valueOf(0.85)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal common = area.subtract(usable);
        commercialRecords.add(
            new Object[] {
              UUID.randomUUID(),
              propertyId,
              teamId,
              usable,
              "sqm",
              common,
              "sqm",
              random.nextInt(0, 5),
              BigDecimal.valueOf(2.7 + random.nextDouble() * 1.3).setScale(2, RoundingMode.HALF_UP),
              "m",
              "RETAIL".equals(propertyType)
                  || "RESTAURANT".equals(propertyType)
                  || "CAFE".equals(propertyType),
              random.nextBoolean(),
              "COMMERCIAL",
              random.nextInt(10, 100),
              random.nextInt(1, 4),
              "RESTAURANT".equals(propertyType) || "CAFE".equals(propertyType),
              true,
              now,
              now,
              createdBy,
              createdBy
            });
      }
      case "INDUSTRIAL" -> {
        industrialRecords.add(
            new Object[] {
              UUID.randomUUID(),
              propertyId,
              teamId,
              BigDecimal.valueOf(4.0 + random.nextDouble() * 8.0).setScale(2, RoundingMode.HALF_UP),
              "m",
              random.nextInt(1, 6),
              random.nextInt(1, 4),
              BigDecimal.valueOf(1000 + random.nextInt(4000)),
              "kg_sqm",
              random.nextInt(50, 500),
              "kva",
              true,
              random.nextInt(3) == 0,
              random.nextInt(3) == 0 ? BigDecimal.valueOf(5 + random.nextInt(20)) : null,
              random.nextInt(3) == 0 ? "metric_tons" : null,
              random.nextInt(4) == 0,
              true,
              random.nextBoolean(),
              BigDecimal.valueOf(random.nextInt(500, 5000)),
              "sqm",
              "INDUSTRIAL",
              now,
              now,
              createdBy,
              createdBy
            });
      }
      case "AGRICULTURAL" -> {
        BigDecimal arableLand =
            area.multiply(BigDecimal.valueOf(0.6 + random.nextDouble() * 0.3))
                .setScale(2, RoundingMode.HALF_UP);
        String[] soilTypes = {"CLAY", "LOAM", "SANDY", "PEAT", "CHALK"};
        String[] waterSources = {"WELL", "CANAL", "RIVER", "MUNICIPAL"};
        String[] irrigationTypes = {"DRIP", "SPRINKLER", "FLOOD", "NONE"};
        String[] fencingTypes = {"WIRE", "WOODEN", "HEDGE", "NONE"};
        String[] currentUses = {
          "ARABLE_FARMING", "LIVESTOCK", "HORTICULTURE", "VITICULTURE", "MIXED"
        };

        agriculturalRecords.add(
            new Object[] {
              UUID.randomUUID(),
              propertyId,
              teamId,
              area,
              "sqm",
              arableLand,
              "sqm",
              pick(soilTypes),
              random.nextBoolean(),
              pick(waterSources),
              pick(irrigationTypes),
              pick(fencingTypes),
              random.nextBoolean(),
              random.nextBoolean() ? "Barn, tool shed" : null,
              pick(currentUses),
              "AGRICULTURAL",
              now,
              now,
              createdBy,
              createdBy
            });
      }
      default -> {
        // MIXED_USE — no specific detail table
      }
    }
  }

  // --- Financial data collection ---

  private void collectFinancialData(
      List<Object[]> acquisitionRecords,
      List<Object[]> valuationRecords,
      List<Object[]> financingRecords,
      List<Object[]> insuranceRecords,
      List<Object[]> taxRecords,
      List<Object[]> feeRecords,
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

    // === ACQUISITION ===
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
    acquisitionRecords.add(
        new Object[] {
          UUID.randomUUID(),
          newAcquisitionId(),
          propertyId,
          teamId,
          acquisitionType,
          acquisitionDate,
          BigDecimal.valueOf(purchasePrice),
          currency,
          BigDecimal.valueOf(closingCosts),
          currency,
          renovationCosts != null ? BigDecimal.valueOf(renovationCosts) : null,
          renovationCurrency,
          BigDecimal.valueOf(landValue),
          currency,
          depreciationMethod,
          depreciationYears,
          acquisitionNotes,
          now,
          now,
          createdBy,
          createdBy
        });

    // === VALUATIONS ===
    // MARKET valuation
    valuationRecords.add(
        new Object[] {
          UUID.randomUUID(),
          newValuationId(),
          propertyId,
          teamId,
          "MARKET",
          LocalDate.now(clock).minusMonths(random.nextInt(1, 24)),
          BigDecimal.valueOf(marketValue),
          currency,
          APPRAISER_NAMES[i % APPRAISER_NAMES.length],
          random.nextInt(4) == 0 ? "Annual market assessment" : null,
          now,
          now,
          createdBy,
          createdBy
        });

    // TAX_ASSESSED valuation
    long taxAssessedValue = (long) (marketValue * (0.70 + random.nextDouble() * 0.15));
    valuationRecords.add(
        new Object[] {
          UUID.randomUUID(),
          newValuationId(),
          propertyId,
          teamId,
          "TAX_ASSESSED",
          LocalDate.now(clock).minusMonths(random.nextInt(4, 10)),
          BigDecimal.valueOf(taxAssessedValue),
          currency,
          TAX_AUTHORITIES[i % TAX_AUTHORITIES.length],
          null,
          now,
          now,
          createdBy,
          createdBy
        });

    // APPRAISAL valuation (first 10 properties)
    if (i < 10) {
      long appraisalValue = (long) (marketValue * (0.90 + random.nextDouble() * 0.05));
      valuationRecords.add(
          new Object[] {
            UUID.randomUUID(),
            newValuationId(),
            propertyId,
            teamId,
            "APPRAISAL",
            LocalDate.now(clock).minusMonths(random.nextInt(12, 24)),
            BigDecimal.valueOf(appraisalValue),
            currency,
            APPRAISER_NAMES[i % APPRAISER_NAMES.length],
            "Independent appraisal for refinancing",
            now,
            now,
            createdBy,
            createdBy
          });
    }

    // === FINANCING ===
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
      int termYears = 25 + random.nextInt(0, 6);
      int termMonths = termYears * 12;

      double ltv = 0.55 + random.nextDouble() * 0.20;
      long originalAmount = Math.round(purchasePrice * ltv);

      long monthlyPayment =
          calculateMonthlyPayment(originalAmount, interestRate.doubleValue(), termMonths);

      int yearsElapsed = 2025 - acquisitionYear;
      int totalYears = termYears;
      double remainingFraction = Math.max(0.0, 1.0 - ((double) yearsElapsed / totalYears));
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

      LocalDate loanEndDate = acquisitionDate.plusMonths(termMonths);
      String financingStatus;
      if (isRefinanced) {
        financingStatus = "REFINANCED";
      } else if (loanEndDate.isBefore(LocalDate.now(clock))) {
        financingStatus = "COMPLETED";
      } else {
        financingStatus = "ACTIVE";
      }

      financingRecords.add(
          new Object[] {
            financingId,
            newFinancingId(),
            propertyId,
            teamId,
            "MORTGAGE",
            rateType,
            LENDER_NAMES[i % LENDER_NAMES.length],
            loanNumber,
            BigDecimal.valueOf(originalAmount),
            currency,
            BigDecimal.valueOf(currentBalance),
            currency,
            interestRate,
            BigDecimal.valueOf(monthlyPayment),
            currency,
            "VARIABLE".equals(rateType),
            acquisitionDate.plusDays(15),
            loanEndDate,
            termMonths,
            financingStatus,
            financingNotes,
            now,
            now,
            createdBy,
            createdBy
          });

      ctx.getFinancingIdsByTeam().computeIfAbsent(teamId, k -> new ArrayList<>()).add(financingId);

      // Prop 5: refinanced
      if (isRefinanced) {
        UUID refinancedId = UUID.randomUUID();
        BigDecimal lowerRate =
            interestRate.subtract(BigDecimal.ONE).max(BigDecimal.valueOf(150, 2));
        long refinancedPayment =
            calculateMonthlyPayment(currentBalance, lowerRate.doubleValue(), 300);

        financingRecords.add(
            new Object[] {
              refinancedId,
              newFinancingId(),
              propertyId,
              teamId,
              "MORTGAGE",
              "FIXED",
              LENDER_NAMES[(i + 1) % LENDER_NAMES.length],
              "MTG-REFI-" + String.format("%04d", random.nextInt(1000, 9999)),
              BigDecimal.valueOf(currentBalance),
              currency,
              BigDecimal.valueOf(Math.round(currentBalance * 0.95)),
              currency,
              lowerRate,
              BigDecimal.valueOf(refinancedPayment),
              currency,
              false,
              LocalDate.now(clock).minusYears(1),
              LocalDate.now(clock).minusYears(1).plusMonths(300),
              300,
              "ACTIVE",
              "Refinanced at lower rate",
              now,
              now,
              createdBy,
              createdBy
            });

        ctx.getFinancingIdsByTeam().get(teamId).add(refinancedId);
      }
    }

    // === INSURANCE ===
    String provider = INSURANCE_PROVIDERS[i % INSURANCE_PROVIDERS.length];
    long buildingPremium = (long) (marketValue * 0.002) + random.nextInt(100, 300);
    long coverageAmount = (long) (marketValue * (0.80 + random.nextDouble() * 0.20));

    // BUILDING insurance for all
    insuranceRecords.add(
        new Object[] {
          UUID.randomUUID(),
          newInsuranceId(),
          propertyId,
          teamId,
          "BUILDING",
          provider,
          "BLD-" + String.format("%06d", random.nextInt(100000, 999999)),
          BigDecimal.valueOf(coverageAmount),
          currency,
          BigDecimal.valueOf(buildingPremium),
          currency,
          "ANNUALLY",
          acquisitionDate,
          acquisitionDate.plusYears(10),
          "ACTIVE",
          random.nextInt(5) == 0 ? "Comprehensive building coverage" : null,
          now,
          now,
          createdBy,
          createdBy
        });

    // LIABILITY insurance (props 0-19)
    if (i <= 19) {
      insuranceRecords.add(
          new Object[] {
            UUID.randomUUID(),
            newInsuranceId(),
            propertyId,
            teamId,
            "LIABILITY",
            provider,
            "LIB-" + String.format("%06d", random.nextInt(100000, 999999)),
            BigDecimal.valueOf(500_000_00L),
            currency,
            BigDecimal.valueOf(random.nextInt(200, 600)),
            currency,
            "ANNUALLY",
            acquisitionDate,
            acquisitionDate.plusYears(5),
            "ACTIVE",
            null,
            now,
            now,
            createdBy,
            createdBy
          });
    }

    // RENT_GUARANTEE insurance (residential props 3-14)
    if (i >= 3 && i <= 14 && "RESIDENTIAL".equals(propertyCategory)) {
      insuranceRecords.add(
          new Object[] {
            UUID.randomUUID(),
            newInsuranceId(),
            propertyId,
            teamId,
            "RENT_GUARANTEE",
            provider,
            "RGT-" + String.format("%06d", random.nextInt(100000, 999999)),
            null,
            null,
            BigDecimal.valueOf(random.nextInt(300, 800)),
            currency,
            "ANNUALLY",
            acquisitionDate.plusMonths(1),
            acquisitionDate.plusYears(3),
            "ACTIVE",
            "Covers up to 12 months unpaid rent",
            now,
            now,
            createdBy,
            createdBy
          });
    }

    // === TAXES ===
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
    double taxRatePct =
        switch (propertyCategory) {
          case "COMMERCIAL" -> 0.006 + random.nextDouble() * 0.006;
          case "INDUSTRIAL" -> 0.008 + random.nextDouble() * 0.007;
          case "AGRICULTURAL" -> 0.002 + random.nextDouble() * 0.003;
          default -> 0.004 + random.nextDouble() * 0.006;
        };
    long taxAmount = Math.max(300, Math.round(marketValue * taxRatePct));

    taxRecords.add(
        new Object[] {
          UUID.randomUUID(),
          newPropertyTaxId(),
          propertyId,
          teamId,
          "PROPERTY",
          authority,
          BigDecimal.valueOf(taxAmount),
          currency,
          taxFrequency,
          dueMonths,
          2026,
          LocalDate.of(2026, 1, 1),
          LocalDate.of(2026, 12, 31),
          "ACTIVE",
          random.nextInt(4) == 0 ? "Assessment based on 2026 property valuation" : null,
          now,
          now,
          createdBy,
          createdBy
        });

    // Second tax
    if ("AGRICULTURAL".equals(propertyCategory)) {
      taxRecords.add(
          new Object[] {
            UUID.randomUUID(),
            newPropertyTaxId(),
            propertyId,
            teamId,
            "LAND",
            authority,
            BigDecimal.valueOf(Math.max(200, Math.round(marketValue * 0.002))),
            currency,
            "ANNUALLY",
            "3",
            2026,
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 12, 31),
            "ACTIVE",
            null,
            now,
            now,
            createdBy,
            createdBy
          });
    } else if (i % 3 == 0) {
      taxRecords.add(
          new Object[] {
            UUID.randomUUID(),
            newPropertyTaxId(),
            propertyId,
            teamId,
            "MUNICIPAL",
            authority,
            BigDecimal.valueOf(Math.max(100, Math.round(marketValue * 0.001))),
            currency,
            "ANNUALLY",
            "9",
            2026,
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 12, 31),
            "ACTIVE",
            null,
            now,
            now,
            createdBy,
            createdBy
          });
    }

    // === FEES ===
    long annualRent = rentBaseline.longValue() * 12;
    switch (propertyCategory) {
      case "RESIDENTIAL" -> {
        if (random.nextInt(3) < 2) {
          addFee(
              feeRecords,
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
        addFee(
            feeRecords,
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
        addFee(
            feeRecords,
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
        addFee(
            feeRecords,
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
        addFee(
            feeRecords,
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
        addFee(
            feeRecords,
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
        addFee(
            feeRecords,
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
        addFee(
            feeRecords,
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
        addFee(
            feeRecords,
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
        addFee(
            feeRecords,
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
        addFee(
            feeRecords,
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
        addFee(
            feeRecords,
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
        addFee(
            feeRecords,
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
          addFee(
              feeRecords,
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
        addFee(
            feeRecords,
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
        addFee(
            feeRecords,
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
          addFee(
              feeRecords,
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

  private void addFee(
      List<Object[]> feeRecords,
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
    feeRecords.add(
        new Object[] {
          UUID.randomUUID(),
          newPropertyFeeId(),
          propertyId,
          teamId,
          feeType,
          name,
          BigDecimal.valueOf(annualAmount),
          currency,
          frequency,
          dueMths,
          startDate,
          "ACTIVE",
          random.nextInt(5) == 0 ? "Annual rate subject to review" : null,
          now,
          now,
          createdBy,
          createdBy
        });
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
