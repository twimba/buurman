package com.buurman.service.notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.Notification;
import com.buurman.domain.Sid;
import com.buurman.domain.identifier.NotificationIdentifier;
import com.buurman.dto.response.CommunicationAudience;
import com.buurman.dto.response.CommunicationBodyResponse;
import com.buurman.dto.response.CommunicationResponse;
import com.buurman.exception.NotFoundException;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.NotificationRepository;
import com.buurman.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;

/**
 * The delivery trail for one payment or contract.
 *
 * <p>Separate from {@link NotificationCenterService}, which stays TEAM_ADMIN-only for the admin
 * delivery log. Reading a timeline is TEAM_VIEWER and above: it reveals no more than the payment
 * and the contact already visible on the same page.
 */
@Service
@RequiredArgsConstructor
public class CommunicationService {

  private final NotificationRepository notificationRepository;
  private final PaymentRepository paymentRepository;
  private final ContractRepository contractRepository;
  private final NotificationService notificationService;

  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('TEAM_VIEWER')")
  public List<CommunicationResponse> getPaymentCommunications(Sid identifier, UUID teamId) {
    // Resolve through the team-scoped lookup FIRST: an identifier from another team must fail
    // here, before any notification is read.
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

  /**
   * The stored message of one communication about this payment, for the preview.
   *
   * <p>TEAM_VIEWER, the same as reading the timeline, and deliberately not TEAM_EDITOR: the editor
   * gate on resend is about a side effect that costs money, not about disclosure. The body is the
   * prose form of what is already on the page — the tenant, the property, the amount, the due date
   * — under a subject line the timeline already shows this same viewer.
   *
   * <p>NotificationCenterService is TEAM_ADMIN for a different reason: it is the unscoped,
   * team-wide delivery log covering every entity and every type, including verification codes and
   * invitations. This endpoint has no such breadth — it reaches only what this payment is about.
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('TEAM_VIEWER')")
  public CommunicationBodyResponse getPaymentCommunicationBody(
      Sid paymentIdentifier, NotificationIdentifier communicationIdentifier, UUID teamId) {
    UUID paymentId =
        paymentRepository
            .findByIdentifierAndTeamId(paymentIdentifier, teamId)
            .orElseThrow(() -> new NotFoundException("Payment not found"))
            .getId();
    return toBodyResponse(
        messageAbout(
            communicationIdentifier, teamId, Notification::getRelatedPaymentId, paymentId));
  }

  /** See {@link #getPaymentCommunicationBody}. */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('TEAM_VIEWER')")
  public CommunicationBodyResponse getContractCommunicationBody(
      Sid contractIdentifier, NotificationIdentifier communicationIdentifier, UUID teamId) {
    UUID contractId =
        contractRepository
            .findByIdentifierAndTeamId(contractIdentifier, teamId)
            .orElseThrow(() -> new NotFoundException("Contract not found"))
            .getId();
    return toBodyResponse(
        messageAbout(
            communicationIdentifier, teamId, Notification::getRelatedContractId, contractId));
  }

  /**
   * Two gates, both required. The team-scoped lookup keeps another team's notifications out; the
   * entity check keeps this team's *other* notifications out. Without the second, any notification
   * identifier in the team would be readable through any payment the caller can already see, which
   * the cross-team test would not catch.
   */
  private Notification messageAbout(
      NotificationIdentifier identifier,
      UUID teamId,
      Function<Notification, Optional<UUID>> relatedId,
      UUID expectedEntityId) {
    return notificationRepository
        .findByIdentifierAndTeamId(identifier, teamId)
        .filter(
            notification ->
                relatedId.apply(notification).filter(expectedEntityId::equals).isPresent())
        .orElseThrow(() -> new NotFoundException("Communication not found"));
  }

  private CommunicationBodyResponse toBodyResponse(Notification notification) {
    return new CommunicationBodyResponse(
        notification.getIdentifier().orElseThrow(),
        notification.getChannel().name(),
        notification.getSubject(),
        notification.getBody());
  }

  /**
   * Spec S4: resending sends a real message and costs money, so it sits a role above reading. The
   * notification is resolved team-scoped first, exactly as the read paths are.
   */
  @Transactional
  @PreAuthorize("hasRole('TEAM_EDITOR')")
  public CommunicationResponse resend(NotificationIdentifier identifier, UUID teamId, UUID userId) {
    return toResponse(notificationService.resend(teamId, identifier, userId));
  }

  /**
   * A contact is the tenant or another counterparty; a user is someone on the team. Checked in that
   * order because a notification carrying both is addressed to the contact — the user id is then
   * the team member who triggered it, not a second recipient.
   */
  private static CommunicationAudience audienceOf(Notification notification) {
    if (notification.getRecipientContactId().isPresent()) {
      return CommunicationAudience.CONTACT;
    }
    if (notification.getRecipientUserId().isPresent()) {
      return CommunicationAudience.TEAM;
    }
    return CommunicationAudience.UNKNOWN;
  }

  private List<CommunicationResponse> toResponses(List<Notification> notifications) {
    return notifications.stream().map(this::toResponse).toList();
  }

  private CommunicationResponse toResponse(Notification notification) {
    return new CommunicationResponse(
        notification.getIdentifier().orElseThrow(),
        notification.getNotificationType().name(),
        notification.getChannel().name(),
        audienceOf(notification),
        notification.getSubject(),
        notification.getRecipientEmail(),
        notification.getRecipientPhone(),
        notification.getStatus().name(),
        notification.getProviderError(),
        notification.getFirstOpenedAt().isPresent(),
        notification.getFirstOpenedAt(),
        notification.getResentFromId().isPresent(),
        notification.getCreatedAt());
  }
}
