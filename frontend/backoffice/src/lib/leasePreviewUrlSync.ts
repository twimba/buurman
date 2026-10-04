export interface UrlWriteInput {
  /** The debounced form, encoded. */
  encodedDebounced: string;
  /** The debounced value the last time the write decision ran. */
  lastSeenDebounced: string;
  /** The last query the page wrote or initialised the form from. */
  lastWritten: string;
  currentUrlQuery: string;
}

/**
 * Whether the debounced form should be written to the URL: only when it changed since the last
 * decision and differs from what the page already wrote or the URL already holds. A stale
 * debounced value after explicit navigation therefore never reverts the URL.
 */
export const shouldWriteUrl = ({
  encodedDebounced,
  lastSeenDebounced,
  lastWritten,
  currentUrlQuery,
}: UrlWriteInput): boolean =>
  encodedDebounced !== lastSeenDebounced &&
  encodedDebounced !== lastWritten &&
  encodedDebounced !== currentUrlQuery;
