package com.buurman.service.export.google;

import org.springframework.stereotype.Component;

import com.buurman.domain.GoogleAccessToken;
import com.buurman.domain.GoogleSheetExport;
import com.buurman.dto.response.PortfolioDashboardResponse;
import com.buurman.service.export.tabular.PortfolioDashboardTabularExportBuilder;
import com.buurman.service.export.tabular.SheetsRenderer;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PortfolioDashboardGoogleSheetExporter {

  private final PortfolioDashboardTabularExportBuilder builder;
  private final SheetsRenderer renderer;

  public GoogleSheetExport generate(
      GoogleAccessToken token, PortfolioDashboardResponse dashboard, String title) {
    return renderer.render(builder.build(dashboard), token, title);
  }
}
