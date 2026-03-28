/**
 * Feature flag key constants. Prevents typos and enables IDE navigation.
 * Keep in sync with backend FeatureFlags.java (frontend-relevant flags only).
 */
export const FeatureFlags = {
  REPORTS: 'reports',
  EXCEL_EXPORT: 'excel_export',
  SMS_NOTIFICATIONS: 'sms_notifications',
  EMAIL_NOTIFICATIONS: 'email_notifications',
  BLOCK_EMAIL_NOTIFICATIONS: 'block_email_notifications',
  BLOCK_SMS_NOTIFICATIONS: 'block_sms_notifications',
  SWAGGER: 'swagger',
} as const;

export type FeatureFlagKey = (typeof FeatureFlags)[keyof typeof FeatureFlags];
