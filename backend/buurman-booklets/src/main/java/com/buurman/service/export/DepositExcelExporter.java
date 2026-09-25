package com.buurman.service.export;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.buurman.service.export.tabular.DepositTabularExportBuilder;
import com.buurman.service.export.tabular.ExcelRenderer;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DepositExcelExporter {
  private final DepositTabularExportBuilder builder;
  private final ExcelRenderer renderer;

  public byte[] generate(UUID teamId) {
    return renderer.render(builder.build(teamId));
  }
}
