package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.CONTACT_NOTES;
import static java.time.ZoneOffset.UTC;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.ContactNote;
import com.buurman.domain.Sid;
import com.buurman.exception.NotFoundException;
import com.buurman.mapper.ContactNoteRecordMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ContactNoteRepository {

  private final DSLContext dsl;
  private final ContactNoteRecordMapper mapper;
  private final Clock clock;

  public ContactNote save(ContactNote note) {
    LocalDateTime now = LocalDateTime.now(clock);

    if (note.getId() == null) {
      // INSERT
      UUID newId = UUID.randomUUID();

      dsl.insertInto(CONTACT_NOTES)
          .set(CONTACT_NOTES.ID, newId)
          .set(CONTACT_NOTES.IDENTIFIER, note.getIdentifier().orElseThrow())
          .set(CONTACT_NOTES.TEAM_ID, note.getTeamId())
          .set(CONTACT_NOTES.CONTACT_ID, note.getContactId())
          .set(CONTACT_NOTES.INTERACTION_TYPE, note.getInteractionType().name())
          .set(CONTACT_NOTES.SUBJECT, note.getSubject().orElse(null))
          .set(CONTACT_NOTES.BODY, note.getBody())
          .set(CONTACT_NOTES.OCCURRED_AT, LocalDateTime.ofInstant(note.getOccurredAt(), UTC))
          .set(CONTACT_NOTES.FOLLOW_UP_DATE, note.getFollowUpDate().orElse(null))
          .set(CONTACT_NOTES.FOLLOW_UP_REMINDER_SENT, note.isFollowUpReminderSent())
          .set(CONTACT_NOTES.PINNED, note.isPinned())
          .set(CONTACT_NOTES.CREATED_AT, now)
          .set(CONTACT_NOTES.UPDATED_AT, now)
          .set(CONTACT_NOTES.CREATED_BY, note.getCreatedBy())
          .set(CONTACT_NOTES.UPDATED_BY, note.getUpdatedBy())
          .execute();

      note.setId(newId);
      note.setCreatedAt(now.toInstant(UTC));
      note.setUpdatedAt(now.toInstant(UTC));
    } else {
      // UPDATE
      dsl.update(CONTACT_NOTES)
          .set(CONTACT_NOTES.INTERACTION_TYPE, note.getInteractionType().name())
          .set(CONTACT_NOTES.SUBJECT, note.getSubject().orElse(null))
          .set(CONTACT_NOTES.BODY, note.getBody())
          .set(CONTACT_NOTES.OCCURRED_AT, LocalDateTime.ofInstant(note.getOccurredAt(), UTC))
          .set(CONTACT_NOTES.FOLLOW_UP_DATE, note.getFollowUpDate().orElse(null))
          .set(CONTACT_NOTES.FOLLOW_UP_REMINDER_SENT, note.isFollowUpReminderSent())
          .set(CONTACT_NOTES.PINNED, note.isPinned())
          .set(CONTACT_NOTES.UPDATED_AT, now)
          .set(CONTACT_NOTES.UPDATED_BY, note.getUpdatedBy())
          .where(CONTACT_NOTES.ID.eq(note.getId()).and(CONTACT_NOTES.TEAM_ID.eq(note.getTeamId())))
          .execute();

      note.setUpdatedAt(now.toInstant(UTC));
    }

    return note;
  }

  public List<ContactNote> findByContactIdAndTeamId(UUID contactId, UUID teamId) {
    return List.copyOf(
        dsl.selectFrom(CONTACT_NOTES)
            .where(
                CONTACT_NOTES
                    .CONTACT_ID
                    .eq(contactId)
                    .and(CONTACT_NOTES.TEAM_ID.eq(teamId))
                    .and(CONTACT_NOTES.DELETED_AT.isNull()))
            .orderBy(CONTACT_NOTES.OCCURRED_AT.desc())
            .fetch()
            .map(mapper::toDomain));
  }

  public Optional<ContactNote> findByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return dsl.selectFrom(CONTACT_NOTES)
        .where(
            CONTACT_NOTES
                .IDENTIFIER
                .eq(identifier)
                .and(CONTACT_NOTES.TEAM_ID.eq(teamId))
                .and(CONTACT_NOTES.DELETED_AT.isNull()))
        .fetchOptional()
        .map(mapper::toDomain);
  }

  public ContactNote getByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return findByIdentifierAndTeamId(identifier, teamId)
        .orElseThrow(() -> new NotFoundException("Contact note not found"));
  }

  public void softDeleteByIdAndTeamId(UUID id, UUID teamId) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(CONTACT_NOTES)
        .set(CONTACT_NOTES.DELETED_AT, now)
        .where(CONTACT_NOTES.ID.eq(id).and(CONTACT_NOTES.TEAM_ID.eq(teamId)))
        .execute();
  }

  public List<ContactNote> findPendingFollowUps(LocalDate date) {
    return List.copyOf(
        dsl.selectFrom(CONTACT_NOTES)
            .where(
                CONTACT_NOTES
                    .FOLLOW_UP_DATE
                    .le(date)
                    .and(CONTACT_NOTES.FOLLOW_UP_REMINDER_SENT.eq(false))
                    .and(CONTACT_NOTES.DELETED_AT.isNull()))
            .fetch()
            .map(mapper::toDomain));
  }

  public void markFollowUpReminderSent(UUID noteId, UUID teamId) {
    dsl.update(CONTACT_NOTES)
        .set(CONTACT_NOTES.FOLLOW_UP_REMINDER_SENT, true)
        .where(CONTACT_NOTES.ID.eq(noteId).and(CONTACT_NOTES.TEAM_ID.eq(teamId)))
        .execute();
  }

  public void setPinned(UUID noteId, UUID teamId, boolean pinned) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(CONTACT_NOTES)
        .set(CONTACT_NOTES.PINNED, pinned)
        .set(CONTACT_NOTES.UPDATED_AT, now)
        .where(CONTACT_NOTES.ID.eq(noteId).and(CONTACT_NOTES.TEAM_ID.eq(teamId)))
        .execute();
  }

  public void anonymizeByContactId(UUID contactId, UUID teamId) {
    dsl.update(CONTACT_NOTES)
        .set(CONTACT_NOTES.BODY, "Content erased per GDPR request")
        .set(CONTACT_NOTES.SUBJECT, (String) null)
        .where(CONTACT_NOTES.CONTACT_ID.eq(contactId).and(CONTACT_NOTES.TEAM_ID.eq(teamId)))
        .execute();
  }
}
