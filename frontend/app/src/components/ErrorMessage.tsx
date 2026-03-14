import { AlertCircle } from 'lucide-react';

interface ErrorMessageProps {
  message: string;
}

export const ErrorMessage = ({ message }: ErrorMessageProps) => {
  return (
    <div className="flex items-center gap-2 p-4 bg-error-bg border border-error-border rounded-lg text-error-text">
      <AlertCircle className="h-5 w-5" />
      <span>{message}</span>
    </div>
  );
};
