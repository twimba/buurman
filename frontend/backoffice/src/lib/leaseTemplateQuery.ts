export const LEASE_TEMPLATES_KEY_PREFIX = 'lease-clause-templates';

/**
 * One key for the whole view (all countries + language). Every key change is a new query, which
 * is what lets `placeholderData: keepPreviousData` keep the old rows while the new ones load.
 */
export const leaseTemplatesKey = (countries: string[], language: string) =>
  [LEASE_TEMPLATES_KEY_PREFIX, { countries, language }] as const;

export interface MergedTemplates<T> {
  templates: T[];
  failedCountries: string[];
}

/** Merges per-country `Promise.allSettled` results: a failing country does not hide the rest. */
export const mergeSettled = <T>(
  countries: string[],
  results: PromiseSettledResult<T[]>[]
): MergedTemplates<T> => ({
  templates: results.flatMap((r) => (r.status === 'fulfilled' ? r.value : [])),
  failedCountries: countries.filter((_, i) => results[i].status === 'rejected'),
});
