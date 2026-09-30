package com.buurman.service.export;

import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.buurman.service.export.tabular.ContractTabularExportBuilder;
import com.buurman.service.export.tabular.CsvRenderer;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ContractCsvExporter {
  private final ContractTabularExportBuilder builder;
  private final CsvRenderer renderer;

  public byte[] generate(
      UUID teamId,
      @Nullable String status,
      @Nullable String search,
      @Nullable Integer endingWithinDays) {
    return renderer.render(builder.build(teamId, status, search, endingWithinDays));
  }
}
