package com.buurman.service;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import com.buurman.domain.identifier.ContactIdentifier;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.dto.response.PortfolioDashboardResponse;
import com.buurman.dto.response.PropertyDashboardResponse;

/**
 * Generates byte-array export artefacts (PDF, CSV, Excel) for every supported entity. The
 * Drive-bound Google Sheets exports live in a sibling interface {@link GoogleSheetExportService} to
 * keep this surface focused on file-download outputs.
 */
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

  // One-page A4-landscape summary cards
  byte[] generatePropertySummaryPDF(
      PropertyIdentifier propertyIdentifier, UUID teamId, Locale locale);

  byte[] generateContractSummaryPDF(
      ContractIdentifier contractIdentifier, UUID teamId, Locale locale);

  byte[] generateContactSummaryPDF(ContactIdentifier contactIdentifier, UUID teamId, Locale locale);

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

  // Properties
  byte[] generatePropertiesCSV(UUID teamId);

  byte[] generatePropertiesExcel(UUID teamId);

  // Deposits
  byte[] generateDepositsCSV(UUID teamId);

  byte[] generateDepositsExcel(UUID teamId);

  // Payments
  byte[] generatePaymentsCSV(UUID teamId);

  byte[] generatePaymentsExcel(UUID teamId);

  // Expenses
  byte[] generateExpensesCSV(UUID teamId);

  byte[] generateExpensesExcel(UUID teamId);

  // Contracts
  byte[] generateContractsCSV(UUID teamId);

  byte[] generateContractsExcel(UUID teamId);
}
