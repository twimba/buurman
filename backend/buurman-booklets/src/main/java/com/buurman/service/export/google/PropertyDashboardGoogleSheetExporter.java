package com.buurman.service.export.google;

import org.springframework.stereotype.Component;

import com.buurman.domain.GoogleAccessToken;
import com.buurman.domain.GoogleSheetExport;
import com.buurman.dto.response.PropertyDashboardResponse;
import com.buurman.service.export.tabular.PropertyDashboardTabularExportBuilder;
import com.buurman.service.export.tabular.SheetsRenderer;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PropertyDashboardGoogleSheetExporter {

  private final PropertyDashboardTabularExportBuilder builder;
  private final SheetsRenderer renderer;

  public GoogleSheetExport generate(
      GoogleAccessToken token, PropertyDashboardResponse dashboard, String title) {
    return renderer.render(builder.build(dashboard), token, title);
  }
}
