import { useState, useMemo } from 'react';
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
  AlertTriangle,
  Loader2,
  ChevronDown,
  ChevronRight,
} from 'lucide-react';
import { Button } from '../ui';
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
import { useToast } from '../../context/ToastContext';

export const CalendarFeedsSection = () => {
  const { data: feeds = [], isLoading } = useCalendarFeeds();
  const createFeedMutation = useCreateCalendarFeed();
  const rotateMutation = useRotateCalendarFeedToken();
  const deleteMutation = useDeleteCalendarFeed();
  const { showToast } = useToast();

  const [copiedId, setCopiedId] = useState<string | null>(null);
  const [confirmRotateId, setConfirmRotateId] = useState<string | null>(null);
  const [confirmDeleteId, setConfirmDeleteId] = useState<string | null>(null);
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
      showToast('Feed URL copied to clipboard', 'info');
      setTimeout(() => setCopiedId(null), 2000);
    } catch {
      showToast('Failed to copy link', 'error');
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
      CalendarFeedType.TENANT_PAYMENTS,
      CalendarFeedType.CONTRACT,
    ];
    const labels: Record<CalendarFeedType, string> = {
      [CalendarFeedType.ALL_PAYMENTS]: 'All Payments',
      [CalendarFeedType.PROPERTY_PAYMENTS]: 'Property Payments',
      [CalendarFeedType.TENANT_PAYMENTS]: 'Tenant Payments',
      [CalendarFeedType.CONTRACT]: 'Contract',
    };
    const icons: Record<CalendarFeedType, typeof Calendar> = {
      [CalendarFeedType.ALL_PAYMENTS]: Calendar,
      [CalendarFeedType.PROPERTY_PAYMENTS]: Home,
      [CalendarFeedType.TENANT_PAYMENTS]: User,
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
  }, [feeds]);

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
      <div className="bg-white dark:bg-[#14161f] rounded-xl shadow-sm p-6">
        <div className="flex items-center gap-3 mb-2">
          <Calendar className="h-6 w-6 text-primary-500 dark:text-primary-300" />
          <h2 className="text-xl font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
            Calendar Feeds
          </h2>
        </div>
        <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] mb-4">
          Subscribe to payment due dates in Google Calendar, Apple Calendar, or
          any app that supports iCalendar feeds. Each feed has a unique URL that
          you can add as a calendar subscription.
        </p>

        {!hasAllPaymentsFeed && (
          <Button
            variant="primary"
            leftIcon={<Plus />}
            onClick={handleCreateAllPayments}
            isLoading={createFeedMutation.isPending}
          >
            Create All Payments Feed
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
              <ChevronRight className="h-4 w-4 text-[#9ca0b8] dark:text-[#5c6180]" />
            ) : (
              <ChevronDown className="h-4 w-4 text-[#9ca0b8] dark:text-[#5c6180]" />
            )}
            <group.Icon className="h-4 w-4 text-[#9ca0b8] dark:text-[#5c6180]" />
            <h3 className="text-sm font-semibold text-[#3d4463] dark:text-[#c4c8db] uppercase tracking-wider group-hover:text-[#1a1d2e] dark:group-hover:text-[#eef0f6] transition-colors">
              {group.label}
            </h3>
            <span className="text-xs text-[#9ca0b8] dark:text-[#5c6180]">
              ({group.feeds.length})
            </span>
          </button>
          {!collapsedGroups.has(group.type) && (
            <div className="bg-white dark:bg-[#14161f] rounded-xl shadow-sm divide-y divide-[#e2e6f0] dark:divide-[#2a2e3f]">
              {group.feeds.map((feed) => (
                <div key={feed.identifier} className="p-5 space-y-3">
                  {/* Header row: label + actions */}
                  <div className="flex items-center justify-between gap-4">
                    <span className="font-medium text-[#1a1d2e] dark:text-[#eef0f6] truncate">
                      {getFeedLabel(feed)}
                    </span>

                    <div className="flex items-center gap-1 flex-shrink-0">
                      <button
                        onClick={() =>
                          handleCopy(feed.feedUrl, feed.identifier)
                        }
                        className="p-2 rounded-lg hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] transition-colors"
                        title="Copy URL"
                      >
                        {copiedId === feed.identifier ? (
                          <Check className="h-4 w-4 text-green-500" />
                        ) : (
                          <Copy className="h-4 w-4 text-[#6b7194] dark:text-[#8b90a8]" />
                        )}
                      </button>
                      <button
                        onClick={() => setConfirmRotateId(feed.identifier)}
                        className="p-2 rounded-lg hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] transition-colors"
                        title="Regenerate URL"
                      >
                        <RefreshCw className="h-4 w-4 text-[#6b7194] dark:text-[#8b90a8]" />
                      </button>
                      <button
                        onClick={() => setConfirmDeleteId(feed.identifier)}
                        className="p-2 rounded-lg hover:bg-red-50 dark:hover:bg-red-900/20 transition-colors"
                        title="Delete feed"
                      >
                        <Trash2 className="h-4 w-4 text-[#9ca0b8] dark:text-[#5c6180] hover:text-red-500" />
                      </button>
                    </div>
                  </div>

                  {/* URL display */}
                  <code className="block text-xs bg-[#f1f3f9] dark:bg-[#1a1d28] text-[#3d4463] dark:text-[#c4c8db] px-3 py-2 rounded-lg truncate border border-[#e2e6f0] dark:border-[#2a2e3f]">
                    {feed.feedUrl}
                  </code>

                  {/* Confirmation banners */}
                  {confirmRotateId === feed.identifier && (
                    <div className="flex items-center gap-3 bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-700 rounded-lg px-4 py-3">
                      <AlertTriangle className="h-4 w-4 text-yellow-600 dark:text-yellow-400 flex-shrink-0" />
                      <span className="text-sm text-yellow-800 dark:text-yellow-300 flex-1">
                        Regenerating will invalidate the current URL.
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
                          Regenerate
                        </Button>
                      </div>
                    </div>
                  )}
                  {confirmDeleteId === feed.identifier && (
                    <div className="flex items-center gap-3 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-700 rounded-lg px-4 py-3">
                      <AlertTriangle className="h-4 w-4 text-red-600 dark:text-red-400 flex-shrink-0" />
                      <span className="text-sm text-red-800 dark:text-red-300 flex-1">
                        Delete this calendar feed?
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
        <div className="bg-white dark:bg-[#14161f] rounded-xl shadow-sm p-12 text-center">
          <Calendar className="h-12 w-12 text-[#c9cfd9] dark:text-[#3a3f54] mx-auto mb-3" />
          <p className="text-[#6b7194] dark:text-[#8b90a8]">
            No calendar feeds yet. Create one to get started.
          </p>
        </div>
      )}
    </div>
  );
};
