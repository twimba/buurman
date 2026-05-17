import {
  type PointerEvent as ReactPointerEvent,
  type ReactNode,
  useEffect,
  useRef,
  useState,
} from 'react';
import { cn } from '../utils/cn';

export interface SwipeActionItem {
  /** Visible label. Use a short verb ("Mark Paid", "Archive", "Delete"). */
  label: string;
  /** Optional Lucide icon. */
  icon?: React.ComponentType<{ className?: string }>;
  /** Action handler. The container snaps closed before invocation. */
  onAction: () => void | Promise<void>;
  /** Visual tone. */
  tone?: 'success' | 'warning' | 'danger' | 'neutral';
}

export interface SwipeActionProps {
  /** Actions revealed when the user swipes left (right-edge actions). */
  leftActions?: SwipeActionItem[];
  /** Actions revealed when the user swipes right (left-edge actions). */
  rightActions?: SwipeActionItem[];
  /** Row content. The whole row is the click target. */
  children: ReactNode;
  /** Forwarded onClick — fires when the user taps the closed row. */
  onClick?: () => void;
  className?: string;
  /** Force-disable swipe (e.g. inside a form, or on desktop). Default: enabled on touch only. */
  enabled?: boolean;
}

const TONE_CLASS: Record<NonNullable<SwipeActionItem['tone']>, string> = {
  success: 'bg-success-text text-white',
  warning: 'bg-warning-text text-white',
  danger: 'bg-error-text text-white',
  neutral: 'bg-surface-inset text-text-primary',
};

const ACTION_WIDTH = 88; // px per action button — comfortable thumb target
const OPEN_THRESHOLD = 0.4; // 40% of the action group width opens fully
const VELOCITY_THRESHOLD = 0.5; // px/ms — a flick past this opens regardless

/**
 * iOS-style swipe-actionable list row. Pointer-events-based so it works for
 * touch, mouse, and stylus uniformly. Snaps open or closed with a small
 * threshold + velocity check. Always exposes the same actions via explicit
 * buttons in the parent — swipe is bonus, not the only path.
 */
export function SwipeAction({
  leftActions = [],
  rightActions = [],
  children,
  onClick,
  className,
  enabled = true,
}: SwipeActionProps) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const startX = useRef(0);
  const startY = useRef(0);
  const startTime = useRef(0);
  const startOffset = useRef(0);
  const lastX = useRef(0);
  const lastTime = useRef(0);
  const dragging = useRef(false);
  const captured = useRef(false);

  // Only enable swipe on touch pointers — mouse users tap, swipe is a touch idiom.
  const [isTouch, setIsTouch] = useState(() =>
    typeof window === 'undefined'
      ? false
      : window.matchMedia('(pointer: coarse)').matches
  );
  useEffect(() => {
    const mq = window.matchMedia('(pointer: coarse)');
    const handler = (e: MediaQueryListEvent) => setIsTouch(e.matches);
    mq.addEventListener('change', handler);
    return () => mq.removeEventListener('change', handler);
  }, []);

  const [offset, setOffset] = useState(0);
  const swipeEnabled = enabled && isTouch;

  const leftWidth = leftActions.length * ACTION_WIDTH;
  const rightWidth = rightActions.length * ACTION_WIDTH;
  // Negative offset = revealed left (right-edge) actions; positive = right (left-edge) actions.
  const minOffset = -leftWidth;
  const maxOffset = rightWidth;

  const closeAndAction = (action: SwipeActionItem) => {
    setOffset(0);
    queueMicrotask(() => action.onAction());
  };

  const onPointerDown = (e: ReactPointerEvent<HTMLDivElement>) => {
    if (!swipeEnabled || e.pointerType === 'mouse') {
      return;
    }
    startX.current = e.clientX;
    startY.current = e.clientY;
    startTime.current = performance.now();
    startOffset.current = offset;
    lastX.current = e.clientX;
    lastTime.current = startTime.current;
    dragging.current = false;
    captured.current = false;
  };

  const onPointerMove = (e: ReactPointerEvent<HTMLDivElement>) => {
    if (!swipeEnabled || startTime.current === 0) {
      return;
    }
    const dx = e.clientX - startX.current;
    const dy = e.clientY - startY.current;
    // Engage horizontal drag only when we're clearly horizontal, to avoid
    // hijacking vertical page scroll.
    if (!dragging.current) {
      if (Math.abs(dx) < 8 || Math.abs(dy) > Math.abs(dx)) {
        return;
      }
      dragging.current = true;
      if (containerRef.current?.setPointerCapture) {
        try {
          containerRef.current.setPointerCapture(e.pointerId);
          captured.current = true;
        } catch {
          /* ignore */
        }
      }
    }
    const next = Math.min(
      maxOffset,
      Math.max(minOffset, startOffset.current + dx)
    );
    setOffset(next);
    lastX.current = e.clientX;
    lastTime.current = performance.now();
  };

  const onPointerUp = (e: ReactPointerEvent<HTMLDivElement>) => {
    if (!swipeEnabled) {
      return;
    }
    const wasDrag = dragging.current;
    if (captured.current && containerRef.current?.releasePointerCapture) {
      try {
        containerRef.current.releasePointerCapture(e.pointerId);
      } catch {
        /* ignore */
      }
    }
    dragging.current = false;
    captured.current = false;
    if (!wasDrag) {
      startTime.current = 0;
      return;
    }
    // Snap decision: full-open if past threshold OR if released with enough velocity.
    const dt = Math.max(1, performance.now() - lastTime.current);
    const velocity = (lastX.current - startX.current) / dt;
    const past = offset / (offset < 0 ? minOffset : maxOffset || 1);
    if (offset < 0) {
      if (past > OPEN_THRESHOLD || velocity < -VELOCITY_THRESHOLD) {
        setOffset(minOffset);
      } else {
        setOffset(0);
      }
    } else if (offset > 0) {
      if (past > OPEN_THRESHOLD || velocity > VELOCITY_THRESHOLD) {
        setOffset(maxOffset);
      } else {
        setOffset(0);
      }
    } else {
      setOffset(0);
    }
    startTime.current = 0;
  };

  const onPointerCancel = () => {
    setOffset(0);
    dragging.current = false;
    captured.current = false;
    startTime.current = 0;
  };

  const handleRowClick = () => {
    if (offset !== 0) {
      // Tap on the row while a swipe is open just closes it.
      setOffset(0);
      return;
    }
    onClick?.();
  };

  return (
    <div
      ref={containerRef}
      className={cn(
        'relative overflow-hidden rounded-lg touch-pan-y',
        className
      )}
      onPointerDown={onPointerDown}
      onPointerMove={onPointerMove}
      onPointerUp={onPointerUp}
      onPointerCancel={onPointerCancel}
    >
      {/* Right-edge actions (revealed on swipe left) */}
      {leftActions.length > 0 && (
        <div
          className="absolute inset-y-0 right-0 flex"
          style={{ width: leftWidth }}
          aria-hidden={offset >= 0}
        >
          {leftActions.map((a, i) => {
            const Icon = a.icon;
            return (
              <button
                key={i}
                type="button"
                tabIndex={offset < 0 ? 0 : -1}
                onClick={(e) => {
                  e.stopPropagation();
                  closeAndAction(a);
                }}
                className={cn(
                  'flex flex-col items-center justify-center gap-1 text-xs font-medium focus-ring',
                  TONE_CLASS[a.tone ?? 'neutral']
                )}
                style={{ width: ACTION_WIDTH }}
              >
                {Icon && <Icon className="h-5 w-5" />}
                {a.label}
              </button>
            );
          })}
        </div>
      )}

      {/* Left-edge actions (revealed on swipe right) */}
      {rightActions.length > 0 && (
        <div
          className="absolute inset-y-0 left-0 flex"
          style={{ width: rightWidth }}
          aria-hidden={offset <= 0}
        >
          {rightActions.map((a, i) => {
            const Icon = a.icon;
            return (
              <button
                key={i}
                type="button"
                tabIndex={offset > 0 ? 0 : -1}
                onClick={(e) => {
                  e.stopPropagation();
                  closeAndAction(a);
                }}
                className={cn(
                  'flex flex-col items-center justify-center gap-1 text-xs font-medium focus-ring',
                  TONE_CLASS[a.tone ?? 'neutral']
                )}
                style={{ width: ACTION_WIDTH }}
              >
                {Icon && <Icon className="h-5 w-5" />}
                {a.label}
              </button>
            );
          })}
        </div>
      )}

      {/* Foreground row — translates horizontally while swiping */}
      <div
        onClick={handleRowClick}
        className="relative bg-surface-card transition-transform"
        style={{
          transform: `translateX(${offset}px)`,
          transitionDuration: dragging.current ? '0ms' : '160ms',
        }}
      >
        {children}
      </div>
    </div>
  );
}

SwipeAction.displayName = 'SwipeAction';
