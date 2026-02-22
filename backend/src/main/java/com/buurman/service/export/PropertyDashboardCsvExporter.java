package com.buurman.service.export;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.buurman.dto.response.PropertyDashboardResponse;
import com.buurman.dto.response.PropertyDashboardResponse.CategorySlice;
import com.buurman.dto.response.PropertyDashboardResponse.MonthlyDataPoint;
import com.buurman.dto.response.PropertyDashboardResponse.SummaryMetrics;
import com.buurman.exception.ExternalServiceException;
import com.opencsv.CSVWriter;

@Component
public class PropertyDashboardCsvExporter {

  public byte[] generate(PropertyDashboardResponse dashboard) {
    try (StringWriter sw = new StringWriter();
        CSVWriter writer = new CSVWriter(sw)) {

      SummaryMetrics s = dashboard.summary();

      // Summary section
      writer.writeNext(new String[] {"--- Summary Metrics ---"});
      writer.writeNext(new String[] {"Metric", "Value"});
      writer.writeNext(new String[] {"Currency", s.currency() != null ? s.currency() : "N/A"});
      writer.writeNext(row("Total ROI %", s.totalRoiPercent()));
      writer.writeNext(row("Annualized ROI %", s.annualizedRoiPercent()));
      writer.writeNext(row("Cap Rate %", s.capRatePercent()));
      writer.writeNext(row("Cash-on-Cash %", s.cashOnCashPercent()));
      writer.writeNext(row("Monthly Cash Flow", s.monthlyCashFlow()));
      writer.writeNext(row("Annual NOI", s.annualNoi()));
      writer.writeNext(row("Total Equity", s.totalEquity()));
      writer.writeNext(row("Equity Growth %", s.equityGrowthPercent()));
      writer.writeNext(row("Occupancy Rate %", s.occupancyRatePercent()));
      writer.writeNext(row("Gross Rent Multiplier", s.grossRentMultiplier()));
      writer.writeNext(new String[] {""});

      // Monthly cash flow
      writer.writeNext(new String[] {"--- Monthly Cash Flow ---"});
      writer.writeNext(new String[] {"Month", "Income", "Expenses", "Mortgage", "Net"});
      for (MonthlyDataPoint m : dashboard.cashFlow().months()) {
        writer.writeNext(
            new String[] {
              m.month(),
              m.income().toPlainString(),
              m.expenses().toPlainString(),
              m.mortgage().toPlainString(),
              m.net().toPlainString()
            });
      }
      writer.writeNext(new String[] {""});

      // Expense breakdown
      writer.writeNext(new String[] {"--- Expense Breakdown ---"});
      writer.writeNext(new String[] {"Category", "Amount"});
      for (CategorySlice c : dashboard.expenseBreakdown().categories()) {
        writer.writeNext(new String[] {c.category(), c.amount().toPlainString()});
      }

      // UTF-8 BOM for Excel compatibility
      byte[] bom = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
      byte[] csv = sw.toString().getBytes(StandardCharsets.UTF_8);
      byte[] result = new byte[bom.length + csv.length];
      System.arraycopy(bom, 0, result, 0, bom.length);
      System.arraycopy(csv, 0, result, bom.length, csv.length);
      return result;
    } catch (Exception e) {
      throw new ExternalServiceException("Failed to generate dashboard CSV", e);
    }
  }

  private static String[] row(String label, @Nullable BigDecimal value) {
    return new String[] {label, value != null ? value.toPlainString() : "N/A"};
  }
}
