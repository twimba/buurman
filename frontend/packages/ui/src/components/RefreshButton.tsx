import { RefreshCw } from 'lucide-react';
import { cn } from '../utils/cn';

interface RefreshButtonProps {
  onClick: () => void;
  isRefreshing?: boolean;
}

export const RefreshButton = ({
  onClick,
  isRefreshing,
}: RefreshButtonProps) => (
  <button
    type="button"
    onClick={onClick}
    disabled={isRefreshing}
    title="Refresh data"
    aria-label="Refresh data"
    className="inline-flex items-center justify-center min-h-touch min-w-touch p-2 border border-border-strong rounded-lg text-text-secondary hover:bg-surface-inset hover:text-text-primary transition-colors disabled:opacity-50 focus-ring"
  >
    <RefreshCw className={cn('h-5 w-5', isRefreshing && 'animate-spin')} />
  </button>
);
