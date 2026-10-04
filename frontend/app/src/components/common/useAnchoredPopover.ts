import {
  useCallback,
  useEffect,
  useLayoutEffect,
  useRef,
  useState,
  type CSSProperties,
} from 'react';
import { computePopoverPlacement } from './popoverPlacement';

const HIDDEN: CSSProperties = { top: 0, left: 0, visibility: 'hidden' };

/**
 * Open state + viewport placement for a popover portalled to `document.body` (position: fixed)
 * and anchored to a trigger. Opens below by default, flips above when there is no room, clamps its
 * height to the space on the chosen side and stays inside the viewport horizontally. Repositions
 * on resize and on scroll of the page or any ancestor scroller while open. Escape closes it.
 *
 * Attach `popoverRef` to the popover element and apply `popoverStyle` to it; the popover element
 * must be a flex column so its content can shrink to the clamped `maxHeight`.
 */
export const useAnchoredPopover = (align: 'left' | 'right' = 'right') => {
  const [open, setOpen] = useState(false);
  const triggerRef = useRef<HTMLDivElement>(null);
  const popoverRef = useRef<HTMLDivElement>(null);
  const [popoverStyle, setPopoverStyle] = useState<CSSProperties>(HIDDEN);

  const reposition = useCallback(() => {
    const trigger = triggerRef.current;
    const popover = popoverRef.current;
    if (!trigger || !popover) {
      return;
    }
    // Measure the natural height, not the one clamped by the previous placement.
    popover.style.maxHeight = '';
    const { width, height } = popover.getBoundingClientRect();
    const { top, left, maxHeight } = computePopoverPlacement({
      trigger: trigger.getBoundingClientRect(),
      popover: { width, height },
      viewport: {
        width: document.documentElement.clientWidth,
        height: document.documentElement.clientHeight,
      },
      align,
    });
    setPopoverStyle({ top, left, maxHeight });
  }, [align]);

  useLayoutEffect(() => {
    if (!open) {
      return;
    }
    reposition();
    window.addEventListener('resize', reposition);
    // Capture phase: scroll events do not bubble, this also catches ancestor scroll containers.
    window.addEventListener('scroll', reposition, true);
    return () => {
      window.removeEventListener('resize', reposition);
      window.removeEventListener('scroll', reposition, true);
      setPopoverStyle(HIDDEN);
    };
  }, [open, reposition]);

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

  return { open, setOpen, triggerRef, popoverRef, popoverStyle };
};
