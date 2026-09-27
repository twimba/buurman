package com.buurman.service.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import com.buurman.domain.Contract;
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
}
