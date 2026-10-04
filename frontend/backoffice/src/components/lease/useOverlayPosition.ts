import { useLayoutEffect } from 'react';
import type { RefObject } from 'react';
import { computePlacement } from '../../lib/overlayPlacement';

/**
 * Positions a portaled, `position: fixed` panel next to its trigger. Writes styles straight to
 * the DOM (no state) and re-runs on scroll (capture, so inner scroll containers count) and resize.
 */
export const useOverlayPosition = (
  active: boolean,
  triggerRef: RefObject<HTMLElement | null>,
  panelRef: RefObject<HTMLElement | null>,
  align: 'left' | 'right'
) => {
  useLayoutEffect(() => {
    if (!active) {
      return;
    }
    const update = () => {
      const trigger = triggerRef.current;
      const panel = panelRef.current;
      if (!trigger || !panel) {
        return;
      }
      panel.style.maxHeight = '';
      const rect = trigger.getBoundingClientRect();
      const p = computePlacement(
        rect,
        { width: panel.offsetWidth, height: panel.offsetHeight },
        { width: window.innerWidth, height: window.innerHeight },
        align
      );
      panel.style.top = `${p.top}px`;
      panel.style.left = `${p.left}px`;
      if (p.maxHeight !== undefined) {
        panel.style.maxHeight = `${p.maxHeight}px`;
      }
      panel.style.visibility = 'visible';
    };
    update();
    window.addEventListener('scroll', update, true);
    window.addEventListener('resize', update);
    return () => {
      window.removeEventListener('scroll', update, true);
      window.removeEventListener('resize', update);
    };
  }, [active, triggerRef, panelRef, align]);
};
