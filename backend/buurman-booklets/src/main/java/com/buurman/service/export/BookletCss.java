package com.buurman.service.export;

/**
 * Composable CSS fragments for booklet PDFs. Each method returns a self-contained CSS block.
 * Exporters compose the CSS they need by concatenating the relevant fragments.
 *
 * <p>Usage example (property booklet):
 *
 * <pre>{@code
 * String css = BookletCss.base()
 *     + BookletCss.propertyStatusBadges()
 *     + BookletCss.checkItems()
 *     + BookletCss.paymentTable();
 * }</pre>
 */
final class BookletCss {

  private BookletCss() {}

  /**
   * Page layout, running footer, body, cover, content pages, sections, detail grids, text blocks.
   */
  static String base() {
    return """
    @page { margin: 40px 50px 70px 50px; \
    @bottom-center { content: element(running-footer); } \
    @bottom-right { content: 'Page ' counter(page) ' of ' counter(pages); font-size: 9px; \
    color: #a0aec0; font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; } }
    @page:first { margin: 0; @bottom-center { content: none; } @bottom-right { content: none; } }
    .running-footer { position: running(running-footer); width: 100%; border-top: 1px solid \
    #e2e8f0; padding-top: 8px; font-size: 9px; color: #a0aec0; }
    body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; margin: 0; padding: 0; \
    color: #1a202c; font-size: 13px; line-height: 1.5; }

    .cover { page-break-after: always; padding: 0; height: 100vh; display: flex; \
    flex-direction: column; justify-content: center; align-items: center; }
    .cover-header { text-align: center; margin-bottom: 40px; }
    .cover-title { font-size: 44px; font-weight: 700; color: #1a365d; letter-spacing: 3px; \
    margin-bottom: 12px; }
    .cover-subtitle { font-size: 16px; color: #718096; letter-spacing: 2px; margin-bottom: 6px; }
    .cover-date { font-size: 12px; color: #a0aec0; }
    .cover-summary { width: 80%; max-width: 520px; border-collapse: collapse; margin-top: 20px; }
    .cover-summary td { padding: 14px 20px; }
    .cs-label { font-size: 10px; text-transform: uppercase; letter-spacing: 1.2px; \
    color: #718096; font-weight: 600; margin-bottom: 4px; }
    .cs-value { font-size: 15px; color: #1a202c; font-weight: 500; }
    .cover-footer { position: absolute; bottom: 40px; text-align: center; font-size: 10px; \
    color: #a0aec0; width: 100%; }

    .status-badge { display: inline-block; padding: 8px 28px; border-radius: 20px; \
    font-size: 14px; font-weight: 700; letter-spacing: 1.5px; text-transform: uppercase; }

    .page { page-break-before: always; }
    .page-header { border-bottom: 2px solid #1a365d; padding-bottom: 10px; margin-bottom: 30px; \
    font-size: 20px; font-weight: 700; color: #1a365d; }

    .section-title { font-size: 16px; font-weight: 700; color: #1a365d; margin: 28px 0 14px 0; \
    padding-left: 12px; border-left: 4px solid #2b6cb0; }

    .detail-grid { width: 100%; border-collapse: collapse; }
    .detail-grid td { padding: 10px 16px; vertical-align: top; width: 50%; }
    .fg-label { font-size: 10px; text-transform: uppercase; letter-spacing: 1px; \
    color: #718096; font-weight: 600; margin-bottom: 3px; }
    .fg-value { font-size: 14px; color: #1a202c; }

    .text-block { background-color: #f7fafc; border: 1px solid #e2e8f0; border-radius: 6px; \
    padding: 16px; font-size: 13px; color: #2d3748; line-height: 1.6; margin-top: 8px; }
    .text-block p { margin: 0 0 8px 0; }
    .text-block ul, .text-block ol { margin: 4px 0; padding-left: 24px; }
    .text-block li { margin-bottom: 4px; }
    .text-block strong, .text-block b { font-weight: 600; }
    .text-block h1, .text-block h2, .text-block h3, .text-block h4 { color: #1a365d; \
    margin: 12px 0 6px 0; }
    """;
  }

  // ── Status badges (one method per booklet type) ─────────────────

  static String propertyStatusBadges() {
    return """
    .status-vacant { background-color: #f0fdf4; color: #166534; border: 2px solid #bbf7d0; }
    .status-occupied { background-color: #eff6ff; color: #1e40af; border: 2px solid #bfdbfe; }
    .status-maintenance { background-color: #fefce8; color: #854d0e; border: 2px solid #fef08a; }
    .status-unavailable { background-color: #f9fafb; color: #374151; border: 2px solid #e5e7eb; }
    """;
  }

  static String contractStatusBadges() {
    return """
    .status-active { background-color: #f0fdf4; color: #166534; border: 2px solid #bbf7d0; }
    .status-draft { background-color: #f9fafb; color: #374151; border: 2px solid #e5e7eb; }
    .status-expired { background-color: #fef2f2; color: #991b1b; border: 2px solid #fecaca; }
    .status-terminated { background-color: #fef2f2; color: #991b1b; border: 2px solid #fecaca; }
    .status-pending_signature { background-color: #fefce8; color: #854d0e; \
    border: 2px solid #fef08a; }
    """;
  }

  // ── Reusable component styles ───────────────────────────────────

  static String paymentTable() {
    return """
    .payment-table { width: 100%; border-collapse: collapse; margin-top: 10px; }
    .payment-table thead th { background-color: #edf2f7; padding: 10px 12px; text-align: left; \
    font-size: 10px; text-transform: uppercase; letter-spacing: 1px; color: #4a5568; \
    font-weight: 700; border-bottom: 2px solid #cbd5e0; }
    .payment-table tbody td { padding: 9px 12px; border-bottom: 1px solid #e2e8f0; \
    font-size: 13px; }
    .payment-table tbody tr:nth-child(even) { background-color: #f7fafc; }
    """;
  }

  static String paymentStatusBadges() {
    return """
    .pay-status { display: inline-block; padding: 3px 10px; border-radius: 4px; \
    font-size: 11px; font-weight: 600; letter-spacing: 0.3px; }
    .pay-paid { background-color: #f0fdf4; color: #166534; }
    .pay-pending { background-color: #fefce8; color: #854d0e; }
    .pay-partially_paid { background-color: #fefce8; color: #854d0e; }
    .pay-overdue { background-color: #fef2f2; color: #991b1b; }
    .pay-cancelled { background-color: #f9fafb; color: #6b7280; }
    """;
  }

  static String summaryGrid() {
    return """
    .summary-grid { width: 100%; border-collapse: separate; border-spacing: 10px 0; }
    .summary-grid td { border-radius: 8px; padding: 16px; text-align: center; \
    vertical-align: top; width: 25%; }
    .sc-label { font-size: 11px; text-transform: uppercase; letter-spacing: 1px; \
    font-weight: 600; margin-bottom: 6px; }
    .sc-amount { font-size: 20px; font-weight: 700; margin-bottom: 2px; }
    .sc-count { font-size: 11px; }
    """;
  }

  static String checkItems() {
    return """
    .check-item { padding: 6px 0; font-size: 13px; color: #2d3748; }
    .check-icon { color: #38a169; font-weight: bold; margin-right: 8px; }
    """;
  }

  static String partyCards() {
    return """
    .party-card { background-color: #fff; border: 1px solid #e2e8f0; border-left: 3px solid; \
    border-radius: 6px; padding: 20px 24px; margin-bottom: 14px; }
    .party-role { display: inline-block; font-size: 10px; font-weight: 600; \
    letter-spacing: 0.8px; text-transform: uppercase; padding: 2px 10px; border-radius: 3px; \
    margin-bottom: 6px; }
    .party-name { font-size: 20px; font-weight: 700; color: #1a202c; margin-bottom: 12px; \
    padding-bottom: 10px; border-bottom: 1px solid #edf2f7; }
    .party-details { border-collapse: collapse; width: 100%; }
    .pd-label { font-size: 10px; text-transform: uppercase; letter-spacing: 0.8px; \
    color: #a0aec0; font-weight: 600; padding: 4px 8px 4px 0; width: 90px; vertical-align: top; }
    .pd-value { font-size: 13px; color: #2d3748; padding: 4px 20px 4px 0; vertical-align: top; }
    """;
  }

  static String paymentInstructionCards() {
    return """
    .pi-card { background-color: #f7fafc; border: 1px solid #e2e8f0; border-left: 3px solid \
    #2b6cb0; border-radius: 6px; padding: 20px 24px; margin-bottom: 20px; }
    .pi-header { margin-bottom: 12px; padding-bottom: 10px; border-bottom: 1px solid #edf2f7; }
    .pi-name { font-size: 17px; font-weight: 700; color: #1a202c; margin-right: 12px; }
    .pi-method { display: inline-block; font-size: 10px; font-weight: 600; \
    letter-spacing: 0.8px; text-transform: uppercase; padding: 2px 10px; border-radius: 3px; \
    color: #2b6cb0; background-color: #eff6ff; vertical-align: middle; }
    .pi-current { display: inline-block; font-size: 10px; font-weight: 600; \
    letter-spacing: 0.8px; text-transform: uppercase; padding: 2px 10px; border-radius: 3px; \
    color: #166534; background-color: #f0fdf4; vertical-align: middle; margin-left: 8px; }
    .pi-period { font-size: 12px; color: #718096; margin-bottom: 12px; }
    .pi-details { margin-top: 10px; padding-top: 8px; border-top: 1px solid #edf2f7; }
    """;
  }
}
