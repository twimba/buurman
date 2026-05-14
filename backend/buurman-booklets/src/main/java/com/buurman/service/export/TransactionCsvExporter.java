package com.buurman.service.export;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.buurman.service.export.tabular.CsvRenderer;
import com.buurman.service.export.tabular.TransactionTabularExportBuilder;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TransactionCsvExporter {

  private final TransactionTabularExportBuilder builder;
  private final CsvRenderer renderer;

  public byte[] generate(Optional<LocalDate> startDate, Optional<LocalDate> endDate, UUID teamId) {
    return renderer.render(builder.build(startDate, endDate, teamId));
  }
}
