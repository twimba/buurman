package com.buurman.service;

import static com.buurman.jooq.generated.Tables.AMENITIES;
import static com.buurman.jooq.generated.Tables.CONTACTS;
import static com.buurman.jooq.generated.Tables.CONTACT_ADDRESSES;
import static com.buurman.jooq.generated.Tables.CONTRACTS;
import static com.buurman.jooq.generated.Tables.CONTRACT_PARTIES;
import static com.buurman.jooq.generated.Tables.CONTRACT_PAYMENT_INSTRUCTIONS;
import static com.buurman.jooq.generated.Tables.CONTRACT_RENT_PERIODS;
import static com.buurman.jooq.generated.Tables.DOCUMENTS;
import static com.buurman.jooq.generated.Tables.EXPENSES;
import static com.buurman.jooq.generated.Tables.PAYMENTS;
import static com.buurman.jooq.generated.Tables.PAYMENT_RECEIVALS;
import static com.buurman.jooq.generated.Tables.PHOTOS;
import static com.buurman.jooq.generated.Tables.PROPERTIES;
import static com.buurman.jooq.generated.Tables.PROPERTY_ACQUISITIONS;
import static com.buurman.jooq.generated.Tables.PROPERTY_FEES;
import static com.buurman.jooq.generated.Tables.PROPERTY_FINANCINGS;
import static com.buurman.jooq.generated.Tables.PROPERTY_INSURANCES;
import static com.buurman.jooq.generated.Tables.PROPERTY_OCCUPANCY_PERIODS;
import static com.buurman.jooq.generated.Tables.PROPERTY_OUTDOOR_AREAS;
import static com.buurman.jooq.generated.Tables.PROPERTY_TAXES;
import static com.buurman.jooq.generated.Tables.PROPERTY_VALUATIONS;
import static com.buurman.jooq.generated.Tables.TEAM_MEMBERS;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.Table;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.buurman.domain.GoogleAccessToken;
import com.buurman.domain.GoogleSheetExport;
import com.buurman.service.export.google.GoogleSheetBuilder;
import com.buurman.service.export.google.GoogleSheetExportPipeline;

import lombok.RequiredArgsConstructor;

/**
 * Synchronous Google Sheets takeout — exports all tabular team data into a single multi-tab Google
 * Sheet in the user's Drive. Binary artefacts (documents, photos, generated PDFs) are explicitly
 * excluded; only metadata tables are included.
 *
 * <p>Tab order follows the app's sidebar menu order, then drills into the relational details for
 * each top-level entity.
 */
@Service
@RequiredArgsConstructor
public class TakeoutGoogleSheetService {

  private final DSLContext dsl;
  private final GoogleSheetExportPipeline pipeline;

  /**
   * Hard cap on rows fetched per tab. Two reasons:
   *
   * <ul>
   *   <li>Google Sheets has a 10M-cell-per-spreadsheet limit. With 22 tabs and an average ~20
   *       columns, ~22,000 rows per tab fits well under the limit.
   *   <li>Bounded memory: the JOOQ result is materialised in memory before being handed to the
   *       Sheets API in one batchUpdate. 50k rows × 22 tabs at ~1 KB / row is safely below 1 GB.
   * </ul>
   *
   * Tabs that hit the cap include an explicit truncation row at the bottom so the user can see
   * which entities need a per-entity export to retrieve in full.
   */
  static final int MAX_ROWS_PER_TAB = 50_000;

  /** Ordered list of (tab label, JOOQ table) — matches the sidebar menu ordering. */
  private static final List<TakeoutTab> TABS =
      List.of(
          new TakeoutTab("Properties", PROPERTIES),
          new TakeoutTab("Property Acquisitions", PROPERTY_ACQUISITIONS),
          new TakeoutTab("Property Valuations", PROPERTY_VALUATIONS),
          new TakeoutTab("Property Fees", PROPERTY_FEES),
          new TakeoutTab("Property Taxes", PROPERTY_TAXES),
          new TakeoutTab("Property Insurances", PROPERTY_INSURANCES),
          new TakeoutTab("Property Financings", PROPERTY_FINANCINGS),
          new TakeoutTab("Property Outdoor Areas", PROPERTY_OUTDOOR_AREAS),
          new TakeoutTab("Amenities", AMENITIES),
          new TakeoutTab("Property Occupancy Periods", PROPERTY_OCCUPANCY_PERIODS),
          new TakeoutTab("Contacts", CONTACTS),
          new TakeoutTab("Contact Addresses", CONTACT_ADDRESSES),
          new TakeoutTab("Contracts", CONTRACTS),
          new TakeoutTab("Contract Parties", CONTRACT_PARTIES),
          new TakeoutTab("Contract Rent Periods", CONTRACT_RENT_PERIODS),
          new TakeoutTab("Contract Payment Instructions", CONTRACT_PAYMENT_INSTRUCTIONS),
          new TakeoutTab("Payments", PAYMENTS),
          new TakeoutTab("Payment Receivals", PAYMENT_RECEIVALS),
          new TakeoutTab("Expenses", EXPENSES),
          new TakeoutTab("Documents (metadata)", DOCUMENTS),
          new TakeoutTab("Photos (metadata)", PHOTOS),
          new TakeoutTab("Team Members", TEAM_MEMBERS));

  @PreAuthorize("hasRole('TEAM_ADMIN')")
  public GoogleSheetExport export(GoogleAccessToken token, UUID teamId, String title) {
    return pipeline.export(
        token,
        title,
        builder -> {
          for (TakeoutTab spec : TABS) {
            populateTab(builder.tab(spec.label()), spec.table(), teamId);
          }
        });
  }

  private <R extends Record> void populateTab(
      GoogleSheetBuilder.TabSpec tab, Table<R> table, UUID teamId) {
    Field<?>[] fields = table.fields();
    String[] headers = new String[fields.length];
    for (int i = 0; i < fields.length; i++) {
      headers[i] = fields[i].getName();
    }
    tab.headers(headers);

    // Apply per-column number formats so dates / numbers / booleans render natively in Sheets
    // instead of as raw toString() text.
    for (int i = 0; i < fields.length; i++) {
      GoogleSheetBuilder.ColumnFormat fmt = formatFor(fields[i].getType());
      if (fmt != GoogleSheetBuilder.ColumnFormat.TEXT) {
        tab.columnFormat(i, fmt);
      }
    }

    Field<UUID> teamIdField = table.field("team_id", UUID.class);
    Field<?> deletedAtField = table.field("deleted_at");

    // Fetch one row beyond the cap so we can detect whether truncation happened.
    int fetchLimit = MAX_ROWS_PER_TAB + 1;

    Result<R> records;
    if (teamIdField != null && deletedAtField != null) {
      records =
          dsl.selectFrom(table)
              .where(teamIdField.eq(teamId))
              .and(deletedAtField.isNull())
              .limit(fetchLimit)
              .fetch();
    } else if (teamIdField != null) {
      records = dsl.selectFrom(table).where(teamIdField.eq(teamId)).limit(fetchLimit).fetch();
    } else {
      records = dsl.selectFrom(table).limit(fetchLimit).fetch();
    }

    boolean truncated = records.size() > MAX_ROWS_PER_TAB;
    int rowsToWrite = truncated ? MAX_ROWS_PER_TAB : records.size();
    for (int r = 0; r < rowsToWrite; r++) {
      R record = records.get(r);
      Object[] row = new Object[fields.length];
      for (int i = 0; i < fields.length; i++) {
        row[i] = renderCell(record.get(fields[i]));
      }
      tab.row(row);
    }

    if (truncated) {
      // Inline truncation marker: a single all-cells-banner row in the first column. Users see it
      // immediately in Sheets without having to scroll the whole tab.
      Object[] notice = new Object[fields.length];
      notice[0] =
          "⚠ Truncated at "
              + MAX_ROWS_PER_TAB
              + " rows — use the per-entity Google Sheets export for the full data.";
      for (int i = 1; i < fields.length; i++) {
        notice[i] = "";
      }
      tab.row(notice);
    }
  }

  /** Maps a Java/JOOQ column type to the Sheets column format that renders it natively. */
  private static GoogleSheetBuilder.ColumnFormat formatFor(Class<?> type) {
    if (type == LocalDate.class) {
      return GoogleSheetBuilder.ColumnFormat.DATE;
    }
    if (Number.class.isAssignableFrom(type)) {
      if (type == Integer.class || type == Long.class || type == Short.class) {
        return GoogleSheetBuilder.ColumnFormat.INTEGER;
      }
      return GoogleSheetBuilder.ColumnFormat.NUMBER;
    }
    return GoogleSheetBuilder.ColumnFormat.TEXT;
  }

  /**
   * Coerces a value to something Sheets will render meaningfully. Numbers and dates pass through as
   * their native Java type so the column format takes effect. Timestamps are rendered as ISO-8601
   * strings (Sheets doesn't have a native zoned-timestamp format). Everything else becomes {@code
   * toString()}.
   */
  private static Object renderCell(Object value) {
    if (value == null) {
      return "";
    }
    if (value instanceof Number n) {
      return n.doubleValue();
    }
    if (value instanceof BigDecimal bd) {
      return bd.doubleValue();
    }
    if (value instanceof LocalDate d) {
      return d.toString();
    }
    if (value instanceof LocalDateTime dt) {
      return dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
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

  private record TakeoutTab(String label, Table<? extends Record> table) {}
}
