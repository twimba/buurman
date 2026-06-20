package com.buurman.service.backoffice.cost;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.buurman.domain.backoffice.CostProviderId;
import com.buurman.repository.backoffice.CostManualAmountRepository;

import lombok.RequiredArgsConstructor;

/**
 * Resolves a provider's manual monthly EUR amount: admin-edited value ({@code cost_manual_amount})
 * first, falling back to the configured seed. Used when a provider has no API source, or its API
 * source is unavailable.
 */
@Component
@RequiredArgsConstructor
public class ManualCostResolver {

  private final CostManualAmountRepository repository;
  private final CostProperties props;

  /** EUR minor units for the provider, or empty when nothing is configured. */
  public Optional<Long> amountEurMinor(CostProviderId id) {
    Map<String, Long> edited = repository.all();
    Long dbValue = edited.get(id.name());
    if (dbValue != null) {
      return Optional.of(dbValue);
    }
    Double seed = props.manualEur().get(id.name().toLowerCase());
    if (seed != null && seed > 0) {
      return Optional.of(Math.round(seed * 100));
    }
    return Optional.empty();
  }
}
