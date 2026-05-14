package com.buurman.service.export.tabular;

/**
 * Logical column type that renderers translate to format-specific markup (CSV ignores it, Excel
 * applies a {@code DataFormat}, Google Sheets applies a {@code NumberFormat}).
 */
public enum TabularColumnFormat {
  TEXT,
  DATE,
  NUMBER,
  INTEGER,
  CURRENCY,
  PERCENT
}
