package com.buurman.domain.backoffice;

/** How a cost figure was obtained — drives the confidence badge in the UI. */
public enum CostSourceType {
  /** Real billed/usage amount from the provider's API. */
  ACTUAL,
  /** Computed run-rate from resources × pricing (no money API). */
  ESTIMATED,
  /** Flat monthly subscription amount configured by an admin. */
  SUBSCRIPTION
}
