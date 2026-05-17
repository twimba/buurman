import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

const EDGE_PX = 24;
const TRIGGER_PX = 80;
const VELOCITY_THRESHOLD = 0.5; // px/ms

/**
 * Left-edge swipe-back gesture. Listens for pointer-down within the
 * left 24 px of the viewport, then on pointer-up fires `navigate(-1)` if
 * the user swiped right past 80 px OR with velocity > 0.5 px/ms.
 *
 * Matches iOS native behavior. Skipped on desktop (mouse pointer type)
 * and when the keyboard is open (avoids accidental triggers during form
 * editing).
 *
 * Mount once at app root.
 */
export function useSwipeBack(): void {
  const navigate = useNavigate();

  useEffect(() => {
    let start: { x: number; y: number; t: number } | null = null;

    const onPointerDown = (e: PointerEvent) => {
      if (e.pointerType === 'mouse') {
        return;
      }
      if (e.clientX > EDGE_PX) {
        return;
      }
      if (document.documentElement.hasAttribute('data-keyboard-open')) {
        return;
      }
      start = { x: e.clientX, y: e.clientY, t: e.timeStamp };
    };

    const onPointerUp = (e: PointerEvent) => {
      const s = start;
      start = null;
      if (!s) {
        return;
      }
      const dx = e.clientX - s.x;
      const dy = Math.abs(e.clientY - s.y);
      // Reject mostly-vertical gestures (scroll) and short pulls.
      if (dy > dx * 0.6) {
        return;
      }
      const dt = Math.max(1, e.timeStamp - s.t);
      const velocity = dx / dt;
      if (dx > TRIGGER_PX || velocity > VELOCITY_THRESHOLD) {
        navigate(-1);
      }
    };

    const onPointerCancel = () => {
      start = null;
    };

    window.addEventListener('pointerdown', onPointerDown, { passive: true });
    window.addEventListener('pointerup', onPointerUp, { passive: true });
    window.addEventListener('pointercancel', onPointerCancel, {
      passive: true,
    });
    return () => {
      window.removeEventListener('pointerdown', onPointerDown);
      window.removeEventListener('pointerup', onPointerUp);
      window.removeEventListener('pointercancel', onPointerCancel);
    };
  }, [navigate]);
}
