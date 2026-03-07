import { AlertTriangle } from 'lucide-react';

interface StalenessWarningProps {
  lastReviewedAt?: string;
}

export const StalenessWarning = ({ lastReviewedAt }: StalenessWarningProps) => {
  const formattedDate = lastReviewedAt
    ? new Date(lastReviewedAt).toLocaleDateString(undefined, {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
      })
    : 'unknown';

  return (
    <div className="flex items-start gap-3 p-4 rounded-lg bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800">
      <AlertTriangle className="h-5 w-5 text-amber-600 dark:text-amber-400 flex-shrink-0 mt-0.5" />
      <div>
        <p className="text-sm font-medium text-amber-800 dark:text-amber-300">
          Regulation data may be outdated
        </p>
        <p className="text-sm text-amber-700 dark:text-amber-400 mt-1">
          Last reviewed: {formattedDate}. Please verify with official sources before applying rent increases.
        </p>
      </div>
    </div>
  );
};
