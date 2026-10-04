# E-signature Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let a landlord send an already-generated letter/addendum PDF for e-signature from inside Buurman (self-hosted Documenso), track per-signer status, and get the signed PDF back into Documents automatically — gated behind a feature flag, no billing/paid-tier dependency.

**Architecture:** Two new tables (`signature_requests`, `signature_signers`) track a signing round per document. A `SignatureProviderClient` interface with a single `DocumensoClient` implementation talks to a self-hosted Documenso sidecar over its REST API (multipart create → JSON distribute → per-signer webhook events → download completed PDF+certificate). A new `SignatureController` (buurman-letters) exposes create/get endpoints; the existing `WebhookController` (buurman-notifications) gains a `handleDocumensoEvents` method that delegates to a new `SignatureWebhookService`. Everything is gated by a new `esignature_enabled` feature flag (default off).

**Tech Stack:** Java 25 / Spring Boot 4 / JOOQ 3.20 (backend), Spring `RestClient` for the Documenso HTTP calls (same pattern as `DocumentRenderer`→Gotenberg), React 19 / TanStack Query 5 / TypeScript (frontend), self-hosted Documenso (Docker sidecar, AGPL-3.0, Community Edition).

**Spec:** `docs/superpowers/specs/2026-09-29-esignature-integration-design.md`

## Global Constraints

- Every query filters by `team_id`. Never bypass this (CLAUDE.md).
- All `if`/`else`/`for`/`while` bodies use curly braces, no exceptions (CLAUDE.md).
- Use idiomatic `Optional` API — never `if (opt != null)` or unchecked `.get()` (CLAUDE.md).
- Response DTOs expose only `identifier` (Sid), never internal UUIDs (CLAUDE.md).
- Never hard-delete; soft delete via `deleted_at` (CLAUDE.md).
- Set `created_by`/`updated_by` manually in repository INSERT/UPDATE (CLAUDE.md).
- New migration starts at `V077` (current head is `V076__multi_unit_flag_default_on.sql`).
- After any `openapi/src/` edit, run `make bundle-openapi` before touching generated code.
- After any `openapi/app.yaml` change, run `cd frontend && yarn generate:api` before touching frontend code that consumes it.
- Documenso is self-hosted now; the client only knows a `baseUrl` + `apiKey` — switching to Documenso Cloud later is a config change, never a code change (spec's "Provider decision").
- No contract-status transition, no lease-agreement wiring, no billing/paid-tier gating, no tenant-portal embedding — all explicitly out of scope per the spec.

## Review Focus

- **Cross-team document access**: a landlord from Team B passes a Team-A document identifier to `createSignatureRequest` → must 404, not leak or attach to the wrong team's signature request. (Task 6 — `SignatureServiceTest`.)
- **Feature flag disabled**: a team without `esignature_enabled` calls the create endpoint → must be rejected before any provider call or DB write happens. (Task 6 — `SignatureServiceTest`.)
- **Webhook for an unknown/foreign envelope ID**: Documenso (or an attacker) posts a webhook whose `envelopeId` matches no `signature_requests` row → must no-op safely, never throw, never 500. (Task 8 — `SignatureWebhookServiceTest`.)
- **Out-of-order/duplicate webhook delivery**: a late `DOCUMENT_OPENED` event arrives after the signer already reached `SIGNED` → must not regress the stored status. (Task 8 — `SignatureWebhookServiceTest`, mirrors `WebhookServiceTest`'s existing status-rank coverage.)
- **Provider unreachable when sending**: Documenso's `/envelope/create` times out or 5xxs → the request must land as `FAILED` in the database, not stay `PENDING` forever with no signers ever created, and the landlord must see an actionable error rather than a silent no-op. (Task 6 — `SignatureServiceTest`.)

---

## Task 1: Database schema

**Files:**
- Create: `backend/buurman-jooq/src/main/resources/db/migration/V077__esignature.sql`
- Create: `backend/buurman-jooq/src/main/resources/db/migration/V078__esignature_feature_flag.sql`

**Interfaces:**
- Produces: tables `signature_requests`, `signature_signers` (columns below); JOOQ-generated `Tables.SIGNATURE_REQUESTS` / `Tables.SIGNATURE_SIGNERS` (and their `*Record` classes) for Task 2/3 to consume. A new row in `feature_flags` with `key = 'esignature_enabled'`.

- [ ] **Step 1: Write `V077__esignature.sql`**

```sql
CREATE TABLE signature_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(26) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams (id),
    document_id UUID NOT NULL REFERENCES documents (id),
    signed_document_id UUID REFERENCES documents (id),
    provider VARCHAR(32) NOT NULL,
    provider_submission_id VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    deleted_at TIMESTAMP,
    CONSTRAINT uq_signature_requests_team_identifier UNIQUE (team_id, identifier)
);

CREATE INDEX idx_signature_requests_team ON signature_requests (team_id);

CREATE INDEX idx_signature_requests_document ON signature_requests (document_id);

CREATE UNIQUE INDEX idx_signature_requests_provider_submission ON signature_requests (
    provider,
    provider_submission_id
);

CREATE TABLE signature_signers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    signature_request_id UUID NOT NULL REFERENCES signature_requests (id),
    contact_id UUID REFERENCES contacts (id),
    email VARCHAR NOT NULL,
    role VARCHAR(32) NOT NULL,
    provider_signer_id VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    signed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_signature_signers_request ON signature_signers (signature_request_id);
```

- [ ] **Step 2: Write `V078__esignature_feature_flag.sql`**

```sql
INSERT INTO
    feature_flags (
        key,
        value_type,
        default_enabled,
        default_value,
        description
    )
VALUES
    (
        'esignature_enabled',
        'boolean',
        FALSE,
        NULL,
        'E-signature (Documenso) integration for generated letters/addenda'
    );
```

- [ ] **Step 3: Regenerate JOOQ**

Run: `cd backend && mvn generate-sources -pl buurman-jooq -am`
Expected: BUILD SUCCESS; `backend/buurman-jooq/target/generated-sources/jooq/com/buurman/jooq/generated/tables/SignatureRequests.java` and `SignatureSigners.java` (plus their `records` package counterparts) now exist.

- [ ] **Step 4: Full install to confirm nothing downstream broke**

Run: `cd backend && mvn clean install -DskipTests`
Expected: BUILD SUCCESS across all 12 modules.

- [ ] **Step 5: Commit**

```bash
git add backend/buurman-jooq/src/main/resources/db/migration/V077__esignature.sql backend/buurman-jooq/src/main/resources/db/migration/V078__esignature_feature_flag.sql
git commit -m "feat(db): add signature_requests/signature_signers tables and esignature_enabled flag"
```

---

## Task 2: Domain model, identifiers, DTOs (buurman-common)

**Files:**
- Modify: `backend/buurman-common/src/main/java/com/buurman/util/EntityPrefix.java`
- Modify: `backend/buurman-common/src/main/java/com/buurman/util/SidGenerator.java`
- Modify: `backend/buurman-common/src/main/java/com/buurman/util/FeatureFlags.java`
- Create: `backend/buurman-common/src/main/java/com/buurman/domain/identifier/SignatureRequestIdentifier.java`
- Create: `backend/buurman-common/src/main/java/com/buurman/domain/SignatureRequestStatus.java`
- Create: `backend/buurman-common/src/main/java/com/buurman/domain/SignatureSignerStatus.java`
- Create: `backend/buurman-common/src/main/java/com/buurman/domain/SignatureSignerRole.java`
- Create: `backend/buurman-common/src/main/java/com/buurman/domain/SignatureRequest.java`
- Create: `backend/buurman-common/src/main/java/com/buurman/domain/SignatureSigner.java`
- Create: `backend/buurman-common/src/main/java/com/buurman/dto/response/SignatureSignerResponse.java`
- Create: `backend/buurman-common/src/main/java/com/buurman/dto/response/SignatureRequestResponse.java`
- Test: `backend/buurman-common/src/test/java/com/buurman/util/SidGeneratorTest.java` (extend if it exists, else create)

**Interfaces:**
- Produces: `SidGenerator.newSignatureRequestId(): SignatureRequestIdentifier`; `SignatureRequest`/`SignatureSigner` domain classes (Lombok `@Data @Builder`, matching `Document`'s shape) for Task 3's repositories; `SignatureRequestResponse`/`SignatureSignerResponse` records for Task 6/7.

- [ ] **Step 1: Add entity prefixes**

Edit `EntityPrefix.java`, add two enum constants (alphabetical position doesn't matter — the file isn't sorted, e.g. `CEX`/`RCO` are appended at the end):

```java
  CEX("CEX", "Contract Extensions"),
  RCO("RCO", "Rent Components"),
  SGR("SGR", "Signature Requests"),
  SGS("SGS", "Signature Signers");
```

- [ ] **Step 2: Add identifier class**

`SignatureRequestIdentifier.java`:

```java
package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;
import com.fasterxml.jackson.annotation.JsonCreator;

@SkipTestCoverage
public final class SignatureRequestIdentifier extends Sid {

  private SignatureRequestIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static SignatureRequestIdentifier of(String value) {
    return new SignatureRequestIdentifier(value);
  }
}
```

`signature_signers` has no `identifier` column (spec: signers are only ever read nested inside a `SignatureRequestResponse`, never addressed directly) — no `SignatureSignerIdentifier` class needed.

- [ ] **Step 3: Add SidGenerator methods**

Add to `SidGenerator.java` (with the matching import for `SignatureRequestIdentifier`):

```java
  public static SignatureRequestIdentifier newSignatureRequestId() {
    return SignatureRequestIdentifier.of(generateRaw(EntityPrefix.SGR));
  }
```

- [ ] **Step 4: Add enums**

`SignatureRequestStatus.java`:

```java
package com.buurman.domain;

public enum SignatureRequestStatus {
  PENDING,
  PARTIALLY_SIGNED,
  COMPLETED,
  DECLINED,
  CANCELLED,
  FAILED
}
```

`SignatureSignerStatus.java`:

```java
package com.buurman.domain;

public enum SignatureSignerStatus {
  PENDING,
  VIEWED,
  SIGNED,
  DECLINED
}
```

`SignatureSignerRole.java`:

```java
package com.buurman.domain;

public enum SignatureSignerRole {
  LANDLORD,
  TENANT
}
```

- [ ] **Step 5: Add domain classes**

`SignatureRequest.java`:

```java
package com.buurman.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@SuppressWarnings("NullAway.Init")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignatureRequest {

  private UUID id;
  @Builder.Default private Optional<Sid> identifier = Optional.empty();
  private UUID teamId;
  private UUID documentId;
  @Builder.Default private Optional<UUID> signedDocumentId = Optional.empty();
  private String provider;
  private String providerSubmissionId;
  private SignatureRequestStatus status;
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  @Builder.Default private Optional<Instant> deletedAt = Optional.empty();
}
```

`SignatureSigner.java`:

```java
package com.buurman.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@SuppressWarnings("NullAway.Init")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignatureSigner {

  private UUID id;
  private UUID signatureRequestId;
  @Builder.Default private Optional<UUID> contactId = Optional.empty();
  private String email;
  private SignatureSignerRole role;
  private String providerSignerId;
  private SignatureSignerStatus status;
  @Builder.Default private Optional<Instant> signedAt = Optional.empty();
  private Instant createdAt;
  private Instant updatedAt;
}
```

- [ ] **Step 6: Add response DTOs**

`SignatureSignerResponse.java`:

```java
package com.buurman.dto.response;

import java.time.Instant;
import java.util.Optional;

import com.buurman.domain.SignatureSignerRole;
import com.buurman.domain.SignatureSignerStatus;

public record SignatureSignerResponse(
    String email,
    SignatureSignerRole role,
    SignatureSignerStatus status,
    Optional<Instant> signedAt) {}
```

`SignatureRequestResponse.java`:

```java
package com.buurman.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.buurman.domain.Sid;
import com.buurman.domain.SignatureRequestStatus;

public record SignatureRequestResponse(
    Sid identifier,
    Sid documentIdentifier,
    Optional<Sid> signedDocumentIdentifier,
    SignatureRequestStatus status,
    List<SignatureSignerResponse> signers,
    Instant createdAt,
    Instant updatedAt) {}
```

- [ ] **Step 7: Register the feature flag key**

Edit `FeatureFlags.java` — add the constant, append to `ALL_KEYS`, add a `DEFAULTS` entry:

```java
  public static final String ESIGNATURE_ENABLED = "esignature_enabled";
```

Add `ESIGNATURE_ENABLED` to the `List.of(...)` in `ALL_KEYS` and `Map.entry(ESIGNATURE_ENABLED, false)` to `DEFAULTS`.

- [ ] **Step 8: Build**

Run: `cd backend && mvn clean install -DskipTests -pl buurman-common -am`
Expected: BUILD SUCCESS.

- [ ] **Step 9: Commit**

```bash
git add backend/buurman-common
git commit -m "feat(domain): add SignatureRequest/SignatureSigner domain model and DTOs"
```

---

## Task 3: Repositories (buurman-core)

**Files:**
- Create: `backend/buurman-core/src/main/java/com/buurman/mapper/SignatureRequestRecordMapper.java`
- Create: `backend/buurman-core/src/main/java/com/buurman/mapper/SignatureSignerRecordMapper.java`
- Create: `backend/buurman-core/src/main/java/com/buurman/repository/SignatureRequestRepository.java`
- Create: `backend/buurman-core/src/main/java/com/buurman/repository/SignatureSignerRepository.java`
- Test: `backend/buurman-core/src/test/java/com/buurman/repository/SignatureRequestRepositoryIntegrationTest.java`
- Test: `backend/buurman-core/src/test/java/com/buurman/repository/SignatureSignerRepositoryIntegrationTest.java`
- Modify: `backend/buurman-core/src/test/java/com/buurman/repository/AbstractRepositoryIntegrationTest.java` (add cleanup lines)
- Modify: `backend/buurman-core/src/test/java/com/buurman/repository/TestDataHelper.java` (add an `insertDocument` helper if one doesn't already exist — check first)

**Interfaces:**
- Consumes: `Document` domain class + `documents` table (Task 1), `SignatureRequest`/`SignatureSigner` domain classes (Task 2).
- Produces: `SignatureRequestRepository.save/getByIdentifierAndTeamId/findByProviderAndProviderSubmissionId/updateStatus`, `SignatureSignerRepository.save/findBySignatureRequestId/updateStatus` — Task 6 (create flow) and Task 8 (webhook flow) call these directly.

- [ ] **Step 1: Write the mappers**

`SignatureRequestRecordMapper.java`:

```java
package com.buurman.mapper;

import static java.time.ZoneOffset.UTC;

import java.time.Instant;
import java.time.LocalDateTime;

import org.jspecify.annotations.Nullable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.buurman.domain.SignatureRequest;
import com.buurman.domain.SignatureRequestStatus;
import com.buurman.jooq.generated.tables.records.SignatureRequestsRecord;

@Mapper(componentModel = "spring")
public interface SignatureRequestRecordMapper {

  @Mapping(target = "identifier", expression = "java(java.util.Optional.of(record.getIdentifier()))")
  @Mapping(
      target = "signedDocumentId",
      expression = "java(java.util.Optional.ofNullable(record.getSignedDocumentId()))")
  @Mapping(target = "status", expression = "java(SignatureRequestStatus.valueOf(record.getStatus()))")
  @Mapping(target = "createdAt", expression = "java(toInstant(record.getCreatedAt()))")
  @Mapping(target = "updatedAt", expression = "java(toInstant(record.getUpdatedAt()))")
  @Mapping(
      target = "deletedAt",
      expression = "java(java.util.Optional.ofNullable(toInstant(record.getDeletedAt())))")
  SignatureRequest toDomain(SignatureRequestsRecord record);

  default @Nullable Instant toInstant(@Nullable LocalDateTime localDateTime) {
    return localDateTime == null ? null : localDateTime.toInstant(UTC);
  }
}
```

`SignatureSignerRecordMapper.java`:

```java
package com.buurman.mapper;

import static java.time.ZoneOffset.UTC;

import java.time.Instant;
import java.time.LocalDateTime;

import org.jspecify.annotations.Nullable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.buurman.domain.SignatureSigner;
import com.buurman.domain.SignatureSignerStatus;
import com.buurman.jooq.generated.tables.records.SignatureSignersRecord;

@Mapper(componentModel = "spring")
public interface SignatureSignerRecordMapper {

  @Mapping(target = "contactId", expression = "java(java.util.Optional.ofNullable(record.getContactId()))")
  @Mapping(target = "status", expression = "java(SignatureSignerStatus.valueOf(record.getStatus()))")
  @Mapping(
      target = "signedAt",
      expression = "java(java.util.Optional.ofNullable(toInstant(record.getSignedAt())))")
  @Mapping(target = "createdAt", expression = "java(toInstant(record.getCreatedAt()))")
  @Mapping(target = "updatedAt", expression = "java(toInstant(record.getUpdatedAt()))")
  SignatureSigner toDomain(SignatureSignersRecord record);

  default @Nullable Instant toInstant(@Nullable LocalDateTime localDateTime) {
    return localDateTime == null ? null : localDateTime.toInstant(UTC);
  }
}
```

- [ ] **Step 2: Write `SignatureRequestRepository`**

```java
package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.SIGNATURE_REQUESTS;
import static com.buurman.util.SidGenerator.newSignatureRequestId;
import static java.time.ZoneOffset.UTC;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.Sid;
import com.buurman.domain.SignatureRequest;
import com.buurman.exception.NotFoundException;
import com.buurman.mapper.SignatureRequestRecordMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SignatureRequestRepository {

  private final DSLContext dsl;
  private final SignatureRequestRecordMapper mapper;
  private final Clock clock;

  public SignatureRequest save(SignatureRequest request) {
    LocalDateTime now = LocalDateTime.now(clock);

    if (request.getId() == null) {
      UUID newId = UUID.randomUUID();
      Sid identifier = newSignatureRequestId();

      dsl.insertInto(SIGNATURE_REQUESTS)
          .set(SIGNATURE_REQUESTS.ID, newId)
          .set(SIGNATURE_REQUESTS.IDENTIFIER, identifier)
          .set(SIGNATURE_REQUESTS.TEAM_ID, request.getTeamId())
          .set(SIGNATURE_REQUESTS.DOCUMENT_ID, request.getDocumentId())
          .set(SIGNATURE_REQUESTS.SIGNED_DOCUMENT_ID, request.getSignedDocumentId().orElse(null))
          .set(SIGNATURE_REQUESTS.PROVIDER, request.getProvider())
          .set(SIGNATURE_REQUESTS.PROVIDER_SUBMISSION_ID, request.getProviderSubmissionId())
          .set(SIGNATURE_REQUESTS.STATUS, request.getStatus().name())
          .set(SIGNATURE_REQUESTS.CREATED_AT, now)
          .set(SIGNATURE_REQUESTS.UPDATED_AT, now)
          .set(SIGNATURE_REQUESTS.CREATED_BY, request.getCreatedBy())
          .set(SIGNATURE_REQUESTS.UPDATED_BY, request.getUpdatedBy())
          .execute();

      request.setId(newId);
      request.setIdentifier(Optional.of(identifier));
      request.setCreatedAt(now.toInstant(UTC));
      request.setUpdatedAt(now.toInstant(UTC));
    } else {
      dsl.update(SIGNATURE_REQUESTS)
          .set(SIGNATURE_REQUESTS.SIGNED_DOCUMENT_ID, request.getSignedDocumentId().orElse(null))
          .set(SIGNATURE_REQUESTS.STATUS, request.getStatus().name())
          .set(SIGNATURE_REQUESTS.UPDATED_AT, now)
          .set(SIGNATURE_REQUESTS.UPDATED_BY, request.getUpdatedBy())
          .where(
              SIGNATURE_REQUESTS.ID.eq(request.getId()).and(SIGNATURE_REQUESTS.TEAM_ID.eq(request.getTeamId())))
          .execute();
      request.setUpdatedAt(now.toInstant(UTC));
    }

    return request;
  }

  public Optional<SignatureRequest> findByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return dsl.selectFrom(SIGNATURE_REQUESTS)
        .where(
            SIGNATURE_REQUESTS
                .IDENTIFIER
                .eq(identifier)
                .and(SIGNATURE_REQUESTS.TEAM_ID.eq(teamId))
                .and(SIGNATURE_REQUESTS.DELETED_AT.isNull()))
        .fetchOptional()
        .map(mapper::toDomain);
  }

  public SignatureRequest getByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return findByIdentifierAndTeamId(identifier, teamId)
        .orElseThrow(() -> new NotFoundException("Signature request not found"));
  }

  /** Team-agnostic lookup used only by webhook processing, which has no authenticated team context. */
  public Optional<SignatureRequest> findByProviderAndProviderSubmissionId(
      String provider, String providerSubmissionId) {
    return dsl.selectFrom(SIGNATURE_REQUESTS)
        .where(
            SIGNATURE_REQUESTS
                .PROVIDER
                .eq(provider)
                .and(SIGNATURE_REQUESTS.PROVIDER_SUBMISSION_ID.eq(providerSubmissionId))
                .and(SIGNATURE_REQUESTS.DELETED_AT.isNull()))
        .fetchOptional()
        .map(mapper::toDomain);
  }

  public List<SignatureRequest> findByDocumentIdAndTeamId(UUID documentId, UUID teamId) {
    return List.copyOf(
        dsl.selectFrom(SIGNATURE_REQUESTS)
            .where(
                SIGNATURE_REQUESTS
                    .DOCUMENT_ID
                    .eq(documentId)
                    .and(SIGNATURE_REQUESTS.TEAM_ID.eq(teamId))
                    .and(SIGNATURE_REQUESTS.DELETED_AT.isNull()))
            .orderBy(SIGNATURE_REQUESTS.CREATED_AT.desc())
            .fetch()
            .map(mapper::toDomain));
  }
}
```

- [ ] **Step 3: Write `SignatureSignerRepository`**

```java
package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.SIGNATURE_SIGNERS;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.SignatureSigner;
import com.buurman.domain.SignatureSignerStatus;
import com.buurman.mapper.SignatureSignerRecordMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SignatureSignerRepository {

  private final DSLContext dsl;
  private final SignatureSignerRecordMapper mapper;
  private final Clock clock;

  public SignatureSigner save(SignatureSigner signer) {
    LocalDateTime now = LocalDateTime.now(clock);
    UUID newId = UUID.randomUUID();

    dsl.insertInto(SIGNATURE_SIGNERS)
        .set(SIGNATURE_SIGNERS.ID, newId)
        .set(SIGNATURE_SIGNERS.SIGNATURE_REQUEST_ID, signer.getSignatureRequestId())
        .set(SIGNATURE_SIGNERS.CONTACT_ID, signer.getContactId().orElse(null))
        .set(SIGNATURE_SIGNERS.EMAIL, signer.getEmail())
        .set(SIGNATURE_SIGNERS.ROLE, signer.getRole().name())
        .set(SIGNATURE_SIGNERS.PROVIDER_SIGNER_ID, signer.getProviderSignerId())
        .set(SIGNATURE_SIGNERS.STATUS, signer.getStatus().name())
        .set(SIGNATURE_SIGNERS.CREATED_AT, now)
        .set(SIGNATURE_SIGNERS.UPDATED_AT, now)
        .execute();

    signer.setId(newId);
    signer.setCreatedAt(now.toInstant(java.time.ZoneOffset.UTC));
    signer.setUpdatedAt(now.toInstant(java.time.ZoneOffset.UTC));
    return signer;
  }

  public List<SignatureSigner> findBySignatureRequestId(UUID signatureRequestId) {
    return List.copyOf(
        dsl.selectFrom(SIGNATURE_SIGNERS)
            .where(SIGNATURE_SIGNERS.SIGNATURE_REQUEST_ID.eq(signatureRequestId))
            .orderBy(SIGNATURE_SIGNERS.CREATED_AT.asc())
            .fetch()
            .map(mapper::toDomain));
  }

  public void updateStatus(UUID id, SignatureSignerStatus status, Optional<Instant> signedAt) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(SIGNATURE_SIGNERS)
        .set(SIGNATURE_SIGNERS.STATUS, status.name())
        .set(
            SIGNATURE_SIGNERS.SIGNED_AT,
            signedAt.map(i -> LocalDateTime.ofInstant(i, java.time.ZoneOffset.UTC)).orElse(null))
        .set(SIGNATURE_SIGNERS.UPDATED_AT, now)
        .where(SIGNATURE_SIGNERS.ID.eq(id))
        .execute();
  }
}
```

- [ ] **Step 4: Confirm `TestDataHelper.insertDocument` exists, add it if not**

Run: `grep -n "insertDocument" backend/buurman-core/src/test/java/com/buurman/repository/TestDataHelper.java`

If it prints nothing, add (following the exact shape of `insertContact`/`insertExpense` in the same file — team-scoped INSERT returning the new row's `UUID`, using `identifier`/`entity_type='CONTRACT'`/`file_key`/`file_name`/`mime_type`/`uploaded_by` columns from `V005__documents_and_media.sql`):

```java
  static UUID insertDocument(DSLContext dsl, UUID teamId, UUID entityId, UUID uploadedBy) {
    UUID id = UUID.randomUUID();
    dsl.insertInto(DSL.table("documents"))
        .set(DSL.field("id", UUID.class), id)
        .set(DSL.field("identifier", String.class), SidGenerator.newDocumentId().value())
        .set(DSL.field("team_id", UUID.class), teamId)
        .set(DSL.field("entity_type", String.class), "CONTRACT")
        .set(DSL.field("entity_id", UUID.class), entityId)
        .set(DSL.field("file_key", String.class), "test/" + id + ".pdf")
        .set(DSL.field("file_name", String.class), "test.pdf")
        .set(DSL.field("file_size", Long.class), 1024L)
        .set(DSL.field("mime_type", String.class), "application/pdf")
        .set(DSL.field("uploaded_by", UUID.class), uploadedBy)
        .set(DSL.field("uploaded_at", LocalDateTime.class), LocalDateTime.now())
        .execute();
    return id;
  }
```

(Add `import com.buurman.util.SidGenerator;` and `import java.time.LocalDateTime;` if not already present in the file.)

- [ ] **Step 5: Add cleanup lines to `AbstractRepositoryIntegrationTest`**

Insert before the existing `dsl.deleteFrom(DSL.table("documents")).execute();` line (FK order: signers reference requests, requests reference documents):

```java
    dsl.deleteFrom(DSL.table("signature_signers")).execute();
    dsl.deleteFrom(DSL.table("signature_requests")).execute();
```

- [ ] **Step 6: Write `SignatureRequestRepositoryIntegrationTest`**

```java
package com.buurman.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.buurman.domain.SignatureRequest;
import com.buurman.domain.SignatureRequestStatus;
import com.buurman.mapper.SignatureRequestRecordMapperImpl;

@DisplayName("SignatureRequestRepository")
class SignatureRequestRepositoryIntegrationTest extends AbstractRepositoryIntegrationTest {

  private SignatureRequestRepository repository;
  private UUID teamADocumentId;
  private UUID teamBDocumentId;

  @BeforeEach
  void setUp() {
    repository = new SignatureRequestRepository(dsl, new SignatureRequestRecordMapperImpl(), CLOCK);
    UUID teamAPropertyId = TestDataHelper.insertProperty(dsl, TEAM_A_ID, USER_ID);
    UUID teamBPropertyId = TestDataHelper.insertProperty(dsl, TEAM_B_ID, USER_ID);
    UUID teamAContractId = TestDataHelper.insertContract(dsl, TEAM_A_ID, teamAPropertyId, USER_ID);
    UUID teamBContractId = TestDataHelper.insertContract(dsl, TEAM_B_ID, teamBPropertyId, USER_ID);
    teamADocumentId = TestDataHelper.insertDocument(dsl, TEAM_A_ID, teamAContractId, USER_ID);
    teamBDocumentId = TestDataHelper.insertDocument(dsl, TEAM_B_ID, teamBContractId, USER_ID);
  }

  private SignatureRequest newRequest(UUID teamId, UUID documentId) {
    return SignatureRequest.builder()
        .teamId(teamId)
        .documentId(documentId)
        .provider("documenso")
        .providerSubmissionId("envelope_" + UUID.randomUUID())
        .status(SignatureRequestStatus.PENDING)
        .createdBy(USER_ID)
        .updatedBy(USER_ID)
        .build();
  }

  @Test
  @DisplayName("saves and re-reads a request, and a team-B lookup by a team-A identifier finds nothing")
  void teamIsolation() {
    SignatureRequest saved = repository.save(newRequest(TEAM_A_ID, teamADocumentId));
    assertThat(saved.getIdentifier()).isPresent();

    var foundOwnTeam =
        repository.findByIdentifierAndTeamId(saved.getIdentifier().orElseThrow(), TEAM_A_ID);
    assertThat(foundOwnTeam).isPresent();

    var foundWrongTeam =
        repository.findByIdentifierAndTeamId(saved.getIdentifier().orElseThrow(), TEAM_B_ID);
    assertThat(foundWrongTeam).isEmpty();
  }

  @Test
  @DisplayName("findByProviderAndProviderSubmissionId is team-agnostic, for webhook processing")
  void findsByProviderSubmissionIdAcrossTeams() {
    SignatureRequest saved = repository.save(newRequest(TEAM_B_ID, teamBDocumentId));

    var found =
        repository.findByProviderAndProviderSubmissionId("documenso", saved.getProviderSubmissionId());

    assertThat(found).isPresent();
    assertThat(found.orElseThrow().getTeamId()).isEqualTo(TEAM_B_ID);
  }

  @Test
  @DisplayName("save() on an existing request updates status and signedDocumentId in place")
  void updateExistingRequest() {
    SignatureRequest saved = repository.save(newRequest(TEAM_A_ID, teamADocumentId));

    saved.setStatus(SignatureRequestStatus.COMPLETED);
    saved.setSignedDocumentId(java.util.Optional.of(teamADocumentId));
    repository.save(saved);

    SignatureRequest reloaded =
        repository.getByIdentifierAndTeamId(saved.getIdentifier().orElseThrow(), TEAM_A_ID);
    assertThat(reloaded.getStatus()).isEqualTo(SignatureRequestStatus.COMPLETED);
    assertThat(reloaded.getSignedDocumentId()).contains(teamADocumentId);
  }
}
```

- [ ] **Step 7: Run it, verify pass**

Run: `cd backend && mvn clean install -DskipTests -pl buurman-core -am && mvn test -pl buurman-core -am -Dtest=SignatureRequestRepositoryIntegrationTest`
Expected: 3 tests pass. (Docker must be running — Testcontainers starts a `postgres:18-alpine` container.)

- [ ] **Step 8: Write `SignatureSignerRepositoryIntegrationTest`**

```java
package com.buurman.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.buurman.domain.SignatureRequest;
import com.buurman.domain.SignatureRequestStatus;
import com.buurman.domain.SignatureSigner;
import com.buurman.domain.SignatureSignerRole;
import com.buurman.domain.SignatureSignerStatus;
import com.buurman.mapper.SignatureRequestRecordMapperImpl;
import com.buurman.mapper.SignatureSignerRecordMapperImpl;

@DisplayName("SignatureSignerRepository")
class SignatureSignerRepositoryIntegrationTest extends AbstractRepositoryIntegrationTest {

  private SignatureSignerRepository repository;
  private UUID signatureRequestId;

  @BeforeEach
  void setUp() {
    repository = new SignatureSignerRepository(dsl, new SignatureSignerRecordMapperImpl(), CLOCK);
    SignatureRequestRepository requestRepository =
        new SignatureRequestRepository(dsl, new SignatureRequestRecordMapperImpl(), CLOCK);
    UUID propertyId = TestDataHelper.insertProperty(dsl, TEAM_A_ID, USER_ID);
    UUID contractId = TestDataHelper.insertContract(dsl, TEAM_A_ID, propertyId, USER_ID);
    UUID documentId = TestDataHelper.insertDocument(dsl, TEAM_A_ID, contractId, USER_ID);
    SignatureRequest request =
        requestRepository.save(
            SignatureRequest.builder()
                .teamId(TEAM_A_ID)
                .documentId(documentId)
                .provider("documenso")
                .providerSubmissionId("envelope_test")
                .status(SignatureRequestStatus.PENDING)
                .createdBy(USER_ID)
                .updatedBy(USER_ID)
                .build());
    signatureRequestId = request.getId();
  }

  @Test
  @DisplayName("saves signers and lists them back in creation order")
  void savesAndLists() {
    SignatureSigner landlord =
        repository.save(
            SignatureSigner.builder()
                .signatureRequestId(signatureRequestId)
                .email("landlord@example.com")
                .role(SignatureSignerRole.LANDLORD)
                .providerSignerId("1")
                .status(SignatureSignerStatus.PENDING)
                .build());
    SignatureSigner tenant =
        repository.save(
            SignatureSigner.builder()
                .signatureRequestId(signatureRequestId)
                .email("tenant@example.com")
                .role(SignatureSignerRole.TENANT)
                .providerSignerId("2")
                .status(SignatureSignerStatus.PENDING)
                .build());

    var signers = repository.findBySignatureRequestId(signatureRequestId);
    assertThat(signers).extracting(SignatureSigner::getId).containsExactly(landlord.getId(), tenant.getId());
  }

  @Test
  @DisplayName("updateStatus sets status and signedAt")
  void updateStatusSetsSignedAt() {
    SignatureSigner signer =
        repository.save(
            SignatureSigner.builder()
                .signatureRequestId(signatureRequestId)
                .email("tenant@example.com")
                .role(SignatureSignerRole.TENANT)
                .providerSignerId("3")
                .status(SignatureSignerStatus.PENDING)
                .build());

    Instant signedAt = Instant.parse("2026-03-01T13:00:00Z");
    repository.updateStatus(signer.getId(), SignatureSignerStatus.SIGNED, Optional.of(signedAt));

    var reloaded = repository.findBySignatureRequestId(signatureRequestId);
    assertThat(reloaded).hasSize(1);
    assertThat(reloaded.get(0).getStatus()).isEqualTo(SignatureSignerStatus.SIGNED);
    assertThat(reloaded.get(0).getSignedAt()).contains(signedAt);
  }
}
```

- [ ] **Step 9: Run both integration tests, verify pass**

Run: `mvn test -pl buurman-core -am -Dtest='Signature*RepositoryIntegrationTest'`
Expected: 5 tests pass total.

- [ ] **Step 10: Commit**

```bash
git add backend/buurman-core/src/main/java/com/buurman/mapper/SignatureRequestRecordMapper.java backend/buurman-core/src/main/java/com/buurman/mapper/SignatureSignerRecordMapper.java backend/buurman-core/src/main/java/com/buurman/repository/SignatureRequestRepository.java backend/buurman-core/src/main/java/com/buurman/repository/SignatureSignerRepository.java backend/buurman-core/src/test/java/com/buurman/repository/SignatureRequestRepositoryIntegrationTest.java backend/buurman-core/src/test/java/com/buurman/repository/SignatureSignerRepositoryIntegrationTest.java backend/buurman-core/src/test/java/com/buurman/repository/AbstractRepositoryIntegrationTest.java backend/buurman-core/src/test/java/com/buurman/repository/TestDataHelper.java
git commit -m "feat(esignature): add SignatureRequest/SignatureSigner repositories with team-isolation tests"
```

---

## Task 4: Provider abstraction + Documenso config (buurman-core)

**Files:**
- Create: `backend/buurman-core/src/main/java/com/buurman/service/esignature/SignerRequest.java`
- Create: `backend/buurman-core/src/main/java/com/buurman/service/esignature/ProviderSigner.java`
- Create: `backend/buurman-core/src/main/java/com/buurman/service/esignature/SignatureSubmission.java`
- Create: `backend/buurman-core/src/main/java/com/buurman/service/esignature/SignedDocument.java`
- Create: `backend/buurman-core/src/main/java/com/buurman/service/esignature/SignatureProviderClient.java`
- Create: `backend/buurman-core/src/main/java/com/buurman/config/models/DocumensoProperties.java`
- Create: `backend/buurman-core/src/main/java/com/buurman/config/DocumensoConfig.java`

**Interfaces:**
- Produces: `SignatureProviderClient` interface — Task 5's `DocumensoClient` implements it; Task 6's `SignatureService` depends on it (by interface, not the concrete class) so a future second provider only needs a new `@Component` implementation.

- [ ] **Step 1: Write the small value records**

`SignerRequest.java`:

```java
package com.buurman.service.esignature;

import com.buurman.domain.SignatureSignerRole;

public record SignerRequest(String email, String name, SignatureSignerRole role) {}
```

`ProviderSigner.java`:

```java
package com.buurman.service.esignature;

public record ProviderSigner(String providerSignerId, String email) {}
```

`SignatureSubmission.java`:

```java
package com.buurman.service.esignature;

import java.util.List;

public record SignatureSubmission(String providerSubmissionId, List<ProviderSigner> signers) {}
```

`SignedDocument.java`:

```java
package com.buurman.service.esignature;

public record SignedDocument(byte[] signedPdfBytes, byte[] certificatePdfBytes) {}
```

- [ ] **Step 2: Write the interface**

```java
package com.buurman.service.esignature;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * A provider capable of collecting e-signatures on a PDF. The only implementation today is {@link
 * DocumensoClient}; a second provider would add another {@code @Component} implementing this
 * interface, never a change to callers.
 */
public interface SignatureProviderClient {

  /** Uploads the PDF, adds the given signers, and sends it for signature (all in parallel). */
  SignatureSubmission createSubmission(byte[] pdfBytes, String fileName, List<SignerRequest> signers);

  /** Fetches the final signed PDF and its audit certificate once every signer has signed. */
  SignedDocument downloadCompleted(String providerSubmissionId);

  /** Validates a webhook's shared-secret header against the configured secret. */
  boolean isValidWebhookSecret(@Nullable String providedSecret);
}
```

- [ ] **Step 3: Write `DocumensoProperties`**

```java
package com.buurman.config.models;

import java.util.Optional;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.buurman.util.SkipTestCoverage;

@ConfigurationProperties(prefix = "documenso")
@SkipTestCoverage
public record DocumensoProperties(String baseUrl, String apiKey, Optional<String> webhookSecret) {

  public DocumensoProperties {
    webhookSecret = Optional.ofNullable(webhookSecret).flatMap(o -> o);
  }
}
```

- [ ] **Step 4: Write `DocumensoConfig`**

Unlike `MailgunConfig`/`TwilioConfig` (`@Profile("!local")` — those SaaS APIs have no local equivalent, local dev sends mail via Mailpit's SMTP instead), Documenso runs as a real Docker sidecar in every profile including `local` (Task 10), so this bean must be active everywhere — same as `DocumentRenderer`'s un-gated Gotenberg config.

```java
package com.buurman.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.buurman.config.models.DocumensoProperties;

@Configuration
public class DocumensoConfig {

  @Bean
  public RestClient documensoRestClient(DocumensoProperties properties) {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofSeconds(5));
    factory.setReadTimeout(Duration.ofSeconds(30));
    return RestClient.builder()
        .baseUrl(properties.baseUrl() + "/api/v2")
        .defaultHeader("Authorization", properties.apiKey())
        .requestFactory(factory)
        .build();
  }
}
```

- [ ] **Step 5: Add config to `application.yml`**

Insert after the `twilio:` block in `backend/buurman-app/src/main/resources/application.yml`:

```yaml
# Documenso Configuration (self-hosted e-signature sidecar; see docker-compose.yml)
documenso:
  base-url: ${DOCUMENSO_BASE_URL:http://localhost:3001}
  api-key: ${DOCUMENSO_API_KEY:}
  webhook-secret: ${DOCUMENSO_WEBHOOK_SECRET:}
```

- [ ] **Step 6: Build**

Run: `cd backend && mvn clean install -DskipTests -pl buurman-core -am`
Expected: BUILD SUCCESS.

- [ ] **Step 7: Commit**

```bash
git add backend/buurman-core/src/main/java/com/buurman/service/esignature backend/buurman-core/src/main/java/com/buurman/config/models/DocumensoProperties.java backend/buurman-core/src/main/java/com/buurman/config/DocumensoConfig.java backend/buurman-app/src/main/resources/application.yml
git commit -m "feat(esignature): add SignatureProviderClient abstraction and Documenso config"
```

---

## Task 5: DocumensoClient implementation

**Files:**
- Create: `backend/buurman-core/src/main/java/com/buurman/service/esignature/DocumensoClient.java`
- Test: `backend/buurman-core/src/test/java/com/buurman/service/esignature/DocumensoClientTest.java`

**Interfaces:**
- Consumes: `SignatureProviderClient` (Task 4), `RestClient` bean named `documensoRestClient` (Task 4).
- Produces: `DocumensoClient` — the `@Component` Task 6's `SignatureService` autowires by interface.

Grounded against Documenso's real public API (fetched from `documenso/documenso` on GitHub, not guessed): `POST /envelope/create` (multipart: `payload` JSON part + `files` part) returns `{"id": "envelope_..."}`; `POST /envelope/distribute {"envelopeId": "..."}` returns `{"success": true, "id": "...", "recipients": [{"id": 1, "email": "...", ...}]}`; `GET /envelope/{envelopeId}` returns the full envelope including `envelopeItems: [{"id": "envelope_item_..."}]`; `GET /envelope/item/{envelopeItemId}/download?version=signed` streams the signed PDF; `GET /envelope/{envelopeId}/certificate/download` streams the signing certificate PDF. Webhook secret verification is a plain constant-time string compare against the `X-Documenso-Secret` header value — Documenso does not use HMAC.

- [ ] **Step 1: Write `DocumensoClient`**

```java
package com.buurman.service.esignature;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import com.buurman.config.models.DocumensoProperties;
import com.buurman.exception.ExternalServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class DocumensoClient implements SignatureProviderClient {

  private final RestClient client;
  private final ObjectMapper objectMapper;
  private final @Nullable String webhookSecret;

  public DocumensoClient(
      RestClient documensoRestClient, ObjectMapper objectMapper, DocumensoProperties properties) {
    this.client = documensoRestClient;
    this.objectMapper = objectMapper;
    this.webhookSecret = properties.webhookSecret().orElse(null);
  }

  @Override
  public SignatureSubmission createSubmission(
      byte[] pdfBytes, String fileName, List<SignerRequest> signers) {
    try {
      String envelopeId = createEnvelope(pdfBytes, fileName, signers);
      return distributeEnvelope(envelopeId);
    } catch (ExternalServiceException e) {
      throw e;
    } catch (Exception e) {
      throw new ExternalServiceException("Documenso submission failed", e);
    }
  }

  private String createEnvelope(byte[] pdfBytes, String fileName, List<SignerRequest> signers)
      throws Exception {
    List<Map<String, Object>> recipients =
        signers.stream()
            .map(s -> Map.<String, Object>of("email", s.email(), "name", s.name(), "role", "SIGNER"))
            .toList();
    Map<String, Object> payload = Map.of("type", "DOCUMENT", "title", fileName, "recipients", recipients);

    MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
    parts.add("payload", objectMapper.writeValueAsString(payload));
    parts.add(
        "files",
        new ByteArrayResource(pdfBytes) {
          @Override
          public String getFilename() {
            return fileName;
          }
        });

    JsonNode response =
        client
            .post()
            .uri("/envelope/create")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(parts)
            .retrieve()
            .body(JsonNode.class);

    String envelopeId = response == null ? null : response.path("id").asText(null);
    if (envelopeId == null || envelopeId.isEmpty()) {
      throw new ExternalServiceException("Documenso did not return an envelope id");
    }
    return envelopeId;
  }

  private SignatureSubmission distributeEnvelope(String envelopeId) {
    JsonNode response =
        client
            .post()
            .uri("/envelope/distribute")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("envelopeId", envelopeId))
            .retrieve()
            .body(JsonNode.class);

    if (response == null || !response.path("success").asBoolean(false)) {
      throw new ExternalServiceException("Documenso failed to distribute envelope " + envelopeId);
    }

    List<ProviderSigner> providerSigners = new ArrayList<>();
    for (JsonNode recipient : response.path("recipients")) {
      providerSigners.add(
          new ProviderSigner(recipient.path("id").asText(), recipient.path("email").asText()));
    }
    return new SignatureSubmission(envelopeId, providerSigners);
  }

  @Override
  public SignedDocument downloadCompleted(String providerSubmissionId) {
    try {
      JsonNode envelope =
          client.get().uri("/envelope/{envelopeId}", providerSubmissionId).retrieve().body(JsonNode.class);
      if (envelope == null) {
        throw new ExternalServiceException("Documenso envelope not found: " + providerSubmissionId);
      }

      String envelopeItemId = envelope.path("envelopeItems").path(0).path("id").asText(null);
      if (envelopeItemId == null) {
        throw new ExternalServiceException("Documenso envelope has no items: " + providerSubmissionId);
      }

      byte[] signedPdf =
          client
              .get()
              .uri(
                  uriBuilder ->
                      uriBuilder
                          .path("/envelope/item/{envelopeItemId}/download")
                          .queryParam("version", "signed")
                          .build(envelopeItemId))
              .retrieve()
              .body(byte[].class);
      byte[] certificate =
          client
              .get()
              .uri("/envelope/{envelopeId}/certificate/download", providerSubmissionId)
              .retrieve()
              .body(byte[].class);

      if (signedPdf == null || signedPdf.length == 0) {
        throw new ExternalServiceException("Documenso returned an empty signed document");
      }
      return new SignedDocument(signedPdf, certificate == null ? new byte[0] : certificate);
    } catch (ExternalServiceException e) {
      throw e;
    } catch (Exception e) {
      throw new ExternalServiceException("Documenso download failed for " + providerSubmissionId, e);
    }
  }

  @Override
  public boolean isValidWebhookSecret(@Nullable String providedSecret) {
    if (webhookSecret == null || webhookSecret.isBlank()) {
      return true; // no secret configured (e.g. local dev) — matches Mailgun/Twilio's unconfigured behavior
    }
    if (providedSecret == null) {
      return false;
    }
    return MessageDigest.isEqual(
        providedSecret.getBytes(StandardCharsets.UTF_8), webhookSecret.getBytes(StandardCharsets.UTF_8));
  }
}
```

- [ ] **Step 2: Write the failing test first (throwaway local HTTP server, no mocking library — same pattern as `DocumentRendererTest`)**

```java
package com.buurman.service.esignature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.buurman.config.models.DocumensoProperties;
import com.buurman.domain.SignatureSignerRole;
import com.buurman.exception.ExternalServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

@DisplayName("DocumensoClient")
class DocumensoClientTest {

  private HttpServer server;
  private volatile @Nullable String capturedCreateBody;
  private volatile int createStatus = 200;
  private volatile String createResponse = "{\"id\":\"envelope_abc123\"}";
  private volatile String distributeResponse =
      "{\"success\":true,\"id\":\"envelope_abc123\",\"recipients\":"
          + "[{\"id\":1,\"email\":\"tenant@example.com\"}]}";

  @BeforeEach
  void startServer() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/api/v2/envelope/create",
        exchange -> {
          capturedCreateBody =
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.ISO_8859_1);
          byte[] body = createResponse.getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(createStatus, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    server.createContext(
        "/api/v2/envelope/distribute",
        exchange -> {
          byte[] body = distributeResponse.getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    server.createContext(
        "/api/v2/envelope/envelope_abc123",
        exchange -> {
          String body = "{\"envelopeItems\":[{\"id\":\"envelope_item_1\"}]}";
          byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, bytes.length);
          exchange.getResponseBody().write(bytes);
          exchange.close();
        });
    server.createContext(
        "/api/v2/envelope/item/envelope_item_1/download",
        exchange -> {
          byte[] pdf = "%PDF-1.7\nsigned".getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, pdf.length);
          exchange.getResponseBody().write(pdf);
          exchange.close();
        });
    server.createContext(
        "/api/v2/envelope/envelope_abc123/certificate/download",
        exchange -> {
          byte[] pdf = "%PDF-1.7\ncertificate".getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, pdf.length);
          exchange.getResponseBody().write(pdf);
          exchange.close();
        });
    server.start();
  }

  @AfterEach
  void stopServer() {
    server.stop(0);
  }

  private DocumensoClient client(String webhookSecret) {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofSeconds(2));
    factory.setReadTimeout(Duration.ofSeconds(5));
    String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    RestClient restClient =
        RestClient.builder().baseUrl(baseUrl + "/api/v2").requestFactory(factory).build();
    return new DocumensoClient(
        restClient, new ObjectMapper(), new DocumensoProperties(baseUrl, "api_test", Optional.of(webhookSecret)));
  }

  @Test
  @DisplayName("createSubmission posts multipart payload+files, then distributes, and returns provider signers")
  void createSubmissionSendsAndDistributes() {
    SignatureSubmission submission =
        client("secret")
            .createSubmission(
                "%PDF-1.7\ndoc".getBytes(StandardCharsets.UTF_8),
                "addendum.pdf",
                List.of(new SignerRequest("tenant@example.com", "Jane Tenant", SignatureSignerRole.TENANT)));

    assertThat(submission.providerSubmissionId()).isEqualTo("envelope_abc123");
    assertThat(submission.signers()).hasSize(1);
    assertThat(submission.signers().get(0).email()).isEqualTo("tenant@example.com");
    assertThat(capturedCreateBody).contains("addendum.pdf").contains("\"role\":\"SIGNER\"");
  }

  @Test
  @DisplayName("distribute failure (success:false) maps to ExternalServiceException")
  void distributeFailureMapsToExternalServiceException() {
    distributeResponse = "{\"success\":false}";
    assertThatThrownBy(
            () ->
                client("secret")
                    .createSubmission(
                        "%PDF".getBytes(StandardCharsets.UTF_8),
                        "x.pdf",
                        List.of(new SignerRequest("a@example.com", "A", SignatureSignerRole.TENANT))))
        .isInstanceOf(ExternalServiceException.class);
  }

  @Test
  @DisplayName("create HTTP error maps to ExternalServiceException")
  void createHttpErrorMapsToExternalServiceException() {
    createStatus = 503;
    createResponse = "service unavailable";
    assertThatThrownBy(
            () ->
                client("secret")
                    .createSubmission(
                        "%PDF".getBytes(StandardCharsets.UTF_8),
                        "x.pdf",
                        List.of(new SignerRequest("a@example.com", "A", SignatureSignerRole.TENANT))))
        .isInstanceOf(ExternalServiceException.class);
  }

  @Test
  @DisplayName("downloadCompleted fetches envelope, then signed PDF + certificate")
  void downloadCompletedFetchesBothPdfs() {
    SignedDocument document = client("secret").downloadCompleted("envelope_abc123");

    assertThat(new String(document.signedPdfBytes(), 0, 4, StandardCharsets.UTF_8)).isEqualTo("%PDF");
    assertThat(new String(document.certificatePdfBytes(), 0, 4, StandardCharsets.UTF_8)).isEqualTo("%PDF");
  }

  @Test
  @DisplayName("isValidWebhookSecret: correct secret passes, wrong/missing secret fails")
  void webhookSecretValidation() {
    DocumensoClient c = client("expected-secret");
    assertThat(c.isValidWebhookSecret("expected-secret")).isTrue();
    assertThat(c.isValidWebhookSecret("wrong-secret")).isFalse();
    assertThat(c.isValidWebhookSecret(null)).isFalse();
  }

  @Test
  @DisplayName("isValidWebhookSecret: no secret configured accepts anything (local dev)")
  void noSecretConfiguredAcceptsAnything() {
    DocumensoClient c = client("");
    assertThat(c.isValidWebhookSecret(null)).isTrue();
    assertThat(c.isValidWebhookSecret("anything")).isTrue();
  }
}
```

- [ ] **Step 3: Run it, verify pass**

Run: `cd backend && mvn clean install -DskipTests -pl buurman-core -am && mvn test -pl buurman-core -am -Dtest=DocumensoClientTest`
Expected: 6 tests pass.

- [ ] **Step 4: Commit**

```bash
git add backend/buurman-core/src/main/java/com/buurman/service/esignature/DocumensoClient.java backend/buurman-core/src/test/java/com/buurman/service/esignature/DocumensoClientTest.java
git commit -m "feat(esignature): implement DocumensoClient against the real Documenso v2 API"
```

---

## Task 6: SignatureService (create-and-send orchestration)

**Files:**
- Create: `backend/buurman-core/src/main/java/com/buurman/service/esignature/SignatureService.java`
- Test: `backend/buurman-core/src/test/java/com/buurman/service/esignature/SignatureServiceTest.java`

**Interfaces:**
- Consumes: `SignatureProviderClient` (Task 4/5), `SignatureRequestRepository`/`SignatureSignerRepository` (Task 3), `DocumentRepository.getByIdentifierAndTeamId(Sid, UUID): Document` (existing), `ContractPartyRepository.findByContractIdAndTeamId(UUID, UUID): List<ContractParty>` (existing), `ContactRepository.findByIdAndTeamId(UUID, UUID): Optional<Contact>` (existing), `S3StorageService.downloadFile(String): InputStream` (existing), `FeatureFlagService.isEnabled(String, UserPrincipal): boolean` (existing).
- Produces: `SignatureService.createSignatureRequest(ContractIdentifier, DocumentIdentifier, UserPrincipal): SignatureRequestResponse` and `SignatureService.getSignatureRequest(ContractIdentifier, DocumentIdentifier, SignatureRequestIdentifier, UserPrincipal): SignatureRequestResponse` — Task 7's `SignatureController` calls both directly.

- [ ] **Step 1: Write the failing test first**

This drives out the full orchestration: feature-flag gate, cross-team document lookup, signer derivation (principal = landlord, `ContractParty` rows with a `contactId` = tenants), provider failure handling.

```java
package com.buurman.service.esignature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.buurman.domain.Contact;
import com.buurman.domain.ContractParty;
import com.buurman.domain.ContractPartyRole;
import com.buurman.domain.Document;
import com.buurman.domain.Sid;
import com.buurman.domain.SignatureRequestStatus;
import com.buurman.domain.SignatureSigner;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.domain.identifier.DocumentIdentifier;
import com.buurman.exception.BusinessRuleException;
import com.buurman.exception.ExternalServiceException;
import com.buurman.exception.NotFoundException;
import com.buurman.repository.ContactRepository;
import com.buurman.repository.ContractPartyRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.DocumentRepository;
import com.buurman.repository.SignatureRequestRepository;
import com.buurman.repository.SignatureSignerRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.service.FeatureFlagService;
import com.buurman.service.S3StorageService;

class SignatureServiceTest {

  private final FeatureFlagService featureFlagService = mock(FeatureFlagService.class);
  private final DocumentRepository documentRepository = mock(DocumentRepository.class);
  private final ContractRepository contractRepository = mock(ContractRepository.class);
  private final ContractPartyRepository contractPartyRepository = mock(ContractPartyRepository.class);
  private final ContactRepository contactRepository = mock(ContactRepository.class);
  private final S3StorageService s3StorageService = mock(S3StorageService.class);
  private final SignatureProviderClient providerClient = mock(SignatureProviderClient.class);
  private final SignatureRequestRepository signatureRequestRepository = mock(SignatureRequestRepository.class);
  private final SignatureSignerRepository signatureSignerRepository = mock(SignatureSignerRepository.class);

  private SignatureService service;

  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID CONTRACT_ID = UUID.randomUUID();
  private static final UUID DOCUMENT_ID = UUID.randomUUID();
  private static final UUID CONTACT_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();

  private final UserPrincipal principal =
      new UserPrincipal(USER_ID, "USR1", "kc-1", "landlord@example.com", "Landlord", TEAM_ID, "TEA1", null);

  @BeforeEach
  void setUp() {
    service =
        new SignatureService(
            featureFlagService,
            documentRepository,
            contractRepository,
            contractPartyRepository,
            contactRepository,
            s3StorageService,
            providerClient,
            signatureRequestRepository,
            signatureSignerRepository);

    when(featureFlagService.isEnabled("esignature_enabled", principal)).thenReturn(true);

    Document document =
        Document.builder().id(DOCUMENT_ID).teamId(TEAM_ID).fileKey("k").fileName("addendum.pdf").build();
    when(documentRepository.getByIdentifierAndTeamId(any(Sid.class), org.mockito.ArgumentMatchers.eq(TEAM_ID)))
        .thenReturn(document);
    when(s3StorageService.downloadFile("k"))
        .thenReturn(new ByteArrayInputStream("%PDF-1.7\ndoc".getBytes()));

    com.buurman.domain.Contract contract = com.buurman.domain.Contract.builder().id(CONTRACT_ID).build();
    when(contractRepository.getByIdentifierAndTeamId(any(Sid.class), org.mockito.ArgumentMatchers.eq(TEAM_ID)))
        .thenReturn(contract);

    ContractParty tenantParty =
        ContractParty.builder()
            .contractId(CONTRACT_ID)
            .contactId(Optional.of(CONTACT_ID))
            .role(ContractPartyRole.PRIMARY_TENANT)
            .build();
    when(contractPartyRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
        .thenReturn(List.of(tenantParty));

    Contact tenantContact = Contact.builder().id(CONTACT_ID).email(Optional.of("tenant@example.com")).build();
    when(contactRepository.findByIdAndTeamId(CONTACT_ID, TEAM_ID)).thenReturn(Optional.of(tenantContact));
  }

  @Test
  @DisplayName("rejects when the feature flag is disabled, before touching the provider or the database")
  void rejectsWhenFlagDisabled() {
    when(featureFlagService.isEnabled("esignature_enabled", principal)).thenReturn(false);

    assertThatThrownBy(
            () ->
                service.createSignatureRequest(
                    ContractIdentifier.of("CON00000000000000000000001"),
                    DocumentIdentifier.of("DOC00000000000000000000001"),
                    principal))
        .isInstanceOf(BusinessRuleException.class);

    verifyNoInteractions(providerClient, signatureRequestRepository, signatureSignerRepository);
  }

  @Test
  @DisplayName("a document identifier from another team is not found — no cross-team leak")
  void crossTeamDocumentIsNotFound() {
    when(documentRepository.getByIdentifierAndTeamId(any(Sid.class), org.mockito.ArgumentMatchers.eq(TEAM_ID)))
        .thenThrow(new NotFoundException("Document not found"));

    assertThatThrownBy(
            () ->
                service.createSignatureRequest(
                    ContractIdentifier.of("CON00000000000000000000001"),
                    DocumentIdentifier.of("DOC00000000000000000000001"),
                    principal))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  @DisplayName("sends the document to landlord (principal) + every tenant contract party with an email")
  void sendsToLandlordAndTenants() {
    when(providerClient.createSubmission(any(byte[].class), any(String.class), anyList()))
        .thenReturn(
            new SignatureSubmission(
                "envelope_1",
                List.of(
                    new ProviderSigner("1", "landlord@example.com"),
                    new ProviderSigner("2", "tenant@example.com"))));
    when(signatureRequestRepository.save(any()))
        .thenAnswer(
            invocation -> {
              var request = (com.buurman.domain.SignatureRequest) invocation.getArgument(0);
              request.setId(UUID.randomUUID());
              request.setIdentifier(Optional.of(Sid.of("SGR00000000000000000000001")));
              return request;
            });
    when(signatureSignerRepository.save(any()))
        .thenAnswer(
            invocation -> {
              var signer = (SignatureSigner) invocation.getArgument(0);
              signer.setId(UUID.randomUUID());
              return signer;
            });

    var response =
        service.createSignatureRequest(
            ContractIdentifier.of("CON00000000000000000000001"),
            DocumentIdentifier.of("DOC00000000000000000000001"),
            principal);

    assertThat(response.status()).isEqualTo(SignatureRequestStatus.PENDING);
    org.mockito.Mockito.verify(providerClient)
        .createSubmission(
            any(byte[].class),
            any(String.class),
            org.mockito.ArgumentMatchers.argThat(
                signers ->
                    signers.size() == 2
                        && signers.stream().anyMatch(s -> s.email().equals("landlord@example.com"))
                        && signers.stream().anyMatch(s -> s.email().equals("tenant@example.com"))));
  }

  @Test
  @DisplayName("provider failure leaves the request FAILED, not stuck PENDING with no signers")
  void providerFailureMarksRequestFailed() {
    when(providerClient.createSubmission(any(byte[].class), any(String.class), anyList()))
        .thenThrow(new ExternalServiceException("Documenso unreachable"));
    when(signatureRequestRepository.save(any()))
        .thenAnswer(
            invocation -> {
              var request = (com.buurman.domain.SignatureRequest) invocation.getArgument(0);
              if (request.getId() == null) {
                request.setId(UUID.randomUUID());
                request.setIdentifier(Optional.of(Sid.of("SGR00000000000000000000002")));
              }
              return request;
            });

    assertThatThrownBy(
            () ->
                service.createSignatureRequest(
                    ContractIdentifier.of("CON00000000000000000000001"),
                    DocumentIdentifier.of("DOC00000000000000000000001"),
                    principal))
        .isInstanceOf(ExternalServiceException.class);

    org.mockito.Mockito.verify(signatureRequestRepository, org.mockito.Mockito.times(2)).save(any());
    org.mockito.Mockito.verify(signatureSignerRepository, org.mockito.Mockito.never()).save(any());
  }
}
```

- [ ] **Step 2: Run it, verify it fails to compile / fails**

Run: `mvn test -pl buurman-core -am -Dtest=SignatureServiceTest`
Expected: compile error — `SignatureService` doesn't exist yet.

- [ ] **Step 3: Write `SignatureService`**

```java
package com.buurman.service.esignature;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.Contact;
import com.buurman.domain.Contract;
import com.buurman.domain.ContractParty;
import com.buurman.domain.Document;
import com.buurman.domain.Sid;
import com.buurman.domain.SignatureRequest;
import com.buurman.domain.SignatureRequestStatus;
import com.buurman.domain.SignatureSigner;
import com.buurman.domain.SignatureSignerRole;
import com.buurman.domain.SignatureSignerStatus;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.domain.identifier.DocumentIdentifier;
import com.buurman.domain.identifier.SignatureRequestIdentifier;
import com.buurman.dto.response.SignatureRequestResponse;
import com.buurman.dto.response.SignatureSignerResponse;
import com.buurman.exception.BusinessRuleException;
import com.buurman.repository.ContactRepository;
import com.buurman.repository.ContractPartyRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.DocumentRepository;
import com.buurman.repository.SignatureRequestRepository;
import com.buurman.repository.SignatureSignerRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.service.FeatureFlagService;
import com.buurman.service.S3StorageService;
import com.buurman.util.FeatureFlags;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SignatureService {

  private static final String PROVIDER = "documenso";

  private final FeatureFlagService featureFlagService;
  private final DocumentRepository documentRepository;
  private final ContractRepository contractRepository;
  private final ContractPartyRepository contractPartyRepository;
  private final ContactRepository contactRepository;
  private final S3StorageService s3StorageService;
  private final SignatureProviderClient providerClient;
  private final SignatureRequestRepository signatureRequestRepository;
  private final SignatureSignerRepository signatureSignerRepository;

  @Transactional
  public SignatureRequestResponse createSignatureRequest(
      ContractIdentifier contractIdentifier, DocumentIdentifier documentIdentifier, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();

    if (!featureFlagService.isEnabled(FeatureFlags.ESIGNATURE_ENABLED, principal)) {
      throw new BusinessRuleException("E-signature is not enabled for this team");
    }

    Document document = documentRepository.getByIdentifierAndTeamId(documentIdentifier, teamId);
    Contract contract = contractRepository.getByIdentifierAndTeamId(contractIdentifier, teamId);

    List<SignerRequest> signerRequests = new ArrayList<>();
    signerRequests.add(new SignerRequest(principal.getEmail(), principal.getName(), SignatureSignerRole.LANDLORD));

    List<ContractParty> parties = contractPartyRepository.findByContractIdAndTeamId(contract.getId(), teamId);
    for (ContractParty party : parties) {
      party
          .getContactId()
          .flatMap(contactId -> contactRepository.findByIdAndTeamId(contactId, teamId))
          .flatMap(Contact::getEmail)
          .ifPresent(email -> signerRequests.add(new SignerRequest(email, email, SignatureSignerRole.TENANT)));
    }

    SignatureRequest request =
        signatureRequestRepository.save(
            SignatureRequest.builder()
                .teamId(teamId)
                .documentId(document.getId())
                .provider(PROVIDER)
                .providerSubmissionId("pending-" + UUID.randomUUID())
                .status(SignatureRequestStatus.PENDING)
                .createdBy(principal.getUserId())
                .updatedBy(principal.getUserId())
                .build());

    SignatureSubmission submission;
    try {
      byte[] pdfBytes = readAllBytes(s3StorageService.downloadFile(document.getFileKey()));
      submission = providerClient.createSubmission(pdfBytes, document.getFileName(), signerRequests);
    } catch (RuntimeException e) {
      request.setStatus(SignatureRequestStatus.FAILED);
      request.setUpdatedBy(principal.getUserId());
      signatureRequestRepository.save(request);
      throw e;
    }

    request.setProviderSubmissionId(submission.providerSubmissionId());
    signatureRequestRepository.save(request);

    Map<String, String> emailByProviderSignerId =
        submission.signers().stream()
            .collect(java.util.stream.Collectors.toMap(ProviderSigner::providerSignerId, ProviderSigner::email));
    List<SignatureSigner> savedSigners = new ArrayList<>();
    for (SignerRequest signerRequest : signerRequests) {
      String providerSignerId =
          emailByProviderSignerId.entrySet().stream()
              .filter(e -> e.getValue().equalsIgnoreCase(signerRequest.email()))
              .map(Map.Entry::getKey)
              .findFirst()
              .orElse(signerRequest.email());
      savedSigners.add(
          signatureSignerRepository.save(
              SignatureSigner.builder()
                  .signatureRequestId(request.getId())
                  .email(signerRequest.email())
                  .role(signerRequest.role())
                  .providerSignerId(providerSignerId)
                  .status(SignatureSignerStatus.PENDING)
                  .build()));
    }

    log.info(
        "Sent document {} for signature: request {} ({} signers)",
        documentIdentifier.value(),
        request.getIdentifier().orElseThrow().value(),
        savedSigners.size());

    return toResponse(request, savedSigners);
  }

  public SignatureRequestResponse getSignatureRequest(
      ContractIdentifier contractIdentifier,
      DocumentIdentifier documentIdentifier,
      SignatureRequestIdentifier signatureRequestIdentifier,
      UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    // contractIdentifier/documentIdentifier are validated implicitly: the request is looked up by
    // its own team-scoped identifier, so a mismatched contract/document in the URL simply can't
    // resolve to a different team's request — team scoping is what matters, not path consistency.
    SignatureRequest request =
        signatureRequestRepository.getByIdentifierAndTeamId(signatureRequestIdentifier, teamId);
    List<SignatureSigner> signers = signatureSignerRepository.findBySignatureRequestId(request.getId());
    return toResponse(request, signers);
  }

  private SignatureRequestResponse toResponse(SignatureRequest request, List<SignatureSigner> signers) {
    Sid originalDocumentIdentifier =
        documentRepository.findByIdAndTeamId(request.getDocumentId(), request.getTeamId())
            .flatMap(Document::getIdentifier)
            .orElseThrow();
    Optional<Sid> signedDocumentIdentifier =
        request
            .getSignedDocumentId()
            .flatMap(id -> documentRepository.findByIdAndTeamId(id, request.getTeamId()))
            .flatMap(Document::getIdentifier);

    List<SignatureSignerResponse> signerResponses =
        signers.stream()
            .map(s -> new SignatureSignerResponse(s.getEmail(), s.getRole(), s.getStatus(), s.getSignedAt()))
            .toList();

    return new SignatureRequestResponse(
        request.getIdentifier().orElseThrow(),
        originalDocumentIdentifier,
        signedDocumentIdentifier,
        request.getStatus(),
        signerResponses,
        request.getCreatedAt(),
        request.getUpdatedAt());
  }

  private static byte[] readAllBytes(InputStream in) {
    try (in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      in.transferTo(out);
      return out.toByteArray();
    } catch (java.io.IOException e) {
      throw new com.buurman.exception.ExternalServiceException("Failed to read document from storage", e);
    }
  }
}
```

- [ ] **Step 4: Run the test, verify it passes**

Run: `mvn test -pl buurman-core -am -Dtest=SignatureServiceTest`
Expected: 4 tests pass.

- [ ] **Step 5: Full core module test run (guard against regressions in `FeatureFlagService`/`DocumentRepository` consumers)**

Run: `mvn clean install -DskipTests -pl buurman-core -am && mvn test -pl buurman-core -am`
Expected: BUILD SUCCESS, all existing + new tests pass.

- [ ] **Step 6: Commit**

```bash
git add backend/buurman-core/src/main/java/com/buurman/service/esignature/SignatureService.java backend/buurman-core/src/test/java/com/buurman/service/esignature/SignatureServiceTest.java
git commit -m "feat(esignature): add SignatureService create/get orchestration with feature-flag gate"
```

---

## Task 7: OpenAPI spec + SignatureController (buurman-letters)

**Files:**
- Modify: `openapi/src/paths/contracts.yaml` (add two anchors: `signature-requests`, `signature-requests-item`)
- Modify: `openapi/src/app.yaml` (register the two new paths + three new schemas: `SignatureRequestResponse`, `SignatureSignerResponse`, plus reuse existing `SignatureRequestIdentifier` — add it as a new identifier schema alongside `DocumentIdentifier`)
- Create: `backend/buurman-letters/src/main/java/com/buurman/controller/SignatureController.java`
- Test: `backend/buurman-letters/src/test/java/com/buurman/controller/SignatureControllerTest.java`

**Interfaces:**
- Consumes: `SignatureService.createSignatureRequest`/`getSignatureRequest` (Task 6).
- Produces: generated `SignaturesApi` interface (from the `Signatures` tag) that `SignatureController` implements; frontend Orval client functions `createSignatureRequest`/`getSignatureRequest` (consumed in Task 9).

- [ ] **Step 1: Add the identifier schema**

In `openapi/src/app.yaml`, next to the existing `DocumentIdentifier` schema (around line 787), add:

```yaml
    SignatureRequestIdentifier:
      type: string
      description: Signature request identifier
```

- [ ] **Step 2: Add the response schemas**

Next to `DocumentResponse` (around line 3049), add:

```yaml
    SignatureSignerResponse:
      type: object
      description: A single signer's status within a signature request
      properties:
        email:
          type: string
        role:
          type: string
          enum: [LANDLORD, TENANT]
        status:
          type: string
          enum: [PENDING, VIEWED, SIGNED, DECLINED]
        signedAt:
          type: string
          format: date-time
      required:
        - email
        - role
        - status
    SignatureRequestResponse:
      type: object
      description: An e-signature request for a document, with per-signer status
      properties:
        identifier:
          type: string
          example: SGR01HZQX7V8B3K5M2N4P6R9T0W
        documentIdentifier:
          type: string
        signedDocumentIdentifier:
          type: string
        status:
          type: string
          enum: [PENDING, PARTIALLY_SIGNED, COMPLETED, DECLINED, CANCELLED, FAILED]
        signers:
          type: array
          items:
            $ref: '#/components/schemas/SignatureSignerResponse'
        createdAt:
          type: string
          format: date-time
        updatedAt:
          type: string
          format: date-time
      required:
        - identifier
        - documentIdentifier
        - status
        - signers
        - createdAt
        - updatedAt
```

- [ ] **Step 3: Add the two path anchors to `openapi/src/paths/contracts.yaml`**

Append at the end of the file:

```yaml
signature-requests:
  post:
    tags:
      - Signatures
    summary: Send a document for e-signature
    description: Sends the given document to the landlord and every tenant contract party with an email, in parallel, via the configured e-signature provider
    operationId: createSignatureRequest
    parameters:
      - name: contractIdentifier
        in: path
        description: Contract identifier
        required: true
        schema:
          $ref: '#/components/schemas/ContractIdentifier'
      - name: documentIdentifier
        in: path
        description: Document identifier
        required: true
        schema:
          $ref: '#/components/schemas/DocumentIdentifier'
    responses:
      "200":
        description: Signature request created and sent
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/SignatureRequestResponse'
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
      "409":
        description: Conflict - business rule violation
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

signature-requests-item:
  get:
    tags:
      - Signatures
    summary: Get a signature request's status
    description: Returns the current status of a signature request and every signer's status, for polling
    operationId: getSignatureRequest
    parameters:
      - name: contractIdentifier
        in: path
        description: Contract identifier
        required: true
        schema:
          $ref: '#/components/schemas/ContractIdentifier'
      - name: documentIdentifier
        in: path
        description: Document identifier
        required: true
        schema:
          $ref: '#/components/schemas/DocumentIdentifier'
      - name: signatureRequestIdentifier
        in: path
        description: Signature request identifier
        required: true
        schema:
          $ref: '#/components/schemas/SignatureRequestIdentifier'
    responses:
      "200":
        description: Signature request status
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/SignatureRequestResponse'
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

- [ ] **Step 4: Register the two new URL paths in `openapi/src/app.yaml`**

In the `paths:` map, next to the existing `/contracts/{contractIdentifier}/rent-periods/...` entries:

```yaml
  /contracts/{contractIdentifier}/documents/{documentIdentifier}/signature-requests:
    $ref: 'paths/contracts.yaml#/signature-requests'
  /contracts/{contractIdentifier}/documents/{documentIdentifier}/signature-requests/{signatureRequestIdentifier}:
    $ref: 'paths/contracts.yaml#/signature-requests-item'
```

- [ ] **Step 5: Bundle**

Run: `make bundle-openapi` (or `python3 scripts/bundle_openapi.py`)
Expected: `openapi/app.yaml` regenerated with no errors; `git diff openapi/app.yaml` shows the two new paths + three new schemas.

- [ ] **Step 6: Regenerate backend API interfaces and rebuild**

Run: `cd backend && mvn clean install -DskipTests -pl buurman-core -am` (the `SignaturesApi` interface is generated as part of the `buurman-core` build via the openapi-generator Maven plugin — confirm by checking the target directory)
Run: `find backend -path '*/generated-sources/*' -iname 'SignaturesApi.java'`
Expected: file exists.

- [ ] **Step 7: Write `SignatureController`**

```java
package com.buurman.controller;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.domain.identifier.DocumentIdentifier;
import com.buurman.domain.identifier.SignatureRequestIdentifier;
import com.buurman.dto.response.SignatureRequestResponse;
import com.buurman.generated.api.SignaturesApi;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.esignature.SignatureService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class SignatureController implements SignaturesApi {

  private final SignatureService signatureService;

  @Override
  public SignatureRequestResponse createSignatureRequest(
      ContractIdentifier contractIdentifier, DocumentIdentifier documentIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return signatureService.createSignatureRequest(contractIdentifier, documentIdentifier, principal);
  }

  @Override
  public SignatureRequestResponse getSignatureRequest(
      ContractIdentifier contractIdentifier,
      DocumentIdentifier documentIdentifier,
      SignatureRequestIdentifier signatureRequestIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return signatureService.getSignatureRequest(
        contractIdentifier, documentIdentifier, signatureRequestIdentifier, principal);
  }
}
```

(If the generated interface's method signatures differ slightly — e.g. parameter order — match whatever `SignaturesApi.java` under `target/generated-sources` actually declares; openapi-generator derives parameter order from the YAML `parameters:` list order, which is `contractIdentifier, documentIdentifier[, signatureRequestIdentifier]` above, so this should match exactly.)

- [ ] **Step 8: Write a controller test**

Follow the existing convention for a thin controller test — check `LetterController` has no dedicated unit test file (it doesn't; letter generation is tested at the service layer, per `RentChangeDocumentGenerationService`). Since `SignatureController` is equally thin (pure delegation), skip a dedicated controller test and rely on Task 6's `SignatureServiceTest` for behavior coverage — consistent with this module's existing testing convention. Instead, add one `@SpringBootTest`-free unit test asserting the delegation itself, since this is the first controller in the module wired to a brand-new generated interface and a typo in wiring (wrong method name/param order) would otherwise only surface at runtime:

```java
package com.buurman.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.buurman.domain.Sid;
import com.buurman.domain.SignatureRequestStatus;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.domain.identifier.DocumentIdentifier;
import com.buurman.dto.response.SignatureRequestResponse;
import com.buurman.service.esignature.SignatureService;

class SignatureControllerTest {

  @Test
  void createSignatureRequestDelegatesToService() {
    SignatureService signatureService = mock(SignatureService.class);
    SignatureController controller = new SignatureController(signatureService);
    ContractIdentifier contractId = ContractIdentifier.of("CON00000000000000000000001");
    DocumentIdentifier documentId = DocumentIdentifier.of("DOC00000000000000000000001");
    SignatureRequestResponse expected =
        new SignatureRequestResponse(
            Sid.of("SGR00000000000000000000001"),
            Sid.of(documentId.value()),
            Optional.empty(),
            SignatureRequestStatus.PENDING,
            List.of(),
            Instant.now(),
            Instant.now());
    when(signatureService.createSignatureRequest(
            org.mockito.ArgumentMatchers.eq(contractId),
            org.mockito.ArgumentMatchers.eq(documentId),
            org.mockito.ArgumentMatchers.any()))
        .thenReturn(expected);

    // SecurityUtils.getCurrentPrincipal() requires a SecurityContext; this test only exercises
    // the delegation shape, so run it inside a thread with a stubbed context — mirrors the pattern
    // used by other thin-controller tests in this codebase for SecurityUtils-dependent methods.
    var authentication =
        new org.springframework.security.authentication.TestingAuthenticationToken(
            new com.buurman.security.UserPrincipal(
                java.util.UUID.randomUUID(), "USR1", "kc-1", "l@example.com", "L",
                java.util.UUID.randomUUID(), "TEA1", null),
            null);
    org.springframework.security.core.context.SecurityContextHolder.getContext()
        .setAuthentication(authentication);
    try {
      SignatureRequestResponse actual = controller.createSignatureRequest(contractId, documentId);
      assertThat(actual).isEqualTo(expected);
    } finally {
      org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }
  }
}
```

If `SecurityUtils.getCurrentPrincipal()` doesn't read from `SecurityContextHolder` this way (check `backend/buurman-core/src/main/java/com/buurman/security/SecurityUtils.java` first), adjust the stubbing to match its actual mechanism — the intent (stub the current principal, assert pure delegation) is what matters.

- [ ] **Step 9: Run it, verify pass**

Run: `mvn clean install -DskipTests -pl buurman-letters -am && mvn test -pl buurman-letters -am -Dtest=SignatureControllerTest`
Expected: 1 test passes.

- [ ] **Step 10: Commit**

```bash
git add openapi/src/paths/contracts.yaml openapi/src/app.yaml openapi/app.yaml backend/buurman-letters/src/main/java/com/buurman/controller/SignatureController.java backend/buurman-letters/src/test/java/com/buurman/controller/SignatureControllerTest.java
git commit -m "feat(esignature): add signature-requests API (OpenAPI + SignatureController)"
```

---

## Task 8: Webhook handling (buurman-notifications)

**Files:**
- Modify: `openapi/src/paths/reference.yaml` (add `webhooks-documenso-events` anchor)
- Modify: `openapi/src/app.yaml` (register `/webhooks/documenso/events`)
- Modify: `backend/buurman-notifications/src/main/java/com/buurman/controller/WebhookController.java` (add `handleDocumensoEvents`)
- Create: `backend/buurman-notifications/src/main/java/com/buurman/service/notification/SignatureWebhookService.java`
- Test: `backend/buurman-notifications/src/test/java/com/buurman/service/notification/SignatureWebhookServiceTest.java`

**Interfaces:**
- Consumes: `SignatureRequestRepository.findByProviderAndProviderSubmissionId`/`save` (Task 3), `SignatureSignerRepository.findBySignatureRequestId`/`updateStatus` (Task 3), `SignatureProviderClient.downloadCompleted`/`isValidWebhookSecret` (Task 4/5), `S3StorageService.uploadFile` + `DocumentRepository.save` (existing).
- Produces: `SignatureWebhookService.processDocumensoEvent(String rawPayload, String secretHeader)` — `WebhookController.handleDocumensoEvents` calls this.

- [ ] **Step 1: Add the webhook path**

In `openapi/src/paths/reference.yaml`, append (mirroring `webhooks-mailgun-events` immediately above it):

```yaml
webhooks-documenso-events:
  post:
    tags:
      - Webhooks
    summary: Documenso event webhook
    description: Receives signing lifecycle events from the self-hosted Documenso e-signature sidecar
    operationId: handleDocumensoEvents
    parameters:
      - name: X-Documenso-Secret
        in: header
        description: Shared secret configured on the Documenso webhook
        required: false
        schema:
          type: string
    requestBody:
      content:
        application/json:
          schema:
            type: string
            description: Raw JSON event payload
      required: true
    responses:
      "200":
        description: OK
      "400":
        description: Bad request - validation error or malformed input
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
      "409":
        description: Conflict - business rule violation
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
```

In `openapi/src/app.yaml`'s `paths:` map, next to `/webhooks/mailgun/events`:

```yaml
  /webhooks/documenso/events:
    $ref: 'paths/reference.yaml#/webhooks-documenso-events'
```

- [ ] **Step 2: Bundle and rebuild**

Run: `make bundle-openapi && cd backend && mvn clean install -DskipTests -pl buurman-notifications -am`
Expected: BUILD SUCCESS; `WebhooksApi.java` now declares `handleDocumensoEvents(String body, Optional<String> xDocumensoSecret)`.

- [ ] **Step 3: Write the failing test for `SignatureWebhookService`**

```java
package com.buurman.service.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.buurman.domain.Document;
import com.buurman.domain.SignatureRequest;
import com.buurman.domain.SignatureRequestStatus;
import com.buurman.domain.SignatureSigner;
import com.buurman.domain.SignatureSignerRole;
import com.buurman.domain.SignatureSignerStatus;
import com.buurman.repository.DocumentRepository;
import com.buurman.repository.SignatureRequestRepository;
import com.buurman.repository.SignatureSignerRepository;
import com.buurman.service.S3StorageService;
import com.buurman.service.esignature.SignatureProviderClient;
import com.buurman.service.esignature.SignedDocument;
import com.fasterxml.jackson.databind.ObjectMapper;

class SignatureWebhookServiceTest {

  private final SignatureRequestRepository signatureRequestRepository = mock(SignatureRequestRepository.class);
  private final SignatureSignerRepository signatureSignerRepository = mock(SignatureSignerRepository.class);
  private final SignatureProviderClient providerClient = mock(SignatureProviderClient.class);
  private final DocumentRepository documentRepository = mock(DocumentRepository.class);
  private final S3StorageService s3StorageService = mock(S3StorageService.class);
  private final com.buurman.repository.TeamRepository teamRepository = mock(com.buurman.repository.TeamRepository.class);

  private SignatureWebhookService service;

  private static final UUID REQUEST_ID = UUID.randomUUID();
  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID DOCUMENT_ID = UUID.randomUUID();
  private static final UUID SIGNER_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    service =
        new SignatureWebhookService(
            signatureRequestRepository,
            signatureSignerRepository,
            providerClient,
            documentRepository,
            s3StorageService,
            teamRepository,
            new ObjectMapper());
    when(providerClient.isValidWebhookSecret(any())).thenReturn(true);
    when(teamRepository.findById(TEAM_ID))
        .thenReturn(
            Optional.of(
                com.buurman.domain.Team.builder()
                    .id(TEAM_ID)
                    .identifier(Optional.of(com.buurman.domain.Sid.of("TEA00000000000000000000001")))
                    .build()));
  }

  private SignatureRequest existingRequest(SignatureRequestStatus status) {
    return SignatureRequest.builder()
        .id(REQUEST_ID)
        .teamId(TEAM_ID)
        .documentId(DOCUMENT_ID)
        .provider("documenso")
        .providerSubmissionId("envelope_abc123")
        .status(status)
        .build();
  }

  private SignatureSigner existingSigner(SignatureSignerStatus status) {
    return SignatureSigner.builder()
        .id(SIGNER_ID)
        .signatureRequestId(REQUEST_ID)
        .email("tenant@example.com")
        .role(SignatureSignerRole.TENANT)
        .providerSignerId("52")
        .status(status)
        .build();
  }

  @Test
  @DisplayName("invalid webhook secret is rejected before any DB read")
  void invalidSecretRejected() {
    when(providerClient.isValidWebhookSecret("wrong")).thenReturn(false);

    assertThatThrownByProcessing(openedEventPayload(), "wrong");

    verify(signatureRequestRepository, never()).findByProviderAndProviderSubmissionId(any(), any());
  }

  @Test
  @DisplayName("unknown envelope id (no matching signature_request) is ignored safely, never throws")
  void unknownEnvelopeIdIgnoredSafely() {
    when(signatureRequestRepository.findByProviderAndProviderSubmissionId("documenso", "envelope_abc123"))
        .thenReturn(Optional.empty());

    service.processDocumensoEvent(openedEventPayload(), "secret");

    verify(signatureSignerRepository, never()).findBySignatureRequestId(any());
  }

  @Test
  @DisplayName("DOCUMENT_OPENED sets a PENDING signer to VIEWED")
  void documentOpenedSetsViewed() {
    when(signatureRequestRepository.findByProviderAndProviderSubmissionId("documenso", "envelope_abc123"))
        .thenReturn(Optional.of(existingRequest(SignatureRequestStatus.PENDING)));
    when(signatureSignerRepository.findBySignatureRequestId(REQUEST_ID))
        .thenReturn(List.of(existingSigner(SignatureSignerStatus.PENDING)));

    service.processDocumensoEvent(openedEventPayload(), "secret");

    verify(signatureSignerRepository).updateStatus(SIGNER_ID, SignatureSignerStatus.VIEWED, Optional.empty());
  }

  @Test
  @DisplayName("a stale DOCUMENT_OPENED arriving after SIGNED does not regress the signer's status")
  void staleOpenedDoesNotRegressSignedSigner() {
    when(signatureRequestRepository.findByProviderAndProviderSubmissionId("documenso", "envelope_abc123"))
        .thenReturn(Optional.of(existingRequest(SignatureRequestStatus.PARTIALLY_SIGNED)));
    when(signatureSignerRepository.findBySignatureRequestId(REQUEST_ID))
        .thenReturn(List.of(existingSigner(SignatureSignerStatus.SIGNED)));

    service.processDocumensoEvent(openedEventPayload(), "secret");

    verify(signatureSignerRepository, never()).updateStatus(eq(SIGNER_ID), any(), any());
  }

  @Test
  @DisplayName("DOCUMENT_REJECTED sets the signer DECLINED and the request DECLINED")
  void documentRejectedDeclines() {
    when(signatureRequestRepository.findByProviderAndProviderSubmissionId("documenso", "envelope_abc123"))
        .thenReturn(Optional.of(existingRequest(SignatureRequestStatus.PENDING)));
    when(signatureSignerRepository.findBySignatureRequestId(REQUEST_ID))
        .thenReturn(List.of(existingSigner(SignatureSignerStatus.PENDING)));

    service.processDocumensoEvent(rejectedEventPayload(), "secret");

    verify(signatureSignerRepository).updateStatus(SIGNER_ID, SignatureSignerStatus.DECLINED, Optional.empty());
    verify(signatureRequestRepository)
        .save(org.mockito.ArgumentMatchers.argThat(r -> r.getStatus() == SignatureRequestStatus.DECLINED));
  }

  @Test
  @DisplayName("DOCUMENT_COMPLETED downloads the signed PDF+certificate, uploads, and marks COMPLETED")
  void documentCompletedDownloadsAndPersists() {
    when(signatureRequestRepository.findByProviderAndProviderSubmissionId("documenso", "envelope_abc123"))
        .thenReturn(Optional.of(existingRequest(SignatureRequestStatus.PARTIALLY_SIGNED)));
    when(signatureSignerRepository.findBySignatureRequestId(REQUEST_ID))
        .thenReturn(List.of(existingSigner(SignatureSignerStatus.SIGNED)));
    when(providerClient.downloadCompleted("envelope_abc123"))
        .thenReturn(new SignedDocument("%PDF-signed".getBytes(), "%PDF-cert".getBytes()));
    Document originalDocument =
        Document.builder().id(DOCUMENT_ID).teamId(TEAM_ID).entityType("CONTRACT").entityId(UUID.randomUUID())
            .identifier(Optional.of(com.buurman.domain.Sid.of("DOC00000000000000000000009")))
            .fileName("addendum.pdf").build();
    when(documentRepository.findByIdAndTeamId(DOCUMENT_ID, TEAM_ID)).thenReturn(Optional.of(originalDocument));
    when(s3StorageService.uploadFile(any(byte[].class), any(), any(), any(), any(), any())).thenReturn("k2");
    when(documentRepository.save(any()))
        .thenAnswer(
            invocation -> {
              Document d = invocation.getArgument(0);
              d.setId(UUID.randomUUID());
              return d;
            });

    service.processDocumensoEvent(completedEventPayload(), "secret");

    verify(signatureRequestRepository)
        .save(org.mockito.ArgumentMatchers.argThat(r -> r.getStatus() == SignatureRequestStatus.COMPLETED));
  }

  private void assertThatThrownByProcessing(String payload, String secret) {
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.processDocumensoEvent(payload, secret))
        .isInstanceOf(com.buurman.exception.ForbiddenException.class);
  }

  private String openedEventPayload() {
    return """
        {"event":"DOCUMENT_OPENED","payload":{"envelopeId":"envelope_abc123","status":"PENDING",
        "recipients":[{"id":52,"email":"tenant@example.com","readStatus":"OPENED","signingStatus":"NOT_SIGNED"}]}}
        """;
  }

  private String rejectedEventPayload() {
    return """
        {"event":"DOCUMENT_REJECTED","payload":{"envelopeId":"envelope_abc123","status":"REJECTED",
        "recipients":[{"id":52,"email":"tenant@example.com","signingStatus":"REJECTED"}]}}
        """;
  }

  private String completedEventPayload() {
    return """
        {"event":"DOCUMENT_COMPLETED","payload":{"envelopeId":"envelope_abc123","status":"COMPLETED",
        "recipients":[{"id":52,"email":"tenant@example.com","signingStatus":"SIGNED"}]}}
        """;
  }
}
```

- [ ] **Step 4: Run it, verify it fails to compile (class doesn't exist)**

Run: `mvn test -pl buurman-notifications -am -Dtest=SignatureWebhookServiceTest`
Expected: compile error.

- [ ] **Step 5: Write `SignatureWebhookService`**

```java
package com.buurman.service.notification;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import com.buurman.domain.Document;
import com.buurman.domain.Sid;
import com.buurman.domain.SignatureRequest;
import com.buurman.domain.SignatureRequestStatus;
import com.buurman.domain.SignatureSigner;
import com.buurman.domain.SignatureSignerStatus;
import com.buurman.domain.Team;
import com.buurman.exception.ForbiddenException;
import com.buurman.repository.DocumentRepository;
import com.buurman.repository.SignatureRequestRepository;
import com.buurman.repository.SignatureSignerRepository;
import com.buurman.repository.TeamRepository;
import com.buurman.service.S3StorageService;
import com.buurman.service.esignature.SignatureProviderClient;
import com.buurman.service.esignature.SignedDocument;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SignatureWebhookService {

  /** Forward-only guard, same shape as {@link WebhookService}'s STATUS_RANK. */
  private static final Map<SignatureSignerStatus, Integer> STATUS_RANK =
      Map.of(
          SignatureSignerStatus.PENDING, 0,
          SignatureSignerStatus.VIEWED, 1,
          SignatureSignerStatus.SIGNED, 2,
          SignatureSignerStatus.DECLINED, 2);

  private final SignatureRequestRepository signatureRequestRepository;
  private final SignatureSignerRepository signatureSignerRepository;
  private final SignatureProviderClient providerClient;
  private final DocumentRepository documentRepository;
  private final S3StorageService s3StorageService;
  private final TeamRepository teamRepository;
  private final ObjectMapper objectMapper;

  public void processDocumensoEvent(String rawPayload, String secretHeader) {
    if (!providerClient.isValidWebhookSecret(secretHeader)) {
      log.warn("Documenso webhook secret verification failed");
      throw new ForbiddenException("Documenso webhook secret verification failed");
    }

    try {
      JsonNode root = objectMapper.readTree(rawPayload);
      String event = root.path("event").asText();
      JsonNode payload = root.path("payload");
      String envelopeId = payload.path("envelopeId").asText();

      if (envelopeId.isEmpty()) {
        log.warn("Documenso webhook missing envelopeId");
        return;
      }

      Optional<SignatureRequest> maybeRequest =
          signatureRequestRepository.findByProviderAndProviderSubmissionId("documenso", envelopeId);
      if (maybeRequest.isEmpty()) {
        log.debug("Documenso webhook for unknown envelope {} — ignoring", envelopeId);
        return;
      }
      SignatureRequest request = maybeRequest.get();

      List<SignatureSigner> signers = signatureSignerRepository.findBySignatureRequestId(request.getId());
      Map<String, SignatureSigner> byProviderSignerId =
          signers.stream()
              .collect(java.util.stream.Collectors.toMap(SignatureSigner::getProviderSignerId, s -> s));

      for (JsonNode recipient : payload.path("recipients")) {
        String recipientId = recipient.path("id").asText();
        SignatureSigner signer = byProviderSignerId.get(recipientId);
        if (signer == null) {
          continue;
        }
        SignatureSignerStatus newStatus = mapSignerStatus(recipient);
        if (isForwardProgress(signer.getStatus(), newStatus)) {
          Optional<java.time.Instant> signedAt =
              recipient.has("signedAt") && !recipient.path("signedAt").isNull()
                  ? Optional.of(java.time.Instant.parse(recipient.path("signedAt").asText()))
                  : Optional.empty();
          signatureSignerRepository.updateStatus(signer.getId(), newStatus, signedAt);
          signer.setStatus(newStatus);
        }
      }

      switch (event) {
        case "DOCUMENT_REJECTED" -> finalizeRequestStatus(request, SignatureRequestStatus.DECLINED);
        case "DOCUMENT_CANCELLED" -> finalizeRequestStatus(request, SignatureRequestStatus.CANCELLED);
        case "DOCUMENT_COMPLETED" -> completeRequest(request, envelopeId);
        default -> updatePartialProgress(request, signers);
      }
    } catch (ForbiddenException e) {
      throw e;
    } catch (Exception e) {
      log.error("Error processing Documenso webhook: {}", e.getMessage(), e);
    }
  }

  private SignatureSignerStatus mapSignerStatus(JsonNode recipient) {
    String signingStatus = recipient.path("signingStatus").asText("");
    String readStatus = recipient.path("readStatus").asText("");
    if ("REJECTED".equals(signingStatus)) {
      return SignatureSignerStatus.DECLINED;
    }
    if ("SIGNED".equals(signingStatus)) {
      return SignatureSignerStatus.SIGNED;
    }
    if ("OPENED".equals(readStatus)) {
      return SignatureSignerStatus.VIEWED;
    }
    return SignatureSignerStatus.PENDING;
  }

  private boolean isForwardProgress(SignatureSignerStatus current, SignatureSignerStatus candidate) {
    return STATUS_RANK.getOrDefault(candidate, 0) > STATUS_RANK.getOrDefault(current, 0);
  }

  private void updatePartialProgress(SignatureRequest request, List<SignatureSigner> signers) {
    boolean anySigned = signers.stream().anyMatch(s -> s.getStatus() == SignatureSignerStatus.SIGNED);
    if (anySigned && request.getStatus() == SignatureRequestStatus.PENDING) {
      finalizeRequestStatus(request, SignatureRequestStatus.PARTIALLY_SIGNED);
    }
  }

  private void finalizeRequestStatus(SignatureRequest request, SignatureRequestStatus status) {
    if (request.getStatus() == SignatureRequestStatus.COMPLETED
        || request.getStatus() == SignatureRequestStatus.DECLINED
        || request.getStatus() == SignatureRequestStatus.CANCELLED) {
      return; // already terminal — never regress a terminal outcome
    }
    request.setStatus(status);
    signatureRequestRepository.save(request);
  }

  private void completeRequest(SignatureRequest request, String envelopeId) {
    if (request.getStatus() == SignatureRequestStatus.COMPLETED) {
      return; // duplicate DOCUMENT_COMPLETED delivery — do not re-download/re-upload
    }

    SignedDocument signed = providerClient.downloadCompleted(envelopeId);
    Document original =
        documentRepository
            .findByIdAndTeamId(request.getDocumentId(), request.getTeamId())
            .orElseThrow();
    Sid teamIdentifier =
        teamRepository.findById(request.getTeamId()).flatMap(Team::getIdentifier).orElseThrow();
    Sid documentIdentifier = original.getIdentifier().orElseThrow();

    String fileName = "signed-" + original.getFileName();
    String fileKey =
        s3StorageService.uploadFile(
            signed.signedPdfBytes(),
            MediaType.APPLICATION_PDF_VALUE,
            teamIdentifier,
            original.getEntityType(),
            documentIdentifier,
            fileName);

    Document signedDocument =
        Document.builder()
            .teamId(request.getTeamId())
            .entityType(original.getEntityType())
            .entityId(original.getEntityId())
            .fileKey(fileKey)
            .fileName(fileName)
            .fileSize((long) signed.signedPdfBytes().length)
            .mimeType(MediaType.APPLICATION_PDF_VALUE)
            .title(Optional.of("Signed: " + original.getFileName()))
            .notes(Optional.empty())
            .uploadedBy(request.getUpdatedBy())
            .build();
    Document savedSignedDocument = documentRepository.save(signedDocument);

    request.setSignedDocumentId(Optional.of(savedSignedDocument.getId()));
    request.setStatus(SignatureRequestStatus.COMPLETED);
    signatureRequestRepository.save(request);

    log.info("Signature request {} completed; signed document {} stored", envelopeId, savedSignedDocument.getId());
  }
}
```

This mirrors the `s3StorageService.uploadFile(byte[], String, Sid, String, Sid, String)` overload used by `RentChangeDocumentGenerationService` (`pdf, mimeType, teamIdentifier, entityType, entityIdentifier, filename`) — the team `Sid` comes from `TeamRepository.findById(...).getIdentifier()` and the entity `Sid` from the original `Document`'s own identifier, never from a raw UUID.

- [ ] **Step 6: Add `handleDocumensoEvents` to `WebhookController`**

```java
  private final com.buurman.service.notification.SignatureWebhookService signatureWebhookService;

  @Override
  public void handleDocumensoEvents(String body, Optional<String> xDocumensoSecret) {
    try {
      signatureWebhookService.processDocumensoEvent(body, xDocumensoSecret.orElse(null));
    } catch (ForbiddenException e) {
      throw e;
    } catch (Exception e) {
      log.error("Error processing Documenso webhook: {}", e.getMessage(), e);
    }
  }
```

(Add the field to the constructor-injected field list at the top of the class; `@RequiredArgsConstructor` picks it up automatically. Double-check the generated method signature's exact parameter name for the header — it may be `xDocumensoSecret` per openapi-generator's camelCase conversion of `X-Documenso-Secret`; match whatever `WebhooksApi.java` actually declares.)

- [ ] **Step 7: Run the webhook service test, verify pass**

Run: `mvn clean install -DskipTests -pl buurman-notifications -am && mvn test -pl buurman-notifications -am -Dtest=SignatureWebhookServiceTest`
Expected: 6 tests pass.

- [ ] **Step 8: Run the full notifications module test suite (guard against `WebhookController` regressions)**

Run: `mvn test -pl buurman-notifications -am`
Expected: all pass, including existing `WebhookServiceTest`/`WebhookControllerTest` if present.

- [ ] **Step 9: Commit**

```bash
git add openapi/src/paths/reference.yaml openapi/src/app.yaml openapi/app.yaml backend/buurman-notifications/src/main/java/com/buurman/controller/WebhookController.java backend/buurman-notifications/src/main/java/com/buurman/service/notification/SignatureWebhookService.java backend/buurman-notifications/src/test/java/com/buurman/service/notification/SignatureWebhookServiceTest.java
git commit -m "feat(esignature): process Documenso webhooks and persist the signed PDF"
```

---

## Task 9: Frontend — hooks, components, feature-gated UI

**Files:**
- Modify: `frontend/app/src/constants/featureFlags.ts`
- Modify: `frontend/app/src/lib/queryKeys.ts`
- Create: `frontend/app/src/hooks/useSignatureRequestHooks.ts`
- Create: `frontend/app/src/components/documents/SignatureStatusBadge.tsx`
- Create: `frontend/app/src/components/documents/SignatureRequestPanel.tsx`
- Modify: `frontend/app/src/components/properties/DocumentList.tsx` (add optional `renderRowAction` slot)
- Modify: `frontend/app/src/components/contracts/ContractDocumentsTab.tsx` (wire the slot to `SignatureRequestPanel`, behind `FeatureGate`)
- Test: `frontend/app/src/components/documents/__tests__/SignatureStatusBadge.test.tsx`
- Test: `frontend/app/src/components/documents/__tests__/SignatureRequestPanel.test.tsx`

**Interfaces:**
- Consumes: generated `createSignatureRequest`/`getSignatureRequest` functions (from Task 7's OpenAPI bundle, `yarn generate:api`), `queryKeys`, `useMutationWithToast`, `FeatureGate`.

- [ ] **Step 1: Regenerate the frontend API client**

Run: `cd frontend && yarn generate:api`
Expected: `frontend/app/src/generated/api/signatures/signatures.ts` now exists, exporting `createSignatureRequest(contractIdentifier, documentIdentifier)` and `getSignatureRequest(contractIdentifier, documentIdentifier, signatureRequestIdentifier)`; `frontend/app/src/generated/models/` gains `SignatureRequestResponse`/`SignatureSignerResponse` types.

- [ ] **Step 2: Add the feature flag constant**

Edit `frontend/app/src/constants/featureFlags.ts`, add `ESIGNATURE_ENABLED: 'esignature_enabled',` to the `FeatureFlags` object.

- [ ] **Step 3: Add query keys**

Edit `frontend/app/src/lib/queryKeys.ts`, add under the `contracts` (or a new top-level) section:

```ts
  signatureRequests: {
    detail: (
      contractId?: string,
      documentId?: string,
      signatureRequestId?: string
    ) => k('signatureRequest', contractId, documentId, signatureRequestId),
  },
```

- [ ] **Step 4: Write the hooks**

`frontend/app/src/hooks/useSignatureRequestHooks.ts`:

```ts
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';
import {
  createSignatureRequest,
  getSignatureRequest,
} from '../generated/api/signatures/signatures';
import type { SignatureRequestResponse } from '../generated/models';
import { queryKeys } from '../lib/queryKeys';

const TERMINAL_STATUSES: SignatureRequestResponse['status'][] = [
  'COMPLETED',
  'DECLINED',
  'CANCELLED',
  'FAILED',
];

export const useSignatureRequest = (
  contractId: string | undefined,
  documentId: string | undefined,
  signatureRequestId: string | undefined
) => {
  return useQuery({
    queryKey: queryKeys.signatureRequests.detail(
      contractId,
      documentId,
      signatureRequestId
    ),
    queryFn: () =>
      getSignatureRequest(
        contractId ?? '',
        documentId ?? '',
        signatureRequestId ?? ''
      ),
    enabled: !!contractId && !!documentId && !!signatureRequestId,
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      return status && TERMINAL_STATUSES.includes(status) ? false : 3000;
    },
  });
};

export const useCreateSignatureRequest = (contractId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Sent for signature',
    mutationFn: (documentId: string) =>
      createSignatureRequest(contractId, documentId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.documents(contractId),
      });
    },
  });
};
```

- [ ] **Step 5: Write `SignatureStatusBadge`**

```tsx
import type { SignatureRequestResponse } from '@/generated/models';

const LABELS: Record<SignatureRequestResponse['status'], string> = {
  PENDING: 'Pending',
  PARTIALLY_SIGNED: 'Partially signed',
  COMPLETED: 'Signed',
  DECLINED: 'Declined',
  CANCELLED: 'Cancelled',
  FAILED: 'Failed',
};

const COLORS: Record<SignatureRequestResponse['status'], string> = {
  PENDING: 'bg-warning-bg text-warning-text',
  PARTIALLY_SIGNED: 'bg-info-bg text-info-text',
  COMPLETED: 'bg-success-bg text-success-text',
  DECLINED: 'bg-error-bg text-error-text',
  CANCELLED: 'bg-surface-inset text-text-secondary',
  FAILED: 'bg-error-bg text-error-text',
};

interface SignatureStatusBadgeProps {
  status: SignatureRequestResponse['status'];
}

export const SignatureStatusBadge = ({
  status,
}: SignatureStatusBadgeProps) => (
  <span
    className={`px-2 py-0.5 text-xs font-medium rounded-full ${COLORS[status]}`}
  >
    {LABELS[status]}
  </span>
);
```

- [ ] **Step 6: Write `SignatureRequestPanel`**

A small self-contained widget: shows a "Send for signature" button when there's no active request for this document yet, otherwise the badge + signer list.

```tsx
import { useState } from 'react';
import { PenLine } from 'lucide-react';
import { SignatureStatusBadge } from './SignatureStatusBadge';
import {
  useSignatureRequest,
  useCreateSignatureRequest,
} from '@/hooks/useSignatureRequestHooks';
import { LoadingSpinner } from '@buurman/ui';

interface SignatureRequestPanelProps {
  contractId: string;
  documentId: string;
}

export const SignatureRequestPanel = ({
  contractId,
  documentId,
}: SignatureRequestPanelProps) => {
  const [signatureRequestId, setSignatureRequestId] = useState<string>();
  const createMutation = useCreateSignatureRequest(contractId);
  const { data: request, isLoading } = useSignatureRequest(
    contractId,
    documentId,
    signatureRequestId
  );

  const handleSend = async () => {
    const created = await createMutation.mutateAsync(documentId);
    setSignatureRequestId(created.identifier);
  };

  if (!signatureRequestId) {
    return (
      <button
        type="button"
        onClick={handleSend}
        disabled={createMutation.isPending}
        className="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium text-primary-500 hover:bg-primary-50 rounded-md transition-colors disabled:opacity-50"
      >
        <PenLine className="h-4 w-4" />
        {createMutation.isPending ? 'Sending…' : 'Send for signature'}
      </button>
    );
  }

  if (isLoading || !request) {
    return <LoadingSpinner size="sm" />;
  }

  return (
    <div className="flex items-center gap-2">
      <SignatureStatusBadge status={request.status} />
      <span className="text-xs text-text-secondary">
        {request.signers.filter((s) => s.status === 'SIGNED').length}/
        {request.signers.length} signed
      </span>
    </div>
  );
};
```

- [ ] **Step 7: Add the row-action slot to `DocumentList`**

Add a new optional prop and render it next to the existing action buttons (desktop table only — mobile card list keeps its existing swipe actions unchanged, since this feature is a secondary action, not a primary swipe gesture):

```tsx
interface DocumentListProps {
  documents: DocumentResponse[];
  isLoading: boolean;
  error: Error | null;
  onUpload: (file: File, title?: string, notes?: string) => Promise<void>;
  onDelete: (documentId: string) => Promise<void>;
  isUploading: boolean;
  isDeleting: boolean;
  readOnly?: boolean;
  renderRowAction?: (doc: DocumentResponse) => React.ReactNode;
}
```

In the desktop `<table>` row's actions cell (the `<div className="flex justify-end gap-1.5" ...>` block), add before the closing `</div>`:

```tsx
                          {renderRowAction?.(doc)}
```

And destructure `renderRowAction` in the component's parameter list alongside the other props.

- [ ] **Step 8: Wire it in `ContractDocumentsTab`**

```tsx
import {
  useContractDocuments,
  useUploadContractDocument,
  useDeleteContractDocument,
} from '@/hooks/useContractHooks';
import { DocumentList } from '@/components/properties/DocumentList';
import { SignatureRequestPanel } from '@/components/documents/SignatureRequestPanel';
import { FeatureGate } from '@/components/FeatureGate';
import { FeatureFlags } from '@/constants/featureFlags';
import { useTeam } from '@/context/TeamContext';

interface ContractDocumentsTabProps {
  contractId: string;
}

export const ContractDocumentsTab = ({
  contractId,
}: ContractDocumentsTabProps) => {
  const { canEditData } = useTeam();

  const {
    data: documents = [],
    isLoading: docsLoading,
    error: docsError,
  } = useContractDocuments(contractId);
  const uploadDocumentMutation = useUploadContractDocument(contractId);
  const deleteDocumentMutation = useDeleteContractDocument(contractId);

  const handleUploadDocument = async (
    file: File,
    title?: string,
    notes?: string
  ) => {
    await uploadDocumentMutation.mutateAsync({ file, title, notes });
  };

  const handleDeleteDocument = async (documentId: string) => {
    await deleteDocumentMutation.mutateAsync(documentId);
  };

  return (
    <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
      <DocumentList
        documents={documents}
        onUpload={handleUploadDocument}
        onDelete={handleDeleteDocument}
        isLoading={docsLoading}
        error={docsError}
        isUploading={uploadDocumentMutation.isPending}
        isDeleting={deleteDocumentMutation.isPending}
        readOnly={!canEditData}
        renderRowAction={
          canEditData
            ? (doc) => (
                <FeatureGate flag={FeatureFlags.ESIGNATURE_ENABLED}>
                  <SignatureRequestPanel
                    contractId={contractId}
                    documentId={doc.identifier}
                  />
                </FeatureGate>
              )
            : undefined
        }
      />
    </div>
  );
};
```

- [ ] **Step 9: Write `SignatureStatusBadge.test.tsx`**

```tsx
import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { SignatureStatusBadge } from '../SignatureStatusBadge';

describe('SignatureStatusBadge', () => {
  it.each([
    ['PENDING', 'Pending'],
    ['PARTIALLY_SIGNED', 'Partially signed'],
    ['COMPLETED', 'Signed'],
    ['DECLINED', 'Declined'],
    ['CANCELLED', 'Cancelled'],
    ['FAILED', 'Failed'],
  ] as const)('renders %s as "%s"', (status, label) => {
    render(<SignatureStatusBadge status={status} />);
    expect(screen.getByText(label)).toBeInTheDocument();
  });
});
```

- [ ] **Step 10: Write `SignatureRequestPanel.test.tsx`**

Check an existing test file in `frontend/app/src/components/**/__tests__/` first for this codebase's exact React Query test-wrapper convention (a shared `renderWithQueryClient` helper is likely already used elsewhere — reuse it rather than hand-rolling a `QueryClientProvider` wrapper here) and match it. The test itself:

```tsx
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { SignatureRequestPanel } from '../SignatureRequestPanel';
import * as signaturesApi from '@/generated/api/signatures/signatures';

// Replace this import/wrapper with whatever this codebase's existing shared
// React-Query test helper is (see note above) — this local reimplementation
// is a fallback only if none exists yet.
import { renderWithQueryClient } from '@/test-utils/renderWithQueryClient';

describe('SignatureRequestPanel', () => {
  it('shows a Send for signature button, then the status badge after sending', async () => {
    vi.spyOn(signaturesApi, 'createSignatureRequest').mockResolvedValue({
      identifier: 'SGR00000000000000000000001',
      documentIdentifier: 'DOC00000000000000000000001',
      status: 'PENDING',
      signers: [],
      createdAt: '2026-03-01T12:00:00Z',
      updatedAt: '2026-03-01T12:00:00Z',
    });
    vi.spyOn(signaturesApi, 'getSignatureRequest').mockResolvedValue({
      identifier: 'SGR00000000000000000000001',
      documentIdentifier: 'DOC00000000000000000000001',
      status: 'PENDING',
      signers: [
        {
          email: 'tenant@example.com',
          role: 'TENANT',
          status: 'PENDING',
        },
      ],
      createdAt: '2026-03-01T12:00:00Z',
      updatedAt: '2026-03-01T12:00:00Z',
    });

    renderWithQueryClient(
      <SignatureRequestPanel
        contractId="CON00000000000000000000001"
        documentId="DOC00000000000000000000001"
      />
    );

    const button = screen.getByRole('button', { name: /send for signature/i });
    await userEvent.click(button);

    await waitFor(() => {
      expect(screen.getByText('Pending')).toBeInTheDocument();
      expect(screen.getByText('0/1 signed')).toBeInTheDocument();
    });
  });
});
```

- [ ] **Step 11: Run the frontend test suite**

Run: `cd frontend && yarn test`
Expected: all existing tests still pass, plus the 8 new assertions above (6 badge cases + 1 panel flow, spread across the two files' `it`/`it.each` blocks).

- [ ] **Step 12: Lint**

Run: `cd frontend && yarn lint`
Expected: no new errors. Fix any with `yarn lint --fix` if they're auto-fixable style issues.

- [ ] **Step 13: Manual smoke check**

Run: `cd frontend && yarn dev` (with backend running and `esignature_enabled` flag turned on for your dev team via the backoffice flag admin UI), open a contract's Documents tab, confirm the "Send for signature" button appears on each row and the badge updates after clicking (Documenso sidecar from Task 10 must be running for this to actually succeed end-to-end — a 502 from `DocumensoConfig`'s `RestClient` is expected until Task 10 lands).

- [ ] **Step 14: Commit**

```bash
git add frontend/app/src
git commit -m "feat(esignature): add Send for signature UI on contract documents"
```

---

## Task 10: Local dev infrastructure (Docker, config)

**Files:**
- Modify: `docker-compose.yml` (add `documenso` + `documenso-postgres` services)
- Modify: `.env.example` (document the new env vars)
- Modify: `scripts/setup-local-certs.sh` (add Documenso signing-certificate generation)
- Modify: `CLAUDE.md` (add Documenso to the services table and local-dev steps — small, factual addition only)

**Interfaces:**
- Produces: a running Documenso instance reachable at `http://localhost:3001` from a host-run backend (`make dev`) and at `http://documenso:3000` from a containerized backend (`make up`), with webhooks configured to reach the Buurman backend.

- [ ] **Step 1: Add certificate generation to `scripts/setup-local-certs.sh`**

Read the existing script first to match its style (it already generates mkcert wildcard certs), then append a section generating Documenso's self-signed PKCS#12 signing certificate if it doesn't already exist:

```bash
# Documenso e-signature sidecar: self-signed PKCS#12 signing certificate.
# Grounded against https://docs.documenso.com self-hosting docs — Documenso refuses to
# sign documents without a certificate present at startup.
DOCUMENSO_CERT_DIR="docker/documenso"
DOCUMENSO_CERT_PATH="$DOCUMENSO_CERT_DIR/cert.p12"
if [ ! -f "$DOCUMENSO_CERT_PATH" ]; then
  mkdir -p "$DOCUMENSO_CERT_DIR"
  echo "Generating Documenso local signing certificate..."
  openssl genrsa -out /tmp/documenso-private.key 2048
  openssl req -new -x509 -key /tmp/documenso-private.key -out /tmp/documenso-cert.crt -days 365 \
    -subj "/C=NL/ST=NH/L=Amsterdam/O=Buurman Dev/OU=Engineering/CN=Buurman Dev Signing CA"
  openssl pkcs12 -export -out "$DOCUMENSO_CERT_PATH" \
    -inkey /tmp/documenso-private.key -in /tmp/documenso-cert.crt \
    -passout pass:buurman-dev
  rm /tmp/documenso-private.key /tmp/documenso-cert.crt
  echo "Documenso certificate written to $DOCUMENSO_CERT_PATH (passphrase: buurman-dev, local dev only)"
fi
```

- [ ] **Step 2: Add `.gitignore` entry for the generated cert**

Check `.gitignore` for other local-only generated artifacts (e.g. how the mkcert certs are excluded) and add the same pattern for `docker/documenso/cert.p12` — this is a locally-generated secret-bearing file and must never be committed.

- [ ] **Step 3: Add the Documenso services to `docker-compose.yml`**

Insert after the `mailpit:` service (reusing its SMTP for Documenso's own notification emails, and following the exact sidecar/healthcheck/network conventions used by `gotenberg`/`mailpit` above):

```yaml
  documenso-postgres:
    image: postgres:15
    environment:
      - POSTGRES_USER=documenso
      - POSTGRES_PASSWORD=documenso
      - POSTGRES_DB=documenso
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U documenso -d documenso"]
      interval: 10s
      timeout: 5s
      retries: 5
    volumes:
      - documenso-postgres-data:/var/lib/postgresql/data
    networks:
      - buurman-network

  documenso:
    image: documenso/documenso:latest
    depends_on:
      documenso-postgres:
        condition: service_healthy
    environment:
      - PORT=3000
      - NEXTAUTH_SECRET=${DOCUMENSO_NEXTAUTH_SECRET:-dev-nextauth-secret-not-for-production}
      - NEXT_PRIVATE_ENCRYPTION_KEY=${DOCUMENSO_ENCRYPTION_KEY:-dev-encryption-key-32-chars-long}
      - NEXT_PRIVATE_ENCRYPTION_SECONDARY_KEY=${DOCUMENSO_ENCRYPTION_SECONDARY_KEY:-dev-secondary-key-32-chars-long}
      - NEXT_PUBLIC_WEBAPP_URL=${DOCUMENSO_WEBAPP_URL:-http://localhost:3001}
      - NEXT_PRIVATE_DATABASE_URL=postgres://documenso:documenso@documenso-postgres:5432/documenso
      - NEXT_PRIVATE_SMTP_TRANSPORT=smtp-auth
      - NEXT_PRIVATE_SMTP_HOST=mailpit
      - NEXT_PRIVATE_SMTP_PORT=1025
      - NEXT_PRIVATE_SMTP_FROM_NAME=Buurman Signing (Dev)
      - NEXT_PRIVATE_SMTP_FROM_ADDRESS=signing@local.buurman.io
      - NEXT_PRIVATE_SIGNING_TRANSPORT=local
      - NEXT_PRIVATE_SIGNING_LOCAL_FILE_PATH=/opt/documenso/cert.p12
      - NEXT_PRIVATE_SIGNING_PASSPHRASE=buurman-dev
    ports:
      # Host port so a host-run backend (`make dev`) can reach it at http://localhost:3001.
      # Containerised backend (`make up`) reaches it as http://documenso:3000.
      - "${DOCUMENSO_HOST_PORT:-3001}:3000"
    volumes:
      - ./docker/documenso/cert.p12:/opt/documenso/cert.p12:ro
    labels:
      - "buurman.workspace=${COMPOSE_PROJECT_NAME:-buurman}"
    networks:
      - buurman-network
```

Add `documenso-postgres-data:` to the top-level `volumes:` section, next to `postgres_data`/`awrust-data`.

- [ ] **Step 4: Add env var documentation to `.env.example`**

Read the file first to match its existing section-comment style (e.g. how the Mailgun/Twilio section is introduced), then add:

```bash
# Documenso (self-hosted e-signature sidecar)
DOCUMENSO_HOST_PORT=3001
DOCUMENSO_BASE_URL=http://localhost:3001
DOCUMENSO_API_KEY=
DOCUMENSO_WEBHOOK_SECRET=
DOCUMENSO_NEXTAUTH_SECRET=
DOCUMENSO_ENCRYPTION_KEY=
DOCUMENSO_ENCRYPTION_SECONDARY_KEY=
DOCUMENSO_WEBAPP_URL=http://localhost:3001
```

- [ ] **Step 5: Start the stack and verify Documenso comes up healthy**

Run: `make dev` (or `make up` if that's the profile you're testing)
Run: `docker compose ps documenso documenso-postgres`
Expected: both `Up`/`healthy`.
Run: `curl -sf http://localhost:3001/api/v2/envelope -H "Authorization: invalid" | head -c 200`
Expected: some JSON error response (not a connection refused) — confirms the API is reachable. A real `DOCUMENSO_API_KEY` still needs to be created once through Documenso's own web UI at `http://localhost:3001` (first-run signup) — this is a one-time manual step per environment, not something to script, since it involves creating the first admin account interactively.

- [ ] **Step 6: Configure the webhook manually (one-time, documented, not scripted)**

In the Documenso web UI (`http://localhost:3001` → Settings → Webhooks), add a webhook pointing at:
- `local` profile (host-run backend): `http://host.docker.internal:8081/webhooks/documenso/events` (Documenso's container needs Docker Desktop's `host.docker.internal` DNS to reach the host-run backend — this only works with Docker Desktop, not plain Linux Docker Engine without the `--add-host=host.docker.internal:host-gateway` flag; note this limitation in `CLAUDE.md`'s Common Issues section if it isn't already covered by an existing host-reachability note).
- `docker` profile (containerized backend): `http://backend:8081/webhooks/documenso/events` (same-network service-name DNS, no special config).

Set the webhook secret to match `DOCUMENSO_WEBHOOK_SECRET` in `.env`.

- [ ] **Step 7: Update `CLAUDE.md`**

Add a `documenso` row to the Docker Services table:

| Service | URL | Purpose |
|---------|-----|---------|
| documenso | http://localhost:3001 (no Traefik route — internal signing sidecar) | Self-hosted e-signature (Documenso) |

And one line under "Common Issues" noting the `host.docker.internal` webhook-reachability requirement from Step 6.

- [ ] **Step 8: Commit**

```bash
git add docker-compose.yml .env.example scripts/setup-local-certs.sh .gitignore CLAUDE.md
git commit -m "feat(esignature): add self-hosted Documenso sidecar to local dev infrastructure"
```

---

## Final verification (whole-branch)

- [ ] Run: `make test` (from repo root) — full backend suite, install-then-test.
Expected: all pass, including the ~20 new backend tests across Tasks 3/5/6/8.
- [ ] Run: `cd frontend && yarn test && yarn lint`
Expected: all pass.
- [ ] Run: `make bundle-openapi && git diff --exit-code openapi/app.yaml`
Expected: no diff (bundle is already up to date from Tasks 7/8).
- [ ] Manually walk the golden path once with Documenso running: generate a rent-increase letter on a contract with a tenant contact that has an email → click "Send for signature" → sign as both landlord and tenant using the emailed Documenso links (check Mailpit at `https://mailpit.local.buurman.io` for both the Documenso signing-invite emails and the "signed" notification) → confirm the signed PDF appears in the contract's Documents tab within a few seconds of the last signature.
