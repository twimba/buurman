package com.buurman.service.backoffice.cost;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.buurman.domain.backoffice.CostProviderId;
import com.buurman.repository.backoffice.CostManualAmountRepository;

import lombok.RequiredArgsConstructor;

/**
 * Resolves a provider's manual monthly EUR amount from the admin-edited value in {@code
 * cost_manual_amount}. Used when a provider has no API source, or its API source is unavailable.
 * Empty until a Buurmy enters an amount on the Costs page — there is no config seed.
 */
@Component
@RequiredArgsConstructor
public class ManualCostResolver {

  private final CostManualAmountRepository repository;

  /** EUR minor units for the provider, or empty when no amount has been set. */
  public Optional<Long> amountEurMinor(CostProviderId id) {
    return Optional.ofNullable(repository.all().get(id.name()));
  }
}
