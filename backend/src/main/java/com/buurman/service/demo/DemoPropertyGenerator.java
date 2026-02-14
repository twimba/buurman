package com.buurman.service.demo;

import com.buurman.util.EntityPrefix;
import com.buurman.util.UlidGenerator;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static com.buurman.jooq.generated.Tables.PROPERTIES;
import static com.buurman.jooq.generated.Tables.PROPERTY_OUTDOOR_AREAS;

@Component
public class DemoPropertyGenerator {

    private static final Logger log = LoggerFactory.getLogger(DemoPropertyGenerator.class);

    private final DSLContext dsl;
    private final Random random = new Random(42);

    private static final String[] PROPERTY_TYPES = {"APARTMENT", "HOUSE", "STUDIO", "COMMERCIAL"};
    private static final String[] CONSTRUCTION_TYPES = {"BRICK", "CONCRETE", "WOOD", "MIXED"};
    private static final String[] FOUNDATION_TYPES = {"CONCRETE_SLAB", "CRAWL_SPACE", "BASEMENT", "PILE"};
    private static final String[] ROOF_TYPES = {"FLAT", "PITCHED", "HIP", "GABLE"};
    private static final String[] FLOORING_TYPES = {"HARDWOOD", "LAMINATE", "TILE", "VINYL"};
    private static final String[] WINDOW_TYPES = {"SINGLE_PANE", "DOUBLE_PANE", "TRIPLE_PANE"};
    private static final String[] HEATING_TYPES = {"CENTRAL", "DISTRICT", "HEAT_PUMP", "GAS"};
    private static final String[] ENERGY_RATINGS = {"A+", "A", "B", "C", "D"};
    private static final String[] INTERNET_TYPES = {"FIBER", "CABLE", "DSL"};
    private static final String[] PARKING_TYPES = {"GARAGE", "STREET", "UNDERGROUND", "NONE"};
    private static final String[] OUTDOOR_TYPES = {"BALCONY", "TERRACE", "GARDEN"};

    // teamKey -> number of properties
    public static final Map<String, Integer> PROPERTIES_PER_TEAM = Map.of(
            "demo-team", 8,
            "team-alpha", 6,
            "team-beta", 5
    );

    // Country data: country -> (cities, streets, postalCodeFormat, latRange, lonRange)
    private static final List<CountryData> COUNTRIES = List.of(
            new CountryData("Netherlands",
                    new String[]{"Amsterdam", "Rotterdam", "Den Haag", "Utrecht", "Eindhoven", "Leiden", "Haarlem", "Delft"},
                    new String[]{"Keizersgracht", "Prinsengracht", "Herengracht", "Vondelstraat", "Beethovenstraat", "Apollolaan", "Singel", "Overtoom"},
                    "####_AA", 51.8, 53.0, 4.0, 6.0),
            new CountryData("Germany",
                    new String[]{"Berlin", "Munich", "Hamburg", "Frankfurt", "Cologne", "Stuttgart", "Düsseldorf", "Dresden"},
                    new String[]{"Friedrichstraße", "Kurfürstendamm", "Schillerstraße", "Goethestraße", "Berliner Straße", "Hauptstraße", "Bahnhofstraße", "Mozartstraße"},
                    "#####", 48.0, 54.0, 6.0, 14.0),
            new CountryData("United Kingdom",
                    new String[]{"London", "Manchester", "Birmingham", "Edinburgh", "Bristol", "Liverpool", "Oxford", "Cambridge"},
                    new String[]{"Baker Street", "King's Road", "Church Lane", "High Street", "Park Avenue", "Victoria Road", "Station Road", "Mill Lane"},
                    "AA## #AA", 51.0, 56.0, -4.0, 1.5),
            new CountryData("France",
                    new String[]{"Paris", "Lyon", "Marseille", "Bordeaux", "Nice", "Toulouse", "Strasbourg", "Nantes"},
                    new String[]{"Rue de Rivoli", "Avenue des Champs-Élysées", "Boulevard Saint-Germain", "Rue de la Paix", "Avenue Montaigne", "Rue du Faubourg", "Place Vendôme", "Rue de Seine"},
                    "#####", 43.0, 49.0, -1.0, 7.0),
            new CountryData("Spain",
                    new String[]{"Madrid", "Barcelona", "Valencia", "Seville", "Málaga", "Bilbao", "Granada", "San Sebastián"},
                    new String[]{"Calle Gran Vía", "Paseo de la Castellana", "Avenida Diagonal", "Calle Mayor", "Calle de Alcalá", "Rambla de Catalunya", "Calle Serrano", "Paseo del Prado"},
                    "#####", 36.0, 43.5, -6.0, 3.0),
            new CountryData("Portugal",
                    new String[]{"Lisbon", "Porto", "Faro", "Coimbra", "Braga", "Funchal", "Aveiro", "Évora"},
                    new String[]{"Rua Augusta", "Avenida da Liberdade", "Rua de Santa Catarina", "Rua do Carmo", "Praça do Comércio", "Rua dos Clérigos", "Avenida dos Aliados", "Rua da Prata"},
                    "####-###", 37.0, 42.0, -9.5, -6.0)
    );

    record CountryData(String name, String[] cities, String[] streets, String postalFormat,
                        double latMin, double latMax, double lonMin, double lonMax) {}

    private final Clock clock;

    public DemoPropertyGenerator(DSLContext dsl, Clock clock) {
        this.dsl = dsl;
        this.clock = clock;
    }

    public void generate(DemoDataContext ctx) {
        LocalDateTime now = LocalDateTime.now(clock);

        for (var entry : PROPERTIES_PER_TEAM.entrySet()) {
            String teamKey = entry.getKey();
            int count = entry.getValue();
            UUID teamId = ctx.getTeamIds().get(teamKey);
            UUID createdBy = ctx.getAdminUserForTeam(teamKey);
            List<UUID> propertyIds = new ArrayList<>();

            for (int i = 0; i < count; i++) {
                UUID propertyId = UUID.randomUUID();
                String propertyType = PROPERTY_TYPES[i % PROPERTY_TYPES.length];
                String status = i < count - 1 ? "OCCUPIED" : "VACANT";

                // Pick a country — cycle through them for variety
                CountryData country = COUNTRIES.get(i % COUNTRIES.size());

                int houseNumber = random.nextInt(1, 200);
                String street = pick(country.streets()) + " " + houseNumber;
                String city = pick(country.cities());
                String postalCode = generatePostalCode(country.postalFormat());

                double lat = country.latMin() + random.nextDouble() * (country.latMax() - country.latMin());
                double lon = country.lonMin() + random.nextDouble() * (country.lonMax() - country.lonMin());

                int yearBuilt = random.nextInt(1920, 2020);
                int bedrooms = switch (propertyType) {
                    case "STUDIO" -> 1;
                    case "APARTMENT" -> random.nextInt(1, 4);
                    case "HOUSE" -> random.nextInt(2, 6);
                    case "COMMERCIAL" -> 0;
                    default -> 2;
                };
                int bathrooms = Math.max(1, bedrooms / 2 + 1);
                BigDecimal area = BigDecimal.valueOf(switch (propertyType) {
                    case "STUDIO" -> random.nextInt(25, 50);
                    case "APARTMENT" -> random.nextInt(50, 120);
                    case "HOUSE" -> random.nextInt(80, 250);
                    case "COMMERCIAL" -> random.nextInt(50, 300);
                    default -> 75;
                });

                dsl.insertInto(PROPERTIES)
                        .set(PROPERTIES.ID, propertyId)
                        .set(PROPERTIES.IDENTIFIER, UlidGenerator.generate(EntityPrefix.PRO))
                        .set(PROPERTIES.TEAM_ID, teamId)
                        .set(PROPERTIES.STREET, street)
                        .set(PROPERTIES.CITY, city)
                        .set(PROPERTIES.POSTAL_CODE, postalCode)
                        .set(PROPERTIES.COUNTRY, country.name())
                        .set(PROPERTIES.LATITUDE, BigDecimal.valueOf(lat))
                        .set(PROPERTIES.LONGITUDE, BigDecimal.valueOf(lon))
                        .set(PROPERTIES.BEDROOMS, bedrooms)
                        .set(PROPERTIES.BATHROOMS, bathrooms)
                        .set(PROPERTIES.AREA_VALUE, area)
                        .set(PROPERTIES.AREA_UNIT, "sqm")
                        .set(PROPERTIES.PROPERTY_TYPE, propertyType)
                        .set(PROPERTIES.STATUS, status)
                        .set(PROPERTIES.YEAR_BUILT, yearBuilt)
                        .set(PROPERTIES.YEAR_LAST_RENOVATED, yearBuilt < 2000 ? yearBuilt + random.nextInt(5, 30) : null)
                        .set(PROPERTIES.CONSTRUCTION_TYPE, pick(CONSTRUCTION_TYPES))
                        .set(PROPERTIES.FOUNDATION_TYPE, pick(FOUNDATION_TYPES))
                        .set(PROPERTIES.ROOF_TYPE, pick(ROOF_TYPES))
                        .set(PROPERTIES.FLOORING_TYPE, pick(FLOORING_TYPES))
                        .set(PROPERTIES.WINDOW_TYPE, pick(WINDOW_TYPES))
                        .set(PROPERTIES.NUMBER_OF_FLOORS, random.nextInt(1, 4))
                        .set(PROPERTIES.ENERGY_EFFICIENCY_RATING, pick(ENERGY_RATINGS))
                        .set(PROPERTIES.ENERGY_CERTIFICATE_EXPIRY_DATE, LocalDate.now(clock).plusYears(random.nextInt(1, 5)))
                        .set(PROPERTIES.HEATING_TYPE, pick(HEATING_TYPES))
                        .set(PROPERTIES.COOLING_TYPE, random.nextBoolean() ? "CENTRAL_AC" : "NONE")
                        .set(PROPERTIES.HOT_WATER_SYSTEM, "BOILER")
                        .set(PROPERTIES.ELECTRICITY_CONNECTION_TYPE, "MUNICIPAL")
                        .set(PROPERTIES.ELECTRICITY_CAPACITY_AMPS, random.nextBoolean() ? 25 : 35)
                        .set(PROPERTIES.WATER_CONNECTION_TYPE, "MUNICIPAL")
                        .set(PROPERTIES.HAS_GAS_CONNECTION, random.nextBoolean())
                        .set(PROPERTIES.SEWAGE_TYPE, "MUNICIPAL")
                        .set(PROPERTIES.INTERNET_CONNECTION_TYPE, pick(INTERNET_TYPES))
                        .set(PROPERTIES.INTERNET_MAX_SPEED_MBPS, random.nextBoolean() ? 500 : 1000)
                        .set(PROPERTIES.INTERNET_STATUS, "ACTIVE")
                        .set(PROPERTIES.PARKING_SPACES, random.nextInt(0, 3))
                        .set(PROPERTIES.PARKING_TYPE, pick(PARKING_TYPES))
                        .set(PROPERTIES.HAS_SMOKE_DETECTORS, true)
                        .set(PROPERTIES.HAS_CO_DETECTORS, random.nextBoolean())
                        .set(PROPERTIES.HAS_FIRE_EXTINGUISHER, random.nextBoolean())
                        .set(PROPERTIES.HAS_SPRINKLER_SYSTEM, false)
                        .set(PROPERTIES.HAS_ALARM_SYSTEM, random.nextBoolean())
                        .set(PROPERTIES.HAS_SECURITY_CAMERAS, false)
                        .set(PROPERTIES.HAS_SECURE_ENTRY, random.nextBoolean())
                        .set(PROPERTIES.IS_WHEELCHAIR_ACCESSIBLE, random.nextInt(5) == 0)
                        .set(PROPERTIES.HAS_ELEVATOR, "APARTMENT".equals(propertyType) && random.nextBoolean())
                        .set(PROPERTIES.HAS_STEP_FREE_ENTRANCE, random.nextInt(3) == 0)
                        .set(PROPERTIES.HAS_ADAPTED_BATHROOM, false)
                        .set(PROPERTIES.CREATED_AT, now.minusDays(random.nextInt(30, 365)))
                        .set(PROPERTIES.UPDATED_AT, now)
                        .set(PROPERTIES.CREATED_BY, createdBy)
                        .set(PROPERTIES.UPDATED_BY, createdBy)
                        .execute();

                propertyIds.add(propertyId);
                ctx.incrementProperties();

                // Add outdoor areas for some properties
                if (random.nextBoolean()) {
                    int outdoorCount = random.nextInt(1, 3);
                    Set<String> usedTypes = new HashSet<>();
                    for (int j = 0; j < outdoorCount; j++) {
                        String outdoorType = pick(OUTDOOR_TYPES);
                        if (usedTypes.add(outdoorType)) {
                            dsl.insertInto(PROPERTY_OUTDOOR_AREAS)
                                    .set(PROPERTY_OUTDOOR_AREAS.ID, UUID.randomUUID())
                                    .set(PROPERTY_OUTDOOR_AREAS.IDENTIFIER, UlidGenerator.generate(EntityPrefix.POA))
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
            log.info("Created {} properties for team {}", count, teamKey);
        }
    }

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
