import { useState, useMemo, useCallback, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import FullCalendar from '@fullcalendar/react';
import dayGridPlugin from '@fullcalendar/daygrid';
import iCalendarPlugin from '@fullcalendar/icalendar';
import { Button, LoadingSpinner, ModalWrapper, Select } from '@buurman/ui';
import { AlertTriangle, ChevronLeft, ChevronRight } from 'lucide-react';
import './fullcalendar-theme.css';

const MONTHS = [
  'January',
  'February',
  'March',
  'April',
  'May',
  'June',
  'July',
  'August',
  'September',
  'October',
  'November',
  'December',
];

const currentYear = new Date().getFullYear();
const YEARS = Array.from({ length: 11 }, (_, i) => currentYear - 3 + i);

interface CalendarPreviewModalProps {
  open: boolean;
  onClose: () => void;
  feedUrl: string;
  feedLabel: string;
}

export const CalendarPreviewModal = ({
  open,
  onClose,
  feedUrl,
  feedLabel,
}: CalendarPreviewModalProps) => {
  const { t } = useTranslation('settings');
  const calendarRef = useRef<FullCalendar>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [currentDate, setCurrentDate] = useState(new Date());

  const eventSource = useMemo(
    () => ({ url: feedUrl, format: 'ics' as const }),
    [feedUrl]
  );

  const handleLoading = useCallback((loading: boolean) => {
    setIsLoading(loading);
    if (loading) {
      setError(null);
    }
  }, []);

  const handleError = useCallback(() => {
    setError(t('calendarFeeds.preview.errorMessage'));
    setIsLoading(false);
  }, [t]);

  const navigateTo = useCallback((date: Date) => {
    const api = calendarRef.current?.getApi();
    if (api) {
      api.gotoDate(date);
      setCurrentDate(date);
    }
  }, []);

  const handlePrev = useCallback(() => {
    const api = calendarRef.current?.getApi();
    if (api) {
      api.prev();
      setCurrentDate(api.getDate());
    }
  }, []);

  const handleNext = useCallback(() => {
    const api = calendarRef.current?.getApi();
    if (api) {
      api.next();
      setCurrentDate(api.getDate());
    }
  }, []);

  const handleToday = useCallback(() => {
    const api = calendarRef.current?.getApi();
    if (api) {
      api.today();
      setCurrentDate(api.getDate());
    }
  }, []);

  const handleMonthChange = useCallback(
    (e: React.ChangeEvent<HTMLSelectElement>) => {
      const newDate = new Date(currentDate);
      newDate.setMonth(Number(e.target.value));
      navigateTo(newDate);
    },
    [currentDate, navigateTo]
  );

  const handleYearChange = useCallback(
    (e: React.ChangeEvent<HTMLSelectElement>) => {
      const newDate = new Date(currentDate);
      newDate.setFullYear(Number(e.target.value));
      navigateTo(newDate);
    },
    [currentDate, navigateTo]
  );

  const isHidden = isLoading || !!error;

  return (
    <ModalWrapper
      open={open}
      onClose={onClose}
      title={feedLabel}
      subtitle={t('calendarFeeds.preview.subtitle')}
      size="xl"
    >
      {isLoading && !error && (
        <div className="flex items-center justify-center py-12">
          <LoadingSpinner />
        </div>
      )}

      {error && (
        <div className="flex flex-col items-center justify-center py-12 gap-3">
          <AlertTriangle className="h-8 w-8 text-warning-text" />
          <p className="text-sm text-text-secondary text-center">{error}</p>
        </div>
      )}

      <div
        className="fc-buurman"
        style={{
          visibility: isHidden ? 'hidden' : 'visible',
          height: isHidden ? 0 : 'auto',
          overflow: 'hidden',
        }}
      >
        {/* Custom toolbar */}
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-1">
            <Button
              variant="ghost"
              size="sm"
              onClick={handlePrev}
              leftIcon={<ChevronLeft />}
            >
              {''}
            </Button>
            <Button
              variant="ghost"
              size="sm"
              onClick={handleNext}
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
              className="w-auto"
            >
              {MONTHS.map((m, i) => (
                <option key={m} value={i}>
                  {m}
                </option>
              ))}
            </Select>
            <Select
              size="sm"
              value={currentDate.getFullYear()}
              onChange={handleYearChange}
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

        <FullCalendar
          ref={calendarRef}
          plugins={[dayGridPlugin, iCalendarPlugin]}
          initialView="dayGridMonth"
          events={eventSource}
          loading={handleLoading}
          eventSourceFailure={handleError}
          height="auto"
          headerToolbar={false}
          dayMaxEvents={3}
          firstDay={1}
          fixedWeekCount={false}
        />
      </div>
    </ModalWrapper>
  );
};
