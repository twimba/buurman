package com.buurman.service.export;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.buurman.dto.response.PropertyDashboardResponse;
import com.buurman.dto.response.PropertyDashboardResponse.CategorySlice;
import com.buurman.dto.response.PropertyDashboardResponse.MonthlyDataPoint;
import com.buurman.dto.response.PropertyDashboardResponse.SummaryMetrics;
import com.buurman.exception.ExternalServiceException;

@Component
public class PropertyDashboardExcelExporter {

  public byte[] generate(PropertyDashboardResponse dashboard) {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream os = new ByteArrayOutputStream()) {

      Font boldFont = workbook.createFont();
      boldFont.setBold(true);
      CellStyle headerStyle = workbook.createCellStyle();
      headerStyle.setFont(boldFont);

      SummaryMetrics s = dashboard.summary();

      // Sheet 1: Summary
      Sheet summarySheet = workbook.createSheet("Summary");
      int r = 0;
      r = writeHeader(summarySheet, r, headerStyle, "Metric", "Value");
      r = writeRow(summarySheet, r, "Currency", s.currency().orElse("N/A"));
      r = writeNumericRow(summarySheet, r, "Total ROI %", s.totalRoiPercent().orElse(null));
      r = writeNumericRow(summarySheet, r, "Annualized ROI %", s.annualizedRoiPercent().orElse(null));
      r = writeNumericRow(summarySheet, r, "Cap Rate %", s.capRatePercent().orElse(null));
      r = writeNumericRow(summarySheet, r, "Cash-on-Cash %", s.cashOnCashPercent().orElse(null));
      r = writeNumericRow(summarySheet, r, "Monthly Cash Flow", s.monthlyCashFlow().orElse(null));
      r = writeNumericRow(summarySheet, r, "Annual NOI", s.annualNoi().orElse(null));
      r = writeNumericRow(summarySheet, r, "Total Equity", s.totalEquity().orElse(null));
      r = writeNumericRow(summarySheet, r, "Equity Growth %", s.equityGrowthPercent().orElse(null));
      r = writeNumericRow(summarySheet, r, "Occupancy Rate %", s.occupancyRatePercent().orElse(null));
      writeNumericRow(summarySheet, r, "Gross Rent Multiplier", s.grossRentMultiplier().orElse(null));
      summarySheet.autoSizeColumn(0);
      summarySheet.autoSizeColumn(1);

      // Sheet 2: Monthly Cash Flow
      Sheet cashFlowSheet = workbook.createSheet("Monthly Cash Flow");
      int cr = 0;
      cr = writeHeader(cashFlowSheet, cr, headerStyle, "Month", "Income", "Expenses", "Mortgage", "Net");
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

      // Sheet 3: Expense Breakdown
      Sheet expenseSheet = workbook.createSheet("Expense Breakdown");
      int er = 0;
      er = writeHeader(expenseSheet, er, headerStyle, "Category", "Amount");
      for (CategorySlice c : dashboard.expenseBreakdown().categories()) {
        Row row = expenseSheet.createRow(er++);
        row.createCell(0).setCellValue(c.category());
        row.createCell(1).setCellValue(c.amount().doubleValue());
      }
      expenseSheet.autoSizeColumn(0);
      expenseSheet.autoSizeColumn(1);

      workbook.write(os);
      return os.toByteArray();
    } catch (Exception e) {
      throw new ExternalServiceException("Failed to generate Excel", e);
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

  private static int writeNumericRow(
      Sheet sheet, int rowNum, String label, @Nullable BigDecimal value) {
    Row row = sheet.createRow(rowNum);
    row.createCell(0).setCellValue(label);
    if (value != null) {
      row.createCell(1).setCellValue(value.doubleValue());
    } else {
      row.createCell(1).setCellValue("N/A");
    }
    return rowNum + 1;
  }
}
