import { useEffect, useRef } from 'react';
import { AlertTriangle, X } from 'lucide-react';

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
      if (e.key === 'Escape') onCancel();
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onCancel]);

  const isDanger = variant === 'danger';

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto" onClick={onCancel}>
      <div className="flex items-center justify-center min-h-screen px-4 py-8">
        <div className="fixed inset-0 bg-black/40 backdrop-blur-sm" />

        <div
          className="relative bg-white dark:bg-[#14161f] rounded-lg shadow-xl dark:shadow-black/20 w-full max-w-md"
          onClick={(e) => e.stopPropagation()}
        >
          <div className="px-6 pt-6 pb-2">
            <div className="flex items-start gap-4">
              <div
                className={`flex-shrink-0 w-10 h-10 rounded-full flex items-center justify-center ${
                  isDanger
                    ? 'bg-red-100 dark:bg-red-900/20'
                    : 'bg-blue-100 dark:bg-blue-900/20'
                }`}
              >
                <AlertTriangle
                  className={`h-5 w-5 ${isDanger ? 'text-red-600 dark:text-red-400' : 'text-blue-600 dark:text-blue-400'}`}
                />
              </div>
              <div className="flex-1 min-w-0">
                <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                  {title}
                </h3>
                <p className="mt-2 text-sm text-[#6b7194] dark:text-[#8b90a8] leading-relaxed">
                  {message}
                </p>
              </div>
              <button
                onClick={onCancel}
                className="flex-shrink-0 p-1 text-[#9ca0b8] dark:text-[#5c6180] hover:text-[#3d4463] dark:hover:text-[#c4c8db] rounded transition-colors"
              >
                <X className="h-5 w-5" />
              </button>
            </div>
          </div>

          <div className="px-6 py-4 flex justify-end gap-3">
            <button
              ref={cancelRef}
              onClick={onCancel}
              disabled={isLoading}
              className="px-4 py-2 text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] bg-white dark:bg-[#1e2130] border border-[#c9cfd9] dark:border-[#3a3f54] rounded-md hover:bg-[#f1f3f9] dark:hover:bg-[#2a2e3f] transition-colors disabled:opacity-50"
            >
              {cancelLabel}
            </button>
            <button
              onClick={onConfirm}
              disabled={isLoading}
              className={`px-4 py-2 text-sm font-medium text-white rounded-md transition-colors disabled:opacity-50 ${
                isDanger
                  ? 'bg-red-600 hover:bg-red-700'
                  : 'bg-[#5c7cfa] hover:bg-[#4c6ef5]'
              }`}
            >
              {confirmLabel}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
