package com.buurman.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.buurman.domain.Contract;
import com.buurman.domain.ContractExtension;
import com.buurman.domain.Document;
import com.buurman.domain.SignatureRequest;
import com.buurman.domain.TimelineEventType;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.dto.response.RecentActivityResponse;
import com.buurman.dto.response.TimelineEventResponse;
import com.buurman.repository.ContractExtensionRepository;
import com.buurman.repository.ContractRentPeriodRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.DocumentRepository;
import com.buurman.repository.SignatureRequestRepository;
import com.buurman.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ContractTimelineService {

  private final ContractRepository contractRepository;
  private final AuditService auditService;
  private final ContractRentPeriodRepository rentPeriodRepository;
  private final ContractExtensionRepository extensionRepository;
  private final DocumentRepository documentRepository;
  private final SignatureRequestRepository signatureRequestRepository;

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR', 'TEAM_VIEWER')")
  public List<TimelineEventResponse> getTimeline(
      ContractIdentifier identifier, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Contract contract = contractRepository.getByIdentifierAndTeamId(identifier, teamId);

    List<TimelineEventResponse> events = new ArrayList<>();
    events.addAll(mapAuditEvents(teamId, contract.getId()));
    events.addAll(mapRentPeriods(teamId, contract.getId()));
    events.addAll(mapExtensions(teamId, contract.getId()));

    List<Document> documents =
        documentRepository.findByEntityAndTeamId("CONTRACT", contract.getId(), teamId);
    events.addAll(mapDocuments(documents));
    events.addAll(mapSignatureRequests(documents, teamId));

    return events.stream()
        .sorted(Comparator.comparing(TimelineEventResponse::timestamp).reversed())
        .toList();
  }

  private List<TimelineEventResponse> mapAuditEvents(UUID teamId, UUID contractId) {
    return auditService.getEntityAuditLog(teamId, "CONTRACT", contractId).stream()
        .map(this::toAuditTimelineEvent)
        .toList();
  }

  private TimelineEventResponse toAuditTimelineEvent(RecentActivityResponse activity) {
    TimelineEventType type =
        "CREATE".equals(activity.action())
            ? TimelineEventType.CONTRACT_CREATED
            : activity.changedFields().containsKey("status")
                ? TimelineEventType.CONTRACT_STATUS_CHANGED
                : TimelineEventType.AUDIT_OTHER;
    return new TimelineEventResponse(
        type,
        activity.timestamp(),
        activity.description().orElse(activity.action()),
        Optional.empty(),
        Optional.empty());
  }

  private List<TimelineEventResponse> mapRentPeriods(UUID teamId, UUID contractId) {
    return rentPeriodRepository.findByContractIdAndTeamId(contractId, teamId).stream()
        .map(
            period ->
                new TimelineEventResponse(
                    TimelineEventType.RENT_CHANGED,
                    period.getCreatedAt(),
                    "Rent changed to "
                        + period.getRentAmount()
                        + " effective "
                        + period.getEffectiveFrom(),
                    Optional.empty(),
                    period.getIdentifier()))
        .toList();
  }

  private List<TimelineEventResponse> mapExtensions(UUID teamId, UUID contractId) {
    List<TimelineEventResponse> events = new ArrayList<>();
    for (ContractExtension extension :
        extensionRepository.findByContractIdAndTeamId(contractId, teamId)) {
      events.add(
          new TimelineEventResponse(
              TimelineEventType.EXTENSION_CREATED,
              extension.getCreatedAt(),
              "Extension #" + extension.getExtensionNumber() + " created",
              Optional.empty(),
              extension.getIdentifier()));
      extension
          .getActivatedAt()
          .ifPresent(
              activatedAt ->
                  events.add(
                      new TimelineEventResponse(
                          TimelineEventType.EXTENSION_ACTIVATED,
                          activatedAt,
                          "Extension #" + extension.getExtensionNumber() + " activated",
                          Optional.empty(),
                          extension.getIdentifier())));
      if (extension.getStatus() == ContractExtension.ExtensionStatus.DECLINED) {
        events.add(
            new TimelineEventResponse(
                TimelineEventType.EXTENSION_DECLINED,
                extension
                    .getUpdatedAt(), // no dedicated declinedAt column — see plan's Review Focus
                "Extension #" + extension.getExtensionNumber() + " declined",
                extension.getDeclinedReason(),
                extension.getIdentifier()));
      }
    }
    return events;
  }

  private List<TimelineEventResponse> mapDocuments(List<Document> documents) {
    return documents.stream()
        .map(
            document ->
                new TimelineEventResponse(
                    TimelineEventType.DOCUMENT_UPLOADED,
                    document.getUploadedAt(),
                    "Document added: " + document.getFileName(),
                    Optional.empty(),
                    document.getIdentifier()))
        .toList();
  }

  private List<TimelineEventResponse> mapSignatureRequests(List<Document> documents, UUID teamId) {
    List<TimelineEventResponse> events = new ArrayList<>();
    for (Document document : documents) {
      for (SignatureRequest request :
          signatureRequestRepository.findByDocumentIdAndTeamId(document.getId(), teamId)) {
        events.add(
            new TimelineEventResponse(
                TimelineEventType.SIGNATURE_SENT,
                request.getCreatedAt(),
                "Sent for signature: " + document.getFileName(),
                Optional.empty(),
                request.getIdentifier()));
        switch (request.getStatus()) {
          case COMPLETED ->
              events.add(
                  new TimelineEventResponse(
                      TimelineEventType.SIGNATURE_COMPLETED,
                      request.getUpdatedAt(),
                      "Signed: " + document.getFileName(),
                      Optional.empty(),
                      request.getIdentifier()));
          case DECLINED ->
              events.add(
                  new TimelineEventResponse(
                      TimelineEventType.SIGNATURE_DECLINED,
                      request.getUpdatedAt(),
                      "Signature declined: " + document.getFileName(),
                      Optional.empty(),
                      request.getIdentifier()));
          default -> {
            /* PENDING/PARTIALLY_SIGNED/CANCELLED/FAILED produce no second event yet */
          }
        }
      }
    }
    return events;
  }
}
