package com.buurman.service.export.tabular;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.Optional;

import org.apache.poi.ss.usermodel.BuiltinFormats;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import com.buurman.exception.ExternalServiceException;

/**
 * Renders a {@link TabularExport} as an XLSX byte stream. One {@link TabularSheet} per Excel
 * worksheet; header row is bold + tinted; frozen at row 1; per-column number formats applied;
 * columns auto-sized.
 */
@Component
public class ExcelRenderer {

  public byte[] render(TabularExport export) {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream os = new ByteArrayOutputStream()) {
      CellStyle headerStyle = buildHeaderStyle(workbook);
      DataFormat dataFormat = workbook.createDataFormat();

      for (TabularSheet spec : export.sheets()) {
        Sheet sheet = workbook.createSheet(sanitizeSheetName(spec.title()));
        // Header row
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < spec.columns().size(); i++) {
          Cell cell = headerRow.createCell(i);
          cell.setCellValue(spec.columns().get(i).header());
          cell.setCellStyle(headerStyle);
        }
        sheet.createFreezePane(0, 1);

        // Per-column data style
        CellStyle[] columnStyles = new CellStyle[spec.columns().size()];
        for (int i = 0; i < spec.columns().size(); i++) {
          columnStyles[i] = buildColumnStyle(workbook, dataFormat, spec.columns().get(i).format());
        }

        // Data rows
        int rowIdx = 1;
        for (Object[] row : spec.rows()) {
          Row excelRow = sheet.createRow(rowIdx++);
          for (int i = 0; i < row.length; i++) {
            writeCell(excelRow.createCell(i), row[i], columnStyles[i]);
          }
        }

        for (int i = 0; i < spec.columns().size(); i++) {
          sheet.autoSizeColumn(i);
        }
      }

      workbook.write(os);
      return os.toByteArray();
    } catch (Exception e) {
      throw new ExternalServiceException("Failed to generate Excel", e);
    }
  }

  private static CellStyle buildHeaderStyle(XSSFWorkbook workbook) {
    Font bold = workbook.createFont();
    bold.setBold(true);
    bold.setColor(IndexedColors.WHITE.getIndex());
    CellStyle style = workbook.createCellStyle();
    style.setFont(bold);
    style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
    style.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
    return style;
  }

  private static CellStyle buildColumnStyle(
      XSSFWorkbook workbook, DataFormat dataFormat, TabularColumnFormat format) {
    CellStyle style = workbook.createCellStyle();
    switch (format) {
      case DATE -> style.setDataFormat(dataFormat.getFormat("yyyy-mm-dd"));
      case NUMBER -> style.setDataFormat(dataFormat.getFormat("#,##0.00"));
      case CURRENCY -> style.setDataFormat(dataFormat.getFormat("#,##0.00"));
      case INTEGER -> style.setDataFormat((short) BuiltinFormats.getBuiltinFormat("#,##0"));
      case PERCENT -> style.setDataFormat(dataFormat.getFormat("0.00%"));
      case TEXT -> {
        // default; no format
      }
    }
    return style;
  }

  private static void writeCell(Cell cell, Object value, CellStyle style) {
    cell.setCellStyle(style);
    if (value == null) {
      cell.setBlank();
      return;
    }
    if (value instanceof Optional<?> opt) {
      if (opt.isPresent()) {
        writeCell(cell, opt.get(), style);
      } else {
        cell.setBlank();
      }
      return;
    }
    if (value instanceof Number n) {
      cell.setCellValue(n.doubleValue());
      return;
    }
    if (value instanceof BigDecimal bd) {
      cell.setCellValue(bd.doubleValue());
      return;
    }
    if (value instanceof Boolean b) {
      cell.setCellValue(b);
      return;
    }
    cell.setCellValue(value.toString());
  }

  private static String sanitizeSheetName(String name) {
    // Excel: 31 char max, no [ ] : * ? / \
    String cleaned = name.replaceAll("[\\[\\]:*?/\\\\]", " ");
    return cleaned.length() > 31 ? cleaned.substring(0, 31) : cleaned;
  }
}
