import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import type { CalendarEvent } from '../../utils/ics';
import {
  buildMonthGrid,
  getWeekdayLabels,
  type CalendarGridDay,
} from '../../utils/calendarGrid';

/** Events shown inline in a day cell before collapsing into a "+N more" affordance. */
const MAX_VISIBLE_EVENTS = 3;

interface MonthCalendarProps {
  year: number;
  month: number;
  eventsByDay: Map<string, CalendarEvent[]>;
  selectedDayKey: string | null;
  onSelectDay: (day: CalendarGridDay, events: CalendarEvent[]) => void;
}

interface DayCellProps {
  day: CalendarGridDay;
  events: CalendarEvent[];
  isSelected: boolean;
  longDateLabel: string;
  onSelect: () => void;
}

const DayCell = ({
  day,
  events,
  isSelected,
  longDateLabel,
  onSelect,
}: DayCellProps) => {
  const { t } = useTranslation('settings');
  const hasEvents = events.length > 0;
  const hidden = events.length - MAX_VISIBLE_EVENTS;

  const dayNumber = (
    <span
      className={[
        'text-[0.8125rem] leading-none',
        day.isToday
          ? 'font-bold text-primary-700'
          : day.inCurrentMonth
            ? 'font-medium text-text-primary'
            : 'font-medium text-text-disabled',
      ].join(' ')}
    >
      {day.date.getDate()}
    </span>
  );

  const body = (
    <>
      <span className="flex justify-end px-2 pt-1.5">{dayNumber}</span>
      <span className="flex flex-col gap-0.5 px-1 pb-1">
        {events.slice(0, MAX_VISIBLE_EVENTS).map((event) => (
          <span
            key={`${event.uid}-${day.key}`}
            className="truncate rounded bg-primary-500 px-1 py-px text-left text-[0.6875rem] font-medium text-white"
            title={event.title}
          >
            {event.title}
          </span>
        ))}
        {hidden > 0 && (
          <span className="px-1 text-left text-[0.6875rem] font-semibold text-primary-600">
            {t('calendarFeeds.preview.moreEvents', { count: hidden })}
          </span>
        )}
      </span>
    </>
  );

  const shared = [
    'flex min-h-[5.25rem] w-full flex-col text-left align-top transition-colors',
    day.isToday ? 'bg-primary-50/60' : '',
    day.inCurrentMonth ? '' : 'bg-surface-inset/40',
  ].join(' ');

  return (
    <td
      className="border-b border-r border-border-default p-0 last:border-r-0"
      aria-current={day.isToday ? 'date' : undefined}
    >
      {hasEvents ? (
        <button
          type="button"
          onClick={onSelect}
          aria-pressed={isSelected}
          aria-label={`${longDateLabel} — ${t('calendarFeeds.preview.dayEventCount', {
            count: events.length,
          })}`}
          className={[
            shared,
            'cursor-pointer hover:bg-surface-inset focus:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-primary-500',
            isSelected ? 'ring-2 ring-inset ring-primary-500' : '',
          ].join(' ')}
        >
          {body}
        </button>
      ) : (
        <div className={shared} aria-label={longDateLabel}>
          {body}
        </div>
      )}
    </td>
  );
};

/**
 * Read-only month grid for Buurman calendar feeds.
 *
 * Rendered as a real table so screen readers get row/column semantics for free;
 * days carrying events are buttons, so the grid is fully keyboard navigable.
 */
export const MonthCalendar = ({
  year,
  month,
  eventsByDay,
  selectedDayKey,
  onSelectDay,
}: MonthCalendarProps) => {
  const { i18n } = useTranslation('settings');
  const locale = i18n.language;

  const weeks = useMemo(() => buildMonthGrid(year, month), [year, month]);
  const weekdays = useMemo(() => getWeekdayLabels(locale), [locale]);
  const weekdaysLong = useMemo(() => getWeekdayLabels(locale, 'long'), [locale]);
  const longDate = useMemo(
    () =>
      new Intl.DateTimeFormat(locale, {
        weekday: 'long',
        day: 'numeric',
        month: 'long',
        year: 'numeric',
      }),
    [locale]
  );

  return (
    <table className="w-full table-fixed border-collapse overflow-hidden rounded-lg border border-border-default">
      <thead>
        <tr className="bg-surface-inset">
          {weekdays.map((label, index) => (
            <th
              key={label}
              scope="col"
              className="border-b border-r border-border-default py-2 text-[0.6875rem] font-semibold uppercase tracking-wider text-text-secondary last:border-r-0"
            >
              <abbr title={weekdaysLong[index]} className="no-underline">
                {label}
              </abbr>
            </th>
          ))}
        </tr>
      </thead>
      <tbody>
        {weeks.map((week) => (
          <tr key={week[0].key} className="last:[&>td]:border-b-0">
            {week.map((day) => {
              const events = eventsByDay.get(day.key) ?? [];
              return (
                <DayCell
                  key={day.key}
                  day={day}
                  events={events}
                  isSelected={selectedDayKey === day.key}
                  longDateLabel={longDate.format(day.date)}
                  onSelect={() => onSelectDay(day, events)}
                />
              );
            })}
          </tr>
        ))}
      </tbody>
    </table>
  );
};
