package com.buurman.service;

import static com.buurman.util.SidGenerator.newContactRelationshipId;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.Contact;
import com.buurman.domain.ContactRelationship;
import com.buurman.domain.identifier.ContactIdentifier;
import com.buurman.domain.identifier.ContactRelationshipIdentifier;
import com.buurman.dto.request.CreateContactRelationshipRequest;
import com.buurman.dto.request.UpdateContactRelationshipRequest;
import com.buurman.dto.response.ContactRelationshipResponse;
import com.buurman.dto.response.ContactSummary;
import com.buurman.mapper.ContactMapper;
import com.buurman.repository.ContactRelationshipRepository;
import com.buurman.repository.ContactRepository;
import com.buurman.security.UserPrincipal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContactRelationshipService {

  private final ContactRelationshipRepository relationshipRepository;
  private final ContactRepository contactRepository;
  private final ContactMapper contactMapper;
  private final AuditService auditService;
  private final MetricsService metricsService;
  private final Clock clock;

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public ContactRelationshipResponse createRelationship(
      ContactIdentifier sourceContactIdentifier,
      CreateContactRelationshipRequest request,
      UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Contact sourceContact =
        contactRepository.getByIdentifierAndTeamId(sourceContactIdentifier, teamId);
    Contact targetContact =
        contactRepository.getByIdentifierAndTeamId(request.targetContactIdentifier(), teamId);

    // Prevent self-reference
    if (sourceContact.getId().equals(targetContact.getId())) {
      throw new IllegalArgumentException("A contact cannot have a relationship with itself");
    }

    // Check for duplicate relationship
    if (relationshipRepository.existsByPairAndType(
        sourceContact.getId(), targetContact.getId(), teamId, request.relationshipType())) {
      throw new IllegalArgumentException(
          "A relationship of type "
              + request.relationshipType().getDisplayName()
              + " already exists between these contacts");
    }

    ContactRelationship relationship =
        ContactRelationship.builder()
            .identifier(Optional.of(newContactRelationshipId()))
            .teamId(teamId)
            .sourceContactId(sourceContact.getId())
            .targetContactId(targetContact.getId())
            .relationshipType(request.relationshipType())
            .notes(request.notes())
            .createdAt(clock.instant())
            .updatedAt(clock.instant())
            .createdBy(principal.getUserId())
            .updatedBy(principal.getUserId())
            .build();

    ContactRelationship saved = relationshipRepository.save(relationship);

    metricsService.incrementCounter("contact_relationship.total");
    log.info(
        "Contact relationship created: {} between {} and {} in team {}",
        saved.getIdentifier().orElseThrow(),
        sourceContactIdentifier,
        request.targetContactIdentifier(),
        teamId);

    auditService.logCreate(
        teamId, "CONTACT_RELATIONSHIP", saved.getId(), principal.getUserId(), saved);

    return toResponse(saved, sourceContact, targetContact, true);
  }

  public List<ContactRelationshipResponse> getRelationships(
      ContactIdentifier contactIdentifier, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Contact contact = contactRepository.getByIdentifierAndTeamId(contactIdentifier, teamId);

    List<ContactRelationship> relationships =
        relationshipRepository.findByContactIdAndTeamId(contact.getId(), teamId);

    return relationships.stream()
        .map(
            relationship -> {
              boolean isSource = relationship.getSourceContactId().equals(contact.getId());
              UUID relatedContactId =
                  isSource
                      ? relationship.getTargetContactId()
                      : relationship.getSourceContactId();
              Contact relatedContact =
                  contactRepository.getByIdAndTeamId(relatedContactId, teamId);

              if (isSource) {
                return toResponse(relationship, contact, relatedContact, true);
              } else {
                return toResponse(relationship, contact, relatedContact, false);
              }
            })
        .toList();
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public ContactRelationshipResponse updateRelationship(
      ContactIdentifier contactIdentifier,
      ContactRelationshipIdentifier relationshipIdentifier,
      UpdateContactRelationshipRequest request,
      UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Contact contact = contactRepository.getByIdentifierAndTeamId(contactIdentifier, teamId);
    ContactRelationship relationship =
        relationshipRepository.getByIdentifierAndTeamId(relationshipIdentifier, teamId);

    ContactRelationship oldRelationship = cloneRelationship(relationship);

    relationship.setRelationshipType(request.relationshipType());
    relationship.setNotes(request.notes());
    relationship.setUpdatedBy(principal.getUserId());
    relationship.setUpdatedAt(clock.instant());

    ContactRelationship updated = relationshipRepository.save(relationship);

    log.info(
        "Contact relationship updated: {} in team {}",
        updated.getIdentifier().orElseThrow(),
        teamId);

    auditService.logUpdate(
        teamId,
        "CONTACT_RELATIONSHIP",
        updated.getId(),
        principal.getUserId(),
        oldRelationship,
        updated,
        auditService.getChangedFields(oldRelationship, updated));

    boolean isSource = updated.getSourceContactId().equals(contact.getId());
    UUID relatedContactId =
        isSource ? updated.getTargetContactId() : updated.getSourceContactId();
    Contact relatedContact = contactRepository.getByIdAndTeamId(relatedContactId, teamId);

    return toResponse(updated, contact, relatedContact, isSource);
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public void deleteRelationship(
      ContactIdentifier contactIdentifier,
      ContactRelationshipIdentifier relationshipIdentifier,
      UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    contactRepository.getByIdentifierAndTeamId(contactIdentifier, teamId);
    ContactRelationship relationship =
        relationshipRepository.getByIdentifierAndTeamId(relationshipIdentifier, teamId);

    relationshipRepository.softDeleteByIdAndTeamId(relationship.getId(), teamId);

    log.info("Contact relationship deleted: {} in team {}", relationshipIdentifier, teamId);

    auditService.logDelete(
        teamId, "CONTACT_RELATIONSHIP", relationship.getId(), principal.getUserId(), relationship);
  }

  /**
   * Builds a response from the viewing contact's perspective.
   *
   * @param relationship the relationship entity
   * @param viewingContact the contact whose perspective we're building from
   * @param relatedContact the other contact in the relationship
   * @param isSource true if viewingContact is the source in the relationship
   */
  private ContactRelationshipResponse toResponse(
      ContactRelationship relationship,
      Contact viewingContact,
      Contact relatedContact,
      boolean isSource) {
    ContactSummary relatedSummary = contactMapper.toSummary(relatedContact);

    String displayLabel =
        isSource
            ? relationship.getRelationshipType().getDisplayName()
            : relationship.getRelationshipType().inverseDisplayName();

    return new ContactRelationshipResponse(
        relationship.getIdentifier().orElseThrow(),
        relatedSummary,
        relationship.getRelationshipType(),
        displayLabel,
        relationship.getNotes(),
        relationship.getCreatedAt(),
        Optional.of(relationship.getUpdatedAt()));
  }

  private ContactRelationship cloneRelationship(ContactRelationship relationship) {
    return ContactRelationship.builder()
        .id(relationship.getId())
        .identifier(relationship.getIdentifier())
        .teamId(relationship.getTeamId())
        .sourceContactId(relationship.getSourceContactId())
        .targetContactId(relationship.getTargetContactId())
        .relationshipType(relationship.getRelationshipType())
        .notes(relationship.getNotes())
        .createdAt(relationship.getCreatedAt())
        .updatedAt(relationship.getUpdatedAt())
        .createdBy(relationship.getCreatedBy())
        .updatedBy(relationship.getUpdatedBy())
        .deletedAt(relationship.getDeletedAt())
        .build();
  }
}
