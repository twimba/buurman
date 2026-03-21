package com.buurman.service;

import static com.buurman.domain.Contract.ContractStatus.ACTIVE;
import static com.buurman.domain.Contract.ContractType.FIXED_TERM;
import static com.buurman.domain.Contract.ContractType.INDEFINITE;
import static com.buurman.domain.ContractExtension.ExtensionStatus.DECLINED;
import static com.buurman.domain.ContractExtension.ExtensionStatus.DRAFT;
import static com.buurman.domain.ContractExtension.RentAdjustmentType.FIXED_AMOUNT;
import static com.buurman.domain.ContractExtension.RentAdjustmentType.FIXED_PERCENTAGE;
import static com.buurman.domain.ContractExtension.RentAdjustmentType.NONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import com.buurman.config.models.AppProperties;
import com.buurman.domain.Contract;
import com.buurman.domain.ContractExtension;
import com.buurman.domain.ContractRentPeriod;
import com.buurman.domain.Property;
import com.buurman.domain.Sid;
import com.buurman.domain.identifier.ContractExtensionIdentifier;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.dto.request.CreateContractExtensionRequest;
import com.buurman.dto.request.DeclineContractExtensionRequest;
import com.buurman.dto.response.ContractExtensionResponse;
import com.buurman.exception.BadRequestException;
import com.buurman.exception.BusinessRuleException;
import com.buurman.repository.ContractExtensionRepository;
import com.buurman.repository.ContractRentPeriodRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.TeamRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.service.notification.NotificationService;
import com.buurman.util.MoneyAmount;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContractExtensionService")
class ContractExtensionServiceTest {

  @Mock private ContractExtensionRepository extensionRepository;
  @Mock private ContractRepository contractRepository;
  @Mock private ContractRentPeriodRepository rentPeriodRepository;
  @Mock private PropertyRepository propertyRepository;
  @Mock private TeamRepository teamRepository;
  @Mock private NotificationService notificationService;
  @Mock private AuditService auditService;
  @Mock private TransactionTemplate transactionTemplate;
  @Mock private AppProperties appProperties;

  private ContractExtensionService service;
  private UserPrincipal principal;

  private static final Clock FIXED_CLOCK =
      Clock.fixed(Instant.parse("2026-03-15T12:00:00Z"), ZoneId.of("UTC"));
  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID CONTRACT_ID = UUID.randomUUID();
  private static final ContractIdentifier CONTRACT_SID =
      ContractIdentifier.of("con_test12345678901234567");

  @BeforeEach
  void setUp() {
    service =
        new ContractExtensionService(
            extensionRepository,
            contractRepository,
            rentPeriodRepository,
            propertyRepository,
            teamRepository,
            notificationService,
            auditService,
            transactionTemplate,
            appProperties,
            FIXED_CLOCK);
    principal =
        new UserPrincipal(
            USER_ID,
            "usr_test",
            "kc-123",
            "test@example.com",
            "Test User",
            TEAM_ID,
            "team_test",
            com.buurman.domain.TeamRole.TEAM_ADMIN);
  }

  private Contract activeFixedTermContract() {
    return Contract.builder()
        .id(CONTRACT_ID)
        .identifier(Optional.of(Sid.of("con_test12345678901234567")))
        .teamId(TEAM_ID)
        .propertyId(UUID.randomUUID())
        .contractType(FIXED_TERM)
        .startDate(LocalDate.of(2025, 1, 1))
        .endDate(Optional.of(LocalDate.of(2026, 6, 30)))
        .rentAmount(MoneyAmount.of(new BigDecimal("1000.00"), "EUR"))
        .paymentFrequency(Contract.PaymentFrequency.MONTHLY)
        .status(ACTIVE)
        .renewalMode(Contract.RenewalMode.MANUAL)
        .renewalTermMonths(Optional.of(12))
        .rentAdjustmentType(NONE)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .createdBy(USER_ID)
        .updatedBy(USER_ID)
        .build();
  }

  @Nested
  @DisplayName("createExtension")
  class CreateExtension {

    @Test
    @DisplayName("creates DRAFT extension with computed new end date")
    void createsDraftExtension() {
      Contract contract = activeFixedTermContract();
      when(contractRepository.getByIdentifierAndTeamId(CONTRACT_SID, TEAM_ID))
          .thenReturn(contract);
      when(extensionRepository.findDraftByContractId(CONTRACT_ID, TEAM_ID))
          .thenReturn(Optional.empty());
      when(extensionRepository.findActiveByContractId(CONTRACT_ID, TEAM_ID))
          .thenReturn(Optional.empty());
      when(extensionRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
          .thenReturn(List.of());
      when(extensionRepository.getNextExtensionNumber(CONTRACT_ID, TEAM_ID)).thenReturn(1);
      when(extensionRepository.save(any(ContractExtension.class)))
          .thenAnswer(inv -> {
            ContractExtension ext = inv.getArgument(0);
            ext.setId(UUID.randomUUID());
            ext.setCreatedAt(Instant.now());
            return ext;
          });

      CreateContractExtensionRequest request =
          new CreateContractExtensionRequest(
              Optional.empty(), Optional.empty(), Optional.empty(),
              Optional.empty(), Optional.of("Annual renewal"));

      ContractExtensionResponse response =
          service.createExtension(CONTRACT_SID, request, principal);

      assertThat(response.status()).isEqualTo(DRAFT);
      // renewalTermMonths = 12, so new end date = 2026-06-30 + 12 months = 2027-06-30
      assertThat(response.newEndDate()).contains(LocalDate.of(2027, 6, 30));
      assertThat(response.previousEndDate()).isEqualTo(LocalDate.of(2026, 6, 30));
    }

    @Test
    @DisplayName("rejects extension for non-ACTIVE contract")
    void rejectsNonActive() {
      Contract contract = activeFixedTermContract();
      contract.setStatus(Contract.ContractStatus.DRAFT);
      when(contractRepository.getByIdentifierAndTeamId(CONTRACT_SID, TEAM_ID))
          .thenReturn(contract);

      CreateContractExtensionRequest request =
          new CreateContractExtensionRequest(
              Optional.empty(), Optional.empty(), Optional.empty(),
              Optional.empty(), Optional.empty());

      assertThatThrownBy(() -> service.createExtension(CONTRACT_SID, request, principal))
          .isInstanceOf(BusinessRuleException.class)
          .hasMessageContaining("active contracts");
    }

    @Test
    @DisplayName("rejects extension for INDEFINITE contract")
    void rejectsIndefinite() {
      Contract contract = activeFixedTermContract();
      contract.setContractType(INDEFINITE);
      when(contractRepository.getByIdentifierAndTeamId(CONTRACT_SID, TEAM_ID))
          .thenReturn(contract);

      CreateContractExtensionRequest request =
          new CreateContractExtensionRequest(
              Optional.empty(), Optional.empty(), Optional.empty(),
              Optional.empty(), Optional.empty());

      assertThatThrownBy(() -> service.createExtension(CONTRACT_SID, request, principal))
          .isInstanceOf(BusinessRuleException.class)
          .hasMessageContaining("fixed-term contracts");
    }

    @Test
    @DisplayName("rejects when draft extension already exists")
    void rejectsDuplicateDraft() {
      Contract contract = activeFixedTermContract();
      when(contractRepository.getByIdentifierAndTeamId(CONTRACT_SID, TEAM_ID))
          .thenReturn(contract);
      when(extensionRepository.findDraftByContractId(CONTRACT_ID, TEAM_ID))
          .thenReturn(Optional.of(ContractExtension.builder().build()));

      CreateContractExtensionRequest request =
          new CreateContractExtensionRequest(
              Optional.empty(), Optional.empty(), Optional.empty(),
              Optional.empty(), Optional.empty());

      assertThatThrownBy(() -> service.createExtension(CONTRACT_SID, request, principal))
          .isInstanceOf(BusinessRuleException.class)
          .hasMessageContaining("pending draft");
    }

    @Test
    @DisplayName("rejects when new end date is before current end date")
    void rejectsEarlierEndDate() {
      Contract contract = activeFixedTermContract();
      when(contractRepository.getByIdentifierAndTeamId(CONTRACT_SID, TEAM_ID))
          .thenReturn(contract);
      when(extensionRepository.findDraftByContractId(CONTRACT_ID, TEAM_ID))
          .thenReturn(Optional.empty());
      when(extensionRepository.findActiveByContractId(CONTRACT_ID, TEAM_ID))
          .thenReturn(Optional.empty());
      when(extensionRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
          .thenReturn(List.of());

      CreateContractExtensionRequest request =
          new CreateContractExtensionRequest(
              Optional.of(LocalDate.of(2025, 1, 1)),
              Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());

      assertThatThrownBy(() -> service.createExtension(CONTRACT_SID, request, principal))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("after the current end date");
    }

    @Test
    @DisplayName("rejects when max renewals reached")
    void rejectsMaxRenewals() {
      Contract contract = activeFixedTermContract();
      contract.setMaxRenewals(Optional.of(2));
      when(contractRepository.getByIdentifierAndTeamId(CONTRACT_SID, TEAM_ID))
          .thenReturn(contract);
      when(extensionRepository.findDraftByContractId(CONTRACT_ID, TEAM_ID))
          .thenReturn(Optional.empty());
      when(extensionRepository.findActiveByContractId(CONTRACT_ID, TEAM_ID))
          .thenReturn(Optional.empty());
      when(extensionRepository.countActiveAndSuperseded(CONTRACT_ID, TEAM_ID)).thenReturn(2);

      CreateContractExtensionRequest request =
          new CreateContractExtensionRequest(
              Optional.empty(), Optional.empty(), Optional.empty(),
              Optional.empty(), Optional.empty());

      assertThatThrownBy(() -> service.createExtension(CONTRACT_SID, request, principal))
          .isInstanceOf(BusinessRuleException.class)
          .hasMessageContaining("Maximum number of renewals");
    }
  }

  @Nested
  @DisplayName("declineExtension")
  class DeclineExtension {

    @Test
    @DisplayName("transitions DRAFT to DECLINED with reason")
    void declinesDraftExtension() {
      Contract contract = activeFixedTermContract();
      ContractExtension extension =
          ContractExtension.builder()
              .id(UUID.randomUUID())
              .identifier(Optional.of(Sid.of("cex_test12345678901234567")))
              .teamId(TEAM_ID)
              .contractId(CONTRACT_ID)
              .extensionNumber(1)
              .previousEndDate(LocalDate.of(2026, 6, 30))
              .newEndDate(Optional.of(LocalDate.of(2027, 6, 30)))
              .previousRentAmount(MoneyAmount.of(new BigDecimal("1000.00"), "EUR"))
              .newRentAmount(MoneyAmount.of(new BigDecimal("1000.00"), "EUR"))
              .rentAdjustmentType(NONE)
              .status(DRAFT)
              .triggerType(ContractExtension.TriggerType.MANUAL)
              .createdAt(Instant.now())
              .updatedAt(Instant.now())
              .createdBy(USER_ID)
              .updatedBy(USER_ID)
              .build();

      ContractExtensionIdentifier extSid =
          ContractExtensionIdentifier.of("cex_test12345678901234567");
      when(contractRepository.getByIdentifierAndTeamId(CONTRACT_SID, TEAM_ID))
          .thenReturn(contract);
      when(extensionRepository.getByIdentifierAndTeamId(extSid, TEAM_ID)).thenReturn(extension);
      when(extensionRepository.save(any(ContractExtension.class)))
          .thenAnswer(inv -> inv.getArgument(0));

      DeclineContractExtensionRequest request =
          new DeclineContractExtensionRequest(Optional.of("Tenant disagreed"));

      ContractExtensionResponse response =
          service.declineExtension(CONTRACT_SID, extSid, request, principal);

      assertThat(response.status()).isEqualTo(DECLINED);
    }

    @Test
    @DisplayName("rejects declining non-DRAFT extension")
    void rejectsDeclineOfNonDraft() {
      Contract contract = activeFixedTermContract();
      ContractExtension extension =
          ContractExtension.builder()
              .id(UUID.randomUUID())
              .identifier(Optional.of(Sid.of("cex_test12345678901234567")))
              .teamId(TEAM_ID)
              .contractId(CONTRACT_ID)
              .extensionNumber(1)
              .previousEndDate(LocalDate.of(2026, 6, 30))
              .newEndDate(Optional.of(LocalDate.of(2027, 6, 30)))
              .previousRentAmount(MoneyAmount.of(new BigDecimal("1000.00"), "EUR"))
              .newRentAmount(MoneyAmount.of(new BigDecimal("1000.00"), "EUR"))
              .rentAdjustmentType(NONE)
              .status(ContractExtension.ExtensionStatus.ACTIVE)
              .triggerType(ContractExtension.TriggerType.MANUAL)
              .createdAt(Instant.now())
              .updatedAt(Instant.now())
              .createdBy(USER_ID)
              .updatedBy(USER_ID)
              .build();

      ContractExtensionIdentifier extSid =
          ContractExtensionIdentifier.of("cex_test12345678901234567");
      when(contractRepository.getByIdentifierAndTeamId(CONTRACT_SID, TEAM_ID))
          .thenReturn(contract);
      when(extensionRepository.getByIdentifierAndTeamId(extSid, TEAM_ID)).thenReturn(extension);

      DeclineContractExtensionRequest request =
          new DeclineContractExtensionRequest(Optional.of("reason"));

      assertThatThrownBy(
              () -> service.declineExtension(CONTRACT_SID, extSid, request, principal))
          .isInstanceOf(BusinessRuleException.class)
          .hasMessageContaining("Cannot decline");
    }
  }

  @Nested
  @DisplayName("computeNewRent")
  class ComputeNewRent {

    @Test
    @DisplayName("FIXED_PERCENTAGE with 5% adjusts rent correctly")
    void fixedPercentageAdjustsRent() {
      Contract contract = activeFixedTermContract();
      contract.setRentAdjustmentType(FIXED_PERCENTAGE);
      contract.setRentAdjustmentValue(Optional.of(new BigDecimal("5")));

      when(contractRepository.getByIdentifierAndTeamId(CONTRACT_SID, TEAM_ID))
          .thenReturn(contract);
      when(extensionRepository.findDraftByContractId(CONTRACT_ID, TEAM_ID))
          .thenReturn(Optional.empty());
      when(extensionRepository.findActiveByContractId(CONTRACT_ID, TEAM_ID))
          .thenReturn(Optional.empty());
      when(extensionRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
          .thenReturn(List.of());
      when(extensionRepository.getNextExtensionNumber(CONTRACT_ID, TEAM_ID)).thenReturn(1);
      when(extensionRepository.save(any(ContractExtension.class)))
          .thenAnswer(inv -> {
            ContractExtension ext = inv.getArgument(0);
            ext.setId(UUID.randomUUID());
            ext.setCreatedAt(Instant.now());
            return ext;
          });

      CreateContractExtensionRequest request =
          new CreateContractExtensionRequest(
              Optional.empty(), Optional.empty(), Optional.empty(),
              Optional.empty(), Optional.empty());

      ContractExtensionResponse response =
          service.createExtension(CONTRACT_SID, request, principal);

      // 1000 * 1.05 = 1050.00
      assertThat(response.newRentAmount()).isEqualByComparingTo("1050.00");
    }

    @Test
    @DisplayName("FIXED_AMOUNT with 50 EUR adds to rent correctly")
    void fixedAmountAdjustsRent() {
      Contract contract = activeFixedTermContract();
      contract.setRentAdjustmentType(FIXED_AMOUNT);
      contract.setRentAdjustmentValue(Optional.of(new BigDecimal("50")));

      when(contractRepository.getByIdentifierAndTeamId(CONTRACT_SID, TEAM_ID))
          .thenReturn(contract);
      when(extensionRepository.findDraftByContractId(CONTRACT_ID, TEAM_ID))
          .thenReturn(Optional.empty());
      when(extensionRepository.findActiveByContractId(CONTRACT_ID, TEAM_ID))
          .thenReturn(Optional.empty());
      when(extensionRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
          .thenReturn(List.of());
      when(extensionRepository.getNextExtensionNumber(CONTRACT_ID, TEAM_ID)).thenReturn(1);
      when(extensionRepository.save(any(ContractExtension.class)))
          .thenAnswer(
              inv -> {
                ContractExtension ext = inv.getArgument(0);
                ext.setId(UUID.randomUUID());
                ext.setCreatedAt(Instant.now());
                return ext;
              });

      CreateContractExtensionRequest request =
          new CreateContractExtensionRequest(
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty());

      ContractExtensionResponse response =
          service.createExtension(CONTRACT_SID, request, principal);

      // 1000 + 50 = 1050.00
      assertThat(response.newRentAmount()).isEqualByComparingTo("1050.00");
    }

    @Test
    @DisplayName("MANUAL rent adjustment uses explicit newRentAmount")
    void manualRentAdjustmentUsesExplicitAmount() {
      Contract contract = activeFixedTermContract();

      when(contractRepository.getByIdentifierAndTeamId(CONTRACT_SID, TEAM_ID))
          .thenReturn(contract);
      when(extensionRepository.findDraftByContractId(CONTRACT_ID, TEAM_ID))
          .thenReturn(Optional.empty());
      when(extensionRepository.findActiveByContractId(CONTRACT_ID, TEAM_ID))
          .thenReturn(Optional.empty());
      when(extensionRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
          .thenReturn(List.of());
      when(extensionRepository.getNextExtensionNumber(CONTRACT_ID, TEAM_ID)).thenReturn(1);
      when(extensionRepository.save(any(ContractExtension.class)))
          .thenAnswer(
              inv -> {
                ContractExtension ext = inv.getArgument(0);
                ext.setId(UUID.randomUUID());
                ext.setCreatedAt(Instant.now());
                return ext;
              });

      CreateContractExtensionRequest request =
          new CreateContractExtensionRequest(
              Optional.empty(),
              Optional.of(new BigDecimal("1500.00")),
              Optional.empty(),
              Optional.empty(),
              Optional.empty());

      ContractExtensionResponse response =
          service.createExtension(CONTRACT_SID, request, principal);

      assertThat(response.newRentAmount()).isEqualByComparingTo("1500.00");
    }
  }

  @Nested
  @DisplayName("activateExtension")
  class ActivateExtension {

    private ContractExtension draftExtension() {
      return ContractExtension.builder()
          .id(UUID.randomUUID())
          .identifier(Optional.of(Sid.of("cex_test12345678901234567")))
          .teamId(TEAM_ID)
          .contractId(CONTRACT_ID)
          .extensionNumber(1)
          .previousEndDate(LocalDate.of(2026, 6, 30))
          .newEndDate(Optional.of(LocalDate.of(2027, 6, 30)))
          .previousRentAmount(MoneyAmount.of(new BigDecimal("1000.00"), "EUR"))
          .newRentAmount(MoneyAmount.of(new BigDecimal("1050.00"), "EUR"))
          .rentAdjustmentType(FIXED_PERCENTAGE)
          .rentAdjustmentValue(Optional.of(new BigDecimal("5")))
          .status(DRAFT)
          .triggerType(ContractExtension.TriggerType.MANUAL)
          .createdAt(Instant.now())
          .updatedAt(Instant.now())
          .createdBy(USER_ID)
          .updatedBy(USER_ID)
          .build();
    }

    @Test
    @DisplayName("happy path: activates DRAFT extension, supersedes previous, creates rent period")
    void activatesDraftExtension() {
      Contract contract = activeFixedTermContract();
      ContractExtension extension = draftExtension();
      ContractExtensionIdentifier extSid =
          ContractExtensionIdentifier.of("cex_test12345678901234567");

      when(contractRepository.getByIdentifierAndTeamId(CONTRACT_SID, TEAM_ID))
          .thenReturn(contract);
      when(extensionRepository.getByIdentifierAndTeamId(extSid, TEAM_ID))
          .thenReturn(extension);
      // No currently active extension
      when(extensionRepository.findActiveByContractId(CONTRACT_ID, TEAM_ID))
          .thenReturn(Optional.empty());
      when(rentPeriodRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
          .thenReturn(List.of());
      when(rentPeriodRepository.save(any(ContractRentPeriod.class)))
          .thenAnswer(
              inv -> {
                ContractRentPeriod p = inv.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
              });
      when(extensionRepository.save(any(ContractExtension.class)))
          .thenAnswer(inv -> inv.getArgument(0));
      when(propertyRepository.getByIdAndTeamId(any(UUID.class), eq(TEAM_ID)))
          .thenReturn(
              Property.builder()
                  .street("Test Street")
                  .city("Amsterdam")
                  .build());

      ContractExtensionResponse response =
          service.activateExtension(CONTRACT_SID, extSid, principal);

      assertThat(response.status())
          .isEqualTo(ContractExtension.ExtensionStatus.ACTIVE);
      // Verify audit was logged
      verify(auditService)
          .logUpdate(
              eq(TEAM_ID),
              eq("CONTRACT_EXTENSION"),
              any(UUID.class),
              eq(USER_ID),
              any(),
              any(),
              any());
    }

    @Test
    @DisplayName("rejects activating non-DRAFT extension")
    void rejectsActivatingNonDraft() {
      Contract contract = activeFixedTermContract();
      ContractExtension extension = draftExtension();
      extension.setStatus(ContractExtension.ExtensionStatus.ACTIVE);
      ContractExtensionIdentifier extSid =
          ContractExtensionIdentifier.of("cex_test12345678901234567");

      when(contractRepository.getByIdentifierAndTeamId(CONTRACT_SID, TEAM_ID))
          .thenReturn(contract);
      when(extensionRepository.getByIdentifierAndTeamId(extSid, TEAM_ID))
          .thenReturn(extension);

      assertThatThrownBy(
              () -> service.activateExtension(CONTRACT_SID, extSid, principal))
          .isInstanceOf(BusinessRuleException.class)
          .hasMessageContaining("Cannot activate");
    }
  }

  @Nested
  @DisplayName("cancelExtension")
  class CancelExtension {

    @Test
    @DisplayName("happy path: cancels DRAFT extension")
    void cancelsDraftExtension() {
      Contract contract = activeFixedTermContract();
      UUID extensionId = UUID.randomUUID();
      ContractExtension extension =
          ContractExtension.builder()
              .id(extensionId)
              .identifier(Optional.of(Sid.of("cex_test12345678901234567")))
              .teamId(TEAM_ID)
              .contractId(CONTRACT_ID)
              .extensionNumber(1)
              .previousEndDate(LocalDate.of(2026, 6, 30))
              .newEndDate(Optional.of(LocalDate.of(2027, 6, 30)))
              .previousRentAmount(MoneyAmount.of(new BigDecimal("1000.00"), "EUR"))
              .newRentAmount(MoneyAmount.of(new BigDecimal("1000.00"), "EUR"))
              .rentAdjustmentType(NONE)
              .status(DRAFT)
              .triggerType(ContractExtension.TriggerType.MANUAL)
              .createdAt(Instant.now())
              .updatedAt(Instant.now())
              .createdBy(USER_ID)
              .updatedBy(USER_ID)
              .build();

      ContractExtensionIdentifier extSid =
          ContractExtensionIdentifier.of("cex_test12345678901234567");
      when(contractRepository.getByIdentifierAndTeamId(CONTRACT_SID, TEAM_ID))
          .thenReturn(contract);
      when(extensionRepository.getByIdentifierAndTeamId(extSid, TEAM_ID))
          .thenReturn(extension);

      service.cancelExtension(CONTRACT_SID, extSid, principal);

      verify(extensionRepository).cancelByIdAndTeamId(extensionId, TEAM_ID, USER_ID);
      verify(auditService)
          .logDelete(
              eq(TEAM_ID), eq("CONTRACT_EXTENSION"), eq(extensionId), eq(USER_ID), any());
    }
  }
}
