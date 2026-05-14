package com.buurman.service.export.google;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.buurman.domain.GoogleSheetExport;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.AddBandingRequest;
import com.google.api.services.sheets.v4.model.AutoResizeDimensionsRequest;
import com.google.api.services.sheets.v4.model.BandedRange;
import com.google.api.services.sheets.v4.model.BasicFilter;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateValuesRequest;
import com.google.api.services.sheets.v4.model.CellFormat;
import com.google.api.services.sheets.v4.model.Color;
import com.google.api.services.sheets.v4.model.DimensionRange;
import com.google.api.services.sheets.v4.model.GridProperties;
import com.google.api.services.sheets.v4.model.GridRange;
import com.google.api.services.sheets.v4.model.NumberFormat;
import com.google.api.services.sheets.v4.model.RepeatCellRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.SetBasicFilterRequest;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.SpreadsheetProperties;
import com.google.api.services.sheets.v4.model.TextFormat;
import com.google.api.services.sheets.v4.model.ValueRange;

/**
 * Builder for multi-tab Google Sheets with rich formatting (frozen header rows, bold + tinted
 * header cells, banded rows, currency / date / number columns, auto-resized columns).
 *
 * <p>Usage:
 *
 * <pre>{@code
 * GoogleSheetBuilder builder = new GoogleSheetBuilder("Buurman — Contacts — 2026-05-14");
 * builder
 *     .tab("Contacts")
 *     .headers("Name", "Email", "Phone")
 *     .row("Alice", "alice@example.com", "555-0100")
 *     .row("Bob", "bob@example.com", "555-0200");
 * GoogleSheetExport result = builder.create(sheets);
 * }</pre>
 */
public final class GoogleSheetBuilder {

  /** Supported column formats. Maps to Google Sheets numberFormat patterns. */
  public enum ColumnFormat {
    /** No special formatting; values rendered as-is. */
    TEXT,
    /** ISO date YYYY-MM-DD. */
    DATE,
    /** Plain number with up to 2 decimal places. */
    NUMBER,
    /** Currency with 2 decimal places, locale-neutral; the currency code is taken from values. */
    CURRENCY,
    /** Integer (no decimals). */
    INTEGER,
    /** Percentage, two decimal places. */
    PERCENT
  }

  private final String title;
  private final Map<String, TabSpec> tabs = new LinkedHashMap<>();

  public GoogleSheetBuilder(String title) {
    this.title = title;
  }

  public TabSpec tab(String tabTitle) {
    TabSpec t = new TabSpec(tabTitle);
    tabs.put(tabTitle, t);
    return t;
  }

  public GoogleSheetExport create(Sheets sheets) {
    if (tabs.isEmpty()) {
      throw new IllegalStateException("Cannot create a Google Sheet with no tabs");
    }
    try {
      Spreadsheet spreadsheet = buildSpreadsheetShell();
      Spreadsheet created = sheets.spreadsheets().create(spreadsheet).execute();
      String spreadsheetId = created.getSpreadsheetId();

      Map<String, Integer> sheetIds = new LinkedHashMap<>();
      for (Sheet s : created.getSheets()) {
        sheetIds.put(s.getProperties().getTitle(), s.getProperties().getSheetId());
      }

      writeValues(sheets, spreadsheetId);
      applyFormatting(sheets, spreadsheetId, sheetIds);

      String url = "https://docs.google.com/spreadsheets/d/" + spreadsheetId + "/edit";
      return new GoogleSheetExport(spreadsheetId, url);
    } catch (IOException e) {
      throw GoogleSheetsClientFactory.translate("creating Google Sheet", e);
    }
  }

  private Spreadsheet buildSpreadsheetShell() {
    List<Sheet> sheetShells = new ArrayList<>();
    int sheetIndex = 0;
    for (TabSpec tab : tabs.values()) {
      SheetProperties props = new SheetProperties();
      props.setTitle(tab.title);
      props.setIndex(sheetIndex++);

      GridProperties grid = new GridProperties();
      grid.setFrozenRowCount(1);
      grid.setRowCount(Math.max(tab.rows.size() + 10, 100));
      grid.setColumnCount(Math.max(tab.headers.size(), 26));
      props.setGridProperties(grid);

      Sheet sheet = new Sheet();
      sheet.setProperties(props);
      sheetShells.add(sheet);
    }

    SpreadsheetProperties spreadsheetProps = new SpreadsheetProperties();
    spreadsheetProps.setTitle(title);

    Spreadsheet spreadsheet = new Spreadsheet();
    spreadsheet.setProperties(spreadsheetProps);
    spreadsheet.setSheets(sheetShells);
    return spreadsheet;
  }

  /**
   * Target cell count per Sheets {@code values.batchUpdate} request. Sheets caps request bodies
   * around 10 MB; at ~25 bytes/cell JSON-encoded that's a theoretical ceiling of ~400k cells per
   * request. We sit comfortably below that — large enough to amortise round-trips for typical
   * exports, small enough that one slow request can't time out the whole takeout.
   *
   * <p>Each chunk also keeps each individual HTTP call short enough to fit inside the backend's
   * 60-second per-call window and to make exponential-backoff retries cheap when Google returns a
   * transient 429/5xx.
   */
  private static final int TARGET_CELLS_PER_REQUEST = 50_000;

  /**
   * Writes all tab values in chunks. The naive implementation sent one batchUpdate-values request
   * carrying every cell of every tab — fine for per-entity exports (hundreds of cells) but brittle
   * for takeout exports (potentially millions of cells across 22 tabs). This version groups {@link
   * ValueRange}s into requests of at most {@link #TARGET_CELLS_PER_REQUEST} cells, splitting an
   * individual tab's rows across multiple sequential ranges when needed.
   */
  private void writeValues(Sheets sheets, String spreadsheetId) throws IOException {
    List<ValueRange> buffer = new ArrayList<>();
    int bufferCells = 0;

    for (TabSpec tab : tabs.values()) {
      int columnCount = Math.max(1, tab.headers.size());
      int rowsPerChunk = Math.max(1, TARGET_CELLS_PER_REQUEST / columnCount);

      List<List<Object>> tabRows = new ArrayList<>(tab.rows.size() + 1);
      tabRows.add(new ArrayList<>(tab.headers));
      tabRows.addAll(tab.rows);

      for (int start = 0; start < tabRows.size(); start += rowsPerChunk) {
        int end = Math.min(start + rowsPerChunk, tabRows.size());
        List<List<Object>> chunk = tabRows.subList(start, end);
        String range = "'" + tab.title.replace("'", "''") + "'!A" + (start + 1);
        int chunkCells = chunk.size() * columnCount;

        if (!buffer.isEmpty() && bufferCells + chunkCells > TARGET_CELLS_PER_REQUEST) {
          flushValues(sheets, spreadsheetId, buffer);
          buffer = new ArrayList<>();
          bufferCells = 0;
        }
        buffer.add(new ValueRange().setRange(range).setValues(new ArrayList<>(chunk)));
        bufferCells += chunkCells;
      }
    }
    if (!buffer.isEmpty()) {
      flushValues(sheets, spreadsheetId, buffer);
    }
  }

  private static void flushValues(Sheets sheets, String spreadsheetId, List<ValueRange> data)
      throws IOException {
    BatchUpdateValuesRequest body =
        new BatchUpdateValuesRequest().setValueInputOption("USER_ENTERED").setData(data);
    sheets.spreadsheets().values().batchUpdate(spreadsheetId, body).execute();
  }

  private void applyFormatting(Sheets sheets, String spreadsheetId, Map<String, Integer> sheetIds)
      throws IOException {
    List<Request> requests = new ArrayList<>();
    for (TabSpec tab : tabs.values()) {
      Integer sheetId = sheetIds.get(tab.title);
      if (sheetId == null) {
        continue;
      }
      requests.addAll(formatHeaderRow(sheetId, tab.headers.size()));
      if (tab.bandedRows && !tab.rows.isEmpty()) {
        requests.add(bandedRange(sheetId, tab.headers.size(), tab.rows.size()));
      }
      requests.addAll(applyColumnFormats(sheetId, tab));
      requests.add(autoResizeColumns(sheetId, tab.headers.size()));
      if (!tab.rows.isEmpty()) {
        requests.add(basicFilter(sheetId, tab.headers.size(), tab.rows.size()));
      }
    }
    if (!requests.isEmpty()) {
      BatchUpdateSpreadsheetRequest body =
          new BatchUpdateSpreadsheetRequest().setRequests(requests);
      sheets.spreadsheets().batchUpdate(spreadsheetId, body).execute();
    }
  }

  private static List<Request> formatHeaderRow(int sheetId, int columnCount) {
    Color headerBg = color(0.13f, 0.30f, 0.50f);
    Color headerFg = color(1.0f, 1.0f, 1.0f);

    TextFormat textFormat = new TextFormat();
    textFormat.setBold(true);
    textFormat.setForegroundColor(headerFg);

    CellFormat headerFormat = new CellFormat();
    headerFormat.setBackgroundColor(headerBg);
    headerFormat.setTextFormat(textFormat);
    headerFormat.setHorizontalAlignment("LEFT");

    RepeatCellRequest repeat = new RepeatCellRequest();
    repeat.setRange(
        new GridRange()
            .setSheetId(sheetId)
            .setStartRowIndex(0)
            .setEndRowIndex(1)
            .setStartColumnIndex(0)
            .setEndColumnIndex(columnCount));
    repeat.setCell(
        new com.google.api.services.sheets.v4.model.CellData().setUserEnteredFormat(headerFormat));
    repeat.setFields("userEnteredFormat(backgroundColor,textFormat,horizontalAlignment)");

    return List.of(new Request().setRepeatCell(repeat));
  }

  private static Request bandedRange(int sheetId, int columnCount, int dataRowCount) {
    BandedRange band = new BandedRange();
    band.setRange(
        new GridRange()
            .setSheetId(sheetId)
            .setStartRowIndex(0)
            .setEndRowIndex(dataRowCount + 1)
            .setStartColumnIndex(0)
            .setEndColumnIndex(columnCount));
    com.google.api.services.sheets.v4.model.BandingProperties rowProps =
        new com.google.api.services.sheets.v4.model.BandingProperties();
    rowProps.setHeaderColor(color(0.13f, 0.30f, 0.50f));
    rowProps.setFirstBandColor(color(1.0f, 1.0f, 1.0f));
    rowProps.setSecondBandColor(color(0.95f, 0.96f, 0.98f));
    band.setRowProperties(rowProps);
    return new Request().setAddBanding(new AddBandingRequest().setBandedRange(band));
  }

  private static List<Request> applyColumnFormats(int sheetId, TabSpec tab) {
    List<Request> requests = new ArrayList<>();
    for (int colIndex = 0; colIndex < tab.columnFormats.size(); colIndex++) {
      ColumnFormat fmt = tab.columnFormats.get(colIndex);
      if (fmt == ColumnFormat.TEXT) {
        continue;
      }
      NumberFormat numberFormat = numberFormatFor(fmt);
      CellFormat cellFormat = new CellFormat().setNumberFormat(numberFormat);
      RepeatCellRequest repeat = new RepeatCellRequest();
      repeat.setRange(
          new GridRange()
              .setSheetId(sheetId)
              .setStartRowIndex(1)
              .setStartColumnIndex(colIndex)
              .setEndColumnIndex(colIndex + 1));
      repeat.setCell(
          new com.google.api.services.sheets.v4.model.CellData().setUserEnteredFormat(cellFormat));
      repeat.setFields("userEnteredFormat.numberFormat");
      requests.add(new Request().setRepeatCell(repeat));
    }
    return requests;
  }

  private static NumberFormat numberFormatFor(ColumnFormat fmt) {
    NumberFormat nf = new NumberFormat();
    switch (fmt) {
      case DATE -> {
        nf.setType("DATE");
        nf.setPattern("yyyy-mm-dd");
      }
      case NUMBER -> {
        nf.setType("NUMBER");
        nf.setPattern("#,##0.00");
      }
      case CURRENCY -> {
        nf.setType("CURRENCY");
        nf.setPattern("#,##0.00");
      }
      case INTEGER -> {
        nf.setType("NUMBER");
        nf.setPattern("#,##0");
      }
      case PERCENT -> {
        nf.setType("PERCENT");
        nf.setPattern("0.00%");
      }
      case TEXT -> throw new IllegalArgumentException("TEXT format has no number format");
    }
    return nf;
  }

  /**
   * Installs a Sheets <em>Basic Filter</em> over the header row + data range, so the user gets the
   * per-column filter / sort dropdowns automatically — the same affordance as <em>Data → Create a
   * filter</em> in the Sheets UI. Skipped when a tab has no data rows.
   */
  private static Request basicFilter(int sheetId, int columnCount, int dataRowCount) {
    BasicFilter filter = new BasicFilter();
    filter.setRange(
        new GridRange()
            .setSheetId(sheetId)
            .setStartRowIndex(0)
            .setEndRowIndex(dataRowCount + 1)
            .setStartColumnIndex(0)
            .setEndColumnIndex(columnCount));
    return new Request().setSetBasicFilter(new SetBasicFilterRequest().setFilter(filter));
  }

  private static Request autoResizeColumns(int sheetId, int columnCount) {
    AutoResizeDimensionsRequest auto = new AutoResizeDimensionsRequest();
    auto.setDimensions(
        new DimensionRange()
            .setSheetId(sheetId)
            .setDimension("COLUMNS")
            .setStartIndex(0)
            .setEndIndex(columnCount));
    return new Request().setAutoResizeDimensions(auto);
  }

  private static Color color(float r, float g, float b) {
    Color c = new Color();
    c.setRed(r);
    c.setGreen(g);
    c.setBlue(b);
    return c;
  }

  /** Per-tab data + formatting accumulator. */
  public static final class TabSpec {
    private final String title;
    private final List<String> headers = new ArrayList<>();
    private final List<List<Object>> rows = new ArrayList<>();
    private final List<ColumnFormat> columnFormats = new ArrayList<>();
    private boolean bandedRows = true;

    private TabSpec(String title) {
      this.title = title;
    }

    public TabSpec headers(String... headerLabels) {
      headers.clear();
      for (String h : headerLabels) {
        headers.add(h);
      }
      while (columnFormats.size() < headers.size()) {
        columnFormats.add(ColumnFormat.TEXT);
      }
      return this;
    }

    public TabSpec row(Object... cells) {
      List<Object> r = new ArrayList<>(cells.length);
      for (Object c : cells) {
        r.add(c == null ? "" : c);
      }
      rows.add(r);
      return this;
    }

    public TabSpec columnFormat(int columnIndex, ColumnFormat format) {
      while (columnFormats.size() <= columnIndex) {
        columnFormats.add(ColumnFormat.TEXT);
      }
      columnFormats.set(columnIndex, format);
      return this;
    }

    public TabSpec banded(boolean enabled) {
      this.bandedRows = enabled;
      return this;
    }
  }
}
