import { useTranslation } from 'react-i18next';
import {
  CheckCircle2,
  FileText,
  FilePlus,
  PenLine,
  Repeat,
  History as HistoryIcon,
} from 'lucide-react';
import { EmptyState, LoadingSpinner } from '@buurman/ui';
import { ErrorMessage } from '@/components/ErrorMessage';
import { useFormatDate } from '@/hooks/useFormatDate';
import type {
  TimelineEventResponse,
  TimelineEventResponseType,
} from '@/generated/models';

interface ContractTimelineProps {
  events: TimelineEventResponse[];
  isLoading: boolean;
  isError?: boolean;
}

const ICON_BY_TYPE: Record<
  TimelineEventResponseType,
  React.ComponentType<{ className?: string }>
> = {
  CONTRACT_CREATED: FilePlus,
  CONTRACT_STATUS_CHANGED: Repeat,
  RENT_CHANGED: Repeat,
  EXTENSION_CREATED: FileText,
  EXTENSION_ACTIVATED: CheckCircle2,
  EXTENSION_DECLINED: FileText,
  DOCUMENT_UPLOADED: FileText,
  DOCUMENT_GENERATED: FileText,
  SIGNATURE_SENT: PenLine,
  SIGNATURE_COMPLETED: CheckCircle2,
  SIGNATURE_DECLINED: PenLine,
  AUDIT_OTHER: HistoryIcon,
};

const TINT_BY_TYPE: Record<TimelineEventResponseType, string> = {
  CONTRACT_CREATED: 'bg-success-bg text-success-text',
  CONTRACT_STATUS_CHANGED: 'bg-info-bg text-info-text',
  RENT_CHANGED: 'bg-info-bg text-info-text',
  EXTENSION_CREATED:
    'bg-primary-50 dark:bg-primary-950 text-primary-600 dark:text-primary-300',
  EXTENSION_ACTIVATED: 'bg-success-bg text-success-text',
  EXTENSION_DECLINED: 'bg-error-bg text-error-text',
  DOCUMENT_UPLOADED:
    'bg-primary-50 dark:bg-primary-950 text-primary-600 dark:text-primary-300',
  DOCUMENT_GENERATED:
    'bg-primary-50 dark:bg-primary-950 text-primary-600 dark:text-primary-300',
  SIGNATURE_SENT: 'bg-info-bg text-info-text',
  SIGNATURE_COMPLETED: 'bg-success-bg text-success-text',
  SIGNATURE_DECLINED: 'bg-error-bg text-error-text',
  AUDIT_OTHER: 'bg-surface-inset text-text-secondary',
};

export const ContractTimeline = ({
  events,
  isLoading,
  isError = false,
}: ContractTimelineProps) => {
  const { t } = useTranslation('contracts');
  const { formatDateTime, formatRelative } = useFormatDate();

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-8">
        <LoadingSpinner />
      </div>
    );
  }

  if (isError) {
    return <ErrorMessage message={t('history.failedToLoad')} />;
  }

  if (events.length === 0) {
    return (
      <EmptyState
        variant="inline"
        icon={<HistoryIcon className="h-8 w-8" />}
        title={t('history.empty')}
        description={t('history.emptyDescription')}
      />
    );
  }

  return (
    <ol className="relative border-l border-border-default ml-3 space-y-6">
      {events.map((event, index) => {
        const Icon = ICON_BY_TYPE[event.type];
        return (
          <li
            key={`${event.type}-${event.timestamp}-${index}`}
            className="ml-6"
          >
            <span
              className={`absolute -left-3 flex h-6 w-6 items-center justify-center rounded-full ring-4 ring-surface-card ${TINT_BY_TYPE[event.type]}`}
            >
              <Icon className="h-3.5 w-3.5" />
            </span>
            <div className="flex flex-wrap items-baseline justify-between gap-x-3 gap-y-1">
              <p className="text-sm font-medium text-text-primary">
                {event.title}
              </p>
              <time
                dateTime={event.timestamp}
                title={formatDateTime(event.timestamp)}
                className="text-xs text-text-muted"
              >
                {formatRelative(event.timestamp)}
              </time>
            </div>
            {event.description && (
              <p className="mt-1 text-xs text-text-secondary">
                {event.description}
              </p>
            )}
          </li>
        );
      })}
    </ol>
  );
};
