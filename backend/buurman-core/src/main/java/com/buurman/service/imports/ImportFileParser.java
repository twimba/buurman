package com.buurman.service.imports;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.buurman.exception.BadRequestException;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

@Component
public class ImportFileParser {

  private static final String FORMAT_CSV = "CSV";
  private static final String FORMAT_XLSX = "XLSX";

  public record ParsedFile(
      List<String> columns,
      List<Map<String, String>> rows,
      String fileFormat) {}

  public ParsedFile parse(MultipartFile file) {
    String filename = file.getOriginalFilename();
    if (filename == null || filename.isBlank()) {
      throw new BadRequestException("File name is required");
    }

    String lower = filename.toLowerCase(Locale.ROOT);
    if (lower.endsWith(".csv")) {
      return parseCsv(file);
    } else if (lower.endsWith(".xlsx")) {
      return parseXlsx(file);
    } else {
      throw new BadRequestException("Unsupported file format. Accepted: CSV, XLSX");
    }
  }

  public String detectFormat(MultipartFile file) {
    String filename = file.getOriginalFilename();
    if (filename != null && filename.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
      return FORMAT_XLSX;
    }
    return FORMAT_CSV;
  }

  private static final String UTF8_BOM = "\uFEFF";

  private ParsedFile parseCsv(MultipartFile file) {
    try (InputStream is = file.getInputStream();
        CSVReader reader = new CSVReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

      List<String[]> allLines = reader.readAll();
      if (allLines.isEmpty()) {
        throw new BadRequestException("CSV file is empty");
      }

      String[] headerRow = allLines.get(0);
      List<String> columns = new ArrayList<>();
      for (int i = 0; i < headerRow.length; i++) {
        String col = headerRow[i].trim();
        // Strip UTF-8 BOM from first column
        if (i == 0 && col.startsWith(UTF8_BOM)) {
          col = col.substring(1);
        }
        columns.add(deduplicateColumnName(col, columns));
      }

      if (columns.isEmpty()) {
        throw new BadRequestException("No columns found in CSV header");
      }

      List<Map<String, String>> rows = new ArrayList<>();
      for (int i = 1; i < allLines.size(); i++) {
        String[] line = allLines.get(i);
        boolean allEmpty = true;
        for (String val : line) {
          if (val != null && !val.trim().isEmpty()) {
            allEmpty = false;
            break;
          }
        }
        if (allEmpty) {
          continue;
        }

        Map<String, String> row = new LinkedHashMap<>();
        for (int j = 0; j < columns.size(); j++) {
          String value = j < line.length ? line[j].trim() : "";
          row.put(columns.get(j), value);
        }
        rows.add(row);
      }

      return new ParsedFile(columns, rows, FORMAT_CSV);
    } catch (IOException | CsvException e) {
      throw new BadRequestException("Failed to parse CSV file: " + e.getMessage());
    }
  }

  private ParsedFile parseXlsx(MultipartFile file) {
    try (InputStream is = file.getInputStream();
        Workbook workbook = new XSSFWorkbook(is)) {

      Sheet sheet = workbook.getSheetAt(0);
      if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
        throw new BadRequestException("XLSX file is empty");
      }

      DataFormatter formatter = new DataFormatter();
      Row headerRow = sheet.getRow(0);
      if (headerRow == null) {
        throw new BadRequestException("No header row found in XLSX");
      }

      List<String> columns = new ArrayList<>();
      for (int j = 0; j < headerRow.getLastCellNum(); j++) {
        Cell cell = headerRow.getCell(j);
        String value = cell != null ? formatter.formatCellValue(cell).trim() : "";
        columns.add(deduplicateColumnName(value, columns));
      }

      if (columns.isEmpty()) {
        throw new BadRequestException("No columns found in XLSX header");
      }

      List<Map<String, String>> rows = new ArrayList<>();
      for (int i = 1; i <= sheet.getLastRowNum(); i++) {
        Row row = sheet.getRow(i);
        if (row == null) {
          continue;
        }

        boolean allEmpty = true;
        Map<String, String> rowData = new LinkedHashMap<>();
        for (int j = 0; j < columns.size(); j++) {
          Cell cell = row.getCell(j);
          String value = cell != null ? formatter.formatCellValue(cell).trim() : "";
          rowData.put(columns.get(j), value);
          if (!value.isEmpty()) {
            allEmpty = false;
          }
        }

        if (!allEmpty) {
          rows.add(rowData);
        }
      }

      return new ParsedFile(columns, rows, FORMAT_XLSX);
    } catch (IOException e) {
      throw new BadRequestException("Failed to parse XLSX file: " + e.getMessage());
    }
  }

  /** Appends a numeric suffix if the column name already exists in the list. */
  private String deduplicateColumnName(String name, List<String> existing) {
    if (!existing.contains(name)) {
      return name;
    }
    int suffix = 2;
    while (existing.contains(name + "_" + suffix)) {
      suffix++;
    }
    return name + "_" + suffix;
  }
}
