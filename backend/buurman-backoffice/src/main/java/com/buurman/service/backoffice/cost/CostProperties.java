package com.buurman.service.backoffice.cost;

import java.util.Map;
import java.util.Optional;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.buurman.util.SkipTestCoverage;

/**
 * Cost-tracking configuration. {@code fx} maps an original currency code to its EUR rate. {@code
 * manualEur} holds seed flat monthly EUR amounts (keyed by lowercased {@code CostProviderId});
 * these are overridden at runtime by admin-edited values in {@code cost_manual_amount}.
 */
@ConfigurationProperties(prefix = "backoffice.cost")
@SkipTestCoverage
public record CostProperties(
    String baseCurrency,
    Map<String, Double> fx,
    Hetzner hetzner,
    Cloudflare cloudflare,
    Mailgun mailgun,
    Map<String, Double> manualEur,
    String fxApiUrl) {

  public CostProperties {
    if (baseCurrency == null || baseCurrency.isBlank()) {
      baseCurrency = "EUR";
    }
    if (fxApiUrl == null || fxApiUrl.isBlank()) {
      fxApiUrl = "https://api.frankfurter.dev";
    }
    fx = fx == null ? Map.of() : fx;
    manualEur = manualEur == null ? Map.of() : manualEur;
    hetzner = hetzner == null ? new Hetzner(Optional.empty()) : hetzner;
    cloudflare =
        cloudflare == null ? new Cloudflare(Optional.empty(), Optional.empty()) : cloudflare;
    mailgun = mailgun == null ? new Mailgun(0, 0) : mailgun;
  }

  @SkipTestCoverage
  public record Hetzner(Optional<String> token) {
    public Hetzner {
      token = blankToEmpty(token);
    }
  }

  @SkipTestCoverage
  public record Cloudflare(Optional<String> token, Optional<String> accountId) {
    public Cloudflare {
      token = blankToEmpty(token);
      accountId = blankToEmpty(accountId);
    }
  }

  /** Volume-based Mailgun estimate: flat plan fee + per-email rate (EUR). */
  @SkipTestCoverage
  public record Mailgun(double baseEur, double perEmailEur) {}

  private static Optional<String> blankToEmpty(Optional<String> value) {
    return Optional.ofNullable(value).flatMap(o -> o).filter(s -> !s.isBlank());
  }
}
