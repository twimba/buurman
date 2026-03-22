package com.buurman.service;

import static com.buurman.util.SidGenerator.newContactNoteId;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.Contact;
import com.buurman.domain.ContactNote;
import com.buurman.domain.User;
import com.buurman.domain.identifier.ContactIdentifier;
import com.buurman.domain.identifier.ContactNoteIdentifier;
import com.buurman.dto.request.CreateContactNoteRequest;
import com.buurman.dto.request.UpdateContactNoteRequest;
import com.buurman.dto.response.ContactNoteResponse;
import com.buurman.repository.ContactNoteRepository;
import com.buurman.repository.ContactRepository;
import com.buurman.repository.UserRepository;
import com.buurman.security.UserPrincipal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContactNoteService {

  private final ContactNoteRepository noteRepository;
  private final ContactRepository contactRepository;
  private final UserRepository userRepository;
  private final AuditService auditService;
  private final MetricsService metricsService;
  private final Clock clock;

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public ContactNoteResponse createNote(
      ContactIdentifier contactIdentifier,
      CreateContactNoteRequest request,
      UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Contact contact = contactRepository.getByIdentifierAndTeamId(contactIdentifier, teamId);

    ContactNote note =
        ContactNote.builder()
            .identifier(Optional.of(newContactNoteId()))
            .teamId(teamId)
            .contactId(contact.getId())
            .interactionType(request.interactionType())
            .subject(request.subject())
            .body(request.body())
            .occurredAt(request.occurredAt())
            .followUpDate(request.followUpDate())
            .followUpReminderSent(false)
            .pinned(false)
            .createdAt(clock.instant())
            .updatedAt(clock.instant())
            .createdBy(principal.getUserId())
            .updatedBy(principal.getUserId())
            .build();

    ContactNote saved = noteRepository.save(note);

    metricsService.incrementCounter("contact_note.total");
    log.info(
        "Contact note created: {} for contact {} in team {}",
        saved.getIdentifier().orElseThrow(),
        contactIdentifier,
        teamId);

    auditService.logCreate(teamId, "CONTACT_NOTE", saved.getId(), principal.getUserId(), saved);

    return toResponse(saved);
  }

  @PreAuthorize("hasRole('TEAM_VIEWER')")
  public List<ContactNoteResponse> getNotes(
      ContactIdentifier contactIdentifier, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Contact contact = contactRepository.getByIdentifierAndTeamId(contactIdentifier, teamId);

    List<ContactNote> notes = noteRepository.findByContactIdAndTeamId(contact.getId(), teamId);

    // Batch-load users to avoid N+1
    Set<UUID> userIds = notes.stream().map(ContactNote::getCreatedBy).collect(Collectors.toSet());
    Map<UUID, User> usersById =
        userRepository.findByIds(userIds).stream()
            .collect(Collectors.toMap(User::getId, Function.identity()));

    return notes.stream().map(note -> toResponse(note, usersById)).toList();
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public ContactNoteResponse updateNote(
      ContactIdentifier contactIdentifier,
      ContactNoteIdentifier noteIdentifier,
      UpdateContactNoteRequest request,
      UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    contactRepository.getByIdentifierAndTeamId(contactIdentifier, teamId);
    ContactNote note = noteRepository.getByIdentifierAndTeamId(noteIdentifier, teamId);

    ContactNote oldNote = cloneNote(note);

    note.setInteractionType(request.interactionType());
    note.setSubject(request.subject());
    note.setBody(request.body());
    note.setOccurredAt(request.occurredAt());

    // Reset followUpReminderSent if followUpDate changed
    boolean followUpDateChanged = !note.getFollowUpDate().equals(request.followUpDate());
    note.setFollowUpDate(request.followUpDate());
    if (followUpDateChanged) {
      note.setFollowUpReminderSent(false);
    }

    note.setUpdatedBy(principal.getUserId());
    note.setUpdatedAt(clock.instant());

    ContactNote updated = noteRepository.save(note);

    log.info("Contact note updated: {} in team {}", updated.getIdentifier().orElseThrow(), teamId);

    auditService.logUpdate(
        teamId,
        "CONTACT_NOTE",
        updated.getId(),
        principal.getUserId(),
        oldNote,
        updated,
        auditService.getChangedFields(oldNote, updated));

    return toResponse(updated);
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public void deleteNote(
      ContactIdentifier contactIdentifier,
      ContactNoteIdentifier noteIdentifier,
      UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    contactRepository.getByIdentifierAndTeamId(contactIdentifier, teamId);
    ContactNote note = noteRepository.getByIdentifierAndTeamId(noteIdentifier, teamId);

    noteRepository.softDeleteByIdAndTeamId(note.getId(), teamId);

    log.info("Contact note deleted: {} in team {}", noteIdentifier, teamId);

    auditService.logDelete(teamId, "CONTACT_NOTE", note.getId(), principal.getUserId(), note);
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public ContactNoteResponse pinNote(
      ContactIdentifier contactIdentifier,
      ContactNoteIdentifier noteIdentifier,
      UserPrincipal principal) {
    return setPinned(contactIdentifier, noteIdentifier, true, principal);
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public ContactNoteResponse unpinNote(
      ContactIdentifier contactIdentifier,
      ContactNoteIdentifier noteIdentifier,
      UserPrincipal principal) {
    return setPinned(contactIdentifier, noteIdentifier, false, principal);
  }

  private ContactNoteResponse setPinned(
      ContactIdentifier contactIdentifier,
      ContactNoteIdentifier noteIdentifier,
      boolean pinned,
      UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    contactRepository.getByIdentifierAndTeamId(contactIdentifier, teamId);
    ContactNote note = noteRepository.getByIdentifierAndTeamId(noteIdentifier, teamId);

    noteRepository.setPinned(note.getId(), teamId, pinned);
    note.setPinned(pinned);

    return toResponse(note);
  }

  private ContactNoteResponse toResponse(ContactNote note) {
    String createdByName =
        userRepository.findById(note.getCreatedBy()).map(User::getFullName).orElse("Unknown User");
    return toResponse(note, createdByName);
  }

  private ContactNoteResponse toResponse(ContactNote note, Map<UUID, User> usersById) {
    String createdByName =
        Optional.ofNullable(usersById.get(note.getCreatedBy()))
            .map(User::getFullName)
            .orElse("Unknown User");
    return toResponse(note, createdByName);
  }

  private ContactNoteResponse toResponse(ContactNote note, String createdByName) {
    return new ContactNoteResponse(
        note.getIdentifier().orElseThrow(),
        note.getInteractionType(),
        note.getSubject(),
        note.getBody(),
        note.getOccurredAt(),
        note.getFollowUpDate(),
        note.isFollowUpReminderSent(),
        note.isPinned(),
        Optional.of(createdByName),
        note.getCreatedAt(),
        Optional.of(note.getUpdatedAt()));
  }

  private ContactNote cloneNote(ContactNote note) {
    return ContactNote.builder()
        .id(note.getId())
        .identifier(note.getIdentifier())
        .teamId(note.getTeamId())
        .contactId(note.getContactId())
        .interactionType(note.getInteractionType())
        .subject(note.getSubject())
        .body(note.getBody())
        .occurredAt(note.getOccurredAt())
        .followUpDate(note.getFollowUpDate())
        .followUpReminderSent(note.isFollowUpReminderSent())
        .pinned(note.isPinned())
        .createdAt(note.getCreatedAt())
        .updatedAt(note.getUpdatedAt())
        .createdBy(note.getCreatedBy())
        .updatedBy(note.getUpdatedBy())
        .deletedAt(note.getDeletedAt())
        .build();
  }
}
