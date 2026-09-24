package com.buurman.document;

import static java.util.stream.Collectors.joining;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

/** Null-safe value formatting shared by every document exporter. */
public final class DocumentFormatting {

  private DocumentFormatting() {}

  /** {@code PENDING_SIGNATURE} → {@code Pending Signature}; null → empty. */
  public static String formatEnumValue(@Nullable String value) {
    if (value == null) {
      return "";
    }
    return Arrays.stream(value.split("_"))
        .map(
            word ->
                word.substring(0, 1).toUpperCase(Locale.ROOT)
                    + word.substring(1).toLowerCase(Locale.ROOT))
        .collect(joining(" "));
  }

  /** Formats the date with the given pattern, or an em dash when absent. */
  public static String formatDate(@Nullable LocalDate date, DateTimeFormatter fmt) {
    return date != null ? date.format(fmt) : "\u2014";
  }
}
