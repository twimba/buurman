package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.REGISTRATION_INVITATION_USAGES;
import static com.buurman.jooq.generated.Tables.USERS;
import static java.time.ZoneOffset.UTC;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.RegistrationInvitationUsage;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RegistrationInvitationUsageRepository {

  private final DSLContext dsl;
  private final Clock clock;

  public void save(UUID invitationId, UUID userId) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.insertInto(REGISTRATION_INVITATION_USAGES)
        .set(REGISTRATION_INVITATION_USAGES.ID, UUID.randomUUID())
        .set(REGISTRATION_INVITATION_USAGES.INVITATION_ID, invitationId)
        .set(REGISTRATION_INVITATION_USAGES.USER_ID, userId)
        .set(REGISTRATION_INVITATION_USAGES.USED_AT, now)
        .execute();
  }

  public List<RegistrationInvitationUsage> findByInvitationId(UUID invitationId) {
    return List.copyOf(
        dsl.select(
                REGISTRATION_INVITATION_USAGES.ID,
                REGISTRATION_INVITATION_USAGES.INVITATION_ID,
                REGISTRATION_INVITATION_USAGES.USER_ID,
                REGISTRATION_INVITATION_USAGES.USED_AT,
                USERS.EMAIL,
                USERS.FIRST_NAME,
                USERS.LAST_NAME)
            .from(REGISTRATION_INVITATION_USAGES)
            .join(USERS)
            .on(REGISTRATION_INVITATION_USAGES.USER_ID.eq(USERS.ID))
            .where(REGISTRATION_INVITATION_USAGES.INVITATION_ID.eq(invitationId))
            .orderBy(REGISTRATION_INVITATION_USAGES.USED_AT.desc())
            .fetch(
                record -> {
                  RegistrationInvitationUsage usage = new RegistrationInvitationUsage();
                  usage.setId(record.get(REGISTRATION_INVITATION_USAGES.ID));
                  usage.setInvitationId(record.get(REGISTRATION_INVITATION_USAGES.INVITATION_ID));
                  usage.setUserId(record.get(REGISTRATION_INVITATION_USAGES.USER_ID));
                  usage.setUsedAt(
                      record.get(REGISTRATION_INVITATION_USAGES.USED_AT).toInstant(UTC));
                  usage.setUserEmail(record.get(USERS.EMAIL));
                  String firstName = record.get(USERS.FIRST_NAME);
                  String lastName = record.get(USERS.LAST_NAME);
                  usage.setUserName(
                      (firstName != null ? firstName : "")
                          + " "
                          + (lastName != null ? lastName : ""));
                  return usage;
                }));
  }
}
