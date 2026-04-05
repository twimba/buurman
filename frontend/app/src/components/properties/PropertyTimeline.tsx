import { useTranslation } from 'react-i18next';
import { usePropertyTimeline } from '@/hooks/useOccupancyPeriodHooks';
import { LoadingSpinner } from '@buurman/ui';
import { Home, FileText, AlertCircle } from 'lucide-react';
import { useFormatDate } from '@/hooks/useFormatDate';

interface PropertyTimelineProps {
  propertyIdentifier: string;
}

const typeConfigStyles = {
  SELF_OCCUPANCY: {
    labelKey: 'timeline.selfOccupancy',
    icon: Home,
    bg: 'bg-info-bg',
    border: 'border-info-border',
    dot: 'bg-info-text',
    text: 'text-info-text',
  },
  CONTRACT: {
    labelKey: 'timeline.contract',
    icon: FileText,
    bg: 'bg-info-bg',
    border: 'border-info-border',
    dot: 'bg-info-text',
    text: 'text-info-text',
  },
  VACANCY: {
    labelKey: 'timeline.vacancy',
    icon: AlertCircle,
    bg: 'bg-surface-inset',
    border: 'border-border-default',
    dot: 'bg-neutral-400',
    text: 'text-text-secondary',
  },
};

export const PropertyTimeline = ({
  propertyIdentifier,
}: PropertyTimelineProps) => {
  const { t } = useTranslation('properties');
  const { data: timeline, isLoading } = usePropertyTimeline(propertyIdentifier);
  const { formatDate } = useFormatDate();

  if (isLoading) {
    return <LoadingSpinner />;
  }

  if (!timeline?.entries.length) {
    return (
      <div className="text-center py-12 text-text-secondary">
        {t('timeline.noEntries')}
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {timeline.entries.map((entry) => {
        const config = typeConfigStyles[entry.type];
        const Icon = config.icon;

        return (
          <div
            key={`${entry.type}-${entry.identifier}`}
            className={`flex items-start gap-4 p-4 rounded-lg border ${config.bg} ${config.border}`}
          >
            <div
              className={`flex-shrink-0 w-10 h-10 rounded-full flex items-center justify-center ${config.bg}`}
            >
              <Icon className={`h-5 w-5 ${config.text}`} />
            </div>
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 mb-1">
                <span className={`text-sm font-semibold ${config.text}`}>
                  {t(config.labelKey)}
                </span>
                {entry.description && (
                  <span className="text-xs text-text-secondary">
                    ({entry.description})
                  </span>
                )}
              </div>
              <div className="text-sm text-text-secondary">
                {formatDate(entry.startDate)} &mdash;{' '}
                {entry.endDate ? formatDate(entry.endDate) : t('timeline.ongoing')}
              </div>
              {entry.metadata && (
                <div className="text-xs text-text-secondary mt-1">
                  {entry.metadata}
                </div>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
};
