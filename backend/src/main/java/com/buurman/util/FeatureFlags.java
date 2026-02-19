package com.buurman.util;

/**
 * Feature flag key constants. Prevents typos and enables IDE navigation. Keep in sync with
 * Flagsmith dashboard and app/src/constants/featureFlags.ts.
 */
public final class FeatureFlags {

  private FeatureFlags() {}

  public static final String REPORTS = "reports";
  public static final String INVITATION_REQUIRED = "invitation_required";
  public static final String SMS_NOTIFICATIONS = "sms_notifications";
  public static final String EMAIL_NOTIFICATIONS = "email_notifications";
}
