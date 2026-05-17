import * as Dialog from '@radix-ui/react-dialog';
import { X } from 'lucide-react';
import {
  type ReactNode,
  useCallback,
  useEffect,
  useRef,
  useState,
} from 'react';
import { cn } from '../utils/cn';

/**
 * Snap point as a fraction of the dynamic viewport height. e.g. `0.4` =
 * "open the sheet at 40dvh". Values must be ascending; the smallest is
 * the "peek" and the largest is "full". Drag below the smallest past
 * the dismiss threshold closes the sheet.
 */
export type SheetSnapPoint = number;

export interface SheetProps {
  open: boolean;
  onClose: () => void;
  title: string;
  description?: string;
  children: ReactNode;
  footer?: ReactNode;
  /** Render as a bottom sheet always (don't auto-route to dialog on `md+`). */
  forceSheet?: boolean;
  /** Prevent close via overlay click / Escape (still allow programmatic close). */
  preventClose?: boolean;
  /**
   * Sorted ascending dvh fractions, e.g. `[0.4, 0.92]` for peek/full.
   * When provided, the sheet opens at the first snap point and the drag
   * handle snaps between points. When omitted, the sheet behaves as a
   * single full-height sheet (today's default at max-h:92dvh).
   */
  snapPoints?: SheetSnapPoint[];
  /**
   * Initial snap index when `snapPoints` is provided. Defaults to 0 (peek).
   */
  initialSnap?: number;
  className?: string;
}

const DISMISS_THRESHOLD_RATIO = 0.25;
const VELOCITY_DISMISS = 0.6;

/**
 * Responsive modal:
 * - phone (`<md`): full-width bottom sheet with drag-handle and safe-area padding
 * - tablet/desktop (`md+`): centered dialog (matches existing `ModalWrapper` visuals)
 *
 * Built on Radix Dialog so focus trap, ESC, scroll lock, ARIA are handled.
 * No Vaul dependency.
 *
 * Pass `snapPoints={[0.4, 0.92]}` for peek/full snap behavior — useful for
 * long forms where the user can scan summary at peek then drag up to fill.
 */
export function Sheet({
  open,
  onClose,
  title,
  description,
  children,
  footer,
  forceSheet,
  preventClose,
  snapPoints,
  initialSnap = 0,
  className,
}: SheetProps) {
  const [isDesktop, setIsDesktop] = useState(() =>
    typeof window === 'undefined'
      ? true
      : window.matchMedia('(min-width: 768px)').matches
  );
  const [snapIndex, setSnapIndex] = useState(initialSnap);

  useEffect(() => {
    if (forceSheet) {
      return;
    }
    const mq = window.matchMedia('(min-width: 768px)');
    const handler = (e: MediaQueryListEvent) => setIsDesktop(e.matches);
    mq.addEventListener('change', handler);
    return () => mq.removeEventListener('change', handler);
  }, [forceSheet]);

  // Note: snapIndex persists across open/close cycles intentionally — a
  // user dragging to "full" once usually wants the next open to land there
  // too. Callers needing a hard reset can pass a `key` to remount the
  // Sheet.

  const handleOpenChange = useCallback(
    (nextOpen: boolean) => {
      if (!nextOpen && !preventClose) {
        onClose();
      }
    },
    [onClose, preventClose]
  );

  const renderAsSheet = forceSheet || !isDesktop;
  const useSnap = renderAsSheet && snapPoints && snapPoints.length > 0;
  const snapHeightDvh = useSnap ? snapPoints[snapIndex] * 100 : null;

  return (
    <Dialog.Root open={open} onOpenChange={handleOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay
          className={cn(
            'fixed inset-0 z-50 bg-surface-overlay',
            'data-[state=open]:animate-in data-[state=open]:fade-in-0',
            'data-[state=closed]:animate-out data-[state=closed]:fade-out-0'
          )}
        />
        <Dialog.Content
          className={cn(
            'fixed z-50 bg-surface-card border border-border-default flex flex-col outline-none',
            renderAsSheet
              ? [
                  // Bottom sheet
                  'inset-x-0 bottom-0',
                  // When snap points drive height, omit max-h so the dvh
                  // height we set inline takes effect.
                  useSnap ? '' : 'max-h-[92dvh]',
                  'rounded-t-2xl',
                  'data-[state=open]:animate-slide-up',
                  'data-[state=closed]:animate-slide-down',
                  'transition-[height] duration-200 ease-out',
                ]
              : [
                  // Centered dialog
                  'top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2',
                  'w-full max-w-lg rounded-xl shadow-lg',
                  'data-[state=open]:animate-in data-[state=open]:fade-in-0 data-[state=open]:zoom-in-95',
                  'data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=closed]:zoom-out-95',
                  'max-h-[85vh]',
                ],
            className
          )}
          style={
            renderAsSheet
              ? {
                  paddingBottom:
                    'calc(var(--safe-bottom, 0px) + var(--kbd-inset, 0px))',
                  paddingLeft: 'var(--safe-left, 0px)',
                  paddingRight: 'var(--safe-right, 0px)',
                  ...(snapHeightDvh != null
                    ? { height: `${snapHeightDvh}dvh` }
                    : undefined),
                }
              : undefined
          }
        >
          {renderAsSheet && (
            <DragHandle
              snapPoints={useSnap ? snapPoints : undefined}
              snapIndex={snapIndex}
              onSnapTo={(next) => setSnapIndex(next)}
              onDismiss={() => {
                if (!preventClose) {
                  onClose();
                }
              }}
            />
          )}

          {/* Header */}
          <div className="flex items-start justify-between border-b border-border-default px-6 py-3">
            <div>
              <Dialog.Title className="text-lg font-semibold text-text-primary">
                {title}
              </Dialog.Title>
              {description && (
                <Dialog.Description className="mt-1 text-sm text-text-secondary">
                  {description}
                </Dialog.Description>
              )}
            </div>
            {!preventClose && (
              <Dialog.Close
                aria-label="Close"
                className="rounded-md p-2 -mr-2 min-h-touch min-w-touch text-text-muted hover:bg-surface-inset hover:text-text-primary focus-ring"
              >
                <X className="h-5 w-5" />
              </Dialog.Close>
            )}
          </div>

          {/* Body */}
          <SheetBody>{children}</SheetBody>

          {/* Footer */}
          {footer && (
            <div className="flex items-center justify-end gap-2 border-t border-border-default px-6 py-3">
              {footer}
            </div>
          )}
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}

Sheet.displayName = 'Sheet';

interface DragHandleProps {
  snapPoints?: SheetSnapPoint[];
  snapIndex: number;
  onSnapTo: (index: number) => void;
  onDismiss: () => void;
}

/**
 * Drag handle for the bottom sheet. Two modes:
 *
 * 1. **No snap points (default):** drag down translates the sheet; release
 *    past 25 % of sheet height OR velocity > 0.6 px/ms dismisses, else
 *    snaps back to position 0.
 *
 * 2. **Snap points provided:** the sheet's height is driven by the current
 *    snap index. Drag computes the would-be new height (current height
 *    minus drag-down distance); on release, snaps to the nearest snap
 *    point. Dragging below the smallest snap point past the dismiss
 *    threshold dismisses.
 *
 * Built on native pointer events (no @use-gesture dep).
 */
function DragHandle({
  snapPoints,
  snapIndex,
  onSnapTo,
  onDismiss,
}: DragHandleProps) {
  const startRef = useRef<{
    y: number;
    t: number;
    sheet: HTMLElement;
    startHeight: number;
  } | null>(null);

  const handlePointerDown = (e: React.PointerEvent<HTMLDivElement>) => {
    const sheet = e.currentTarget.closest(
      '[role="dialog"]'
    ) as HTMLElement | null;
    if (!sheet) {
      return;
    }
    (e.target as Element).setPointerCapture?.(e.pointerId);
    const startHeight = sheet.getBoundingClientRect().height;
    startRef.current = {
      y: e.clientY,
      t: e.timeStamp,
      sheet,
      startHeight,
    };
    sheet.style.transition = 'none';
  };

  const handlePointerMove = (e: React.PointerEvent<HTMLDivElement>) => {
    const start = startRef.current;
    if (!start) {
      return;
    }
    const dy = e.clientY - start.y;
    if (snapPoints) {
      // Snap mode: shrink height as user drags down, grow as they drag up.
      const target = Math.max(0, start.startHeight - dy);
      start.sheet.style.height = `${target}px`;
    } else {
      // Translate mode: only allow downward translation.
      const downOnly = Math.max(0, dy);
      start.sheet.style.transform = `translateY(${downOnly}px)`;
    }
  };

  const handlePointerEnd = (e: React.PointerEvent<HTMLDivElement>) => {
    const start = startRef.current;
    if (!start) {
      return;
    }
    startRef.current = null;
    const dy = e.clientY - start.y;
    const dt = Math.max(1, e.timeStamp - start.t);
    const velocity = dy / dt;

    if (snapPoints) {
      // Snap-mode release.
      const viewportH =
        window.innerHeight ||
        document.documentElement.clientHeight ||
        start.startHeight;
      const finalHeight = Math.max(0, start.startHeight - dy);
      const finalRatio = finalHeight / viewportH;
      const smallestSnap = snapPoints[0];

      // Dismiss if we've dragged well below the smallest snap or flicked
      // down with enough velocity.
      const dismissThresholdRatio = smallestSnap * (1 - DISMISS_THRESHOLD_RATIO);
      const shouldDismiss =
        finalRatio < dismissThresholdRatio || velocity > VELOCITY_DISMISS;

      if (shouldDismiss) {
        start.sheet.style.transition = '';
        start.sheet.style.height = '';
        onDismiss();
        return;
      }

      // Find nearest snap.
      let nearestIdx = 0;
      let nearestDelta = Infinity;
      for (let i = 0; i < snapPoints.length; i++) {
        const d = Math.abs(finalRatio - snapPoints[i]);
        if (d < nearestDelta) {
          nearestDelta = d;
          nearestIdx = i;
        }
      }
      // Restore CSS-driven height; clear the inline override.
      start.sheet.style.transition = 'height 200ms ease-out';
      start.sheet.style.height = '';
      if (nearestIdx !== snapIndex) {
        onSnapTo(nearestIdx);
      }
      return;
    }

    // Translate-mode release.
    const downOnly = Math.max(0, dy);
    const sheetHeight = start.startHeight;
    const shouldDismiss =
      downOnly > sheetHeight * DISMISS_THRESHOLD_RATIO ||
      velocity > VELOCITY_DISMISS;
    if (shouldDismiss) {
      start.sheet.style.transition = '';
      start.sheet.style.transform = '';
      onDismiss();
    } else {
      start.sheet.style.transition = 'transform 180ms ease-out';
      start.sheet.style.transform = '';
    }
  };

  return (
    <div
      role="button"
      aria-label={snapPoints ? 'Drag to resize or dismiss' : 'Drag to dismiss'}
      tabIndex={-1}
      onPointerDown={handlePointerDown}
      onPointerMove={handlePointerMove}
      onPointerUp={handlePointerEnd}
      onPointerCancel={handlePointerEnd}
      className="mx-auto my-2 py-1.5 px-6 touch-none"
    >
      <div
        aria-hidden
        className="mx-auto h-1.5 w-10 rounded-full bg-border-strong"
      />
    </div>
  );
}

/**
 * Sheet body wrapper that auto-scrolls the focused input into view when
 * the iOS soft keyboard opens. Without this, tapping an input near the
 * bottom of a long form leaves it hidden behind the keyboard — Radix
 * doesn't scroll Dialog content for focus the way `<select>` does.
 */
function SheetBody({ children }: { children: ReactNode }) {
  const ref = useRef<HTMLDivElement | null>(null);
  useEffect(() => {
    const root = ref.current;
    if (!root) {
      return;
    }
    const handleFocusIn = (e: FocusEvent) => {
      const target = e.target as HTMLElement | null;
      if (!target) {
        return;
      }
      if (
        target.tagName !== 'INPUT' &&
        target.tagName !== 'TEXTAREA' &&
        target.tagName !== 'SELECT' &&
        !(target as HTMLElement).isContentEditable
      ) {
        return;
      }
      // Defer past keyboard-open layout shift so scrollIntoView lands on
      // the post-keyboard viewport, not the pre-keyboard one.
      window.setTimeout(() => {
        target.scrollIntoView({ block: 'center', behavior: 'smooth' });
      }, 250);
    };
    root.addEventListener('focusin', handleFocusIn);
    return () => root.removeEventListener('focusin', handleFocusIn);
  }, []);
  return (
    <div
      ref={ref}
      className="flex-1 overflow-y-auto overscroll-contain px-6 py-4"
    >
      {children}
    </div>
  );
}
