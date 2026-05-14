package com.buurman.service.export.tabular;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.buurman.domain.GoogleAccessToken;
import com.buurman.domain.GoogleSheetExport;
import com.buurman.service.export.google.GoogleSheetBuilder;
import com.buurman.service.export.google.GoogleSheetExportPipeline;

import lombok.RequiredArgsConstructor;

/**
 * Renders a {@link TabularExport} into a new Google Sheet in the user's Drive. Maps the logical
 * {@link TabularColumnFormat} to the Google Sheets number-format equivalent and coerces cell values
 * to their native Java types (so column formats actually take effect in Sheets).
 */
@Component
@RequiredArgsConstructor
public class SheetsRenderer {

  private final GoogleSheetExportPipeline pipeline;

  public GoogleSheetExport render(TabularExport export, GoogleAccessToken token, String baseTitle) {
    return pipeline.export(
        token,
        baseTitle,
        builder -> {
          for (TabularSheet sheet : export.sheets()) {
            GoogleSheetBuilder.TabSpec tab = builder.tab(sheet.title());
            tab.headers(headerLabels(sheet));
            for (int i = 0; i < sheet.columns().size(); i++) {
              tab.columnFormat(i, mapFormat(sheet.columns().get(i).format()));
            }
            for (Object[] row : sheet.rows()) {
              Object[] coerced = new Object[row.length];
              for (int i = 0; i < row.length; i++) {
                coerced[i] = coerce(row[i]);
              }
              tab.row(coerced);
            }
          }
        });
  }

  private static String[] headerLabels(TabularSheet sheet) {
    String[] out = new String[sheet.columns().size()];
    for (int i = 0; i < sheet.columns().size(); i++) {
      out[i] = sheet.columns().get(i).header();
    }
    return out;
  }

  private static GoogleSheetBuilder.ColumnFormat mapFormat(TabularColumnFormat format) {
    return switch (format) {
      case TEXT -> GoogleSheetBuilder.ColumnFormat.TEXT;
      case DATE -> GoogleSheetBuilder.ColumnFormat.DATE;
      case NUMBER -> GoogleSheetBuilder.ColumnFormat.NUMBER;
      case INTEGER -> GoogleSheetBuilder.ColumnFormat.INTEGER;
      case CURRENCY -> GoogleSheetBuilder.ColumnFormat.CURRENCY;
      case PERCENT -> GoogleSheetBuilder.ColumnFormat.PERCENT;
    };
  }

  /** Convert builder-output values to types Sheets renders natively. */
  private static Object coerce(Object value) {
    if (value == null) {
      return "";
    }
    if (value instanceof Optional<?> opt) {
      return opt.map(SheetsRenderer::coerce).orElse("");
    }
    if (value instanceof BigDecimal bd) {
      return bd.doubleValue();
    }
    if (value instanceof Number n) {
      return n.doubleValue();
    }
    if (value instanceof LocalDate d) {
      return d.toString();
    }
    if (value instanceof LocalDateTime dt) {
      return dt.toString();
    }
    if (value instanceof Instant i) {
      return i.toString();
    }
    if (value instanceof OffsetDateTime odt) {
      return odt.toString();
    }
    if (value instanceof Boolean b) {
      return b;
    }
    return value.toString();
  }
}
