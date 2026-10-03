import { useEffect, useId, useRef, useState } from 'react';
import type { ReactNode } from 'react';
import { createPortal } from 'react-dom';
import { useOverlayPosition } from './useOverlayPosition';

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
  useOverlayPosition(open, triggerRef, panelRef, align);

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
      const target = e.target as Node;
      if (
        !rootRef.current?.contains(target) &&
        !panelRef.current?.contains(target)
      ) {
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

  // The panel is portaled to <body>, so Tab would leave the DOM order: close at its edges.
  const onPanelKeyDown = (e: React.KeyboardEvent<HTMLDivElement>) => {
    if (e.key !== 'Tab') {
      return;
    }
    const items = panelRef.current?.querySelectorAll<HTMLElement>(
      'button, a[href], input, select'
    );
    const first = items?.[0];
    const last = items?.[items.length - 1];
    const active = document.activeElement;
    if (
      !items?.length ||
      (e.shiftKey && (active === first || active === panelRef.current)) ||
      (!e.shiftKey && active === last)
    ) {
      e.preventDefault();
      setOpen(false);
    }
  };

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
      {open &&
        createPortal(
          <div
            ref={panelRef}
            id={panelId}
            role="dialog"
            aria-label={label}
            tabIndex={-1}
            style={{ position: 'fixed', top: 0, left: 0, visibility: 'hidden' }}
            onKeyDown={onPanelKeyDown}
            className={`z-50 max-h-[70vh] max-w-[90vw] overflow-y-auto rounded-lg border border-border-default bg-surface-card p-4 text-left normal-case tracking-normal shadow-xl focus:outline-none ${widthClassName}`}
          >
            {typeof children === 'function' ? children(close) : children}
          </div>,
          document.body
        )}
    </span>
  );
};
