import { useEffect, useId, useRef, useState } from 'react';
import type { ReactNode } from 'react';
import { createPortal } from 'react-dom';
import { useOverlayPosition } from './useOverlayPosition';

interface TooltipProps {
  content: ReactNode;
  children: ReactNode;
}

/**
 * Hover/focus tooltip rendered in a portal (so table overflow wrappers cannot clip it). The
 * wrapper is focusable so keyboard users reach it, the content stays hoverable (WCAG 1.4.13)
 * and Escape dismisses it. Never the only home of the information: the legend popover and the
 * form help line carry the same copy.
 */
export const Tooltip = ({ content, children }: TooltipProps) => {
  const [visible, setVisible] = useState(false);
  const id = useId();
  const triggerRef = useRef<HTMLSpanElement>(null);
  const panelRef = useRef<HTMLSpanElement>(null);
  const hideTimer = useRef<ReturnType<typeof setTimeout>>(undefined);
  useOverlayPosition(visible, triggerRef, panelRef, 'left');

  const show = () => {
    clearTimeout(hideTimer.current);
    setVisible(true);
  };
  // Short grace period lets the pointer travel from the trigger onto the tooltip.
  const hide = () => {
    hideTimer.current = setTimeout(() => setVisible(false), 120);
  };

  useEffect(() => () => clearTimeout(hideTimer.current), []);

  useEffect(() => {
    if (!visible) {
      return;
    }
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setVisible(false);
      }
    };
    document.addEventListener('keydown', onKeyDown);
    return () => document.removeEventListener('keydown', onKeyDown);
  }, [visible]);

  return (
    <span
      ref={triggerRef}
      className="relative inline-flex rounded focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500/40"
      tabIndex={0}
      aria-describedby={visible ? id : undefined}
      onMouseEnter={show}
      onMouseLeave={hide}
      onFocus={show}
      onBlur={hide}
    >
      {children}
      {visible &&
        createPortal(
          <span
            ref={panelRef}
            id={id}
            role="tooltip"
            style={{ position: 'fixed', top: 0, left: 0, visibility: 'hidden' }}
            onMouseEnter={show}
            onMouseLeave={hide}
            className="z-50 block w-64 overflow-y-auto rounded-md border border-border-default bg-surface-card p-3 text-left text-xs font-normal normal-case tracking-normal text-text-secondary shadow-lg"
          >
            {content}
          </span>,
          document.body
        )}
    </span>
  );
};
