import { useState } from 'react';
import { Search, X, SlidersHorizontal } from 'lucide-react';
import { cn } from '../utils/cn';

export type FilterDef =
  | {
      type: 'toggle';
      key: string;
      label: string;
      options: { value: string | undefined; label: string }[];
    }
  | {
      type: 'select';
      key: string;
      label: string;
      options: { value: string; label: string }[];
    }
  | {
      type: 'search';
      key: string;
      label: string;
      placeholder?: string;
      debounce?: number;
    };

interface FilterBarProps {
  filters: FilterDef[];
  values: Record<string, string | undefined>;
  onChange: (values: Record<string, string | undefined>) => void;
  onReset?: () => void;
  className?: string;
}

export function FilterBar({
  filters,
  values,
  onChange,
  onReset,
  className,
}: FilterBarProps) {
  const [mobileOpen, setMobileOpen] = useState(false);
  const activeCount = Object.values(values).filter(Boolean).length;

  const updateFilter = (key: string, value: string | undefined) => {
    onChange({ ...values, [key]: value || undefined });
  };

  const filterContent = (
    <div className={cn('flex flex-wrap items-center gap-3', className)}>
      {filters.map((filter) => {
        if (filter.type === 'search') {
          return (
            <div key={filter.key} className="relative min-w-[200px] flex-1">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-text-muted" />
              <input
                type="search"
                placeholder={filter.placeholder || filter.label}
                value={values[filter.key] || ''}
                onChange={(e) => updateFilter(filter.key, e.target.value)}
                className="h-9 w-full rounded-md border border-border-default bg-surface-card pl-9 pr-3 text-sm text-text-primary placeholder:text-text-muted focus-ring"
              />
            </div>
          );
        }

        if (filter.type === 'select') {
          return (
            <select
              key={filter.key}
              value={values[filter.key] || ''}
              onChange={(e) => updateFilter(filter.key, e.target.value)}
              className="h-9 rounded-md border border-border-default bg-surface-card px-3 text-sm text-text-primary focus-ring"
            >
              <option value="">{filter.label}</option>
              {filter.options.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          );
        }

        if (filter.type === 'toggle') {
          return (
            <div
              key={filter.key}
              className="flex items-center rounded-lg border border-border-default bg-surface-card p-0.5"
            >
              {filter.options.map((opt) => (
                <button
                  key={opt.value ?? '__all__'}
                  onClick={() => updateFilter(filter.key, opt.value)}
                  className={cn(
                    'rounded-md px-3 py-1.5 text-sm font-medium transition-colors',
                    values[filter.key] === opt.value
                      ? 'bg-primary-500 text-white shadow-xs'
                      : 'text-text-secondary hover:text-text-primary'
                  )}
                >
                  {opt.label}
                </button>
              ))}
            </div>
          );
        }

        return null;
      })}

      {onReset && activeCount > 0 && (
        <button
          onClick={onReset}
          className="inline-flex items-center gap-1 rounded-md px-2.5 py-1.5 text-sm text-text-secondary hover:bg-surface-inset hover:text-text-primary"
        >
          <X className="h-3.5 w-3.5" />
          Clear
        </button>
      )}
    </div>
  );

  return (
    <>
      {/* Mobile toggle */}
      <button
        className="flex items-center gap-2 rounded-md border border-border-default bg-surface-card px-3 py-2 text-sm font-medium text-text-secondary md:hidden"
        onClick={() => setMobileOpen(!mobileOpen)}
      >
        <SlidersHorizontal className="h-4 w-4" />
        Filters
        {activeCount > 0 && (
          <span className="flex h-5 w-5 items-center justify-center rounded-full bg-primary-500 text-xs text-white">
            {activeCount}
          </span>
        )}
      </button>

      {/* Desktop: always visible */}
      <div className="hidden md:block">{filterContent}</div>

      {/* Mobile: collapsible */}
      {mobileOpen && <div className="md:hidden">{filterContent}</div>}
    </>
  );
}

FilterBar.displayName = 'FilterBar';
