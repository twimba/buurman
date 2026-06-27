import { forwardRef } from 'react';

interface EurInputProps {
  value: string;
  onChange: (value: string) => void;
  onEnter?: () => void;
  onEscape?: () => void;
  disabled?: boolean;
  invalid?: boolean;
  autoFocus?: boolean;
  ariaLabel?: string;
  placeholder?: string;
  /** Width utility class for the field, e.g. "w-24". */
  className?: string;
  /** Trailing unit shown after the amount (e.g. "/ email" for a per-unit rate). */
  unit?: string;
}

/**
 * A money input framed as EUR: a fixed "€" adornment, right-aligned tabular figures, and a decimal
 * keypad on mobile. Stays a text input (not type=number) so it tolerates the European comma and
 * never silently coerces "1e3"/partial input to a number — validation/parsing is the caller's job
 * via {@link parseEurToMinor}. Enter/Escape are forwarded for inline-editor ergonomics.
 */
export const EurInput = forwardRef<HTMLInputElement, EurInputProps>(
  (
    {
      value,
      onChange,
      onEnter,
      onEscape,
      disabled,
      invalid,
      autoFocus,
      ariaLabel,
      placeholder,
      className = 'w-28',
      unit,
    },
    ref
  ) => (
    <span
      className={`group inline-flex items-center rounded-lg border bg-surface-card pl-2 pr-1.5 transition-colors focus-within:ring-[3px] focus-within:ring-primary-500/15 ${
        invalid
          ? 'border-error-text focus-within:border-error-text'
          : 'border-border-default focus-within:border-primary-400'
      } ${className}`}
    >
      <span
        aria-hidden="true"
        className="select-none pr-1 text-sm font-medium text-text-muted"
      >
        €
      </span>
      <input
        ref={ref}
        type="text"
        inputMode="decimal"
        value={value}
        disabled={disabled}
        autoFocus={autoFocus}
        placeholder={placeholder}
        aria-label={ariaLabel}
        aria-invalid={invalid || undefined}
        onChange={(e) => onChange(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === 'Enter' && onEnter) {
            e.preventDefault();
            onEnter();
          }
          if (e.key === 'Escape' && onEscape) {
            onEscape();
          }
        }}
        className="w-full min-w-0 bg-transparent py-1 text-right text-sm tabular-nums text-text-primary outline-none placeholder:text-text-muted disabled:opacity-50"
      />
      {unit && (
        <span className="whitespace-nowrap pl-1 text-[11px] text-text-muted">
          {unit}
        </span>
      )}
    </span>
  )
);

EurInput.displayName = 'EurInput';
