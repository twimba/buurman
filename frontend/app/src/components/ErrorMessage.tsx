import { AlertCircle } from 'lucide-react';
import { Button } from '@buurman/ui';

interface ErrorMessageProps {
  message: string;
  description?: string;
  onRetry?: () => void;
  retryLabel?: string;
  isRetrying?: boolean;
}

export const ErrorMessage = ({
  message,
  description,
  onRetry,
  retryLabel,
  isRetrying,
}: ErrorMessageProps) => {
  return (
    <div
      role="alert"
      className="flex flex-col gap-3 p-4 bg-error-bg border border-error-border rounded-lg text-error-text sm:flex-row sm:items-center sm:justify-between"
    >
      <div className="flex items-start gap-2 min-w-0">
        <AlertCircle className="h-5 w-5 shrink-0 mt-0.5" aria-hidden="true" />
        <div className="min-w-0 break-words">
          <span>{message}</span>
          {description && <p className="text-sm">{description}</p>}
        </div>
      </div>
      {onRetry && (
        <Button
          variant="secondary"
          size="sm"
          className="shrink-0 self-start sm:self-auto"
          onClick={onRetry}
          isLoading={isRetrying}
        >
          {retryLabel}
        </Button>
      )}
    </div>
  );
};

ErrorMessage.displayName = 'ErrorMessage';
