import { useState, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Calendar,
  Copy,
  Check,
  RefreshCw,
  Trash2,
  Plus,
  Link,
  Home,
  User,
  Eye,
  AlertTriangle,
  Loader2,
  ChevronDown,
  ChevronRight,
} from 'lucide-react';
import { Button, useToast } from '@buurman/ui';
import { CalendarPreviewModal } from './CalendarPreviewModal';
import {
  useCalendarFeeds,
  useCreateCalendarFeed,
  useRotateCalendarFeedToken,
  useDeleteCalendarFeed,
} from '../../hooks/useCalendarFeedHooks';
import {
  CalendarFeedResponseFeedType as CalendarFeedType,
  type CalendarFeedResponse,
} from '../../generated/models';

export const CalendarFeedsSection = () => {
  const { t } = useTranslation('settings');
  const { data: feeds = [], isLoading } = useCalendarFeeds();
  const createFeedMutation = useCreateCalendarFeed();
  const rotateMutation = useRotateCalendarFeedToken();
  const deleteMutation = useDeleteCalendarFeed();
  const { showToast } = useToast();

  const [copiedId, setCopiedId] = useState<string | null>(null);
  const [confirmRotateId, setConfirmRotateId] = useState<string | null>(null);
  const [confirmDeleteId, setConfirmDeleteId] = useState<string | null>(null);
  const [previewFeed, setPreviewFeed] = useState<CalendarFeedResponse | null>(
    null
  );
  const [collapsedGroups, setCollapsedGroups] = useState<Set<string>>(
    new Set()
  );

  const toggleGroup = (type: string) => {
    setCollapsedGroups((prev) => {
      const next = new Set(prev);
      if (next.has(type)) {
        next.delete(type);
      } else {
        next.add(type);
      }
      return next;
    });
  };

  const hasAllPaymentsFeed = feeds.some(
    (f) => f.feedType === CalendarFeedType.ALL_PAYMENTS
  );

  const handleCopy = async (url: string, identifier: string) => {
    try {
      await navigator.clipboard.writeText(url);
      setCopiedId(identifier);
      showToast(t('calendarFeeds.copied'), 'info');
      setTimeout(() => setCopiedId(null), 2000);
    } catch {
      showToast(t('calendarFeeds.copyFailed'), 'error');
    }
  };

  const handleCreateAllPayments = () => {
    createFeedMutation.mutate({ feedType: CalendarFeedType.ALL_PAYMENTS });
  };

  const handleRotate = (identifier: string) => {
    rotateMutation.mutate(identifier, {
      onSuccess: () => setConfirmRotateId(null),
    });
  };

  const handleDelete = (identifier: string) => {
    deleteMutation.mutate(identifier, {
      onSuccess: () => setConfirmDeleteId(null),
    });
  };

  const getFeedLabel = (feed: CalendarFeedResponse) => {
    return feed.entityLabel || feed.identifier;
  };

  const feedGroups = useMemo(() => {
    const order: CalendarFeedType[] = [
      CalendarFeedType.ALL_PAYMENTS,
      CalendarFeedType.PROPERTY_PAYMENTS,
      CalendarFeedType.CONTACT_PAYMENTS,
      CalendarFeedType.CONTRACT,
    ];
    const labels: Record<CalendarFeedType, string> = {
      [CalendarFeedType.ALL_PAYMENTS]: t('calendarFeeds.feedTypes.allPayments'),
      [CalendarFeedType.PROPERTY_PAYMENTS]: t(
        'calendarFeeds.feedTypes.propertyPayments'
      ),
      [CalendarFeedType.CONTACT_PAYMENTS]: t(
        'calendarFeeds.feedTypes.contactPayments'
      ),
      [CalendarFeedType.CONTRACT]: t('calendarFeeds.feedTypes.contract'),
    };
    const icons: Record<CalendarFeedType, typeof Calendar> = {
      [CalendarFeedType.ALL_PAYMENTS]: Calendar,
      [CalendarFeedType.PROPERTY_PAYMENTS]: Home,
      [CalendarFeedType.CONTACT_PAYMENTS]: User,
      [CalendarFeedType.CONTRACT]: Link,
    };
    const grouped = new Map<CalendarFeedType, CalendarFeedResponse[]>();
    for (const feed of feeds) {
      const list = grouped.get(feed.feedType) ?? [];
      list.push(feed);
      grouped.set(feed.feedType, list);
    }
    return order
      .filter((type) => grouped.has(type))
      .map((type) => ({
        type,
        label: labels[type],
        Icon: icons[type],
        feeds: grouped.get(type) ?? [],
      }));
  }, [feeds, t]);

  if (isLoading) {
    return (
      <div className="flex justify-center py-12">
        <Loader2 className="h-8 w-8 animate-spin text-primary-500" />
      </div>
    );
  }

  return (
    <div className="mt-6 space-y-6">
      {/* Header */}
      <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
        <div className="flex items-center gap-3 mb-2">
          <Calendar className="h-6 w-6 text-primary-500 dark:text-primary-300" />
          <h2 className="text-xl font-semibold text-text-primary">
            {t('calendarFeeds.title')}
          </h2>
        </div>
        <p className="text-sm text-text-secondary mb-4">
          {t('calendarFeeds.subtitle')}
        </p>

        {!hasAllPaymentsFeed && (
          <Button
            variant="primary"
            leftIcon={<Plus />}
            onClick={handleCreateAllPayments}
            isLoading={createFeedMutation.isPending}
          >
            {t('calendarFeeds.createAllPayments')}
          </Button>
        )}
      </div>

      {/* Feeds List - grouped by type */}
      {feedGroups.map((group) => (
        <div key={group.type}>
          <button
            onClick={() => toggleGroup(group.type)}
            className="flex items-center gap-2 mb-3 group w-full text-left"
          >
            {collapsedGroups.has(group.type) ? (
              <ChevronRight className="h-4 w-4 text-text-muted " />
            ) : (
              <ChevronDown className="h-4 w-4 text-text-muted " />
            )}
            <group.Icon className="h-4 w-4 text-text-muted " />
            <h3 className="text-sm font-semibold text-text-secondary uppercase tracking-wider group-hover:text-text-primary transition-colors">
              {group.label}
            </h3>
            <span className="text-xs text-text-muted">
              ({group.feeds.length})
            </span>
          </button>
          {!collapsedGroups.has(group.type) && (
            <div className="bg-surface-card rounded-lg shadow-sm border border-border-default divide-y divide-border-default">
              {group.feeds.map((feed) => (
                <div key={feed.identifier} className="p-5 space-y-3">
                  {/* Header row: label + actions */}
                  <div className="flex items-center justify-between gap-4">
                    <span className="font-medium text-text-primary truncate">
                      {getFeedLabel(feed)}
                    </span>

                    <div className="flex items-center gap-1 flex-shrink-0">
                      <button
                        onClick={() => setPreviewFeed(feed)}
                        className="p-2 rounded-lg hover:bg-surface-inset transition-colors"
                        title={t('calendarFeeds.previewCalendar')}
                      >
                        <Eye className="h-4 w-4 text-text-secondary" />
                      </button>
                      <button
                        onClick={() =>
                          handleCopy(feed.feedUrl, feed.identifier)
                        }
                        className="p-2 rounded-lg hover:bg-surface-inset transition-colors"
                        title={t('calendarFeeds.copyUrl')}
                      >
                        {copiedId === feed.identifier ? (
                          <Check className="h-4 w-4 text-success-text" />
                        ) : (
                          <Copy className="h-4 w-4 text-text-secondary " />
                        )}
                      </button>
                      <button
                        onClick={() => setConfirmRotateId(feed.identifier)}
                        className="p-2 rounded-lg hover:bg-surface-inset transition-colors"
                        title={t('calendarFeeds.regenerateUrl')}
                      >
                        <RefreshCw className="h-4 w-4 text-text-secondary " />
                      </button>
                      <button
                        onClick={() => setConfirmDeleteId(feed.identifier)}
                        className="p-2 rounded-lg hover:bg-error-bg transition-colors"
                        title={t('calendarFeeds.deleteFeed')}
                      >
                        <Trash2 className="h-4 w-4 text-text-muted hover:text-error-text" />
                      </button>
                    </div>
                  </div>

                  {/* URL display */}
                  <code className="block text-xs bg-surface-inset text-text-secondary px-3 py-2 rounded-lg truncate border border-border-default">
                    {feed.feedUrl}
                  </code>

                  {/* Confirmation banners */}
                  {confirmRotateId === feed.identifier && (
                    <div className="flex items-center gap-3 bg-warning-bg border border-warning-border rounded-lg px-4 py-3">
                      <AlertTriangle className="h-4 w-4 text-warning-text flex-shrink-0" />
                      <span className="text-sm text-warning-text flex-1">
                        {t('calendarFeeds.regenerateWarning')}
                      </span>
                      <div className="flex items-center gap-2 flex-shrink-0">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => setConfirmRotateId(null)}
                        >
                          Cancel
                        </Button>
                        <Button
                          variant="danger"
                          size="sm"
                          onClick={() => handleRotate(feed.identifier)}
                          isLoading={rotateMutation.isPending}
                        >
                          {t('calendarFeeds.regenerate')}
                        </Button>
                      </div>
                    </div>
                  )}
                  {confirmDeleteId === feed.identifier && (
                    <div className="flex items-center gap-3 bg-error-bg border border-error-border rounded-lg px-4 py-3">
                      <AlertTriangle className="h-4 w-4 text-error-text flex-shrink-0" />
                      <span className="text-sm text-error-text flex-1">
                        {t('calendarFeeds.deleteConfirm')}
                      </span>
                      <div className="flex items-center gap-2 flex-shrink-0">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => setConfirmDeleteId(null)}
                        >
                          Cancel
                        </Button>
                        <Button
                          variant="danger"
                          size="sm"
                          onClick={() => handleDelete(feed.identifier)}
                          isLoading={deleteMutation.isPending}
                        >
                          Delete
                        </Button>
                      </div>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      ))}

      {/* Empty state */}
      {feeds.length === 0 && (
        <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-12 text-center">
          <Calendar className="h-12 w-12 text-text-disabled mx-auto mb-3" />
          <p className="text-text-secondary">{t('calendarFeeds.empty')}</p>
        </div>
      )}

      {/* Preview modal */}
      {previewFeed && (
        <CalendarPreviewModal
          open={!!previewFeed}
          onClose={() => setPreviewFeed(null)}
          feedUrl={previewFeed.feedUrl}
          feedLabel={previewFeed.entityLabel || previewFeed.identifier}
        />
      )}
    </div>
  );
};
