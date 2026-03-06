package com.buurman.service.export;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.buurman.dto.response.PortfolioDashboardResponse;
import com.buurman.dto.response.PortfolioDashboardResponse.PortfolioSummary;
import com.buurman.dto.response.PortfolioDashboardResponse.PropertyPerformance;
import com.buurman.dto.response.PropertyDashboardResponse.MonthlyDataPoint;
import com.buurman.exception.ExternalServiceException;
import com.opencsv.CSVWriter;

@Component
public class PortfolioDashboardCsvExporter {

  public byte[] generate(PortfolioDashboardResponse dashboard) {
    try (StringWriter sw = new StringWriter();
        CSVWriter writer = new CSVWriter(sw)) {

      PortfolioSummary s = dashboard.summary();

      // Summary section
      writer.writeNext(new String[] {"--- Portfolio Summary Metrics ---"});
      writer.writeNext(new String[] {"Metric", "Value"});
      writer.writeNext(new String[] {"Currency", dashboard.currency().orElse("N/A")});
      writer.writeNext(
          new String[] {
            "Properties with Financial Data",
            dashboard.propertiesWithFinancialData() + " / " + dashboard.totalProperties()
          });
      writer.writeNext(row("Total Portfolio Value", s.totalPortfolioValue().orElse(null)));
      writer.writeNext(row("Total Equity", s.totalEquity().orElse(null)));
      writer.writeNext(row("Monthly Cash Flow", s.monthlyCashFlow().orElse(null)));
      writer.writeNext(row("Annual NOI", s.annualNoi().orElse(null)));
      writer.writeNext(row("Weighted Cap Rate %", s.weightedCapRate().orElse(null)));
      writer.writeNext(row("Weighted Cash-on-Cash %", s.weightedCashOnCash().orElse(null)));
      writer.writeNext(row("Portfolio Occupancy %", s.portfolioOccupancy().orElse(null)));
      writer.writeNext(row("Debt-to-Equity", s.debtToEquity().orElse(null)));
      writer.writeNext(row("DSCR", s.portfolioDscr().orElse(null)));
      writer.writeNext(row("Income Concentration %", s.incomeConcentration().orElse(null)));
      writer.writeNext(row("Data Completeness %", s.dataCompleteness().orElse(null)));
      writer.writeNext(new String[] {""});

      // Monthly cash flow
      writer.writeNext(new String[] {"--- Aggregated Monthly Cash Flow ---"});
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

      // Property comparison
      writer.writeNext(new String[] {"--- Property Comparison ---"});
      writer.writeNext(
          new String[] {
            "Property",
            "Category",
            "Monthly Cash Flow",
            "Annual NOI",
            "Cap Rate %",
            "Cash-on-Cash %",
            "Occupancy %",
            "Data Completeness %",
            "Currency",
            "Currency Mismatch"
          });
      for (PropertyPerformance pp : dashboard.propertyComparison()) {
        writer.writeNext(
            new String[] {
              pp.address(),
              pp.category(),
              pp.monthlyCashFlow().map(BigDecimal::toPlainString).orElse("N/A"),
              pp.annualNoi().map(BigDecimal::toPlainString).orElse("N/A"),
              pp.capRate().map(BigDecimal::toPlainString).orElse("N/A"),
              pp.cashOnCash().map(BigDecimal::toPlainString).orElse("N/A"),
              pp.occupancyRate().map(BigDecimal::toPlainString).orElse("N/A"),
              String.valueOf(pp.completenessPercent()),
              pp.currency().orElse("N/A"),
              String.valueOf(pp.currencyMismatch())
            });
      }

      // UTF-8 BOM for Excel compatibility
      byte[] bom = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
      byte[] csv = sw.toString().getBytes(StandardCharsets.UTF_8);
      byte[] result = new byte[bom.length + csv.length];
      System.arraycopy(bom, 0, result, 0, bom.length);
      System.arraycopy(csv, 0, result, bom.length, csv.length);
      return result;
    } catch (Exception e) {
      throw new ExternalServiceException("Failed to generate portfolio dashboard CSV", e);
    }
  }

  private static String[] row(String label, @Nullable BigDecimal value) {
    return new String[] {label, value != null ? value.toPlainString() : "N/A"};
  }
}
