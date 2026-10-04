package com.buurman.util;

import java.util.List;
import java.util.Map;

/**
 * Feature flag key constants. Prevents typos and enables IDE navigation. Keep in sync with the
 * feature_flags DB table and frontend/app/src/constants/featureFlags.ts.
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
  public static final String GOOGLE_SHEETS_EXPORT = "google_sheets_export";
  public static final String SWAGGER = "swagger";
  public static final String MULTI_UNIT = "multi_unit";
  public static final String ESIGNATURE_ENABLED = "esignature_enabled";

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
          GOOGLE_SHEETS_EXPORT,
          SWAGGER,
          MULTI_UNIT,
          ESIGNATURE_ENABLED);

  /** Compile-time defaults — ultimate fallback when both cache and DB are unreachable. */
  public static final Map<String, Boolean> DEFAULTS =
      Map.ofEntries(
          Map.entry(REPORTS, true),
          Map.entry(INVITATION_REQUIRED, true),
          Map.entry(SMS_NOTIFICATIONS, false),
          Map.entry(EMAIL_NOTIFICATIONS, true),
          Map.entry(BLOCK_EMAIL_NOTIFICATIONS, false),
          Map.entry(BLOCK_SMS_NOTIFICATIONS, false),
          Map.entry(TAKEOUT_MAX_EXPORTS, true),
          Map.entry(EXCEL_EXPORT, false),
          Map.entry(GOOGLE_SHEETS_EXPORT, false),
          Map.entry(SWAGGER, true),
          Map.entry(MULTI_UNIT, true),
          Map.entry(ESIGNATURE_ENABLED, false));

  /** Returns the compile-time default for a flag key, or false for unknown keys. */
  public static boolean defaultEnabled(String flagKey) {
    return Boolean.TRUE.equals(DEFAULTS.get(flagKey));
  }
}
