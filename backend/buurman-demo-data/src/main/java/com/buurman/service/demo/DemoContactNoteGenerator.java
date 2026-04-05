package com.buurman.service.demo;

import static com.buurman.jooq.generated.Tables.CONTACT_NOTES;
import static com.buurman.util.SidGenerator.newContactNoteId;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import com.buurman.domain.InteractionType;
import com.buurman.domain.Sid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class DemoContactNoteGenerator {

  private final DSLContext dsl;
  private final Clock clock;
  private final Random random = new Random(77);

  // ~3 notes per contact on average
  private static final int MIN_NOTES_PER_CONTACT = 0;
  private static final int MAX_NOTES_PER_CONTACT = 7;

  private static final InteractionType[] INTERACTION_TYPES = InteractionType.values();

  private static final String[] NOTE_BODIES = {
    "Contact called to ask about the status of the repair request. Informed them it will be handled"
        + " within 3 working days.",
    "Met at the property for the lease signing. All documents signed. Keys handed over.",
    "Conducted annual inspection. Property is in good condition. Minor scuff on hallway wall —"
        + " noted.",
    "Discussed upcoming lease renewal. Contact is interested in a 12-month extension at current"
        + " rent.",
    "Reminder sent for outstanding payment. Contact acknowledged and promised to pay by end of"
        + " week.",
    "Contact called to report a leak under the kitchen sink. Plumber scheduled for Thursday.",
    "Initial viewing went well. Contact is interested but wants to think it over.",
    "Move-out inspection completed. Small cleaning fee applies. Deposit to be refunded within 14"
        + " days.",
    "Keys collected from contact. Property now vacant. Meter readings taken.",
    "Spoke with contact regarding noise complaints from neighbours. Situation resolved amicably.",
    "Contact requested permission to install a new dishwasher. Approved with written confirmation.",
    "Contact informed us of a change of employment. Updated income details on file.",
    "Visited property after storm damage report. Roof tiles dislodged — contractor contacted.",
    "Contact provided updated emergency contact details. Recorded in system.",
    "Discussed subletting request. Declined per lease terms. Contact acknowledged.",
    "Follow-up call after maintenance visit. Contact confirmed issue was resolved satisfactorily.",
    "Contact expressed interest in purchasing the property if it comes up for sale.",
    "Sent welcome pack to new contact. Confirmed receipt via email.",
    "Conducted energy efficiency check. Advised contact to bleed radiators before winter.",
    "Contact reported mould near bathroom window. Ventilation improved and mould treated.",
    "Received complaint about broken communal door lock. Notified building manager.",
    "Pre-renewal check. Contact would like minor repairs completed before renewing.",
    "Contact notified of planned maintenance window (water shut-off) next Tuesday 09:00–12:00.",
    "Confirmed direct debit setup. First payment expected next month.",
  };

  public void generate(DemoDataContext ctx) {
    LocalDateTime now = LocalDateTime.now(clock);
    int totalNotes = 0;

    for (var teamEntry : ctx.getTeamIds().entrySet()) {
      String teamKey = teamEntry.getKey();
      UUID teamId = teamEntry.getValue();
      UUID createdBy = ctx.getAdminUserForTeam(teamKey).orElse(null);

      List<UUID> contactIds = ctx.getContactIdsByTeam().getOrDefault(teamId, List.of());

      List<Object[]> records = new ArrayList<>();

      for (UUID contactId : contactIds) {
        int noteCount = random.nextInt(MIN_NOTES_PER_CONTACT, MAX_NOTES_PER_CONTACT + 1);

        for (int n = 0; n < noteCount; n++) {
          InteractionType interactionType =
              INTERACTION_TYPES[random.nextInt(INTERACTION_TYPES.length)];

          // ~70% of notes have a subject; pick from type-specific options
          Optional<String> subject =
              random.nextInt(10) < 7
                  ? Optional.of(randomSubjectFor(interactionType))
                  : Optional.empty();
          String body = NOTE_BODIES[random.nextInt(NOTE_BODIES.length)];

          LocalDateTime occurredAt = now.minusDays(random.nextInt(1, 730));
          boolean pinned = n == 0 && random.nextInt(10) < 3; // ~30% chance first note is pinned

          // ~25% of notes have a follow-up date
          LocalDate followUpDate = null;
          if (random.nextInt(4) == 0) {
            followUpDate = occurredAt.toLocalDate().plusDays(random.nextInt(3, 30));
          }

          // Mark reminder as already sent for follow-up dates in the past
          boolean reminderSent =
              followUpDate != null && followUpDate.isBefore(LocalDate.now(clock));

          Sid identifier = newContactNoteId();

          records.add(
              new Object[] {
                UUID.randomUUID(),
                identifier,
                teamId,
                contactId,
                interactionType.name(),
                subject.orElse(null),
                body,
                occurredAt,
                followUpDate,
                reminderSent,
                pinned,
                occurredAt,
                occurredAt,
                createdBy,
                createdBy
              });

          totalNotes++;
        }
      }

      if (!records.isEmpty()) {
        var insert =
            dsl.insertInto(CONTACT_NOTES)
                .columns(
                    CONTACT_NOTES.ID,
                    CONTACT_NOTES.IDENTIFIER,
                    CONTACT_NOTES.TEAM_ID,
                    CONTACT_NOTES.CONTACT_ID,
                    CONTACT_NOTES.INTERACTION_TYPE,
                    CONTACT_NOTES.SUBJECT,
                    CONTACT_NOTES.BODY,
                    CONTACT_NOTES.OCCURRED_AT,
                    CONTACT_NOTES.FOLLOW_UP_DATE,
                    CONTACT_NOTES.FOLLOW_UP_REMINDER_SENT,
                    CONTACT_NOTES.PINNED,
                    CONTACT_NOTES.CREATED_AT,
                    CONTACT_NOTES.UPDATED_AT,
                    CONTACT_NOTES.CREATED_BY,
                    CONTACT_NOTES.UPDATED_BY)
                .values(
                    (UUID) null,
                    (Sid) null,
                    (UUID) null,
                    (UUID) null,
                    (String) null,
                    (String) null,
                    (String) null,
                    (LocalDateTime) null,
                    (LocalDate) null,
                    (Boolean) null,
                    (Boolean) null,
                    (LocalDateTime) null,
                    (LocalDateTime) null,
                    (UUID) null,
                    (UUID) null);
        var batch = dsl.batch(insert);
        for (Object[] r : records) {
          batch = batch.bind(r);
        }
        batch.execute();
      }

      log.info("Created contact notes for team {}", teamKey);
    }

    log.info("Created {} contact notes total", totalNotes);
  }

  private String randomSubjectFor(InteractionType type) {
    String[] options =
        switch (type) {
          case PHONE_CALL ->
              new String[] {
                "Rent inquiry", "Maintenance follow-up", "Renewal discussion", "Payment reminder"
              };
          case MEETING ->
              new String[] {
                "Lease signing", "Property viewing", "Onboarding meeting", "Dispute resolution"
              };
          case VIEWING ->
              new String[] {"Initial viewing", "Second viewing", "Pre-move-in inspection"};
          case KEY_HANDOVER -> new String[] {"Keys handed over", "Spare key collected"};
          case INSPECTION ->
              new String[] {"Annual inspection", "Move-out inspection", "Damage assessment"};
          case NOTE -> new String[] {"Internal note", "Memo"};
          case OTHER -> new String[] {"Follow-up required", "Miscellaneous"};
        };
    return options[random.nextInt(options.length)];
  }
}
