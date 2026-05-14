package com.buurman.service.export;

import org.springframework.stereotype.Component;

import com.buurman.dto.response.PropertyDashboardResponse;
import com.buurman.service.export.tabular.ExcelRenderer;
import com.buurman.service.export.tabular.PropertyDashboardTabularExportBuilder;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PropertyDashboardExcelExporter {

  private final PropertyDashboardTabularExportBuilder builder;
  private final ExcelRenderer renderer;

  public byte[] generate(PropertyDashboardResponse dashboard) {
    return renderer.render(builder.build(dashboard));
  }
}
