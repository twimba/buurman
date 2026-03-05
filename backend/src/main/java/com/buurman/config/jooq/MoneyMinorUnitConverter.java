package com.buurman.config.jooq;

import java.math.BigDecimal;

import org.jooq.Converter;

/**
 * JOOQ converter that automatically converts between minor-unit BIGINT storage (e.g. 10012) and
 * major-unit BigDecimal (e.g. 100.12) with fixed scale 2.
 *
 * <p><b>Why fixed scale 2:</b> A JOOQ {@link Converter} operates on a single column and cannot
 * access the companion {@code *_currency} column to determine the scale dynamically. All currently
 * supported currencies (EUR, USD, GBP, SEK, CHF, PLN — see BUUR-51) use 2 fractional digits. If
 * currencies with a different scale are ever supported (e.g. JPY=0, BHD=3), this converter must be
 * extended — either via a lookup table keyed by currency code injected at build time, or by
 * switching to manual conversion in mappers where both columns are accessible.
 *
 * <p>The domain layer ({@link com.buurman.util.MoneyAmount#toMinorUnits()}) is already
 * currency-aware and uses {@link com.buurman.util.CurrencyUtils#getFractionalDigits(String)}.
 */
public class MoneyMinorUnitConverter implements Converter<Long, BigDecimal> {

  /** All supported currencies currently use 2 fractional digits (see BUUR-51). */
  private static final int SCALE = 2;

  @Override
  public BigDecimal from(Long databaseValue) {
    if (databaseValue == null) {
      return null;
    }
    return BigDecimal.valueOf(databaseValue, SCALE);
  }

  @Override
  public Long to(BigDecimal userValue) {
    if (userValue == null) {
      return null;
    }
    return userValue.movePointRight(SCALE).longValueExact();
  }

  @Override
  public Class<Long> fromType() {
    return Long.class;
  }

  @Override
  public Class<BigDecimal> toType() {
    return BigDecimal.class;
  }
}
