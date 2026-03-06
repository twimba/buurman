package com.buurman.service.export;

import static com.buurman.service.export.BookletHelper.escapeHtml;

import java.math.BigDecimal;
import java.util.Locale;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.buurman.dto.response.PortfolioDashboardResponse;
import com.buurman.dto.response.PortfolioDashboardResponse.PortfolioSummary;
import com.buurman.dto.response.PortfolioDashboardResponse.PropertyPerformance;
import com.buurman.dto.response.PropertyDashboardResponse.MonthlyDataPoint;
import com.buurman.util.CurrencyUtils;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PortfolioDashboardPdfExporter {

  private final PdfRenderer pdfRenderer;

  public byte[] generate(PortfolioDashboardResponse dashboard) {
    String html = buildHtml(dashboard);
    return pdfRenderer.renderHtml(html);
  }

  private String buildHtml(PortfolioDashboardResponse dashboard) {
    PortfolioSummary s = dashboard.summary();
    String currency = dashboard.currency().orElse("EUR");

    StringBuilder html = new StringBuilder();
    html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'/><style>");
    appendCss(html);
    html.append("</style></head><body>");

    // Title
    html.append("<h1>Portfolio Investment Dashboard</h1>");
    html.append("<p class='subtitle'>")
        .append(dashboard.propertiesWithFinancialData())
        .append(" of ")
        .append(dashboard.totalProperties())
        .append(" properties with financial data</p>");

    // Summary metrics
    html.append("<div class='summary'>");
    metricCard(html, "Portfolio Value", fmtMoney(s.totalPortfolioValue().orElse(null), currency));
    metricCard(html, "Total Equity", fmtMoney(s.totalEquity().orElse(null), currency));
    metricCard(html, "Monthly Cash Flow", fmtMoney(s.monthlyCashFlow().orElse(null), currency));
    metricCard(html, "Annual NOI", fmtMoney(s.annualNoi().orElse(null), currency));
    metricCard(html, "Weighted Cap Rate", fmtPct(s.weightedCapRate().orElse(null)));
    metricCard(html, "Weighted Cash-on-Cash", fmtPct(s.weightedCashOnCash().orElse(null)));
    metricCard(html, "Occupancy", fmtPct(s.portfolioOccupancy().orElse(null)));
    metricCard(html, "Debt-to-Equity", fmtRatio(s.debtToEquity().orElse(null)));
    metricCard(html, "DSCR", fmtRatio(s.portfolioDscr().orElse(null)));
    metricCard(
        html, "Income Concentration", fmtPct(s.incomeConcentration().orElse(null)));
    metricCard(html, "Data Completeness", fmtPct(s.dataCompleteness().orElse(null)));
    html.append("</div>");

    // Property comparison table
    if (!dashboard.propertyComparison().isEmpty()) {
      html.append("<h2>Property Comparison</h2>");
      html.append(
          "<table><thead><tr><th>Property</th><th>Category</th><th>Cash Flow</th>"
              + "<th>Annual NOI</th><th>Cap Rate</th><th>Cash-on-Cash</th>"
              + "<th>Occupancy</th><th>Data %</th></tr></thead><tbody>");
      for (PropertyPerformance pp : dashboard.propertyComparison()) {
        html.append("<tr>");
        html.append("<td>").append(escapeHtml(pp.address())).append("</td>");
        html.append("<td>").append(escapeHtml(humanize(pp.category()))).append("</td>");
        String propCurrency = pp.currency().orElse(currency);
        html.append("<td class='right'>")
            .append(fmtMoney(pp.monthlyCashFlow().orElse(null), propCurrency))
            .append("</td>");
        html.append("<td class='right'>")
            .append(fmtMoney(pp.annualNoi().orElse(null), propCurrency))
            .append("</td>");
        html.append("<td class='right'>")
            .append(fmtPct(pp.capRate().orElse(null)))
            .append("</td>");
        html.append("<td class='right'>")
            .append(fmtPct(pp.cashOnCash().orElse(null)))
            .append("</td>");
        html.append("<td class='right'>")
            .append(fmtPct(pp.occupancyRate().orElse(null)))
            .append("</td>");
        html.append("<td class='right'>").append(pp.completenessPercent()).append("%</td>");
        html.append("</tr>");
      }
      html.append("</tbody></table>");
    }

    // Cash flow table
    if (!dashboard.cashFlow().months().isEmpty()) {
      html.append("<h2>Aggregated Monthly Cash Flow</h2>");
      html.append(
          "<table><thead><tr><th>Month</th><th>Income</th><th>Expenses</th>"
              + "<th>Mortgage</th><th>Net</th></tr></thead><tbody>");
      for (MonthlyDataPoint m : dashboard.cashFlow().months()) {
        html.append("<tr>");
        html.append("<td>").append(escapeHtml(m.month())).append("</td>");
        html.append("<td class='right'>").append(fmtMoney(m.income(), currency)).append("</td>");
        html.append("<td class='right'>")
            .append(fmtMoney(m.expenses(), currency))
            .append("</td>");
        html.append("<td class='right'>")
            .append(fmtMoney(m.mortgage(), currency))
            .append("</td>");
        html.append("<td class='right ")
            .append(m.net().signum() >= 0 ? "positive" : "negative")
            .append("'>")
            .append(fmtMoney(m.net(), currency))
            .append("</td>");
        html.append("</tr>");
      }
      html.append("</tbody></table>");
    }

    html.append("</body></html>");
    return html.toString();
  }

  private void metricCard(StringBuilder html, String label, String value) {
    html.append("<div class='metric-card'><div class='metric-label'>")
        .append(escapeHtml(label))
        .append("</div><div class='metric-value'>")
        .append(escapeHtml(value))
        .append("</div></div>");
  }

  private static String fmtPct(@Nullable BigDecimal value) {
    return value != null ? value.toPlainString() + "%" : "N/A";
  }

  private static String fmtRatio(@Nullable BigDecimal value) {
    return value != null ? value.toPlainString() : "N/A";
  }

  private static String fmtMoney(@Nullable BigDecimal value, @Nullable String currencyCode) {
    return CurrencyUtils.formatCurrency(value, currencyCode != null ? currencyCode : "EUR");
  }

  private static String humanize(@Nullable String enumValue) {
    if (enumValue == null || enumValue.isBlank()) {
      return enumValue != null ? enumValue : "";
    }
    String[] words = enumValue.split("_", -1);
    StringBuilder sb = new StringBuilder();
    for (String w : words) {
      if (!sb.isEmpty()) {
        sb.append(' ');
      }
      sb.append(w.substring(0, 1).toUpperCase(Locale.ROOT))
          .append(w.substring(1).toLowerCase(Locale.ROOT));
    }
    return sb.toString();
  }

  private void appendCss(StringBuilder css) {
    css.append(
        """
        body { font-family: 'Helvetica', sans-serif; font-size: 11px; color: #1a1d2e; padding: 20px; }
        h1 { font-size: 20px; margin-bottom: 4px; color: #1a1d2e; }
        h2 { font-size: 14px; margin: 20px 0 8px 0; color: #3d4463; border-bottom: 1px solid #e2e6f0; padding-bottom: 4px; }
        .subtitle { font-size: 11px; color: #6b7194; margin-bottom: 16px; }
        .summary { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 20px; }
        .metric-card { border: 1px solid #e2e6f0; border-radius: 6px; padding: 8px 12px; min-width: 100px; }
        .metric-label { font-size: 9px; color: #6b7194; text-transform: uppercase; }
        .metric-value { font-size: 14px; font-weight: bold; color: #1a1d2e; }
        table { width: 100%; border-collapse: collapse; margin-bottom: 16px; }
        th { background: #f1f3f9; text-align: left; padding: 6px 8px; font-size: 10px; color: #3d4463; }
        td { padding: 5px 8px; border-bottom: 1px solid #f1f3f9; }
        .right { text-align: right; }
        .positive { color: #10B981; }
        .negative { color: #EF4444; }
        """);
  }
}
