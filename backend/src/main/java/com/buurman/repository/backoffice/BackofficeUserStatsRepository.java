package com.buurman.repository.backoffice;

import static com.buurman.jooq.generated.Tables.TEAMS;
import static com.buurman.jooq.generated.Tables.TEAM_MEMBERS;
import static org.jooq.impl.DSL.count;

import java.util.Map;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class BackofficeUserStatsRepository {

  private final DSLContext dsl;

  public Map<UUID, Integer> countTeamsPerUser() {
    return dsl.select(TEAM_MEMBERS.USER_ID, count())
        .from(TEAM_MEMBERS)
        .where(TEAM_MEMBERS.DELETED_AT.isNull())
        .groupBy(TEAM_MEMBERS.USER_ID)
        .fetchMap(TEAM_MEMBERS.USER_ID, count());
  }

  public Map<UUID, Integer> countDemoTeamsPerUser() {
    return dsl.select(TEAM_MEMBERS.USER_ID, count())
        .from(TEAM_MEMBERS)
        .join(TEAMS)
        .on(TEAMS.ID.eq(TEAM_MEMBERS.TEAM_ID))
        .where(
            TEAM_MEMBERS
                .DELETED_AT
                .isNull()
                .and(TEAMS.DELETED_AT.isNull())
                .and(TEAMS.DEMO.isTrue()))
        .groupBy(TEAM_MEMBERS.USER_ID)
        .fetchMap(TEAM_MEMBERS.USER_ID, count());
  }

  public long countTeamsForUser(UUID userId) {
    Long result =
        dsl.selectCount()
            .from(TEAM_MEMBERS)
            .where(TEAM_MEMBERS.USER_ID.eq(userId).and(TEAM_MEMBERS.DELETED_AT.isNull()))
            .fetchOne(0, Long.class);
    return result != null ? result : 0L;
  }

  public long countDemoTeamsForUser(UUID userId) {
    Long result =
        dsl.selectCount()
            .from(TEAM_MEMBERS)
            .join(TEAMS)
            .on(TEAMS.ID.eq(TEAM_MEMBERS.TEAM_ID))
            .where(
                TEAM_MEMBERS
                    .USER_ID
                    .eq(userId)
                    .and(TEAM_MEMBERS.DELETED_AT.isNull())
                    .and(TEAMS.DELETED_AT.isNull())
                    .and(TEAMS.DEMO.isTrue()))
            .fetchOne(0, Long.class);
    return result != null ? result : 0L;
  }
}
