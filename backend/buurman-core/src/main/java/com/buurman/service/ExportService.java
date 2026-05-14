package com.buurman.service;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import com.buurman.domain.GoogleAccessToken;
import com.buurman.domain.GoogleSheetExport;
import com.buurman.domain.identifier.ContactIdentifier;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.dto.response.PortfolioDashboardResponse;
import com.buurman.dto.response.PropertyDashboardResponse;

public interface ExportService {

  byte[] generateTransactionHistoryCSV(
      Optional<LocalDate> startDate, Optional<LocalDate> endDate, UUID teamId);

  byte[] generateTransactionHistoryPDF(
      Optional<LocalDate> startDate, Optional<LocalDate> endDate, UUID teamId);

  byte[] generatePropertyBrochurePDF(
      PropertyIdentifier propertyIdentifier, UUID teamId, Locale locale);

  byte[] generateContractReportPDF(
      ContractIdentifier contractIdentifier, UUID teamId, Locale locale);

  byte[] generateContactReportPDF(ContactIdentifier contactIdentifier, UUID teamId, Locale locale);

  byte[] generatePropertyDashboardPDF(PropertyDashboardResponse dashboard, UUID teamId);

  byte[] generatePropertyDashboardCSV(PropertyDashboardResponse dashboard);

  byte[] generatePortfolioDashboardPDF(PortfolioDashboardResponse dashboard, UUID teamId);

  byte[] generatePortfolioDashboardCSV(PortfolioDashboardResponse dashboard);

  byte[] generateTransactionHistoryExcel(
      Optional<LocalDate> startDate, Optional<LocalDate> endDate, UUID teamId);

  byte[] generatePropertyDashboardExcel(PropertyDashboardResponse dashboard);

  byte[] generatePortfolioDashboardExcel(PortfolioDashboardResponse dashboard);

  byte[] generateContactsCSV(UUID teamId);

  byte[] generateContactsExcel(UUID teamId);

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

  // Properties
  byte[] generatePropertiesCSV(UUID teamId);

  byte[] generatePropertiesExcel(UUID teamId);

  GoogleSheetExport generatePropertiesGoogleSheet(
      GoogleAccessToken token, UUID teamId, String title);

  // Payments
  byte[] generatePaymentsCSV(UUID teamId);

  byte[] generatePaymentsExcel(UUID teamId);

  GoogleSheetExport generatePaymentsGoogleSheet(GoogleAccessToken token, UUID teamId, String title);

  // Expenses
  byte[] generateExpensesCSV(UUID teamId);

  byte[] generateExpensesExcel(UUID teamId);

  GoogleSheetExport generateExpensesGoogleSheet(GoogleAccessToken token, UUID teamId, String title);

  // Contracts
  byte[] generateContractsCSV(UUID teamId);

  byte[] generateContractsExcel(UUID teamId);

  GoogleSheetExport generateContractsGoogleSheet(
      GoogleAccessToken token, UUID teamId, String title);
}
