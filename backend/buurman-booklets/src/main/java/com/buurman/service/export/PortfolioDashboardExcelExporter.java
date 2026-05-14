package com.buurman.service.export;

import org.springframework.stereotype.Component;

import com.buurman.dto.response.PortfolioDashboardResponse;
import com.buurman.service.export.tabular.ExcelRenderer;
import com.buurman.service.export.tabular.PortfolioDashboardTabularExportBuilder;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PortfolioDashboardExcelExporter {

  private final PortfolioDashboardTabularExportBuilder builder;
  private final ExcelRenderer renderer;

  public byte[] generate(PortfolioDashboardResponse dashboard) {
    return renderer.render(builder.build(dashboard));
  }
}
