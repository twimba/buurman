import { useState, useRef, useEffect, useCallback } from 'react';
import { ChevronDown } from 'lucide-react';
import type { LucideIcon } from 'lucide-react';

export interface IconSelectOption {
  value: string;
  label: string;
  Icon: LucideIcon;
}

interface IconSelectProps {
  value: string;
  options: IconSelectOption[];
  onChange: (value: string) => void;
  disabled?: boolean;
  className?: string;
}

export const IconSelect = ({
  value,
  options,
  onChange,
  disabled = false,
  className = '',
}: IconSelectProps) => {
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const selected = options.find((o) => o.value === value);

  const close = useCallback(() => setOpen(false), []);

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (
        containerRef.current &&
        !containerRef.current.contains(e.target as Node)
      ) {
        close();
      }
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, [close]);

  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        close();
      }
    };
    document.addEventListener('keydown', handler);
    return () => document.removeEventListener('keydown', handler);
  }, [close]);

  const base =
    'w-full border border-border-strong rounded bg-surface-card text-text-primary';
  const focusRing = open
    ? 'border-primary-500 ring-1 ring-primary-500'
    : 'hover:border-neutral-400 dark:hover:border-neutral-500';

  return (
    <div ref={containerRef} className={`relative ${className}`}>
      <button
        type="button"
        disabled={disabled}
        onClick={() => !disabled && setOpen((v) => !v)}
        className={`${base} ${focusRing} px-3 py-2 flex items-center gap-2.5 text-sm text-left disabled:opacity-60 disabled:cursor-not-allowed`}
      >
        {selected ? (
          <>
            <selected.Icon
              size={15}
              className="text-primary-500 flex-shrink-0"
            />
            <span className="flex-1 truncate">{selected.label}</span>
          </>
        ) : (
          <span className="flex-1 text-text-muted">Select…</span>
        )}
        <ChevronDown
          size={14}
          className={`flex-shrink-0 text-text-muted transition-transform duration-150 ${open ? 'rotate-180' : ''}`}
        />
      </button>

      {open && (
        <div className="absolute z-50 w-full mt-1 bg-surface-card border border-border-strong rounded shadow-lg max-h-64 overflow-y-auto">
          {options.map((opt) => {
            const isSelected = opt.value === value;
            return (
              <button
                key={opt.value}
                type="button"
                onMouseDown={(e) => e.preventDefault()}
                onClick={() => {
                  onChange(opt.value);
                  close();
                }}
                className={`w-full text-left px-3 py-2 flex items-center gap-2.5 text-sm transition-colors ${
                  isSelected
                    ? 'bg-primary-50 dark:bg-surface-raised text-primary-500'
                    : 'text-text-primary hover:bg-surface-inset'
                }`}
              >
                <opt.Icon
                  size={15}
                  className={
                    isSelected ? 'text-primary-500' : 'text-text-muted'
                  }
                />
                <span>{opt.label}</span>
                {isSelected && (
                  <span className="ml-auto text-primary-500 text-xs font-bold">
                    ✓
                  </span>
                )}
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
};
