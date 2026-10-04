import type {
  DocumentLanguage,
  LeaseAgreementPreviewResponse,
  LeaseKind,
} from '../generated/models';
import { LANGUAGE_LABELS } from './leaseClauseTable';
import { LEASE_KIND_META } from './leaseKindMeta';

export interface PreviewWarning {
  id: string;
  severity: 'warning' | 'info' | 'error';
  message: string;
}

export interface PreviewRequested {
  country: string;
  language: DocumentLanguage;
  kind: LeaseKind;
}

/** Notices shown above the preview, derived from what the server actually rendered. */
export const deriveWarnings = (
  requested: PreviewRequested,
  response: LeaseAgreementPreviewResponse
): PreviewWarning[] => {
  if (response.availability.startsWith('UNAVAILABLE')) {
    return [
      {
        id: 'unavailable',
        severity: 'warning',
        message:
          response.availability === 'UNAVAILABLE_NO_COUNTRY'
            ? 'Choose a country to preview its lease agreement.'
            : `No lease agreement is available for ${requested.country} yet, so there is nothing to preview.`,
      },
    ];
  }
  const warnings: PreviewWarning[] = [];
  const kindLabel = LEASE_KIND_META[requested.kind].label;
  if (response.languageUsed && response.languageUsed !== requested.language) {
    warnings.push({
      id: 'language-fallback',
      severity: 'warning',
      message: `No ${LANGUAGE_LABELS[requested.language]} document for ${kindLabel} in ${requested.country}. Rendered in ${LANGUAGE_LABELS[response.languageUsed]} (fallback).`,
    });
  }
  if (response.source === 'EXAMPLE_TEXT') {
    warnings.push({
      id: 'example-text',
      severity: 'warning',
      message:
        'Showing placeholder text: this country has no country document yet.',
    });
  } else if (response.kindUsed && response.kindUsed !== requested.kind) {
    warnings.push({
      id: 'kind-fallback',
      severity: 'warning',
      message: `No ${kindLabel} document for ${requested.country}. Using ${LEASE_KIND_META[response.kindUsed].label} (fallback).`,
    });
  }
  return warnings;
};

/** Friendly message for a failed preview call; includes the ProblemDetail text when present. */
export const previewErrorMessage = (e: unknown): string => {
  const ax = e as {
    response?: { status?: number; data?: { detail?: string } };
    message?: string;
  };
  const status = ax?.response?.status;
  const detail = ax?.response?.data?.detail;
  if (status === 400) {
    return `The sample options are invalid: ${detail ?? 'check the form.'}`;
  }
  if (status === 409) {
    return `This clause selection can't be rendered: ${detail ?? 'a rule was violated.'}`;
  }
  if (status === 403) {
    return 'Only backoffice admins can preview lease agreements.';
  }
  const reason = detail ?? ax?.message;
  return reason
    ? `The preview could not be generated: ${reason}`
    : 'The preview could not be generated.';
};
