package com.buurman.service.export.tabular;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.buurman.service.export.TransactionDataLoader;
import com.buurman.service.export.TransactionRecord;

import lombok.RequiredArgsConstructor;

/** Builds a single-sheet TabularExport of payments + expenses for the team in the date range. */
@Component
@RequiredArgsConstructor
public class TransactionTabularExportBuilder {

  private final TransactionDataLoader dataLoader;

  public TabularExport build(
      Optional<LocalDate> startDate, Optional<LocalDate> endDate, UUID teamId) {
    List<TransactionRecord> transactions = dataLoader.load(startDate, endDate, teamId);

    TabularExport export = new TabularExport("transactions");
    TabularSheet sheet =
        export.addSheet(
            "Transactions",
            List.of(
                TabularColumn.of("Date", TabularColumnFormat.DATE),
                TabularColumn.text("Type"),
                TabularColumn.text("Description"),
                TabularColumn.text("Property"),
                TabularColumn.text("Category"),
                TabularColumn.of("Amount", TabularColumnFormat.CURRENCY),
                TabularColumn.text("Currency")));

    for (TransactionRecord t : transactions) {
      sheet.addRow(
          t.date().toString(),
          t.type(),
          t.description(),
          t.property(),
          t.category().orElse(""),
          t.amount(),
          t.currency());
    }
    return export;
  }
}
