package com.buurman.service.demo;

import com.buurman.util.EntityPrefix;
import com.buurman.util.UlidGenerator;
import net.datafaker.Faker;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

import static com.buurman.jooq.generated.Tables.TENANTS;
import static com.buurman.jooq.generated.Tables.TENANT_ADDRESSES;

@Component
public class DemoTenantGenerator {

    private static final Logger log = LoggerFactory.getLogger(DemoTenantGenerator.class);

    private final DSLContext dsl;
    private final Clock clock;
    private final Faker faker = new Faker(Locale.ENGLISH, new Random(42));
    private final Random random = new Random(42);

    // teamKey -> number of tenants
    public static final Map<String, Integer> TENANTS_PER_TEAM = Map.of(
            "demo-team", 10,
            "team-alpha", 8,
            "team-beta", 6
    );

    private static final String[] DUTCH_CITIES = {
            "Amsterdam", "Rotterdam", "Den Haag", "Utrecht", "Eindhoven",
            "Tilburg", "Groningen", "Almere", "Breda", "Nijmegen"
    };

    public DemoTenantGenerator(DSLContext dsl, Clock clock) {
        this.dsl = dsl;
        this.clock = clock;
    }

    public void generate(DemoDataContext ctx) {
        LocalDateTime now = LocalDateTime.now(clock);

        for (var entry : TENANTS_PER_TEAM.entrySet()) {
            String teamKey = entry.getKey();
            int count = entry.getValue();
            UUID teamId = ctx.getTeamIds().get(teamKey);
            UUID createdBy = ctx.getAdminUserForTeam(teamKey);
            List<UUID> tenantIds = new ArrayList<>();

            for (int i = 0; i < count; i++) {
                UUID tenantId = UUID.randomUUID();
                String firstName = faker.name().firstName();
                String lastName = faker.name().lastName();
                String email = (firstName.toLowerCase() + "." + lastName.toLowerCase()
                        + "." + teamKey.replace("-", "") + i + "@example.com")
                        .replaceAll("[^a-z0-9.@]", "");

                dsl.insertInto(TENANTS)
                        .set(TENANTS.ID, tenantId)
                        .set(TENANTS.IDENTIFIER, UlidGenerator.generate(EntityPrefix.TEN))
                        .set(TENANTS.TEAM_ID, teamId)
                        .set(TENANTS.FIRST_NAME, firstName)
                        .set(TENANTS.LAST_NAME, lastName)
                        .set(TENANTS.EMAIL, email)
                        .set(TENANTS.PHONE, "+316" + String.format("%08d", random.nextInt(10000000, 99999999)))
                        .set(TENANTS.TAX_NUMBER, "NL" + String.format("%09d", random.nextInt(100000000, 999999999)) + "B01")
                        .set(TENANTS.ID_NUMBER, String.format("%09d", random.nextInt(100000000, 999999999)))
                        .set(TENANTS.CREATED_AT, now.minusDays(random.nextInt(30, 365)))
                        .set(TENANTS.UPDATED_AT, now)
                        .set(TENANTS.CREATED_BY, createdBy)
                        .set(TENANTS.UPDATED_BY, createdBy)
                        .execute();

                // Add CURRENT address for each tenant (one active per tenant allowed)
                String city = DUTCH_CITIES[random.nextInt(DUTCH_CITIES.length)];
                int houseNum = random.nextInt(1, 200);
                String postalCode = String.format("%04d %s", random.nextInt(1000, 9999),
                        "" + (char) ('A' + random.nextInt(26)) + (char) ('A' + random.nextInt(26)));

                dsl.insertInto(TENANT_ADDRESSES)
                        .set(TENANT_ADDRESSES.ID, UUID.randomUUID())
                        .set(TENANT_ADDRESSES.IDENTIFIER, UlidGenerator.generate(EntityPrefix.TAD))
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
                ctx.incrementTenants();
            }

            ctx.getTenantIdsByTeam().put(teamId, tenantIds);
            log.info("Created {} tenants for team {}", count, teamKey);
        }
    }
}
