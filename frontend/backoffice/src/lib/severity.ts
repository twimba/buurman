/**
 * Severity styling for the mission-control dashboard. Color is reinforcement only — every
 * consumer must also render the visible/aria label, never color alone.
 */
export type Severity = 'crit' | 'warn' | 'ok' | 'info';

interface SeverityStyle {
  /** Small filled status dot. */
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
    dot: 'bg-red-500',
    chip: 'bg-red-50 text-red-700',
    text: 'text-red-700',
    label: 'Critical',
  },
  warn: {
    dot: 'bg-amber-500',
    chip: 'bg-amber-50 text-amber-700',
    text: 'text-amber-700',
    label: 'Warning',
  },
  ok: {
    dot: 'bg-emerald-500',
    chip: 'bg-emerald-50 text-emerald-700',
    text: 'text-emerald-700',
    label: 'OK',
  },
  info: {
    dot: 'bg-primary-500',
    chip: 'bg-primary-50 text-primary-700',
    text: 'text-primary-700',
    label: 'Info',
  },
};

const RANK: Record<Severity, number> = { crit: 0, warn: 1, info: 2, ok: 3 };

export const severityStyle = (severity?: string): SeverityStyle =>
  STYLES[(severity as Severity) in STYLES ? (severity as Severity) : 'info'];

export const severityRank = (severity?: string): number =>
  RANK[(severity as Severity) in RANK ? (severity as Severity) : 'info'];
