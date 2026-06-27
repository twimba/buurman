/**
 * Severity styling for the mission-control dashboard. Color is reinforcement only — every
 * consumer must also render the visible/aria label, never color alone.
 */
export type Severity = 'crit' | 'warn' | 'ok' | 'info';

interface SeverityStyle {
  /** Small filled status dot (semantic Tailwind background class). */
  dot: string;
  /** Chip background + text. */
  chip: string;
  /** Foreground text color. */
  text: string;
  /** Accessible label. */
  label: string;
}

const STYLES: Record<Severity, SeverityStyle> = {
  crit: {
    dot: 'bg-error',
    chip: 'bg-error-bg text-error-text',
    text: 'text-error-text',
    label: 'Critical',
  },
  warn: {
    dot: 'bg-warning',
    chip: 'bg-warning-bg text-warning-text',
    text: 'text-warning-text',
    label: 'Warning',
  },
  ok: {
    dot: 'bg-success',
    chip: 'bg-success-bg text-success-text',
    text: 'text-success-text',
    label: 'OK',
  },
  info: {
    dot: 'bg-info',
    chip: 'bg-info-bg text-info-text',
    text: 'text-info-text',
    label: 'Info',
  },
};

const RANK: Record<Severity, number> = { crit: 0, warn: 1, info: 2, ok: 3 };

export const severityStyle = (severity?: string): SeverityStyle =>
  STYLES[(severity as Severity) in STYLES ? (severity as Severity) : 'info'];

export const severityRank = (severity?: string): number =>
  RANK[(severity as Severity) in RANK ? (severity as Severity) : 'info'];
