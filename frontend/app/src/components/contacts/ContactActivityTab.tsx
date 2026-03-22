import { useContactActivity } from '@/hooks/useContactHooks';
import {
  InteractionType,
  INTERACTION_TYPE_LABELS,
  ContactActivityItem,
} from '@/types/contact';
import { useFormatDate } from '@/hooks/useFormatDate';
import { RichTextDisplay } from '@/components/common/RichTextDisplay';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { Pagination } from '@buurman/ui';
import { usePagination } from '@/hooks/usePagination';
import {
  Activity,
  StickyNote,
  FileText,
  Phone,
  Users,
  Eye,
  Key,
  Search as SearchIcon,
  MoreHorizontal,
  Pin,
} from 'lucide-react';
import { formatDistanceToNow } from 'date-fns';

const EVENT_TYPE_ICONS: Record<string, React.ReactNode> = {
  AUDIT: <FileText className="h-4 w-4" />,
  NOTE: <StickyNote className="h-4 w-4" />,
};

const INTERACTION_ICONS: Record<InteractionType, React.ReactNode> = {
  [InteractionType.PHONE_CALL]: <Phone className="h-4 w-4" />,
  [InteractionType.MEETING]: <Users className="h-4 w-4" />,
  [InteractionType.VIEWING]: <Eye className="h-4 w-4" />,
  [InteractionType.KEY_HANDOVER]: <Key className="h-4 w-4" />,
  [InteractionType.INSPECTION]: <SearchIcon className="h-4 w-4" />,
  [InteractionType.NOTE]: <StickyNote className="h-4 w-4" />,
  [InteractionType.OTHER]: <MoreHorizontal className="h-4 w-4" />,
};

const EVENT_TYPE_COLORS: Record<string, string> = {
  AUDIT: 'bg-info-bg text-info-text',
  NOTE: 'bg-primary-50 text-primary-700 dark:bg-primary-950 dark:text-primary-300',
};

interface ContactActivityTabProps {
  contactId: string;
}

export const ContactActivityTab = ({ contactId }: ContactActivityTabProps) => {
  const { formatDate } = useFormatDate();
  const { pageParams, page, size, handlePageChange, handleSizeChange } =
    usePagination({ defaultSize: 25 });

  const { data, isLoading } = useContactActivity(contactId, pageParams);

  const items = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;
  const totalElements = data?.totalElements ?? 0;

  const getIcon = (item: ContactActivityItem) => {
    if (item.eventType === 'NOTE' && item.interactionType) {
      return INTERACTION_ICONS[item.interactionType] ?? EVENT_TYPE_ICONS.NOTE;
    }
    return EVENT_TYPE_ICONS[item.eventType] ?? <Activity className="h-4 w-4" />;
  };

  const getColorClass = (item: ContactActivityItem) => {
    return (
      EVENT_TYPE_COLORS[item.eventType] ??
      'bg-surface-inset text-text-secondary'
    );
  };

  if (isLoading && items.length === 0) {
    return (
      <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
        <LoadingSpinner />
      </div>
    );
  }

  return (
    <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-xl font-semibold text-text-primary">
          Activity ({totalElements})
        </h2>
      </div>

      {items.length === 0 ? (
        <div className="text-center py-8">
          <Activity className="h-12 w-12 text-text-disabled mx-auto mb-3" />
          <p className="text-text-secondary">No activity yet</p>
          <p className="text-sm text-text-muted mt-1">
            Activity from notes, updates, and other events will appear here
          </p>
        </div>
      ) : (
        <>
          <div className="relative">
            {/* Timeline line */}
            <div className="absolute left-[19px] top-0 bottom-0 w-px bg-border-default" />

            <div className="space-y-0">
              {items.map((item, index) => (
                <div
                  key={`${item.eventType}-${item.occurredAt}-${index}`}
                  className="relative flex gap-4 pb-4"
                >
                  {/* Timeline dot */}
                  <div
                    className={`relative z-10 flex-shrink-0 w-10 h-10 rounded-full flex items-center justify-center ${getColorClass(item)}`}
                  >
                    {getIcon(item)}
                  </div>

                  {/* Content */}
                  <div className="flex-1 min-w-0 pt-1">
                    <p className="text-sm text-text-primary">
                      {item.description}
                    </p>

                    {/* Note details (inline for NOTE events) */}
                    {item.eventType === 'NOTE' && item.noteBody && (
                      <div className="mt-2 border border-border-default rounded-lg p-3 bg-surface-page">
                        <div className="flex items-center gap-2 mb-1">
                          {item.interactionType && (
                            <span className="text-xs font-medium px-2 py-0.5 rounded-full bg-surface-inset text-text-secondary">
                              {INTERACTION_TYPE_LABELS[item.interactionType]}
                            </span>
                          )}
                          {item.pinned && (
                            <span className="text-xs font-medium px-2 py-0.5 rounded-full bg-primary-100 text-primary-700 dark:bg-primary-900 dark:text-primary-300">
                              <Pin className="h-3 w-3 inline mr-1" />
                              Pinned
                            </span>
                          )}
                        </div>
                        {item.noteSubject && (
                          <p className="text-sm font-medium text-text-primary mb-1">
                            {item.noteSubject}
                          </p>
                        )}
                        <div className="text-sm text-text-secondary line-clamp-3">
                          <RichTextDisplay content={item.noteBody} />
                        </div>
                      </div>
                    )}

                    <div className="flex items-center gap-2 mt-1 text-xs text-text-muted">
                      <span>
                        {formatDistanceToNow(new Date(item.occurredAt), {
                          addSuffix: true,
                        })}
                      </span>
                      <span>·</span>
                      <span>{formatDate(item.occurredAt)}</span>
                      {item.createdByName && (
                        <>
                          <span>·</span>
                          <span>{item.createdByName}</span>
                        </>
                      )}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="mt-4 pt-4 border-t border-border-default">
              <Pagination
                page={page}
                totalPages={totalPages}
                totalElements={totalElements}
                size={size}
                onPageChange={handlePageChange}
                onSizeChange={handleSizeChange}
              />
            </div>
          )}
        </>
      )}
    </div>
  );
};
