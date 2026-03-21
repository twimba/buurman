package com.buurman.service;

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

import com.buurman.config.models.AppProperties;
import com.buurman.domain.Contract;
import com.buurman.domain.ContractRentPeriod;
import com.buurman.domain.Sid;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.dto.request.CreateRentPeriodRequest;
import com.buurman.exception.BusinessRuleException;
import com.buurman.mapper.ContractRentPeriodMapper;
import com.buurman.repository.ContractExtensionRepository;
import com.buurman.repository.ContractRentPeriodRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PaymentRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.service.notification.NotificationService;
import com.buurman.util.MoneyAmount;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContractRentPeriodService")
class ContractRentPeriodServiceTest {

  @Mock private ContractRentPeriodRepository rentPeriodRepository;
  @Mock private ContractRepository contractRepository;
  @Mock private ContractExtensionRepository contractExtensionRepository;
  @Mock private PaymentRepository paymentRepository;
  @Mock private PropertyRepository propertyRepository;
  @Mock private ContractRentPeriodMapper rentPeriodMapper;
  @Mock private AuditService auditService;
  @Mock private NotificationService notificationService;
  @Mock private ContractPartyService contractPartyService;
  @Mock private AppProperties appProperties;

  private ContractRentPeriodService service;
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
        new ContractRentPeriodService(
            rentPeriodRepository,
            contractRepository,
            contractExtensionRepository,
            paymentRepository,
            propertyRepository,
            rentPeriodMapper,
            auditService,
            notificationService,
            contractPartyService,
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

  private Contract activeContract() {
    return Contract.builder()
        .id(CONTRACT_ID)
        .identifier(Optional.of(Sid.of("con_test12345678901234567")))
        .teamId(TEAM_ID)
        .propertyId(UUID.randomUUID())
        .contractType(Contract.ContractType.FIXED_TERM)
        .startDate(LocalDate.of(2026, 1, 1))
        .endDate(Optional.of(LocalDate.of(2027, 1, 1)))
        .rentAmount(MoneyAmount.of(new BigDecimal("1000.00"), "EUR"))
        .paymentFrequency(Contract.PaymentFrequency.MONTHLY)
        .status(Contract.ContractStatus.ACTIVE)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .createdBy(USER_ID)
        .updatedBy(USER_ID)
        .build();
  }

  @Nested
  @DisplayName("createInitialRentPeriod")
  class InitialRentPeriod {

    @Test
    @DisplayName("creates rent period matching contract start date and rent amount")
    void createsInitialPeriod() {
      Contract contract = activeContract();
      when(rentPeriodRepository.save(any(ContractRentPeriod.class)))
          .thenAnswer(inv -> inv.getArgument(0));

      service.createInitialRentPeriod(contract, principal);

      ArgumentCaptor<ContractRentPeriod> captor =
          ArgumentCaptor.forClass(ContractRentPeriod.class);
      verify(rentPeriodRepository).save(captor.capture());
      ContractRentPeriod saved = captor.getValue();
      assertThat(saved.getEffectiveFrom()).isEqualTo(contract.getStartDate());
      assertThat(saved.getRentAmount().value()).isEqualByComparingTo("1000.00");
      assertThat(saved.getContractId()).isEqualTo(contract.getId());
    }
  }

  @Nested
  @DisplayName("addRentPeriod validation")
  class AddRentPeriodValidation {

    @Test
    @DisplayName("rejects effective date before contract start date")
    void rejectsDateBeforeContractStart() {
      when(contractRepository.getByIdentifierAndTeamId(CONTRACT_SID, TEAM_ID))
          .thenReturn(activeContract());

      CreateRentPeriodRequest request =
          new CreateRentPeriodRequest(
              new BigDecimal("1200.00"),
              LocalDate.of(2025, 12, 1),
              Optional.empty());

      assertThatThrownBy(() -> service.addRentPeriod(CONTRACT_SID, request, principal))
          .isInstanceOf(BusinessRuleException.class)
          .hasMessageContaining("before the contract start date");
    }

    @Test
    @DisplayName("rejects duplicate effective date")
    void rejectsDuplicateEffectiveDate() {
      Contract contract = activeContract();
      when(contractRepository.getByIdentifierAndTeamId(CONTRACT_SID, TEAM_ID))
          .thenReturn(contract);
      when(contractExtensionRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
          .thenReturn(List.of());
      when(rentPeriodRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
          .thenReturn(
              List.of(
                  ContractRentPeriod.builder()
                      .id(UUID.randomUUID())
                      .teamId(TEAM_ID)
                      .contractId(CONTRACT_ID)
                      .rentAmount(MoneyAmount.of(new BigDecimal("1000.00"), "EUR"))
                      .effectiveFrom(LocalDate.of(2026, 6, 1))
                      .createdBy(USER_ID)
                      .updatedBy(USER_ID)
                      .build()));

      CreateRentPeriodRequest request =
          new CreateRentPeriodRequest(
              new BigDecimal("1200.00"),
              LocalDate.of(2026, 6, 1),
              Optional.empty());

      assertThatThrownBy(() -> service.addRentPeriod(CONTRACT_SID, request, principal))
          .isInstanceOf(BusinessRuleException.class)
          .hasMessageContaining("already exists");
    }
  }

  @Nested
  @DisplayName("updateInitialRentPeriod")
  class UpdateInitialRentPeriod {

    @Test
    @DisplayName("updates single rent period when contract rent changes")
    void updatesSinglePeriod() {
      Contract contract = activeContract();
      contract.setRentAmount(MoneyAmount.of(new BigDecimal("1100.00"), "EUR"));
      contract.setStartDate(LocalDate.of(2026, 2, 1));

      ContractRentPeriod existing =
          ContractRentPeriod.builder()
              .id(UUID.randomUUID())
              .teamId(TEAM_ID)
              .contractId(CONTRACT_ID)
              .rentAmount(MoneyAmount.of(new BigDecimal("1000.00"), "EUR"))
              .effectiveFrom(LocalDate.of(2026, 1, 1))
              .createdBy(USER_ID)
              .updatedBy(USER_ID)
              .build();
      when(rentPeriodRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
          .thenReturn(List.of(existing));
      when(rentPeriodRepository.save(any(ContractRentPeriod.class)))
          .thenAnswer(inv -> inv.getArgument(0));

      service.updateInitialRentPeriod(contract, principal);

      ArgumentCaptor<ContractRentPeriod> captor =
          ArgumentCaptor.forClass(ContractRentPeriod.class);
      verify(rentPeriodRepository).save(captor.capture());
      assertThat(captor.getValue().getRentAmount().value()).isEqualByComparingTo("1100.00");
      assertThat(captor.getValue().getEffectiveFrom()).isEqualTo(LocalDate.of(2026, 2, 1));
    }
  }

  @Nested
  @DisplayName("getCurrentRent and getRentAtDate")
  class RentLookups {

    @Test
    @DisplayName("getCurrentRent delegates to repository")
    void getCurrentRentDelegatesToRepo() {
      ContractRentPeriod period =
          ContractRentPeriod.builder()
              .rentAmount(MoneyAmount.of(new BigDecimal("1200.00"), "EUR"))
              .effectiveFrom(LocalDate.of(2026, 3, 1))
              .build();
      when(rentPeriodRepository.findCurrentByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
          .thenReturn(Optional.of(period));

      Optional<ContractRentPeriod> result = service.getCurrentRent(CONTRACT_ID, TEAM_ID);

      assertThat(result).isPresent();
      assertThat(result.get().getRentAmount().value()).isEqualByComparingTo("1200.00");
    }

    @Test
    @DisplayName("getRentAtDate delegates to repository with date")
    void getRentAtDateDelegatesToRepo() {
      LocalDate queryDate = LocalDate.of(2026, 6, 15);
      when(rentPeriodRepository.findAtDateByContractIdAndTeamId(CONTRACT_ID, TEAM_ID, queryDate))
          .thenReturn(Optional.empty());

      Optional<ContractRentPeriod> result =
          service.getRentAtDate(CONTRACT_ID, TEAM_ID, queryDate);

      assertThat(result).isEmpty();
      verify(rentPeriodRepository)
          .findAtDateByContractIdAndTeamId(CONTRACT_ID, TEAM_ID, queryDate);
    }
  }
}
