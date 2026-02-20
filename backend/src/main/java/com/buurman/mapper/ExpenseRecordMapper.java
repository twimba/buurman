package com.buurman.mapper;

import static java.time.ZoneOffset.UTC;

import org.springframework.stereotype.Component;

import com.buurman.domain.Expense;
import com.buurman.jooq.generated.tables.records.ExpensesRecord;
import com.buurman.util.CurrencyUtils;

@Component
public class ExpenseRecordMapper {

  public Expense toDomain(ExpensesRecord record) {
    if (record == null) {
      return null;
    }

    Expense expense = new Expense();
    expense.setId(record.getId());
    expense.setIdentifier(record.getIdentifier());
    expense.setTeamId(record.getTeamId());
    expense.setPropertyId(record.getPropertyId());
    expense.setCategory(Expense.ExpenseCategory.valueOf(record.getCategory()));
    expense.setAmount(CurrencyUtils.toMajorUnits(record.getAmount(), record.getCurrency()));
    expense.setCurrency(record.getCurrency());
    expense.setExpenseDate(record.getExpenseDate());
    expense.setDescription(record.getDescription());
    expense.setNotes(record.getNotes());
    expense.setCreatedAt(
        record.getCreatedAt() != null ? record.getCreatedAt().toInstant(UTC) : null);
    expense.setUpdatedAt(
        record.getUpdatedAt() != null ? record.getUpdatedAt().toInstant(UTC) : null);
    expense.setCreatedBy(record.getCreatedBy());
    expense.setUpdatedBy(record.getUpdatedBy());
    expense.setDeletedAt(
        record.getDeletedAt() != null ? record.getDeletedAt().toInstant(UTC) : null);

    return expense;
  }
}
