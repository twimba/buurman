package com.buurman.service.export;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.buurman.service.export.tabular.ContractTabularExportBuilder;
import com.buurman.service.export.tabular.CsvRenderer;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ContractCsvExporter {
  private final ContractTabularExportBuilder builder;
  private final CsvRenderer renderer;

  public byte[] generate(UUID teamId) {
    return renderer.render(builder.build(teamId));
  }
}
