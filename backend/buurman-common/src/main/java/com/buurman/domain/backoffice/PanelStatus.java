package com.buurman.domain.backoffice;

/** Status of a backoffice dashboard panel's data source. */
public enum PanelStatus {
  /** Real, live data is present. */
  LIVE,
  /** Data source is not wired yet; render a preview placeholder with a CTA. */
  PREVIEW,
  /** Panel is intentionally turned off. */
  DISABLED
}
