import { useSyncExternalStore } from 'react';

/** Matches Tailwind's `sm` breakpoint (640px). Below this = mobile. */
const MOBILE_QUERY = '(max-width: 639px)';

const subscribe = (callback: () => void) => {
  const mql = window.matchMedia(MOBILE_QUERY);
  mql.addEventListener('change', callback);
  return () => mql.removeEventListener('change', callback);
};

const getSnapshot = () => window.matchMedia(MOBILE_QUERY).matches;
const getServerSnapshot = () => false;

/**
 * Returns `true` when the viewport is below the `sm` breakpoint (< 640px).
 * Uses `useSyncExternalStore` for tear-free reads.
 */
export const useIsMobile = (): boolean =>
  useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot);
