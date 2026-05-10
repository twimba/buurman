import { useState, useMemo, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { Calendar } from 'lucide-react';

export type PeriodPreset =
  | 'month'
  | 'quarter'
  | 'ytd'
  | 'year'
  | '3'
  | '6'
  | '12'
  | '24'
  | '36'
  | 'all'
  | 'custom';

export interface PeriodDateRange {
  startDate: string; // YYYY-MM-DD
  endDate: string; // YYYY-MM-DD
}

export interface PeriodFilterProps {
  /** Which preset buttons to show. Defaults to reports-style. */
  presets?: PeriodPreset[];
  /** Called when the date range changes. */
  onChange: (range: PeriodDateRange) => void;
  /** Default preset on mount. */
  defaultPreset?: PeriodPreset;
}

const PRESET_LABEL_KEYS: Record<PeriodPreset, string> = {
  month: 'period.thisMonth',
  quarter: 'period.thisQuarter',
  ytd: 'period.ytd',
  year: 'period.thisYear',
  '3': 'period.3m',
  '6': 'period.6m',
  '12': 'period.12m',
  '24': 'period.24m',
  '36': 'period.36m',
  all: 'period.allTime',
  custom: 'period.custom',
};

const DEFAULT_PRESETS: PeriodPreset[] = [
  'month',
  'quarter',
  'year',
  'all',
  'custom',
];

function computeDateRange(
  preset: PeriodPreset,
  customStart: string,
  customEnd: string
): PeriodDateRange {
  const today = new Date();
  const endDate = today.toISOString().split('T')[0];
  let startDate: string;

  switch (preset) {
    case 'month': {
      const d = new Date(today.getFullYear(), today.getMonth(), 1);
      startDate = d.toISOString().split('T')[0];
      break;
    }
    case 'quarter': {
      const q = Math.floor(today.getMonth() / 3);
      const d = new Date(today.getFullYear(), q * 3, 1);
      startDate = d.toISOString().split('T')[0];
      break;
    }
    case 'ytd': {
      const d = new Date(today.getFullYear(), 0, 1);
      startDate = d.toISOString().split('T')[0];
      break;
    }
    case 'year': {
      const d = new Date(today.getFullYear(), 0, 1);
      startDate = d.toISOString().split('T')[0];
      break;
    }
    case '3':
    case '6':
    case '12':
    case '24':
    case '36': {
      const months = parseInt(preset, 10);
      const d = new Date(today);
      d.setMonth(d.getMonth() - months);
      startDate = d.toISOString().split('T')[0];
      break;
    }
    case 'all':
      startDate = '1982-07-24';
      break;
    case 'custom':
      startDate =
        customStart ||
        new Date(today.getFullYear(), today.getMonth(), 1)
          .toISOString()
          .split('T')[0];
      return {
        startDate,
        endDate: customEnd || endDate,
      };
  }

  return { startDate, endDate };
}

export function PeriodFilter({
  presets = DEFAULT_PRESETS,
  onChange,
  defaultPreset = 'month',
}: PeriodFilterProps) {
  const { t } = useTranslation('common');
  const [activePreset, setActivePreset] = useState<PeriodPreset>(defaultPreset);
  const [customStartDate, setCustomStartDate] = useState('');
  const [customEndDate, setCustomEndDate] = useState('');

  const range = useMemo(
    () => computeDateRange(activePreset, customStartDate, customEndDate),
    [activePreset, customStartDate, customEndDate]
  );

  const handlePresetChange = useCallback(
    (preset: PeriodPreset) => {
      setActivePreset(preset);
      const newRange = computeDateRange(preset, customStartDate, customEndDate);
      onChange(newRange);
    },
    [customStartDate, customEndDate, onChange]
  );

  const handleCustomStartChange = useCallback(
    (value: string) => {
      setCustomStartDate(value);
      setActivePreset('custom');
      onChange(computeDateRange('custom', value, customEndDate));
    },
    [customEndDate, onChange]
  );

  const handleCustomEndChange = useCallback(
    (value: string) => {
      setCustomEndDate(value);
      setActivePreset('custom');
      onChange(computeDateRange('custom', customStartDate, value));
    },
    [customStartDate, onChange]
  );

  // Trigger initial range on mount
  useMemo(() => {
    onChange(range);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div className="flex items-center gap-2 flex-wrap">
      <Calendar className="h-4 w-4 text-text-secondary shrink-0" />
      <div className="flex gap-1 flex-wrap">
        {presets.map((preset) => (
          <button
            key={preset}
            onClick={() => handlePresetChange(preset)}
            className={`px-3 py-1.5 rounded-md text-xs font-medium transition-colors ${
              activePreset === preset
                ? 'bg-primary-500 text-white'
                : 'bg-surface-inset text-text-secondary hover:bg-neutral-100'
            }`}
          >
            {t(PRESET_LABEL_KEYS[preset])}
          </button>
        ))}
      </div>
      {activePreset === 'custom' && (
        <div className="flex items-center gap-2">
          <input
            type="date"
            value={customStartDate}
            onChange={(e) => handleCustomStartChange(e.target.value)}
            className="px-2 py-1.5 border border-border-strong rounded-md text-xs focus:ring-2 focus:ring-primary-500 focus:border-transparent bg-surface-card text-text-primary"
          />
          <span className="text-xs text-text-secondary">{t('period.to')}</span>
          <input
            type="date"
            value={customEndDate}
            onChange={(e) => handleCustomEndChange(e.target.value)}
            className="px-2 py-1.5 border border-border-strong rounded-md text-xs focus:ring-2 focus:ring-primary-500 focus:border-transparent bg-surface-card text-text-primary"
          />
        </div>
      )}
    </div>
  );
}

/** Hook version for pages that need startDate/endDate in state */
export function usePeriodFilter(defaultPreset: PeriodPreset = 'month') {
  const [range, setRange] = useState<PeriodDateRange>(() =>
    computeDateRange(defaultPreset, '', '')
  );
  return { range, setRange };
}
