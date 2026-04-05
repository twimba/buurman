import { Construction } from 'lucide-react';
import { useTranslation } from 'react-i18next';

interface WorkInProgressProps {
  pageName?: string;
  message?: string;
}

export const WorkInProgress = ({
  pageName,
  message,
}: WorkInProgressProps) => {
  const { t } = useTranslation('common');

  const resolvedMessage = message ?? t('wip.message');

  return (
    <div className="flex items-center justify-center min-h-[calc(100vh-4rem)]">
      <div className="text-center max-w-md px-6">
        {/* Icon */}
        <div className="mb-8 flex justify-center">
          <div className="relative">
            <div className="absolute inset-0 bg-primary-100 rounded-full blur-2xl opacity-50 animate-pulse" />
            <Construction
              className="h-32 w-32 text-primary-500 dark:text-primary-300 relative"
              strokeWidth={1.5}
            />
          </div>
        </div>

        {/* Title */}
        <h1 className="text-3xl font-bold text-text-primary mb-4">
          {pageName
            ? t('wip.titleWithPage', { page: pageName })
            : t('wip.title')}
        </h1>

        {/* Message */}
        <p className="text-lg text-text-secondary mb-2">{resolvedMessage}</p>

        {/* Subtext */}
        <p className="text-sm text-text-secondary">
          {t('wip.subtext')}
        </p>

        {/* Decorative dots */}
        <div className="mt-8 flex justify-center gap-2">
          <div
            className="h-2 w-2 rounded-full bg-primary-500 animate-bounce"
            style={{ animationDelay: '0ms' }}
          />
          <div
            className="h-2 w-2 rounded-full bg-primary-500 animate-bounce"
            style={{ animationDelay: '150ms' }}
          />
          <div
            className="h-2 w-2 rounded-full bg-primary-500 animate-bounce"
            style={{ animationDelay: '300ms' }}
          />
        </div>
      </div>
    </div>
  );
};
