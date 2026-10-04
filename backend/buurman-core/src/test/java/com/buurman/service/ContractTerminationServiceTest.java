package com.buurman.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import com.buurman.domain.Contract;
import com.buurman.domain.ContractTermination;
import com.buurman.domain.ContractTerminationStatus;
import com.buurman.domain.Document;
import com.buurman.domain.Sid;
import com.buurman.domain.TerminationGivenBy;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.dto.request.TerminateContractRequest;
import com.buurman.exception.BadRequestException;
import com.buurman.exception.BusinessRuleException;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.ContractTerminationRepository;
import com.buurman.repository.DocumentRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.service.regulation.TerminationRuleResolver;
import com.buurman.util.Constants;
import com.buurman.util.MoneyAmount;

class ContractTerminationServiceTest {

  private final ContractRepository contractRepository = mock(ContractRepository.class);
  private final ContractTerminationRepository terminationRepository =
      mock(ContractTerminationRepository.class);
  private final TerminationRuleResolver ruleResolver = mock(TerminationRuleResolver.class);
  private final ContractService contractService = mock(ContractService.class);
  private final ContractTerminationLetterGenerator letterExporter =
      mock(ContractTerminationLetterGenerator.class);
  private final DepositService depositService = mock(DepositService.class);
  private final DocumentRepository documentRepository = mock(DocumentRepository.class);
  private final S3StorageService s3StorageService = mock(S3StorageService.class);
  private final TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
  private final Clock clock = Clock.fixed(Instant.parse("2026-03-15T12:00:00Z"), ZoneOffset.UTC);

  private ContractTerminationService service;

  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID CONTRACT_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();
  private static final ContractIdentifier IDENTIFIER =
      ContractIdentifier.of("CON00000000000000000000001");

  private final UserPrincipal principal =
      new UserPrincipal(
          USER_ID, "USR1", "kc-1", "landlord@example.com", "Landlord", TEAM_ID, "TEA1", null);

  private Contract contract;

  @BeforeEach
  void setUp() {
    service =
        new ContractTerminationService(
            contractRepository,
            terminationRepository,
            ruleResolver,
            contractService,
            letterExporter,
            depositService,
            documentRepository,
            s3StorageService,
            transactionTemplate,
            clock);

    // sweepDueTerminations runs each termination through transactionTemplate rather than a
    // method-level @Transactional (see its javadoc) — the mock must actually invoke the callback
    // for the sweep tests below to observe any effect.
    doAnswer(
            inv -> {
              Consumer<TransactionStatus> callback = inv.getArgument(0);
              callback.accept(null);
              return null;
            })
        .when(transactionTemplate)
        .executeWithoutResult(any());

    contract =
        Contract.builder()
            .id(CONTRACT_ID)
            .identifier(Optional.<Sid>of(IDENTIFIER))
            .teamId(TEAM_ID)
            .status(Contract.ContractStatus.ACTIVE)
            .startDate(LocalDate.now(ZoneId.systemDefault()).minusYears(2))
            .landlordNoticeDays(30)
            .rentAmount(MoneyAmount.of(new java.math.BigDecimal("1000.00"), "EUR"))
            .build();
    when(contractRepository.getByIdentifierAndTeamId(
            any(Sid.class), org.mockito.ArgumentMatchers.eq(TEAM_ID)))
        .thenReturn(contract);
    when(terminationRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
        .thenReturn(Optional.empty());
    when(terminationRepository.save(any()))
        .thenAnswer(
            inv -> {
              ContractTermination t = inv.getArgument(0);
              if (t.getId() == null) {
                t.setId(UUID.randomUUID());
                t.setIdentifier(Optional.of(Sid.of("CTM00000000000000000000001")));
              }
              return t;
            });
    when(ruleResolver.resolve(any(), any(), any()))
        .thenReturn(
            new TerminationRuleResolver.TerminationComputation(
                90, false, java.util.List.of(), TerminationRuleResolver.Source.CATALOG_RULE));
    when(letterExporter.generate(any(), any(), any())).thenReturn(new byte[] {1, 2, 3});
    when(s3StorageService.uploadFile(any(), any(), any(), any(), any(), any()))
        .thenReturn("s3/key.pdf");
    when(documentRepository.save(any()))
        .thenAnswer(
            inv -> {
              Document d = inv.getArgument(0);
              d.setId(UUID.randomUUID());
              d.setIdentifier(Optional.of(Sid.of("DOC00000000000000000000001")));
              return d;
            });
  }

  @Test
  @DisplayName("override earlier than computed without a reason is rejected")
  void overrideWithoutReasonRejected() {
    var request =
        new TerminateContractRequest(
            TerminationGivenBy.LANDLORD,
            LocalDate.now(ZoneId.systemDefault()),
            Optional.empty(),
            Optional.of(LocalDate.now(ZoneId.systemDefault()).plusDays(10)),
            Optional.empty(),
            Optional.empty());

    assertThatThrownBy(() -> service.terminate(IDENTIFIER, request, principal))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  @DisplayName(
      "a contract already having a termination record is rejected with a business rule error")
  void alreadyTerminatedRejected() {
    when(terminationRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
        .thenReturn(Optional.of(ContractTermination.builder().id(UUID.randomUUID()).build()));

    var request =
        new TerminateContractRequest(
            TerminationGivenBy.LANDLORD,
            LocalDate.now(ZoneId.systemDefault()),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());

    assertThatThrownBy(() -> service.terminate(IDENTIFIER, request, principal))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  @DisplayName(
      "a valid termination transitions the contract to NOTICE_GIVEN and persists the computed date")
  void validTerminationSucceeds() {
    var request =
        new TerminateContractRequest(
            TerminationGivenBy.LANDLORD,
            LocalDate.now(ZoneId.systemDefault()),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());

    service.terminate(IDENTIFIER, request, principal);

    // Goes straight to the package-private transitionStatus: the public changeContractStatus
    // refuses NOTICE_GIVEN so the generic status endpoint cannot bypass this workflow.
    verify(contractService)
        .transitionStatus(
            org.mockito.ArgumentMatchers.eq(contract),
            org.mockito.ArgumentMatchers.eq(Contract.ContractStatus.NOTICE_GIVEN),
            org.mockito.ArgumentMatchers.eq(USER_ID),
            org.mockito.ArgumentMatchers.eq(Optional.empty()));
    verify(contractService, org.mockito.Mockito.never()).changeContractStatus(any(), any(), any());
  }

  @Test
  @DisplayName("a whitespace-only overrideReason is rejected the same as a missing one")
  void blankOverrideReasonRejected() {
    var request =
        new TerminateContractRequest(
            TerminationGivenBy.LANDLORD,
            LocalDate.now(ZoneId.systemDefault()),
            Optional.empty(),
            Optional.of(LocalDate.now(ZoneId.systemDefault()).plusDays(10)),
            Optional.of("   "),
            Optional.empty());

    assertThatThrownBy(() -> service.terminate(IDENTIFIER, request, principal))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("overrideReason");
    verifyNoInteractions(contractService);
  }

  @Test
  @DisplayName("an earlier effective end date with a real overrideReason is accepted")
  void overrideWithReasonAccepted() {
    LocalDate earlier = LocalDate.now(ZoneId.systemDefault()).plusDays(10);
    var request =
        new TerminateContractRequest(
            TerminationGivenBy.LANDLORD,
            LocalDate.now(ZoneId.systemDefault()),
            Optional.empty(),
            Optional.of(earlier),
            Optional.of("Tenant agreed to leave early"),
            Optional.empty());

    var response = service.terminate(IDENTIFIER, request, principal);

    assertThat(response.effectiveEndDate()).isEqualTo(earlier);
  }

  private void stubGroundsRequired(List<String> codes) {
    when(ruleResolver.resolve(any(), any(), any()))
        .thenReturn(
            new TerminationRuleResolver.TerminationComputation(
                90, true, codes, TerminationRuleResolver.Source.CATALOG_RULE));
  }

  private TerminateContractRequest requestWithGround(Optional<String> groundCode) {
    return new TerminateContractRequest(
        TerminationGivenBy.LANDLORD,
        LocalDate.now(ZoneId.systemDefault()),
        groundCode,
        Optional.empty(),
        Optional.empty(),
        Optional.empty());
  }

  @Test
  @DisplayName("when the rule requires grounds, a missing or blank groundCode is rejected")
  void groundsRequiredButMissingRejected() {
    stubGroundsRequired(List.of("NON_PAYMENT", "OWN_USE"));

    assertThatThrownBy(
            () -> service.terminate(IDENTIFIER, requestWithGround(Optional.empty()), principal))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("groundCode");
    assertThatThrownBy(
            () -> service.terminate(IDENTIFIER, requestWithGround(Optional.of("  ")), principal))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("groundCode");
    verifyNoInteractions(contractService);
    org.mockito.Mockito.verify(terminationRepository, org.mockito.Mockito.never()).save(any());
  }

  @Test
  @DisplayName("when the rule requires grounds, a groundCode outside the allowed set is rejected")
  void groundsRequiredWithUnknownCodeRejected() {
    stubGroundsRequired(List.of("NON_PAYMENT", "OWN_USE"));

    assertThatThrownBy(
            () ->
                service.terminate(
                    IDENTIFIER, requestWithGround(Optional.of("I_FEEL_LIKE_IT")), principal))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("NON_PAYMENT");
    verifyNoInteractions(contractService);
  }

  @Test
  @DisplayName("when the rule requires grounds, an allowed groundCode is accepted and recorded")
  void groundsRequiredWithAllowedCodeAccepted() {
    stubGroundsRequired(List.of("NON_PAYMENT", "OWN_USE"));

    var response =
        service.terminate(IDENTIFIER, requestWithGround(Optional.of("OWN_USE")), principal);

    assertThat(response.groundCode()).contains("OWN_USE");
  }

  @Test
  @DisplayName("when grounds are required but the rule lists no codes, any non-blank code passes")
  void groundsRequiredWithNoCatalogAcceptsAnyCode() {
    stubGroundsRequired(List.of());

    var response =
        service.terminate(IDENTIFIER, requestWithGround(Optional.of("CUSTOM")), principal);

    assertThat(response.groundCode()).contains("CUSTOM");
  }

  @Test
  @DisplayName("the generated notice letter is persisted as a Document linked to the contract")
  void noticeLetterIsPersistedAsDocument() {
    var request =
        new TerminateContractRequest(
            TerminationGivenBy.LANDLORD,
            LocalDate.now(ZoneId.systemDefault()),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());

    service.terminate(IDENTIFIER, request, principal);

    ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
    verify(documentRepository).save(captor.capture());
    Document saved = captor.getValue();
    assertThat(saved.getEntityType()).isEqualTo("CONTRACT");
    assertThat(saved.getEntityId()).isEqualTo(contract.getId());
  }

  @Test
  @DisplayName("the deposit's return-due deadline is pushed to effective end date plus 30 days")
  void depositReturnDueDateIsSetThirtyDaysAfterEffectiveEndDate() {
    LocalDate noticeDate = LocalDate.now(ZoneId.systemDefault());
    var request =
        new TerminateContractRequest(
            TerminationGivenBy.LANDLORD,
            noticeDate,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());

    service.terminate(IDENTIFIER, request, principal);

    LocalDate expectedEffectiveEndDate = noticeDate.plusDays(90);
    verify(depositService)
        .updateReturnDueDate(IDENTIFIER, expectedEffectiveEndDate.plusDays(30), principal);
  }

  @Test
  @DisplayName(
      "sweepDueTerminations transitions a NOTICE_GIVEN termination past its effective end date to"
          + " TERMINATED, and transitions the underlying contract too")
  void sweepTransitionsDueTermination() {
    ContractTermination due =
        ContractTermination.builder()
            .id(UUID.randomUUID())
            .teamId(TEAM_ID)
            .contractId(CONTRACT_ID)
            .status(com.buurman.domain.ContractTerminationStatus.NOTICE_GIVEN)
            .effectiveEndDate(LocalDate.now(ZoneId.systemDefault()).minusDays(1))
            .build();
    when(terminationRepository.findDueForTransition(any())).thenReturn(List.of(due));
    when(contractRepository.getByIdAndTeamId(CONTRACT_ID, TEAM_ID)).thenReturn(contract);

    service.sweepDueTerminations();

    verify(terminationRepository)
        .save(
            org.mockito.ArgumentMatchers.argThat(
                t -> t.getStatus() == ContractTerminationStatus.TERMINATED));
    verify(contractService)
        .transitionStatus(
            org.mockito.ArgumentMatchers.eq(contract),
            org.mockito.ArgumentMatchers.eq(Contract.ContractStatus.TERMINATED),
            org.mockito.ArgumentMatchers.eq(Constants.SYSTEM_USER_ID),
            org.mockito.ArgumentMatchers.any());
  }

  @Test
  @DisplayName(
      "sweepDueTerminations reconciles the termination row without re-attempting an invalid"
          + " TERMINATED->TERMINATED transition when the contract was already terminated by"
          + " another path — a prior version of this code threw there, rolling back the"
          + " termination.save() in the same transaction and leaving it NOTICE_GIVEN forever")
  void sweepReconcilesAlreadyTerminatedContractWithoutRetransitioning() {
    ContractTermination due =
        ContractTermination.builder()
            .id(UUID.randomUUID())
            .teamId(TEAM_ID)
            .contractId(CONTRACT_ID)
            .status(ContractTerminationStatus.NOTICE_GIVEN)
            .effectiveEndDate(LocalDate.now(ZoneId.systemDefault()).minusDays(1))
            .build();
    Contract alreadyTerminatedContract =
        Contract.builder()
            .id(CONTRACT_ID)
            .teamId(TEAM_ID)
            .status(Contract.ContractStatus.TERMINATED)
            .build();
    when(terminationRepository.findDueForTransition(any())).thenReturn(List.of(due));
    when(contractRepository.getByIdAndTeamId(CONTRACT_ID, TEAM_ID))
        .thenReturn(alreadyTerminatedContract);

    service.sweepDueTerminations();

    verify(terminationRepository)
        .save(
            org.mockito.ArgumentMatchers.argThat(
                t -> t.getStatus() == ContractTerminationStatus.TERMINATED));
    verifyNoInteractions(contractService);
  }

  @Test
  @DisplayName(
      "sweepDueTerminations computes \"today\" from the injected Clock, not the system default"
          + " clock, so the NOTICE_GIVEN->TERMINATED boundary is testable and timezone-stable")
  void sweepUsesInjectedClockNotSystemClock() {
    when(terminationRepository.findDueForTransition(any())).thenReturn(List.of());

    service.sweepDueTerminations();

    verify(terminationRepository).findDueForTransition(LocalDate.of(2026, 3, 15));
  }

  @Test
  @DisplayName("sweepDueTerminations does nothing when no terminations are due")
  void sweepNoOpWhenNoneDue() {
    when(terminationRepository.findDueForTransition(any())).thenReturn(List.of());

    service.sweepDueTerminations(); // must not throw

    org.mockito.Mockito.verify(terminationRepository, org.mockito.Mockito.never()).save(any());
    org.mockito.Mockito.verifyNoInteractions(contractService);
  }

  @Test
  @DisplayName(
      "a termination that fails to sweep does not prevent a later termination in the same sweep"
          + " run from being fully processed — each runs in its own transactionTemplate call, not"
          + " one @Transactional spanning the whole sweep")
  void sweepIsolatesFailuresPerTermination() {
    UUID otherContractId = UUID.randomUUID();
    ContractTermination failing =
        ContractTermination.builder()
            .id(UUID.randomUUID())
            .teamId(TEAM_ID)
            .contractId(CONTRACT_ID)
            .status(ContractTerminationStatus.NOTICE_GIVEN)
            .effectiveEndDate(LocalDate.now(ZoneId.systemDefault()).minusDays(1))
            .build();
    ContractTermination succeeding =
        ContractTermination.builder()
            .id(UUID.randomUUID())
            .teamId(TEAM_ID)
            .contractId(otherContractId)
            .status(ContractTerminationStatus.NOTICE_GIVEN)
            .effectiveEndDate(LocalDate.now(ZoneId.systemDefault()).minusDays(1))
            .build();
    Contract otherContract = Contract.builder().id(otherContractId).teamId(TEAM_ID).build();
    when(terminationRepository.findDueForTransition(any()))
        .thenReturn(List.of(failing, succeeding));
    // Simulates transitionStatus (or any other @Transactional collaborator it calls) throwing —
    // the exact failure mode that marks an ambient transaction rollbackOnly in production.
    when(contractRepository.getByIdAndTeamId(CONTRACT_ID, TEAM_ID))
        .thenThrow(new RuntimeException("boom"));
    when(contractRepository.getByIdAndTeamId(otherContractId, TEAM_ID)).thenReturn(otherContract);

    service.sweepDueTerminations(); // must not throw despite the first termination failing

    verify(terminationRepository, org.mockito.Mockito.never())
        .save(org.mockito.ArgumentMatchers.argThat(t -> t.getContractId().equals(CONTRACT_ID)));
    verify(terminationRepository)
        .save(
            org.mockito.ArgumentMatchers.argThat(
                t ->
                    t.getContractId().equals(otherContractId)
                        && t.getStatus() == ContractTerminationStatus.TERMINATED));
    verify(contractService)
        .transitionStatus(
            org.mockito.ArgumentMatchers.eq(otherContract),
            org.mockito.ArgumentMatchers.eq(Contract.ContractStatus.TERMINATED),
            org.mockito.ArgumentMatchers.eq(Constants.SYSTEM_USER_ID),
            org.mockito.ArgumentMatchers.any());
    // Each termination went through its own transactionTemplate.executeWithoutResult call
    // (rather than a single ambient transaction), which is what makes this isolation possible.
    verify(transactionTemplate, org.mockito.Mockito.times(2)).executeWithoutResult(any());
  }

  @Test
  @DisplayName("previewTermination returns the resolver's computation without persisting anything")
  void previewTerminationHasNoSideEffects() {
    LocalDate noticeDate = LocalDate.now(ZoneId.systemDefault());
    clearInvocations(
        terminationRepository, contractService, letterExporter, depositService, documentRepository);

    ContractTerminationService.TerminationPreview preview =
        service.previewTermination(IDENTIFIER, TerminationGivenBy.LANDLORD, noticeDate, principal);

    assertThat(preview.computedEndDate()).isEqualTo(noticeDate.plusDays(90));
    assertThat(preview.noticeDays()).isEqualTo(90);
    assertThat(preview.groundsRequired()).isFalse();
    assertThat(preview.source()).isEqualTo(TerminationRuleResolver.Source.CATALOG_RULE);

    verifyNoInteractions(
        terminationRepository, contractService, letterExporter, depositService, documentRepository);
  }
}
