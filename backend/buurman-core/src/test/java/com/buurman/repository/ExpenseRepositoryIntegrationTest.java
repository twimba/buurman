package com.buurman.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.buurman.domain.Expense;
import com.buurman.domain.Expense.ExpenseCategory;
import com.buurman.mapper.ExpenseRecordMapper;
import com.buurman.util.MoneyAmount;
import com.buurman.util.SidGenerator;

/**
 * {@code ExpenseRepository.save()}'s unit_id column, on both the INSERT and UPDATE branches
 * (BUUR-106 follow-up register, section F, item 7). unit_id distinguishes a unit-level expense (no
 * allocation rows) from a building-level one (split across every unit by {@code
 * ExpenseAllocationService}), so a wrong binding here would silently misclassify an expense.
 */
@DisplayName("ExpenseRepository")
class ExpenseRepositoryIntegrationTest extends AbstractRepositoryIntegrationTest {

  private ExpenseRepository repository;
  private UUID propertyId;
  private UUID unitId;

  @BeforeEach
  void setUpRepository() {
    repository = new ExpenseRepository(dsl, new ExpenseRecordMapper(), CLOCK);
    propertyId = TestDataHelper.insertProperty(dsl, TEAM_A_ID, USER_ID);
    unitId = UUID.randomUUID();
    TestDataHelper.insertUnit(dsl, unitId, propertyId, TEAM_A_ID, "1", "VACANT");
  }

  private Expense expense(Optional<UUID> unitId) {
    return Expense.builder()
        .teamId(TEAM_A_ID)
        .propertyId(propertyId)
        .unitId(unitId)
        .category(ExpenseCategory.MAINTENANCE)
        .amount(MoneyAmount.of(new BigDecimal("120.00"), "EUR"))
        .expenseDate(LocalDate.of(2026, 3, 1))
        .description("Roof repair")
        .identifier(Optional.of(SidGenerator.newExpenseId()))
        .createdBy(USER_ID)
        .updatedBy(USER_ID)
        .build();
  }

  @Test
  @DisplayName("save()'s INSERT branch persists unit_id when the expense is unit-level")
  void insertPersistsUnitId() {
    Expense saved = repository.save(expense(Optional.of(unitId)));

    // Re-fetch independently of the save() return value.
    Expense reloaded =
        repository.getByIdentifierAndTeamId(saved.getIdentifier().orElseThrow(), TEAM_A_ID);

    assertThat(reloaded.getUnitId()).contains(unitId);
  }

  @Test
  @DisplayName("save()'s INSERT branch leaves unit_id null for a building-level expense")
  void insertLeavesUnitIdNullWhenAbsent() {
    Expense saved = repository.save(expense(Optional.empty()));

    Expense reloaded =
        repository.getByIdentifierAndTeamId(saved.getIdentifier().orElseThrow(), TEAM_A_ID);

    assertThat(reloaded.getUnitId()).isEmpty();
  }

  @Test
  @DisplayName("save()'s UPDATE branch persists a newly-assigned unit_id")
  void updatePersistsNewlyAssignedUnitId() {
    Expense saved = repository.save(expense(Optional.empty()));

    saved.setUnitId(Optional.of(unitId));
    repository.save(saved);

    Expense reloaded =
        repository.getByIdentifierAndTeamId(saved.getIdentifier().orElseThrow(), TEAM_A_ID);
    assertThat(reloaded.getUnitId()).contains(unitId);
  }

  @Test
  @DisplayName(
      "save()'s UPDATE branch clears unit_id when the expense is converted back to"
          + " building-level")
  void updateClearsUnitIdWhenRemoved() {
    Expense saved = repository.save(expense(Optional.of(unitId)));

    saved.setUnitId(Optional.empty());
    repository.save(saved);

    Expense reloaded =
        repository.getByIdentifierAndTeamId(saved.getIdentifier().orElseThrow(), TEAM_A_ID);
    assertThat(reloaded.getUnitId()).isEmpty();
  }
}
