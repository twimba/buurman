import { useCallback, useRef } from 'react';

interface UseLongPressOptions {
  /** ms to hold before firing. Default 500 (iOS native long-press feel). */
  delayMs?: number;
  /** Movement tolerance in px — gesture cancels if pointer moves more. */
  moveTolerancePx?: number;
  /** Run on mouse devices too (default: touch/pen only). */
  enableMouse?: boolean;
}

interface LongPressHandlers<T extends Element> {
  onPointerDown: (e: React.PointerEvent<T>) => void;
  onPointerMove: (e: React.PointerEvent<T>) => void;
  onPointerUp: (e: React.PointerEvent<T>) => void;
  onPointerCancel: (e: React.PointerEvent<T>) => void;
  onPointerLeave: (e: React.PointerEvent<T>) => void;
}

/**
 * Long-press detector. Returns a handler bag to spread onto any element:
 *
 *     <div {...useLongPress(onLongPress)}>...</div>
 *
 * Default behavior: touch/pen only, 500 ms hold, cancels if the pointer
 * moves more than 8 px (so scroll gestures don't trigger). The callback
 * fires once per hold; the timer is cleared on pointerup / pointercancel /
 * pointerleave so a regular tap doesn't fire it.
 *
 * Pair with useSelectionMode + SelectionBar for "long-press a card →
 * enter selection mode" patterns.
 */
export function useLongPress<T extends Element = Element>(
  onLongPress: (e: React.PointerEvent<T>) => void,
  options: UseLongPressOptions = {}
): LongPressHandlers<T> {
  const {
    delayMs = 500,
    moveTolerancePx = 8,
    enableMouse = false,
  } = options;
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const startRef = useRef<{ x: number; y: number } | null>(null);

  const clear = useCallback(() => {
    if (timerRef.current != null) {
      clearTimeout(timerRef.current);
      timerRef.current = null;
    }
    startRef.current = null;
  }, []);

  const onPointerDown = useCallback(
    (e: React.PointerEvent<T>) => {
      if (!enableMouse && e.pointerType === 'mouse') {
        return;
      }
      startRef.current = { x: e.clientX, y: e.clientY };
      // Capture event for setTimeout closure (e is pooled but currentTarget
      // is stable enough for our usage and we copy what we need).
      const capturedEvent = e;
      timerRef.current = setTimeout(() => {
        timerRef.current = null;
        onLongPress(capturedEvent);
      }, delayMs);
    },
    [onLongPress, delayMs, enableMouse]
  );

  const onPointerMove = useCallback(
    (e: React.PointerEvent<T>) => {
      const start = startRef.current;
      if (!start) {
        return;
      }
      const dx = Math.abs(e.clientX - start.x);
      const dy = Math.abs(e.clientY - start.y);
      if (dx > moveTolerancePx || dy > moveTolerancePx) {
        clear();
      }
    },
    [clear, moveTolerancePx]
  );

  return {
    onPointerDown,
    onPointerMove,
    onPointerUp: clear,
    onPointerCancel: clear,
    onPointerLeave: clear,
  };
}
