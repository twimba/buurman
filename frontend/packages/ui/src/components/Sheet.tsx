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
  className?: string;
}

/**
 * Responsive modal:
 * - phone (`<md`): full-width bottom sheet with drag-handle and safe-area padding
 * - tablet/desktop (`md+`): centered dialog (matches existing `ModalWrapper` visuals)
 *
 * Built on Radix Dialog so focus trap, ESC, scroll lock, ARIA are handled.
 * No Vaul dependency.
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
  className,
}: SheetProps) {
  const [isDesktop, setIsDesktop] = useState(() =>
    typeof window === 'undefined'
      ? true
      : window.matchMedia('(min-width: 768px)').matches
  );

  useEffect(() => {
    if (forceSheet) {
      return;
    }
    const mq = window.matchMedia('(min-width: 768px)');
    const handler = (e: MediaQueryListEvent) => setIsDesktop(e.matches);
    mq.addEventListener('change', handler);
    return () => mq.removeEventListener('change', handler);
  }, [forceSheet]);

  const handleOpenChange = useCallback(
    (nextOpen: boolean) => {
      if (!nextOpen && !preventClose) {
        onClose();
      }
    },
    [onClose, preventClose]
  );

  const renderAsSheet = forceSheet || !isDesktop;

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
                  'max-h-[92dvh]',
                  'rounded-t-2xl',
                  'data-[state=open]:animate-slide-up',
                  'data-[state=closed]:animate-slide-down',
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
                }
              : undefined
          }
        >
          {renderAsSheet && (
            <div
              aria-hidden
              className="mx-auto my-2 h-1.5 w-10 rounded-full bg-border-strong"
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
