package com.buurman.service.export.tabular;

import java.util.ArrayList;
import java.util.List;

/**
 * Format-agnostic representation of an export: one or more {@link TabularSheet}s plus a default
 * filename stem (without extension) for byte-output renderers (CSV, Excel). The Sheets renderer
 * ignores the filename and uses a title supplied separately.
 *
 * <p>Builders produce one of these from domain data; renderers consume one and emit a concrete
 * format. This keeps CSV, Excel, and Google Sheets in lock-step — adding a column means editing
 * exactly one place (the builder), not three exporters.
 */
public final class TabularExport {

  private final String filenameStem;
  private final List<TabularSheet> sheets = new ArrayList<>();

  public TabularExport(String filenameStem) {
    this.filenameStem = filenameStem;
  }

  public TabularSheet addSheet(String title, List<TabularColumn> columns) {
    TabularSheet sheet = new TabularSheet(title, columns);
    sheets.add(sheet);
    return sheet;
  }

  public String filenameStem() {
    return filenameStem;
  }

  public List<TabularSheet> sheets() {
    return sheets;
  }
}
