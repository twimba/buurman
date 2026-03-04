package com.buurman.mapper;

import static java.time.ZoneOffset.UTC;

import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.buurman.domain.Expense;
import com.buurman.jooq.generated.tables.records.ExpensesRecord;
import com.buurman.util.CurrencyUtils;

@Component
public class ExpenseRecordMapper {

  public Optional<Expense> toDomain(@Nullable ExpensesRecord record) {
    if (record == null) {
      return Optional.empty();
    }

    Expense expense = new Expense();
    expense.setId(record.getId());
    expense.setIdentifier(java.util.Optional.of(record.getIdentifier()));
    expense.setTeamId(record.getTeamId());
    expense.setPropertyId(record.getPropertyId());
    expense.setCategory(Expense.ExpenseCategory.valueOf(record.getCategory()));
    expense.setAmount(CurrencyUtils.toMajorUnits(record.getAmount(), record.getCurrency()));
    expense.setCurrency(record.getCurrency());
    expense.setExpenseDate(record.getExpenseDate());
    expense.setDescription(record.getDescription());
    expense.setNotes(Optional.ofNullable(record.getNotes()));
    expense.setCreatedAt(record.getCreatedAt().toInstant(UTC));
    expense.setUpdatedAt(record.getUpdatedAt().toInstant(UTC));
    expense.setCreatedBy(record.getCreatedBy());
    expense.setUpdatedBy(record.getUpdatedBy());
    expense.setDeletedAt(Optional.ofNullable(record.getDeletedAt()).map(dt -> dt.toInstant(UTC)));

    return Optional.of(expense);
  }
}
