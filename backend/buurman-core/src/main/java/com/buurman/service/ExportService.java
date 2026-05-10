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

public interface ExportService {

  byte[] generateTransactionHistoryCSV(
      Optional<LocalDate> startDate, Optional<LocalDate> endDate, UUID teamId);

  byte[] generateTransactionHistoryPDF(
      Optional<LocalDate> startDate, Optional<LocalDate> endDate, UUID teamId);

  byte[] generatePropertyBrochurePDF(
      PropertyIdentifier propertyIdentifier, UUID teamId, Locale locale);

  byte[] generateContractReportPDF(
      ContractIdentifier contractIdentifier, UUID teamId, Locale locale);

  byte[] generateContactReportPDF(
      ContactIdentifier contactIdentifier, UUID teamId, Locale locale);

  byte[] generatePropertyDashboardPDF(PropertyDashboardResponse dashboard);

  byte[] generatePropertyDashboardCSV(PropertyDashboardResponse dashboard);

  byte[] generatePortfolioDashboardPDF(PortfolioDashboardResponse dashboard);

  byte[] generatePortfolioDashboardCSV(PortfolioDashboardResponse dashboard);

  byte[] generateTransactionHistoryExcel(
      Optional<LocalDate> startDate, Optional<LocalDate> endDate, UUID teamId);

  byte[] generatePropertyDashboardExcel(PropertyDashboardResponse dashboard);

  byte[] generatePortfolioDashboardExcel(PortfolioDashboardResponse dashboard);

  byte[] generateContactsCSV(UUID teamId);

  byte[] generateContactsExcel(UUID teamId);
}
