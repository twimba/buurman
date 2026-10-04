import { describe, expect, it } from 'vitest';
import { createDebouncer } from '../debounce';

const fakeTimers = () => {
  let next = 1;
  const pending = new Map<number, () => void>();
  return {
    timers: {
      set: (fn: () => void) => {
        pending.set(next, fn);
        return next++;
      },
      clear: (id: number) => {
        pending.delete(id);
      },
    },
    fire: () => {
      [...pending.values()].forEach((fn) => fn());
      pending.clear();
    },
    size: () => pending.size,
  };
};

describe('createDebouncer', () => {
  it('only delivers the last value after the delay', () => {
    const t = fakeTimers();
    const seen: number[] = [];
    const d = createDebouncer((v: number) => seen.push(v), 400, t.timers);
    d.call(1);
    d.call(2);
    d.call(3);
    expect(t.size()).toBe(1);
    expect(seen).toEqual([]);
    t.fire();
    expect(seen).toEqual([3]);
  });

  it('cancel drops the pending value', () => {
    const t = fakeTimers();
    const seen: number[] = [];
    const d = createDebouncer((v: number) => seen.push(v), 400, t.timers);
    d.call(1);
    d.cancel();
    t.fire();
    expect(seen).toEqual([]);
  });

  it('flush delivers immediately once', () => {
    const t = fakeTimers();
    const seen: number[] = [];
    const d = createDebouncer((v: number) => seen.push(v), 400, t.timers);
    d.call(7);
    d.flush();
    d.flush();
    t.fire();
    expect(seen).toEqual([7]);
  });
});
