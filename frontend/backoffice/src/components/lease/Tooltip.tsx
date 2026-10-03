import { useEffect, useId, useState } from 'react';
import type { ReactNode } from 'react';

interface TooltipProps {
  content: ReactNode;
  children: ReactNode;
}

/**
 * Hover/focus tooltip. The wrapper is focusable so keyboard users reach it, the content is
 * hoverable (WCAG 1.4.13) and Escape dismisses it. Never the only home of the information:
 * the legend popover and the form help line carry the same copy.
 */
export const Tooltip = ({ content, children }: TooltipProps) => {
  const [visible, setVisible] = useState(false);
  const id = useId();

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
      className="relative inline-flex rounded focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500/40"
      tabIndex={0}
      aria-describedby={visible ? id : undefined}
      onMouseEnter={() => setVisible(true)}
      onMouseLeave={() => setVisible(false)}
      onFocus={() => setVisible(true)}
      onBlur={() => setVisible(false)}
    >
      {children}
      {visible && (
        <span
          id={id}
          role="tooltip"
          className="absolute left-0 top-full z-30 mt-1 w-64 rounded-md border border-border-default bg-surface-card p-3 text-left text-xs font-normal normal-case tracking-normal text-text-secondary shadow-lg"
        >
          {content}
        </span>
      )}
    </span>
  );
};
