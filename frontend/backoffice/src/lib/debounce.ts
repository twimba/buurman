export interface Timers<H = number> {
  set: (fn: () => void, ms: number) => H;
  clear: (handle: H) => void;
}

const BROWSER_TIMERS: Timers<ReturnType<typeof setTimeout>> = {
  set: (fn, ms) => setTimeout(fn, ms),
  clear: (handle) => clearTimeout(handle),
};

/** Trailing-edge debouncer with injectable timers (for tests). */
export const createDebouncer = <T, H = ReturnType<typeof setTimeout>>(
  fn: (value: T) => void,
  delayMs: number,
  timers: Timers<H> = BROWSER_TIMERS as unknown as Timers<H>
) => {
  let handle: H | null = null;
  let pending: { value: T } | null = null;
  const cancel = () => {
    if (handle !== null) {
      timers.clear(handle);
    }
    handle = null;
    pending = null;
  };
  const flush = () => {
    const p = pending;
    cancel();
    if (p) {
      fn(p.value);
    }
  };
  const call = (value: T) => {
    if (handle !== null) {
      timers.clear(handle);
    }
    pending = { value };
    handle = timers.set(flush, delayMs);
  };
  return { call, cancel, flush };
};
