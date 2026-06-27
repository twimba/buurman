package com.buurman.service.backoffice.cost;

import java.util.List;
import java.util.Optional;

import com.buurman.domain.backoffice.CostProviderId;
import com.buurman.domain.backoffice.CostSourceType;

/** A pluggable source of one provider's current monthly cost. */
public interface CostSource {

  CostProviderId id();

  /**
   * Reads the current monthly cost. Must never throw — return {@code unavailable} on any failure.
   */
  ProviderReading read();

  /** A single line within a provider's cost (e.g. "Servers", "SMS"). */
  record LineItem(String label, long amountMinor) {}

  /**
   * One provider's reading. {@code amountMinor} is in {@code currency}'s minor units. When {@code
   * available} is false the figure is ignored and the panel shows a "connect" hint.
   */
  record ProviderReading(
      CostProviderId provider,
      CostSourceType type,
      String currency,
      long amountMinor,
      List<LineItem> breakdown,
      boolean available,
      Optional<String> note) {

    public static ProviderReading unavailable(CostProviderId provider, String note) {
      return new ProviderReading(
          provider, CostSourceType.SUBSCRIPTION, "EUR", 0, List.of(), false, Optional.of(note));
    }

    public static ProviderReading of(
        CostProviderId provider,
        CostSourceType type,
        String currency,
        long amountMinor,
        List<LineItem> breakdown) {
      return new ProviderReading(
          provider, type, currency, amountMinor, breakdown, true, Optional.empty());
    }
  }
}
