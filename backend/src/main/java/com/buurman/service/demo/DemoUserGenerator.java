package com.buurman.service.demo;

import static com.buurman.jooq.generated.Tables.USERS;
import static com.buurman.util.UlidGenerator.newUserId;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class DemoUserGenerator {

  private final DSLContext dsl;
  private final Clock clock;

  public void generate(DemoDataContext ctx) {
    LocalDateTime now = LocalDateTime.now(clock);

    for (DemoUsers.DemoUser user : DemoUsers.ALL_USERS) {
      UUID userId = UUID.randomUUID();
      String identifier = newUserId().value();
      String keycloakId = ctx.getKeycloakIds().get(user.email());

      dsl.insertInto(USERS)
          .set(USERS.ID, userId)
          .set(USERS.IDENTIFIER, identifier)
          .set(USERS.KEYCLOAK_ID, keycloakId)
          .set(USERS.EMAIL, user.email())
          .set(USERS.FIRST_NAME, user.firstName())
          .set(USERS.LAST_NAME, user.lastName())
          .set(USERS.EMAIL_VERIFIED_AT, now)
          .set(USERS.CREATED_AT, now)
          .set(USERS.UPDATED_AT, now)
          .execute();

      ctx.getUserIds().put(user.email(), userId);
      ctx.incrementUsers();
      log.info("Created demo user: {}", user.email());
    }

    // Set default/active team for each user (prefer the team where they are admin/owner)
    for (DemoUsers.DemoUser user : DemoUsers.ALL_USERS) {
      UUID userId = ctx.getUserIds().get(user.email());
      String defaultTeamKey =
          user.teamRoles().entrySet().stream()
              .filter(e -> e.getValue().isOwner() || "TEAM_ADMIN".equals(e.getValue().role()))
              .map(java.util.Map.Entry::getKey)
              .findFirst()
              .orElse(user.teamRoles().keySet().iterator().next());
      UUID defaultTeamId = ctx.getTeamIds().get(defaultTeamKey);

      dsl.update(USERS)
          .set(USERS.DEFAULT_TEAM_ID, defaultTeamId)
          .set(USERS.ACTIVE_TEAM_ID, defaultTeamId)
          .where(USERS.ID.eq(userId))
          .execute();
    }
  }
}
