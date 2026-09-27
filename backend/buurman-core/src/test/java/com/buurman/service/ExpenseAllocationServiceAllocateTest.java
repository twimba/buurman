package com.buurman.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buurman.domain.AllocationBasis;
import com.buurman.domain.Expense;
import com.buurman.domain.ExpenseAllocation;
import com.buurman.domain.Property;
import com.buurman.domain.Unit;
import com.buurman.domain.UnitStatus;
import com.buurman.domain.UnitType;
import com.buurman.repository.ExpenseAllocationRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.UnitRepository;
import com.buurman.util.MoneyAmount;
import com.buurman.util.SidGenerator;

/**
 * {@link ExpenseAllocationService#allocate} must never overwrite a landlord's hand-split (MANUAL)
 * allocation, since it runs automatically on every building-level expense edit — only the explicit
 * recompute endpoint ({@link ExpenseAllocationService#recompute}) is allowed to lift that override
 * (BUUR-106 Critical 2).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExpenseAllocationService.allocate — MANUAL override protection")
class ExpenseAllocationServiceAllocateTest {

  @Mock private ExpenseAllocationRepository expenseAllocationRepository;
  @Mock private UnitRepository unitRepository;
  @Mock private PropertyRepository propertyRepository;

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-03-01T12:00:00Z"), ZoneOffset.UTC);
  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID ACTOR_ID = UUID.randomUUID();

  private ExpenseAllocationService service;

  @org.junit.jupiter.api.BeforeEach
  void setUp() {
    // propertyRepository is deliberately null here: the MANUAL guard must short-circuit before
    // any property/unit lookup, so a test that reached requirePropertyRepository() would NPE.
    service =
        new ExpenseAllocationService(expenseAllocationRepository, unitRepository, null, CLOCK);
  }

  @Test
  @DisplayName(
      "leaves an active MANUAL set untouched — refuses to recompute over a hand-split override")
  void doesNotOverwriteActiveManualBasis() {
    UUID expenseId = UUID.randomUUID();
    UUID propertyId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();

    Expense expense =
        Expense.builder()
            .id(expenseId)
            .teamId(TEAM_ID)
            .propertyId(propertyId)
            .category(Expense.ExpenseCategory.MAINTENANCE)
            .amount(new MoneyAmount(new BigDecimal("4800.00"), "EUR"))
            .expenseDate(LocalDate.of(2026, 1, 1))
            .description("Boiler replacement")
            .build();

    Unit unit =
        Unit.builder()
            .id(unitId)
            .teamId(TEAM_ID)
            .propertyId(propertyId)
            .identifier(Optional.of(SidGenerator.newUnitId()))
            .unitNumber("1")
            .unitType(UnitType.APARTMENT)
            .status(UnitStatus.OCCUPIED)
            .sortOrder(0)
            .build();

    ExpenseAllocation manualRow =
        ExpenseAllocation.builder()
            .id(UUID.randomUUID())
            .identifier(Optional.of(SidGenerator.newExpenseAllocationId()))
            .teamId(TEAM_ID)
            .expenseId(expenseId)
            .unitId(unitId)
            .amount(new MoneyAmount(new BigDecimal("2400.00"), "EUR"))
            .basis(AllocationBasis.MANUAL)
            .createdAt(Optional.of(CLOCK.instant()))
            .build();

    when(expenseAllocationRepository.findByExpenseIdAndTeamId(expenseId, TEAM_ID))
        .thenReturn(List.of(manualRow));
    when(unitRepository.findByIdsAndTeamId(List.of(unitId), TEAM_ID)).thenReturn(List.of(unit));

    List<com.buurman.dto.response.ExpenseAllocationResponse> result =
        service.allocate(expense, ACTOR_ID);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).basis()).isEqualTo(AllocationBasis.MANUAL);
    assertThat(result.get(0).amount()).contains(new BigDecimal("2400.00"));

    verify(expenseAllocationRepository, never()).replaceForExpense(any(), any(), any(), any());
    verify(propertyRepository, never()).getByIdAndTeamId(any(), any());
    verify(unitRepository, never()).findAllByPropertyIdAndTeamId(any(), any());
  }

  @Test
  @DisplayName("recomputes normally when the active set is not MANUAL")
  void proceedsWhenActiveBasisIsNotManual() {
    UUID expenseId = UUID.randomUUID();
    UUID propertyId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();

    Expense expense =
        Expense.builder()
            .id(expenseId)
            .teamId(TEAM_ID)
            .propertyId(propertyId)
            .category(Expense.ExpenseCategory.MAINTENANCE)
            .amount(new MoneyAmount(new BigDecimal("1200.00"), "EUR"))
            .expenseDate(LocalDate.of(2026, 1, 1))
            .description("Roof repair")
            .build();

    Unit unit =
        Unit.builder()
            .id(unitId)
            .teamId(TEAM_ID)
            .propertyId(propertyId)
            .identifier(Optional.of(SidGenerator.newUnitId()))
            .unitNumber("1")
            .unitType(UnitType.APARTMENT)
            .status(UnitStatus.OCCUPIED)
            .sortOrder(0)
            .build();

    ExpenseAllocation equalRow =
        ExpenseAllocation.builder()
            .id(UUID.randomUUID())
            .identifier(Optional.of(SidGenerator.newExpenseAllocationId()))
            .teamId(TEAM_ID)
            .expenseId(expenseId)
            .unitId(unitId)
            .amount(new MoneyAmount(new BigDecimal("1200.00"), "EUR"))
            .basis(AllocationBasis.EQUAL)
            .createdAt(Optional.of(CLOCK.instant()))
            .build();

    ExpenseAllocationService serviceWithProperty =
        new ExpenseAllocationService(
            expenseAllocationRepository, unitRepository, propertyRepository, CLOCK);

    Property property =
        Property.builder()
            .id(propertyId)
            .teamId(TEAM_ID)
            .allocationBasis(AllocationBasis.EQUAL)
            .build();

    when(expenseAllocationRepository.findByExpenseIdAndTeamId(expenseId, TEAM_ID))
        .thenReturn(List.of(equalRow));
    when(propertyRepository.getByIdAndTeamId(propertyId, TEAM_ID)).thenReturn(property);
    when(unitRepository.findAllByPropertyIdAndTeamId(propertyId, TEAM_ID))
        .thenReturn(List.of(unit));
    when(unitRepository.findByIdsAndTeamId(List.of(unitId), TEAM_ID)).thenReturn(List.of(unit));
    when(expenseAllocationRepository.replaceForExpense(any(), any(), any(), any()))
        .thenAnswer(
            invocation -> {
              List<ExpenseAllocation> allocations = invocation.getArgument(2);
              allocations.forEach(
                  a -> {
                    a.setId(UUID.randomUUID());
                    a.setIdentifier(Optional.of(SidGenerator.newExpenseAllocationId()));
                    a.setCreatedAt(Optional.of(CLOCK.instant()));
                  });
              return allocations;
            });

    serviceWithProperty.allocate(expense, ACTOR_ID);

    verify(expenseAllocationRepository, org.mockito.Mockito.times(1))
        .replaceForExpense(any(), any(), any(), any());
  }
}
