package com.buurman.service.export;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.buurman.service.export.tabular.ExcelRenderer;
import com.buurman.service.export.tabular.TransactionTabularExportBuilder;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TransactionExcelExporter {

  private final TransactionTabularExportBuilder builder;
  private final ExcelRenderer renderer;

  public byte[] generate(Optional<LocalDate> startDate, Optional<LocalDate> endDate, UUID teamId) {
    return renderer.render(builder.build(startDate, endDate, teamId));
  }
}
