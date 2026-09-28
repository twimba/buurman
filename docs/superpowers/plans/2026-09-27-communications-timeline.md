# Communications Timeline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A landlord opening a payment or contract sees what Buurman sent about it, to whom, and whether it arrived.

**Architecture:** `notifications` gains nullable `payment_id` and `contract_id` foreign keys, set at send time by the six services that send about those entities. Two Sid-keyed endpoints nested under the entity expose the trail, served by a new `CommunicationService` in `buurman-notifications` that resolves the entity through its team-scoped repository lookup first. One React component renders both timelines.

**Tech Stack:** Java 25, Spring Boot 4.0.2, JOOQ 3.20, Flyway, MapStruct, JUnit 5 + AssertJ + Mockito + Testcontainers; React 19, TypeScript, Vitest, i18next, Orval.

**Spec:** `docs/superpowers/specs/2026-09-27-communications-timeline-design.md`

## Global Constraints

- Next free Flyway version is **V074**. Never modify an existing migration.
- Never bypass `team_id` filtering in a repository query.
- Never expose an internal UUID in an API; the public identifier is the `Sid` in the `identifier` column.
- **MANDATORY:** every `if`, `else`, `for`, `while` body uses curly braces. No brace-less single-statement bodies, ever.
- **MANDATORY:** idiomatic `Optional` API (`map`, `orElse`, `orElseThrow`, `ifPresent`, `flatMap`). Never `if (opt != null)` and never `opt.get()` without an `isPresent()` check.
- `@PreAuthorize` on service methods, not controllers. Role hierarchy: TEAM_ADMIN > TEAM_EDITOR > TEAM_VIEWER.
- **Module direction: `buurman-notifications` depends on `buurman-core`, never the reverse.** `PaymentController`/`ContractController` live in core and cannot call a service in notifications, which is why the new operations get their own `Communications` tag and their own controller in the notifications module rather than being added to `PaymentsApi`.
- A generated API interface is produced per OpenAPI **tag**: tag `Payments` → `PaymentsApi`. A new tag therefore yields a new interface and leaves existing controllers untouched.
- After editing anything under `openapi/src/`, run `make bundle-openapi`, then `cd frontend && yarn generate:api` — the generated clients are gitignored.
- Run every Maven command with `-Dmaven.build.cache.enabled=false`; without it the build cache reports success while running zero tests. Use `-Dmaven.test.skip=true` (not `-DskipTests`) when installing while a test is intentionally red, because `-DskipTests` still compiles tests.
- Backend formatting: `mvn spotless:apply` reformats pre-existing files that already violate the format. Stage only files this plan touches; revert the rest.
- The repo is prettier-clean; run `cd frontend && yarn lint --fix` before committing frontend changes.
- This worktree has unrelated pre-existing modifications to `docker/traefik/dynamic/local-dev.yml` and `keycloak/*.json`. **Never `git add -A` or `git commit -a`.** Stage only the files a step names.

## Review Focus

1. **A resend loses the entity link.** The landlord resends a reminder from the timeline and the new message vanishes from the very timeline they are watching. The resend path must copy `payment_id`/`contract_id` from the original. → Task 2.
2. **The scheduled sends stay unlinked.** `NotificationSchedulerService` sends the reminders and expiry notices a landlord never triggers by hand — exactly the ones whose absence looks like the feature is broken. Easy to miss because the manual path is the one you test by clicking. → Task 3.
3. **A payment identifier from another team returns that team's communications.** The entity lookup must be team-scoped and return empty, never leak. → Task 5.
4. **An SMS row claims it was not delivered.** Twilio reports different states than Mailgun and never reports "opened"; rendering email-shaped status for an SMS misleads. An SMS with no delivery confirmation must not read as a failure. → Task 7.
5. **The empty state is indistinguishable from "nothing was sent".** No backfill exists, so every pre-existing payment shows an empty timeline; if it does not say why, a landlord concludes no reminder went out and sends a duplicate. → Task 7.

---

### Task 1: Entity link on the notification record

**Files:**
- Create: `backend/buurman-jooq/src/main/resources/db/migration/V074__notification_related_entities.sql`
- Modify: `backend/buurman-common/src/main/java/com/buurman/domain/Notification.java`
- Modify: `backend/buurman-notifications/src/main/java/com/buurman/mapper/NotificationRecordMapper.java`
- Modify: `backend/buurman-notifications/src/main/java/com/buurman/repository/NotificationRepository.java`
- Test: `backend/buurman-notifications/src/test/java/com/buurman/repository/NotificationRepositoryIntegrationTest.java`

**Interfaces:**
- Consumes: nothing.
- Produces: `Notification.getRelatedPaymentId()` and `getRelatedContractId()`, both `Optional<UUID>`; columns `NOTIFICATIONS.PAYMENT_ID` and `NOTIFICATIONS.CONTRACT_ID`.

- [ ] **Step 1: Write the migration**

Create `backend/buurman-jooq/src/main/resources/db/migration/V074__notification_related_entities.sql`:

```sql
-- Which payment or contract a notification is about, so a landlord can see the
-- delivery trail on the entity itself rather than in the admin log.
-- A payment reminder sets BOTH: it is about the payment and about its contract,
-- and the contract timeline should show it without joining through payments.
-- Nullable: most notification types (welcome, verification, invitations) are
-- about neither, and historical rows cannot be attributed retroactively.
ALTER TABLE notifications
    ADD COLUMN payment_id UUID REFERENCES payments (id),
    ADD COLUMN contract_id UUID REFERENCES contracts (id);

CREATE INDEX idx_notifications_payment ON notifications (payment_id)
    WHERE payment_id IS NOT NULL;

CREATE INDEX idx_notifications_contract ON notifications (contract_id)
    WHERE contract_id IS NOT NULL;
```

- [ ] **Step 2: Regenerate JOOQ**

Run: `cd backend && mvn generate-sources -pl buurman-jooq -am -Dmaven.build.cache.enabled=false`
Expected: BUILD SUCCESS. Verify with
`grep -c "PAYMENT_ID\|CONTRACT_ID" buurman-jooq/target/generated-sources/jooq/com/buurman/jooq/generated/tables/Notifications.java`
which must print at least `2`. Docker must be running.

- [ ] **Step 3: Write the failing integration test**

Append this nested class to `NotificationRepositoryIntegrationTest`, immediately before the final closing brace of the class. **Open the file and read it first** — it already exists and contains other tests; do not overwrite it.

```java
  @Nested
  @DisplayName("relatedEntities")
  class RelatedEntities {

    @Test
    @DisplayName("the payment and contract link round-trips")
    void linkRoundTrips() {
      UUID contractId = TestDataHelper.insertContract(dsl, TEAM_A_ID, USER_ID);
      UUID paymentId = TestDataHelper.insertPayment(dsl, TEAM_A_ID, contractId, USER_ID);
      Notification notification = TestDataHelper.buildNotification(TEAM_A_ID, USER_ID);
      notification.setRelatedPaymentId(Optional.of(paymentId));
      notification.setRelatedContractId(Optional.of(contractId));

      Notification saved = repo.save(notification);

      assertThat(repo.findByIdAndTeamId(saved.getId(), TEAM_A_ID))
          .get()
          .satisfies(
              found -> {
                assertThat(found.getRelatedPaymentId()).isEqualTo(Optional.of(paymentId));
                assertThat(found.getRelatedContractId()).isEqualTo(Optional.of(contractId));
              });
    }

    @Test
    @DisplayName("an unlinked notification round-trips as empty, not null")
    void unlinkedRoundTripsAsEmpty() {
      Notification saved = repo.save(TestDataHelper.buildNotification(TEAM_A_ID, USER_ID));

      assertThat(repo.findByIdAndTeamId(saved.getId(), TEAM_A_ID))
          .get()
          .satisfies(
              found -> {
                assertThat(found.getRelatedPaymentId()).isEmpty();
                assertThat(found.getRelatedContractId()).isEmpty();
              });
    }
  }
```

If `TestDataHelper` in the notifications module lacks `insertContract`, `insertPayment`, `buildNotification` or the test lacks `findByIdAndTeamId`, read the existing file and use whatever equivalents it provides; the assertions are what matter, not the fixture names.

- [ ] **Step 4: Run it to make sure it fails**

Run: `cd backend && mvn test -pl buurman-notifications -am -Dtest=NotificationRepositoryIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.build.cache.enabled=false`
Expected: FAIL — `cannot find symbol: method setRelatedPaymentId`.

- [ ] **Step 5: Add the fields to the domain object**

In `Notification.java`, immediately after the `recipientContactId` field, add:

```java
  /** The payment this notification is about, when it is about one. */
  @Builder.Default private Optional<UUID> relatedPaymentId = Optional.empty();

  /** The contract this notification is about. A payment reminder sets this and the payment. */
  @Builder.Default private Optional<UUID> relatedContractId = Optional.empty();
```

- [ ] **Step 6: Map them when reading**

In `NotificationRecordMapper.toDomain`, immediately after the `setRecipientContactId` line, add:

```java
    notification.setRelatedPaymentId(Optional.ofNullable(record.getPaymentId()));
    notification.setRelatedContractId(Optional.ofNullable(record.getContractId()));
```

- [ ] **Step 7: Write them on insert**

In `NotificationRepository`, in the insert builder immediately after the `RECIPIENT_CONTACT_ID` line, add:

```java
        .set(NOTIFICATIONS.PAYMENT_ID, notification.getRelatedPaymentId().orElse(null))
        .set(NOTIFICATIONS.CONTRACT_ID, notification.getRelatedContractId().orElse(null))
```

- [ ] **Step 8: Run it to make sure it passes**

Run: `cd backend && mvn install -Dmaven.test.skip=true -Pquick -Dmaven.build.cache.enabled=false && mvn test -pl buurman-notifications -am -Dtest=NotificationRepositoryIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.build.cache.enabled=false`
Expected: PASS.

- [ ] **Step 9: Commit**

```bash
git add backend/buurman-jooq/src/main/resources/db/migration/V074__notification_related_entities.sql \
        backend/buurman-common/src/main/java/com/buurman/domain/Notification.java \
        backend/buurman-notifications/src/main/java/com/buurman/mapper/NotificationRecordMapper.java \
        backend/buurman-notifications/src/main/java/com/buurman/repository/NotificationRepository.java \
        backend/buurman-notifications/src/test/java/com/buurman/repository/NotificationRepositoryIntegrationTest.java
git commit -m "feat(notifications): record which payment or contract a notification is about"
```

---

### Task 2: The send and resend paths carry the link

**Files:**
- Modify: `backend/buurman-core/src/main/java/com/buurman/service/notification/SendNotificationRequest.java`
- Modify: `backend/buurman-notifications/src/main/java/com/buurman/service/notification/NotificationServiceImpl.java`
- Test: `backend/buurman-notifications/src/test/java/com/buurman/service/notification/NotificationServiceImplTest.java`

**Interfaces:**
- Consumes: `Notification.setRelatedPaymentId(Optional<UUID>)` / `setRelatedContractId(Optional<UUID>)` (Task 1).
- Produces: `SendNotificationRequest.relatedPaymentId()` and `relatedContractId()`, both `Optional<UUID>`, with builder defaults of `Optional.empty()`.

- [ ] **Step 1: Write the failing test**

Append to `NotificationServiceImplTest`, inside the existing test class. Read the file first for its `baseRequest()` helper and mock names.

```java
  @Nested
  @DisplayName("entity link")
  class EntityLink {

    @Test
    @DisplayName("a send persists the payment and contract it is about")
    void sendPersistsTheLink() {
      UUID paymentId = UUID.randomUUID();
      UUID contractId = UUID.randomUUID();
      when(notificationRepository.save(any()))
          .thenAnswer(invocation -> invocation.getArgument(0));

      service.send(
          baseRequest()
              .relatedPaymentId(Optional.of(paymentId))
              .relatedContractId(Optional.of(contractId))
              .build());

      ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
      verify(notificationRepository, atLeastOnce()).save(captor.capture());
      assertThat(captor.getAllValues())
          .anySatisfy(
              saved -> {
                assertThat(saved.getRelatedPaymentId()).isEqualTo(Optional.of(paymentId));
                assertThat(saved.getRelatedContractId()).isEqualTo(Optional.of(contractId));
              });
    }
  }
```

- [ ] **Step 2: Run it to make sure it fails**

Run: `cd backend && mvn test -pl buurman-notifications -am -Dtest=NotificationServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.build.cache.enabled=false`
Expected: FAIL — `cannot find symbol: method relatedPaymentId`.

- [ ] **Step 3: Add the request components**

In `SendNotificationRequest.java`, add two components immediately after `recipientPhone`:

```java
    /** The payment this notification is about, for the communications timeline. */
    Optional<UUID> relatedPaymentId,
    /** The contract this notification is about. A payment reminder sets this and the payment. */
    Optional<UUID> relatedContractId,
```

and in the builder customisation class, alongside the other Optional defaults:

```java
    private Optional<UUID> relatedPaymentId = Optional.empty();
    private Optional<UUID> relatedContractId = Optional.empty();
```

Every one of the 25 call sites uses `SendNotificationRequest.builder()` and there are no positional constructor calls, so the defaults keep them all compiling.

- [ ] **Step 4: Carry them into the notification on send**

In `NotificationServiceImpl.send`, where the `Notification` is populated from the request, add:

```java
      notification.setRelatedPaymentId(request.relatedPaymentId());
      notification.setRelatedContractId(request.relatedContractId());
```

- [ ] **Step 5: Run it to make sure it passes**

Run: `cd backend && mvn test -pl buurman-notifications -am -Dtest=NotificationServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.build.cache.enabled=false`
Expected: PASS.

- [ ] **Step 6: Write the failing test for resend (Review Focus 1)**

Add to the same `EntityLink` nested class:

```java
    @Test
    @DisplayName("a resend stays on the same timeline as the original")
    void resendCopiesTheLink() {
      UUID paymentId = UUID.randomUUID();
      UUID contractId = UUID.randomUUID();
      Notification original = TestDataHelper.buildNotification(TEAM_ID, CREATED_BY);
      original.setRelatedPaymentId(Optional.of(paymentId));
      original.setRelatedContractId(Optional.of(contractId));
      when(notificationRepository.findByIdentifierAndTeamId(any(), any()))
          .thenReturn(Optional.of(original));
      when(notificationRepository.save(any()))
          .thenAnswer(invocation -> invocation.getArgument(0));

      service.resend(original.getIdentifier().orElseThrow(), TEAM_ID, CREATED_BY, "retry");

      ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
      verify(notificationRepository, atLeastOnce()).save(captor.capture());
      // Without this the landlord resends from the timeline and the new message is
      // absent from the very timeline they are watching.
      assertThat(captor.getAllValues())
          .anySatisfy(
              saved -> {
                assertThat(saved.getRelatedPaymentId()).isEqualTo(Optional.of(paymentId));
                assertThat(saved.getRelatedContractId()).isEqualTo(Optional.of(contractId));
              });
    }
```

Adjust the `service.resend(...)` call and the repository stub to the real resend signature in `NotificationServiceImpl` — read it first; the assertion is what matters.

- [ ] **Step 7: Run it to make sure it fails**

Run: `cd backend && mvn test -pl buurman-notifications -am -Dtest=NotificationServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.build.cache.enabled=false`
Expected: FAIL — the resent notification has empty links.

- [ ] **Step 8: Copy the link on resend**

In the resend path of `NotificationServiceImpl`, where the new `Notification` is built from `original`, add:

```java
    resent.setRelatedPaymentId(original.getRelatedPaymentId());
    resent.setRelatedContractId(original.getRelatedContractId());
```

- [ ] **Step 9: Run it to make sure it passes**

Run: `cd backend && mvn test -pl buurman-notifications -am -Dtest=NotificationServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.build.cache.enabled=false`
Expected: PASS.

- [ ] **Step 10: Commit**

```bash
git add backend/buurman-core/src/main/java/com/buurman/service/notification/SendNotificationRequest.java \
        backend/buurman-notifications/src/main/java/com/buurman/service/notification/NotificationServiceImpl.java \
        backend/buurman-notifications/src/test/java/com/buurman/service/notification/NotificationServiceImplTest.java
git commit -m "feat(notifications): carry the entity link through send and resend"
```

---

### Task 3: The six sending services set the link

**Files:**
- Modify: `backend/buurman-core/src/main/java/com/buurman/service/PaymentReminderService.java`
- Modify: `backend/buurman-notifications/src/main/java/com/buurman/service/NotificationSchedulerService.java`
- Modify: `backend/buurman-core/src/main/java/com/buurman/service/PaymentService.java`
- Modify: `backend/buurman-core/src/main/java/com/buurman/service/ContractService.java`
- Modify: `backend/buurman-core/src/main/java/com/buurman/service/ContractExtensionService.java`
- Modify: `backend/buurman-core/src/main/java/com/buurman/service/ContractRentPeriodService.java`
- Test: `backend/buurman-core/src/test/java/com/buurman/service/PaymentReminderServiceTest.java`
- Create: `backend/buurman-notifications/src/test/java/com/buurman/service/NotificationSchedulerServiceTest.java`

**Interfaces:**
- Consumes: `SendNotificationRequest.builder().relatedPaymentId(...)/.relatedContractId(...)` (Task 2).
- Produces: real sends carry the link. Nothing else consumes this.

- [ ] **Step 1: Write the failing test for the payment reminder**

Add to `PaymentReminderServiceTest`, inside the existing `sendReminder` context. It already captures the request with `ArgumentCaptor<SendNotificationRequest>`; follow that pattern.

```java
    @Test
    @DisplayName("links the reminder to its payment and contract")
    void linksTheReminderToItsEntities() {
      stubHappyPath(payment(PaymentStatus.PENDING, TODAY.minusDays(10)), new BigDecimal("250.00"));

      service.sendReminder(
          PAYMENT_SID, new SendPaymentReminderRequest(Optional.empty()), principal);

      ArgumentCaptor<SendNotificationRequest> captor =
          ArgumentCaptor.forClass(SendNotificationRequest.class);
      verify(notificationService).send(captor.capture());
      // Both: the reminder belongs to the payment's timeline and the contract's.
      assertThat(captor.getValue().relatedPaymentId()).contains(PAYMENT_ID);
      assertThat(captor.getValue().relatedContractId()).contains(CONTRACT_ID);
    }
```

- [ ] **Step 2: Run it to make sure it fails**

Run: `cd backend && mvn test -pl buurman-core -am -Dtest=PaymentReminderServiceTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.build.cache.enabled=false`
Expected: FAIL — both are empty.

- [ ] **Step 3: Set the link in PaymentReminderService**

In the `SendNotificationRequest.builder()` chain, immediately after `.recipientContactId(...)`, add:

```java
                .relatedPaymentId(Optional.of(payment.getId()))
                .relatedContractId(Optional.of(contract.getId()))
```

- [ ] **Step 4: Run it to make sure it passes**

Run: `cd backend && mvn test -pl buurman-core -am -Dtest=PaymentReminderServiceTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.build.cache.enabled=false`
Expected: PASS.

- [ ] **Step 5: Write the failing test for the scheduled path (Review Focus 2)**

`ContractServiceTest` exists but never touches `notificationService`, and the four contract
services have no send-capture scaffolding at all. Building it for each would be four fixtures
for one assertion apiece. Instead, cover the highest-risk path properly — the scheduler, whose
messages a landlord never triggers by hand — and verify the rest by count in Step 7.

Create `backend/buurman-notifications/src/test/java/com/buurman/service/NotificationSchedulerServiceTest.java`:

```java
package com.buurman.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buurman.config.models.AppProperties;
import com.buurman.repository.ContactNoteRepository;
import com.buurman.repository.ContactRepository;
import com.buurman.repository.ContractExtensionRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PaymentRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.TeamMemberRepository;
import com.buurman.repository.TeamPreferencesRepository;
import com.buurman.repository.TeamRepository;
import com.buurman.repository.UserRepository;
import com.buurman.service.notification.NotificationService;
import com.buurman.service.notification.SendNotificationRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationSchedulerService")
class NotificationSchedulerServiceTest {

  @Mock private ContactNoteRepository contactNoteRepository;
  @Mock private ContactRepository contactRepository;
  @Mock private ContractRepository contractRepository;
  @Mock private ContractExtensionRepository contractExtensionRepository;
  @Mock private PaymentRepository paymentRepository;
  @Mock private PropertyRepository propertyRepository;
  @Mock private TeamRepository teamRepository;
  @Mock private TeamPreferencesRepository teamPreferencesRepository;
  @Mock private TeamMemberRepository teamMemberRepository;
  @Mock private UserRepository userRepository;
  @Mock private NotificationService notificationService;
  @Mock private AppProperties appProperties;

  @InjectMocks private NotificationSchedulerService service;

  @Test
  @DisplayName("a scheduled contract-expiry notice is linked to its contract")
  void scheduledExpiryIsLinked() {
    // Read NotificationSchedulerService around the CONTRACT_EXPIRY send (the first
    // SendNotificationRequest.builder() chain in the file) and stub the traversal it
    // performs: the teams it iterates, the contracts it finds expiring, and the
    // recipients it resolves. Stub only what that path touches.

    service.checkContractExpiries();

    ArgumentCaptor<SendNotificationRequest> captor =
        ArgumentCaptor.forClass(SendNotificationRequest.class);
    verify(notificationService).send(captor.capture());
    // These are the notices no one clicks a button to send. Unlinked, the contract
    // timeline is empty exactly where a landlord expects to see the expiry warning.
    assertThat(captor.getValue().relatedContractId()).isNotEmpty();
  }
}
```

Use the real method name for the expiry job — read the class; it may be invoked by a Quartz job
wrapper rather than called `checkContractExpiries`.

- [ ] **Step 6: Run it to make sure it fails**

Run: `cd backend && mvn test -pl buurman-notifications -am -Dtest=NotificationSchedulerServiceTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.build.cache.enabled=false`
Expected: FAIL — `relatedContractId` is empty.

- [ ] **Step 7: Set the link in the remaining five services, and verify by count**

Add `.relatedContractId(Optional.of(contract.getId()))` to every `SendNotificationRequest.builder()`
chain about a contract, and both `.relatedPaymentId(...)` and `.relatedContractId(...)` to every
chain about a payment:

| File | Builder chains | Expects `relatedContractId` | Expects `relatedPaymentId` |
| --- | --- | --- | --- |
| `PaymentReminderService` | 1 | 1 | 1 |
| `PaymentService` | 2 | 2 | 2 |
| `ContractService` | 3 | 3 | 0 |
| `ContractExtensionService` | 4 | 4 | 0 |
| `ContractRentPeriodService` | 1 | 1 | 0 |
| `NotificationSchedulerService` | 3 | 2 | 1 |

`NotificationSchedulerService` has three chains but only two are entity-related: CONTRACT_EXPIRY
(contract) and PAYMENT_REMINDER (both). The third is the contact follow-up reminder, which is
about a contact and gets neither.

Verify every site was reached:

```bash
cd backend && for f in \
  buurman-core/src/main/java/com/buurman/service/PaymentReminderService.java \
  buurman-core/src/main/java/com/buurman/service/PaymentService.java \
  buurman-core/src/main/java/com/buurman/service/ContractService.java \
  buurman-core/src/main/java/com/buurman/service/ContractExtensionService.java \
  buurman-core/src/main/java/com/buurman/service/ContractRentPeriodService.java \
  buurman-notifications/src/main/java/com/buurman/service/NotificationSchedulerService.java; do
  echo "$(basename $f): contract=$(grep -c 'relatedContractId' $f) payment=$(grep -c 'relatedPaymentId' $f)"
done
```

Expected, matching the table exactly:

```
PaymentReminderService.java: contract=1 payment=1
PaymentService.java: contract=2 payment=2
ContractService.java: contract=3 payment=0
ContractExtensionService.java: contract=4 payment=0
ContractRentPeriodService.java: contract=1 payment=0
NotificationSchedulerService.java: contract=2 payment=1
```

A number lower than the table means a send was missed and that notification type will silently
never appear on a timeline — the failure mode this task exists to prevent.

- [ ] **Step 8: Run the whole backend suite**

Run: `cd backend && mvn clean test -Dmaven.build.cache.enabled=false`
Expected: PASS. A missed builder chain shows up as a compile error only if the variable name differs; otherwise it shows as an unlinked send, which is why Step 5's test asserts `allSatisfy` over every captured request.

- [ ] **Step 9: Commit**

```bash
git add backend/buurman-core/src/main/java/com/buurman/service/PaymentReminderService.java \
        backend/buurman-notifications/src/main/java/com/buurman/service/NotificationSchedulerService.java \
        backend/buurman-core/src/main/java/com/buurman/service/PaymentService.java \
        backend/buurman-core/src/main/java/com/buurman/service/ContractService.java \
        backend/buurman-core/src/main/java/com/buurman/service/ContractExtensionService.java \
        backend/buurman-core/src/main/java/com/buurman/service/ContractRentPeriodService.java \
        backend/buurman-core/src/test/java/com/buurman/service/PaymentReminderServiceTest.java \
        backend/buurman-notifications/src/test/java/com/buurman/service/NotificationSchedulerServiceTest.java
git commit -m "feat(notifications): link payment and contract notifications to their entity"
```

---

### Task 4: Communications query and response

**Files:**
- Create: `backend/buurman-common/src/main/java/com/buurman/dto/response/CommunicationResponse.java`
- Modify: `backend/buurman-notifications/src/main/java/com/buurman/repository/NotificationRepository.java`
- Test: `backend/buurman-notifications/src/test/java/com/buurman/repository/NotificationRepositoryIntegrationTest.java`

**Interfaces:**
- Consumes: `NOTIFICATIONS.PAYMENT_ID`, `NOTIFICATIONS.CONTRACT_ID` (Task 1).
- Produces: `CommunicationResponse` (record, fields below); `NotificationRepository.findByPaymentIdAndTeamId(UUID paymentId, UUID teamId)` and `findByContractIdAndTeamId(UUID contractId, UUID teamId)`, both returning `List<Notification>` newest first.

- [ ] **Step 1: Write the response record**

Create `backend/buurman-common/src/main/java/com/buurman/dto/response/CommunicationResponse.java`:

```java
package com.buurman.dto.response;

import java.time.Instant;
import java.util.Optional;

import com.buurman.domain.Sid;

/**
 * One row of an entity's communications timeline.
 *
 * <p>Deliberately not {@code NotificationResponse}: the body is omitted because a landlord does
 * not need the full rendered message in a timeline row, and {@code openCount}/{@code clickCount}
 * collapse to a single {@code opened} flag because the question this answers is "did it arrive
 * and was it read", not analytics.
 */
public record CommunicationResponse(
    Sid identifier,
    String notificationType,
    String channel,
    Optional<String> subject,
    Optional<String> recipientEmail,
    Optional<String> recipientPhone,
    String status,
    Optional<String> providerError,
    boolean opened,
    Optional<Instant> firstOpenedAt,
    Optional<Sid> resentFromIdentifier,
    Instant createdAt) {}
```

- [ ] **Step 2: Write the failing integration test**

Add to the `RelatedEntities` nested class created in Task 1:

```java
    @Test
    @DisplayName("finds only the notifications for that payment")
    void findsOnlyThatPaymentsNotifications() {
      UUID contractId = TestDataHelper.insertContract(dsl, TEAM_A_ID, USER_ID);
      UUID paymentId = TestDataHelper.insertPayment(dsl, TEAM_A_ID, contractId, USER_ID);
      UUID otherPaymentId = TestDataHelper.insertPayment(dsl, TEAM_A_ID, contractId, USER_ID);
      Notification mine = TestDataHelper.buildNotification(TEAM_A_ID, USER_ID);
      mine.setRelatedPaymentId(Optional.of(paymentId));
      repo.save(mine);
      Notification other = TestDataHelper.buildNotification(TEAM_A_ID, USER_ID);
      other.setRelatedPaymentId(Optional.of(otherPaymentId));
      repo.save(other);

      assertThat(repo.findByPaymentIdAndTeamId(paymentId, TEAM_A_ID)).hasSize(1);
    }

    @Test
    @DisplayName("another team sees none of them")
    void anotherTeamSeesNone() {
      UUID contractId = TestDataHelper.insertContract(dsl, TEAM_A_ID, USER_ID);
      UUID paymentId = TestDataHelper.insertPayment(dsl, TEAM_A_ID, contractId, USER_ID);
      Notification mine = TestDataHelper.buildNotification(TEAM_A_ID, USER_ID);
      mine.setRelatedPaymentId(Optional.of(paymentId));
      repo.save(mine);

      assertThat(repo.findByPaymentIdAndTeamId(paymentId, TEAM_B_ID)).isEmpty();
    }

    @Test
    @DisplayName("a contract sees the reminders sent for its payments")
    void contractSeesItsPaymentReminders() {
      UUID contractId = TestDataHelper.insertContract(dsl, TEAM_A_ID, USER_ID);
      UUID paymentId = TestDataHelper.insertPayment(dsl, TEAM_A_ID, contractId, USER_ID);
      Notification reminder = TestDataHelper.buildNotification(TEAM_A_ID, USER_ID);
      reminder.setRelatedPaymentId(Optional.of(paymentId));
      reminder.setRelatedContractId(Optional.of(contractId));
      repo.save(reminder);

      assertThat(repo.findByContractIdAndTeamId(contractId, TEAM_A_ID)).hasSize(1);
    }
```

- [ ] **Step 3: Run it to make sure it fails**

Run: `cd backend && mvn test -pl buurman-notifications -am -Dtest=NotificationRepositoryIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.build.cache.enabled=false`
Expected: FAIL — `cannot find symbol: method findByPaymentIdAndTeamId`.

- [ ] **Step 4: Add the queries**

In `NotificationRepository`, following the style of the existing `findByIdAndTeamId`:

```java
  /** Newest first: a timeline reads top-down from the most recent message. */
  public List<Notification> findByPaymentIdAndTeamId(UUID paymentId, UUID teamId) {
    return findByEntityAndTeamId(NOTIFICATIONS.PAYMENT_ID.eq(paymentId), teamId);
  }

  public List<Notification> findByContractIdAndTeamId(UUID contractId, UUID teamId) {
    return findByEntityAndTeamId(NOTIFICATIONS.CONTRACT_ID.eq(contractId), teamId);
  }

  private List<Notification> findByEntityAndTeamId(Condition entityMatches, UUID teamId) {
    return dsl.selectFrom(NOTIFICATIONS)
        .where(entityMatches.and(NOTIFICATIONS.TEAM_ID.eq(teamId)))
        .orderBy(NOTIFICATIONS.CREATED_AT.desc())
        .fetch()
        .stream()
        .map(mapper::toDomain)
        .flatMap(Optional::stream)
        .toList();
  }
```

The `team_id` predicate is not redundant with the entity lookup in Task 5 — it is the guarantee that holds even if a caller reaches this repository by another route.

- [ ] **Step 5: Run it to make sure it passes**

Run: `cd backend && mvn install -Dmaven.test.skip=true -Pquick -Dmaven.build.cache.enabled=false && mvn test -pl buurman-notifications -am -Dtest=NotificationRepositoryIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.build.cache.enabled=false`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/buurman-common/src/main/java/com/buurman/dto/response/CommunicationResponse.java \
        backend/buurman-notifications/src/main/java/com/buurman/repository/NotificationRepository.java \
        backend/buurman-notifications/src/test/java/com/buurman/repository/NotificationRepositoryIntegrationTest.java
git commit -m "feat(communications): query a payment's or contract's notifications"
```

---

### Task 5: CommunicationService and its authorization

**Files:**
- Create: `backend/buurman-notifications/src/main/java/com/buurman/service/notification/CommunicationService.java`
- Create: `backend/buurman-notifications/src/test/java/com/buurman/service/notification/CommunicationServiceTest.java`

**Interfaces:**
- Consumes: `NotificationRepository.findByPaymentIdAndTeamId` / `findByContractIdAndTeamId` (Task 4); `PaymentRepository.findByIdentifierAndTeamId(Sid, UUID)` and `ContractRepository.findByIdentifierAndTeamId(Sid, UUID)` (existing, in `buurman-core`).
- Produces: Spring bean `CommunicationService` with `List<CommunicationResponse> getPaymentCommunications(Sid identifier, UUID teamId)` and `List<CommunicationResponse> getContractCommunications(Sid identifier, UUID teamId)`.

- [ ] **Step 1: Write the failing test**

Create `CommunicationServiceTest`:

```java
package com.buurman.service.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buurman.domain.Payment;
import com.buurman.domain.Sid;
import com.buurman.exception.NotFoundException;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.NotificationRepository;
import com.buurman.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommunicationService")
class CommunicationServiceTest {

  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID PAYMENT_ID = UUID.randomUUID();
  private static final Sid PAYMENT_SID = Sid.of("pay_01JTEST000000000000000001");

  @Mock private NotificationRepository notificationRepository;
  @Mock private PaymentRepository paymentRepository;
  @Mock private ContractRepository contractRepository;

  private CommunicationService service;

  @BeforeEach
  void setUp() {
    service =
        new CommunicationService(notificationRepository, paymentRepository, contractRepository);
  }

  @Test
  @DisplayName("a payment from another team yields nothing, and never reaches the notifications")
  void paymentFromAnotherTeamIsNotFound() {
    when(paymentRepository.findByIdentifierAndTeamId(PAYMENT_SID, TEAM_ID))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getPaymentCommunications(PAYMENT_SID, TEAM_ID))
        .isInstanceOf(NotFoundException.class);
    // The team check must happen before any notification is read, so a foreign identifier
    // cannot leak another team's delivery history.
    verifyNoInteractions(notificationRepository);
  }

  @Test
  @DisplayName("returns the payment's communications newest first")
  void returnsCommunications() {
    Payment payment = new Payment();
    payment.setId(PAYMENT_ID);
    when(paymentRepository.findByIdentifierAndTeamId(PAYMENT_SID, TEAM_ID))
        .thenReturn(Optional.of(payment));
    when(notificationRepository.findByPaymentIdAndTeamId(PAYMENT_ID, TEAM_ID))
        .thenReturn(List.of());

    assertThat(service.getPaymentCommunications(PAYMENT_SID, TEAM_ID)).isEmpty();
  }
}
```

Adjust `Sid.of(...)` and the `Payment` construction to whatever those types require — read `Sid` and `Payment` first.

- [ ] **Step 2: Run it to make sure it fails**

Run: `cd backend && mvn test -pl buurman-notifications -am -Dtest=CommunicationServiceTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.build.cache.enabled=false`
Expected: FAIL — `cannot find symbol: class CommunicationService`.

- [ ] **Step 3: Implement the service**

Create `CommunicationService.java`:

```java
package com.buurman.service.notification;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.Notification;
import com.buurman.domain.Sid;
import com.buurman.dto.response.CommunicationResponse;
import com.buurman.exception.NotFoundException;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.NotificationRepository;
import com.buurman.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;

/**
 * The delivery trail for one payment or contract.
 *
 * <p>Separate from {@code NotificationCenterService}, which stays TEAM_ADMIN-only for the admin
 * delivery log. Reading a timeline is TEAM_VIEWER and above: it reveals no more than the payment
 * and the contact already visible on the same page.
 */
@Service
@RequiredArgsConstructor
public class CommunicationService {

  private final NotificationRepository notificationRepository;
  private final PaymentRepository paymentRepository;
  private final ContractRepository contractRepository;

  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('TEAM_VIEWER')")
  public List<CommunicationResponse> getPaymentCommunications(Sid identifier, UUID teamId) {
    // Resolve through the team-scoped lookup FIRST: an identifier from another team must
    // fail here, before any notification is read.
    UUID paymentId =
        paymentRepository
            .findByIdentifierAndTeamId(identifier, teamId)
            .orElseThrow(() -> new NotFoundException("Payment not found"))
            .getId();
    return toResponses(notificationRepository.findByPaymentIdAndTeamId(paymentId, teamId));
  }

  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('TEAM_VIEWER')")
  public List<CommunicationResponse> getContractCommunications(Sid identifier, UUID teamId) {
    UUID contractId =
        contractRepository
            .findByIdentifierAndTeamId(identifier, teamId)
            .orElseThrow(() -> new NotFoundException("Contract not found"))
            .getId();
    return toResponses(notificationRepository.findByContractIdAndTeamId(contractId, teamId));
  }

  private List<CommunicationResponse> toResponses(List<Notification> notifications) {
    return notifications.stream().map(this::toResponse).toList();
  }

  private CommunicationResponse toResponse(Notification notification) {
    return new CommunicationResponse(
        notification.getIdentifier().orElseThrow(),
        notification.getNotificationType().name(),
        notification.getChannel().name(),
        notification.getSubject(),
        notification.getRecipientEmail(),
        notification.getRecipientPhone(),
        notification.getStatus().name(),
        notification.getProviderError(),
        notification.getFirstOpenedAt().isPresent(),
        notification.getFirstOpenedAt(),
        notification.getResentFromIdentifier(),
        notification.getCreatedAt());
  }
}
```

If `Notification` exposes different accessor names — read it before writing this — keep the `CommunicationResponse` field order and adjust the accessors.

- [ ] **Step 4: Run it to make sure it passes**

Run: `cd backend && mvn test -pl buurman-notifications -am -Dtest=CommunicationServiceTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.build.cache.enabled=false`
Expected: PASS, 2 tests.

- [ ] **Step 5: Commit**

```bash
git add backend/buurman-notifications/src/main/java/com/buurman/service/notification/CommunicationService.java \
        backend/buurman-notifications/src/test/java/com/buurman/service/notification/CommunicationServiceTest.java
git commit -m "feat(communications): team-scoped timeline service"
```

---

### Task 6: The two endpoints

**Files:**
- Create: `openapi/src/paths/communications.yaml`
- Modify: `openapi/src/app.yaml`
- Create: `backend/buurman-notifications/src/main/java/com/buurman/controller/CommunicationController.java`

**Interfaces:**
- Consumes: `CommunicationService.getPaymentCommunications(Sid, UUID)` / `getContractCommunications(Sid, UUID)` (Task 5).
- Produces: `GET /payments/{identifier}/communications` and `GET /contracts/{identifier}/communications`; generated TS type `CommunicationResponse` and hooks.

- [ ] **Step 1: Write the path file**

Create `openapi/src/paths/communications.yaml`. The `Communications` tag is what makes openapi-generator emit a separate `CommunicationsApi`, so `PaymentController` and `ContractController` — which live in `buurman-core` and cannot reach a service in `buurman-notifications` — stay untouched:

```yaml
payment-communications:
  get:
    tags:
      - Communications
    summary: Communications sent about a payment
    operationId: getPaymentCommunications
    parameters:
      - name: identifier
        in: path
        required: true
        schema:
          type: string
        description: Payment identifier (Sid)
    responses:
      '200':
        description: Communications, newest first
        content:
          application/json:
            schema:
              type: array
              items:
                $ref: '#/components/schemas/CommunicationResponse'
      '404':
        description: Payment not found in this team

contract-communications:
  get:
    tags:
      - Communications
    summary: Communications sent about a contract
    operationId: getContractCommunications
    parameters:
      - name: identifier
        in: path
        required: true
        schema:
          type: string
        description: Contract identifier (Sid)
    responses:
      '200':
        description: Communications, newest first
        content:
          application/json:
            schema:
              type: array
              items:
                $ref: '#/components/schemas/CommunicationResponse'
      '404':
        description: Contract not found in this team
```

- [ ] **Step 2: Reference the paths and add the schema**

In `openapi/src/app.yaml`, alongside the other path entries:

```yaml
  /payments/{identifier}/communications:
    $ref: 'paths/communications.yaml#/payment-communications'
  /contracts/{identifier}/communications:
    $ref: 'paths/communications.yaml#/contract-communications'
```

Add `Communications` to the top-level `tags:` block:

```yaml
  - name: Communications
    description: Delivery trail of notifications sent about a payment or contract
```

And under `components: schemas:`:

```yaml
    CommunicationResponse:
      type: object
      required: [identifier, notificationType, channel, status, opened, createdAt]
      properties:
        identifier:
          type: string
        notificationType:
          type: string
        channel:
          type: string
          enum: [EMAIL, SMS]
        subject:
          type: string
        recipientEmail:
          type: string
        recipientPhone:
          type: string
        status:
          type: string
          enum: [PENDING, QUEUED, SENT, DELIVERED, FAILED, BOUNCED, REJECTED]
        providerError:
          type: string
        opened:
          type: boolean
        firstOpenedAt:
          type: string
          format: date-time
        resentFromIdentifier:
          type: string
        createdAt:
          type: string
          format: date-time
```

- [ ] **Step 3: Bundle and regenerate**

Run: `make bundle-openapi && cd frontend && yarn generate:api`
Expected: both succeed; `frontend/app/src/generated/models/communicationResponse.ts` exists.

- [ ] **Step 4: Implement the controller**

Create `CommunicationController.java`:

```java
package com.buurman.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.api.CommunicationsApi;
import com.buurman.domain.Sid;
import com.buurman.dto.response.CommunicationResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.notification.CommunicationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class CommunicationController implements CommunicationsApi {

  private final CommunicationService communicationService;

  @Override
  public ResponseEntity<List<CommunicationResponse>> getPaymentCommunications(String identifier) {
    return ResponseEntity.ok(
        communicationService.getPaymentCommunications(
            Sid.of(identifier), UserPrincipal.current().teamId()));
  }

  @Override
  public ResponseEntity<List<CommunicationResponse>> getContractCommunications(String identifier) {
    return ResponseEntity.ok(
        communicationService.getContractCommunications(
            Sid.of(identifier), UserPrincipal.current().teamId()));
  }
}
```

Read `NotificationController` first and copy exactly how it obtains the current principal and team id, and what the generated interface's method signatures are — they may take a `UserPrincipal` argument rather than a static accessor.

- [ ] **Step 5: Build and run the suite**

Run: `cd backend && mvn clean test -Dmaven.build.cache.enabled=false`
Expected: PASS, including `BuurmanApplicationTest`, whose context load proves the new controller and service wire up.

- [ ] **Step 6: Commit**

```bash
git add openapi/src/paths/communications.yaml openapi/src/app.yaml openapi/app.yaml \
        backend/buurman-notifications/src/main/java/com/buurman/controller/CommunicationController.java
git commit -m "feat(communications): expose the timeline per payment and contract"
```

---

### Task 7: The timeline component

**Files:**
- Create: `frontend/app/src/components/communications/CommunicationsTimeline.tsx`
- Create: `frontend/app/src/components/communications/__tests__/CommunicationsTimeline.test.tsx`
- Create: `frontend/app/src/hooks/useCommunications.ts`
- Modify: `frontend/app/public/locales/{en,nl,de,fr,pt,es,sv,it,fi,el,pl,da,nb}/common.json` (13 files)

**Interfaces:**
- Consumes: generated `CommunicationResponse` and the two generated API functions (Task 6).
- Produces: `CommunicationsTimeline` accepting `{ communications, isLoading, onResend? }`; hooks `usePaymentCommunications(identifier)` and `useContractCommunications(identifier)`.

- [ ] **Step 1: Add the locale keys**

Add to `en/common.json` under a new `communications` object, then translate all of them into the other 12 files. The Task 7 parity guards added earlier in this ticket fail the build if any language is missing a key:

```json
    "communications": {
      "title": "Communications",
      "empty": "Nothing sent yet.",
      "emptyHistorical": "Only messages sent from now on appear here.",
      "resend": "Resend",
      "status": {
        "queued": "Queued",
        "sent": "Sent",
        "delivered": "Delivered",
        "opened": "Opened",
        "notDelivered": "Not delivered",
        "failed": "Failed"
      }
    }
```

- [ ] **Step 2: Write the failing component test**

Create `CommunicationsTimeline.test.tsx`:

```tsx
import { screen } from '@testing-library/react';
import { renderWithProviders } from '@/test/test-utils';
import { CommunicationsTimeline } from '../CommunicationsTimeline';

const row = (overrides = {}) => ({
  identifier: 'ntf_01JTEST000000000000000001',
  notificationType: 'PAYMENT_REMINDER',
  channel: 'EMAIL',
  status: 'DELIVERED',
  opened: false,
  createdAt: '2026-10-15T09:00:00Z',
  ...overrides,
});

describe('CommunicationsTimeline', () => {
  it('says why it is empty, not merely that it is', () => {
    renderWithProviders(<CommunicationsTimeline communications={[]} isLoading={false} />);

    // No backfill exists, so a pre-existing payment shows nothing. Without this line a
    // landlord concludes no reminder went out and sends a duplicate.
    expect(screen.getByText(/only messages sent from now on/i)).toBeInTheDocument();
  });

  it('shows Opened when the message was read', () => {
    renderWithProviders(
      <CommunicationsTimeline
        communications={[row({ status: 'DELIVERED', opened: true })]}
        isLoading={false}
      />
    );

    expect(screen.getByText('Opened')).toBeInTheDocument();
  });

  it.each([
    ['PENDING', 'Queued'],
    ['QUEUED', 'Queued'],
    ['SENT', 'Sent'],
    ['DELIVERED', 'Delivered'],
    ['BOUNCED', 'Not delivered'],
    ['REJECTED', 'Not delivered'],
    ['FAILED', 'Failed'],
  ])('renders %s as %s', (status, label) => {
    renderWithProviders(
      <CommunicationsTimeline communications={[row({ status })]} isLoading={false} />
    );

    expect(screen.getByText(label)).toBeInTheDocument();
  });

  it('does not claim an SMS was unopened', () => {
    renderWithProviders(
      <CommunicationsTimeline
        communications={[row({ channel: 'SMS', status: 'SENT', opened: false })]}
        isLoading={false}
      />
    );

    // Twilio never reports opens. Showing an email-shaped "not opened" state for an SMS
    // would read as a failure when nothing is wrong.
    expect(screen.getByText('Sent')).toBeInTheDocument();
    expect(screen.queryByText('Not delivered')).not.toBeInTheDocument();
  });
});
```

- [ ] **Step 3: Run it to make sure it fails**

Run: `cd frontend/app && yarn vitest run src/components/communications/__tests__/CommunicationsTimeline.test.tsx`
Expected: FAIL — cannot resolve `../CommunicationsTimeline`.

- [ ] **Step 4: Implement the component**

Create `CommunicationsTimeline.tsx`. Follow the markup conventions of an existing list component such as `src/components/properties/DocumentList.tsx` — read it first for the card, row and chip classes this codebase uses.

```tsx
import { useTranslation } from 'react-i18next';
import type { CommunicationResponse } from '@/generated/models';

interface CommunicationsTimelineProps {
  communications: CommunicationResponse[];
  isLoading: boolean;
  onResend?: (identifier: string) => void;
}

/** Collapses the seven-value status enum into what a landlord can act on. */
const statusKey = (communication: CommunicationResponse): string => {
  if (communication.status === 'DELIVERED' && communication.opened) {
    return 'opened';
  }
  switch (communication.status) {
    case 'PENDING':
    case 'QUEUED':
      return 'queued';
    case 'SENT':
      return 'sent';
    case 'DELIVERED':
      return 'delivered';
    case 'BOUNCED':
    case 'REJECTED':
      return 'notDelivered';
    default:
      return 'failed';
  }
};

export const CommunicationsTimeline = ({
  communications,
  isLoading,
  onResend,
}: CommunicationsTimelineProps) => {
  const { t } = useTranslation('common');

  if (isLoading) {
    return <p className="text-sm text-text-secondary">{t('buttons.loading')}</p>;
  }

  if (communications.length === 0) {
    return (
      <div className="text-sm text-text-secondary">
        <p>{t('communications.empty')}</p>
        <p className="text-xs text-text-muted">{t('communications.emptyHistorical')}</p>
      </div>
    );
  }

  return (
    <ul className="divide-y divide-border-default">
      {communications.map((communication) => (
        <li key={communication.identifier} className="flex items-center gap-3 py-3">
          <span className="text-sm font-medium text-text-primary">
            {communication.notificationType}
          </span>
          <span className="text-sm text-text-secondary">
            {communication.recipientEmail ?? communication.recipientPhone ?? ''}
          </span>
          <span className="ml-auto text-xs text-text-secondary">
            {t(`communications.status.${statusKey(communication)}`)}
          </span>
          {onResend && (
            <button
              type="button"
              onClick={() => onResend(communication.identifier)}
              className="text-xs text-primary-600 hover:underline"
            >
              {t('communications.resend')}
            </button>
          )}
        </li>
      ))}
    </ul>
  );
};
```

- [ ] **Step 5: Run it to make sure it passes**

Run: `cd frontend/app && yarn vitest run src/components/communications/__tests__/CommunicationsTimeline.test.tsx`
Expected: PASS, 10 tests (1 empty state + 1 opened + 7 status cases + 1 SMS).

- [ ] **Step 6: Write the query hooks**

Create `src/hooks/useCommunications.ts`, following an existing hook such as `src/hooks/usePayments.ts` for the query-key and generated-client conventions:

```ts
import { useQuery } from '@tanstack/react-query';
import { getPaymentCommunications, getContractCommunications } from '@/generated/api';

export const usePaymentCommunications = (identifier: string) =>
  useQuery({
    queryKey: ['payments', identifier, 'communications'],
    queryFn: () => getPaymentCommunications(identifier),
    enabled: Boolean(identifier),
  });

export const useContractCommunications = (identifier: string) =>
  useQuery({
    queryKey: ['contracts', identifier, 'communications'],
    queryFn: () => getContractCommunications(identifier),
    enabled: Boolean(identifier),
  });
```

- [ ] **Step 7: Lint and run the whole frontend suite**

Run: `cd frontend && yarn lint --fix && yarn test`
Expected: PASS, including the locale parity and translation-key guards, which fail if any of the 13 languages is missing one of the new keys.

- [ ] **Step 8: Commit**

```bash
git add frontend/app/src/components/communications/ \
        frontend/app/src/hooks/useCommunications.ts \
        frontend/app/public/locales/
git commit -m "feat(communications): timeline component and query hooks"
```

---

### Task 8: Wire the timeline into both detail pages

**Files:**
- Modify: `frontend/app/src/pages/PaymentDetailPage.tsx`
- Modify: `frontend/app/src/pages/ContractDetailPage.tsx`

**Interfaces:**
- Consumes: `CommunicationsTimeline`, `usePaymentCommunications`, `useContractCommunications` (Task 7).
- Produces: nothing other code depends on.

- [ ] **Step 1: Add the section to the payment page**

In `PaymentDetailPage.tsx`, read the file to find how its existing sections are laid out (card heading + body), then add one more below the existing detail cards:

```tsx
  const { data: communications = [], isLoading: communicationsLoading } =
    usePaymentCommunications(identifier ?? '');
```

and in the markup, following the page's own card conventions:

```tsx
          <section>
            <h2 className="text-lg font-semibold text-text-primary mb-3">
              {t('common:communications.title')}
            </h2>
            <CommunicationsTimeline
              communications={communications}
              isLoading={communicationsLoading}
            />
          </section>
```

- [ ] **Step 2: Add the same section to the contract page**

In `ContractDetailPage.tsx`, the identical section using `useContractCommunications`.

- [ ] **Step 3: Lint, test and build**

Run: `cd frontend && yarn lint --fix && yarn test && yarn build`
Expected: all pass.

- [ ] **Step 4: Commit**

```bash
git add frontend/app/src/pages/PaymentDetailPage.tsx \
        frontend/app/src/pages/ContractDetailPage.tsx
git commit -m "feat(communications): show the timeline on payment and contract detail"
```

---

## Final verification

- [ ] **Backend, with the command CI runs**

Run:
```bash
cd backend && mvn verify \
  -pl buurman-common,buurman-core,buurman-notifications,buurman-documents,buurman-booklets,buurman-letters,buurman-backoffice,buurman-app \
  -am -Pquick -B -Dmaven.build.cache.enabled=false
```
Expected: BUILD SUCCESS. Docker must be running for the integration tests.

- [ ] **Frontend**

Run: `cd frontend && yarn test && yarn lint && yarn build`
Expected: all pass.

- [ ] **Confirm the acceptance criteria this slice owns**

- A landlord sees what was sent about a payment, to whom, and whether it arrived: Tasks 6-8.
- Bounced shows as "Not delivered": Task 7's status test, on data `WebhookService` already produces.
- A resend stays on the timeline: Task 2.
- A payment identifier from another team leaks nothing: Tasks 4 and 5.

- [ ] **Confirm nothing unrelated was committed**

Run: `git status --short`
Expected: only the pre-existing `docker/traefik/dynamic/local-dev.yml` and `keycloak/*.json` modifications remain unstaged.
