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
  private static final String[] INTERNET_TYPES = {"FIBER", "CABLE", "DSL"};
  private static final String[] PARKING_TYPES = {"GARAGE", "STREET", "UNDERGROUND", "NONE"};
  private static final String[] OUTDOOR_TYPES = {"BALCONY", "TERRACE", "GARDEN"};

  /** Country-keyed provider names — picked off the property's actual country, not the index. */
  private static final Map<String, String> INSURANCE_PROVIDERS =
      Map.of(
          "Netherlands", "Nationale-Nederlanden",
          "Germany", "Allianz",
          "France", "AXA France",
          "Spain", "Mapfre",
          "Portugal", "Fidelidade",
          "Italy", "Generali");

  private static final Map<String, String> TAX_AUTHORITIES =
      Map.of(
          "Netherlands", "Belastingdienst",
          "Germany", "Finanzamt Berlin",
          "France", "Centre des Impôts",
          "Spain", "Hacienda Pública",
          "Portugal", "Autoridade Tributária",
          "Italy", "Agenzia delle Entrate");

  private static final Map<String, String> APPRAISER_NAMES =
      Map.of(
          "Netherlands", "Makelaardij Van der Berg",
          "Germany", "Immobilienbewertung Schmidt",
          "France", "Cabinet d'Expertise Dupont",
          "Spain", "Sociedad de Tasación",
          "Portugal", "Avaliadores Certificados",
          "Italy", "Perizie Immobiliari");

  private static final Map<String, String> LENDER_NAMES =
      Map.of(
          "Netherlands", "ABN AMRO",
          "Germany", "Deutsche Bank",
          "France", "BNP Paribas",
          "Spain", "Banco Santander",
          "Portugal", "Millennium BCP",
          "Italy", "UniCredit");

  public static final int PROPERTIES_PER_TEAM = PortfolioCatalog.ENTRIES.size();

  /**
   * Curated portfolio of a small Western/Southern European landlord.
   *
   * <p>10 units across NL/PT/ES/IT/DE: 9 residential + 1 commercial. Prices, rents, and mortgage
   * terms are tuned so aggregate cash flow is modestly positive — the two oldest NL assets are
   * paid-off and subsidise the more recent leveraged purchases.
   */
  private record PortfolioEntry(
      String countryName,
      String city,
      String street,
      int houseNumber,
      String postalCode,
      double latitude,
      double longitude,
      String category,
      String propertyType,
      int areaSqm,
      int bedrooms,
      int yearBuilt,
      LocalDate acquisitionDate,
      long purchasePriceEuros,
      long monthlyRentEuros,
      boolean hasMortgage,
      long mortgageOriginalEuros,
      double mortgageInterestRate,
      int mortgageTermYears) {}

  private static final class PortfolioCatalog {
    static final List<PortfolioEntry> ENTRIES =
        List.of(
            new PortfolioEntry(
                "Netherlands",
                "Amsterdam",
                "Keizersgracht",
                142,
                "1015 CW",
                52.3676,
                4.8884,
                "RESIDENTIAL",
                "APARTMENT",
                75,
                2,
                1898,
                LocalDate.of(2003, 4, 12),
                280_000L,
                1_950L,
                false,
                0L,
                0.0,
                0),
            new PortfolioEntry(
                "Netherlands",
                "Amsterdam",
                "Vondelstraat",
                88,
                "1054 GR",
                52.3580,
                4.8689,
                "RESIDENTIAL",
                "STUDIO",
                38,
                1,
                1925,
                LocalDate.of(2008, 9, 20),
                210_000L,
                1_250L,
                false,
                0L,
                0.0,
                0),
            new PortfolioEntry(
                "Netherlands",
                "Rotterdam",
                "Coolsingel",
                64,
                "3012 AD",
                51.9225,
                4.4791,
                "RESIDENTIAL",
                "APARTMENT",
                82,
                2,
                1980,
                LocalDate.of(2014, 6, 10),
                245_000L,
                1_750L,
                true,
                140_000L,
                3.0,
                25),
            new PortfolioEntry(
                "Netherlands",
                "Utrecht",
                "Oudegracht",
                312,
                "3511 PB",
                52.0907,
                5.1214,
                "RESIDENTIAL",
                "TOWNHOUSE",
                110,
                3,
                1965,
                LocalDate.of(2018, 3, 15),
                410_000L,
                2_100L,
                true,
                240_000L,
                2.0,
                30),
            new PortfolioEntry(
                "Netherlands",
                "Haarlem",
                "Generaal Cronjéstraat",
                24,
                "2021 JC",
                52.3874,
                4.6462,
                "RESIDENTIAL",
                "HOUSE",
                145,
                4,
                1955,
                LocalDate.of(2021, 11, 8),
                575_000L,
                2_600L,
                true,
                340_000L,
                1.8,
                30),
            new PortfolioEntry(
                "Portugal",
                "Lisbon",
                "Rua da Madalena",
                78,
                "1100-321",
                38.7100,
                -9.1357,
                "RESIDENTIAL",
                "APARTMENT",
                70,
                2,
                1948,
                LocalDate.of(2016, 9, 22),
                195_000L,
                1_300L,
                true,
                115_000L,
                2.5,
                25),
            new PortfolioEntry(
                "Portugal",
                "Porto",
                "Rua de Cedofeita",
                233,
                "4050-178",
                41.1500,
                -8.6175,
                "RESIDENTIAL",
                "STUDIO",
                42,
                1,
                1932,
                LocalDate.of(2020, 5, 18),
                145_000L,
                900L,
                true,
                85_000L,
                1.5,
                25),
            new PortfolioEntry(
                "Spain",
                "Valencia",
                "Carrer de Colón",
                18,
                "46004",
                39.4699,
                -0.3763,
                "RESIDENTIAL",
                "APARTMENT",
                85,
                2,
                1970,
                LocalDate.of(2019, 7, 30),
                215_000L,
                1_400L,
                true,
                125_000L,
                2.5,
                25),
            new PortfolioEntry(
                "Italy",
                "Milan",
                "Via Tortona",
                27,
                "20144",
                45.4525,
                9.1700,
                "RESIDENTIAL",
                "APARTMENT",
                55,
                1,
                1962,
                LocalDate.of(2022, 1, 12),
                340_000L,
                1_800L,
                true,
                200_000L,
                3.5,
                25),
            new PortfolioEntry(
                "Germany",
                "Berlin",
                "Schliemannstraße",
                9,
                "10437",
                52.5410,
                13.4180,
                "COMMERCIAL",
                "RETAIL",
                60,
                0,
                1928,
                LocalDate.of(2017, 4, 5),
                280_000L,
                2_800L,
                true,
                165_000L,
                2.0,
                25));

    static PortfolioEntry get(int i) {
      return ENTRIES.get(i);
    }
  }

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

  private static CountryData countryByName(String name) {
    for (CountryData c : COUNTRIES) {
      if (c.name().equals(name)) {
        return c;
      }
    }
    throw new IllegalStateException("Unknown country in portfolio: " + name);
  }

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
        PortfolioEntry entry = PortfolioCatalog.get(i);
        UUID propertyId = UUID.randomUUID();

        String propertyCategory = entry.category();
        String propertyType = entry.propertyType();

        CountryData country = countryByName(entry.countryName());
        String street = entry.street() + " " + entry.houseNumber();
        String city = entry.city();
        String postalCode = entry.postalCode();

        double lat = entry.latitude();
        double lon = entry.longitude();

        LocalDate acquisitionDate = entry.acquisitionDate();

        int yearBuilt = entry.yearBuilt();
        BigDecimal area = BigDecimal.valueOf(entry.areaSqm());

        int bedrooms = entry.bedrooms();
        int bathrooms = bedrooms == 0 ? 0 : Math.max(1, bedrooms / 2 + 1);

        // Category-specific construction attributes
        String constructionType = constructionTypeForCategory(propertyCategory);
        String foundationType = foundationTypeForCategory(propertyCategory);
        String roofType = roofTypeForCategory(propertyCategory);
        int floors = floorsForCategory(propertyCategory);

        // created_at is around the acquisition date (property was "added" when acquired)
        LocalDateTime createdAt = acquisitionDate.atStartOfDay().plusDays(random.nextInt(0, 30));

        BigDecimal rentBaseline = BigDecimal.valueOf(entry.monthlyRentEuros());

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
              propertyType,
              yearBuilt,
              yearBuilt < 2000 ? yearBuilt + random.nextInt(5, 30) : null,
              constructionType,
              foundationType,
              roofType,
              floors,
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
              "INDUSTRIAL".equals(propertyCategory) && random.nextBoolean(),
              !"AGRICULTURAL".equals(propertyCategory) && random.nextBoolean(),
              "INDUSTRIAL".equals(propertyCategory) || "COMMERCIAL".equals(propertyCategory),
              !"AGRICULTURAL".equals(propertyCategory) && random.nextBoolean(),
              "COMMERCIAL".equals(propertyCategory) || random.nextInt(5) == 0,
              "APARTMENT".equals(propertyType) && random.nextBoolean(),
              "COMMERCIAL".equals(propertyCategory) || random.nextInt(3) == 0,
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
            ctx,
            entry);

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
                PROPERTIES.PROPERTY_TYPE,
                PROPERTIES.YEAR_BUILT,
                PROPERTIES.YEAR_LAST_RENOVATED,
                PROPERTIES.CONSTRUCTION_TYPE,
                PROPERTIES.FOUNDATION_TYPE,
                PROPERTIES.ROOF_TYPE,
                PROPERTIES.NUMBER_OF_FLOORS,
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
                PROPERTIES.HAS_SPRINKLER_SYSTEM,
                PROPERTIES.HAS_ALARM_SYSTEM,
                PROPERTIES.HAS_SECURITY_CAMERAS,
                PROPERTIES.HAS_SECURE_ENTRY,
                PROPERTIES.IS_WHEELCHAIR_ACCESSIBLE,
                PROPERTIES.HAS_ELEVATOR,
                PROPERTIES.HAS_STEP_FREE_ENTRANCE,
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
                (String) null,
                (Integer) null,
                (Integer) null,
                (String) null,
                (String) null,
                (String) null,
                (Integer) null,
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

  private int floorsForCategory(String category) {
    return switch (category) {
      case "INDUSTRIAL" -> 1;
      case "AGRICULTURAL" -> 1;
      case "COMMERCIAL" -> random.nextInt(1, 6);
      default -> random.nextInt(1, 4);
    };
  }

  // --- Property valuation ---

  private long currentMarketValue(long acquisitionPriceMinor, int acquisitionYear) {
    int yearsSince = LocalDate.now(clock).getYear() - acquisitionYear;
    double appreciation = Math.pow(1.03, yearsSince);
    return Math.round(acquisitionPriceMinor * appreciation);
  }

  /** Country rent multiplier, exposed for the expense generator's country scaling. */
  private double countryRentMultiplier(String countryName) {
    return switch (countryName) {
      case "Netherlands" -> 1.0;
      case "Germany" -> 0.85;
      case "France" -> 0.95;
      case "Spain" -> 0.65;
      case "Portugal" -> 0.55;
      case "Italy" -> 0.8;
      default -> 1.0;
    };
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
        // TODO(BUUR-106 Task 9): property_residential_details was dropped in V068; residential
        // demo details move to unit_residential_details at unit level.
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
      DemoDataContext ctx,
      PortfolioEntry entry) {

    int acquisitionYear = acquisitionDate.getYear();
    long purchasePrice = entry.purchasePriceEuros();
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
          APPRAISER_NAMES.get(entry.countryName()),
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
          TAX_AUTHORITIES.get(entry.countryName()),
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
            APPRAISER_NAMES.get(entry.countryName()),
            "Independent appraisal for refinancing",
            now,
            now,
            createdBy,
            createdBy
          });
    }

    // === FINANCING ===
    if (entry.hasMortgage()) {
      String rateType = "FIXED";
      BigDecimal interestRate =
          BigDecimal.valueOf(entry.mortgageInterestRate()).setScale(2, RoundingMode.HALF_UP);
      int termYears = entry.mortgageTermYears();
      int termMonths = termYears * 12;
      long originalAmount = entry.mortgageOriginalEuros();

      long monthlyPayment =
          calculateMonthlyPayment(originalAmount, interestRate.doubleValue(), termMonths);

      int yearsElapsed = LocalDate.now(clock).getYear() - acquisitionYear;
      double remainingFraction =
          Math.max(0.0, 1.0 - ((double) yearsElapsed / Math.max(1, termYears)));
      // Front-loaded interest curve: remaining principal decays slower than time elapses
      remainingFraction = Math.pow(remainingFraction, 0.7);
      long currentBalance = Math.round(originalAmount * remainingFraction);

      String loanNumber = "MTG-" + acquisitionYear + "-" + String.format("%04d", 1000 + i * 137);
      String financingNotes = "Fixed-rate mortgage, repayment schedule per amortization table";

      UUID financingId = UUID.randomUUID();
      LocalDate loanEndDate = acquisitionDate.plusMonths(termMonths);
      String financingStatus = loanEndDate.isBefore(LocalDate.now(clock)) ? "COMPLETED" : "ACTIVE";

      financingRecords.add(
          new Object[] {
            financingId,
            newFinancingId(),
            propertyId,
            teamId,
            "MORTGAGE",
            rateType,
            LENDER_NAMES.get(entry.countryName()),
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
    }

    // === INSURANCE ===
    String provider = INSURANCE_PROVIDERS.get(entry.countryName());
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
    String authority = TAX_AUTHORITIES.get(entry.countryName());
    String taxFrequency;
    String dueMonths;
    if ("COMMERCIAL".equals(propertyCategory) || "AGRICULTURAL".equals(propertyCategory)) {
      taxFrequency = "ANNUALLY";
      dueMonths = "3";
    } else if (i % 3 == 0) {
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
          case "COMMERCIAL" -> 0.003 + random.nextDouble() * 0.003;
          case "INDUSTRIAL" -> 0.008 + random.nextDouble() * 0.007;
          case "AGRICULTURAL" -> 0.002 + random.nextDouble() * 0.003;
          // Residential: ~0.1-0.2% of value (NL OZB realism, similar across PT/ES/IT/FR/DE)
          default -> 0.001 + random.nextDouble() * 0.001;
        };
    long taxAmount = Math.max(150, Math.round(marketValue * taxRatePct));

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
    } else if ("RESIDENTIAL".equals(propertyCategory) && i % 3 == 0) {
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
        // HOA / VvE is apartment-block specific — skip for standalone houses/townhouses/villas
        String propertyType = entry.propertyType();
        boolean inSharedBuilding =
            "APARTMENT".equals(propertyType)
                || "STUDIO".equals(propertyType)
                || "ROOM".equals(propertyType);
        if (inSharedBuilding && random.nextInt(3) < 2) {
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
            "WASTE_MANAGEMENT",
            "Commercial Waste Disposal",
            Math.round(annualRent * (0.015 + random.nextDouble() * 0.015)),
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

  private String pick(String[] array) {
    return array[random.nextInt(array.length)];
  }
}
