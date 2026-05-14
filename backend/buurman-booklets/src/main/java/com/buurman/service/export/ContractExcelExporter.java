package com.buurman.service.export;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.buurman.service.export.tabular.ContractTabularExportBuilder;
import com.buurman.service.export.tabular.ExcelRenderer;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ContractExcelExporter {
  private final ContractTabularExportBuilder builder;
  private final ExcelRenderer renderer;

  public byte[] generate(UUID teamId) {
    return renderer.render(builder.build(teamId));
  }
}
