package com.buurman.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.buurman.domain.Contract;
import com.buurman.domain.ContractExtension;
import com.buurman.domain.ContractExtension.ExtensionStatus;
import com.buurman.domain.ContractExtension.TriggerType;
import com.buurman.domain.Document;
import com.buurman.domain.Sid;
import com.buurman.domain.SignatureRequest;
import com.buurman.domain.SignatureRequestStatus;
import com.buurman.domain.TimelineEventType;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.dto.response.RecentActivityResponse;
import com.buurman.dto.response.TimelineEventResponse;
import com.buurman.exception.NotFoundException;
import com.buurman.repository.ContractExtensionRepository;
import com.buurman.repository.ContractRentPeriodRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.DocumentRepository;
import com.buurman.repository.SignatureRequestRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.util.MoneyAmount;

class ContractTimelineServiceTest {

  private final ContractRepository contractRepository = mock(ContractRepository.class);
  private final AuditService auditService = mock(AuditService.class);
  private final ContractRentPeriodRepository rentPeriodRepository =
      mock(ContractRentPeriodRepository.class);
  private final ContractExtensionRepository extensionRepository =
      mock(ContractExtensionRepository.class);
  private final DocumentRepository documentRepository = mock(DocumentRepository.class);
  private final SignatureRequestRepository signatureRequestRepository =
      mock(SignatureRequestRepository.class);

  private ContractTimelineService service;

  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID CONTRACT_ID = UUID.randomUUID();

  private final UserPrincipal principal =
      new UserPrincipal(
          UUID.randomUUID(),
          "USR1",
          "kc-1",
          "landlord@example.com",
          "Landlord",
          TEAM_ID,
          "TEA1",
          null);

  @BeforeEach
  void setUp() {
    service =
        new ContractTimelineService(
            contractRepository,
            auditService,
            rentPeriodRepository,
            extensionRepository,
            documentRepository,
            signatureRequestRepository);

    Contract contract = Contract.builder().id(CONTRACT_ID).teamId(TEAM_ID).build();
    when(contractRepository.getByIdentifierAndTeamId(any(Sid.class), eq(TEAM_ID)))
        .thenReturn(contract);
    when(auditService.getEntityAuditLog(TEAM_ID, "CONTRACT", CONTRACT_ID)).thenReturn(List.of());
    when(rentPeriodRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
        .thenReturn(List.of());
    when(extensionRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID)).thenReturn(List.of());
    when(documentRepository.findByEntityAndTeamId("CONTRACT", CONTRACT_ID, TEAM_ID))
        .thenReturn(List.of());
  }

  private ContractIdentifier contractId() {
    return ContractIdentifier.of("CON00000000000000000000001");
  }

  @Test
  @DisplayName("a contract with only a CREATE audit row shows exactly that event")
  void onlyCreateEvent() {
    when(auditService.getEntityAuditLog(TEAM_ID, "CONTRACT", CONTRACT_ID))
        .thenReturn(
            List.of(
                new RecentActivityResponse(
                    "CONTRACT",
                    Sid.of("CON00000000000000000000001"),
                    "CONTRACT",
                    "CREATE",
                    "Landlord",
                    Instant.parse("2026-01-01T10:00:00Z"),
                    "Landlord created this contract")));

    List<TimelineEventResponse> timeline = service.getTimeline(contractId(), principal);

    assertThat(timeline).hasSize(1);
    assertThat(timeline.get(0).type()).isEqualTo(TimelineEventType.CONTRACT_CREATED);
  }

  @Test
  @DisplayName(
      "cross-team contract identifier resolves NotFoundException before any source is queried")
  void crossTeamContractNotFound() {
    when(contractRepository.getByIdentifierAndTeamId(any(Sid.class), eq(TEAM_ID)))
        .thenThrow(new NotFoundException("Contract not found"));

    assertThatThrownBy(() -> service.getTimeline(contractId(), principal))
        .isInstanceOf(NotFoundException.class);

    org.mockito.Mockito.verifyNoInteractions(
        rentPeriodRepository, extensionRepository, documentRepository, signatureRequestRepository);
  }

  @Test
  @DisplayName(
      "a signature request with no signed signers yet produces SIGNATURE_SENT only, never a"
          + " fabricated COMPLETED")
  void signatureRequestPendingProducesSentOnly() {
    UUID documentId = UUID.randomUUID();
    Document document =
        Document.builder()
            .id(documentId)
            .teamId(TEAM_ID)
            .identifier(Optional.of(Sid.of("DOC00000000000000000000001")))
            .entityType("CONTRACT")
            .entityId(CONTRACT_ID)
            .fileName("addendum.pdf")
            .uploadedAt(Instant.parse("2026-02-01T08:00:00Z"))
            .build();
    when(documentRepository.findByEntityAndTeamId("CONTRACT", CONTRACT_ID, TEAM_ID))
        .thenReturn(List.of(document));
    when(signatureRequestRepository.findByDocumentIdsAndTeamId(List.of(documentId), TEAM_ID))
        .thenReturn(
            List.of(
                SignatureRequest.builder()
                    .id(UUID.randomUUID())
                    .identifier(Optional.of(Sid.of("SGR00000000000000000000001")))
                    .teamId(TEAM_ID)
                    .documentId(documentId)
                    .provider("documenso")
                    .providerSubmissionId("envelope_1")
                    .status(SignatureRequestStatus.PENDING)
                    .createdAt(Instant.parse("2026-02-01T09:00:00Z"))
                    .updatedAt(Instant.parse("2026-02-01T09:00:00Z"))
                    .build()));

    List<TimelineEventResponse> timeline = service.getTimeline(contractId(), principal);

    assertThat(timeline).hasSize(2); // DOCUMENT_UPLOADED + SIGNATURE_SENT
    assertThat(timeline)
        .extracting(TimelineEventResponse::type)
        .containsExactlyInAnyOrder(
            TimelineEventType.DOCUMENT_UPLOADED, TimelineEventType.SIGNATURE_SENT);
    assertThat(timeline).noneMatch(e -> e.type() == TimelineEventType.SIGNATURE_COMPLETED);
  }

  @Test
  @DisplayName(
      "cross-source events interleave into one timestamp-sorted list, not five concatenated"
          + " sub-lists")
  void crossSourceInterleaving() {
    when(auditService.getEntityAuditLog(TEAM_ID, "CONTRACT", CONTRACT_ID))
        .thenReturn(
            List.of(
                new RecentActivityResponse(
                    "CONTRACT",
                    Sid.of("CON00000000000000000000001"),
                    "CONTRACT",
                    "CREATE",
                    "Landlord",
                    Instant.parse("2026-01-01T00:00:00Z"),
                    "created"),
                new RecentActivityResponse(
                    "CONTRACT",
                    Sid.of("CON00000000000000000000001"),
                    "CONTRACT",
                    "UPDATE",
                    "Landlord",
                    Instant.parse("2026-03-01T00:00:00Z"),
                    "updated")));
    when(extensionRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
        .thenReturn(
            List.of(
                ContractExtension.builder()
                    .id(UUID.randomUUID())
                    .identifier(Optional.of(Sid.of("CEX00000000000000000000001")))
                    .teamId(TEAM_ID)
                    .contractId(CONTRACT_ID)
                    .extensionNumber(1)
                    .previousEndDate(java.time.LocalDate.of(2026, 1, 1))
                    .previousRentAmount(MoneyAmount.of(new BigDecimal("1000.00"), "EUR"))
                    .newRentAmount(MoneyAmount.of(new BigDecimal("1050.00"), "EUR"))
                    .rentAdjustmentType(ContractExtension.RentAdjustmentType.FIXED_PERCENTAGE)
                    .status(ExtensionStatus.ACTIVE)
                    .triggerType(TriggerType.MANUAL)
                    .createdAt(
                        Instant.parse("2026-02-01T00:00:00Z")) // between the two audit events
                    .updatedAt(Instant.parse("2026-02-01T00:00:00Z"))
                    .createdBy(UUID.randomUUID())
                    .updatedBy(UUID.randomUUID())
                    .build()));

    List<TimelineEventResponse> timeline = service.getTimeline(contractId(), principal);

    assertThat(timeline).hasSize(3);
    assertThat(timeline)
        .extracting(TimelineEventResponse::timestamp)
        .isSortedAccordingTo(java.util.Comparator.reverseOrder());
    assertThat(timeline.get(1).type()).isEqualTo(TimelineEventType.EXTENSION_CREATED);
  }

  @Test
  @DisplayName(
      "a declined extension uses updatedAt for EXTENSION_DECLINED, since there is no declinedAt"
          + " column")
  void declinedExtensionUsesUpdatedAt() {
    Instant declinedAt = Instant.parse("2026-04-01T00:00:00Z");
    when(extensionRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
        .thenReturn(
            List.of(
                ContractExtension.builder()
                    .id(UUID.randomUUID())
                    .identifier(Optional.of(Sid.of("CEX00000000000000000000002")))
                    .teamId(TEAM_ID)
                    .contractId(CONTRACT_ID)
                    .extensionNumber(1)
                    .previousEndDate(java.time.LocalDate.of(2026, 1, 1))
                    .previousRentAmount(MoneyAmount.of(new BigDecimal("1000.00"), "EUR"))
                    .newRentAmount(MoneyAmount.of(new BigDecimal("1000.00"), "EUR"))
                    .rentAdjustmentType(ContractExtension.RentAdjustmentType.NONE)
                    .status(ExtensionStatus.DECLINED)
                    .declinedReason(Optional.of("Tenant moving out"))
                    .triggerType(TriggerType.MANUAL)
                    .createdAt(Instant.parse("2026-03-15T00:00:00Z"))
                    .updatedAt(declinedAt)
                    .createdBy(UUID.randomUUID())
                    .updatedBy(UUID.randomUUID())
                    .build()));

    List<TimelineEventResponse> timeline = service.getTimeline(contractId(), principal);

    TimelineEventResponse declined =
        timeline.stream()
            .filter(e -> e.type() == TimelineEventType.EXTENSION_DECLINED)
            .findFirst()
            .orElseThrow();
    assertThat(declined.timestamp()).isEqualTo(declinedAt);
  }
}
