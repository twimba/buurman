import { DocumentLanguage, LeaseKind } from '../generated/models';
import {
  DEFAULT_PREVIEW_FORM,
  MAX_TEXT_LENGTH,
  MAX_UI_TENANTS,
} from './leasePreviewForm';
import { LEASE_COUNTRIES } from './leaseCountries';
import type { PreviewForm } from './leasePreviewForm';

const MAX_FREE_LENGTH = MAX_TEXT_LENGTH;
const CONTRACT_TYPES = ['FIXED_TERM', 'INDEFINITE'] as const;

const oneOf = <T extends string>(
  values: readonly T[],
  raw: string | null,
  fallback: T
): T => values.find((v) => v === raw) ?? fallback;

const raw = (value: string | null, fallback: string): string =>
  value === null ? fallback : value.slice(0, MAX_FREE_LENGTH);

const D = DEFAULT_PREVIEW_FORM;
const COUNTRY_CODES = LEASE_COUNTRIES.map((c) => c.code);

/**
 * Reads the shareable preview state. Enumerated fields (country, kind, language, type) fall back
 * to their default when unknown; free-form fields keep whatever was typed so that partial input
 * is never reset. `buildPreviewRequest` reports the invalid ones as field errors.
 */
export const decodePreviewForm = (params: URLSearchParams): PreviewForm => {
  const tenants = params.getAll('tenant').slice(0, MAX_UI_TENANTS);
  return {
    country: oneOf(COUNTRY_CODES, params.get('country'), D.country),
    kind: oneOf(Object.values(LeaseKind), params.get('kind'), D.kind),
    language: oneOf(
      Object.values(DocumentLanguage),
      params.get('lang'),
      D.language
    ),
    contractType: oneOf(CONTRACT_TYPES, params.get('type'), D.contractType),
    startDate: raw(params.get('start'), D.startDate),
    endDate: raw(params.get('end'), D.endDate),
    rentAmount: raw(params.get('rent'), D.rentAmount),
    utilitiesAmount: raw(params.get('utilities'), D.utilitiesAmount),
    currency: raw(params.get('cur'), D.currency),
    depositAmount: raw(params.get('deposit'), D.depositAmount),
    paymentDueDay: raw(params.get('day'), D.paymentDueDay),
    landlordName: raw(params.get('landlord'), D.landlordName),
    tenantNames:
      tenants.length > 0
        ? tenants.map((t) => t.slice(0, MAX_TEXT_LENGTH))
        : D.tenantNames,
    propertyAddress: raw(params.get('address'), D.propertyAddress),
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
