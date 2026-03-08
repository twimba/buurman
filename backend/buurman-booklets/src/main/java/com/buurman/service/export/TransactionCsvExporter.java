package com.buurman.service.export;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.buurman.exception.ExternalServiceException;
import com.opencsv.CSVWriter;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TransactionCsvExporter {

  private final TransactionDataLoader dataLoader;

  public byte[] generate(Optional<LocalDate> startDate, Optional<LocalDate> endDate, UUID teamId) {
    List<TransactionRecord> transactions = dataLoader.load(startDate, endDate, teamId);

    try (StringWriter sw = new StringWriter();
        CSVWriter writer = new CSVWriter(sw)) {

      writer.writeNext(
          new String[] {
            "Date", "Type", "Description", "Property", "Category", "Amount", "Currency"
          });

      for (TransactionRecord t : transactions) {
        writer.writeNext(
            new String[] {
              t.date().toString(),
              t.type(),
              t.description(),
              t.property(),
              t.category().orElse(""),
              t.amount().toString(),
              t.currency()
            });
      }

      return sw.toString().getBytes(StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new ExternalServiceException("Failed to generate CSV", e);
    }
  }
}
