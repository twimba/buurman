package com.buurman.service.export;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.buurman.service.export.tabular.ExcelRenderer;
import com.buurman.service.export.tabular.PropertyTabularExportBuilder;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PropertyExcelExporter {
  private final PropertyTabularExportBuilder builder;
  private final ExcelRenderer renderer;

  public byte[] generate(UUID teamId) {
    return renderer.render(builder.build(teamId));
  }
}
