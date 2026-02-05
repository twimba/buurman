package com.buurman.service.demo;

import com.buurman.util.EntityPrefix;
import com.buurman.util.UlidGenerator;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static com.buurman.jooq.generated.Tables.TEAMS;

@Component
public class DemoTeamGenerator {

    private static final Logger log = LoggerFactory.getLogger(DemoTeamGenerator.class);

    private final DSLContext dsl;

    // teamKey -> display name
    public static final Map<String, String> TEAMS_MAP = Map.of(
            "demo-team", "Demo Team",
            "team-alpha", "Team Alpha Property Management",
            "team-beta", "Team Beta Rentals"
    );

    public DemoTeamGenerator(DSLContext dsl) {
        this.dsl = dsl;
    }

    public void generate(DemoDataContext ctx) {
        LocalDateTime now = LocalDateTime.now();

        for (var entry : TEAMS_MAP.entrySet()) {
            UUID teamId = UUID.randomUUID();
            String identifier = UlidGenerator.generate(EntityPrefix.TEA);

            String settingsJson = """
                    {"demoData": true, "autoPaymentGeneration": {"enabled": true, "daysBefore": 30}}
                    """.trim();

            dsl.insertInto(TEAMS)
                    .set(TEAMS.ID, teamId)
                    .set(TEAMS.IDENTIFIER, identifier)
                    .set(TEAMS.NAME, entry.getValue())
                    .set(TEAMS.SETTINGS, JSONB.jsonb(settingsJson))
                    .set(TEAMS.CREATED_AT, now)
                    .set(TEAMS.UPDATED_AT, now)
                    .execute();

            ctx.getTeamIds().put(entry.getKey(), teamId);
            ctx.incrementTeams();
            log.info("Created demo team: {} ({})", entry.getValue(), teamId);
        }
    }
}
