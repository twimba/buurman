import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Copy, Check, X, Link, Calendar } from 'lucide-react';
import { Button } from '../ui';
import { useToast } from '../../context/ToastContext';
import {
  useCalendarFeeds,
  useCreateCalendarFeed,
} from '../../hooks/useCalendarFeedHooks';
import {
  CalendarFeedResponseFeedType as CalendarFeedType,
  type CreateCalendarFeedRequest,
} from '../../generated/models';
import { useTeam } from '../../context/TeamContext';

interface CalendarFeedPopoverProps {
  feedType: CalendarFeedType;
  entityIdentifier: string;
}

export const CalendarFeedButton = ({
  feedType,
  entityIdentifier,
}: CalendarFeedPopoverProps) => {
  const navigate = useNavigate();
  const { showToast } = useToast();
  const { canEditTeamSettings } = useTeam();
  const [showPopover, setShowPopover] = useState(false);
  const [copied, setCopied] = useState(false);
  const { data: feeds = [] } = useCalendarFeeds();
  const createMutation = useCreateCalendarFeed();

  const existingFeed = feeds.find((f) => {
    if (f.feedType !== feedType) {
      return false;
    }
    switch (feedType) {
      case CalendarFeedType.CONTRACT:
        return f.contractIdentifier === entityIdentifier;
      case CalendarFeedType.PROPERTY_PAYMENTS:
        return f.propertyIdentifier === entityIdentifier;
      case CalendarFeedType.TENANT_PAYMENTS:
        return f.tenantIdentifier === entityIdentifier;
      default:
        return false;
    }
  });

  const handleClick = () => {
    if (existingFeed) {
      setShowPopover(true);
    } else {
      const request: CreateCalendarFeedRequest = { feedType };
      switch (feedType) {
        case CalendarFeedType.CONTRACT:
          request.contractIdentifier = entityIdentifier;
          break;
        case CalendarFeedType.PROPERTY_PAYMENTS:
          request.propertyIdentifier = entityIdentifier;
          break;
        case CalendarFeedType.TENANT_PAYMENTS:
          request.tenantIdentifier = entityIdentifier;
          break;
      }
      createMutation.mutate(request, {
        onSuccess: () => setShowPopover(true),
      });
    }
  };

  const handleCopy = async (url: string) => {
    try {
      await navigator.clipboard.writeText(url);
      setCopied(true);
      showToast('Calendar feed URL copied', 'info');
      setTimeout(() => setCopied(false), 2000);
    } catch {
      showToast('Failed to copy link', 'error');
    }
  };

  return (
    <div className="relative">
      <Button
        variant="secondary"
        leftIcon={<Calendar />}
        onClick={handleClick}
        isLoading={createMutation.isPending}
      >
        Calendar Feed
      </Button>
      {showPopover && existingFeed && (
        <div className="absolute right-0 top-full mt-2 w-96 bg-white dark:bg-[#1a1d28] rounded-xl shadow-xl border border-[#e2e6f0] dark:border-[#2a2e3f] p-4 z-50">
          <div className="flex items-center justify-between mb-3">
            <h4 className="font-medium text-sm text-[#1a1d2e] dark:text-[#eef0f6]">
              Calendar Subscription URL
            </h4>
            <button
              onClick={() => setShowPopover(false)}
              className="p-1 rounded hover:bg-[#f1f3f9] dark:hover:bg-[#262a3a]"
            >
              <X className="h-4 w-4 text-[#9ca0b8]" />
            </button>
          </div>
          <div className="flex items-center gap-2 mb-3">
            <code className="flex-1 text-xs bg-[#f1f3f9] dark:bg-[#14161f] text-[#3d4463] dark:text-[#c4c8db] px-3 py-2 rounded-lg truncate border border-[#e2e6f0] dark:border-[#2a2e3f]">
              {existingFeed.feedUrl}
            </code>
            <button
              onClick={() => handleCopy(existingFeed.feedUrl)}
              className="flex-shrink-0 p-2 rounded-lg hover:bg-[#f1f3f9] dark:hover:bg-[#262a3a] transition-colors"
            >
              {copied ? (
                <Check className="h-4 w-4 text-green-500" />
              ) : (
                <Copy className="h-4 w-4 text-[#6b7194]" />
              )}
            </button>
          </div>
          <p className="text-xs text-[#9ca0b8] dark:text-[#5c6180]">
            Add this URL to Google Calendar (Settings &gt; Add calendar &gt;
            From URL) or Apple Calendar (File &gt; New Calendar Subscription).
          </p>
          {canEditTeamSettings && (
            <div className="mt-3 pt-3 border-t border-[#e2e6f0] dark:border-[#2a2e3f]">
              <button
                onClick={() => {
                  setShowPopover(false);
                  navigate('/admin/calendar-feeds');
                }}
                className="text-xs text-primary-500 dark:text-primary-300 hover:underline flex items-center gap-1"
              >
                <Link className="h-3 w-3" />
                Manage all calendar feeds in Settings
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
