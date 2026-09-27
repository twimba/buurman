# Communications timeline — design

BUUR-109 item 5. Date: 2026-09-27.

## Intended outcome

A landlord looking at a payment or a contract can see what Buurman sent about
it, to whom, and whether it arrived — without leaving the page and without the
admin delivery log.

Success looks like: a tenant has not paid, the landlord opens the payment, and
sees "Payment reminder, email to jan@example.com, 15/10/2026 — delivered,
opened". They know the tenant saw it, so the next step is a phone call rather
than another reminder. If it says "bounced", they know the email address is
wrong and nothing has reached the tenant at all.

## Current state

Most of this is already built. Verified on this branch:

| Needed | State |
| --- | --- |
| Delivery lifecycle | `NotificationStatus` = PENDING, QUEUED, SENT, DELIVERED, FAILED, BOUNCED, REJECTED |
| Provider feedback | `WebhookService` ingests delivered, opened, clicked, failed, complained, unsubscribed; `failed` + `permanent` maps to BOUNCED |
| Open/click tracking | `notifications.open_count`, `click_count`, `first_opened_at`, `first_clicked_at` |
| Resend | `NotificationCenterService.resendNotification`, with `resent_from_id` and `resend_reason` lineage |
| Team scoping | `NotificationRepository` filters every query by `team_id` |

The "bounced" half of this ticket item therefore does **not** depend on item 6.
Item 6's remaining work is marking the contact's email invalid and surfacing it
on the contact card, which uses the existing `recipient_contact_id`.

What is missing is one thing: **a notification does not record which payment or
contract it is about.** `notifications` has `recipient_user_id`,
`recipient_contact_id`, `recipient_email` and `recipient_phone`, but no entity
link. `content_variables` holds only display strings — `propertyName`,
`amount`, `contactName`, `dueDate` — never an identifier.

## Consequence: there is no backfill

Because no identifier was ever stored, existing notifications cannot be
attributed to a payment or contract retroactively. Every payment and contract
that exists today will show an empty timeline, filling in as new messages go
out. An entity-linked-only timeline was chosen over falling back to
recipient-and-date-window matching: when a landlord is deciding whether to
chase a tenant, showing a reminder for a *different* payment as though it
belonged to this one is worse than showing nothing.

This makes the empty state load-bearing. It must say that only messages sent
from now on are tracked, or it reads as broken.

## S1 — Schema

`V071__notification_related_entities.sql`:

```sql
ALTER TABLE notifications
    ADD COLUMN payment_id UUID REFERENCES payments (id),
    ADD COLUMN contract_id UUID REFERENCES contracts (id);

CREATE INDEX idx_notifications_payment ON notifications (payment_id)
    WHERE payment_id IS NOT NULL;
CREATE INDEX idx_notifications_contract ON notifications (contract_id)
    WHERE contract_id IS NOT NULL;
```

Explicit nullable foreign keys rather than a generic
`related_entity_type`/`related_entity_id` pair, for three reasons. A payment
reminder is about a payment **and** its contract, so it sets both and the
contract timeline is rich without joining through `payments.contract_id`. The
columns carry real referential integrity and JOOQ typing. And it matches how
every other relationship in this schema is modelled.

The cost is a column per entity type. That is acceptable because only two are
in scope and item 6 needs none.

## S2 — Send path

`SendNotificationRequest` gains two components. Every one of the 25 call sites
uses `SendNotificationRequest.builder()` — there are no positional constructor
calls — so giving them builder defaults of `Optional.empty()` keeps all of them
compiling unchanged:

```java
    Optional<UUID> relatedPaymentId,
    Optional<UUID> relatedContractId,
```

`NotificationServiceImpl` passes them through to the `Notification` domain
object; `NotificationRepository` writes them on insert. The resend path copies
them from the original, so a resent reminder stays on the same timeline.

Six services send about these entities and set them:

| Service | Notification types | Sets |
| --- | --- | --- |
| `PaymentReminderService` | PAYMENT_REMINDER (manual and bulk) | payment + contract |
| `NotificationSchedulerService` | PAYMENT_REMINDER (scheduled), CONTRACT_EXPIRY | payment + contract / contract |
| `PaymentService` | PAYMENT_PAID, PAYMENT_RECEIVAL | payment + contract |
| `ContractService` | CONTRACT_CREATED, CONTRACT_STATUS_CHANGED, CONTRACT_REOPENED | contract |
| `ContractExtensionService` | CONTRACT_EXTENDED, CONTRACT_EXTENSION_PENDING, CONTRACT_ROLLED_OVER, CONTRACT_RENEWAL_REMINDER | contract |
| `ContractRentPeriodService` | CONTRACT_RENT_ADJUSTED | contract |

`NotificationSchedulerService` is easy to miss: it is the scheduled path, so
the reminders a landlord never triggers by hand are exactly the ones whose
absence from the timeline would look like the feature is broken. CONTRACT_EXPIRY
is sent from there too, not from `ContractService`.

Welcome, verification, phone verification, team invitation, invitation
accepted, password changed, property created, expense created and the
notification digest set neither — they are not about a payment or a contract.

A digest consolidates several notifications, so it is deliberately left
unlinked rather than attributed to an arbitrary one of them.

## S3 — Read path

Two endpoints, keyed by the entity's Sid:

```
GET /payments/{identifier}/communications
GET /contracts/{identifier}/communications
```

Not new `paymentId`/`contractId` filters on the existing admin
`getNotifications`: those would put internal UUIDs in query parameters, which
this codebase forbids. Nesting under the entity also makes authorization and
team scoping fall out of the existing `getByIdentifierAndTeamId` lookup.

A new `CommunicationResponse` rather than reusing `NotificationResponse`:

```java
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

It omits `body` — a landlord does not need the full rendered message in a
timeline row, and leaving it out keeps the payload small — and collapses
`openCount`/`clickCount` to a single `opened` flag, since the question the
timeline answers is "did it arrive and was it read", not analytics.

Ordered newest first. No pagination: the realistic count per payment is under
ten, and a contract's is bounded by its term.

## S4 — Authorization

A new `CommunicationService` in `buurman-notifications`, so the existing
`NotificationCenterService` keeps its `TEAM_ADMIN`-only guard and the admin
delivery log is untouched.

- Reading a timeline: `TEAM_VIEWER` and above. It reveals no more than the
  payment and contact already visible on the same page.
- Resending from a timeline: `TEAM_EDITOR` and above, because it sends a real
  message and costs money.

Both go through the entity's team-scoped lookup first, so a payment identifier
from another team resolves to nothing rather than leaking its communications.

## S5 — UI

One `CommunicationsTimeline` component, used by `PaymentDetailPage` and
`ContractDetailPage`. Each row: channel icon, translated notification type,
recipient, relative time, a status chip, and a resend button for
`TEAM_EDITOR`+.

Status chips collapse the seven-value enum into what a landlord acts on:

| Chip | Status |
| --- | --- |
| Queued | PENDING, QUEUED |
| Sent | SENT |
| Delivered | DELIVERED, plus "opened" when `firstOpenedAt` is set |
| Not delivered | BOUNCED, REJECTED |
| Failed | FAILED |

BOUNCED and REJECTED read as "Not delivered" with the provider error as a
tooltip, because the distinction between them is operational detail a landlord
cannot act on differently.

All copy goes in the existing 13 locale bundles, and the parity guards added
earlier in this ticket will fail the build if any language is missing a key.

## Testing

- `NotificationRepositoryIntegrationTest` — the entity filter returns only
  matching rows, and a payment from another team returns none.
- `CommunicationServiceTest` — the two authorization levels, and that a resend
  carries the entity link onto the new notification.
- `PaymentReminderServiceTest` / `ContractServiceTest` — the sends set the link.
  Without this the feature silently produces empty timelines, which is the
  failure mode most likely to ship unnoticed.
- `CommunicationsTimeline` component test — one case per status chip, the
  opened variant, and the empty state.
- The existing i18n parity guards cover the new locale keys.

## Out of scope

Property and contact detail pages get no timeline: the ticket names payment and
contract, and `recipient_contact_id` already answers "what did we send this
tenant" if that is wanted later. No pagination, no filtering, no export. Item
6's contact-email-invalid flag is a separate spec.
