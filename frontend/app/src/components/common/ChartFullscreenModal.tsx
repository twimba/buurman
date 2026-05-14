import { useEffect, type ReactNode } from 'react';
import { X } from 'lucide-react';

interface ChartFullscreenModalProps {
  title: string;
  subtitle?: string;
  icon?: ReactNode;
  controls?: ReactNode;
  onClose: () => void;
  children: ReactNode;
}

export const ChartFullscreenModal = ({
  title,
  subtitle,
  icon,
  controls,
  onClose,
  children,
}: ChartFullscreenModalProps) => {
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };
    document.addEventListener('keydown', handleKeyDown);
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      document.body.style.overflow = '';
    };
  }, [onClose]);

  return (
    <div className="fixed inset-0 z-50 flex flex-col bg-surface-page">
      {/* Header */}
      <div className="flex items-center justify-between px-6 py-4 bg-surface-card border-b border-border-default shrink-0">
        <div className="flex items-center gap-3">
          {icon && <span className="text-text-secondary">{icon}</span>}
          <div>
            <h2 className="text-lg font-semibold text-text-primary">{title}</h2>
            {subtitle && (
              <p className="text-sm text-text-secondary">{subtitle}</p>
            )}
          </div>
        </div>
        <div className="flex items-center gap-3">
          {controls && (
            <div className="flex items-center gap-2 flex-wrap justify-end">
              {controls}
            </div>
          )}
          <button
            onClick={onClose}
            className="p-2 rounded-md text-text-muted hover:text-text-secondary hover:bg-surface-inset transition-colors ml-2"
            title="Close"
          >
            <X className="h-5 w-5" />
          </button>
        </div>
      </div>

      {/* Body */}
      <div className="flex-1 overflow-auto p-6">{children}</div>
    </div>
  );
};
