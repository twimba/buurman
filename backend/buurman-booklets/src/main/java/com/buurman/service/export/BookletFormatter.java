package com.buurman.service.export;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.buurman.util.CurrencyUtils;
import com.buurman.util.MoneyAmount;

/**
 * Locale-aware value formatting for booklets &amp; summary cards. Centralises money/date/percent
 * rendering so assemblers feed templates display-ready strings (templates never format). Null-safe:
 * absent values render as an em-dash, never blank or "null".
 */
@Component
public class BookletFormatter {

  private static final String DASH = "—";

  public String money(@Nullable MoneyAmount amount, Locale locale) {
    if (amount == null) {
      return DASH;
    }
    return CurrencyUtils.formatCurrency(amount.value(), amount.currency(), locale);
  }

  public String date(@Nullable LocalDate date, Locale locale) {
    if (date == null) {
      return DASH;
    }
    return date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale));
  }

  /** Compact day+month for tight tiles (e.g. "1 Jul" / "1 juill."). */
  public String dateShort(@Nullable LocalDate date, Locale locale) {
    if (date == null) {
      return DASH;
    }
    return date.format(DateTimeFormatter.ofPattern("d MMM", locale));
  }

  /** Whole-number percent string (locale-neutral digits + '%'); pass the percentage, e.g. 42. */
  public String percentWhole(int value) {
    return value + "%";
  }

  /** Safe integer ratio → 0-100 (returns 0 when total is 0). */
  public int pct(long part, long total) {
    if (total <= 0) {
      return 0;
    }
    return (int) Math.round(part * 100.0 / total);
  }

  /**
   * Hero font-size class by text length (longer text → smaller): hero-l ≤12, hero-m ≤20, else -s.
   */
  public String heroSize(int length) {
    if (length <= 12) {
      return "hero-l";
    }
    if (length <= 20) {
      return "hero-m";
    }
    return "hero-s";
  }

  public String numberOrDash(@Nullable BigDecimal value, Locale locale) {
    return value == null ? DASH : CurrencyUtils.formatNumber(value, locale);
  }
}
