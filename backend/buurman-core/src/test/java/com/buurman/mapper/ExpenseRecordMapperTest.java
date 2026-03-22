package com.buurman.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.buurman.domain.Expense;
import com.buurman.domain.Expense.ExpenseCategory;
import com.buurman.domain.Sid;
import com.buurman.jooq.generated.tables.records.ExpensesRecord;

@DisplayName("ExpenseRecordMapper")
class ExpenseRecordMapperTest {

  private final ExpenseRecordMapper mapper = new ExpenseRecordMapper();

  private static final UUID ID = UUID.randomUUID();
  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID PROPERTY_ID = UUID.randomUUID();
  private static final UUID CREATED_BY = UUID.randomUUID();
  private static final UUID UPDATED_BY = UUID.randomUUID();
  private static final Sid IDENTIFIER = Sid.of("EXP01HQJK4B2X5M3N7P8Q9R0S1T2");
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 1, 12, 0, 0);

  @Nested
  @DisplayName("toDomain")
  class ToDomain {

    @Test
    @DisplayName("returns empty Optional for null record")
    void returnsEmptyForNull() {
      Optional<Expense> result = mapper.toDomain(null);

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("maps all fields from a complete record")
    void mapsCompleteRecord() {
      ExpensesRecord record = createCompleteRecord();

      Optional<Expense> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      Expense expense = result.get();
      assertThat(expense.getId()).isEqualTo(ID);
      assertThat(expense.getIdentifier()).contains(IDENTIFIER);
      assertThat(expense.getTeamId()).isEqualTo(TEAM_ID);
      assertThat(expense.getPropertyId()).isEqualTo(PROPERTY_ID);
      assertThat(expense.getCategory()).isEqualTo(ExpenseCategory.MAINTENANCE);
      assertThat(expense.getAmount().value()).isEqualByComparingTo(new BigDecimal("500.00"));
      assertThat(expense.getAmount().currency()).isEqualTo("EUR");
      assertThat(expense.getExpenseDate()).isEqualTo(LocalDate.of(2026, 2, 15));
      assertThat(expense.getDescription()).isEqualTo("Plumbing repair");
      assertThat(expense.getNotes()).contains("Urgent fix");
      assertThat(expense.getCreatedAt()).isEqualTo(NOW.toInstant(ZoneOffset.UTC));
      assertThat(expense.getUpdatedAt()).isEqualTo(NOW.toInstant(ZoneOffset.UTC));
      assertThat(expense.getCreatedBy()).isEqualTo(CREATED_BY);
      assertThat(expense.getUpdatedBy()).isEqualTo(UPDATED_BY);
      assertThat(expense.getDeletedAt()).isEmpty();
    }

    @Test
    @DisplayName("maps all ExpenseCategory enum values")
    void mapsAllExpenseCategories() {
      for (ExpenseCategory category : ExpenseCategory.values()) {
        ExpensesRecord record = createCompleteRecord();
        record.setCategory(category.name());

        Optional<Expense> result = mapper.toDomain(record);

        assertThat(result).isPresent();
        assertThat(result.get().getCategory()).isEqualTo(category);
      }
    }

    @Test
    @DisplayName("wraps null notes as empty Optional")
    void wrapsNullNotesAsEmpty() {
      ExpensesRecord record = createCompleteRecord();
      record.setNotes(null);

      Optional<Expense> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      assertThat(result.get().getNotes()).isEmpty();
    }

    @Test
    @DisplayName("maps deletedAt when present")
    void mapsDeletedAt() {
      ExpensesRecord record = createCompleteRecord();
      LocalDateTime deletedAt = LocalDateTime.of(2026, 6, 1, 0, 0, 0);
      record.setDeletedAt(deletedAt);

      Optional<Expense> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      assertThat(result.get().getDeletedAt()).contains(deletedAt.toInstant(ZoneOffset.UTC));
    }
  }

  private ExpensesRecord createCompleteRecord() {
    ExpensesRecord record = new ExpensesRecord();
    record.setId(ID);
    record.setIdentifier(IDENTIFIER);
    record.setTeamId(TEAM_ID);
    record.setPropertyId(PROPERTY_ID);
    record.setCategory("MAINTENANCE");
    record.setAmount(new BigDecimal("500.00"));
    record.setCurrency("EUR");
    record.setExpenseDate(LocalDate.of(2026, 2, 15));
    record.setDescription("Plumbing repair");
    record.setNotes("Urgent fix");
    record.setCreatedAt(NOW);
    record.setUpdatedAt(NOW);
    record.setCreatedBy(CREATED_BY);
    record.setUpdatedBy(UPDATED_BY);
    record.setDeletedAt(null);
    return record;
  }
}
