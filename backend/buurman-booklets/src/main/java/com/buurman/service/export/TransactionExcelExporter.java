package com.buurman.service.export;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.buurman.exception.ExternalServiceException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TransactionExcelExporter {

  private final TransactionDataLoader dataLoader;

  public byte[] generate(@Nullable LocalDate startDate, @Nullable LocalDate endDate, UUID teamId) {
    List<TransactionRecord> transactions = dataLoader.load(startDate, endDate, teamId);

    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream os = new ByteArrayOutputStream()) {

      Sheet sheet = workbook.createSheet("Transactions");

      Font boldFont = workbook.createFont();
      boldFont.setBold(true);
      CellStyle headerStyle = workbook.createCellStyle();
      headerStyle.setFont(boldFont);

      String[] headers = {
        "Date", "Type", "Description", "Property", "Category", "Amount", "Currency"
      };
      Row headerRow = sheet.createRow(0);
      for (int i = 0; i < headers.length; i++) {
        Cell cell = headerRow.createCell(i);
        cell.setCellValue(headers[i]);
        cell.setCellStyle(headerStyle);
      }

      int rowNum = 1;
      for (TransactionRecord t : transactions) {
        Row row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue(t.date().toString());
        row.createCell(1).setCellValue(t.type());
        row.createCell(2).setCellValue(t.description());
        row.createCell(3).setCellValue(t.property());
        row.createCell(4).setCellValue(t.category().orElse(""));
        row.createCell(5).setCellValue(t.amount().doubleValue());
        row.createCell(6).setCellValue(t.currency());
      }

      for (int i = 0; i < headers.length; i++) {
        sheet.autoSizeColumn(i);
      }

      workbook.write(os);
      return os.toByteArray();
    } catch (Exception e) {
      throw new ExternalServiceException("Failed to generate Excel", e);
    }
  }
}
