package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.BROADCAST_MESSAGE_DISMISSALS;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class BroadcastMessageDismissalRepository {

  private final DSLContext dsl;
  private final Clock clock;

  public void save(UUID messageId, UUID userId) {
    LocalDateTime now = LocalDateTime.now(clock);

    dsl.insertInto(BROADCAST_MESSAGE_DISMISSALS)
        .set(BROADCAST_MESSAGE_DISMISSALS.ID, UUID.randomUUID())
        .set(BROADCAST_MESSAGE_DISMISSALS.BROADCAST_MESSAGE_ID, messageId)
        .set(BROADCAST_MESSAGE_DISMISSALS.USER_ID, userId)
        .set(BROADCAST_MESSAGE_DISMISSALS.DISMISSED_AT, now)
        .onConflictDoNothing()
        .execute();
  }
}
