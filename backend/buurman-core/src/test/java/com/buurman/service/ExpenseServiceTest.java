package com.buurman.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buurman.config.models.AppProperties;
import com.buurman.domain.Expense;
import com.buurman.domain.TeamRole;
import com.buurman.domain.identifier.ExpenseIdentifier;
import com.buurman.dto.request.UpdateExpenseRequest;
import com.buurman.mapper.ContactMapper;
import com.buurman.mapper.DocumentMapper;
import com.buurman.mapper.ExpenseMapper;
import com.buurman.mapper.ExpenseMapperImpl;
import com.buurman.mapper.OptionalMappingConfig;
import com.buurman.mapper.PropertyMapper;
import com.buurman.repository.AuditLogRepository;
import com.buurman.repository.ContactRepository;
import com.buurman.repository.DocumentRepository;
import com.buurman.repository.ExpenseRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.UnitRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.service.notification.NotificationService;
import com.buurman.util.MoneyAmount;
import com.buurman.util.SidGenerator;

import jakarta.validation.Validator;

/**
 * Uses the real (MapStruct-generated) {@link ExpenseMapperImpl} rather than mocking {@link
 * ExpenseMapper}: {@code updateEntity} is what actually applies an {@link UpdateExpenseRequest}
 * onto the {@link Expense}, and that mutation is exactly what these tests need to be faithful —
 * mocking it would make the amount-changed/unchanged distinction meaningless.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExpenseService.updateExpense — allocation re-run gating (BUUR-106 Critical 2)")
class ExpenseServiceTest {

  @Mock private ExpenseRepository expenseRepository;
  @Mock private PropertyRepository propertyRepository;
  @Mock private UnitRepository unitRepository;
  @Mock private ContactRepository contactRepository;
  @Mock private DocumentRepository documentRepository;
  @Mock private ExpenseAllocationService expenseAllocationService;
  @Mock private PropertyMapper propertyMapper;
  @Mock private ContactMapper contactMapper;
  @Mock private CurrencyEnforcementService currencyEnforcement;
  @Mock private AuditService auditService;
  @Mock private DocumentService documentService;
  @Mock private DocumentMapper documentMapper;
  @Mock private MetricsService metricsService;
  @Mock private NotificationService notificationService;
  @Mock private S3StorageService s3StorageService;
  @Mock private AuditLogRepository auditLogRepository;
  @Mock private AppProperties appProperties;
  @Mock private org.springframework.transaction.PlatformTransactionManager transactionManager;
  @Mock private Validator validator;

  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-03-01T12:00:00Z"), ZoneOffset.UTC);

  private ExpenseService service;
  private UserPrincipal principal;
  private ExpenseIdentifier expenseIdentifier;

  @BeforeEach
  void setUp() {
    ExpenseMapper expenseMapper = wireRealExpenseMapper();

    service =
        new ExpenseService(
            expenseRepository,
            propertyRepository,
            unitRepository,
            contactRepository,
            documentRepository,
            expenseAllocationService,
            expenseMapper,
            propertyMapper,
            contactMapper,
            currencyEnforcement,
            auditService,
            documentService,
            documentMapper,
            metricsService,
            notificationService,
            s3StorageService,
            auditLogRepository,
            appProperties,
            CLOCK,
            transactionManager,
            validator);

    principal =
        new UserPrincipal(
            USER_ID,
            "usr_test",
            "kc-id",
            "test@example.com",
            "Test User",
            TEAM_ID,
            "team_test",
            TeamRole.TEAM_ADMIN);

    expenseIdentifier = SidGenerator.newExpenseId();
  }

  @Test
  @DisplayName("a description-only edit does not re-run the allocation split")
  void descriptionOnlyEditLeavesAllocationsUntouched() {
    Expense expense = buildingLevelExpense("1200.00", "Roof repair");
    when(expenseRepository.getByIdentifierAndTeamId(expenseIdentifier, TEAM_ID))
        .thenReturn(expense);
    when(expenseRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    UpdateExpenseRequest request =
        new UpdateExpenseRequest(
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.of("Roof repair — corrected typo"),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());

    service.updateExpense(expenseIdentifier, request, principal);

    verify(expenseAllocationService, never()).allocate(any(), any());
    verify(expenseAllocationService, never()).retireAllocations(any(), any());
  }

  @Test
  @DisplayName("an amount change does re-run the allocation split")
  void amountChangeReallocates() {
    Expense expense = buildingLevelExpense("1200.00", "Roof repair");
    when(expenseRepository.getByIdentifierAndTeamId(expenseIdentifier, TEAM_ID))
        .thenReturn(expense);
    when(expenseRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    UpdateExpenseRequest request =
        new UpdateExpenseRequest(
            Optional.empty(),
            Optional.of(new BigDecimal("1500.00")),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());

    service.updateExpense(expenseIdentifier, request, principal);

    verify(expenseAllocationService, times(1)).allocate(any(), eq(USER_ID));
    verify(expenseAllocationService, never()).retireAllocations(any(), any());
  }

  private Expense buildingLevelExpense(String amount, String description) {
    return Expense.builder()
        .id(UUID.randomUUID())
        .identifier(Optional.of(expenseIdentifier))
        .teamId(TEAM_ID)
        .propertyId(UUID.randomUUID())
        .category(Expense.ExpenseCategory.MAINTENANCE)
        .amount(new MoneyAmount(new BigDecimal(amount), "EUR"))
        .expenseDate(LocalDate.of(2026, 1, 1))
        .description(description)
        .createdAt(CLOCK.instant())
        .updatedAt(CLOCK.instant())
        .createdBy(USER_ID)
        .updatedBy(USER_ID)
        .build();
  }

  private static ExpenseMapper wireRealExpenseMapper() {
    ExpenseMapperImpl impl = new ExpenseMapperImpl();
    try {
      Field field = ExpenseMapperImpl.class.getDeclaredField("optionalMappingConfig");
      field.setAccessible(true);
      field.set(impl, new OptionalMappingConfig());
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException("Failed to wire OptionalMappingConfig into ExpenseMapperImpl", e);
    }
    return impl;
  }
}
