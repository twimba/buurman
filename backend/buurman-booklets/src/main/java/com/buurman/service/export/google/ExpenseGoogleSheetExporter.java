package com.buurman.service.export.google;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.buurman.domain.GoogleAccessToken;
import com.buurman.domain.GoogleSheetExport;
import com.buurman.service.export.tabular.ExpenseTabularExportBuilder;
import com.buurman.service.export.tabular.SheetsRenderer;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ExpenseGoogleSheetExporter {
  private final ExpenseTabularExportBuilder builder;
  private final SheetsRenderer renderer;

  public GoogleSheetExport generate(GoogleAccessToken token, UUID teamId, String title) {
    return renderer.render(builder.build(teamId), token, title);
  }
}
