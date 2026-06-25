package com.buurman.service.backoffice.cost;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.buurman.repository.backoffice.FxRateRepository;

import lombok.RequiredArgsConstructor;

/**
 * Normalizes an amount in some currency to EUR minor units. Prefers the latest live rate stored by
 * {@link FxRateService}, falling back to the static config rate when none has been fetched yet.
 */
@Component
@RequiredArgsConstructor
public class FxConverter {

  private final CostProperties props;
  private final FxRateRepository fxRateRepository;

  public record Converted(long eurMinor, BigDecimal rate) {}

  /**
   * Convert to EUR using the rate effective on {@code onDate} (the moment the conversion applies
   * to). EUR passes through at rate 1; other currencies use the dated stored rate, falling back to
   * the static configured rate, else empty.
   */
  public Optional<Converted> toEur(String currency, long amountMinor, LocalDate onDate) {
    if ("EUR".equalsIgnoreCase(currency)) {
      return Optional.of(new Converted(amountMinor, BigDecimal.ONE));
    }
    String cur = currency.toUpperCase();
    BigDecimal rate = fxRateRepository.rateOn(cur, onDate).orElseGet(() -> configuredRate(cur));
    if (rate == null || rate.signum() <= 0) {
      return Optional.empty();
    }
    long eurMinor =
        rate.multiply(BigDecimal.valueOf(amountMinor))
            .setScale(0, RoundingMode.HALF_UP)
            .longValue();
    return Optional.of(new Converted(eurMinor, rate));
  }

  private BigDecimal configuredRate(String currency) {
    // currency is already upper-cased by the caller; match config keys case-insensitively so a
    // lowercase YAML key (e.g. `usd:`) is still found instead of silently suppressing the provider.
    Double rate =
        props.fx().entrySet().stream()
            .filter(e -> e.getKey() != null && e.getKey().equalsIgnoreCase(currency))
            .map(java.util.Map.Entry::getValue)
            .findFirst()
            .orElse(null);
    return (rate == null || rate <= 0) ? null : BigDecimal.valueOf(rate);
  }
}
