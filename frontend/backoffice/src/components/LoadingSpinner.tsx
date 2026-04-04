import { LoadingSpinner as SharedLoadingSpinner } from '@buurman/ui';

interface LoadingSpinnerProps {
  message?: string;
}

export const LoadingSpinner = ({
  message = 'Loading...',
}: LoadingSpinnerProps) => (
  <SharedLoadingSpinner message={message} fullScreen />
);
