package com.buurman.service.export.tabular;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.buurman.exception.ExternalServiceException;
import com.opencsv.CSVWriter;

/**
 * Renders a {@link TabularExport} as a single CSV byte stream. Multiple sheets are concatenated
 * with {@code --- {title} ---} section headers and a blank row between sections, matching the
 * existing Buurman CSV-export convention.
 */
@Component
public class CsvRenderer {

  public byte[] render(TabularExport export) {
    try (StringWriter sw = new StringWriter();
        CSVWriter writer = new CSVWriter(sw)) {
      boolean first = true;
      for (TabularSheet sheet : export.sheets()) {
        if (!first) {
          writer.writeNext(new String[] {""});
        }
        first = false;
        writer.writeNext(new String[] {"--- " + sheet.title() + " ---"});
        writer.writeNext(headerStrings(sheet.columns()));
        for (Object[] row : sheet.rows()) {
          writer.writeNext(rowStrings(row));
        }
      }
      writer.flush();
      return sw.toString().getBytes(StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new ExternalServiceException("Failed to generate CSV", e);
    }
  }

  private static String[] headerStrings(List<TabularColumn> columns) {
    String[] out = new String[columns.size()];
    for (int i = 0; i < columns.size(); i++) {
      out[i] = columns.get(i).header();
    }
    return out;
  }

  private static String[] rowStrings(Object[] row) {
    String[] out = new String[row.length];
    for (int i = 0; i < row.length; i++) {
      out[i] = stringify(row[i]);
    }
    return out;
  }

  private static String stringify(Object value) {
    if (value == null) {
      return "";
    }
    if (value instanceof Optional<?> opt) {
      return opt.map(CsvRenderer::stringify).orElse("");
    }
    if (value instanceof BigDecimal bd) {
      return bd.toPlainString();
    }
    if (value instanceof Number || value instanceof Boolean) {
      return value.toString();
    }
    return neutraliseFormula(value.toString());
  }

  /**
   * Text starting with a formula trigger ({@code = + - @}, tab or CR) would be evaluated by Excel
   * and LibreOffice when the CSV is opened; a leading apostrophe forces it to stay text. Negative
   * numbers rendered as strings are left alone.
   */
  static String neutraliseFormula(String text) {
    if (text.isEmpty()) {
      return text;
    }
    char first = text.charAt(0);
    boolean trigger =
        first == '='
            || first == '+'
            || first == '-'
            || first == '@'
            || first == '\t'
            || first == '\r';
    if (!trigger) {
      return text;
    }
    boolean signedNumber =
        (first == '-' || first == '+')
            && text.length() > 1
            && (Character.isDigit(text.charAt(1)) || text.charAt(1) == '.');
    return signedNumber ? text : "'" + text;
  }
}
