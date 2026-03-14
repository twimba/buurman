package com.buurman.util;

import java.util.List;

/**
 * Feature flag key constants. Prevents typos and enables IDE navigation. Keep in sync with PostHog
 * dashboard and frontend/app/src/constants/featureFlags.ts.
 */
public final class FeatureFlags {

  private FeatureFlags() {}

  public static final String REPORTS = "reports";
  public static final String INVITATION_REQUIRED = "invitation_required";
  public static final String SMS_NOTIFICATIONS = "sms_notifications";
  public static final String EMAIL_NOTIFICATIONS = "email_notifications";
  public static final String BLOCK_EMAIL_NOTIFICATIONS = "block_email_notifications";
  public static final String BLOCK_SMS_NOTIFICATIONS = "block_sms_notifications";
  public static final String TAKEOUT_MAX_EXPORTS = "takeout_max_exports";
  public static final String EXCEL_EXPORT = "excel_export";
  public static final String NOTIFICATION_CONSOLIDATION = "notification_consolidation";

  /** All flag keys — used for bulk evaluation (e.g. getAllFlags endpoint). */
  public static final List<String> ALL_KEYS =
      List.of(
          REPORTS,
          INVITATION_REQUIRED,
          SMS_NOTIFICATIONS,
          EMAIL_NOTIFICATIONS,
          BLOCK_EMAIL_NOTIFICATIONS,
          BLOCK_SMS_NOTIFICATIONS,
          TAKEOUT_MAX_EXPORTS,
          EXCEL_EXPORT,
          NOTIFICATION_CONSOLIDATION);
}
