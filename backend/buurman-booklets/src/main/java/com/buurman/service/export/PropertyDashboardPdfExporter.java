package com.buurman.service.export;

import static com.buurman.service.export.BookletHelper.escapeHtml;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.buurman.dto.response.PropertyDashboardResponse;
import com.buurman.dto.response.PropertyDashboardResponse.CategorySlice;
import com.buurman.dto.response.PropertyDashboardResponse.MonthlyDataPoint;
import com.buurman.dto.response.PropertyDashboardResponse.SummaryMetrics;
import com.buurman.repository.TeamPreferencesRepository;
import com.buurman.util.CurrencyUtils;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PropertyDashboardPdfExporter {

  private final DocumentRenderer pdfRenderer;
  private final TeamPreferencesRepository teamPreferencesRepository;

  public byte[] generate(PropertyDashboardResponse dashboard, UUID teamId) {
    String teamCurrency = teamPreferencesRepository.getByTeamId(teamId).getDefaultCurrency();
    String html = buildHtml(dashboard, teamCurrency);
    return pdfRenderer.render(html);
  }

  private String buildHtml(PropertyDashboardResponse dashboard, String teamCurrency) {
    SummaryMetrics s = dashboard.summary();
    String currency = s.currency().orElse(teamCurrency);

    StringBuilder html = new StringBuilder();
    html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'/><style>");
    appendCss(html);
    html.append("</style></head><body>");

    // Title
    html.append("<h1>Property Investment Dashboard</h1>");

    // Summary metrics
    html.append("<div class='summary'>");
    metricCard(html, "Total ROI", fmtPct(s.totalRoiPercent().orElse(null)));
    metricCard(html, "Annualized ROI", fmtPct(s.annualizedRoiPercent().orElse(null)));
    metricCard(html, "Cap Rate", fmtPct(s.capRatePercent().orElse(null)));
    metricCard(html, "Cash-on-Cash", fmtPct(s.cashOnCashPercent().orElse(null)));
    metricCard(html, "Monthly Cash Flow", fmtMoney(s.monthlyCashFlow().orElse(null), currency));
    metricCard(html, "Annual NOI", fmtMoney(s.annualNoi().orElse(null), currency));
    metricCard(html, "Total Equity", fmtMoney(s.totalEquity().orElse(null), currency));
    metricCard(html, "Equity Growth", fmtPct(s.equityGrowthPercent().orElse(null)));
    metricCard(html, "Occupancy", fmtPct(s.occupancyRatePercent().orElse(null)));
    metricCard(html, "GRM", s.grossRentMultiplier().map(v -> v + "x").orElse("N/A"));
    html.append("</div>");

    // Cash flow table
    html.append("<h2>Monthly Cash Flow (Last 12 Months)</h2>");
    html.append(
        "<table><thead><tr><th>Month</th><th>Income</th><th>Expenses</th><th>Mortgage</th><th>Net</th></tr></thead><tbody>");
    for (MonthlyDataPoint m : dashboard.cashFlow().months()) {
      html.append("<tr>");
      html.append("<td>").append(escapeHtml(m.month())).append("</td>");
      html.append("<td class='right'>").append(fmtMoney(m.income(), currency)).append("</td>");
      html.append("<td class='right'>").append(fmtMoney(m.expenses(), currency)).append("</td>");
      html.append("<td class='right'>").append(fmtMoney(m.mortgage(), currency)).append("</td>");
      html.append("<td class='right ")
          .append(m.net().signum() >= 0 ? "positive" : "negative")
          .append("'>")
          .append(fmtMoney(m.net(), currency))
          .append("</td>");
      html.append("</tr>");
    }
    html.append("</tbody></table>");

    // Expense breakdown
    if (!dashboard.expenseBreakdown().categories().isEmpty()) {
      html.append("<h2>Expense Breakdown</h2>");
      html.append("<table><thead><tr><th>Category</th><th>Amount</th></tr></thead><tbody>");
      for (CategorySlice c : dashboard.expenseBreakdown().categories()) {
        html.append("<tr><td>").append(escapeHtml(humanize(c.category()))).append("</td>");
        html.append("<td class='right'>")
            .append(fmtMoney(c.amount(), currency))
            .append("</td></tr>");
      }
      html.append("</tbody></table>");
    }

    // Equity
    html.append("<h2>Equity Overview</h2>");
    html.append("<table><thead><tr><th>Item</th><th>Amount</th></tr></thead><tbody>");
    equityRow(html, "Purchase Price", dashboard.equity().purchasePrice().orElse(null), currency);
    equityRow(
        html,
        "Current Market Value",
        dashboard.equity().currentMarketValue().orElse(null),
        currency);
    equityRow(
        html, "Mortgage Balance", dashboard.equity().mortgageBalance().orElse(null), currency);
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

  private void equityRow(
      StringBuilder html, String label, @Nullable BigDecimal value, String currency) {
    html.append("<tr><td>").append(escapeHtml(label)).append("</td>");
    html.append("<td class='right'>").append(fmtMoney(value, currency)).append("</td></tr>");
  }

  private static String fmtPct(@Nullable BigDecimal value) {
    return value != null ? value.toPlainString() + "%" : "N/A";
  }

  private static String fmtMoney(@Nullable BigDecimal value, String currencyCode) {
    return CurrencyUtils.formatCurrency(value, currencyCode);
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
        body { font-family: 'Satoshi', 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size: 11px; color: #292524; padding: 20px; }
        h1 { font-size: 20px; margin-bottom: 16px; color: #292524; }
        h2 { font-size: 14px; margin: 20px 0 8px 0; color: #44403c; border-bottom: 1px solid #e7e5e4; padding-bottom: 4px; }
        .summary { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 20px; }
        .metric-card { border: 1px solid #e7e5e4; border-radius: 6px; padding: 8px 12px; min-width: 100px; }
        .metric-label { font-size: 9px; color: #78716c; text-transform: uppercase; }
        .metric-value { font-size: 14px; font-weight: bold; color: #292524; }
        table { width: 100%; border-collapse: collapse; margin-bottom: 16px; }
        th { background: #f5f5f4; text-align: left; padding: 6px 8px; font-size: 10px; color: #44403c; }
        td { padding: 5px 8px; border-bottom: 1px solid #f5f5f4; }
        .right { text-align: right; }
        .positive { color: #10B981; }
        .negative { color: #EF4444; }
        """);
  }
}
