package com.buurman.service.demo;

import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.buurman.jooq.generated.Tables.TEAM_MEMBERS;

@Component
public class DemoTeamMemberGenerator {

    private static final Logger log = LoggerFactory.getLogger(DemoTeamMemberGenerator.class);

    private final DSLContext dsl;

    public DemoTeamMemberGenerator(DSLContext dsl) {
        this.dsl = dsl;
    }

    public void generate(DemoDataContext ctx) {
        LocalDateTime now = LocalDateTime.now();

        for (DemoUsers.DemoUser user : DemoUsers.ALL_USERS) {
            UUID userId = ctx.getUserIds().get(user.email());

            for (var entry : user.teamRoles().entrySet()) {
                String teamKey = entry.getKey();
                DemoUsers.TeamRole teamRole = entry.getValue();
                UUID teamId = ctx.getTeamIds().get(teamKey);

                dsl.insertInto(TEAM_MEMBERS)
                        .set(TEAM_MEMBERS.ID, UUID.randomUUID())
                        .set(TEAM_MEMBERS.TEAM_ID, teamId)
                        .set(TEAM_MEMBERS.USER_ID, userId)
                        .set(TEAM_MEMBERS.ROLE, teamRole.role())
                        .set(TEAM_MEMBERS.IS_OWNER, teamRole.isOwner())
                        .set(TEAM_MEMBERS.INVITED_AT, now)
                        .set(TEAM_MEMBERS.JOINED_AT, now)
                        .set(TEAM_MEMBERS.INVITED_BY, userId)
                        .execute();

                log.debug("Added {} to {} as {} (owner={})",
                        user.email(), teamKey, teamRole.role(), teamRole.isOwner());
            }
        }
    }
}
