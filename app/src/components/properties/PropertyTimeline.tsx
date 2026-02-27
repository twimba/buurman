import { usePropertyTimeline } from '@/hooks/useOccupancyPeriodHooks';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { Home, FileText, AlertCircle } from 'lucide-react';
import { useFormatDate } from '@/hooks/useFormatDate';

interface PropertyTimelineProps {
  propertyIdentifier: string;
}

const typeConfig = {
  SELF_OCCUPANCY: {
    label: 'Self-Occupancy',
    icon: Home,
    bg: 'bg-indigo-50 dark:bg-indigo-900/20',
    border: 'border-indigo-200 dark:border-indigo-800',
    dot: 'bg-indigo-500',
    text: 'text-indigo-700 dark:text-indigo-300',
  },
  CONTRACT: {
    label: 'Contract',
    icon: FileText,
    bg: 'bg-blue-50 dark:bg-blue-900/20',
    border: 'border-blue-200 dark:border-blue-800',
    dot: 'bg-blue-500',
    text: 'text-blue-700 dark:text-blue-300',
  },
  VACANCY: {
    label: 'Vacancy',
    icon: AlertCircle,
    bg: 'bg-gray-50 dark:bg-gray-900/20',
    border: 'border-gray-200 dark:border-gray-800',
    dot: 'bg-gray-400',
    text: 'text-gray-700 dark:text-gray-300',
  },
};

export const PropertyTimeline = ({
  propertyIdentifier,
}: PropertyTimelineProps) => {
  const { data: timeline, isLoading } = usePropertyTimeline(propertyIdentifier);
  const { formatDate } = useFormatDate();

  if (isLoading) {
    return <LoadingSpinner />;
  }

  if (!timeline?.entries.length) {
    return (
      <div className="text-center py-12 text-[#6b7194] dark:text-[#8b90a8]">
        No timeline entries yet
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {timeline.entries.map((entry) => {
        const config = typeConfig[entry.type];
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
                  {config.label}
                </span>
                {entry.description && (
                  <span className="text-xs text-[#6b7194] dark:text-[#8b90a8]">
                    ({entry.description})
                  </span>
                )}
              </div>
              <div className="text-sm text-[#3d4463] dark:text-[#c4c8db]">
                {formatDate(entry.startDate)} &mdash;{' '}
                {entry.endDate ? formatDate(entry.endDate) : 'Ongoing'}
              </div>
              {entry.metadata && (
                <div className="text-xs text-[#6b7194] dark:text-[#8b90a8] mt-1">
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
