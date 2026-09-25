package com.buurman.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
import com.buurman.domain.Deposit;
import com.buurman.domain.Deposit.DepositStatus;
import com.buurman.domain.DepositDeduction;
import com.buurman.domain.Sid;
import com.buurman.domain.TeamRole;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.dto.request.CreateDepositDeductionRequest;
import com.buurman.dto.request.ReturnDepositRequest;
import com.buurman.dto.request.UpsertDepositRequest;
import com.buurman.exception.BusinessRuleException;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.DepositRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.util.MoneyAmount;

@ExtendWith(MockitoExtension.class)
class DepositServiceTest {

  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID CONTRACT_ID = UUID.randomUUID();
  private static final ContractIdentifier CONTRACT_SID =
      ContractIdentifier.of("con_01JTEST000000000000000001");
  private static final LocalDate TODAY = LocalDate.of(2026, 3, 1);

  @Mock private DepositRepository depositRepository;
  @Mock private ContractRepository contractRepository;
  @Mock private ContractPartyService contractPartyService;
  @Mock private AuditService auditService;
  @Mock private MetricsService metricsService;

  private final Clock clock =
      Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
  private DepositService service;
  private UserPrincipal principal;
  private Contract contract;

  @BeforeEach
  void setUp() {
    service =
        new DepositService(
            depositRepository,
            contractRepository,
            contractPartyService,
            auditService,
            metricsService,
            clock);
    principal =
        new UserPrincipal(
            USER_ID,
            "usr_test",
            "kc-id",
            "t@example.com",
            "Test",
            TEAM_ID,
            "team_test",
            TeamRole.TEAM_ADMIN,
            true);
    contract =
        Contract.builder()
            .id(CONTRACT_ID)
            .identifier(Optional.of(CONTRACT_SID))
            .teamId(TEAM_ID)
            .propertyId(UUID.randomUUID())
            .rentAmount(MoneyAmount.of(new BigDecimal("1000.00"), "EUR"))
            .build();
    org.mockito.Mockito.lenient()
        .when(contractRepository.getByIdentifierAndTeamId(CONTRACT_SID, TEAM_ID))
        .thenReturn(contract);
    org.mockito.Mockito.lenient()
        .when(depositRepository.save(any(Deposit.class)))
        .thenAnswer(
            inv -> {
              Deposit d = inv.getArgument(0);
              if (d.getId() == null) {
                d.setId(UUID.randomUUID());
              }
              return d;
            });
  }

  private Deposit held(String amount) {
    return Deposit.builder()
        .id(UUID.randomUUID())
        .identifier(Optional.of(Sid.of("dep_01JTEST000000000000000001")))
        .teamId(TEAM_ID)
        .contractId(CONTRACT_ID)
        .amount(MoneyAmount.of(new BigDecimal(amount), "EUR"))
        .status(DepositStatus.HELD)
        .receivedDate(Optional.of(TODAY.minusYears(1)))
        .build();
  }

  @Test
  @DisplayName("first upsert creates an EXPECTED deposit, a received date moves it to HELD")
  void upsertCreates() {
    when(depositRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
        .thenReturn(Optional.empty());
    when(depositRepository.findDeductions(any(), any())).thenReturn(List.of());
    when(contractPartyService.findPrimaryContactForContract(CONTRACT_ID, TEAM_ID))
        .thenReturn(Optional.empty());

    var response =
        service.upsert(
            CONTRACT_SID,
            new UpsertDepositRequest(
                new BigDecimal("2000.00"),
                Optional.of(TODAY),
                Optional.of("Escrow account"),
                Optional.empty(),
                Optional.empty()),
            principal);

    assertThat(response.status()).isEqualTo(DepositStatus.HELD);
    assertThat(response.amount()).isEqualByComparingTo("2000.00");
    assertThat(response.refundable()).isEqualByComparingTo("2000.00");
    assertThat(response.heldWhere()).contains("Escrow account");
  }

  @Test
  @DisplayName("deductions reduce the refundable amount and cannot exceed it")
  void deductions() {
    Deposit deposit = held("2000.00");
    when(depositRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
        .thenReturn(Optional.of(deposit));
    DepositDeduction existing =
        DepositDeduction.builder()
            .depositId(deposit.getId())
            .teamId(TEAM_ID)
            .amount(MoneyAmount.of(new BigDecimal("300.00"), "EUR"))
            .reason("Paint")
            .deductionDate(TODAY)
            .identifier(Optional.of(Sid.of("ddd_01JTEST000000000000000001")))
            .build();
    when(depositRepository.findDeductions(deposit.getId(), TEAM_ID)).thenReturn(List.of(existing));

    assertThatThrownBy(
            () ->
                service.addDeduction(
                    CONTRACT_SID,
                    new CreateDepositDeductionRequest(
                        new BigDecimal("1800.00"), "Everything", Optional.empty()),
                    principal))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("exceeds the refundable amount");

    var response =
        service.addDeduction(
            CONTRACT_SID,
            new CreateDepositDeductionRequest(
                new BigDecimal("200.00"), "Cleaning", Optional.empty()),
            principal);
    assertThat(response.deductionsTotal()).isEqualByComparingTo("300.00");
    assertThat(response.refundable()).isEqualByComparingTo("1700.00");
  }

  @Test
  @DisplayName("returning the full refundable amount closes the deposit; less is a partial return")
  void returns() {
    Deposit deposit = held("2000.00");
    when(depositRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
        .thenReturn(Optional.of(deposit));
    when(depositRepository.findDeductions(deposit.getId(), TEAM_ID)).thenReturn(List.of());

    var partial =
        service.returnDeposit(
            CONTRACT_SID,
            new ReturnDepositRequest(
                TODAY, Optional.of(new BigDecimal("500.00")), Optional.empty()),
            principal);
    assertThat(partial.status()).isEqualTo(DepositStatus.PARTIALLY_RETURNED);
    assertThat(partial.returnedAmount()).isEqualByComparingTo("500.00");
    assertThat(partial.refundable()).isEqualByComparingTo("1500.00");

    var full =
        service.returnDeposit(
            CONTRACT_SID,
            new ReturnDepositRequest(TODAY, Optional.empty(), Optional.of("Bank transfer")),
            principal);
    assertThat(full.status()).isEqualTo(DepositStatus.RETURNED);
    assertThat(full.returnedAmount()).isEqualByComparingTo("2000.00");
    assertThat(full.refundable()).isEqualByComparingTo("0");
  }

  @Test
  @DisplayName("an EXPECTED deposit cannot be returned")
  void expectedCannotBeReturned() {
    Deposit expected = held("1000.00");
    expected.setStatus(DepositStatus.EXPECTED);
    when(depositRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
        .thenReturn(Optional.of(expected));
    assertThatThrownBy(
            () ->
                service.returnDeposit(
                    CONTRACT_SID,
                    new ReturnDepositRequest(TODAY, Optional.empty(), Optional.empty()),
                    principal))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("never received");
  }
}
