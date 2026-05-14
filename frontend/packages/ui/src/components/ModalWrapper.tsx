import * as Dialog from '@radix-ui/react-dialog';
import { X } from 'lucide-react';
import { useCallback, useEffect } from 'react';
import { cn } from '../utils/cn';

interface ModalWrapperProps {
  open: boolean;
  onClose: () => void;
  title: string;
  subtitle?: string;
  size?: 'sm' | 'md' | 'lg' | 'xl' | 'full';
  children: React.ReactNode;
  footer?: React.ReactNode;
  preventClose?: boolean;
  onSubmit?: () => void;
  initialFocusRef?: React.RefObject<HTMLElement>;
  className?: string;
}

const sizeMap = {
  sm: 'max-w-md',
  md: 'max-w-lg',
  lg: 'max-w-2xl',
  xl: 'max-w-4xl',
  full: 'max-w-[calc(100vw-2rem)]',
} as const;

export function ModalWrapper({
  open,
  onClose,
  title,
  subtitle,
  size = 'md',
  children,
  footer,
  preventClose,
  onSubmit,
  initialFocusRef,
  className,
}: ModalWrapperProps) {
  const handleOpenChange = useCallback(
    (nextOpen: boolean) => {
      if (!nextOpen && !preventClose) {
        onClose();
      }
    },
    [onClose, preventClose]
  );

  // Cmd/Ctrl+Enter to submit
  useEffect(() => {
    if (!open || !onSubmit) {
      return;
    }

    function handleKeyDown(e: KeyboardEvent) {
      if (e.key === 'Enter' && (e.metaKey || e.ctrlKey)) {
        e.preventDefault();
        onSubmit?.();
      }
    }

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [open, onSubmit]);

  return (
    <Dialog.Root open={open} onOpenChange={handleOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 z-50 bg-surface-overlay data-[state=open]:animate-in data-[state=open]:fade-in-0 data-[state=closed]:animate-out data-[state=closed]:fade-out-0" />
        <Dialog.Content
          className={cn(
            'fixed top-1/2 left-1/2 z-50 w-full -translate-x-1/2 -translate-y-1/2',
            'bg-surface-card rounded-xl border border-border-default shadow-lg',
            'data-[state=open]:animate-in data-[state=open]:fade-in-0 data-[state=open]:zoom-in-95',
            'data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=closed]:zoom-out-95',
            'max-h-[85vh] flex flex-col',
            sizeMap[size],
            className
          )}
          onOpenAutoFocus={(e) => {
            if (initialFocusRef?.current) {
              e.preventDefault();
              initialFocusRef.current.focus();
            }
          }}
        >
          {/* Header */}
          <div className="flex items-start justify-between border-b border-border-default px-6 py-4">
            <div>
              <Dialog.Title className="text-lg font-semibold text-text-primary">
                {title}
              </Dialog.Title>
              {subtitle && (
                <Dialog.Description className="mt-1 text-sm text-text-secondary">
                  {subtitle}
                </Dialog.Description>
              )}
            </div>
            {!preventClose && (
              <Dialog.Close className="rounded-md p-1.5 text-text-muted hover:bg-surface-inset hover:text-text-primary focus-ring">
                <X className="h-5 w-5" />
                <span className="sr-only">Close</span>
              </Dialog.Close>
            )}
          </div>

          {/* Body */}
          <div className="flex-1 overflow-y-auto px-6 py-4">{children}</div>

          {/* Footer */}
          {footer && (
            <div className="flex items-center justify-end gap-2 border-t border-border-default px-6 py-4">
              {footer}
            </div>
          )}
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}

ModalWrapper.displayName = 'ModalWrapper';
