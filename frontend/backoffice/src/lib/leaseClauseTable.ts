import { DocumentLanguage } from '../generated/models';
import type {
  LeaseClauseTemplateResponse,
  LeaseKind,
} from '../generated/models';
import { LEASE_KIND_ORDER } from './leaseKindMeta';

export const LANGUAGE_LABELS: Record<DocumentLanguage, string> = {
  en: 'English',
  nl: 'Nederlands',
  de: 'Deutsch',
  fr: 'Français',
  pt: 'Português',
  es: 'Español',
  sv: 'Svenska',
  it: 'Italiano',
  fi: 'Suomi',
  el: 'Ελληνικά',
  pl: 'Polski',
  da: 'Dansk',
  nb: 'Norsk bokmål',
};

export const LANGUAGES = Object.values(DocumentLanguage);

export type BehaviourFilter = 'all' | 'optional' | 'pinned' | 'required';
export type BehaviourFlag = 'required' | 'pinned' | 'defaultOff';

export interface TableFilters {
  country: string;
  kind: LeaseKind | 'all';
  behaviour: BehaviourFilter;
  problemsOnly: boolean;
}

export const DEFAULT_FILTERS: TableFilters = {
  country: 'all',
  kind: 'all',
  behaviour: 'all',
  problemsOnly: false,
};

/** The backend returns the key itself when it is defined in no bundle. */
export const isUnresolved = (text: string, key: string): boolean =>
  text === key;

export const isEnglishFallback = (
  template: LeaseClauseTemplateResponse,
  language: DocumentLanguage
): boolean =>
  language !== DocumentLanguage.en &&
  template.missingLanguages.includes(language);

export const hasUnresolvedKey = (t: LeaseClauseTemplateResponse): boolean =>
  isUnresolved(t.titleText, t.titleI18nKey) ||
  isUnresolved(t.bodyText, t.bodyI18nKey);

export const hasProblem = (
  template: LeaseClauseTemplateResponse,
  language: DocumentLanguage
): boolean =>
  isEnglishFallback(template, language) || hasUnresolvedKey(template);

export const behaviourFlags = (
  t: LeaseClauseTemplateResponse
): BehaviourFlag[] => {
  const flags: BehaviourFlag[] = [];
  if (!t.optional) {
    flags.push('required');
  }
  if (t.pinned) {
    flags.push('pinned');
  }
  if (t.optional && !t.defaultIncluded) {
    flags.push('defaultOff');
  }
  return flags;
};

const matchesBehaviour = (
  t: LeaseClauseTemplateResponse,
  behaviour: BehaviourFilter
): boolean => {
  switch (behaviour) {
    case 'optional':
      return t.optional;
    case 'required':
      return !t.optional;
    case 'pinned':
      return t.pinned;
    default:
      return true;
  }
};

export const applyFilters = (
  templates: LeaseClauseTemplateResponse[],
  filters: TableFilters,
  language: DocumentLanguage
): LeaseClauseTemplateResponse[] =>
  templates.filter(
    (t) =>
      (filters.country === 'all' || t.countryCode === filters.country) &&
      (filters.kind === 'all' || t.leaseKind === filters.kind) &&
      matchesBehaviour(t, filters.behaviour) &&
      (!filters.problemsOnly || hasProblem(t, language))
  );

export interface KindGroup {
  kind: LeaseKind;
  rows: LeaseClauseTemplateResponse[];
}

export interface CountryGroup {
  countryCode: string;
  count: number;
  kinds: KindGroup[];
}

/** Countries A-Z, kinds in enum order, rows by sortOrder (the order the lease shows). */
export const groupTemplates = (
  templates: LeaseClauseTemplateResponse[]
): CountryGroup[] => {
  const countries = Array.from(
    new Set(templates.map((t) => t.countryCode))
  ).sort();
  return countries.map((countryCode) => {
    const inCountry = templates.filter((t) => t.countryCode === countryCode);
    const kinds = LEASE_KIND_ORDER.map((kind) => ({
      kind,
      rows: inCountry
        .filter((t) => t.leaseKind === kind)
        .sort((a, b) => a.sortOrder - b.sortOrder),
    })).filter((g) => g.rows.length > 0);
    return { countryCode, count: inCountry.length, kinds };
  });
};
