package com.buurman.service.backoffice.cost;

import java.util.Map;
import java.util.Optional;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.buurman.util.SkipTestCoverage;

/**
 * Cost-tracking configuration. {@code fx} maps an original currency code to its EUR rate; {@code
 * manualEur} holds flat monthly EUR amounts for providers without a usable cost API (keyed by the
 * lowercased {@code CostProviderId}, e.g. {@code cloudflare}, {@code better_stack}).
 */
@ConfigurationProperties(prefix = "backoffice.cost")
@SkipTestCoverage
public record CostProperties(
    String baseCurrency, Map<String, Double> fx, Hetzner hetzner, Map<String, Double> manualEur) {

  public CostProperties {
    if (baseCurrency == null || baseCurrency.isBlank()) {
      baseCurrency = "EUR";
    }
    fx = fx == null ? Map.of() : fx;
    manualEur = manualEur == null ? Map.of() : manualEur;
    hetzner = hetzner == null ? new Hetzner(Optional.empty()) : hetzner;
  }

  @SkipTestCoverage
  public record Hetzner(Optional<String> token) {
    public Hetzner {
      token = Optional.ofNullable(token).flatMap(o -> o).filter(s -> !s.isBlank());
    }
  }
}
