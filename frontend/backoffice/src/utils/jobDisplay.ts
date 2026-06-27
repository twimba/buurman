// Human-friendly labels for Quartz jobs and their groups.
//
// The raw jobName / jobGroup identifiers come straight from the scheduler and
// read like code (`paymentReminderCheckJob`, `scheduling`). These maps turn them
// into something a human actually wants to read, with a one-line description for
// context. The raw identifier is still surfaced (as a tooltip / muted subtitle)
// so operators can correlate with logs and code.

export interface JobDisplay {
  label: string;
  description: string;
}

const JOB_DISPLAY: Record<string, JobDisplay> = {
  autoExtensionJob: {
    label: 'Auto-Extend Contracts',
    description: 'Automatically extends eligible contracts before they lapse',
  },
  contactFollowUpReminderJob: {
    label: 'Contact Follow-up Reminders',
    description: 'Nudges landlords about pending contact follow-ups',
  },
  contractExpiryCheckJob: {
    label: 'Contract Expiry Check',
    description: 'Flags contracts approaching their end date',
  },
  paymentGenerationJob: {
    label: 'Generate Payments',
    description: 'Creates upcoming rent payments from active contracts',
  },
  paymentReminderCheckJob: {
    label: 'Payment Reminders',
    description: 'Sends reminders for due and overdue payments',
  },
  renewalReminderJob: {
    label: 'Renewal Reminders',
    description: 'Reminds landlords about contracts up for renewal',
  },
  costSnapshotJob: {
    label: 'Cost Snapshot',
    description: 'Captures a periodic snapshot of platform costs',
  },
  databaseMetricsRefreshJob: {
    label: 'Database Metrics Refresh',
    description: 'Recomputes cached database metrics',
  },
  demoDataRegenerationJob: {
    label: 'Demo Data Regeneration',
    description: 'Rebuilds the demo team dataset',
  },
  executionHistoryCleanupJob: {
    label: 'Execution History Cleanup',
    description: 'Purges old job execution records',
  },
  impersonationSessionCleanupJob: {
    label: 'Impersonation Session Cleanup',
    description: 'Expires stale impersonation sessions',
  },
  rateLimitCleanupJob: {
    label: 'Rate Limit Cleanup',
    description: 'Clears expired rate-limit counters',
  },
  takeoutCleanupJob: {
    label: 'Takeout Cleanup',
    description: 'Removes expired data-export archives',
  },
  verificationCodeCleanupJob: {
    label: 'Verification Code Cleanup',
    description: 'Deletes expired verification codes',
  },
  notificationOutboxJob: {
    label: 'Notification Outbox',
    description: 'Dispatches queued email and SMS notifications',
  },
  thumbnailBackfillJob: {
    label: 'Thumbnail Backfill',
    description: 'Generates missing photo thumbnails',
  },
};

const GROUP_DISPLAY: Record<string, JobDisplay> = {
  scheduling: {
    label: 'Scheduling & Reminders',
    description: 'Contract lifecycle, payments and tenant reminders',
  },
  system: {
    label: 'System Maintenance',
    description: 'Housekeeping and cleanup jobs',
  },
  notification: {
    label: 'Notifications',
    description: 'Outbound email and SMS delivery',
  },
  metrics: {
    label: 'Metrics',
    description: 'Platform metrics and aggregates',
  },
  demo: {
    label: 'Demo Data',
    description: 'Demo environment data generation',
  },
  media: {
    label: 'Media',
    description: 'Photos, thumbnails and document assets',
  },
  backoffice: {
    label: 'Backoffice',
    description: 'Internal admin and reporting jobs',
  },
};

// Fallback: turn `someCamelCaseJob` into "Some Camel Case".
const humanizeIdentifier = (raw: string): string => {
  const stripped = raw.replace(/(Job|Trigger)$/, '');
  const spaced = stripped
    .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
    .replace(/[_-]+/g, ' ')
    .trim();
  if (!spaced) {
    return raw;
  }
  return spaced.charAt(0).toUpperCase() + spaced.slice(1);
};

export const jobLabel = (jobName: string): string =>
  JOB_DISPLAY[jobName]?.label ?? humanizeIdentifier(jobName);

export const jobDescription = (jobName: string): string | undefined =>
  JOB_DISPLAY[jobName]?.description;

export const groupLabel = (jobGroup: string): string =>
  GROUP_DISPLAY[jobGroup]?.label ?? humanizeIdentifier(jobGroup);

export const groupDescription = (jobGroup: string): string | undefined =>
  GROUP_DISPLAY[jobGroup]?.description;
