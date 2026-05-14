package com.buurman.service.export;

import org.springframework.stereotype.Component;

import com.buurman.dto.response.PortfolioDashboardResponse;
import com.buurman.service.export.tabular.CsvRenderer;
import com.buurman.service.export.tabular.PortfolioDashboardTabularExportBuilder;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PortfolioDashboardCsvExporter {

  private final PortfolioDashboardTabularExportBuilder builder;
  private final CsvRenderer renderer;

  public byte[] generate(PortfolioDashboardResponse dashboard) {
    return renderer.render(builder.build(dashboard));
  }
}
