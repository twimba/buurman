import {
  type ReactNode,
  useCallback,
  useEffect,
  useRef,
  useState,
} from 'react';
import { RefreshCw } from 'lucide-react';
import { cn } from '../utils/cn';

const TRIGGER_THRESHOLD_PX = 80;
const MAX_PULL_PX = 140;

export interface PullToRefreshProps {
  /**
   * Refetch handler. The component awaits this promise before resetting the
   * pull state so the spinner stays visible for the entire request.
   */
  onRefresh: () => Promise<unknown> | unknown;
  /** Scrollable content. Must be the only child; renders its own scroll container. */
  children: ReactNode;
  /** Disable PTR (e.g. when a sheet is open or for desktop). */
  disabled?: boolean;
  className?: string;
}

/**
 * Native-feel pull-to-refresh primitive. Pure pointer events — no
 * `@use-gesture` dep so it's safe in the shared ui package.
 *
 * Activation rule: pointer is pressed inside the scroll container AND the
 * container's scrollTop is 0. We track the drag deltaY and apply
 * `translateY` with a square-root easing so the pull feels rubbery, not
 * 1:1. Release past {@link TRIGGER_THRESHOLD_PX} fires onRefresh.
 *
 * The wrapper itself doesn't scroll — its single child does. This avoids
 * the "drag fights native scroll" race that pure-CSS overscroll causes.
 */
export function PullToRefresh({
  onRefresh,
  children,
  disabled,
  className,
}: PullToRefreshProps) {
  const scrollRef = useRef<HTMLDivElement | null>(null);
  const startRef = useRef<{ y: number; t: number } | null>(null);
  const [pull, setPull] = useState(0);
  const [refreshing, setRefreshing] = useState(false);

  const handlePointerDown = useCallback(
    (e: React.PointerEvent<HTMLDivElement>) => {
      if (disabled || refreshing) {
        return;
      }
      const el = scrollRef.current;
      if (!el || el.scrollTop > 0) {
        return;
      }
      // Only track touch / pen, not mouse — PTR is a mobile gesture.
      if (e.pointerType === 'mouse') {
        return;
      }
      startRef.current = { y: e.clientY, t: e.timeStamp };
    },
    [disabled, refreshing]
  );

  const handlePointerMove = useCallback(
    (e: React.PointerEvent<HTMLDivElement>) => {
      const start = startRef.current;
      if (!start) {
        return;
      }
      const el = scrollRef.current;
      // Cancel the gesture if the user has scrolled away from the top mid-drag.
      if (el && el.scrollTop > 0) {
        startRef.current = null;
        setPull(0);
        return;
      }
      const raw = Math.max(0, e.clientY - start.y);
      // Square-root easing so the pull feels rubbery and self-limits near MAX.
      const eased = Math.min(
        MAX_PULL_PX,
        Math.round(Math.sqrt(raw * 6) * 6.5)
      );
      setPull(eased);
    },
    []
  );

  const handlePointerEnd = useCallback(async () => {
    const start = startRef.current;
    startRef.current = null;
    if (!start) {
      return;
    }
    if (pull < TRIGGER_THRESHOLD_PX) {
      setPull(0);
      return;
    }
    setRefreshing(true);
    setPull(TRIGGER_THRESHOLD_PX);
    try {
      await onRefresh();
    } finally {
      setRefreshing(false);
      setPull(0);
    }
  }, [pull, onRefresh]);

  // Belt-and-braces: also reset pull on Escape / blur.
  useEffect(() => {
    const onBlur = () => {
      startRef.current = null;
      if (!refreshing) {
        setPull(0);
      }
    };
    window.addEventListener('blur', onBlur);
    return () => window.removeEventListener('blur', onBlur);
  }, [refreshing]);

  const progress = Math.min(1, pull / TRIGGER_THRESHOLD_PX);
  const armed = progress >= 1;

  return (
    <div
      onPointerDown={handlePointerDown}
      onPointerMove={handlePointerMove}
      onPointerUp={handlePointerEnd}
      onPointerCancel={handlePointerEnd}
      className={cn('relative h-full overflow-hidden', className)}
    >
      {/* Indicator strip — sits above the content, scrolls down into view
          on pull. Z-indexed below modals/sheets. */}
      <div
        aria-hidden={!pull && !refreshing}
        className="absolute inset-x-0 top-0 z-0 flex items-end justify-center pb-2"
        style={{
          height: `${pull}px`,
          transition: refreshing || pull === 0 ? 'height 180ms ease-out' : 'none',
        }}
      >
        <RefreshCw
          className={cn(
            'h-5 w-5 text-text-secondary transition-transform',
            refreshing && 'animate-spin',
            armed && 'text-primary-500'
          )}
          style={{ transform: `rotate(${progress * 360}deg)` }}
        />
      </div>
      {/* Scroll container — translates down with the pull. */}
      <div
        ref={scrollRef}
        className="h-full overflow-y-auto overscroll-contain"
        style={{
          transform: `translateY(${pull}px)`,
          transition: refreshing || pull === 0 ? 'transform 180ms ease-out' : 'none',
        }}
      >
        {children}
      </div>
    </div>
  );
}

PullToRefresh.displayName = 'PullToRefresh';
