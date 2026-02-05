# Buurman Backend — Architecture Review Report
## Generated: 2026-02-05
## Scope: backend/src/main/java/com/buurman/ (~187 Java files, 25 Flyway migrations)

---

# Executive Summary

- **Critical Issues**: 8
- **High Priority**: 17
- **Medium Priority**: 30
- **Low Priority**: 12

### Top 5 Issues Requiring Immediate Attention

1. **TenantAddressRepository UPDATE has no team_id in WHERE clause** — `TenantAddressRepository.java:74` — allows cross-team data modification
2. **All response DTOs expose internal UUIDs instead of ULID identifiers** — violates architecture rule, 20+ response DTOs affected
3. **GlobalExceptionHandler leaks raw exception messages to clients** — `GlobalExceptionHandler.java:41` — information disclosure vulnerability
4. **Hardcoded credentials in application.yml** — Keycloak admin `admin:admin` and AWS `test:test` committed to source control
5. **TeamRepository.save() UPDATE sets CREATED_BY instead of UPDATED_BY** — `TeamRepository.java:74` — silently corrupts audit trail

---

# Detailed Findings

## Category 1: MULTI-TENANCY INTEGRITY

### Finding 1.1 [SEVERITY: Critical]
- **Location**: `repository/TenantAddressRepository.java:74`
- **Current Code**:
  ```java
  dsl.update(TENANT_ADDRESSES)
      .set(...)
      .where(TENANT_ADDRESSES.ID.eq(address.getId()))  // NO team_id check!
      .execute();
  ```
- **Issue**: The UPDATE statement uses only the record ID in its WHERE clause, without filtering by `team_id`. Any authenticated user who knows an address UUID can modify addresses belonging to other teams.
- **Recommendation**: Add `.and(TENANT_ADDRESSES.TEAM_ID.eq(address.getTeamId()))` to the WHERE clause.
- **Effort**: Small

### Finding 1.2 [SEVERITY: Critical]
- **Location**: `repository/TeamMemberRepository.java:42`, `repository/TeamInvitationRepository.java:47`
- **Issue**: `findById()` methods have no `team_id` filter. If a controller passes a member/invitation ID without team context, cross-team access is possible. Currently mitigated by service-layer `findByIdAndTeamId` pattern elsewhere, but these methods exist and could be misused.
- **Recommendation**: Remove the unscoped `findById()` methods or add team_id parameter.
- **Effort**: Small

### Finding 1.3 [SEVERITY: High]
- **Location**: `controller/TenantController.java:222,231,242`
- **Issue**: The address sub-resource endpoints (`getAddress`, `updateAddress`, `deleteAddress`) accept `{tenantId}` in the URL path but **completely ignore it** — only `addressId` is passed to the service. Combined with Finding 1.1 (TenantAddressRepository UPDATE missing team_id), this means:
  - `GET /api/tenants/{anyTenantId}/addresses/{realAddressId}` returns the address regardless of tenant ownership
  - `PUT /api/tenants/{anyTenantId}/addresses/{realAddressId}` can modify addresses not belonging to that tenant
  - The URL contract is broken — the parent resource ID is unchecked
- **Recommendation**: Pass `tenantId` to service methods and validate the address belongs to that tenant. Add team_id filtering to TenantAddressRepository.
- **Effort**: Small

### Finding 1.4 [SEVERITY: Medium]
- **Location**: `controller/TeamController.java` — all endpoints accept `{teamId}` from URL path
- **Issue**: Every TeamController endpoint takes `{teamId}` as a path variable. If the service does not verify that the authenticated user's `principal.getTeamId()` matches the URL's `teamId`, any authenticated user could operate on any team by manipulating the URL. Defense depends entirely on service-layer validation.
- **Recommendation**: Add a controller-level guard: `if (!teamId.equals(principal.getTeamId())) throw new CrossTeamAccessException()` or use a Spring interceptor.
- **Effort**: Small

### Finding 1.5 [SEVERITY: Medium]
- **Location**: All 16 repositories
- **Issue**: No `TeamAwareRepository` base class. Every query manually appends `.and(TEAM_ID.eq(teamId))`. Forgetting this in a single method causes a data leak. The TenantAddressRepository finding (1.1) proves this risk is real.
- **Recommendation**: Create a base repository class or JOOQ query interceptor that automatically appends `team_id` filtering to all queries.
- **Effort**: Large

---

## Category 2: DATA INTEGRITY & AUDIT TRAIL

### Finding 2.1 [SEVERITY: Critical]
- **Location**: `repository/TeamRepository.java:74`
- **Current Code**:
  ```java
  .set(TEAMS.CREATED_BY, team.getCreatedBy())  // Should be UPDATED_BY
  ```
- **Issue**: The UPDATE branch of `save()` sets `CREATED_BY` instead of `UPDATED_BY`. This silently corrupts the audit trail — the original creator is overwritten, and the updater is never recorded.
- **Recommendation**: Change to `.set(TEAMS.UPDATED_BY, team.getUpdatedBy())`.
- **Effort**: Small

### Finding 2.2 [SEVERITY: High]
- **Location**: `repository/TeamRepository.java:105-109`, `repository/UserRepository.java:74-78`, `repository/TeamMemberRepository.java:70-74`, `repository/TeamInvitationRepository.java:86-90`
- **Current Code**:
  ```java
  public void deleteById(UUID id) {
      dsl.deleteFrom(TEAMS).where(TEAMS.ID.eq(id)).execute();  // HARD DELETE
  }
  ```
- **Issue**: Four repositories implement hard `DELETE FROM` instead of soft delete (`SET deleted_at = NOW()`). This violates the project's soft-delete pattern and breaks the audit trail permanently.
- **Recommendation**: Replace with soft delete pattern: `dsl.update(TABLE).set(DELETED_AT, now).where(...)`.
- **Effort**: Small

### Finding 2.3 [SEVERITY: High]
- **Location**: `service/AuthService.java` (register method)
- **Issue**: Registration creates a Keycloak user first, then creates database records (user, team, member). If the database insert fails after Keycloak succeeds, an orphaned Keycloak user exists with no compensation/rollback logic.
- **Recommendation**: Implement a compensation pattern — if DB creation fails, delete the Keycloak user. Or use the Saga pattern.
- **Effort**: Medium

### Finding 2.4 [SEVERITY: Medium]
- **Location**: All 18 domain entities
- **Issue**: No `equals()` or `hashCode()` implementations on any domain entity. This causes incorrect behavior when entities are used in `Set` collections, `HashMap` keys, or compared in tests. Objects are compared by reference only.
- **Recommendation**: Add `equals()`/`hashCode()` based on `id` field to all domain entities.
- **Effort**: Medium

---

## Category 3: TRANSACTION MANAGEMENT

### Finding 3.1 [SEVERITY: High]
- **Location**: `service/PropertyService.java` (createProperty, updateProperty methods)
- **Issue**: Write operations (insert + audit log) lack `@Transactional`. If the audit log write fails after the property is created, the database is in an inconsistent state.
- **Recommendation**: Add `@Transactional` to all write methods in PropertyService.
- **Effort**: Small

### Finding 3.2 [SEVERITY: High]
- **Location**: `service/PaymentSchedulingService.java:51-52`
- **Current Code**:
  ```java
  @Scheduled(cron = "0 0 * * * *")
  @Transactional  // One giant transaction for ALL teams
  public void scheduledPaymentGeneration() {
  ```
- **Issue**: The entire scheduled job runs in a single `@Transactional`. If team #50 fails, all preceding successful team saves may roll back. The per-team try/catch only handles exceptions at the service call level — if the transaction is rolled back at commit time, all work is lost.
- **Recommendation**: Remove `@Transactional` from the scheduler method. Each `generateFuturePaymentsForContract` already has its own `@Transactional`, which is sufficient.
- **Effort**: Small

---

## Category 4: SECURITY — AUTHENTICATION & AUTHORIZATION

### Finding 4.1 [SEVERITY: Critical]
- **Location**: `config/SecurityConfig.java:44`, `controller/DocumentController.java`
- **Issue**: `DocumentController` lacks `@SecurityRequirement(name = "bearer-jwt")` and has no `@PreAuthorize` on any method. While endpoints are protected by the security filter chain, the controller itself provides no role-based access control. Any authenticated user can upload, delete, and bulk-download documents.
- **Recommendation**: Add `@PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")` to mutating endpoints.
- **Effort**: Small

### Finding 4.2 [SEVERITY: Critical]
- **Location**: `exception/GlobalExceptionHandler.java:39-42`
- **Current Code**:
  ```java
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
      return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", ex.getMessage());
  }
  ```
- **Issue**: The catch-all handler passes raw `ex.getMessage()` to the client. This can leak database column names, SQL errors, file paths, and stack trace fragments.
- **Recommendation**: Return a generic message ("An unexpected error occurred") for unhandled exceptions. Log the full exception server-side.
- **Effort**: Small

### Finding 4.3 [SEVERITY: Critical]
- **Location**: `application.yml:57-61, 70-71`
- **Current Code**:
  ```yaml
  keycloak.admin:
    username: admin
    password: admin
  aws.s3:
    access-key: test
    secret-key: test
  ```
- **Issue**: Admin credentials hardcoded in configuration file committed to source control. No environment-specific profiles exist (`application-dev.yml`, `application-prod.yml`).
- **Recommendation**: Move all credentials to environment variables. Create `application-dev.yml` for local development and `application-prod.yml` using `${ENV_VAR}` syntax.
- **Effort**: Medium

### Finding 4.4 [SEVERITY: High]
- **Location**: `config/SecurityConfig.java:59-70`, `config/WebConfig.java`
- **Issue**: CORS configured in two places (SecurityConfig AND WebConfig) with `allowedHeaders("*")` combined with `allowCredentials(true)`. Wildcard headers with credentials is a dangerous combination.
- **Recommendation**: Remove duplicate CORS config (keep SecurityConfig only). Specify allowed headers explicitly: `["Authorization", "Content-Type", "Accept"]`.
- **Effort**: Small

### Finding 4.5 [SEVERITY: High]
- **Location**: `service/DocumentService.java:67-68`
- **Issue**: MIME type validation relies solely on the `Content-Type` header sent by the client. An attacker can upload a malicious file (e.g., `.exe`, `.html`) with a faked `image/jpeg` Content-Type header.
- **Recommendation**: Validate file content using magic bytes (file signature) in addition to Content-Type header. Use Apache Tika or similar library.
- **Effort**: Medium

### Finding 4.6 [SEVERITY: High]
- **Location**: `controller/DocumentController.java` (bulkDownload method)
- **Issue**: `bulkDownload` accepts `@RequestBody List<UUID>` with no `@Valid`, no wrapper DTO, and no size constraint. An attacker can send thousands of document IDs, causing the server to load all files into a `ByteArrayOutputStream` ZIP (entire ZIP in memory), leading to memory exhaustion / OOM.
- **Recommendation**: Add a wrapper DTO with `@Size(max=50)` on the list. Stream the ZIP output instead of buffering in memory.
- **Effort**: Medium

### Finding 4.7 [SEVERITY: Medium]
- **Location**: `config/SecurityConfig.java:44`
- **Issue**: Swagger UI (`/swagger-ui/**`, `/v3/api-docs/**`) is accessible without authentication, exposing the full API surface to unauthenticated users.
- **Recommendation**: Disable Swagger in production via profile-specific configuration.
- **Effort**: Small

### Finding 4.7 [SEVERITY: Medium]
- **Location**: `application.yml:99-102`
- **Issue**: Actuator endpoints (`/actuator/metrics`, `/actuator/prometheus`) are exposed without authentication. These can leak system information (JVM stats, heap usage, endpoint timings).
- **Recommendation**: Require authentication for all actuator endpoints except `/actuator/health`.
- **Effort**: Small

### Finding 4.8 [SEVERITY: Medium]
- **Location**: `application.yml:114-115`
- **Current Code**:
  ```yaml
  server.error.include-message: always
  ```
- **Issue**: Combined with the catch-all exception handler, this ensures error details are always sent to clients.
- **Recommendation**: Set to `never` or `on_param` in production.
- **Effort**: Small

---

## Category 5: API DESIGN — UUID EXPOSURE

### Finding 5.1 [SEVERITY: Critical]
- **Location**: All response DTOs (35 files in `dto/response/`)
- **Issue**: Every response DTO exposes internal `UUID id` and often `UUID teamId` fields. Per the project architecture: *"External IDs use ULID (identifier). Internal IDs use UUID. NEVER expose internal UUIDs in URLs."* This is violated across the entire API surface.
- **Affected files**: `PropertyResponse`, `TenantResponse`, `ContractResponse`, `PaymentResponse`, `ExpenseResponse`, `DocumentResponse`, `UserResponse`, `TeamMemberResponse`, `InvitationResponse`, `PaymentReceivalResponse`, and all Summary DTOs.
- **Recommendation**: Remove `UUID id` and `UUID teamId` from all response DTOs. Expose `identifier` (ULID) as the external identifier. Update controllers to use identifiers for path variables.
- **Effort**: Large

### Finding 5.2 [SEVERITY: Medium]
- **Location**: `mapper/TeamMapper.java:34`
- **Current Code**:
  ```java
  @Mapping(target = "invitationUrl", expression = "java(\"http://localhost:5173/invitation/\" + invitation.getToken())")
  ```
- **Issue**: Hardcoded `localhost:5173` URL in production mapper code.
- **Recommendation**: Inject configurable base URL from `app.email.base-url` property.
- **Effort**: Small

### Finding 5.3 [SEVERITY: Medium]
- **Location**: `dto/response/DocumentResponse.java:11`
- **Issue**: Exposes S3 `fileKey` (internal storage path). Clients should only receive `downloadUrl` (presigned URL).
- **Recommendation**: Remove `fileKey` from DocumentResponse.
- **Effort**: Small

---

## Category 6: N+1 QUERY PATTERNS

### Finding 6.1 [SEVERITY: High]
- **Location**: `service/PaymentService.java` (enrichPaymentResponse method)
- **Issue**: For each payment in a list, the service executes separate queries for: receivals, contract, property, tenant, documents (proof of payment), documents (receipt). This is 6+ queries per payment.
- **Recommendation**: Create a dedicated repository method with JOINs to fetch payment + related data in a single query.
- **Effort**: Medium

### Finding 6.2 [SEVERITY: High]
- **Location**: `service/TeamService.java:38-40` (getTeamMembers)
- **Current Code**: Contains `// TODO: very inefficient, causes N+1 query problem`
- **Issue**: Calls `userRepository.findById()` per team member in a loop.
- **Recommendation**: Create `findUsersByIds(List<UUID>)` batch method or JOIN in repository.
- **Effort**: Medium

### Finding 6.3 [SEVERITY: High]
- **Location**: `service/ReportService.java` (multiple methods)
- **Issue**: `getPropertyIdForPayment()` called per payment in loop. `getOccupancyTrend()` fetches ALL contracts for EVERY month iteration. Extremely inefficient for large datasets.
- **Recommendation**: Rewrite with batch queries and date-range-based filtering.
- **Effort**: Large

### Finding 6.4 [SEVERITY: Medium]
- **Location**: `service/PropertyService.java` (toResponseWithMainPhoto), `service/TenantService.java` (toResponse), `service/ContractService.java` (toResponse), `service/ExpenseService.java` (enrichExpenseResponse)
- **Issue**: Each service fetches related data (photos, property lookups, documents) per entity in list operations.
- **Recommendation**: Batch-fetch related data and join in-memory.
- **Effort**: Medium

---

## Category 7: PAGINATION

### Finding 7.1 [SEVERITY: High]
- **Location**: All 16 repositories — `findAllByTeamId()` methods
- **Issue**: No repository method supports pagination. All list endpoints fetch every record in the table for a given team. As data grows, these queries will degrade performance and memory usage.
- **Recommendation**: Add `LIMIT/OFFSET` or cursor-based pagination to all list methods. Expose pagination parameters in controllers.
- **Effort**: Large

---

## Category 8: ERROR HANDLING

### Finding 8.1 [SEVERITY: Medium]
- **Location**: Multiple services (PropertyService, TenantService, ContractService, PaymentService, UserTeamService)
- **Issue**: Services throw `RuntimeException("Entity not found")` for missing entities. These are caught by the catch-all handler and returned as 500 Internal Server Error instead of 404.
- **Recommendation**: Create specific exception classes (e.g., `EntityNotFoundException`) handled by `GlobalExceptionHandler` with proper 404 status.
- **Effort**: Medium

### Finding 8.2 [SEVERITY: Medium]
- **Location**: `service/EmailService.java:130-132`
- **Issue**: `@Async` email methods silently catch and log all exceptions. The caller never knows if an email failed to send. For critical emails (invitations, payment reminders), this is a silent failure.
- **Recommendation**: Implement a retry mechanism and/or notification system for failed emails.
- **Effort**: Medium

### Finding 8.3 [SEVERITY: Low]
- **Location**: `controller/ExpenseController.java:64-69`, `controller/ContractController.java:76-81`
- **Issue**: Try-catch blocks that catch `IllegalArgumentException` and rethrow a new `IllegalArgumentException` with the same message. Pointless wrapping.
- **Recommendation**: Let the `GlobalExceptionHandler` handle `IllegalArgumentException` directly.
- **Effort**: Small

---

## Category 9: CODE DUPLICATION & MAINTAINABILITY

### Finding 9.1 [SEVERITY: Medium]
- **Location**: `service/ContractService.java` (updateContract method, ~80 lines)
- **Issue**: Massive manual field-by-field change detection with `if (request.getField() != null) { contract.setField(request.getField()); }` repeated for every field. Same pattern in `service/TenantAddressService.java`.
- **Recommendation**: Use MapStruct's `@MappingTarget` with null-value property mapping strategy, or a generic diff utility.
- **Effort**: Medium

### Finding 9.2 [SEVERITY: Medium]
- **Location**: All 16 repositories
- **Issue**: Massive code duplication. Every repository has nearly identical `save()` methods with manual field-by-field INSERT/UPDATE, identical `softDeleteByIdAndTeamId()`, identical timestamp handling. No shared base class.
- **Recommendation**: Extract a `BaseRepository<T>` with shared CRUD operations.
- **Effort**: Large

### Finding 9.3 [SEVERITY: Medium]
- **Location**: `service/ExportService.java` (~300 lines of HTML string building)
- **Issue**: PDF/HTML generation uses inline string concatenation to build HTML documents. Fragile, untestable, and impossible to maintain.
- **Recommendation**: Use Thymeleaf templates (already a dependency) for HTML generation.
- **Effort**: Medium

### Finding 9.4 [SEVERITY: Low]
- **Location**: `repository/PaymentReceivalRepository.java`
- **Issue**: Uses manual `DSL.field("column_name", Type.class)` and `DSL.table("table_name")` instead of JOOQ-generated table references used by all other repositories. Inconsistent and loses compile-time type safety.
- **Recommendation**: Use JOOQ-generated `PAYMENT_RECEIVALS` table reference.
- **Effort**: Small

---

## Category 10: DTO VALIDATION

### Finding 10.1 [SEVERITY: High]
- **Location**: `dto/request/CreatePaymentRequest.java:18`
- **Issue**: `currency` field missing `@NotNull` validation. Null currency breaks financial calculations.
- **Recommendation**: Add `@NotNull(message = "Currency is required")`.
- **Effort**: Small

### Finding 10.2 [SEVERITY: Medium]
- **Location**: `dto/request/CreateTenantAddressRequest.java:22`
- **Issue**: `status` field missing `@NotNull` validation.
- **Recommendation**: Add `@NotNull`.
- **Effort**: Small

### Finding 10.3 [SEVERITY: Medium]
- **Location**: `dto/request/CreateTenantAddressRequest.java:24-26`, `dto/request/UpdateTenantAddressRequest.java:25-27`
- **Issue**: Coordinates use `Double` type instead of `BigDecimal`. Property DTOs correctly use `BigDecimal`. Inconsistency causes precision loss.
- **Recommendation**: Change to `BigDecimal` for latitude/longitude.
- **Effort**: Small

### Finding 10.4 [SEVERITY: Medium]
- **Location**: Multiple request DTOs (UploadDocumentRequest, LinkTenantToPropertyRequest, SwitchTeamRequest, CreateContractRequest, etc.)
- **Issue**: Request DTOs accept `UUID` parameters for entity references. Per architecture rules, clients should send `identifier` (ULID), and the service layer should resolve to UUID internally.
- **Recommendation**: Change UUID parameters to String identifiers in request DTOs. Add service-layer resolution.
- **Effort**: Large

### Finding 10.5 [SEVERITY: High]
- **Location**: `CreateContractRequest`, `UpdateContractRequest`, `FinancialOverviewRequest`, `TransactionHistoryRequest`, `GenerateReportRequest`
- **Issue**: Zero cross-field date validation across all 5 DTOs with `startDate`/`endDate` pairs. No `@AssertTrue` or class-level constraint verifies `startDate < endDate`. Malformed date ranges reach the service layer unchecked.
- **Recommendation**: Add a custom `@AssertTrue` method or class-level `@ValidDateRange` constraint to all DTOs with date pairs.
- **Effort**: Small

### Finding 10.6 [SEVERITY: Medium]
- **Location**: ~40+ string fields across all request DTOs
- **Issue**: Only 4 DTOs (`CreateExpenseRequest`, `UpdateExpenseRequest`, `UpdateTeamRequest`, `UpdateUserProfileRequest`) have `@Size` constraints on string fields. All other string fields (names, addresses, notes, descriptions, currency codes) accept arbitrarily long input. A client can submit megabytes in a single field.
- **Recommendation**: Add `@Size(max=...)` to all string fields. Use `@Size(min=3, max=3)` for currency codes.
- **Effort**: Medium

### Finding 10.7 [SEVERITY: Medium]
- **Location**: `CreateContractRequest` — `depositAmount`, `securityDeposit`, `lateFeePercentage` fields; `CreatePropertyRequest`/`UpdatePropertyRequest` — `squareMeters` field
- **Issue**: 7 financial `BigDecimal` fields lack `@Positive` or `@PositiveOrZero` constraints. Database has CHECK constraints (> 0) but validation should happen at the DTO level first to provide proper error messages.
- **Recommendation**: Add `@PositiveOrZero` to deposit/security/fee fields, `@Positive` to squareMeters.
- **Effort**: Small

---

## Category 11: MAPPER LAYER

### Finding 11.1 [SEVERITY: Medium]
- **Location**: `mapper/ContractRecordMapper.java`, `mapper/ExpenseRecordMapper.java`, `mapper/PaymentRecordMapper.java`, `mapper/TeamRecordMapper.java`
- **Issue**: Manual `@Component` mapper classes only implement `toDomain()` — no reverse `toRecord()` method. While repository `save()` methods do field-by-field mapping, this creates two separate mapping paths that can drift.
- **Recommendation**: Either add `toRecord()` to manual mappers or convert to MapStruct interfaces for compile-time verification.
- **Effort**: Medium

### Finding 11.2 [SEVERITY: High]
- **Location**: All 6 update mappers: `TenantMapper`, `TenantAddressMapper`, `ContractMapper`, `PropertyMapper`, `ExpenseMapper`, `PaymentMapper`
- **Issue**: No mapper uses `@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)`. When a client sends a partial update with some fields as `null`, the `@MappingTarget` update methods **overwrite existing values with null**. This silently erases data on partial updates.
- **Recommendation**: Add `@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)` to all `updateEntity()` methods.
- **Effort**: Small

### Finding 11.3 [SEVERITY: Medium]
- **Location**: `mapper/TenantAddressMapper.java` (updateEntity method)
- **Issue**: Unlike all other update mappers which explicitly `@Mapping(target=..., ignore=true)` for sensitive fields (`id`, `teamId`, `tenantId`, audit fields), `TenantAddressMapper.updateEntity` has zero explicit ignores. It relies entirely on MapStruct's implicit name-matching behavior. If a future request DTO field name matches an entity field like `tenantId`, it will be silently overwritten.
- **Recommendation**: Add explicit `@Mapping(target = "id", ignore = true)` etc., matching the pattern used by all other update mappers.
- **Effort**: Small

### Finding 11.4 [SEVERITY: Medium]
- **Location**: `mapper/PaymentMapper.java:27-34`
- **Issue**: 8 fields ignored in `toResponse()` mapping (`contract`, `tenant`, `property`, `proofOfPayment`, `receipt`, `receivedAmount`, `balance`, `receivals`). These must be populated manually in the service layer. High risk of missing a field.
- **Recommendation**: Document the ignored fields clearly. Consider a builder or dedicated enrichment method.
- **Effort**: Small

---

## Category 12: CONFIGURATION & INFRASTRUCTURE

### Finding 12.1 [SEVERITY: Medium]
- **Location**: `application.yml` (entire file)
- **Issue**: Single configuration file for all environments. No `application-dev.yml` / `application-prod.yml` separation. Production deployment would use development credentials.
- **Recommendation**: Split into environment-specific profiles with proper credential management.
- **Effort**: Medium

### Finding 12.2 [SEVERITY: High]
- **Location**: `pom.xml` — Keycloak admin client 23.0.5, AWS SDK 2.20.0
- **Issue**: Keycloak client 23.x uses Jakarta EE 10 while Spring Boot 4.x requires Jakarta EE 11 — this may cause classpath conflicts. AWS SDK 2.20.0 is from 2023 and missing security patches. Both are significantly outdated.
- **Recommendation**: Update Keycloak to latest 26.x (Jakarta EE 11 compatible), AWS SDK to latest 2.x. Test thoroughly for compatibility.
- **Effort**: Medium

### Finding 12.3 [SEVERITY: Medium]
- **Location**: `security/MdcFilter.java:21-22`
- **Issue**: `TEAM_ID` and `USER_EMAIL` constants declared but never populated in MDC. Logs lack team context.
- **Recommendation**: Populate these fields from `UserPrincipal`.
- **Effort**: Small

### Finding 12.4 [SEVERITY: Low]
- **Location**: `domain/TeamMember.java` — `role` field
- **Issue**: `TeamMember.role` is a `String` while all other enum-constrained fields use proper Java enums (e.g., `Contract.ContractStatus`, `Payment.PaymentStatus`). The database has a CHECK constraint for valid role values, but the Java code has no compile-time enforcement.
- **Recommendation**: Create a `TeamMember.Role` enum and use it instead of String.
- **Effort**: Small

### Finding 12.5 [SEVERITY: Low]
- **Location**: `service/AuditService.java:22-23`
- **Issue**: Imports JOOQ generated table references (`AUDIT_LOG`, `USERS`) directly in the service layer. This leaks the persistence layer abstraction into business logic.
- **Recommendation**: Move JOOQ-dependent code to repository layer.
- **Effort**: Small

### Finding 12.6 [SEVERITY: Low]
- **Location**: `repository/TeamRepository.java:87`
- **Issue**: Raw SQL string `"settings->'payments'->>'autoGenerationEnabled' = 'true'"` embedded in JOOQ query. Bypasses JOOQ's type safety.
- **Recommendation**: Use JOOQ's JSON functions for type-safe JSONB queries.
- **Effort**: Small

---

## Category 13: DATABASE MIGRATIONS

### Finding 13.1 [SEVERITY: Medium]
- **Location**: `V003__create_documents.sql`
- **Issue**: Documents table has no `identifier` field. Every other entity has a ULID identifier for external use. Documents can only be referenced by internal UUID.
- **Recommendation**: Add `identifier VARCHAR(26) NOT NULL` with a UNIQUE constraint.
- **Effort**: Small

### Finding 13.2 [SEVERITY: Medium]
- **Location**: `V001__create_base_schema.sql:26`
- **Issue**: `team_members` user foreign key uses `ON DELETE CASCADE`. If a user is deleted, all their team memberships silently disappear. This should be `ON DELETE RESTRICT` to prevent accidental data loss.
- **Recommendation**: Change cascade behavior and handle user removal explicitly.
- **Effort**: Small

### Finding 13.3 [SEVERITY: Medium]
- **Location**: `V001__create_base_schema.sql` (teams, users, team_members tables)
- **Issue**: The `teams`, `users`, and `team_members` tables have **no `deleted_at` column**. This means hard deletes are the only option for these entities, violating the project's stated "always use soft deletes" policy. Combined with Finding 2.2 (hard DELETE methods in repositories), user/team removal permanently destroys data.
- **Recommendation**: Add `deleted_at TIMESTAMP` to these tables via a new migration. Update repositories to use soft delete.
- **Effort**: Medium

### Finding 13.4 [SEVERITY: Medium]
- **Location**: `V004__create_audit_log.sql`
- **Issue**: The `idx_audit_log_entity` index is on `(entity_type, entity_id)` without `team_id`. Audit log queries always filter by `team_id` (via the 5-way JOIN in `AuditLogRepository`), but the index doesn't support this, forcing a scan or less efficient index usage.
- **Recommendation**: Create index on `(team_id, entity_type, entity_id)` for the common query pattern.
- **Effort**: Small

### Finding 13.5 [SEVERITY: Medium]
- **Location**: Contracts, payments, expenses migrations
- **Issue**: Inconsistent index strategy — `properties`, `documents`, `tenants`, and `reports` tables use the superior partial index pattern (`WHERE deleted_at IS NULL`), while `contracts`, `payments`, `expenses`, and `payment_receivals` use separate standalone indexes on `team_id` and `deleted_at`. The latter tables would benefit from partial indexes to reduce index size and improve query performance.
- **Recommendation**: Standardize on partial indexes `ON table(team_id) WHERE deleted_at IS NULL` for all soft-deletable tables.
- **Effort**: Small

### Finding 13.6 [SEVERITY: Low]
- **Location**: Multiple migrations
- **Issue**: Inconsistent timestamp defaults — some use `NOW()`, others use `CURRENT_TIMESTAMP`. Both are equivalent in PostgreSQL but inconsistent style.
- **Recommendation**: Standardize on one approach.
- **Effort**: Small

### Finding 13.7 [SEVERITY: Low]
- **Location**: `V009__create_tenants.sql:36,40`
- **Issue**: Two indexes on tenant email — one regular and one unique partial. The regular index is redundant.
- **Recommendation**: Remove the non-unique index.
- **Effort**: Small

---

## Category 14: TESTING

### Finding 14.1 [SEVERITY: High]
- **Location**: Entire codebase
- **Issue**: Zero test coverage. No unit tests, no integration tests, no API tests exist in the repository. The project has testing dependencies (JUnit 5, Mockito, Testcontainers, Spring Security Test) in `pom.xml` but no actual test files.
- **Recommendation**: Prioritize tests for: (1) multi-tenancy isolation, (2) financial calculations (payment receivals, balances), (3) contract status transitions, (4) authorization rules.
- **Effort**: Large

---

# Improvement Roadmap

## Phase 1: Security & Data Integrity (MUST DO)
- [ ] Fix TenantAddressRepository UPDATE to include team_id (Finding 1.1)
- [ ] Fix TeamRepository.save() CREATED_BY → UPDATED_BY bug (Finding 2.1)
- [ ] Fix GlobalExceptionHandler to not leak exception messages (Finding 4.2)
- [ ] Move credentials to environment variables, create env-specific profiles (Finding 4.3)
- [ ] Add @PreAuthorize to DocumentController (Finding 4.1)
- [ ] Replace hard deletes with soft deletes in 4 repositories (Finding 2.2)
- [ ] Fix CORS — remove duplicate config, specify headers explicitly (Finding 4.4)
- [ ] Add magic byte MIME validation for file uploads (Finding 4.5)
- [ ] Remove `findById()` without team_id from TeamMemberRepository, TeamInvitationRepository (Finding 1.2)
- [ ] Fix TenantController address endpoints to validate tenantId ownership (Finding 1.3)
- [ ] Add TeamController guard to verify principal matches URL teamId (Finding 1.4)
- [ ] Add size limit and streaming to DocumentController.bulkDownload (Finding 4.6)

## Phase 2: Reliability & Correctness
- [ ] Add `nullValuePropertyMappingStrategy = IGNORE` to all update mappers (Finding 11.2)
- [ ] Add @Transactional to PropertyService write methods (Finding 3.1)
- [ ] Fix PaymentSchedulingService transaction scope (Finding 3.2)
- [ ] Add compensation/rollback for AuthService registration (Finding 2.3)
- [ ] Add proper exception classes (EntityNotFoundException, etc.) (Finding 8.1)
- [ ] Add @NotNull to CreatePaymentRequest.currency (Finding 10.1)
- [ ] Add cross-field date validation to all DTOs with startDate/endDate (Finding 10.5)
- [ ] Add @PositiveOrZero to 7 unvalidated financial BigDecimal fields (Finding 10.7)
- [ ] Fix TenantAddressMapper missing explicit @Mapping ignores (Finding 11.3)
- [ ] Fix hardcoded localhost in TeamMapper invitation URL (Finding 5.2)
- [ ] Remove fileKey from DocumentResponse (Finding 5.3)
- [ ] Implement email failure retry mechanism (Finding 8.2)

## Phase 3: Performance & Scalability
- [ ] Add pagination to all list endpoints (Finding 7.1)
- [ ] Fix N+1 queries in PaymentService.enrichPaymentResponse (Finding 6.1)
- [ ] Fix N+1 queries in TeamService.getTeamMembers (Finding 6.2)
- [ ] Fix N+1 queries in ReportService (Finding 6.3)
- [ ] Fix N+1 queries in remaining services (Finding 6.4)
- [ ] Add identifier column to documents table (Finding 13.1)
- [ ] Add `deleted_at` to teams, users, team_members tables (Finding 13.3)
- [ ] Add team_id to audit_log entity index (Finding 13.4)
- [ ] Standardize on partial indexes for soft-deletable tables (Finding 13.5)

## Phase 4: Code Quality & Maintainability
- [ ] Remove UUID exposure from all response DTOs — expose identifier only (Finding 5.1)
- [ ] Change request DTOs to use identifier instead of UUID (Finding 10.4)
- [ ] Add equals()/hashCode() to all domain entities (Finding 2.4)
- [ ] Extract BaseRepository to reduce code duplication (Finding 9.2)
- [ ] Add @Size constraints to ~40+ unbounded string fields in request DTOs (Finding 10.6)
- [ ] Replace manual change detection with MapStruct (Finding 9.1)
- [ ] Move ExportService HTML to Thymeleaf templates (Finding 9.3)
- [ ] Update Keycloak and AWS SDK dependencies (Finding 12.2)
- [ ] Write unit and integration tests — target 80% coverage (Finding 14.1)
- [ ] Migrate PaymentReceivalRepository to JOOQ generated classes (Finding 9.4)

---

# Appendix

## A. Positive Patterns (PRESERVE THESE)

- **ULID identifiers**: Consistent use of ULID for external-facing identifiers across all entities. Well-implemented `UlidGenerator.java` utility. *(Keep this pattern — just enforce it in DTOs too)*
- **Soft deletes**: All main tables have `deleted_at` column with proper filtering in queries. Preserves audit trail. *(Keep — fix the 4 hard-delete violations)*
- **Audit logging**: Comprehensive `AuditService` that records CREATE, UPDATE, DELETE operations with old/new values. Good `buildActivityDescription()` for human-readable activity feeds.
- **Multi-tenancy isolation**: `team_id` present on every table with proper foreign key constraints. Repository queries consistently filter by team_id. *(Keep — add the base repository to make it foolproof)*
- **MapStruct mappers**: Clean separation of JOOQ Record → Domain → DTO mapping layers. MapStruct provides compile-time verification of mapping completeness.
- **JOOQ type safety**: Generated JOOQ classes provide compile-time SQL correctness. Strong typing for columns, tables, and conditions.
- **Database constraints**: Excellent CHECK constraints on financial amounts (> 0), date ranges (end >= start), enum values, and business rules (payment_due_day 1-31).
- **JOOQ + Flyway + PostgreSQL**: Solid data layer stack. JOOQ codegen from Flyway migrations ensures schema and Java code stay in sync.
- **Role hierarchy**: Proper Spring Security role hierarchy (TEAM_ADMIN > TEAM_EDITOR > TEAM_VIEWER) with AOP-based `@RequiresTeamRole` annotation.
- **Custom exceptions**: Well-designed domain exceptions (CrossTeamAccessException, InsufficientPermissionsException, etc.) with proper HTTP status mapping.
- **Request DTO validation**: Good use of Jakarta validation annotations (@NotBlank, @NotNull, @Min, @Positive) on most request DTOs with descriptive messages.
- **Stateless JWT auth**: Proper `SessionCreationPolicy.STATELESS` with Keycloak JWT validation. No session cookies to manage.

## B. Files Reviewed

### Repositories (16)
PropertyRepository, PropertyTenantHistoryRepository, TenantRepository, TenantAddressRepository, ExpenseRepository, DocumentRepository, TeamRepository, TeamMemberRepository, UserRepository, UserPreferencesRepository, UserTeamNotificationPreferencesRepository, TeamInvitationRepository, PaymentRepository, AuditLogRepository, ContractRepository, PaymentReceivalRepository

### Services (22)
AuthService, PropertyService, TenantService, ContractService, PaymentService, PaymentSchedulingService, ExpenseService, DocumentService, TeamService, AuditService, DashboardService, ReportService, ExportService, KeycloakService, S3StorageService, EmailService, TeamPermissionService, UserPreferencesService, UserTeamService, TenantAddressService, NotificationSchedulerService, S3BucketInitializer

### Controllers (15)
AuthController, InvitationController, HealthController, DashboardController, ReportController, AuditController, UserPreferencesController, UserController, PropertyController, TeamController, ExpenseController, DocumentController, TenantController, ContractController, PaymentController

### Security (6)
JwtAuthenticationConverter, UserPrincipal, UserAuthentication, MdcFilter, TeamMembershipAspect, RequiresTeamRole

### Config (8)
SecurityConfig, WebConfig, SwaggerConfig, KeycloakConfig, JooqConfig, JacksonConfig, S3Config, AsyncConfig

### Exceptions (6)
GlobalExceptionHandler, CrossTeamAccessException, InsufficientPermissionsException, TeamNotFoundException, TeamMembershipNotFoundException, CannotLeaveOwnedTeamException

### Domain Entities (18)
Team, User, TeamMember, Property, Tenant, TenantAddress, Contract, Payment, PaymentReceival, Expense, Document, AuditLog, PropertyTenantHistory, UserPreferences, UserTeamNotificationPreferences, TeamInvitation, TeamSettings, GeneratedReport

### Request DTOs (33)
CreatePropertyRequest, UpdatePropertyRequest, CreateTenantRequest, UpdateTenantRequest, CreateTenantAddressRequest, UpdateTenantAddressRequest, LinkTenantToPropertyRequest, CreateContractRequest, UpdateContractRequest, ChangeContractStatusRequest, CreatePaymentRequest, UpdatePaymentRequest, MarkPaidRequest, BulkGeneratePaymentsRequest, CreateExpenseRequest, UpdateExpenseRequest, FinancialOverviewRequest, TransactionHistoryRequest, GenerateReportRequest, GeneratePaymentsRequest, RegisterRequest, UpdateProfileRequest, UploadDocumentRequest, CreateInvitationRequest, UpdateMemberRoleRequest, SwitchTeamRequest, SetDefaultTeamRequest, UpdateUserPreferencesRequest, UpdateTeamNotificationPreferencesRequest, TransferOwnershipRequest, UpdateTeamRequest, UpdateTeamSettingsRequest, UpdateUserProfileRequest, CreatePaymentReceivalRequest, UpdatePaymentReceivalRequest

### Response DTOs (35)
HealthResponse, InfoResponse, UserResponse, TeamResponse, DocumentResponse, PropertyResponse, RecentActivityResponse, PropertyTenantHistoryResponse, PropertySummary, DashboardStatsResponse, TenantResponse, TenantSummary, TenantAddressResponse, ContractResponse, ContractSummary, PaymentSummary, ExpenseResponse, ExpenseSummaryResponse, FinancialOverviewResponse, PropertyFinancialSummary, CategoryExpenseSummary, IncomeTrendResponse, ExpenseBreakdownResponse, PropertyComparisonResponse, OccupancyTrendResponse, ReportStatusResponse, TaxSummaryResponse, TransactionResponse, UserTeamResponse, UserPreferencesResponse, UserTeamNotificationPreferencesResponse, TeamMemberResponse, UserProfileResponse, InvitationResponse, PaymentReceivalResponse, PaymentResponse

### Mappers (20)
UserMapper, DocumentMapper, DocumentRecordMapper, PropertyRecordMapper, TeamInvitationRecordMapper, UserRecordMapper, TenantRecordMapper, TenantMapper, TenantAddressMapper, ContractRecordMapper, ContractMapper, PropertyMapper, ExpenseMapper, ExpenseRecordMapper, PaymentRecordMapper, TeamRecordMapper, TeamMemberRecordMapper, TeamMapper, PaymentReceivalMapper, PaymentMapper

### Migrations (25)
V001 through V025

### Configuration
application.yml, pom.xml

## C. Files Skipped
- JOOQ generated classes in `target/generated-sources/jooq/`
- Frontend code in `frontend/`
- Docker configuration
- Test files (none exist)
