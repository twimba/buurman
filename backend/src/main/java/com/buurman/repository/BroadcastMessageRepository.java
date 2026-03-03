package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.BROADCAST_MESSAGES;
import static com.buurman.jooq.generated.Tables.BROADCAST_MESSAGE_DISMISSALS;
import static com.buurman.jooq.generated.Tables.BROADCAST_MESSAGE_TEAMS;
import static com.buurman.jooq.generated.Tables.BROADCAST_MESSAGE_USERS;
import static com.buurman.util.UlidGenerator.newBroadcastMessageId;
import static java.time.ZoneOffset.UTC;
import static org.jooq.impl.DSL.exists;
import static org.jooq.impl.DSL.notExists;
import static org.jooq.impl.DSL.selectOne;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.BroadcastMessage;
import com.buurman.domain.Ulid;
import com.buurman.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class BroadcastMessageRepository {

  private final DSLContext dsl;
  private final Clock clock;

  public BroadcastMessage save(BroadcastMessage msg) {
    LocalDateTime now = LocalDateTime.now(clock);
    UUID id = UUID.randomUUID();
    Ulid identifier = newBroadcastMessageId();

    dsl.insertInto(BROADCAST_MESSAGES)
        .set(BROADCAST_MESSAGES.ID, id)
        .set(BROADCAST_MESSAGES.IDENTIFIER, identifier)
        .set(BROADCAST_MESSAGES.TITLE, msg.getTitle())
        .set(BROADCAST_MESSAGES.BODY, msg.getBody())
        .set(BROADCAST_MESSAGES.SEVERITY, msg.getSeverity())
        .set(BROADCAST_MESSAGES.SCOPE, msg.getScope())
        .set(BROADCAST_MESSAGES.START_AT, LocalDateTime.ofInstant(msg.getStartAt(), UTC))
        .set(
            BROADCAST_MESSAGES.END_AT,
            msg.getEndAt().map(i -> LocalDateTime.ofInstant(i, UTC)).orElse(null))
        .set(BROADCAST_MESSAGES.SHOW_ON_LOGIN, msg.isShowOnLogin())
        .set(BROADCAST_MESSAGES.SHOW_ON_REGISTER, msg.isShowOnRegister())
        .set(BROADCAST_MESSAGES.SHOW_IN_APP, msg.isShowInApp())
        .set(BROADCAST_MESSAGES.CREATED_AT, now)
        .set(BROADCAST_MESSAGES.UPDATED_AT, now)
        .set(BROADCAST_MESSAGES.CREATED_BY, msg.getCreatedBy().orElse(null))
        .set(BROADCAST_MESSAGES.UPDATED_BY, msg.getUpdatedBy().orElse(null))
        .execute();

    msg.setId(id);
    msg.setIdentifier(Optional.of(identifier));
    msg.setCreatedAt(now.toInstant(UTC));
    msg.setUpdatedAt(now.toInstant(UTC));

    insertTeamTargets(id, msg.getTargetTeamIds());
    insertUserTargets(id, msg.getTargetUserIds());

    return msg;
  }

  public BroadcastMessage update(BroadcastMessage msg) {
    LocalDateTime now = LocalDateTime.now(clock);

    dsl.update(BROADCAST_MESSAGES)
        .set(BROADCAST_MESSAGES.TITLE, msg.getTitle())
        .set(BROADCAST_MESSAGES.BODY, msg.getBody())
        .set(BROADCAST_MESSAGES.SEVERITY, msg.getSeverity())
        .set(BROADCAST_MESSAGES.SCOPE, msg.getScope())
        .set(BROADCAST_MESSAGES.START_AT, LocalDateTime.ofInstant(msg.getStartAt(), UTC))
        .set(
            BROADCAST_MESSAGES.END_AT,
            msg.getEndAt().map(i -> LocalDateTime.ofInstant(i, UTC)).orElse(null))
        .set(BROADCAST_MESSAGES.SHOW_ON_LOGIN, msg.isShowOnLogin())
        .set(BROADCAST_MESSAGES.SHOW_ON_REGISTER, msg.isShowOnRegister())
        .set(BROADCAST_MESSAGES.SHOW_IN_APP, msg.isShowInApp())
        .set(BROADCAST_MESSAGES.UPDATED_AT, now)
        .set(BROADCAST_MESSAGES.UPDATED_BY, msg.getUpdatedBy().orElse(null))
        .where(BROADCAST_MESSAGES.ID.eq(msg.getId()))
        .execute();

    msg.setUpdatedAt(now.toInstant(UTC));

    // Replace junction rows
    dsl.deleteFrom(BROADCAST_MESSAGE_TEAMS)
        .where(BROADCAST_MESSAGE_TEAMS.BROADCAST_MESSAGE_ID.eq(msg.getId()))
        .execute();
    dsl.deleteFrom(BROADCAST_MESSAGE_USERS)
        .where(BROADCAST_MESSAGE_USERS.BROADCAST_MESSAGE_ID.eq(msg.getId()))
        .execute();
    insertTeamTargets(msg.getId(), msg.getTargetTeamIds());
    insertUserTargets(msg.getId(), msg.getTargetUserIds());

    return msg;
  }

  public void delete(UUID id) {
    dsl.deleteFrom(BROADCAST_MESSAGES).where(BROADCAST_MESSAGES.ID.eq(id)).execute();
  }

  public Optional<BroadcastMessage> findByIdentifier(Ulid identifier) {
    return dsl.selectFrom(BROADCAST_MESSAGES)
        .where(BROADCAST_MESSAGES.IDENTIFIER.eq(identifier))
        .fetchOptional()
        .map(this::toDomain)
        .map(
            msg -> {
              msg.setTargetTeamIds(loadTeamTargets(msg.getId()));
              msg.setTargetUserIds(loadUserTargets(msg.getId()));
              return msg;
            });
  }

  public BroadcastMessage getByIdentifier(Ulid identifier) {
    return findByIdentifier(identifier)
        .orElseThrow(() -> new NotFoundException("Broadcast message not found"));
  }

  public List<BroadcastMessage> findAll() {
    List<BroadcastMessage> messages =
        dsl.selectFrom(BROADCAST_MESSAGES)
            .orderBy(BROADCAST_MESSAGES.CREATED_AT.desc())
            .fetch()
            .map(this::toDomain);

    if (!messages.isEmpty()) {
      loadTargets(messages);
    }
    return messages;
  }

  public List<BroadcastMessage> findActiveForUser(UUID userId, Optional<UUID> teamId) {
    LocalDateTime now = LocalDateTime.now(clock);

    // Scope filter: GLOBAL, or TEAMS with matching team, or USERS with matching user
    var scopeCondition =
        BROADCAST_MESSAGES
            .SCOPE
            .eq("GLOBAL")
            .or(
                BROADCAST_MESSAGES
                    .SCOPE
                    .eq("USERS")
                    .and(
                        exists(
                            selectOne()
                                .from(BROADCAST_MESSAGE_USERS)
                                .where(
                                    BROADCAST_MESSAGE_USERS.BROADCAST_MESSAGE_ID.eq(
                                        BROADCAST_MESSAGES.ID))
                                .and(BROADCAST_MESSAGE_USERS.USER_ID.eq(userId)))));

    if (teamId.isPresent()) {
      scopeCondition =
          scopeCondition.or(
              BROADCAST_MESSAGES
                  .SCOPE
                  .eq("TEAMS")
                  .and(
                      exists(
                          selectOne()
                              .from(BROADCAST_MESSAGE_TEAMS)
                              .where(
                                  BROADCAST_MESSAGE_TEAMS.BROADCAST_MESSAGE_ID.eq(
                                      BROADCAST_MESSAGES.ID))
                              .and(BROADCAST_MESSAGE_TEAMS.TEAM_ID.eq(teamId.get())))));
    }

    return dsl.selectFrom(BROADCAST_MESSAGES)
        .where(BROADCAST_MESSAGES.SHOW_IN_APP.isTrue())
        .and(BROADCAST_MESSAGES.START_AT.le(now))
        .and(BROADCAST_MESSAGES.END_AT.isNull().or(BROADCAST_MESSAGES.END_AT.gt(now)))
        .and(scopeCondition)
        .and(
            notExists(
                selectOne()
                    .from(BROADCAST_MESSAGE_DISMISSALS)
                    .where(
                        BROADCAST_MESSAGE_DISMISSALS.BROADCAST_MESSAGE_ID.eq(BROADCAST_MESSAGES.ID))
                    .and(BROADCAST_MESSAGE_DISMISSALS.USER_ID.eq(userId))))
        .orderBy(BROADCAST_MESSAGES.START_AT.desc())
        .fetch()
        .map(this::toDomain);
  }

  public List<BroadcastMessage> findActivePublic(boolean login, boolean register) {
    LocalDateTime now = LocalDateTime.now(clock);

    var condition =
        BROADCAST_MESSAGES
            .START_AT
            .le(now)
            .and(BROADCAST_MESSAGES.END_AT.isNull().or(BROADCAST_MESSAGES.END_AT.gt(now)))
            .and(BROADCAST_MESSAGES.SCOPE.eq("GLOBAL"));

    if (login && register) {
      condition =
          condition.and(
              BROADCAST_MESSAGES
                  .SHOW_ON_LOGIN
                  .isTrue()
                  .or(BROADCAST_MESSAGES.SHOW_ON_REGISTER.isTrue()));
    } else if (login) {
      condition = condition.and(BROADCAST_MESSAGES.SHOW_ON_LOGIN.isTrue());
    } else if (register) {
      condition = condition.and(BROADCAST_MESSAGES.SHOW_ON_REGISTER.isTrue());
    }

    return dsl.selectFrom(BROADCAST_MESSAGES)
        .where(condition)
        .orderBy(BROADCAST_MESSAGES.START_AT.desc())
        .fetch()
        .map(this::toDomain);
  }

  private BroadcastMessage toDomain(org.jooq.Record record) {
    var r = record.into(BROADCAST_MESSAGES);
    return BroadcastMessage.builder()
        .id(r.getId())
        .identifier(Optional.of(r.getIdentifier()))
        .title(r.getTitle())
        .body(r.getBody())
        .severity(r.getSeverity())
        .scope(r.getScope())
        .startAt(r.getStartAt().toInstant(UTC))
        .endAt(Optional.ofNullable(r.getEndAt()).map(dt -> dt.toInstant(UTC)))
        .showOnLogin(r.getShowOnLogin())
        .showOnRegister(r.getShowOnRegister())
        .showInApp(r.getShowInApp())
        .createdAt(r.getCreatedAt().toInstant(UTC))
        .updatedAt(r.getUpdatedAt().toInstant(UTC))
        .createdBy(Optional.ofNullable(r.getCreatedBy()))
        .updatedBy(Optional.ofNullable(r.getUpdatedBy()))
        .build();
  }

  private void insertTeamTargets(UUID broadcastMessageId, List<UUID> teamIds) {
    if (teamIds == null || teamIds.isEmpty()) {
      return;
    }
    for (UUID teamId : teamIds) {
      dsl.insertInto(BROADCAST_MESSAGE_TEAMS)
          .set(BROADCAST_MESSAGE_TEAMS.BROADCAST_MESSAGE_ID, broadcastMessageId)
          .set(BROADCAST_MESSAGE_TEAMS.TEAM_ID, teamId)
          .execute();
    }
  }

  private void insertUserTargets(UUID broadcastMessageId, List<UUID> userIds) {
    if (userIds == null || userIds.isEmpty()) {
      return;
    }
    for (UUID userId : userIds) {
      dsl.insertInto(BROADCAST_MESSAGE_USERS)
          .set(BROADCAST_MESSAGE_USERS.BROADCAST_MESSAGE_ID, broadcastMessageId)
          .set(BROADCAST_MESSAGE_USERS.USER_ID, userId)
          .execute();
    }
  }

  private List<UUID> loadTeamTargets(UUID broadcastMessageId) {
    return dsl.select(BROADCAST_MESSAGE_TEAMS.TEAM_ID)
        .from(BROADCAST_MESSAGE_TEAMS)
        .where(BROADCAST_MESSAGE_TEAMS.BROADCAST_MESSAGE_ID.eq(broadcastMessageId))
        .fetch(BROADCAST_MESSAGE_TEAMS.TEAM_ID);
  }

  private List<UUID> loadUserTargets(UUID broadcastMessageId) {
    return dsl.select(BROADCAST_MESSAGE_USERS.USER_ID)
        .from(BROADCAST_MESSAGE_USERS)
        .where(BROADCAST_MESSAGE_USERS.BROADCAST_MESSAGE_ID.eq(broadcastMessageId))
        .fetch(BROADCAST_MESSAGE_USERS.USER_ID);
  }

  /** Batch-load targets for a list of messages (avoids N+1). */
  private void loadTargets(List<BroadcastMessage> messages) {
    List<UUID> ids = messages.stream().map(BroadcastMessage::getId).toList();

    Map<UUID, List<UUID>> teamTargets =
        dsl
            .select(BROADCAST_MESSAGE_TEAMS.BROADCAST_MESSAGE_ID, BROADCAST_MESSAGE_TEAMS.TEAM_ID)
            .from(BROADCAST_MESSAGE_TEAMS)
            .where(BROADCAST_MESSAGE_TEAMS.BROADCAST_MESSAGE_ID.in(ids))
            .fetch()
            .stream()
            .collect(
                Collectors.groupingBy(
                    r -> r.get(BROADCAST_MESSAGE_TEAMS.BROADCAST_MESSAGE_ID),
                    Collectors.mapping(
                        r -> r.get(BROADCAST_MESSAGE_TEAMS.TEAM_ID), Collectors.toList())));

    Map<UUID, List<UUID>> userTargets =
        dsl
            .select(BROADCAST_MESSAGE_USERS.BROADCAST_MESSAGE_ID, BROADCAST_MESSAGE_USERS.USER_ID)
            .from(BROADCAST_MESSAGE_USERS)
            .where(BROADCAST_MESSAGE_USERS.BROADCAST_MESSAGE_ID.in(ids))
            .fetch()
            .stream()
            .collect(
                Collectors.groupingBy(
                    r -> r.get(BROADCAST_MESSAGE_USERS.BROADCAST_MESSAGE_ID),
                    Collectors.mapping(
                        r -> r.get(BROADCAST_MESSAGE_USERS.USER_ID), Collectors.toList())));

    for (BroadcastMessage msg : messages) {
      msg.setTargetTeamIds(teamTargets.getOrDefault(msg.getId(), List.of()));
      msg.setTargetUserIds(userTargets.getOrDefault(msg.getId(), List.of()));
    }
  }
}
