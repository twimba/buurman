import { useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Home } from 'lucide-react';
import { usePropertyTimeline } from '@/hooks/useOccupancyPeriodHooks';
import { useFormatDate } from '@/hooks/useFormatDate';
import {
  OccupancyType,
  OCCUPANCY_TYPE_LABELS,
  type TimelineEntry,
} from '@/types/occupancyPeriod';

interface PropertyLifecycleTimelineProps {
  propertyIdentifier: string;
  onSelfOccupancyClick?: (identifier: string) => void;
}

// ─── Layout constants ──────────────────────────────────────────────────────────

const BAR_TOP = 36;
const AXIS_TOP = 52;
const LABEL_TOP = 68;
const TOTAL_HEIGHT = 100;

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
  entries: TimelineEntry[]
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

  ticks.sort((a, b) => a.pct - b.pct);

  const pruned: LabelTick[] = [];
  for (const tick of ticks) {
    const last = pruned[pruned.length - 1];
    if (!last || tick.pct - last.pct >= 6) {
      pruned.push(tick);
    }
  }

  return pruned;
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
    entry.endDate ? formatDate(entry.endDate) : 'Ongoing'
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
              ? 'Contract'
              : isSelfOccupancy
                ? 'Self-Occupied'
                : 'Vacancy'}
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
            {isContract ? '↗ Open contract' : '✎ Click to edit'}
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
              if (e.key === 'Enter' || e.key === ' ') {
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

const TimelineSkeleton = () => (
  <div
    className="w-full rounded-2xl overflow-hidden"
    style={{
      height: TOTAL_HEIGHT,
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
}: PropertyLifecycleTimelineProps) => {
  const { data: timeline, isLoading } = usePropertyTimeline(propertyIdentifier);
  const navigate = useNavigate();
  const { formatDate } = useFormatDate();

  const [tooltip, setTooltip] = useState<TooltipState | null>(null);

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

  if (isLoading) {
    return <TimelineSkeleton />;
  }

  const acquisitionDateStr = timeline?.acquisitionDate ?? null;
  const entries = timeline?.entries ?? [];

  if (!entries.length && !acquisitionDateStr) {
    return null;
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

  const labelTicks = buildLabelTicks(
    timelineStart,
    totalDays,
    formatDate,
    acquisitionDate,
    entries
  );

  return (
    <div
      className="w-full select-none"
      style={{
        height: TOTAL_HEIGHT,
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
      <div
        style={{
          position: 'absolute',
          left: `${todayPct}%`,
          top: AXIS_TOP,
          transform: 'translateX(-50%)',
          zIndex: 20,
          pointerEvents: 'none',
        }}
      >
        <div
          style={{
            position: 'absolute',
            left: '50%',
            top: '50%',
            transform: 'translate(-50%, -50%)',
            width: 16,
            height: 16,
            borderRadius: '50%',
            background: 'rgba(92,124,250,0.15)',
            animation: 'ping 2s cubic-bezier(0,0,0.2,1) infinite',
          }}
        />
        <div
          style={{
            position: 'absolute',
            left: '50%',
            top: '50%',
            transform: 'translate(-50%, -50%) rotate(45deg)',
            width: 8,
            height: 8,
            background: 'linear-gradient(135deg, #5c7cfa 0%, #818cf8 100%)',
            boxShadow: '0 0 10px rgba(92,124,250,0.8)',
            borderRadius: 2,
          }}
        />
        <div
          style={{
            position: 'absolute',
            top: -16,
            left: '50%',
            transform: 'translateX(-50%)',
            fontSize: 9,
            fontWeight: 700,
            letterSpacing: '0.06em',
            textTransform: 'uppercase',
            color: '#5c7cfa',
            whiteSpace: 'nowrap',
          }}
        >
          Today
        </div>
      </div>

      {/* ── Date label ticks ──────────────────────────────────────────────────── */}
      <div
        style={{
          position: 'absolute',
          left: 0,
          right: 0,
          top: LABEL_TOP,
        }}
      >
        {labelTicks.map((tick) => (
          <div
            key={tick.key}
            style={{
              position: 'absolute',
              left: `${tick.pct}%`,
              transform: 'translateX(-50%)',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              gap: 3,
            }}
          >
            <div
              style={{
                width: 1,
                height: 5,
                background:
                  tick.key === 'today' ? '#5c7cfa' : 'rgba(107,113,148,0.4)',
                borderRadius: 1,
              }}
            />
            <span
              style={{
                fontSize: 10,
                fontWeight: tick.key === 'today' ? 600 : 400,
                color:
                  tick.key === 'today'
                    ? '#5c7cfa'
                    : tick.key === 'acquired' || tick.key === 'start'
                      ? 'rgba(107,113,148,0.9)'
                      : 'rgba(107,113,148,0.65)',
                whiteSpace: 'nowrap',
                letterSpacing: '0.01em',
              }}
            >
              {tick.label}
            </span>
          </div>
        ))}
      </div>

      {/* ── Tooltip ───────────────────────────────────────────────────────────── */}
      {tooltip && (
        <BarTooltip
          tooltip={tooltip}
          formatDate={formatDate}
          isSelfOccupancyClickable={!!onSelfOccupancyClick}
        />
      )}
    </div>
  );
};
