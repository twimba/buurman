import { useState, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { Home } from 'lucide-react';
import { usePropertyTimeline } from '@/hooks/useOccupancyPeriodHooks';
import { useFormatDate } from '@/hooks/useFormatDate';
import {
  OccupancyType,
  OCCUPANCY_TYPE_LABELS,
  type TimelineEntry,
  type FinancingTimelineEntry,
} from '@/types/occupancyPeriod';

interface PropertyLifecycleTimelineProps {
  propertyIdentifier: string;
  onSelfOccupancyClick?: (identifier: string) => void;
  onFinancingClick?: (identifier: string) => void;
}

// ─── Layout constants ──────────────────────────────────────────────────────────

const BAR_TOP = 36;
const AXIS_TOP = 52;
const LABEL_TOP_BASE = 68;
const FINANCING_BAR_TOP = AXIS_TOP + 5;
const FINANCING_LANE_HEIGHT = 6;
const FINANCING_BAR_HEIGHT = 3;

// ─── Date Utilities ────────────────────────────────────────────────────────────

const parseDate = (iso: string): Date => new Date(iso);

const daysBetween = (a: Date, b: Date): number =>
  Math.max(1, Math.round((b.getTime() - a.getTime()) / 86_400_000));

const toPct = (
  startDate: Date,
  targetDate: Date,
  totalDays: number
): number => {
  const days = (targetDate.getTime() - startDate.getTime()) / 86_400_000;
  return Math.min(100, Math.max(0, (days / totalDays) * 100));
};

// ─── Label tick helpers ─────────────────────────────────────────────────────────

interface LabelTick {
  pct: number;
  label: string;
  key: string;
}

const buildLabelTicks = (
  timelineStart: Date,
  totalDays: number,
  formatDate: (d: string | Date) => string,
  acquisitionDate: Date | null,
  entries: TimelineEntry[],
  timelineEnd: Date
): LabelTick[] => {
  const ticks: LabelTick[] = [];
  const seen = new Set<number>();

  const add = (date: Date, label: string, key: string) => {
    const pct = toPct(timelineStart, date, totalDays);
    const bucket = Math.round(pct * 10);
    if (!seen.has(bucket)) {
      seen.add(bucket);
      ticks.push({ pct, label, key });
    }
  };

  if (acquisitionDate) {
    add(acquisitionDate, formatDate(acquisitionDate), 'acquired');
  } else {
    add(timelineStart, formatDate(timelineStart), 'start');
  }

  entries.forEach((entry) => {
    add(
      parseDate(entry.startDate),
      formatDate(entry.startDate),
      `${entry.identifier}-s`
    );
    if (entry.endDate) {
      add(
        parseDate(entry.endDate),
        formatDate(entry.endDate),
        `${entry.identifier}-e`
      );
    }
  });

  const today = new Date();
  add(today, 'Today', 'today');
  add(timelineEnd, formatDate(timelineEnd), 'end');

  ticks.sort((a, b) => a.pct - b.pct);

  // Normal left-to-right pruning — no force-pins yet
  const pruned: LabelTick[] = [];
  for (const tick of ticks) {
    const last = pruned[pruned.length - 1];
    if (!last || tick.pct - last.pct >= 6) {
      pruned.push(tick);
    }
  }

  // Pin 'today': if pruned, replace the nearest non-anchor conflicting tick
  if (!pruned.find((t) => t.key === 'today')) {
    const todayTick = ticks.find((t) => t.key === 'today');
    if (todayTick) {
      const conflictIdx = pruned.findIndex(
        (t) =>
          t.key !== 'acquired' &&
          t.key !== 'start' &&
          t.key !== 'end' &&
          Math.abs(t.pct - todayTick.pct) < 6
      );
      if (conflictIdx !== -1) {
        pruned.splice(conflictIdx, 1, todayTick);
      } else {
        pruned.push(todayTick);
        pruned.sort((a, b) => a.pct - b.pct);
      }
    }
  }

  // Pin 'end': if pruned, replace the nearest non-anchor conflicting tick
  // (skip if today is already occupying that spot)
  if (!pruned.find((t) => t.key === 'end')) {
    const endTick = ticks.find((t) => t.key === 'end');
    if (endTick) {
      const todayInPruned = pruned.find((t) => t.key === 'today');
      const todayConflicts =
        todayInPruned && Math.abs(todayInPruned.pct - endTick.pct) < 6;
      if (!todayConflicts) {
        // Find rightmost conflicting non-anchor tick to replace
        let conflictIdx = -1;
        for (let i = pruned.length - 1; i >= 0; i--) {
          const t = pruned[i];
          if (
            t.key !== 'today' &&
            t.key !== 'acquired' &&
            t.key !== 'start' &&
            Math.abs(t.pct - endTick.pct) < 6
          ) {
            conflictIdx = i;
            break;
          }
        }
        if (conflictIdx !== -1) {
          pruned.splice(conflictIdx, 1, endTick);
        } else {
          pruned.push(endTick);
          pruned.sort((a, b) => a.pct - b.pct);
        }
      }
    }
  }

  return pruned;
};

// ─── Financing lane assignment ─────────────────────────────────────────────────

interface FinancingBarInfo {
  financing: FinancingTimelineEntry;
  lane: number;
  leftPct: number;
  widthPct: number;
}

const assignFinancingLanes = (
  financings: FinancingTimelineEntry[],
  timelineStart: Date,
  timelineEnd: Date,
  totalDays: number
): FinancingBarInfo[] => {
  const sorted = [...financings].sort(
    (a, b) =>
      parseDate(a.startDate).getTime() - parseDate(b.startDate).getTime()
  );
  const laneRightPcts: number[] = [];

  return sorted.map((f) => {
    const startDate = parseDate(f.startDate);
    const endDate = f.endDate ? parseDate(f.endDate) : timelineEnd;
    const leftPct = toPct(timelineStart, startDate, totalDays);
    const rightPct = toPct(timelineStart, endDate, totalDays);
    const widthPct = rightPct - leftPct;

    let lane = laneRightPcts.findIndex((rp) => rp <= leftPct);
    if (lane === -1) {
      lane = laneRightPcts.length;
      laneRightPcts.push(rightPct);
    } else {
      laneRightPcts[lane] = rightPct;
    }

    return { financing: f, lane, leftPct, widthPct };
  });
};

// ─── Formatting helpers ─────────────────────────────────────────────────────────

const formatEnumLabel = (s: string): string =>
  s.replace(/_/g, '').replace(/\b\w/g, (c) => c.toUpperCase());

const formatCurrency = (amount: number, currency: string): string => {
  try {
    return new Intl.NumberFormat(undefined, {
      style: 'currency',
      currency,
      minimumFractionDigits: 0,
      maximumFractionDigits: 2,
    }).format(amount);
  } catch {
    return `${amount} ${currency}`;
  }
};

// ─── Tooltip ───────────────────────────────────────────────────────────────────

interface TooltipState {
  entry: TimelineEntry;
  x: number;
}

interface BarTooltipProps {
  tooltip: TooltipState;
  formatDate: (d: string | Date) => string;
  isSelfOccupancyClickable: boolean;
}

const BarTooltip = ({
  tooltip,
  formatDate,
  isSelfOccupancyClickable,
}: BarTooltipProps) => {
  const { t } = useTranslation('properties');
  const { entry, x } = tooltip;

  const isContract = entry.type === 'CONTRACT';
  const isSelfOccupancy = entry.type === 'SELF_OCCUPANCY';

  // For SELF_OCCUPANCY: description = type name, metadata = occupant name
  // For CONTRACT: description = status, metadata = contract type
  const typeLabel = isSelfOccupancy
    ? (OCCUPANCY_TYPE_LABELS[entry.description as OccupancyType] ??
      entry.description)
    : null;

  const occupantName = isSelfOccupancy ? entry.metadata : null;
  const contractType = isContract ? entry.metadata : null;
  const contractStatus = isContract ? entry.description : null;

  const accentColor = isContract
    ? '#60a5fa'
    : isSelfOccupancy
      ? '#a5b4fc'
      : '#9ca0b8';

  const glowColor = isContract
    ? 'rgba(96,165,250,0.25)'
    : isSelfOccupancy
      ? 'rgba(165,180,252,0.25)'
      : 'rgba(107,113,148,0.15)';

  const dateRange = `${formatDate(entry.startDate)} — ${
    entry.endDate ? formatDate(entry.endDate) : t('timeline.ongoing')
  }`;

  const clampedX = Math.min(Math.max(x, 8), 92);
  const showClickHint =
    isContract || (isSelfOccupancy && isSelfOccupancyClickable);

  return (
    <div
      className="absolute z-50 pointer-events-none"
      style={{
        left: `${clampedX}%`,
        top: BAR_TOP,
        transform: 'translateX(-50%) translateY(calc(-100% - 6px))',
      }}
    >
      {/* Tooltip card */}
      <div
        style={{
          background: 'rgba(10, 11, 20, 0.95)',
          border: `1px solid ${glowColor}`,
          borderRadius: 10,
          padding: '9px 13px',
          backdropFilter: 'blur(24px)',
          WebkitBackdropFilter: 'blur(24px)',
          boxShadow: `0 12px 40px rgba(0,0,0,0.55), 0 0 0 1px rgba(255,255,255,0.04), 0 0 20px ${glowColor}`,
          minWidth: 150,
          maxWidth: 250,
        }}
      >
        {/* Title row */}
        <div className="flex items-center gap-1.5" style={{ marginBottom: 5 }}>
          <div
            style={{
              width: 6,
              height: 6,
              borderRadius: '50%',
              background: accentColor,
              flexShrink: 0,
              boxShadow: `0 0 6px ${accentColor}`,
            }}
          />
          <span
            style={{
              color: accentColor,
              fontSize: 11,
              fontWeight: 700,
              letterSpacing: '0.05em',
              textTransform: 'uppercase',
            }}
          >
            {isContract
              ? t('timeline.contract')
              : isSelfOccupancy
                ? t('detail.timeline.selfOccupied')
                : t('timeline.vacancy')}
          </span>
          {typeLabel && (
            <span
              style={{
                color: 'rgba(139,144,168,0.75)',
                fontSize: 10,
                fontWeight: 400,
              }}
            >
              · {typeLabel}
            </span>
          )}
          {contractStatus && (
            <span
              style={{
                color: 'rgba(139,144,168,0.75)',
                fontSize: 10,
                fontWeight: 400,
              }}
            >
              · {contractStatus}
            </span>
          )}
        </div>

        {/* Occupant name */}
        {occupantName && (
          <div
            style={{
              color: '#c4c8db',
              fontSize: 11,
              marginBottom: 3,
            }}
          >
            {occupantName}
          </div>
        )}

        {/* Contract type */}
        {contractType && (
          <div
            style={{
              color: '#c4c8db',
              fontSize: 11,
              marginBottom: 3,
            }}
          >
            {contractType}
          </div>
        )}

        {/* Date range */}
        <div
          style={{
            color: 'rgba(107,113,148,0.85)',
            fontSize: 10,
            fontVariantNumeric: 'tabular-nums',
            letterSpacing: '0.01em',
          }}
        >
          {dateRange}
        </div>

        {/* Click hint */}
        {showClickHint && (
          <div
            style={{
              marginTop: 7,
              paddingTop: 6,
              borderTop: `1px solid ${glowColor}`,
              color: accentColor,
              fontSize: 10,
              fontWeight: 600,
              letterSpacing: '0.04em',
              opacity: 0.9,
            }}
          >
            {isContract
              ? t('lifecycle.openContract')
              : t('lifecycle.clickToEdit')}
          </div>
        )}
      </div>

      {/* Arrow pointing down toward bar */}
      <div
        style={{
          position: 'absolute',
          left: '50%',
          bottom: -5,
          transform: 'translateX(-50%)',
          width: 0,
          height: 0,
          borderLeft: '5px solid transparent',
          borderRight: '5px solid transparent',
          borderTop: `5px solid ${glowColor}`,
        }}
      />
    </div>
  );
};

// ─── Financing Tooltip ─────────────────────────────────────────────────────────

interface FinancingTooltipProps {
  financing: FinancingTimelineEntry;
  x: number;
  formatDate: (d: string | Date) => string;
}

const FinancingTooltip = ({
  financing,
  x,
  formatDate,
}: FinancingTooltipProps) => {
  const { t } = useTranslation('properties');
  const accentColor = '#f59e0b';
  const glowColor = 'rgba(245,158,11,0.2)';
  const clampedX = Math.min(Math.max(x, 8), 92);

  const dateRange = `${formatDate(financing.startDate)} — ${
    financing.endDate ? formatDate(financing.endDate) : t('timeline.ongoing')
  }`;

  return (
    <div
      className="absolute z-50 pointer-events-none"
      style={{
        left: `${clampedX}%`,
        top: BAR_TOP,
        transform: 'translateX(-50%) translateY(calc(-100% - 6px))',
      }}
    >
      <div
        style={{
          background: 'rgba(10, 11, 20, 0.95)',
          border: `1px solid ${glowColor}`,
          borderRadius: 10,
          padding: '9px 13px',
          backdropFilter: 'blur(24px)',
          WebkitBackdropFilter: 'blur(24px)',
          boxShadow: `0 12px 40px rgba(0,0,0,0.55), 0 0 0 1px rgba(255,255,255,0.04), 0 0 20px ${glowColor}`,
          minWidth: 150,
          maxWidth: 260,
        }}
      >
        {/* Title row */}
        <div className="flex items-center gap-1.5" style={{ marginBottom: 5 }}>
          <div
            style={{
              width: 6,
              height: 6,
              borderRadius: '50%',
              background: accentColor,
              flexShrink: 0,
              boxShadow: `0 0 6px ${accentColor}`,
            }}
          />
          <span
            style={{
              color: accentColor,
              fontSize: 11,
              fontWeight: 700,
              letterSpacing: '0.05em',
              textTransform: 'uppercase',
            }}
          >
            {formatEnumLabel(financing.financingType)}
          </span>
          <span
            style={{
              color: 'rgba(139,144,168,0.75)',
              fontSize: 10,
              fontWeight: 400,
            }}
          >
            · {formatEnumLabel(financing.status)}
          </span>
        </div>

        {/* Lender */}
        {financing.lenderName && (
          <div style={{ color: '#c4c8db', fontSize: 11, marginBottom: 3 }}>
            {financing.lenderName}
          </div>
        )}

        {/* Amount + rate */}
        <div style={{ color: '#c4c8db', fontSize: 11, marginBottom: 3 }}>
          {formatCurrency(
            financing.originalAmount,
            financing.originalAmountCurrency
          )}
          {financing.interestRate != null && (
            <span style={{ color: 'rgba(139,144,168,0.75)', marginLeft: 6 }}>
              @ {financing.interestRate}%
            </span>
          )}
        </div>

        {/* Date range */}
        <div
          style={{
            color: 'rgba(107,113,148,0.85)',
            fontSize: 10,
            fontVariantNumeric: 'tabular-nums',
            letterSpacing: '0.01em',
          }}
        >
          {dateRange}
        </div>

        {/* Click hint */}
        <div
          style={{
            marginTop: 7,
            paddingTop: 6,
            borderTop: `1px solid ${glowColor}`,
            color: accentColor,
            fontSize: 10,
            fontWeight: 600,
            letterSpacing: '0.04em',
            opacity: 0.9,
          }}
        >
          ✎ Click to edit
        </div>
      </div>

      {/* Arrow */}
      <div
        style={{
          position: 'absolute',
          left: '50%',
          bottom: -5,
          transform: 'translateX(-50%)',
          width: 0,
          height: 0,
          borderLeft: '5px solid transparent',
          borderRight: '5px solid transparent',
          borderTop: `5px solid ${glowColor}`,
        }}
      />
    </div>
  );
};

// ─── Financing Bar ─────────────────────────────────────────────────────────────

interface FinancingBarComponentProps {
  info: FinancingBarInfo;
  onHover: (f: FinancingTimelineEntry | null) => void;
  onClick: () => void;
}

const FinancingBarComponent = ({
  info,
  onHover,
  onClick,
}: FinancingBarComponentProps) => {
  const [hovered, setHovered] = useState(false);
  const { financing, lane, leftPct, widthPct } = info;
  const top = FINANCING_BAR_TOP + lane * FINANCING_LANE_HEIGHT;

  const handleMouseEnter = useCallback(() => {
    setHovered(true);
    onHover(financing);
  }, [financing, onHover]);

  const handleMouseLeave = useCallback(() => {
    setHovered(false);
    onHover(null);
  }, [onHover]);

  return (
    <div
      role="button"
      tabIndex={0}
      aria-label={`Financing ${financing.identifier}, click to edit`}
      onMouseEnter={handleMouseEnter}
      onMouseLeave={handleMouseLeave}
      onClick={onClick}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === '') {
          onClick();
        }
      }}
      style={{
        position: 'absolute',
        left: `${leftPct}%`,
        width: `${Math.max(widthPct, 0.8)}%`,
        top,
        height: FINANCING_BAR_HEIGHT,
        borderRadius: 999,
        background: hovered ? 'rgba(245,158,11,0.55)' : 'rgba(245,158,11,0.2)',
        boxShadow: hovered ? '0 0 5px rgba(245,158,11,0.3)' : 'none',
        cursor: 'pointer',
        transition: 'background 150ms ease, box-shadow 150ms ease',
        zIndex: hovered ? 10 : 1,
      }}
    />
  );
};

// ─── Period Bar ────────────────────────────────────────────────────────────────

interface PeriodBarProps {
  entry: TimelineEntry;
  leftPct: number;
  widthPct: number;
  isContract: boolean;
  isSelfOccupancy: boolean;
  isClickable: boolean;
  onHover: (entry: TimelineEntry | null, midPct: number) => void;
  onClick: () => void;
}

const PeriodBar = ({
  entry,
  leftPct,
  widthPct,
  isContract,
  isSelfOccupancy,
  isClickable,
  onHover,
  onClick,
}: PeriodBarProps) => {
  const [hovered, setHovered] = useState(false);

  const baseColor = isContract
    ? 'rgba(59,130,246,1)'
    : isSelfOccupancy
      ? 'rgba(99,102,241,1)'
      : 'rgba(107,113,148,0.5)';

  const glowColor = isContract
    ? 'rgba(59,130,246,0.4)'
    : isSelfOccupancy
      ? 'rgba(99,102,241,0.4)'
      : 'rgba(107,113,148,0.2)';

  const gradientStart = isContract
    ? 'rgba(96,165,250,1)'
    : isSelfOccupancy
      ? 'rgba(129,140,248,1)'
      : 'rgba(107,113,148,0.6)';

  const handleMouseEnter = useCallback(() => {
    setHovered(true);
    onHover(entry, leftPct + widthPct / 2);
  }, [entry, leftPct, widthPct, onHover]);

  const handleMouseLeave = useCallback(() => {
    setHovered(false);
    onHover(null, 0);
  }, [onHover]);

  return (
    <div
      role={isClickable ? 'button' : undefined}
      tabIndex={isClickable ? 0 : undefined}
      aria-label={
        isContract
          ? `Contract ${entry.identifier}, click to open`
          : isSelfOccupancy
            ? `Self-occupancy period${isClickable ? ', click to edit' : ''}`
            : 'Vacancy period'
      }
      onMouseEnter={handleMouseEnter}
      onMouseLeave={handleMouseLeave}
      onClick={isClickable ? onClick : undefined}
      onKeyDown={
        isClickable
          ? (e) => {
              if (e.key === 'Enter' || e.key === '') {
                onClick();
              }
            }
          : undefined
      }
      style={{
        position: 'absolute',
        left: `${leftPct}%`,
        width: `${Math.max(widthPct, 0.8)}%`,
        height: '10px',
        borderRadius: '999px',
        background: `linear-gradient(90deg, ${gradientStart} 0%, ${baseColor} 100%)`,
        boxShadow: hovered
          ? `0 0 10px 2px ${glowColor}, inset 0 1px 0 rgba(255,255,255,0.2)`
          : `inset 0 1px 0 rgba(255,255,255,0.15)`,
        cursor: isClickable ? 'pointer' : 'default',
        transition:
          'box-shadow 150ms ease, opacity 150ms ease, filter 150ms ease',
        opacity: hovered ? 1 : 0.9,
        filter: hovered ? 'brightness(1.15)' : 'brightness(1)',
        zIndex: hovered ? 10 : 2,
      }}
    />
  );
};

// ─── Skeleton ──────────────────────────────────────────────────────────────────

const SKELETON_HEIGHT = 100;

const TimelineSkeleton = () => (
  <div
    className="w-full rounded-2xl overflow-hidden"
    style={{
      height: SKELETON_HEIGHT,
      background: 'rgba(30,33,48,0.4)',
      padding: '16px 20px',
    }}
  >
    <div
      className="h-2 rounded-full mb-4"
      style={{
        background:
          'linear-gradient(90deg, rgba(255,255,255,0.03) 0%, rgba(255,255,255,0.08) 50%, rgba(255,255,255,0.03) 100%)',
        animation: 'pulse 2s cubic-bezier(0.4,0,0.6,1) infinite',
      }}
    />
    <div className="flex gap-3">
      {[40, 25, 15, 20].map((w, i) => (
        <div
          key={i}
          style={{
            width: `${w}%`,
            height: 10,
            borderRadius: 999,
            background:
              'linear-gradient(90deg, rgba(255,255,255,0.04) 0%, rgba(255,255,255,0.1) 50%, rgba(255,255,255,0.04) 100%)',
            animation: 'pulse 2s cubic-bezier(0.4,0,0.6,1) infinite',
            animationDelay: `${i * 150}ms`,
          }}
        />
      ))}
    </div>
  </div>
);

// ─── Main Component ────────────────────────────────────────────────────────────

export const PropertyLifecycleTimeline = ({
  propertyIdentifier,
  onSelfOccupancyClick,
  onFinancingClick,
}: PropertyLifecycleTimelineProps) => {
  const { data: timeline, isLoading } = usePropertyTimeline(propertyIdentifier);
  const navigate = useNavigate();
  const { formatDate } = useFormatDate();

  const [tooltip, setTooltip] = useState<TooltipState | null>(null);
  const [financingTooltip, setFinancingTooltip] = useState<{
    financing: FinancingTimelineEntry;
    x: number;
  } | null>(null);

  const handleHover = useCallback(
    (entry: TimelineEntry | null, midPct: number) => {
      if (entry === null) {
        setTooltip(null);
      } else {
        setTooltip({ entry, x: midPct });
      }
    },
    []
  );

  const handleFinancingHover = useCallback(
    (f: FinancingTimelineEntry | null, midPct?: number) => {
      if (f === null) {
        setFinancingTooltip(null);
      } else {
        setFinancingTooltip({ financing: f, x: midPct ?? 50 });
      }
    },
    []
  );

  if (isLoading) {
    return <TimelineSkeleton />;
  }

  const acquisitionDateStr = timeline?.acquisitionDate ?? null;
  const entries = timeline?.entries ?? [];
  const financings = timeline?.financings ?? [];

  if (!entries.length && !acquisitionDateStr && !financings.length) {
    return (
      <div
        className="w-full select-none"
        style={{ height: 100, position: 'relative' }}
      >
        {/* "No activity yet" — above the axis */}
        <div
          style={{
            position: 'absolute',
            left: 0,
            right: 0,
            top: 8,
            textAlign: 'center',
            fontSize: 12,
            fontWeight: 600,
            color: 'rgba(107,113,148,0.7)',
            letterSpacing: '0.01em',
          }}
        >
          No activity yet
        </div>

        {/* Ghost dots — just above the axis */}
        {[
          { pct: 22, color: 'rgba(59,130,246,0.25)', width: 32 },
          { pct: 50, color: 'rgba(99,102,241,0.25)', width: 24 },
          { pct: 78, color: 'rgba(245,158,11,0.2)', width: 20 },
        ].map(({ pct, color, width }) => (
          <div
            key={pct}
            style={{
              position: 'absolute',
              left: `${pct}%`,
              top: 30,
              transform: 'translateX(-50%)',
              width,
              height: 10,
              borderRadius: 999,
              background: color,
            }}
          />
        ))}

        {/* Ghost axis */}
        <div
          style={{
            position: 'absolute',
            left: 0,
            right: 0,
            top: 46,
            height: 2,
            borderRadius: 999,
            background:
              'repeating-linear-gradient(90deg, rgba(107,113,148,0.18) 0px, rgba(107,113,148,0.18) 6px, transparent 6px, transparent 12px)',
          }}
        />

        {/* Subtitle — below the axis */}
        <div
          style={{
            position: 'absolute',
            left: 0,
            right: 0,
            top: 60,
            textAlign: 'center',
            fontSize: 11,
            color: 'rgba(107,113,148,0.5)',
            lineHeight: 1.5,
          }}
        >
          Contracts, self-occupancy periods and financings will appear here once
          added.
        </div>
      </div>
    );
  }

  // ── Compute timeline bounds ─────────────────────────────────────────────────

  const today = new Date();
  today.setHours(23, 59, 59, 999);

  const allDates: Date[] = entries.flatMap((e) => {
    const dates: Date[] = [parseDate(e.startDate)];
    if (e.endDate) {
      dates.push(parseDate(e.endDate));
    }
    return dates;
  });

  // Include financing dates in bounds
  for (const f of financings) {
    allDates.push(parseDate(f.startDate));
    if (f.endDate) {
      allDates.push(parseDate(f.endDate));
    }
  }

  const acquisitionDate = acquisitionDateStr
    ? parseDate(acquisitionDateStr)
    : null;

  const oneYearAgo = new Date(today);
  oneYearAgo.setFullYear(oneYearAgo.getFullYear() - 1);

  const timelineStart =
    acquisitionDate ??
    (allDates.length > 0
      ? new Date(Math.min(...allDates.map((d) => d.getTime())))
      : oneYearAgo);

  const latestEntryEnd =
    allDates.length > 0
      ? new Date(Math.max(...allDates.map((d) => d.getTime())))
      : today;

  const timelineEnd = latestEntryEnd > today ? latestEntryEnd : today;

  const totalDays = daysBetween(timelineStart, timelineEnd);
  const todayPct = toPct(timelineStart, today, totalDays);

  // ── Financing bar layout ────────────────────────────────────────────────────

  const financingBars = assignFinancingLanes(
    financings,
    timelineStart,
    timelineEnd,
    totalDays
  );
  const numFinancingLanes =
    financingBars.length > 0
      ? Math.max(...financingBars.map((b) => b.lane)) + 1
      : 0;

  const labelTop = Math.max(
    LABEL_TOP_BASE,
    FINANCING_BAR_TOP + numFinancingLanes * FINANCING_LANE_HEIGHT + 4
  );
  const totalHeight = labelTop + 28;

  const labelTicks = buildLabelTicks(
    timelineStart,
    totalDays,
    formatDate,
    acquisitionDate,
    entries,
    timelineEnd
  );

  return (
    <div
      className="w-full select-none"
      style={{
        height: totalHeight,
        position: 'relative',
        fontFamily: 'inherit',
      }}
    >
      {/* ── Axis rail ─────────────────────────────────────────────────────────── */}
      <div
        style={{
          position: 'absolute',
          left: 0,
          right: 0,
          top: AXIS_TOP,
          height: 2,
          borderRadius: 999,
        }}
        className="[background:linear-gradient(90deg,#d1d5db_0%,#9ca3af_40%,rgba(92,124,250,0.5)_85%,rgba(92,124,250,0.9)_100%)] dark:[background:linear-gradient(90deg,#374151_0%,#4b5563_40%,rgba(92,124,250,0.6)_85%,rgba(92,124,250,0.95)_100%)]"
      />

      {/* ── Period bars — all at same level ───────────────────────────────────── */}
      {entries.map((entry) => {
        const isContract = entry.type === 'CONTRACT';
        const isSelfOccupancy = entry.type === 'SELF_OCCUPANCY';
        const isClickable =
          isContract || (isSelfOccupancy && !!onSelfOccupancyClick);

        const leftPct = toPct(
          timelineStart,
          parseDate(entry.startDate),
          totalDays
        );
        const endDate = entry.endDate ? parseDate(entry.endDate) : today;
        const rightPct = toPct(timelineStart, endDate, totalDays);
        const widthPct = rightPct - leftPct;

        return (
          <div
            key={entry.identifier}
            style={{
              position: 'absolute',
              left: `${leftPct}%`,
              width: `${Math.max(widthPct, 0.8)}%`,
              top: BAR_TOP,
              height: 10,
            }}
          >
            <PeriodBar
              entry={entry}
              leftPct={0}
              widthPct={100}
              isContract={isContract}
              isSelfOccupancy={isSelfOccupancy}
              isClickable={isClickable}
              onHover={(e, midPct) =>
                handleHover(e, leftPct + (midPct / 100) * widthPct)
              }
              onClick={() => {
                if (isContract) {
                  navigate(`/contracts/${entry.identifier}`);
                } else if (isSelfOccupancy && onSelfOccupancyClick) {
                  onSelfOccupancyClick(entry.identifier);
                }
              }}
            />
          </div>
        );
      })}

      {/* ── Acquisition marker ─────────────────────────────────────────────────── */}
      {acquisitionDate && (
        <div
          style={{
            position: 'absolute',
            left: `${toPct(timelineStart, acquisitionDate, totalDays)}%`,
            top: AXIS_TOP,
            transform: 'translateX(-50%)',
            zIndex: 20,
          }}
        >
          <div
            style={{
              width: 18,
              height: 18,
              borderRadius: '50%',
              background: 'linear-gradient(135deg, #5c7cfa 0%, #818cf8 100%)',
              border: '2px solid rgba(255,255,255,0.15)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: '0 0 8px rgba(92,124,250,0.5)',
              transform: 'translate(-50%, -50%)',
              position: 'absolute',
              left: '50%',
              top: '50%',
            }}
          >
            <Home
              style={{ width: 9, height: 9, color: 'white' }}
              strokeWidth={2.5}
            />
          </div>
        </div>
      )}

      {/* ── Today marker ──────────────────────────────────────────────────────── */}
      {/* Soft glow blur layer behind the beam */}
      <div
        style={{
          position: 'absolute',
          left: `${todayPct}%`,
          top: 0,
          bottom: 0,
          width: 6,
          transform: 'translateX(-50%)',
          background:
            'linear-gradient(to bottom, transparent 0%, rgba(92,124,250,0.12) 20%, rgba(92,124,250,0.22) 48%, rgba(92,124,250,0.18) 62%, transparent 100%)',
          filter: 'blur(3px)',
          pointerEvents: 'none',
          zIndex: 4,
        }}
      />
      {/* Sharp gradient beam */}
      <div
        style={{
          position: 'absolute',
          left: `${todayPct}%`,
          top: 0,
          bottom: 0,
          width: 1.5,
          transform: 'translateX(-50%)',
          background:
            'linear-gradient(to bottom, transparent 0%, rgba(92,124,250,0.25) 18%, rgba(92,124,250,0.7) 44%, rgba(92,124,250,0.75) 54%, rgba(92,124,250,0.4) 72%, transparent 100%)',
          pointerEvents: 'none',
          zIndex: 5,
        }}
      />
      {/* Pulsing halo ring on axis */}
      <div
        className="animate-ping"
        style={{
          position: 'absolute',
          left: `${todayPct}%`,
          top: AXIS_TOP + 1,
          transform: 'translate(-50%, -50%)',
          width: 18,
          height: 18,
          borderRadius: '50%',
          background: 'rgba(92,124,250,0.18)',
          pointerEvents: 'none',
          zIndex: 8,
          animationDuration: '2.5s',
        }}
      />
      {/* Solid dot with white ring + glow */}
      <div
        style={{
          position: 'absolute',
          left: `${todayPct}%`,
          top: AXIS_TOP + 1,
          transform: 'translate(-50%, -50%)',
          width: 9,
          height: 9,
          borderRadius: '50%',
          background: 'linear-gradient(135deg, #5c7cfa 0%, #818cf8 100%)',
          boxShadow: '0 0 0 2.5px white, 0 0 10px rgba(92,124,250,0.65)',
          pointerEvents: 'none',
          zIndex: 20,
        }}
      />

      {/* ── Financing bars — below axis ───────────────────────────────────────── */}
      {financingBars.map((info) => (
        <FinancingBarComponent
          key={info.financing.identifier}
          info={info}
          onHover={(f) =>
            handleFinancingHover(
              f,
              f !== null ? info.leftPct + info.widthPct / 2 : undefined
            )
          }
          onClick={() => onFinancingClick?.(info.financing.identifier)}
        />
      ))}

      {/* ── Date label ticks ──────────────────────────────────────────────────── */}
      <div
        style={{
          position: 'absolute',
          left: 0,
          right: 0,
          top: labelTop,
        }}
      >
        {labelTicks.map((tick) => (
          <div
            key={tick.key}
            style={{
              position: 'absolute',
              left: `${tick.pct}%`,
              transform:
                tick.pct < 4
                  ? 'translateX(0)'
                  : tick.pct > 96
                    ? 'translateX(-100%)'
                    : 'translateX(-50%)',
              display: 'flex',
              flexDirection: 'column',
              alignItems:
                tick.pct < 4
                  ? 'flex-start'
                  : tick.pct > 96
                    ? 'flex-end'
                    : 'center',
              gap: 3,
            }}
          >
            {tick.key === 'today' ? (
              <span
                style={{
                  fontSize: 9,
                  fontWeight: 700,
                  letterSpacing: '0.07em',
                  textTransform: 'uppercase',
                  color: 'white',
                  background:
                    'linear-gradient(135deg, #5c7cfa 0%, #818cf8 100%)',
                  padding: '2px 7px',
                  borderRadius: 999,
                  boxShadow:
                    '0 2px 8px rgba(92,124,250,0.45), 0 0 0 1px rgba(92,124,250,0.2)',
                  whiteSpace: 'nowrap',
                }}
              >
                Today
              </span>
            ) : (
              <>
                <div
                  style={{
                    width: 1,
                    height: 5,
                    background: 'rgba(107,113,148,0.4)',
                    borderRadius: 1,
                  }}
                />
                <span
                  style={{
                    fontSize: 10,
                    fontWeight:
                      tick.key === 'acquired' || tick.key === 'start'
                        ? 500
                        : 400,
                    color:
                      tick.key === 'acquired' || tick.key === 'start'
                        ? 'rgba(107,113,148,0.9)'
                        : 'rgba(107,113,148,0.65)',
                    whiteSpace: 'nowrap',
                    letterSpacing: '0.01em',
                  }}
                >
                  {tick.label}
                </span>
              </>
            )}
          </div>
        ))}
      </div>

      {/* ── Tooltip ───────────────────────────────────────────────────────────── */}
      {tooltip && !financingTooltip && (
        <BarTooltip
          tooltip={tooltip}
          formatDate={formatDate}
          isSelfOccupancyClickable={!!onSelfOccupancyClick}
        />
      )}
      {financingTooltip && (
        <FinancingTooltip
          financing={financingTooltip.financing}
          x={financingTooltip.x}
          formatDate={formatDate}
        />
      )}
    </div>
  );
};
