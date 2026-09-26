package com.buurman.domain;

/**
 * Topic of a display-only tenancy-law reference entry on a catalogue country.
 *
 * <p>New values added here MUST also be added to {@code openapi/src/app.yaml}'s {@code
 * TenancyRuleTopic} schema (and re-bundled with {@code make bundle-openapi}) so the frontend's
 * generated TS stays in sync.
 */
public enum TenancyRuleTopic {
  NOTICE_PERIOD,
  TENANCY_DURATION,
  DEPOSIT,
  LEASE_FORM,
  REGISTRATION,
  FEES_AND_PENALTIES,
  OTHER
}
