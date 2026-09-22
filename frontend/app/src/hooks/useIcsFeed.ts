import { useQuery } from '@tanstack/react-query';
import { useMemo } from 'react';
import { queryKeys } from '../lib/queryKeys';
import { groupEventsByDay, parseIcsFeed } from '../utils/ics';

/**
 * Loads and parses a Buurman iCalendar feed.
 *
 * The feed is served from the app's own origin (`/calendar/ical/{token}`) and
 * authenticated by the token in the URL, so it is fetched directly with no
 * credentials attached.
 */
export const useIcsFeed = (feedUrl: string, enabled: boolean) => {
  const query = useQuery({
    queryKey: queryKeys.calendarFeeds.preview(feedUrl),
    enabled: enabled && !!feedUrl,
    // Feeds change at most daily; avoid refetching while the user pages months.
    staleTime: 5 * 60 * 1000,
    retry: 1,
    queryFn: async ({ signal }) => {
      const response = await fetch(feedUrl, {
        signal,
        headers: { Accept: 'text/calendar' },
      });
      if (!response.ok) {
        throw new Error(`Feed request failed with status ${response.status}`);
      }
      return parseIcsFeed(await response.text());
    },
  });

  const eventsByDay = useMemo(
    () => groupEventsByDay(query.data ?? []),
    [query.data]
  );

  return { ...query, eventsByDay };
};
