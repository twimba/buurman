package com.buurman.service.export.google;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.buurman.domain.GoogleAccessToken;
import com.buurman.domain.GoogleSheetExport;
import com.buurman.service.export.tabular.SheetsRenderer;
import com.buurman.service.export.tabular.TransactionTabularExportBuilder;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TransactionGoogleSheetExporter {

  private final TransactionTabularExportBuilder builder;
  private final SheetsRenderer renderer;

  public GoogleSheetExport generate(
      GoogleAccessToken token,
      Optional<LocalDate> startDate,
      Optional<LocalDate> endDate,
      UUID teamId,
      String title) {
    return renderer.render(builder.build(startDate, endDate, teamId), token, title);
  }
}
