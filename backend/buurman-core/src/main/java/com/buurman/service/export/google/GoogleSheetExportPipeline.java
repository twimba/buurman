package com.buurman.service.export.google;

import java.util.function.Consumer;

import org.springframework.stereotype.Service;

import com.buurman.domain.GoogleAccessToken;
import com.buurman.domain.GoogleSheetExport;
import com.google.api.services.drive.Drive;
import com.google.api.services.sheets.v4.Sheets;

import lombok.RequiredArgsConstructor;

/**
 * Shared orchestration for every Google Sheets export: builds Sheets + Drive clients, resolves the
 * user's "Buurman exports" folder, ensures a unique title, runs the caller's per-tab builder,
 * creates the spreadsheet, and moves it into the folder. Per-entity exporters supply only the
 * tab-building logic.
 */
@Service
@RequiredArgsConstructor
public class GoogleSheetExportPipeline {

  private final GoogleSheetsClientFactory clientFactory;
  private final BuurmanExportsFolderService folderService;

  public GoogleSheetExport export(
      GoogleAccessToken token, String baseTitle, Consumer<GoogleSheetBuilder> tabsBuilder) {
    Drive drive = clientFactory.drive(token);
    Sheets sheets = clientFactory.sheets(token);

    String folderId = folderService.findOrCreate(drive);
    String uniqueTitle = folderService.uniqueTitle(drive, folderId, baseTitle);

    GoogleSheetBuilder builder = new GoogleSheetBuilder(uniqueTitle);
    tabsBuilder.accept(builder);

    GoogleSheetExport result = builder.create(sheets);
    folderService.moveIntoFolder(drive, result.spreadsheetId(), folderId);
    return result;
  }
}
