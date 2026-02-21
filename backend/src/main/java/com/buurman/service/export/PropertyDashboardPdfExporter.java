package com.buurman.service.export;

import static com.buurman.service.export.BookletHelper.escapeHtml;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.buurman.dto.response.PropertyDashboardResponse;
import com.buurman.dto.response.PropertyDashboardResponse.CategorySlice;
import com.buurman.dto.response.PropertyDashboardResponse.MonthlyDataPoint;
import com.buurman.dto.response.PropertyDashboardResponse.SummaryMetrics;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PropertyDashboardPdfExporter {

  private final PdfRenderer pdfRenderer;

  public byte[] generate(PropertyDashboardResponse dashboard) {
    String html = buildHtml(dashboard);
    return pdfRenderer.renderHtml(html);
  }

  private String buildHtml(PropertyDashboardResponse dashboard) {
    SummaryMetrics s = dashboard.summary();
    String currency = s.currency() != null ? escapeHtml(s.currency()) : "";

    StringBuilder html = new StringBuilder();
    html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'/><style>");
    appendCss(html);
    html.append("</style></head><body>");

    // Title
    html.append("<h1>Property Investment Dashboard</h1>");

    // Summary metrics
    html.append("<div class='summary'>");
    metricCard(html, "Total ROI", fmtPct(s.totalRoiPercent()));
    metricCard(html, "Annualized ROI", fmtPct(s.annualizedRoiPercent()));
    metricCard(html, "Cap Rate", fmtPct(s.capRatePercent()));
    metricCard(html, "Cash-on-Cash", fmtPct(s.cashOnCashPercent()));
    metricCard(html, "Monthly Cash Flow", fmtMoney(s.monthlyCashFlow(), currency));
    metricCard(html, "Annual NOI", fmtMoney(s.annualNoi(), currency));
    metricCard(html, "Total Equity", fmtMoney(s.totalEquity(), currency));
    metricCard(html, "Equity Growth", fmtPct(s.equityGrowthPercent()));
    metricCard(html, "Occupancy", fmtPct(s.occupancyRatePercent()));
    metricCard(
        html, "GRM", s.grossRentMultiplier() != null ? s.grossRentMultiplier() + "x" : "N/A");
    html.append("</div>");

    // Cash flow table
    html.append("<h2>Monthly Cash Flow (Last 12 Months)</h2>");
    html.append(
        "<table><thead><tr><th>Month</th><th>Income</th><th>Expenses</th><th>Mortgage</th><th>Net</th></tr></thead><tbody>");
    for (MonthlyDataPoint m : dashboard.cashFlow().months()) {
      html.append("<tr>");
      html.append("<td>").append(escapeHtml(m.month())).append("</td>");
      html.append("<td class='right'>").append(fmtNum(m.income())).append("</td>");
      html.append("<td class='right'>").append(fmtNum(m.expenses())).append("</td>");
      html.append("<td class='right'>").append(fmtNum(m.mortgage())).append("</td>");
      html.append("<td class='right ")
          .append(m.net().signum() >= 0 ? "positive" : "negative")
          .append("'>")
          .append(fmtNum(m.net()))
          .append("</td>");
      html.append("</tr>");
    }
    html.append("</tbody></table>");

    // Expense breakdown
    if (!dashboard.expenseBreakdown().categories().isEmpty()) {
      html.append("<h2>Expense Breakdown</h2>");
      html.append("<table><thead><tr><th>Category</th><th>Amount</th></tr></thead><tbody>");
      for (CategorySlice c : dashboard.expenseBreakdown().categories()) {
        html.append("<tr><td>").append(escapeHtml(c.category())).append("</td>");
        html.append("<td class='right'>").append(fmtNum(c.amount())).append("</td></tr>");
      }
      html.append("</tbody></table>");
    }

    // Equity
    html.append("<h2>Equity Overview</h2>");
    html.append("<table><thead><tr><th>Item</th><th>Amount</th></tr></thead><tbody>");
    equityRow(html, "Purchase Price", dashboard.equity().purchasePrice(), currency);
    equityRow(html, "Current Market Value", dashboard.equity().currentMarketValue(), currency);
    equityRow(html, "Mortgage Balance", dashboard.equity().mortgageBalance(), currency);
    html.append("</tbody></table>");

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

  private void equityRow(StringBuilder html, String label, BigDecimal value, String currency) {
    html.append("<tr><td>").append(escapeHtml(label)).append("</td>");
    html.append("<td class='right'>").append(fmtMoney(value, currency)).append("</td></tr>");
  }

  private static String fmtPct(BigDecimal value) {
    return value != null ? value.toPlainString() + "%" : "N/A";
  }

  private static String fmtMoney(BigDecimal value, String currency) {
    return value != null ? currency + " " + fmtNum(value) : "N/A";
  }

  private static String fmtNum(BigDecimal value) {
    return value != null ? String.format("%,.2f", value) : "N/A";
  }

  private void appendCss(StringBuilder css) {
    css.append(
        """
        body { font-family: 'Helvetica', sans-serif; font-size: 11px; color: #1a1d2e; padding: 20px; }
        h1 { font-size: 20px; margin-bottom: 16px; color: #1a1d2e; }
        h2 { font-size: 14px; margin: 20px 0 8px 0; color: #3d4463; border-bottom: 1px solid #e2e6f0; padding-bottom: 4px; }
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
