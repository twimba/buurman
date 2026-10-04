import { DocumentLanguage, LeaseKind } from '../generated/models';
import type {
  LeaseAgreementPreviewRequest,
  LeasePreviewClauseChoice,
  LeasePreviewContractType,
  LeasePreviewRentComponent,
} from '../generated/models';

// Mirrors the validation limits of LeasePreviewService on the backend.
export const MAX_CLAUSES = 100;
export const MAX_RENT_COMPONENTS = 20;
export const MAX_TENANTS = 10;
export const MAX_UI_TENANTS = 3;
export const MAX_TEXT_LENGTH = 200;
export const MAX_AMOUNT = 1_000_000_000;
export const MIN_DATE = '1900-01-01';
export const MAX_DATE = '2200-01-01';

export interface PreviewForm {
  country: string;
  kind: LeaseKind;
  language: DocumentLanguage;
  contractType: LeasePreviewContractType;
  startDate: string;
  endDate: string;
  rentAmount: string;
  /** Optional second rent component (utilities advance); empty = none. */
  utilitiesAmount: string;
  currency: string;
  /** Empty = no deposit. */
  depositAmount: string;
  /** Empty = backend default. */
  paymentDueDay: string;
  landlordName: string;
  tenantNames: string[];
  propertyAddress: string;
}

export const DEFAULT_PREVIEW_FORM: PreviewForm = {
  country: 'NL',
  kind: LeaseKind.RESIDENTIAL,
  language: DocumentLanguage.en,
  contractType: 'FIXED_TERM',
  startDate: '2026-01-01',
  endDate: '2026-12-31',
  rentAmount: '1200',
  utilitiesAmount: '',
  currency: 'EUR',
  depositAmount: '',
  paymentDueDay: '1',
  landlordName: 'Sample Landlord B.V.',
  tenantNames: ['Sample Tenant'],
  propertyAddress: 'Keizersgracht 123, 1015 CJ Amsterdam',
};

export type BuildResult =
  | { ok: true; request: LeaseAgreementPreviewRequest }
  | { ok: false; errors: string[] };

const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/;

export const isIsoDate = (value: string): boolean => {
  if (!ISO_DATE.test(value)) {
    return false;
  }
  const d = new Date(`${value}T00:00:00Z`);
  return !Number.isNaN(d.getTime()) && d.toISOString().slice(0, 10) === value;
};

const parseAmount = (raw: string): number | null => {
  const text = raw.trim();
  if (!/^\d+(\.\d+)?$/.test(text)) {
    return null;
  }
  const n = Number(text);
  return Number.isFinite(n) && n > 0 && n <= MAX_AMOUNT ? n : null;
};

const text = (label: string, value: string, errors: string[]): string => {
  const trimmed = value.trim();
  if (trimmed.length === 0 || trimmed.length > MAX_TEXT_LENGTH) {
    errors.push(`${label} must be 1-${MAX_TEXT_LENGTH} characters.`);
  }
  return trimmed;
};

const amount = (
  label: string,
  raw: string,
  errors: string[]
): number | null => {
  const n = parseAmount(raw);
  if (n === null) {
    errors.push(`${label} must be a positive amount up to ${MAX_AMOUNT}.`);
  }
  return n;
};

const checkDates = (form: PreviewForm, errors: string[]): void => {
  const inRange = (d: string) => d >= MIN_DATE && d < MAX_DATE;
  if (!isIsoDate(form.startDate) || !inRange(form.startDate)) {
    errors.push('Start date must be a valid date between 1900 and 2199.');
    return;
  }
  if (form.contractType !== 'FIXED_TERM') {
    return;
  }
  if (!isIsoDate(form.endDate) || !inRange(form.endDate)) {
    errors.push('A fixed-term contract needs a valid end date.');
  } else if (form.endDate <= form.startDate) {
    errors.push('End date must be after the start date.');
  }
};

const checkTenants = (form: PreviewForm, errors: string[]): string[] => {
  if (form.tenantNames.length < 1 || form.tenantNames.length > MAX_TENANTS) {
    errors.push(`Between 1 and ${MAX_TENANTS} tenants are required.`);
  }
  return form.tenantNames.map((t) => text('Tenant name', t, errors));
};

/** Validates the form like the backend does and builds the preview request. */
export const buildPreviewRequest = (
  form: PreviewForm,
  choices: LeasePreviewClauseChoice[] | null
): BuildResult => {
  const errors: string[] = [];
  checkDates(form, errors);
  const landlordName = text('Landlord name', form.landlordName, errors);
  const propertyAddress = text(
    'Property address',
    form.propertyAddress,
    errors
  );
  const tenantNames = checkTenants(form, errors);

  if (!/^[A-Z]{3}$/.test(form.currency)) {
    errors.push('Currency must be a 3-letter upper-case code, e.g. EUR.');
  }
  const rent = amount('Base rent', form.rentAmount, errors);
  const rentComponents: LeasePreviewRentComponent[] = [];
  if (rent !== null) {
    rentComponents.push({
      type: 'BASE_RENT',
      amount: rent,
      currency: form.currency,
    });
  }
  if (form.utilitiesAmount.trim() !== '') {
    const utilities = amount('Utilities advance', form.utilitiesAmount, errors);
    if (utilities !== null) {
      rentComponents.push({
        type: 'UTILITIES_ADVANCE',
        amount: utilities,
        currency: form.currency,
      });
    }
  }
  let deposit: { amount: number; currency: string } | undefined;
  if (form.depositAmount.trim() !== '') {
    const value = amount('Deposit', form.depositAmount, errors);
    deposit =
      value === null ? undefined : { amount: value, currency: form.currency };
  }

  let paymentDueDay: number | undefined;
  if (form.paymentDueDay.trim() !== '') {
    const day = Number(form.paymentDueDay);
    if (!Number.isInteger(day) || day < 1 || day > 31) {
      errors.push('Payment day must be a whole number from 1 to 31.');
    } else {
      paymentDueDay = day;
    }
  }
  if ((choices?.length ?? 0) > MAX_CLAUSES) {
    errors.push(`At most ${MAX_CLAUSES} clauses are allowed.`);
  }
  if (errors.length > 0) {
    return { ok: false, errors };
  }
  return {
    ok: true,
    request: {
      countryCode: form.country,
      leaseKind: form.kind,
      language: form.language,
      sample: {
        contractType: form.contractType,
        startDate: form.startDate,
        endDate: form.contractType === 'FIXED_TERM' ? form.endDate : undefined,
        rentComponents,
        deposit,
        paymentDueDay,
        paymentFrequency: 'MONTHLY',
        landlordName,
        tenantNames,
        propertyAddress,
      },
      clauses: choices ?? undefined,
    },
  };
};
