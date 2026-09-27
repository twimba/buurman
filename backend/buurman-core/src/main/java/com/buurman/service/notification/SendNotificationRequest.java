package com.buurman.service.notification;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.buurman.domain.NotificationType;
import com.buurman.domain.NotificationUrgency;

import lombok.Builder;

@Builder
public record SendNotificationRequest(
    Optional<UUID> teamId,
    NotificationType notificationType,
    Optional<UUID> recipientUserId,
    Optional<UUID> recipientContactId,
    Optional<String> recipientEmail,
    Optional<String> recipientPhone,
    /** The payment this notification is about, for the communications timeline. */
    Optional<UUID> relatedPaymentId,
    /** The contract this notification is about. A payment reminder sets this and the payment. */
    Optional<UUID> relatedContractId,
    /**
     * Context language for rendering (BCP 47 tag) — typically a contract's document language.
     * Ranked BELOW the recipient's own preference, not an override: the resolution order is the
     * contact's language, then the recipient user's, then this, then the team default, then
     * English. See {@code RecipientLocaleResolver}.
     */
    Optional<String> contextLanguageTag,
    String templateName,
    Map<String, Object> templateVariables,
    NotificationUrgency urgency,
    UUID createdBy,
    /** Files (already in object storage) to attach to the email. */
    List<EmailAttachment> attachments) {

  /** Customize the Lombok-generated builder to provide defaults for Optional and urgency fields. */
  @SuppressWarnings("NullAway.Init")
  public static class SendNotificationRequestBuilder {
    private Optional<UUID> teamId = Optional.empty();
    private Optional<UUID> recipientUserId = Optional.empty();
    private Optional<UUID> recipientContactId = Optional.empty();
    private Optional<String> recipientEmail = Optional.empty();
    private Optional<String> recipientPhone = Optional.empty();
    private Optional<UUID> relatedPaymentId = Optional.empty();
    private Optional<UUID> relatedContractId = Optional.empty();
    private Optional<String> contextLanguageTag = Optional.empty();
    private NotificationUrgency urgency = NotificationUrgency.NORMAL;
    private List<EmailAttachment> attachments = List.of();
  }
}
