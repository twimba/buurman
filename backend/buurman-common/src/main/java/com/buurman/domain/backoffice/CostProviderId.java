package com.buurman.domain.backoffice;

/** Cost providers tracked by the backoffice cost dashboard. */
public enum CostProviderId {
  HETZNER("Hetzner"),
  MAILGUN("Mailgun"),
  TWILIO("Twilio"),
  CLOUDFLARE("Cloudflare"),
  BETTER_STACK("Better Stack");

  private final String displayName;

  CostProviderId(String displayName) {
    this.displayName = displayName;
  }

  public String displayName() {
    return displayName;
  }
}
