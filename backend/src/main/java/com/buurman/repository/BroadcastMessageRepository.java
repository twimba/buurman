package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.BROADCAST_MESSAGES;
import static com.buurman.jooq.generated.Tables.BROADCAST_MESSAGE_DISMISSALS;
import static com.buurman.util.UlidGenerator.newBroadcastMessageId;
import static java.time.ZoneOffset.UTC;
import static org.jooq.impl.DSL.notExists;
import static org.jooq.impl.DSL.selectOne;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.BroadcastMessage;
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
    String identifier = newBroadcastMessageId().value();

    dsl.insertInto(BROADCAST_MESSAGES)
        .set(BROADCAST_MESSAGES.ID, id)
        .set(BROADCAST_MESSAGES.IDENTIFIER, identifier)
        .set(BROADCAST_MESSAGES.TITLE, msg.getTitle())
        .set(BROADCAST_MESSAGES.BODY, msg.getBody())
        .set(BROADCAST_MESSAGES.SEVERITY, msg.getSeverity())
        .set(BROADCAST_MESSAGES.START_AT, LocalDateTime.ofInstant(msg.getStartAt(), UTC))
        .set(BROADCAST_MESSAGES.END_AT, msg.getEndAt().map(i -> LocalDateTime.ofInstant(i, UTC)).orElse(null))
        .set(BROADCAST_MESSAGES.SHOW_ON_LOGIN, msg.isShowOnLogin())
        .set(BROADCAST_MESSAGES.SHOW_ON_REGISTER, msg.isShowOnRegister())
        .set(BROADCAST_MESSAGES.SHOW_IN_APP, msg.isShowInApp())
        .set(BROADCAST_MESSAGES.CREATED_AT, now)
        .set(BROADCAST_MESSAGES.UPDATED_AT, now)
        .set(BROADCAST_MESSAGES.CREATED_BY, msg.getCreatedBy().orElse(null))
        .set(BROADCAST_MESSAGES.UPDATED_BY, msg.getUpdatedBy().orElse(null))
        .execute();

    msg.setId(id);
    msg.setIdentifier(identifier);
    msg.setCreatedAt(now.toInstant(UTC));
    msg.setUpdatedAt(now.toInstant(UTC));

    return msg;
  }

  public BroadcastMessage update(BroadcastMessage msg) {
    LocalDateTime now = LocalDateTime.now(clock);

    dsl.update(BROADCAST_MESSAGES)
        .set(BROADCAST_MESSAGES.TITLE, msg.getTitle())
        .set(BROADCAST_MESSAGES.BODY, msg.getBody())
        .set(BROADCAST_MESSAGES.SEVERITY, msg.getSeverity())
        .set(BROADCAST_MESSAGES.START_AT, LocalDateTime.ofInstant(msg.getStartAt(), UTC))
        .set(BROADCAST_MESSAGES.END_AT, msg.getEndAt().map(i -> LocalDateTime.ofInstant(i, UTC)).orElse(null))
        .set(BROADCAST_MESSAGES.SHOW_ON_LOGIN, msg.isShowOnLogin())
        .set(BROADCAST_MESSAGES.SHOW_ON_REGISTER, msg.isShowOnRegister())
        .set(BROADCAST_MESSAGES.SHOW_IN_APP, msg.isShowInApp())
        .set(BROADCAST_MESSAGES.UPDATED_AT, now)
        .set(BROADCAST_MESSAGES.UPDATED_BY, msg.getUpdatedBy().orElse(null))
        .where(BROADCAST_MESSAGES.ID.eq(msg.getId()))
        .execute();

    msg.setUpdatedAt(now.toInstant(UTC));

    return msg;
  }

  public void delete(UUID id) {
    dsl.deleteFrom(BROADCAST_MESSAGES)
        .where(BROADCAST_MESSAGES.ID.eq(id))
        .execute();
  }

  public Optional<BroadcastMessage> findByIdentifier(String identifier) {
    return dsl.selectFrom(BROADCAST_MESSAGES)
        .where(BROADCAST_MESSAGES.IDENTIFIER.eq(identifier))
        .fetchOptional()
        .map(this::toDomain);
  }

  public BroadcastMessage getByIdentifier(String identifier) {
    return findByIdentifier(identifier)
        .orElseThrow(() -> new NotFoundException("Broadcast message not found"));
  }

  public List<BroadcastMessage> findAll() {
    return dsl.selectFrom(BROADCAST_MESSAGES)
        .orderBy(BROADCAST_MESSAGES.CREATED_AT.desc())
        .fetch()
        .map(this::toDomain);
  }

  public List<BroadcastMessage> findActiveForUser(UUID userId) {
    LocalDateTime now = LocalDateTime.now(clock);

    return dsl.selectFrom(BROADCAST_MESSAGES)
        .where(BROADCAST_MESSAGES.SHOW_IN_APP.isTrue())
        .and(BROADCAST_MESSAGES.START_AT.le(now))
        .and(BROADCAST_MESSAGES.END_AT.isNull().or(BROADCAST_MESSAGES.END_AT.gt(now)))
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

    var condition = BROADCAST_MESSAGES.START_AT.le(now)
        .and(BROADCAST_MESSAGES.END_AT.isNull().or(BROADCAST_MESSAGES.END_AT.gt(now)));

    if (login && register) {
      condition = condition.and(
          BROADCAST_MESSAGES.SHOW_ON_LOGIN.isTrue().or(BROADCAST_MESSAGES.SHOW_ON_REGISTER.isTrue()));
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
        .identifier(r.getIdentifier())
        .title(r.getTitle())
        .body(r.getBody())
        .severity(r.getSeverity())
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
}
