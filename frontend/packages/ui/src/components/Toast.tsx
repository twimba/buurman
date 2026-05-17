import {
  createContext,
  useContext,
  useState,
  useCallback,
  type ReactNode,
} from 'react';
import { X, CheckCircle, AlertCircle, Info } from 'lucide-react';
import { cn } from '../utils/cn';

export type ToastType = 'success' | 'error' | 'info';

interface Toast {
  id: string;
  message: ReactNode;
  type: ToastType;
}

interface ToastContextType {
  showToast: (message: ReactNode, type?: ToastType) => void;
}

const ToastContext = createContext<ToastContextType | undefined>(undefined);

export const useToast = () => {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error('useToast must be used within ToastProvider');
  }
  return context;
};

const iconMap = {
  success: CheckCircle,
  error: AlertCircle,
  info: Info,
} as const;

const iconStyles = {
  success: 'text-success',
  error: 'text-error',
  info: 'text-primary-500',
} as const;

const toastStyles = {
  success: 'bg-success-bg border-success-border text-success-text',
  error: 'bg-error-bg border-error-border text-error-text',
  info: 'bg-info-bg border-info-border text-info-text',
} as const;

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);

  const showToast = useCallback(
    (message: ReactNode, type: ToastType = 'info') => {
      const id = Math.random().toString(36).substring(7);

      setToasts((prev) => [...prev, { id, message, type }]);

      setTimeout(() => {
        setToasts((prev) => prev.filter((t) => t.id !== id));
      }, 5000);
    },
    []
  );

  const dismissToast = useCallback((id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  return (
    <ToastContext value={{ showToast }}>
      {children}

      <div
        className="fixed z-50 space-y-2 inset-x-2 max-w-none md:inset-auto md:right-4 md:max-w-md"
        style={{
          top: 'calc(var(--env-banner-height, 0px) + env(safe-area-inset-top, 0px) + 0.5rem)',
        }}
        aria-live="polite"
        role="status"
      >
        {toasts.map((toast) => {
          const Icon = iconMap[toast.type];
          return (
            <div
              key={toast.id}
              className={cn(
                'flex items-start gap-3 rounded-lg border p-4 shadow-lg animate-slide-in',
                toastStyles[toast.type]
              )}
            >
              <Icon
                className={cn('h-5 w-5 shrink-0', iconStyles[toast.type])}
              />
              <div className="flex-1 text-sm font-medium">{toast.message}</div>
              <button
                onClick={() => dismissToast(toast.id)}
                className="shrink-0 transition-opacity hover:opacity-70"
                aria-label="Dismiss"
              >
                <X className="h-4 w-4" />
              </button>
            </div>
          );
        })}
      </div>
    </ToastContext>
  );
}

ToastProvider.displayName = 'ToastProvider';
