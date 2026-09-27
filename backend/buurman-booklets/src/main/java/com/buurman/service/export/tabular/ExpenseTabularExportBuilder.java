package com.buurman.service.export.tabular;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.buurman.domain.Expense;
import com.buurman.domain.ExpenseAllocation;
import com.buurman.domain.Property;
import com.buurman.domain.Unit;
import com.buurman.repository.ExpenseAllocationRepository;
import com.buurman.repository.ExpenseRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.UnitRepository;

import lombok.RequiredArgsConstructor;

/**
 * Single-sheet TabularExport for all team expenses, joined to property address and unit allocation.
 * A building-level expense (no {@code unitId}) is split across its property's units by {@code
 * ExpenseAllocationService}; this export surfaces that split as one row per allocation rather than
 * one row per expense, so the allocated amounts are directly visible/summable. A unit-level expense
 * (an {@code unitId} already set) produces exactly one row naming that unit, with no allocation
 * basis (the whole amount was assigned directly, not computed by a basis rule).
 */
@Component
@RequiredArgsConstructor
public class ExpenseTabularExportBuilder {

  private final ExpenseRepository expenseRepository;
  private final PropertyRepository propertyRepository;
  private final UnitRepository unitRepository;
  private final ExpenseAllocationRepository expenseAllocationRepository;

  public TabularExport build(UUID teamId) {
    List<Expense> expenses = expenseRepository.findAllByTeamId(teamId);

    Map<UUID, String> propertyById = new HashMap<>();
    Map<UUID, String> propertyAddressById = new HashMap<>();
    for (Property p : propertyRepository.findAllByTeamId(teamId)) {
      propertyById.put(p.getId(), p.getIdentifier().map(Object::toString).orElse(""));
      propertyAddressById.put(p.getId(), p.getStreet() + ", " + p.getCity());
    }

    Map<UUID, Unit> unitById = new HashMap<>();
    for (Unit u : unitRepository.findAllByTeamId(teamId)) {
      unitById.put(u.getId(), u);
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
                TabularColumn.text("Allocated Unit"),
                TabularColumn.of("Allocated Amount", TabularColumnFormat.CURRENCY),
                TabularColumn.text("Allocation Basis"),
                TabularColumn.text("Notes"),
                TabularColumn.text("Created At"),
                TabularColumn.text("Updated At")));

    for (Expense e : expenses) {
      if (e.getUnitId().isPresent()) {
        sheet.addRow(
            expenseRow(
                e,
                propertyById,
                propertyAddressById,
                unitLabel(e.getUnitId().get(), unitById),
                e.getAmount().value(),
                ""));
        continue;
      }
      List<ExpenseAllocation> allocations =
          expenseAllocationRepository.findByExpenseIdAndTeamId(e.getId(), teamId);
      if (allocations.isEmpty()) {
        sheet.addRow(expenseRow(e, propertyById, propertyAddressById, "", "", ""));
        continue;
      }
      for (ExpenseAllocation allocation : allocations) {
        sheet.addRow(
            expenseRow(
                e,
                propertyById,
                propertyAddressById,
                unitLabel(allocation.getUnitId(), unitById),
                allocation.getAmount().value(),
                allocation.getBasis().name()));
      }
    }
    return export;
  }

  private Object[] expenseRow(
      Expense e,
      Map<UUID, String> propertyById,
      Map<UUID, String> propertyAddressById,
      Object allocatedUnit,
      Object allocatedAmount,
      Object allocationBasis) {
    return new Object[] {
      e.getIdentifier().map(Object::toString).orElse(""),
      propertyById.getOrDefault(e.getPropertyId(), ""),
      propertyAddressById.getOrDefault(e.getPropertyId(), ""),
      e.getCategory().name(),
      e.getExpenseDate() != null ? e.getExpenseDate().toString() : "",
      e.getDescription(),
      e.getAmount() != null ? e.getAmount().value() : "",
      e.getAmount() != null ? e.getAmount().currency() : "",
      allocatedUnit,
      allocatedAmount,
      allocationBasis,
      e.getNotes().orElse(""),
      e.getCreatedAt() != null ? e.getCreatedAt().toString() : "",
      e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : ""
    };
  }

  private String unitLabel(UUID unitId, Map<UUID, Unit> unitById) {
    Unit unit = unitById.get(unitId);
    if (unit == null) {
      return "";
    }
    return unit.getName()
        .map(name -> unit.getUnitNumber() + " (" + name + ")")
        .orElse(unit.getUnitNumber());
  }
}
