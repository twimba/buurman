package com.buurman.service;

import java.time.LocalDate;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.domain.identifier.TenantIdentifier;
import com.buurman.dto.response.PortfolioDashboardResponse;
import com.buurman.dto.response.PropertyDashboardResponse;

public interface ExportService {

  byte[] generateTransactionHistoryCSV(
      @Nullable LocalDate startDate, @Nullable LocalDate endDate, UUID teamId);

  byte[] generateTransactionHistoryPDF(
      @Nullable LocalDate startDate, @Nullable LocalDate endDate, UUID teamId);

  byte[] generatePropertyBrochurePDF(PropertyIdentifier propertyIdentifier, UUID teamId);

  byte[] generateContractReportPDF(ContractIdentifier contractIdentifier, UUID teamId);

  byte[] generateTenantReportPDF(TenantIdentifier tenantIdentifier, UUID teamId);

  byte[] generatePropertyDashboardPDF(PropertyDashboardResponse dashboard);

  byte[] generatePropertyDashboardCSV(PropertyDashboardResponse dashboard);

  byte[] generatePortfolioDashboardPDF(PortfolioDashboardResponse dashboard);

  byte[] generatePortfolioDashboardCSV(PortfolioDashboardResponse dashboard);
}
