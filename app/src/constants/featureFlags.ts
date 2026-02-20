/**
 * Feature flag key constants. Prevents typos and enables IDE navigation.
 * Keep in sync with Flagsmith dashboard and backend FeatureFlags.java.
 */
export const FeatureFlags = {
  REPORTS: 'reports',
  SMS_NOTIFICATIONS: 'sms_notifications',
} as const;

export type FeatureFlagKey = (typeof FeatureFlags)[keyof typeof FeatureFlags];
