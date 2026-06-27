package com.buurman.service.backoffice.cost;

import java.util.Optional;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.buurman.util.SkipTestCoverage;

/**
 * Cost-tracking configuration. Holds only infrastructure connection settings (FX source, provider
 * API credentials). Editable monetary figures — the Mailgun plan fee / per-email rate and flat
 * provider amounts — live exclusively in the DB ({@code cost_config}, {@code cost_manual_amount}),
 * set from the backoffice Costs page; there is no env/yml seed for them.
 */
@ConfigurationProperties(prefix = "backoffice.cost")
@SkipTestCoverage
public record CostProperties(
    String baseCurrency, Hetzner hetzner, Cloudflare cloudflare, String fxApiUrl) {

  public CostProperties {
    if (baseCurrency == null || baseCurrency.isBlank()) {
      baseCurrency = "EUR";
    }
    if (fxApiUrl == null || fxApiUrl.isBlank()) {
      fxApiUrl = "https://api.frankfurter.dev";
    }
    hetzner = hetzner == null ? new Hetzner(Optional.empty()) : hetzner;
    cloudflare =
        cloudflare == null ? new Cloudflare(Optional.empty(), Optional.empty()) : cloudflare;
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

  private static Optional<String> blankToEmpty(Optional<String> value) {
    return Optional.ofNullable(value).flatMap(o -> o).filter(s -> !s.isBlank());
  }
}
