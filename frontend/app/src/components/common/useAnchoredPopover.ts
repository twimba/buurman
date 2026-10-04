import { useEffect, useRef, useState } from 'react';

export type PopoverPosition =
  { top: number; right: number } | { top: number; left: number };

/**
 * Open state + viewport position for a popover portalled to `document.body` and anchored below a
 * trigger: aligned to the trigger's right edge (default) or left edge. Escape closes it.
 */
export const useAnchoredPopover = (align: 'left' | 'right' = 'right') => {
  const [open, setOpen] = useState(false);
  const triggerRef = useRef<HTMLDivElement>(null);
  const [pos, setPos] = useState<PopoverPosition | null>(null);

  useEffect(() => {
    if (!open || !triggerRef.current) {
      return;
    }
    const rect = triggerRef.current.getBoundingClientRect();
    const top = rect.bottom + window.scrollY + 8;
    setPos(
      align === 'right'
        ? { top, right: window.innerWidth - rect.right }
        : { top, left: rect.left }
    );
  }, [open, align]);

  useEffect(() => {
    if (!open) {
      return;
    }
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setOpen(false);
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [open]);

  return { open, setOpen, triggerRef, pos };
};
