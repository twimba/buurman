export const AnalyticsEvent = {
  BO_TEAM_VIEWED: "bo_team_viewed",
  BO_USER_VIEWED: "bo_user_viewed",
  BO_FEATURE_FLAG_UPDATED: "bo_feature_flag_updated",
  BO_INVITATION_SENT: "bo_invitation_sent",
  BO_BROADCAST_CREATED: "bo_broadcast_created",
  BO_USER_DISABLED: "bo_user_disabled",
  BO_USER_ENABLED: "bo_user_enabled",
} as const;

export type AnalyticsEventName =
  (typeof AnalyticsEvent)[keyof typeof AnalyticsEvent];
