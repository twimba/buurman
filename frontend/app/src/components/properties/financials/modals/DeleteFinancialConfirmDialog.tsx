import { useEffect, useCallback } from 'react';
import { X } from 'lucide-react';

interface DeleteFinancialConfirmDialogProps {
  title: string;
  message: string;
  onConfirm: () => void;
  onCancel: () => void;
  isLoading: boolean;
}

export const DeleteFinancialConfirmDialog = ({
  title,
  message,
  onConfirm,
  onCancel,
  isLoading,
}: DeleteFinancialConfirmDialogProps) => {
  const handleKeyDown = useCallback(
    (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onCancel();
      }
    },
    [onCancel]
  );

  useEffect(() => {
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [handleKeyDown]);

  return (
    <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50">
      <div className="bg-surface-card rounded-lg shadow-xl dark:shadow-black/20 max-w-sm w-full mx-4">
        <div className="flex items-center justify-between p-4 border-b border-border-strong">
          <h2 className="text-lg font-semibold text-text-primary">{title}</h2>
          <button
            type="button"
            onClick={onCancel}
            className="text-text-secondary hover:text-text-secondary"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="p-4">
          <p className="text-sm text-text-secondary">{message}</p>
        </div>

        <div className="flex justify-end gap-3 p-4 border-t border-border-strong">
          <button
            type="button"
            onClick={onCancel}
            className="px-4 py-2 text-sm font-medium text-text-secondary bg-surface-card border border-border-strong rounded-md hover:bg-surface-inset"
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={isLoading}
            className="bg-error-text text-white px-4 py-2 text-sm font-medium rounded-md hover:opacity-90 disabled:opacity-50"
          >
            {isLoading ? 'Deleting...' : 'Delete'}
          </button>
        </div>
      </div>
    </div>
  );
};
