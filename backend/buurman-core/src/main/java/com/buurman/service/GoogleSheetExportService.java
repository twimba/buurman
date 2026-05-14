package com.buurman.service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import com.buurman.domain.GoogleAccessToken;
import com.buurman.domain.GoogleSheetExport;
import com.buurman.dto.response.PortfolioDashboardResponse;
import com.buurman.dto.response.PropertyDashboardResponse;

/**
 * Generates Google Sheets in the user's own Drive. Split out of {@link ExportService} to keep the
 * surfaces focused: {@code ExportService} owns the byte-array output formats (PDF, CSV, Excel),
 * this interface owns the Drive-bound Sheets output. Implementations consume the same shared {@code
 * TabularExport} builders, so adding a column lands in both places without churn.
 */
public interface GoogleSheetExportService {

  GoogleSheetExport generateContactsGoogleSheet(GoogleAccessToken token, UUID teamId, String title);

  GoogleSheetExport generateTransactionHistoryGoogleSheet(
      GoogleAccessToken token,
      Optional<LocalDate> startDate,
      Optional<LocalDate> endDate,
      UUID teamId,
      String title);

  GoogleSheetExport generatePropertyDashboardGoogleSheet(
      GoogleAccessToken token, PropertyDashboardResponse dashboard, String title);

  GoogleSheetExport generatePortfolioDashboardGoogleSheet(
      GoogleAccessToken token, PortfolioDashboardResponse dashboard, String title);

  GoogleSheetExport generatePropertiesGoogleSheet(
      GoogleAccessToken token, UUID teamId, String title);

  GoogleSheetExport generatePaymentsGoogleSheet(GoogleAccessToken token, UUID teamId, String title);

  GoogleSheetExport generateExpensesGoogleSheet(GoogleAccessToken token, UUID teamId, String title);

  GoogleSheetExport generateContractsGoogleSheet(
      GoogleAccessToken token, UUID teamId, String title);
}
