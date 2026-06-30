import { type ComponentType } from 'react';
import { Check } from 'lucide-react';
import { FilterPopover } from './FilterPopover';

export interface FilterOption<T extends string = string> {
  value: T;
  label: string;
}

export interface FilterSelectPopoverProps<T extends string = string> {
  /** Trigger label — keep it generic; the active badge + chips carry selection. */
  label: string;
  /** Optional leading icon for the trigger. */
  icon?: ComponentType<{ className?: string }>;
  /** Selectable options (do NOT include an "All"/reset entry — use `allLabel`). */
  options: FilterOption<T>[];
  /** Single-select current value (`undefined` = none selected). */
  value?: T;
  /** Single-select change handler. Receives `undefined` when toggled off or "All" picked. */
  onChange?: (value: T | undefined) => void;
  /** Multi-select current values. When provided, the component is multi-select. */
  values?: T[];
  /** Multi-select toggle handler. */
  onToggle?: (value: T) => void;
  /** Optional reset row shown at the top in single-select mode (e.g. "All statuses"). */
  allLabel?: string;
  align?: 'start' | 'end';
  panelClassName?: string;
}

const ROW_BASE =
  'flex items-center justify-between gap-3 px-2.5 py-2 rounded-md text-sm text-left transition-colors focus-ring';
const ROW_SELECTED =
  'bg-primary-50 text-primary-700 dark:bg-primary-950 dark:text-primary-300';
const ROW_IDLE = 'text-text-secondary hover:bg-surface-inset';

/**
 * Compact filter dropdown for a single- or multi-select option list — the
 * standard list-page filter control. Wraps {@link FilterPopover} and renders
 * each option as a checkmark row. Single-select closes on pick; multi-select
 * stays open.
 */
export function FilterSelectPopover<T extends string = string>({
  label,
  icon,
  options,
  value,
  onChange,
  values,
  onToggle,
  allLabel,
  align,
  panelClassName,
}: FilterSelectPopoverProps<T>) {
  const multiple = values !== undefined;
  const activeCount = multiple ? values.length : value ? 1 : 0;

  return (
    <FilterPopover
      label={label}
      icon={icon}
      activeCount={activeCount}
      align={align}
      panelClassName={panelClassName}
    >
      {(close) => (
        <div
          role={multiple ? 'group' : 'listbox'}
          aria-label={label}
          className="flex flex-col gap-0.5"
        >
          {!multiple && allLabel !== undefined && (
            <button
              role="option"
              aria-selected={!value}
              onClick={() => {
                onChange?.(undefined);
                close();
              }}
              className={`${ROW_BASE} ${!value ? ROW_SELECTED : ROW_IDLE}`}
            >
              {allLabel}
              {!value && (
                <Check className="h-4 w-4 shrink-0 text-primary-500" />
              )}
            </button>
          )}
          {options.map((opt) => {
            const selected = multiple
              ? values.includes(opt.value)
              : value === opt.value;
            return (
              <button
                key={opt.value}
                role={multiple ? 'checkbox' : 'option'}
                aria-checked={multiple ? selected : undefined}
                aria-selected={multiple ? undefined : selected}
                onClick={() => {
                  if (multiple) {
                    onToggle?.(opt.value);
                  } else {
                    onChange?.(selected ? undefined : opt.value);
                    close();
                  }
                }}
                className={`${ROW_BASE} ${selected ? ROW_SELECTED : ROW_IDLE}`}
              >
                {opt.label}
                {selected && (
                  <Check className="h-4 w-4 shrink-0 text-primary-500" />
                )}
              </button>
            );
          })}
        </div>
      )}
    </FilterPopover>
  );
}

FilterSelectPopover.displayName = 'FilterSelectPopover';
