package com.buurman.service.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buurman.domain.Contract;
import com.buurman.domain.Notification;
import com.buurman.domain.NotificationChannel;
import com.buurman.domain.NotificationStatus;
import com.buurman.domain.NotificationType;
import com.buurman.domain.Payment;
import com.buurman.domain.Sid;
import com.buurman.domain.identifier.NotificationIdentifier;
import com.buurman.dto.response.CommunicationAudience;
import com.buurman.dto.response.CommunicationBodyResponse;
import com.buurman.dto.response.CommunicationResponse;
import com.buurman.exception.NotFoundException;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.NotificationRepository;
import com.buurman.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommunicationService")
class CommunicationServiceTest {

  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID PAYMENT_ID = UUID.randomUUID();
  private static final UUID CONTRACT_ID = UUID.randomUUID();
  private static final Sid PAYMENT_SID = Sid.of("pay_01JTEST000000000000000001");
  private static final Sid CONTRACT_SID = Sid.of("con_01JTEST000000000000000001");

  @Mock private NotificationRepository notificationRepository;
  @Mock private PaymentRepository paymentRepository;
  @Mock private ContractRepository contractRepository;
  @Mock private NotificationService notificationService;

  private CommunicationService service;

  @BeforeEach
  void setUp() {
    service =
        new CommunicationService(
            notificationRepository, paymentRepository, contractRepository, notificationService);
  }

  @Test
  @DisplayName("a payment from another team is not found, and never reaches the notifications")
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
  @DisplayName("a contract from another team is not found either")
  void contractFromAnotherTeamIsNotFound() {
    when(contractRepository.findByIdentifierAndTeamId(CONTRACT_SID, TEAM_ID))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getContractCommunications(CONTRACT_SID, TEAM_ID))
        .isInstanceOf(NotFoundException.class);
    verifyNoInteractions(notificationRepository);
  }

  @Test
  @DisplayName("returns the payment's communications")
  void returnsPaymentCommunications() {
    Payment payment = new Payment();
    payment.setId(PAYMENT_ID);
    when(paymentRepository.findByIdentifierAndTeamId(PAYMENT_SID, TEAM_ID))
        .thenReturn(Optional.of(payment));
    when(notificationRepository.findByPaymentIdAndTeamId(PAYMENT_ID, TEAM_ID))
        .thenReturn(List.of());

    assertThat(service.getPaymentCommunications(PAYMENT_SID, TEAM_ID)).isEmpty();
  }

  @Test
  @DisplayName("returns the contract's communications")
  void returnsContractCommunications() {
    Contract contract = new Contract();
    contract.setId(CONTRACT_ID);
    when(contractRepository.findByIdentifierAndTeamId(CONTRACT_SID, TEAM_ID))
        .thenReturn(Optional.of(contract));
    when(notificationRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
        .thenReturn(List.of());

    assertThat(service.getContractCommunications(CONTRACT_SID, TEAM_ID)).isEmpty();
  }

  /**
   * A single contract-expiry send fans out to one notification per admin/editor per channel
   * alongside the one that goes to the tenant, and the timeline query returns all of them. Without
   * a discriminator the client cannot tell "we told your tenant" from "we told you", so it shows
   * internal copies as if they were tenant contact and offers Resend on each — which would send a
   * colleague a duplicate while the landlord believes they are chasing the tenant.
   *
   * <p>recipientContactId is the discriminator: a contact is a tenant or other counterparty, a
   * recipientUserId is someone on the team.
   */
  @Test
  @DisplayName("marks who each communication actually went to")
  void distinguishesTenantCommunicationsFromInternalCopies() {
    Contract contract = new Contract();
    contract.setId(CONTRACT_ID);
    when(contractRepository.findByIdentifierAndTeamId(CONTRACT_SID, TEAM_ID))
        .thenReturn(Optional.of(contract));
    when(notificationRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
        .thenReturn(
            List.of(
                notification(Optional.of(UUID.randomUUID()), Optional.empty()),
                notification(Optional.empty(), Optional.of(UUID.randomUUID()))));

    List<CommunicationResponse> communications =
        service.getContractCommunications(CONTRACT_SID, TEAM_ID);

    assertThat(communications)
        .extracting(CommunicationResponse::audience)
        .containsExactly(CommunicationAudience.CONTACT, CommunicationAudience.TEAM);
  }

  /**
   * A notification with neither recipient is a system record, not something anyone was told. It
   * must not fall through to CONTACT, or the timeline would claim the tenant was contacted.
   */
  @Test
  @DisplayName("a communication with no contact and no user is neither audience")
  void communicationWithoutARecipientIsNotAttributedToTheTenant() {
    Contract contract = new Contract();
    contract.setId(CONTRACT_ID);
    when(contractRepository.findByIdentifierAndTeamId(CONTRACT_SID, TEAM_ID))
        .thenReturn(Optional.of(contract));
    when(notificationRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
        .thenReturn(List.of(notification(Optional.empty(), Optional.empty())));

    assertThat(service.getContractCommunications(CONTRACT_SID, TEAM_ID))
        .extracting(CommunicationResponse::audience)
        .containsExactly(CommunicationAudience.UNKNOWN);
  }

  private static Notification notification(
      Optional<UUID> recipientContactId, Optional<UUID> recipientUserId) {
    return Notification.builder()
        .identifier(Optional.of(Sid.of("ntf_01JTEST000000000000000001")))
        .notificationType(NotificationType.CONTRACT_EXPIRY)
        .channel(NotificationChannel.EMAIL)
        .status(NotificationStatus.DELIVERED)
        .recipientContactId(recipientContactId)
        .recipientUserId(recipientUserId)
        .createdAt(Instant.now())
        .build();
  }

  private static final NotificationIdentifier COMMUNICATION_SID =
      NotificationIdentifier.of("ntf_01JTEST000000000000000001");

  @Test
  @DisplayName("a body under a payment from another team is not found, and reads no notification")
  void paymentBodyFromAnotherTeamIsNotFound() {
    when(paymentRepository.findByIdentifierAndTeamId(PAYMENT_SID, TEAM_ID))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> service.getPaymentCommunicationBody(PAYMENT_SID, COMMUNICATION_SID, TEAM_ID))
        .isInstanceOf(NotFoundException.class);
    verifyNoInteractions(notificationRepository);
  }

  @Test
  @DisplayName("a body under a contract from another team is not found either")
  void contractBodyFromAnotherTeamIsNotFound() {
    when(contractRepository.findByIdentifierAndTeamId(CONTRACT_SID, TEAM_ID))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> service.getContractCommunicationBody(CONTRACT_SID, COMMUNICATION_SID, TEAM_ID))
        .isInstanceOf(NotFoundException.class);
    verifyNoInteractions(notificationRepository);
  }

  /**
   * The notification lookup must be the team-scoped one. findByIdentifierUnscoped exists on the
   * same repository for the webhook path and is one autocomplete away from being used here.
   */
  @Test
  @DisplayName("a body whose notification belongs to another team is not found")
  void bodyOfANotificationFromAnotherTeamIsNotFound() {
    Payment payment = new Payment();
    payment.setId(PAYMENT_ID);
    when(paymentRepository.findByIdentifierAndTeamId(PAYMENT_SID, TEAM_ID))
        .thenReturn(Optional.of(payment));
    when(notificationRepository.findByIdentifierAndTeamId(COMMUNICATION_SID, TEAM_ID))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> service.getPaymentCommunicationBody(PAYMENT_SID, COMMUNICATION_SID, TEAM_ID))
        .isInstanceOf(NotFoundException.class);
  }

  /**
   * The one that matters. Both the payment and the notification are in the caller's team, so the
   * team check passes — but the notification is about a different payment. Without the entity check
   * every notification body in the team is readable through any payment the caller can see.
   */
  @Test
  @DisplayName("a body about another payment is not readable through this payment")
  void bodyOfANotificationAboutAnotherPaymentIsNotFound() {
    Payment payment = new Payment();
    payment.setId(PAYMENT_ID);
    when(paymentRepository.findByIdentifierAndTeamId(PAYMENT_SID, TEAM_ID))
        .thenReturn(Optional.of(payment));
    when(notificationRepository.findByIdentifierAndTeamId(COMMUNICATION_SID, TEAM_ID))
        .thenReturn(
            Optional.of(bodyNotification(Optional.of(UUID.randomUUID()), Optional.empty())));

    assertThatThrownBy(
            () -> service.getPaymentCommunicationBody(PAYMENT_SID, COMMUNICATION_SID, TEAM_ID))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  @DisplayName("a body about another contract is not readable through this contract")
  void bodyOfANotificationAboutAnotherContractIsNotFound() {
    Contract contract = new Contract();
    contract.setId(CONTRACT_ID);
    when(contractRepository.findByIdentifierAndTeamId(CONTRACT_SID, TEAM_ID))
        .thenReturn(Optional.of(contract));
    when(notificationRepository.findByIdentifierAndTeamId(COMMUNICATION_SID, TEAM_ID))
        .thenReturn(
            Optional.of(bodyNotification(Optional.empty(), Optional.of(UUID.randomUUID()))));

    assertThatThrownBy(
            () -> service.getContractCommunicationBody(CONTRACT_SID, COMMUNICATION_SID, TEAM_ID))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  @DisplayName("returns the stored message for a communication about this payment")
  void returnsTheStoredMessage() {
    Payment payment = new Payment();
    payment.setId(PAYMENT_ID);
    when(paymentRepository.findByIdentifierAndTeamId(PAYMENT_SID, TEAM_ID))
        .thenReturn(Optional.of(payment));
    when(notificationRepository.findByIdentifierAndTeamId(COMMUNICATION_SID, TEAM_ID))
        .thenReturn(Optional.of(bodyNotification(Optional.of(PAYMENT_ID), Optional.empty())));

    CommunicationBodyResponse response =
        service.getPaymentCommunicationBody(PAYMENT_SID, COMMUNICATION_SID, TEAM_ID);

    assertThat(response.body()).isEqualTo("<p>Your rent is due.</p>");
    assertThat(response.subject()).contains("Payment reminder");
    assertThat(response.channel()).isEqualTo("EMAIL");
  }

  private static Notification bodyNotification(
      Optional<UUID> relatedPaymentId, Optional<UUID> relatedContractId) {
    return Notification.builder()
        .identifier(Optional.of(COMMUNICATION_SID))
        .notificationType(NotificationType.PAYMENT_REMINDER)
        .channel(NotificationChannel.EMAIL)
        .status(NotificationStatus.DELIVERED)
        .subject(Optional.of("Payment reminder"))
        .body("<p>Your rent is due.</p>")
        .relatedPaymentId(relatedPaymentId)
        .relatedContractId(relatedContractId)
        .createdAt(Instant.now())
        .build();
  }
}
