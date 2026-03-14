package com.buurman.service.export;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.Optional;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import com.buurman.dto.response.PortfolioDashboardResponse;
import com.buurman.dto.response.PortfolioDashboardResponse.PortfolioSummary;
import com.buurman.dto.response.PortfolioDashboardResponse.PropertyPerformance;
import com.buurman.dto.response.PropertyDashboardResponse.MonthlyDataPoint;
import com.buurman.exception.ExternalServiceException;

@Component
public class PortfolioDashboardExcelExporter {

  public byte[] generate(PortfolioDashboardResponse dashboard) {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream os = new ByteArrayOutputStream()) {

      Font boldFont = workbook.createFont();
      boldFont.setBold(true);
      CellStyle headerStyle = workbook.createCellStyle();
      headerStyle.setFont(boldFont);

      PortfolioSummary s = dashboard.summary();

      // Sheet 1: Summary
      Sheet summarySheet = workbook.createSheet("Summary");
      int r = 0;
      r = writeHeader(summarySheet, r, headerStyle, "Metric", "Value");
      r = writeRow(summarySheet, r, "Currency", dashboard.currency().orElse("N/A"));
      r =
          writeRow(
              summarySheet,
              r,
              "Properties with Financial Data",
              dashboard.propertiesWithFinancialData() + " / " + dashboard.totalProperties());
      r = writeOptionalRow(summarySheet, r, "Total Portfolio Value", s.totalPortfolioValue());
      r = writeOptionalRow(summarySheet, r, "Total Equity", s.totalEquity());
      r = writeOptionalRow(summarySheet, r, "Monthly Cash Flow", s.monthlyCashFlow());
      r = writeOptionalRow(summarySheet, r, "Annual NOI", s.annualNoi());
      r = writeOptionalRow(summarySheet, r, "Weighted Cap Rate %", s.weightedCapRate());
      r = writeOptionalRow(summarySheet, r, "Weighted Cash-on-Cash %", s.weightedCashOnCash());
      r = writeOptionalRow(summarySheet, r, "Portfolio Occupancy %", s.portfolioOccupancy());
      r = writeOptionalRow(summarySheet, r, "Debt-to-Equity", s.debtToEquity());
      r = writeOptionalRow(summarySheet, r, "DSCR", s.portfolioDscr());
      r = writeOptionalRow(summarySheet, r, "Income Concentration %", s.incomeConcentration());
      writeOptionalRow(summarySheet, r, "Data Completeness %", s.dataCompleteness());
      summarySheet.autoSizeColumn(0);
      summarySheet.autoSizeColumn(1);

      // Sheet 2: Monthly Cash Flow
      Sheet cashFlowSheet = workbook.createSheet("Monthly Cash Flow");
      int cr = 0;
      cr =
          writeHeader(
              cashFlowSheet, cr, headerStyle, "Month", "Income", "Expenses", "Mortgage", "Net");
      for (MonthlyDataPoint m : dashboard.cashFlow().months()) {
        Row row = cashFlowSheet.createRow(cr++);
        row.createCell(0).setCellValue(m.month());
        row.createCell(1).setCellValue(m.income().doubleValue());
        row.createCell(2).setCellValue(m.expenses().doubleValue());
        row.createCell(3).setCellValue(m.mortgage().doubleValue());
        row.createCell(4).setCellValue(m.net().doubleValue());
      }
      for (int i = 0; i < 5; i++) {
        cashFlowSheet.autoSizeColumn(i);
      }

      // Sheet 3: Property Comparison
      Sheet compSheet = workbook.createSheet("Property Comparison");
      int pr = 0;
      pr =
          writeHeader(
              compSheet,
              pr,
              headerStyle,
              "Property",
              "Category",
              "Monthly Cash Flow",
              "Annual NOI",
              "Cap Rate %",
              "Cash-on-Cash %",
              "Occupancy %",
              "Data Completeness %",
              "Currency",
              "Currency Mismatch");
      for (PropertyPerformance pp : dashboard.propertyComparison()) {
        Row row = compSheet.createRow(pr++);
        row.createCell(0).setCellValue(pp.address());
        row.createCell(1).setCellValue(pp.category());
        setCellOptional(row.createCell(2), pp.monthlyCashFlow());
        setCellOptional(row.createCell(3), pp.annualNoi());
        setCellOptional(row.createCell(4), pp.capRate());
        setCellOptional(row.createCell(5), pp.cashOnCash());
        setCellOptional(row.createCell(6), pp.occupancyRate());
        row.createCell(7).setCellValue(pp.completenessPercent());
        row.createCell(8).setCellValue(pp.currency().orElse("N/A"));
        row.createCell(9).setCellValue(pp.currencyMismatch());
      }
      for (int i = 0; i < 10; i++) {
        compSheet.autoSizeColumn(i);
      }

      workbook.write(os);
      return os.toByteArray();
    } catch (Exception e) {
      throw new ExternalServiceException("Failed to generate portfolio Excel", e);
    }
  }

  private static int writeHeader(Sheet sheet, int rowNum, CellStyle style, String... headers) {
    Row row = sheet.createRow(rowNum);
    for (int i = 0; i < headers.length; i++) {
      Cell cell = row.createCell(i);
      cell.setCellValue(headers[i]);
      cell.setCellStyle(style);
    }
    return rowNum + 1;
  }

  private static int writeRow(Sheet sheet, int rowNum, String label, String value) {
    Row row = sheet.createRow(rowNum);
    row.createCell(0).setCellValue(label);
    row.createCell(1).setCellValue(value);
    return rowNum + 1;
  }

  private static int writeOptionalRow(
      Sheet sheet, int rowNum, String label, Optional<BigDecimal> value) {
    Row row = sheet.createRow(rowNum);
    row.createCell(0).setCellValue(label);
    if (value.isPresent()) {
      row.createCell(1).setCellValue(value.get().doubleValue());
    } else {
      row.createCell(1).setCellValue("N/A");
    }
    return rowNum + 1;
  }

  private static void setCellOptional(Cell cell, Optional<BigDecimal> value) {
    if (value.isPresent()) {
      cell.setCellValue(value.get().doubleValue());
    } else {
      cell.setCellValue("N/A");
    }
  }
}
