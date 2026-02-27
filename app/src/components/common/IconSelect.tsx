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
    'w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded bg-white dark:bg-[#1e2130] text-[#1a1d2e] dark:text-[#eef0f6]';
  const focusRing = open
    ? 'border-[#5c7cfa] ring-1 ring-[#5c7cfa]'
    : 'hover:border-[#9ca0b8] dark:hover:border-[#5c6180]';

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
            <selected.Icon size={15} className="text-[#5c7cfa] flex-shrink-0" />
            <span className="flex-1 truncate">{selected.label}</span>
          </>
        ) : (
          <span className="flex-1 text-[#9ca0b8]">Select…</span>
        )}
        <ChevronDown
          size={14}
          className={`flex-shrink-0 text-[#9ca0b8] transition-transform duration-150 ${open ? 'rotate-180' : ''}`}
        />
      </button>

      {open && (
        <div className="absolute z-50 w-full mt-1 bg-white dark:bg-[#1e2130] border border-[#c9cfd9] dark:border-[#3a3f54] rounded shadow-lg max-h-64 overflow-y-auto">
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
                    ? 'bg-[#eef1ff] dark:bg-[#252a3d] text-[#5c7cfa]'
                    : 'text-[#1a1d2e] dark:text-[#eef0f6] hover:bg-[#f1f3f9] dark:hover:bg-[#14161f]'
                }`}
              >
                <opt.Icon
                  size={15}
                  className={
                    isSelected
                      ? 'text-[#5c7cfa]'
                      : 'text-[#9ca0b8] dark:text-[#5c6180]'
                  }
                />
                <span>{opt.label}</span>
                {isSelected && (
                  <span className="ml-auto text-[#5c7cfa] text-xs font-bold">
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
