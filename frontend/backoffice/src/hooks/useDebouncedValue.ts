import { useEffect, useState } from 'react';
import { createDebouncer } from '../lib/debounce';

/** Returns `value` after it has stayed unchanged for `delayMs`. */
export const useDebouncedValue = <T>(value: T, delayMs: number): T => {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const d = createDebouncer(setDebounced, delayMs);
    d.call(value);
    return d.cancel;
  }, [value, delayMs]);
  return debounced;
};
