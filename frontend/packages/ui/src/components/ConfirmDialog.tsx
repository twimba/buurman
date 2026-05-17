import { useEffect, useRef } from 'react';
import { AlertTriangle, X } from 'lucide-react';
import { cn } from '../utils/cn';
import { Button } from './Button';

interface ConfirmDialogProps {
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  variant?: 'danger' | 'default';
  isLoading?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}

export const ConfirmDialog = ({
  title,
  message,
  confirmLabel = 'Confirm',
  cancelLabel = 'Cancel',
  variant = 'default',
  isLoading = false,
  onConfirm,
  onCancel,
}: ConfirmDialogProps) => {
  const cancelRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    cancelRef.current?.focus();
  }, []);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onCancel();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onCancel]);

  const isDanger = variant === 'danger';

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto" onClick={onCancel}>
      <div className="flex items-center justify-center min-h-[100dvh] px-4 py-8">
        <div className="fixed inset-0 bg-surface-overlay backdrop-blur-sm" />

        <div
          className="relative bg-surface-card rounded-lg shadow-xl w-full max-w-md"
          onClick={(e) => e.stopPropagation()}
        >
          <div className="px-6 pt-6 pb-2">
            <div className="flex items-start gap-4">
              <div
                className={cn(
                  'flex-shrink-0 w-10 h-10 rounded-full flex items-center justify-center',
                  isDanger ? 'bg-error-bg' : 'bg-info-bg'
                )}
              >
                <AlertTriangle
                  className={cn(
                    'h-5 w-5',
                    isDanger ? 'text-error-text' : 'text-primary-600'
                  )}
                />
              </div>
              <div className="flex-1 min-w-0">
                <h3 className="text-lg font-semibold text-text-primary">
                  {title}
                </h3>
                <p className="mt-2 text-sm text-text-secondary leading-relaxed">
                  {message}
                </p>
              </div>
              <button
                onClick={onCancel}
                className="flex-shrink-0 p-1 text-text-muted hover:text-text-primary rounded transition-colors"
              >
                <X className="h-5 w-5" />
              </button>
            </div>
          </div>

          <div className="px-6 py-4 flex justify-end gap-3">
            <Button
              ref={cancelRef}
              variant="secondary"
              size="md"
              onClick={onCancel}
              disabled={isLoading}
            >
              {cancelLabel}
            </Button>
            <Button
              variant={isDanger ? 'danger' : 'primary'}
              size="md"
              onClick={onConfirm}
              disabled={isLoading}
            >
              {confirmLabel}
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
};
