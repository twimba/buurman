package com.buurman.service.export;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.buurman.service.export.tabular.CsvRenderer;
import com.buurman.service.export.tabular.ExpenseTabularExportBuilder;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ExpenseCsvExporter {
  private final ExpenseTabularExportBuilder builder;
  private final CsvRenderer renderer;

  public byte[] generate(UUID teamId) {
    return renderer.render(builder.build(teamId));
  }
}
