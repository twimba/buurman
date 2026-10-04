import { describe, expect, it } from 'vitest';
import { shouldWriteUrl } from '../leasePreviewUrlSync';

const base = {
  encodedDebounced: 'cur=GBP',
  lastSeenDebounced: '',
  lastWritten: '',
  currentUrlQuery: '',
};

describe('shouldWriteUrl', () => {
  it('writes a new debounced value after normal typing', () => {
    expect(shouldWriteUrl(base)).toBe(true);
  });

  it('does not write when the debounced value is unchanged', () => {
    expect(shouldWriteUrl({ ...base, lastSeenDebounced: 'cur=GBP' })).toBe(
      false
    );
  });

  it('does not write a stale debounced value after explicit navigation', () => {
    // form was reinitialised from ?country=DE, debounced still holds the old value
    expect(
      shouldWriteUrl({
        encodedDebounced: 'cur=GBP',
        lastSeenDebounced: 'cur=GBP',
        lastWritten: 'country=DE',
        currentUrlQuery: 'country=DE',
      })
    ).toBe(false);
  });

  it('does not write once the debounced value converges to the navigated state', () => {
    expect(
      shouldWriteUrl({
        encodedDebounced: 'country=DE',
        lastSeenDebounced: 'cur=GBP',
        lastWritten: 'country=DE',
        currentUrlQuery: 'country=DE',
      })
    ).toBe(false);
  });

  it('does not write when the URL already holds the value (back navigation)', () => {
    expect(shouldWriteUrl({ ...base, currentUrlQuery: 'cur=GBP' })).toBe(false);
  });

  it('writes typing that happens after a navigation', () => {
    expect(
      shouldWriteUrl({
        encodedDebounced: 'country=DE&cur=GBP',
        lastSeenDebounced: 'country=DE',
        lastWritten: 'country=DE',
        currentUrlQuery: 'country=DE',
      })
    ).toBe(true);
  });
});
