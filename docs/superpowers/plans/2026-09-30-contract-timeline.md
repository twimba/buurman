# Contract Timeline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the contract's audit-only "History" tab with a single chronological feed merging audit events, rent changes, extensions, documents, and signature requests.

**Architecture:** One new read-only `ContractTimelineService` in `buurman-core` queries five already-existing, already-team-scoped repositories, maps each source's rows into a common `TimelineEventResponse` shape, and returns them sorted by timestamp descending — no new tables, no new writers, nothing about how or when the underlying data gets created changes. A new sibling endpoint (`GET /contracts/{identifier}/timeline`) exposes it; the frontend gets one new component styled after the existing `CommunicationsTimeline` vertical-rail pattern and one hook, wired into `ContractDetailPage` in place of `ContractHistoryTab`.

**Tech Stack:** Java 25 / Spring Boot 4 / JOOQ (backend, read-only aggregation — no schema), React 19 / TanStack Query 5 / TypeScript (frontend).

**Spec:** `docs/superpowers/specs/2026-09-30-contract-timeline-design.md`

## Global Constraints

- Every query filters by `team_id` (project rule) — the timeline resolves the contract team-scoped first, then every source repository call is already team-scoped by construction (see Task 1).
- All `if`/`else`/`for`/`while` bodies use curly braces (CLAUDE.md).
- Use idiomatic `Optional` API — never `if (opt != null)` or unchecked `.get()` (CLAUDE.md).
- No pagination (spec's explicit scope decision — a contract's lifetime is bounded).
- No new database migration — this feature is pure read-aggregation over existing tables.
- The existing `/contracts/{identifier}/audit-log` endpoint and its `RecentActivityResponse` DTO are untouched — this plan adds a sibling endpoint, never modifies that one.
- `useTabState`'s tab keys persist in the URL (`?tab=history`) — the internal tab key `'history'` is NOT renamed (would break bookmarked/shared links); only the *displayed label text* changes from "History" to "Timeline", via the existing i18n value for the existing key, across all 13 locale bundles.

## Review Focus

- **A contract with only a CREATE audit row and nothing else** (the common case — most contracts have few extensions/rent changes/documents): the timeline must show exactly the creation event, not throw or render an empty timeline for a contract that plainly has history. (Task 1 — `ContractTimelineServiceTest`.)
- **Cross-team contract identifier**: a contract identifier belonging to another team must resolve `NotFoundException` before any source repository is queried, not leak a partial or empty timeline for a contract the caller can't see. (Task 1 — `ContractTimelineServiceTest`.)
- **A signature request with zero signed signers yet**: must produce `SIGNATURE_SENT` only — never a fabricated `SIGNATURE_COMPLETED` just because a request row exists. (Task 1 — `ContractTimelineServiceTest`, matches the spec's explicit callout.)
- **Cross-source interleaving**: an extension event timestamped between two audit-log events must sort into the correct position in the merged, single-sorted list — proving the merge is a real sort-by-timestamp over all sources combined, not five separately-sorted sub-lists concatenated. (Task 1 — `ContractTimelineServiceTest`.)
- **A declined extension with no dedicated "declined at" timestamp**: `ContractExtension` has no `declinedAt` column (only `declinedReason`) — the mapping must use `updatedAt` for the `EXTENSION_DECLINED` event's timestamp rather than silently dropping the event or using a wrong timestamp field. (Task 1 — explicit test.)

---

## Task 1: `ContractTimelineService` (backend aggregation)

**Files:**
- Create: `backend/buurman-common/src/main/java/com/buurman/domain/TimelineEventType.java`
- Create: `backend/buurman-common/src/main/java/com/buurman/dto/response/TimelineEventResponse.java`
- Create: `backend/buurman-core/src/main/java/com/buurman/service/ContractTimelineService.java`
- Test: `backend/buurman-core/src/test/java/com/buurman/service/ContractTimelineServiceTest.java`

**Interfaces:**
- Consumes: `ContractRepository.getByIdentifierAndTeamId(Sid, UUID): Contract` (existing), `AuditService.getEntityAuditLog(UUID teamId, String entityType, UUID entityId): List<RecentActivityResponse>` (existing — already builds human descriptions and resolves user names, reused wholesale, not reimplemented), `ContractRentPeriodRepository.findByContractIdAndTeamId(UUID, UUID): List<ContractRentPeriod>` (existing), `ContractExtensionRepository.findByContractIdAndTeamId(UUID, UUID): List<ContractExtension>` (existing), `DocumentRepository.findByEntityAndTeamId(String, UUID, UUID): List<Document>` (existing), `SignatureRequestRepository.findByDocumentIdAndTeamId(UUID, UUID): List<SignatureRequest>` (existing, this branch).
- Produces: `ContractTimelineService.getTimeline(ContractIdentifier, UserPrincipal): List<TimelineEventResponse>` — Task 2's controller calls this directly.

- [ ] **Step 1: Write the failing test file**

```java
package com.buurman.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.buurman.domain.Contract;
import com.buurman.domain.ContractExtension;
import com.buurman.domain.ContractExtension.ExtensionStatus;
import com.buurman.domain.ContractExtension.TriggerType;
import com.buurman.domain.ContractRentPeriod;
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
          UUID.randomUUID(), "USR1", "kc-1", "landlord@example.com", "Landlord", TEAM_ID, "TEA1", null);

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
    when(contractRepository.getByIdentifierAndTeamId(any(Sid.class), eq(TEAM_ID))).thenReturn(contract);
    when(auditService.getEntityAuditLog(TEAM_ID, "CONTRACT", CONTRACT_ID)).thenReturn(List.of());
    when(rentPeriodRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID)).thenReturn(List.of());
    when(extensionRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID)).thenReturn(List.of());
    when(documentRepository.findByEntityAndTeamId("CONTRACT", CONTRACT_ID, TEAM_ID)).thenReturn(List.of());
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
  @DisplayName("cross-team contract identifier resolves NotFoundException before any source is queried")
  void crossTeamContractNotFound() {
    when(contractRepository.getByIdentifierAndTeamId(any(Sid.class), eq(TEAM_ID)))
        .thenThrow(new NotFoundException("Contract not found"));

    assertThatThrownBy(() -> service.getTimeline(contractId(), principal))
        .isInstanceOf(NotFoundException.class);

    org.mockito.Mockito.verifyNoInteractions(
        rentPeriodRepository, extensionRepository, documentRepository, signatureRequestRepository);
  }

  @Test
  @DisplayName("a signature request with no signed signers yet produces SIGNATURE_SENT only, never a fabricated COMPLETED")
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
            .build();
    when(documentRepository.findByEntityAndTeamId("CONTRACT", CONTRACT_ID, TEAM_ID))
        .thenReturn(List.of(document));
    when(signatureRequestRepository.findByDocumentIdAndTeamId(documentId, TEAM_ID))
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
        .containsExactlyInAnyOrder(TimelineEventType.DOCUMENT_UPLOADED, TimelineEventType.SIGNATURE_SENT);
    assertThat(timeline)
        .noneMatch(e -> e.type() == TimelineEventType.SIGNATURE_COMPLETED);
  }

  @Test
  @DisplayName("cross-source events interleave into one timestamp-sorted list, not five concatenated sub-lists")
  void crossSourceInterleaving() {
    when(auditService.getEntityAuditLog(TEAM_ID, "CONTRACT", CONTRACT_ID))
        .thenReturn(
            List.of(
                new RecentActivityResponse(
                    "CONTRACT", Sid.of("CON00000000000000000000001"), "CONTRACT", "CREATE",
                    "Landlord", Instant.parse("2026-01-01T00:00:00Z"), "created"),
                new RecentActivityResponse(
                    "CONTRACT", Sid.of("CON00000000000000000000001"), "CONTRACT", "UPDATE",
                    "Landlord", Instant.parse("2026-03-01T00:00:00Z"), "updated")));
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
                    .previousRentAmount(MoneyAmount.of(100000, "EUR"))
                    .newRentAmount(MoneyAmount.of(105000, "EUR"))
                    .rentAdjustmentType(com.buurman.domain.RentAdjustmentType.FIXED_PERCENTAGE)
                    .status(ExtensionStatus.ACTIVE)
                    .triggerType(TriggerType.MANUAL)
                    .createdAt(Instant.parse("2026-02-01T00:00:00Z")) // between the two audit events
                    .updatedAt(Instant.parse("2026-02-01T00:00:00Z"))
                    .createdBy(UUID.randomUUID())
                    .updatedBy(UUID.randomUUID())
                    .build()));

    List<TimelineEventResponse> timeline = service.getTimeline(contractId(), principal);

    assertThat(timeline).hasSize(3);
    assertThat(timeline).extracting(TimelineEventResponse::timestamp).isSortedAccordingTo(
        java.util.Comparator.reverseOrder());
    assertThat(timeline.get(1).type()).isEqualTo(TimelineEventType.EXTENSION_CREATED);
  }

  @Test
  @DisplayName("a declined extension uses updatedAt for EXTENSION_DECLINED, since there is no declinedAt column")
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
                    .previousRentAmount(MoneyAmount.of(100000, "EUR"))
                    .newRentAmount(MoneyAmount.of(100000, "EUR"))
                    .rentAdjustmentType(com.buurman.domain.RentAdjustmentType.NONE)
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
```

(`MoneyAmount.of(long minorUnits, String currency)` and `ContractExtension`'s exact builder field names are taken from the real domain class already read during planning — verify against `backend/buurman-common/src/main/java/com/buurman/domain/ContractExtension.java` and `MoneyAmount.java` if either doesn't compile as written; the shapes above were confirmed against the real file during this plan's research.)

- [ ] **Step 2: Run it, verify it fails to compile**

Run: `mvn test -pl buurman-core -am -Dtest=ContractTimelineServiceTest`
Expected: compile error — `ContractTimelineService`, `TimelineEventType`, `TimelineEventResponse` don't exist yet.

- [ ] **Step 3: Write `TimelineEventType`**

```java
package com.buurman.domain;

public enum TimelineEventType {
  CONTRACT_CREATED,
  CONTRACT_STATUS_CHANGED,
  RENT_CHANGED,
  EXTENSION_CREATED,
  EXTENSION_ACTIVATED,
  EXTENSION_DECLINED,
  DOCUMENT_UPLOADED,
  DOCUMENT_GENERATED,
  SIGNATURE_SENT,
  SIGNATURE_COMPLETED,
  SIGNATURE_DECLINED,
  AUDIT_OTHER
}
```

- [ ] **Step 4: Write `TimelineEventResponse`**

```java
package com.buurman.dto.response;

import java.time.Instant;
import java.util.Optional;

import com.buurman.domain.Sid;
import com.buurman.domain.TimelineEventType;

public record TimelineEventResponse(
    TimelineEventType type,
    Instant timestamp,
    String title,
    Optional<String> description,
    Optional<Sid> relatedIdentifier) {}
```

(No separate internal `TimelineEvent` domain record — the spec sketched one, but since this is a pure projection with no persistence and no cross-service reuse beyond the controller, the service builds `TimelineEventResponse` directly. Simpler; nothing is lost.)

- [ ] **Step 5: Write `ContractTimelineService`**

```java
package com.buurman.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.buurman.domain.Contract;
import com.buurman.domain.ContractExtension;
import com.buurman.domain.ContractRentPeriod;
import com.buurman.domain.Document;
import com.buurman.domain.Sid;
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
  public List<TimelineEventResponse> getTimeline(ContractIdentifier identifier, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Contract contract = contractRepository.getByIdentifierAndTeamId(identifier, teamId);

    List<TimelineEventResponse> events = new ArrayList<>();
    events.addAll(mapAuditEvents(teamId, contract.getId()));
    events.addAll(mapRentPeriods(teamId, contract.getId()));
    events.addAll(mapExtensions(teamId, contract.getId()));

    List<Document> documents = documentRepository.findByEntityAndTeamId("CONTRACT", contract.getId(), teamId);
    events.addAll(mapDocuments(documents));
    events.addAll(mapSignatureRequests(documents, teamId));

    return events.stream().sorted(Comparator.comparing(TimelineEventResponse::timestamp).reversed()).toList();
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
        type, activity.timestamp(), activity.description().orElse(activity.action()), Optional.empty(), Optional.empty());
  }

  private List<TimelineEventResponse> mapRentPeriods(UUID teamId, UUID contractId) {
    return rentPeriodRepository.findByContractIdAndTeamId(contractId, teamId).stream()
        .map(
            period ->
                new TimelineEventResponse(
                    TimelineEventType.RENT_CHANGED,
                    period.getCreatedAt(),
                    "Rent changed to " + period.getRentAmount() + " effective " + period.getEffectiveFrom(),
                    Optional.empty(),
                    period.getIdentifier()))
        .toList();
  }

  private List<TimelineEventResponse> mapExtensions(UUID teamId, UUID contractId) {
    List<TimelineEventResponse> events = new ArrayList<>();
    for (ContractExtension extension : extensionRepository.findByContractIdAndTeamId(contractId, teamId)) {
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
                extension.getUpdatedAt(), // no dedicated declinedAt column — see plan's Review Focus
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
          default -> { /* PENDING/PARTIALLY_SIGNED/CANCELLED/FAILED produce no second event yet */ }
        }
      }
    }
    return events;
  }
}
```

- [ ] **Step 6: Run the test, verify it passes**

Run: `mvn clean install -DskipTests -pl buurman-core -am && mvn test -pl buurman-core -am -Dtest=ContractTimelineServiceTest`
Expected: 5/5 pass.

- [ ] **Step 7: Commit**

```bash
git add backend/buurman-common/src/main/java/com/buurman/domain/TimelineEventType.java backend/buurman-common/src/main/java/com/buurman/dto/response/TimelineEventResponse.java backend/buurman-core/src/main/java/com/buurman/service/ContractTimelineService.java backend/buurman-core/src/test/java/com/buurman/service/ContractTimelineServiceTest.java
git commit -m "feat(timeline): add ContractTimelineService aggregating five contract-scoped sources"
```

---

## Task 2: OpenAPI + `ContractController` wiring

**Files:**
- Modify: `openapi/src/paths/contracts.yaml` (new `timeline` anchor, mirroring the existing `audit-log` anchor)
- Modify: `openapi/src/app.yaml` (register `/contracts/{identifier}/timeline`, add `TimelineEventResponse`/`TimelineEventType` schemas)
- Modify: `openapi/app.yaml` (regenerated)
- Modify: `backend/buurman-core/src/main/java/com/buurman/controller/ContractController.java`
- Test: extend `ContractControllerTest` if one exists for this controller, else skip a dedicated controller test (matches this branch's established convention for thin, pure-delegation controller methods — see the e-signature work's `SignatureController` precedent, which only got a dedicated test because it was the first controller wired to a brand-new generated interface; this one reuses the already-proven `ContractsApi` generation path).

**Interfaces:**
- Consumes: `ContractTimelineService.getTimeline(ContractIdentifier, UserPrincipal): List<TimelineEventResponse>` (Task 1).
- Produces: generated `ContractsApi.getContractTimeline` method; frontend Orval client function `getContractTimeline` (consumed in Task 3).

- [ ] **Step 1: Add response schemas to `openapi/src/app.yaml`**

Next to the existing `RecentActivityResponse` schema:

```yaml
    TimelineEventResponse:
      type: object
      description: One event in a contract's unified timeline
      properties:
        type:
          type: string
          enum:
            - CONTRACT_CREATED
            - CONTRACT_STATUS_CHANGED
            - RENT_CHANGED
            - EXTENSION_CREATED
            - EXTENSION_ACTIVATED
            - EXTENSION_DECLINED
            - DOCUMENT_UPLOADED
            - DOCUMENT_GENERATED
            - SIGNATURE_SENT
            - SIGNATURE_COMPLETED
            - SIGNATURE_DECLINED
            - AUDIT_OTHER
        timestamp:
          type: string
          format: date-time
        title:
          type: string
        description:
          type: string
        relatedIdentifier:
          type: string
      required:
        - type
        - timestamp
        - title
```

- [ ] **Step 2: Add the path anchor to `openapi/src/paths/contracts.yaml`**

Append, mirroring the existing `audit-log` anchor exactly (same tags, same 400/401/403/404/409/500 response block, same `security`):

```yaml
timeline:
  get:
    tags:
      - Contracts
    summary: Get unified timeline
    description: Get a merged, chronological feed of everything that happened to a contract — audit events, rent changes, extensions, documents, and signature requests
    operationId: getContractTimeline
    parameters:
      - name: identifier
        in: path
        description: Contract identifier
        required: true
        schema:
          $ref: '#/components/schemas/ContractIdentifier'
    responses:
      "200":
        description: OK
        content:
          application/json:
            schema:
              type: array
              items:
                $ref: '#/components/schemas/TimelineEventResponse'
      "400":
        description: Bad request - validation error or malformed input
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ProblemDetail'
      "401":
        description: Unauthorized - missing or invalid JWT token
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ProblemDetail'
      "403":
        description: Forbidden - insufficient permissions
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ProblemDetail'
      "404":
        description: Resource not found
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ProblemDetail'
      "500":
        description: Internal server error
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ProblemDetail'
    security:
      - bearer-jwt: []
```

- [ ] **Step 3: Register the path in `openapi/src/app.yaml`**

Next to the existing `/contracts/{identifier}/audit-log` entry:

```yaml
  /contracts/{identifier}/timeline:
    $ref: 'paths/contracts.yaml#/timeline'
```

- [ ] **Step 4: Bundle and rebuild**

Run: `make bundle-openapi && cd backend && mvn clean install -DskipTests -pl buurman-core -am`
Expected: BUILD SUCCESS; the generated `ContractsApi.java` (under `buurman-core/target/generated-sources`) now declares `getContractTimeline(ContractIdentifier identifier)`.

- [ ] **Step 5: Add the controller method**

Add to `ContractController.java` — a new field `private final ContractTimelineService contractTimelineService;` (added to the existing `@RequiredArgsConstructor` field list, deliberately NOT routed through the already-very-large `ContractService` the way `getContractAuditLog` routes through it — this keeps `ContractTimelineService` a clean, independently-testable seam rather than adding another method to an already massive class):

```java
  @Override
  public List<TimelineEventResponse> getContractTimeline(ContractIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contractTimelineService.getTimeline(identifier, principal);
  }
```

(Add the necessary `import com.buurman.dto.response.TimelineEventResponse;` and `import com.buurman.service.ContractTimelineService;` if not already present via a wildcard-free import list — check the file's existing import style first.)

- [ ] **Step 6: Rebuild and verify**

Run: `mvn clean install -DskipTests -pl buurman-core -am`
Expected: BUILD SUCCESS.

- [ ] **Step 7: Commit**

```bash
git add openapi/src/paths/contracts.yaml openapi/src/app.yaml openapi/app.yaml backend/buurman-core/src/main/java/com/buurman/controller/ContractController.java
git commit -m "feat(timeline): expose GET /contracts/{identifier}/timeline"
```

---

## Task 3: Frontend `ContractTimeline` component + hook

**Files:**
- Create: `frontend/app/src/hooks/useContractTimeline.ts`
- Create: `frontend/app/src/components/contracts/ContractTimeline.tsx`
- Test: `frontend/app/src/components/contracts/__tests__/ContractTimeline.test.tsx`

**Interfaces:**
- Consumes: generated `getContractTimeline(contractIdentifier)` (from Task 2's OpenAPI bundle, after `yarn generate:api`), `TimelineEventResponse`/`TimelineEventType` generated types.
- Produces: `useContractTimeline(contractId)` hook, `<ContractTimeline events={...} isLoading={...} isError={...} />` component — Task 4 wires both into `ContractDetailPage`.

- [ ] **Step 1: Regenerate the frontend API client**

Run: `cd frontend && yarn generate:api`
Expected: `frontend/app/src/generated/api/contracts/contracts.ts` gains `getContractTimeline(identifier)`; `frontend/app/src/generated/models/` gains `TimelineEventResponse`/`TimelineEventType`.

- [ ] **Step 2: Write the hook**

```ts
import { useQuery } from '@tanstack/react-query';
import { getContractTimeline } from '../generated/api/contracts/contracts';
import { queryKeys } from '../lib/queryKeys';

export const useContractTimeline = (contractId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.contracts.timeline(contractId),
    queryFn: () => getContractTimeline(contractId ?? ''),
    enabled: !!contractId,
  });
};
```

Add `timeline: (contractId?: string) => k('contractTimeline', contractId),` to the `contracts` section of `frontend/app/src/lib/queryKeys.ts` (next to the existing `documents`/`auditLog` entries there).

- [ ] **Step 3: Write `ContractTimeline`**

Styled after the real, already-existing `CommunicationsTimeline` component's vertical-rail pattern (`frontend/app/src/components/communications/CommunicationsTimeline.tsx`) — confirmed on this branch during planning, not a fabricated reuse claim: an `<ol>` with a left border rail, absolutely-positioned tinted icon circles per `<li>`, title + relative time row.

```tsx
import { useTranslation } from 'react-i18next';
import {
  CheckCircle2, FileText, FilePlus, PenLine, Repeat, History as HistoryIcon,
} from 'lucide-react';
import { EmptyState, LoadingSpinner } from '@buurman/ui';
import { ErrorMessage } from '@/components/ErrorMessage';
import { useFormatDate } from '@/hooks/useFormatDate';
import type { TimelineEventResponse, TimelineEventType } from '@/generated/models';

interface ContractTimelineProps {
  events: TimelineEventResponse[];
  isLoading: boolean;
  isError?: boolean;
}

const ICON_BY_TYPE: Record<TimelineEventType, React.ComponentType<{ className?: string }>> = {
  CONTRACT_CREATED: FilePlus,
  CONTRACT_STATUS_CHANGED: Repeat,
  RENT_CHANGED: Repeat,
  EXTENSION_CREATED: FileText,
  EXTENSION_ACTIVATED: CheckCircle2,
  EXTENSION_DECLINED: FileText,
  DOCUMENT_UPLOADED: FileText,
  DOCUMENT_GENERATED: FileText,
  SIGNATURE_SENT: PenLine,
  SIGNATURE_COMPLETED: CheckCircle2,
  SIGNATURE_DECLINED: PenLine,
  AUDIT_OTHER: HistoryIcon,
};

const TINT_BY_TYPE: Record<TimelineEventType, string> = {
  CONTRACT_CREATED: 'bg-success-bg text-success-text',
  CONTRACT_STATUS_CHANGED: 'bg-info-bg text-info-text',
  RENT_CHANGED: 'bg-info-bg text-info-text',
  EXTENSION_CREATED: 'bg-primary-50 dark:bg-primary-950 text-primary-600 dark:text-primary-300',
  EXTENSION_ACTIVATED: 'bg-success-bg text-success-text',
  EXTENSION_DECLINED: 'bg-error-bg text-error-text',
  DOCUMENT_UPLOADED: 'bg-primary-50 dark:bg-primary-950 text-primary-600 dark:text-primary-300',
  DOCUMENT_GENERATED: 'bg-primary-50 dark:bg-primary-950 text-primary-600 dark:text-primary-300',
  SIGNATURE_SENT: 'bg-info-bg text-info-text',
  SIGNATURE_COMPLETED: 'bg-success-bg text-success-text',
  SIGNATURE_DECLINED: 'bg-error-bg text-error-text',
  AUDIT_OTHER: 'bg-surface-inset text-text-secondary',
};

export const ContractTimeline = ({ events, isLoading, isError = false }: ContractTimelineProps) => {
  const { t } = useTranslation('contracts');
  const { formatDateTime, formatRelative } = useFormatDate();

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-8">
        <LoadingSpinner />
      </div>
    );
  }

  if (isError) {
    return <ErrorMessage message={t('history.failedToLoad')} />;
  }

  if (events.length === 0) {
    return (
      <EmptyState
        variant="inline"
        icon={<HistoryIcon className="h-8 w-8" />}
        title={t('history.empty')}
        description={t('history.emptyDescription')}
      />
    );
  }

  return (
    <ol className="relative border-l border-border-default ml-3 space-y-6">
      {events.map((event, index) => {
        const Icon = ICON_BY_TYPE[event.type];
        return (
          <li key={`${event.type}-${event.timestamp}-${index}`} className="ml-6">
            <span
              className={`absolute -left-3 flex h-6 w-6 items-center justify-center rounded-full ring-4 ring-surface-card ${TINT_BY_TYPE[event.type]}`}
            >
              <Icon className="h-3.5 w-3.5" />
            </span>
            <div className="flex flex-wrap items-baseline justify-between gap-x-3 gap-y-1">
              <p className="text-sm font-medium text-text-primary">{event.title}</p>
              <time
                dateTime={event.timestamp}
                title={formatDateTime(event.timestamp)}
                className="text-xs text-text-muted"
              >
                {formatRelative(event.timestamp)}
              </time>
            </div>
            {event.description && (
              <p className="mt-1 text-xs text-text-secondary">{event.description}</p>
            )}
          </li>
        );
      })}
    </ol>
  );
};
```

- [ ] **Step 4: Write the component test**

Check `frontend/app/src/components/communications/__tests__/CommunicationsTimeline.test.tsx` first for this codebase's exact test conventions for a timeline-shaped component (render helper, i18n mock setup) and match it.

```tsx
import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { ContractTimeline } from '../ContractTimeline';
import type { TimelineEventResponse } from '@/generated/models';

describe('ContractTimeline', () => {
  it('shows the empty state when there are no events', () => {
    render(<ContractTimeline events={[]} isLoading={false} />);
    expect(screen.getByText(/history\.empty/i)).toBeInTheDocument();
  });

  it('renders one row per event with its title', () => {
    const events: TimelineEventResponse[] = [
      { type: 'CONTRACT_CREATED', timestamp: '2026-01-01T00:00:00Z', title: 'Contract created' },
      { type: 'SIGNATURE_COMPLETED', timestamp: '2026-02-01T00:00:00Z', title: 'Signed: addendum.pdf' },
    ];
    render(<ContractTimeline events={events} isLoading={false} />);
    expect(screen.getByText('Contract created')).toBeInTheDocument();
    expect(screen.getByText('Signed: addendum.pdf')).toBeInTheDocument();
  });

  it('shows the loading spinner while isLoading is true', () => {
    render(<ContractTimeline events={[]} isLoading={true} />);
    expect(screen.queryByText(/history\.empty/i)).not.toBeInTheDocument();
  });
});
```

(Adjust the empty-state text matcher and i18n mocking to match whatever this codebase's real test setup actually does for `useTranslation` — check an existing component test in the same directory for the established i18n-mocking convention before finalizing.)

- [ ] **Step 5: Run the tests**

Run: `cd frontend && yarn test`
Expected: existing tests still pass, plus the 3 new `ContractTimeline` assertions.

- [ ] **Step 6: Commit**

```bash
git add frontend/app/src/hooks/useContractTimeline.ts frontend/app/src/components/contracts/ContractTimeline.tsx frontend/app/src/components/contracts/__tests__/ContractTimeline.test.tsx frontend/app/src/lib/queryKeys.ts
git commit -m "feat(timeline): add ContractTimeline component and hook"
```

---

## Task 4: Wire into `ContractDetailPage` + rename tab label

**Files:**
- Modify: `frontend/app/src/pages/ContractDetailPage.tsx`
- Modify: `frontend/app/src/components/contracts/ContractHistoryTab.tsx` (deleted — fully superseded, per the spec's explicit "replaced, not extended" decision)
- Modify: all 13 `contracts*.json`/`contracts*.properties`-equivalent locale bundles for the frontend (find the actual i18n file format/location first — this codebase's frontend i18n convention, likely `frontend/app/src/locales/{lang}/contracts.json` or similar; confirm the real path before editing)

**Interfaces:**
- Consumes: `useContractTimeline` + `ContractTimeline` (Task 3).

- [ ] **Step 1: Locate the frontend i18n bundle format**

Run: `find frontend/app/src -iname "contracts.json" -o -iname "contracts.*.json" 2>/dev/null | head -5` (or the equivalent for whatever this codebase's actual i18n file layout turns out to be — the backend's `.properties`-per-language convention does NOT apply to the frontend, which uses `react-i18next`; confirm the real structure before editing).

- [ ] **Step 2: Change the `detail.tabs.history` value (not the key) in every locale bundle**

The English value changes from `"History"` to `"Timeline"`; every other language's existing translated value for that same key changes to that language's word for "Timeline" (or is left as a to-be-translated marker if this codebase's i18n parity guard accepts that — check the guard's actual rule before deciding). The key itself (`detail.tabs.history`) stays exactly as-is, since `useTabState`'s persisted URL param is the *tab identifier* (`'history'`), a separate string from this display label, and is NOT touched (see Global Constraints).

- [ ] **Step 3: Swap the tab content in `ContractDetailPage.tsx`**

Replace:
```tsx
import { ContractHistoryTab } from '@/components/contracts/ContractHistoryTab';
```
with:
```tsx
import { ContractTimeline } from '@/components/contracts/ContractTimeline';
import { useContractTimeline } from '@/hooks/useContractTimeline';
```

Add the hook call alongside the other tab-data hooks already in this component:
```tsx
  const {
    data: timelineEvents = [],
    isLoading: timelineLoading,
    isError: timelineError,
  } = useContractTimeline(id);
```

Replace the tab-content render line:
```tsx
        {activeTab === 'history' && <ContractHistoryTab contractId={id} />}
```
with:
```tsx
        {activeTab === 'history' && (
          <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
            <h2 className="text-xl font-semibold text-text-primary mb-4">
              {t('history.title')}
            </h2>
            <ContractTimeline
              events={timelineEvents}
              isLoading={timelineLoading}
              isError={timelineError}
            />
          </div>
        )}
```

(The `activeTab === 'history'` condition and the `'history'` entry in the `useTabState` valid-tabs array are unchanged — only what renders for that tab changes, per Global Constraints. `t('history.title')` also keeps its existing key; only its bundle *value* needs to read "Timeline" instead of "History" if a section heading inside the tab is desired — confirm whether this heading and the tab-label text should share a key or use two separate ones already present in the bundle, and use whichever avoids introducing a genuinely new key where an existing one already serves the purpose.)

- [ ] **Step 4: Delete the superseded component**

Run: `git rm frontend/app/src/components/contracts/ContractHistoryTab.tsx`

Check for any other importer of `ContractHistoryTab` before deleting (`grep -rl "ContractHistoryTab" frontend/app/src`) — if one exists beyond `ContractDetailPage.tsx`, stop and report rather than deleting out from under a second consumer the spec/plan didn't anticipate.

- [ ] **Step 5: Run frontend tests, lint, and typecheck**

Run: `cd frontend && yarn test && yarn lint`
Expected: all pass; no references to the deleted `ContractHistoryTab` remain (a stale import would fail typecheck/build, not just lint — also run `yarn build` if `yarn lint` alone doesn't typecheck non-linted files).

- [ ] **Step 6: Manual verification note**

This task cannot be manually smoke-tested in this environment (no running dev server) — note in the task report that automated tests are the only verification performed, matching how the e-signature work's frontend task handled the same limitation.

- [ ] **Step 7: Commit**

```bash
git add frontend/app/src/pages/ContractDetailPage.tsx
git rm frontend/app/src/components/contracts/ContractHistoryTab.tsx
git add frontend/app/src/locales
git commit -m "feat(timeline): replace ContractHistoryTab with the unified ContractTimeline"
```

---

## Final verification (whole-branch, this plan's scope)

- [ ] Run: `mvn clean install -DskipTests -pl buurman-core -am && mvn test -pl buurman-core -am -Dtest='ContractTimelineServiceTest'`
Expected: 5/5 pass.
- [ ] Run: `cd frontend && yarn test && yarn lint`
Expected: all pass.
- [ ] Run: `make bundle-openapi && git diff --exit-code openapi/app.yaml`
Expected: no diff (bundle already up to date from Task 2).
