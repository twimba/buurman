import { useSyncExternalStore } from 'react';

/** Matches Tailwind's `sm` breakpoint (640px). Below this = mobile. */
const MOBILE_QUERY = '(max-width: 639px)';

// Single MediaQueryList instance shared across all hook consumers
const mql =
  typeof window !== 'undefined' ? window.matchMedia(MOBILE_QUERY) : null;

const subscribe = (callback: () => void) => {
  mql?.addEventListener('change', callback);
  return () => mql?.removeEventListener('change', callback);
};

const getSnapshot = () => mql?.matches ?? false;
const getServerSnapshot = () => false;

/**
 * Returns `true` when the viewport is below the `sm` breakpoint (< 640px).
 * Uses `useSyncExternalStore` for tear-free reads.
 */
export const useIsMobile = (): boolean =>
  useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot);
