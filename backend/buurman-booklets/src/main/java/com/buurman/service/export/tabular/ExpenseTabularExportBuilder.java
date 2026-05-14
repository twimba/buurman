package com.buurman.service.export.tabular;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.buurman.domain.Expense;
import com.buurman.domain.Property;
import com.buurman.repository.ExpenseRepository;
import com.buurman.repository.PropertyRepository;

import lombok.RequiredArgsConstructor;

/** Single-sheet TabularExport for all team expenses, joined to property address. */
@Component
@RequiredArgsConstructor
public class ExpenseTabularExportBuilder {

  private final ExpenseRepository expenseRepository;
  private final PropertyRepository propertyRepository;

  public TabularExport build(UUID teamId) {
    List<Expense> expenses = expenseRepository.findAllByTeamId(teamId);

    Map<UUID, String> propertyById = new HashMap<>();
    Map<UUID, String> propertyAddressById = new HashMap<>();
    for (Property p : propertyRepository.findAllByTeamId(teamId)) {
      propertyById.put(p.getId(), p.getIdentifier().map(Object::toString).orElse(""));
      propertyAddressById.put(p.getId(), p.getStreet() + ", " + p.getCity());
    }

    TabularExport export = new TabularExport("expenses");
    TabularSheet sheet =
        export.addSheet(
            "Expenses",
            List.of(
                TabularColumn.text("Identifier"),
                TabularColumn.text("Property"),
                TabularColumn.text("Property Address"),
                TabularColumn.text("Category"),
                TabularColumn.of("Expense Date", TabularColumnFormat.DATE),
                TabularColumn.text("Description"),
                TabularColumn.of("Amount", TabularColumnFormat.CURRENCY),
                TabularColumn.text("Currency"),
                TabularColumn.text("Notes"),
                TabularColumn.text("Created At"),
                TabularColumn.text("Updated At")));

    for (Expense e : expenses) {
      sheet.addRow(
          e.getIdentifier().map(Object::toString).orElse(""),
          propertyById.getOrDefault(e.getPropertyId(), ""),
          propertyAddressById.getOrDefault(e.getPropertyId(), ""),
          e.getCategory().name(),
          e.getExpenseDate() != null ? e.getExpenseDate().toString() : "",
          e.getDescription(),
          e.getAmount() != null ? e.getAmount().value() : "",
          e.getAmount() != null ? e.getAmount().currency() : "",
          e.getNotes().orElse(""),
          e.getCreatedAt() != null ? e.getCreatedAt().toString() : "",
          e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : "");
    }
    return export;
  }
}
