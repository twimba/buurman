package com.buurman.service.export.tabular;

import java.util.ArrayList;
import java.util.List;

/**
 * One section of a {@link TabularExport}: a title, a list of columns, and rows of cell values.
 * Rendered as a worksheet in Excel/Sheets and as a section separator + table in CSV.
 *
 * <p>Cell values may be {@code null} (rendered as empty), {@link Number}, {@link Boolean}, {@link
 * java.time.LocalDate}, or any other object (rendered via {@code toString()}). Renderers pick the
 * native representation matching the column's {@link TabularColumnFormat}.
 */
public final class TabularSheet {

  private final String title;
  private final List<TabularColumn> columns;
  private final List<Object[]> rows = new ArrayList<>();

  public TabularSheet(String title, List<TabularColumn> columns) {
    this.title = title;
    this.columns = List.copyOf(columns);
  }

  public TabularSheet addRow(Object... cells) {
    if (cells.length != columns.size()) {
      throw new IllegalArgumentException(
          "Row width "
              + cells.length
              + " does not match "
              + columns.size()
              + " columns in sheet '"
              + title
              + "'");
    }
    rows.add(cells);
    return this;
  }

  public String title() {
    return title;
  }

  public List<TabularColumn> columns() {
    return columns;
  }

  public List<Object[]> rows() {
    return rows;
  }
}
