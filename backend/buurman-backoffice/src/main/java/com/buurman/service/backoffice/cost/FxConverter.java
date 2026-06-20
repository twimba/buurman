package com.buurman.service.backoffice.cost;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/** Normalizes an amount in some currency to EUR minor units using configured static FX rates. */
@Component
@RequiredArgsConstructor
public class FxConverter {

  private final CostProperties props;

  public record Converted(long eurMinor, BigDecimal rate) {}

  /** EUR passes through at rate 1; other currencies need a configured rate, else empty. */
  public Optional<Converted> toEur(String currency, long amountMinor) {
    if ("EUR".equalsIgnoreCase(currency)) {
      return Optional.of(new Converted(amountMinor, BigDecimal.ONE));
    }
    Double rate = props.fx().get(currency.toUpperCase());
    if (rate == null || rate <= 0) {
      return Optional.empty();
    }
    long eurMinor = Math.round(amountMinor * rate);
    return Optional.of(new Converted(eurMinor, BigDecimal.valueOf(rate)));
  }
}
