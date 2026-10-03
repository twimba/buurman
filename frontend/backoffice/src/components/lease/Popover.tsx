import { useEffect, useId, useRef, useState } from 'react';
import type { ReactNode } from 'react';

interface PopoverProps {
  /** Accessible name for both the trigger button and the dialog. */
  label: string;
  trigger: ReactNode;
  children: ReactNode | ((close: () => void) => ReactNode);
  align?: 'left' | 'right';
  triggerClassName?: string;
  widthClassName?: string;
}

/**
 * Minimal click-to-open popover: the trigger is a button with aria-haspopup/aria-expanded, the
 * panel is a non-modal role="dialog" that takes focus on open, closes on Escape or outside
 * click, and returns focus to the trigger.
 */
export const Popover = ({
  label,
  trigger,
  children,
  align = 'left',
  triggerClassName = '',
  widthClassName = 'w-[22.5rem]',
}: PopoverProps) => {
  const [open, setOpen] = useState(false);
  const rootRef = useRef<HTMLSpanElement>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const panelRef = useRef<HTMLDivElement>(null);
  const panelId = useId();

  const close = () => setOpen(false);

  useEffect(() => {
    if (!open) {
      return;
    }
    panelRef.current?.focus();
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setOpen(false);
      }
    };
    const onPointerDown = (e: MouseEvent) => {
      if (!rootRef.current?.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    const panel = panelRef.current;
    const trigger = triggerRef.current;
    document.addEventListener('keydown', onKeyDown);
    document.addEventListener('mousedown', onPointerDown);
    return () => {
      // Return focus to the trigger unless the user already moved it elsewhere.
      const active = document.activeElement;
      if (!active || active === document.body || panel?.contains(active)) {
        trigger?.focus();
      }
      document.removeEventListener('keydown', onKeyDown);
      document.removeEventListener('mousedown', onPointerDown);
    };
  }, [open]);

  return (
    <span ref={rootRef} className="relative inline-flex">
      <button
        ref={triggerRef}
        type="button"
        aria-label={label}
        aria-haspopup="dialog"
        aria-expanded={open}
        aria-controls={open ? panelId : undefined}
        onClick={() => setOpen((o) => !o)}
        className={`rounded-md p-1 text-text-muted hover:text-text-primary hover:bg-surface-inset focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500/40 ${triggerClassName}`}
      >
        {trigger}
      </button>
      {open && (
        <div
          ref={panelRef}
          id={panelId}
          role="dialog"
          aria-label={label}
          tabIndex={-1}
          className={`absolute top-full z-30 mt-1 max-h-[70vh] max-w-[90vw] overflow-y-auto rounded-lg border border-border-default bg-surface-card p-4 text-left normal-case tracking-normal shadow-xl focus:outline-none ${widthClassName} ${
            align === 'right' ? 'right-0' : 'left-0'
          }`}
        >
          {typeof children === 'function' ? children(close) : children}
        </div>
      )}
    </span>
  );
};
