import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { Copy, Check, X, Link, Calendar } from 'lucide-react';
import { Button, useToast } from '@buurman/ui';
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
  const { t } = useTranslation('common');
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
      case CalendarFeedType.CONTACT_PAYMENTS:
        return f.contactIdentifier === entityIdentifier;
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
        case CalendarFeedType.CONTACT_PAYMENTS:
          request.contactIdentifier = entityIdentifier;
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
      showToast(t('calendarFeed.copied'), 'info');
      setTimeout(() => setCopied(false), 2000);
    } catch {
      showToast(t('calendarFeed.copyFailed'), 'error');
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
        {t('calendarFeed.button')}
      </Button>
      {showPopover && existingFeed && (
        <div className="absolute right-0 top-full mt-2 w-96 bg-surface-card rounded-lg shadow-xl border border-border-default p-4 z-50">
          <div className="flex items-center justify-between mb-3">
            <h4 className="font-medium text-sm text-text-primary">
              {t('calendarFeed.title')}
            </h4>
            <button
              onClick={() => setShowPopover(false)}
              className="p-1 rounded hover:bg-surface-inset dark:hover:bg-surface-raised"
            >
              <X className="h-4 w-4 text-text-muted" />
            </button>
          </div>
          <div className="flex items-center gap-2 mb-3">
            <code className="flex-1 text-xs bg-surface-inset text-text-secondary px-3 py-2 rounded-lg truncate border border-border-default">
              {existingFeed.feedUrl}
            </code>
            <button
              onClick={() => handleCopy(existingFeed.feedUrl)}
              className="flex-shrink-0 p-2 rounded-lg hover:bg-surface-inset dark:hover:bg-surface-raised transition-colors"
            >
              {copied ? (
                <Check className="h-4 w-4 text-success-text" />
              ) : (
                <Copy className="h-4 w-4 text-text-secondary" />
              )}
            </button>
          </div>
          <p className="text-xs text-text-muted">
            {t('calendarFeed.instructions')}
          </p>
          {canEditTeamSettings && (
            <div className="mt-3 pt-3 border-t border-border-default">
              <button
                onClick={() => {
                  setShowPopover(false);
                  navigate('/admin/calendar-feeds');
                }}
                className="text-xs text-primary-500 dark:text-primary-300 hover:underline flex items-center gap-1"
              >
                <Link className="h-3 w-3" />
                {t('calendarFeed.manageFeeds')}
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
