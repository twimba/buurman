package com.buurman.domain.backoffice;

/** Cost providers tracked by the backoffice cost dashboard. */
public enum CostProviderId {
  HETZNER("Hetzner", false),
  // Mailgun is formula-based: cost = plan fee + accepted-email volume × per-email rate (params in
  // cost_config, volume pulled live from Mailgun). It has NO manual-amount fallback — when its API
  // is unreachable it stays "estimated, not yet configured" rather than becoming a typed-in total.
  MAILGUN("Mailgun", true),
  TWILIO("Twilio", false),
  CLOUDFLARE("Cloudflare", false),
  BETTER_STACK("Better Stack", false);

  private final String displayName;
  private final boolean formulaBased;

  CostProviderId(String displayName, boolean formulaBased) {
    this.displayName = displayName;
    this.formulaBased = formulaBased;
  }

  public String displayName() {
    return displayName;
  }

  /**
   * True when the provider's cost is computed from an editable formula (Mailgun). Such providers
   * are never costed by, nor fall back to, a manual flat amount.
   */
  public boolean formulaBased() {
    return formulaBased;
  }
}
