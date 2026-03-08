package com.buurman.service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.domain.identifier.TenantIdentifier;
import com.buurman.dto.response.PortfolioDashboardResponse;
import com.buurman.dto.response.PropertyDashboardResponse;

public interface ExportService {

  byte[] generateTransactionHistoryCSV(
      Optional<LocalDate> startDate, Optional<LocalDate> endDate, UUID teamId);

  byte[] generateTransactionHistoryPDF(
      Optional<LocalDate> startDate, Optional<LocalDate> endDate, UUID teamId);

  byte[] generatePropertyBrochurePDF(PropertyIdentifier propertyIdentifier, UUID teamId);

  byte[] generateContractReportPDF(ContractIdentifier contractIdentifier, UUID teamId);

  byte[] generateTenantReportPDF(TenantIdentifier tenantIdentifier, UUID teamId);

  byte[] generatePropertyDashboardPDF(PropertyDashboardResponse dashboard);

  byte[] generatePropertyDashboardCSV(PropertyDashboardResponse dashboard);

  byte[] generatePortfolioDashboardPDF(PortfolioDashboardResponse dashboard);

  byte[] generatePortfolioDashboardCSV(PortfolioDashboardResponse dashboard);

  byte[] generateTransactionHistoryExcel(
      Optional<LocalDate> startDate, Optional<LocalDate> endDate, UUID teamId);

  byte[] generatePropertyDashboardExcel(PropertyDashboardResponse dashboard);

  byte[] generatePortfolioDashboardExcel(PortfolioDashboardResponse dashboard);
}
