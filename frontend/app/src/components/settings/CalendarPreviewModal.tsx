import { useState, useMemo, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Button,
  EmptyState,
  LoadingSpinner,
  ModalWrapper,
  Select,
} from '@buurman/ui';
import {
  AlertTriangle,
  CalendarDays,
  ChevronLeft,
  ChevronRight,
} from 'lucide-react';
import { useIcsFeed } from '../../hooks/useIcsFeed';
import { useAnnounce } from '../../hooks/useAnnounce';
import { MonthCalendar } from './MonthCalendar';
import type { CalendarGridDay } from '../../utils/calendarGrid';
import type { CalendarEvent } from '../../utils/ics';

const getLocalizedMonths = (locale: string) =>
  Array.from({ length: 12 }, (_, i) =>
    new Intl.DateTimeFormat(locale, { month: 'long' }).format(new Date(2000, i))
  );

const currentYear = new Date().getFullYear();
const YEARS = Array.from({ length: 11 }, (_, i) => currentYear - 3 + i);

interface CalendarPreviewModalProps {
  open: boolean;
  onClose: () => void;
  feedUrl: string;
  feedLabel: string;
}

interface SelectedDay {
  key: string;
  label: string;
  events: CalendarEvent[];
}

export const CalendarPreviewModal = ({
  open,
  onClose,
  feedUrl,
  feedLabel,
}: CalendarPreviewModalProps) => {
  const { t, i18n } = useTranslation('settings');
  const announce = useAnnounce();
  const months = useMemo(
    () => getLocalizedMonths(i18n.language),
    [i18n.language]
  );

  const [currentDate, setCurrentDate] = useState(() => new Date());
  const [selectedDay, setSelectedDay] = useState<SelectedDay | null>(null);

  // Switching to another feed without closing should start fresh rather than
  // inherit the month and day the previous preview was left on. Adjusting
  // during render is React's documented alternative to a reset effect.
  const [renderedFeedUrl, setRenderedFeedUrl] = useState(feedUrl);
  if (feedUrl !== renderedFeedUrl) {
    setRenderedFeedUrl(feedUrl);
    setCurrentDate(new Date());
    setSelectedDay(null);
  }

  const { data, eventsByDay, isPending, isError } = useIcsFeed(feedUrl, open);

  const navigateMonths = useCallback((offset: number) => {
    setSelectedDay(null);
    setCurrentDate((previous) => {
      const next = new Date(previous);
      next.setDate(1);
      next.setMonth(next.getMonth() + offset);
      return next;
    });
  }, []);

  const handlePrev = useCallback(() => navigateMonths(-1), [navigateMonths]);
  const handleNext = useCallback(() => navigateMonths(1), [navigateMonths]);

  const handleToday = useCallback(() => {
    setSelectedDay(null);
    setCurrentDate(new Date());
  }, []);

  const handleMonthChange = useCallback(
    (e: React.ChangeEvent<HTMLSelectElement>) => {
      setSelectedDay(null);
      setCurrentDate((previous) => {
        const next = new Date(previous);
        next.setDate(1);
        next.setMonth(Number(e.target.value));
        return next;
      });
    },
    []
  );

  const handleYearChange = useCallback(
    (e: React.ChangeEvent<HTMLSelectElement>) => {
      setSelectedDay(null);
      setCurrentDate((previous) => {
        const next = new Date(previous);
        next.setDate(1);
        next.setFullYear(Number(e.target.value));
        return next;
      });
    },
    []
  );

  const handleSelectDay = useCallback(
    (day: CalendarGridDay, events: CalendarEvent[]) => {
      const label = new Intl.DateTimeFormat(i18n.language, {
        weekday: 'long',
        day: 'numeric',
        month: 'long',
      }).format(day.date);

      setSelectedDay((previous) =>
        previous?.key === day.key ? null : { key: day.key, label, events }
      );
      announce(
        `${label} — ${t('calendarFeeds.preview.dayEventCount', { count: events.length })}`
      );
    },
    [announce, i18n.language, t]
  );

  const isEmptyFeed = !isPending && !isError && (data?.length ?? 0) === 0;

  return (
    <ModalWrapper
      open={open}
      onClose={onClose}
      title={feedLabel}
      subtitle={t('calendarFeeds.preview.subtitle')}
      size="xl"
    >
      {isPending && (
        <div className="flex items-center justify-center py-12">
          <LoadingSpinner />
        </div>
      )}

      {isError && (
        <div className="flex flex-col items-center justify-center gap-3 py-12">
          <AlertTriangle className="h-8 w-8 text-warning-text" />
          <p className="text-center text-sm text-text-secondary">
            {t('calendarFeeds.preview.errorMessage')}
          </p>
        </div>
      )}

      {isEmptyFeed && (
        <EmptyState
          variant="section"
          icon={<CalendarDays />}
          title={t('calendarFeeds.preview.noEvents')}
          description={t('calendarFeeds.preview.noEventsHint')}
        />
      )}

      {!isPending && !isError && !isEmptyFeed && (
        <div>
          <div className="mb-4 flex items-center justify-between">
            <div className="flex items-center gap-1">
              <Button
                variant="ghost"
                size="sm"
                onClick={handlePrev}
                aria-label={t('calendarFeeds.preview.previousMonth')}
                leftIcon={<ChevronLeft />}
              >
                {''}
              </Button>
              <Button
                variant="ghost"
                size="sm"
                onClick={handleNext}
                aria-label={t('calendarFeeds.preview.nextMonth')}
                leftIcon={<ChevronRight />}
              >
                {''}
              </Button>
              <Button variant="secondary" size="sm" onClick={handleToday}>
                {t('calendarFeeds.preview.today')}
              </Button>
            </div>

            <div className="flex items-center gap-2">
              <Select
                size="sm"
                value={currentDate.getMonth()}
                onChange={handleMonthChange}
                aria-label={t('calendarFeeds.preview.month')}
                className="w-auto"
              >
                {months.map((m, i) => (
                  <option key={m} value={i}>
                    {m}
                  </option>
                ))}
              </Select>
              <Select
                size="sm"
                value={currentDate.getFullYear()}
                onChange={handleYearChange}
                aria-label={t('calendarFeeds.preview.year')}
                className="w-auto"
              >
                {YEARS.map((y) => (
                  <option key={y} value={y}>
                    {y}
                  </option>
                ))}
              </Select>
            </div>
          </div>

          <MonthCalendar
            year={currentDate.getFullYear()}
            month={currentDate.getMonth()}
            eventsByDay={eventsByDay}
            selectedDayKey={selectedDay?.key ?? null}
            onSelectDay={handleSelectDay}
          />

          {selectedDay && (
            <div className="mt-4 rounded-lg border border-border-default bg-surface-inset p-3">
              <h3 className="mb-2 text-sm font-semibold capitalize text-text-primary">
                {selectedDay.label}
              </h3>
              <ul className="flex flex-col gap-2">
                {selectedDay.events.map((event) => (
                  <li key={event.uid} className="flex gap-2">
                    <span
                      aria-hidden="true"
                      className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-primary-500"
                    />
                    <div className="min-w-0">
                      <p className="text-sm font-medium text-text-primary">
                        {event.title}
                      </p>
                      {event.description && (
                        <p className="text-xs text-text-secondary">
                          {event.description}
                        </p>
                      )}
                    </div>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}
    </ModalWrapper>
  );
};
