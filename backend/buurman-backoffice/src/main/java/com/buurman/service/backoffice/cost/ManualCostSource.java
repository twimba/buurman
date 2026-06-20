package com.buurman.service.backoffice.cost;

import java.util.List;

import com.buurman.domain.backoffice.CostProviderId;
import com.buurman.domain.backoffice.CostSourceType;

/**
 * Flat monthly EUR cost read from config — used for providers without a usable cost API yet
 * (Cloudflare, Better Stack, Mailgun). Unconfigured (or ≤ 0) reports as unavailable.
 */
public class ManualCostSource implements CostSource {

  private final CostProviderId id;
  private final CostProperties props;

  public ManualCostSource(CostProviderId id, CostProperties props) {
    this.id = id;
    this.props = props;
  }

  @Override
  public CostProviderId id() {
    return id;
  }

  @Override
  public ProviderReading read() {
    String key = id.name().toLowerCase();
    Double eur = props.manualEur().get(key);
    if (eur == null || eur <= 0) {
      return ProviderReading.unavailable(id, "Set backoffice.cost.manual-eur." + key);
    }
    return ProviderReading.of(
        id, CostSourceType.SUBSCRIPTION, "EUR", Math.round(eur * 100), List.of());
  }
}
