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
  private static final String UTF8_BOM = "\uFEFF";

  public record ParsedFile(
      List<String> columns, List<Map<String, String>> rows, String fileFormat) {}

  public ParsedFile parse(MultipartFile file, boolean headerRow) {
    String filename = file.getOriginalFilename();
    if (filename == null || filename.isBlank()) {
      throw new BadRequestException("File name is required");
    }

    String lower = filename.toLowerCase(Locale.ROOT);
    if (lower.endsWith(".csv")) {
      return parseCsv(file, headerRow);
    } else if (lower.endsWith(".xlsx")) {
      return parseXlsx(file, headerRow);
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

  private ParsedFile parseCsv(MultipartFile file, boolean headerRow) {
    try (InputStream is = file.getInputStream();
        CSVReader reader = new CSVReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

      List<String[]> allLines = reader.readAll();
      if (allLines.isEmpty()) {
        throw new BadRequestException("CSV file is empty");
      }

      List<String> columns;
      int dataStartIndex;

      if (headerRow) {
        String[] headerLine = allLines.get(0);
        columns = new ArrayList<>();
        for (int i = 0; i < headerLine.length; i++) {
          String col = headerLine[i].trim();
          if (i == 0 && col.startsWith(UTF8_BOM)) {
            col = col.substring(1);
          }
          columns.add(deduplicateColumnName(col, columns));
        }
        dataStartIndex = 1;
      } else {
        int columnCount = allLines.get(0).length;
        columns = generateSyntheticColumns(columnCount);
        dataStartIndex = 0;
      }

      if (columns.isEmpty()) {
        throw new BadRequestException("No columns found in CSV");
      }

      List<Map<String, String>> rows = new ArrayList<>();
      for (int i = dataStartIndex; i < allLines.size(); i++) {
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

  private ParsedFile parseXlsx(MultipartFile file, boolean headerRow) {
    try (InputStream is = file.getInputStream();
        Workbook workbook = new XSSFWorkbook(is)) {

      Sheet sheet = workbook.getSheetAt(0);
      if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
        throw new BadRequestException("XLSX file is empty");
      }

      DataFormatter formatter = new DataFormatter();
      List<String> columns;
      int dataStartIndex;

      if (headerRow) {
        Row headerLine = sheet.getRow(0);
        if (headerLine == null) {
          throw new BadRequestException("No header row found in XLSX");
        }
        columns = new ArrayList<>();
        for (int j = 0; j < headerLine.getLastCellNum(); j++) {
          Cell cell = headerLine.getCell(j);
          String value = cell != null ? formatter.formatCellValue(cell).trim() : "";
          columns.add(deduplicateColumnName(value, columns));
        }
        dataStartIndex = 1;
      } else {
        Row firstRow = sheet.getRow(0);
        if (firstRow == null) {
          throw new BadRequestException("XLSX file is empty");
        }
        int columnCount = firstRow.getLastCellNum();
        columns = generateSyntheticColumns(columnCount);
        dataStartIndex = 0;
      }

      if (columns.isEmpty()) {
        throw new BadRequestException("No columns found in XLSX");
      }

      List<Map<String, String>> rows = new ArrayList<>();
      for (int i = dataStartIndex; i <= sheet.getLastRowNum(); i++) {
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

  /** Generates synthetic column names: Column A, Column B, ... Column Z, Column AA, ... */
  private List<String> generateSyntheticColumns(int count) {
    List<String> columns = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      columns.add("Column " + toLetterLabel(i));
    }
    return columns;
  }

  private String toLetterLabel(int index) {
    StringBuilder sb = new StringBuilder();
    int remaining = index;
    do {
      sb.insert(0, (char) ('A' + remaining % 26));
      remaining = remaining / 26 - 1;
    } while (remaining >= 0);
    return sb.toString();
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
