package com.buurman.service.export.tabular;

/** A column descriptor: human-readable header + logical format hint. */
public record TabularColumn(String header, TabularColumnFormat format) {
  public static TabularColumn text(String header) {
    return new TabularColumn(header, TabularColumnFormat.TEXT);
  }

  public static TabularColumn of(String header, TabularColumnFormat format) {
    return new TabularColumn(header, format);
  }
}
