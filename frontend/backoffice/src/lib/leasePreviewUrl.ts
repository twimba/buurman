import { DocumentLanguage, LeaseKind } from '../generated/models';
import {
  DEFAULT_PREVIEW_FORM,
  MAX_TEXT_LENGTH,
  MAX_UI_TENANTS,
  isIsoDate,
} from './leasePreviewForm';
import type { PreviewForm } from './leasePreviewForm';

const CONTRACT_TYPES = ['FIXED_TERM', 'INDEFINITE'] as const;

const oneOf = <T extends string>(
  values: readonly T[],
  raw: string | null,
  fallback: T
): T => values.find((v) => v === raw) ?? fallback;

const str = (raw: string | null, fallback: string): string =>
  raw === null ? fallback : raw.slice(0, MAX_TEXT_LENGTH);

const matching = (
  raw: string | null,
  pattern: RegExp,
  fallback: string
): string => (raw !== null && pattern.test(raw) ? raw : fallback);

const date = (raw: string | null, fallback: string): string =>
  raw !== null && (raw === '' || isIsoDate(raw)) ? raw : fallback;

const day = (raw: string | null, fallback: string): string => {
  if (raw === '') {
    return raw;
  }
  const n = Number(raw);
  return raw !== null && /^\d{1,2}$/.test(raw) && n >= 1 && n <= 31
    ? raw
    : fallback;
};

const AMOUNT = /^(\d{0,15}(\.\d{0,6})?)$/;
const D = DEFAULT_PREVIEW_FORM;

/** Reads the shareable preview state; every missing or invalid value falls back to its default. */
export const decodePreviewForm = (params: URLSearchParams): PreviewForm => {
  const tenants = params.getAll('tenant').slice(0, MAX_UI_TENANTS);
  return {
    country: matching(params.get('country'), /^[A-Z]{2}$/, D.country),
    kind: oneOf(Object.values(LeaseKind), params.get('kind'), D.kind),
    language: oneOf(
      Object.values(DocumentLanguage),
      params.get('lang'),
      D.language
    ),
    contractType: oneOf(CONTRACT_TYPES, params.get('type'), D.contractType),
    startDate: date(params.get('start'), D.startDate),
    endDate: date(params.get('end'), D.endDate),
    rentAmount: matching(params.get('rent'), AMOUNT, D.rentAmount),
    utilitiesAmount: matching(
      params.get('utilities'),
      AMOUNT,
      D.utilitiesAmount
    ),
    currency: matching(params.get('cur'), /^[A-Z]{3}$/, D.currency),
    depositAmount: matching(params.get('deposit'), AMOUNT, D.depositAmount),
    paymentDueDay: day(params.get('day'), D.paymentDueDay),
    landlordName: str(params.get('landlord'), D.landlordName),
    tenantNames:
      tenants.length > 0
        ? tenants.map((t) => t.slice(0, MAX_TEXT_LENGTH))
        : D.tenantNames,
    propertyAddress: str(params.get('address'), D.propertyAddress),
  };
};

const SCALARS: [string, Exclude<keyof PreviewForm, 'tenantNames'>][] = [
  ['country', 'country'],
  ['kind', 'kind'],
  ['lang', 'language'],
  ['type', 'contractType'],
  ['start', 'startDate'],
  ['end', 'endDate'],
  ['rent', 'rentAmount'],
  ['utilities', 'utilitiesAmount'],
  ['cur', 'currency'],
  ['deposit', 'depositAmount'],
  ['day', 'paymentDueDay'],
  ['landlord', 'landlordName'],
  ['address', 'propertyAddress'],
];

/** Writes only the values that differ from the defaults. */
export const encodePreviewForm = (form: PreviewForm): URLSearchParams => {
  const params = new URLSearchParams();
  SCALARS.forEach(([param, key]) => {
    if (form[key] !== D[key]) {
      params.set(param, form[key]);
    }
  });
  if (JSON.stringify(form.tenantNames) !== JSON.stringify(D.tenantNames)) {
    form.tenantNames.forEach((t) => params.append('tenant', t));
  }
  return params;
};

export const PREVIEW_PATH = '/lease-clause-templates/preview';

/** Link into the preview with country, kind and language pre-selected. */
export const previewLink = (
  country?: string,
  kind?: LeaseKind,
  language?: DocumentLanguage
): string => {
  const form = decodePreviewForm(new URLSearchParams());
  const query = encodePreviewForm({
    ...form,
    country: country ?? form.country,
    kind: kind ?? form.kind,
    language: language ?? form.language,
  }).toString();
  return query ? `${PREVIEW_PATH}?${query}` : PREVIEW_PATH;
};
