package com.buurman.service.demo;

import static com.buurman.jooq.generated.Tables.TEAMS;
import static com.buurman.util.UlidGenerator.newTeamId;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class DemoTeamGenerator {

  private final DSLContext dsl;
  private final Clock clock;

  // teamKey -> display name
  public static final Map<String, String> TEAMS_MAP =
      Map.of(
          "demo-team", "Demo Team",
          "team-alpha", "Team Alpha Property Management",
          "team-beta", "Team Beta Rentals");

  public void generate(DemoDataContext ctx) {
    LocalDateTime now = LocalDateTime.now(clock);

    for (var entry : TEAMS_MAP.entrySet()) {
      UUID teamId = UUID.randomUUID();
      String identifier = newTeamId().value();

      String settingsJson =
          """
          {"demoData": true, "autoPaymentGeneration": {"enabled": true, "daysBefore": 30}}
          """
              .trim();

      dsl.insertInto(TEAMS)
          .set(TEAMS.ID, teamId)
          .set(TEAMS.IDENTIFIER, identifier)
          .set(TEAMS.NAME, entry.getValue())
          .set(TEAMS.SETTINGS, JSONB.jsonb(settingsJson))
          .set(TEAMS.CREATED_AT, now)
          .set(TEAMS.UPDATED_AT, now)
          .execute();

      ctx.getTeamIds().put(entry.getKey(), teamId);
      ctx.putIdentifier(teamId, identifier);
      ctx.incrementTeams();
      log.info("Created demo team: {} ({})", entry.getValue(), teamId);
    }
  }
}
