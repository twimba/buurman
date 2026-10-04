package com.buurman.service.backoffice.cost;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.buurman.repository.backoffice.FxRateRepository;

import lombok.RequiredArgsConstructor;

/**
 * Normalizes an amount in some currency to EUR minor units, using the latest live rate stored by
 * {@link FxRateService}. Returns empty when no stored rate is available for the currency/date.
 */
@Component
@RequiredArgsConstructor
public class FxConverter {

  private final FxRateRepository fxRateRepository;

  public record Converted(long eurMinor, BigDecimal rate) {}

  /**
   * Convert to EUR using the rate effective on {@code onDate} (the moment the conversion applies
   * to). EUR passes through at rate 1; other currencies use the dated stored rate, else empty when
   * no rate has been fetched/stored yet.
   */
  public Optional<Converted> toEur(String currency, long amountMinor, LocalDate onDate) {
    if ("EUR".equalsIgnoreCase(currency)) {
      return Optional.of(new Converted(amountMinor, BigDecimal.ONE));
    }
    String cur = currency.toUpperCase(Locale.ROOT);
    BigDecimal rate = fxRateRepository.rateOn(cur, onDate).orElse(null);
    if (rate == null || rate.signum() <= 0) {
      return Optional.empty();
    }
    long eurMinor =
        rate.multiply(BigDecimal.valueOf(amountMinor))
            .setScale(0, RoundingMode.HALF_UP)
            .longValue();
    return Optional.of(new Converted(eurMinor, rate));
  }
}
