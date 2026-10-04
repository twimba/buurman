import { describe, expect, it } from 'vitest';
import { deriveWarnings, previewErrorMessage } from '../leasePreviewWarnings';
import type { LeaseAgreementPreviewResponse } from '../../generated/models';

const response = (
  patch: Partial<LeaseAgreementPreviewResponse> = {}
): LeaseAgreementPreviewResponse => ({
  availability: 'AVAILABLE_DOCUMENT',
  html: '<p/>',
  languageUsed: 'nl',
  kindUsed: 'RESIDENTIAL',
  source: 'DOCUMENT',
  clauses: [],
  ...patch,
});
const req = { country: 'NL', language: 'nl', kind: 'RESIDENTIAL' } as const;

describe('deriveWarnings', () => {
  it('has none for an exact document', () => {
    expect(deriveWarnings(req, response())).toEqual([]);
  });

  it('flags a language fallback', () => {
    const w = deriveWarnings(req, response({ languageUsed: 'en' }));
    expect(w).toHaveLength(1);
    expect(w[0].severity).toBe('warning');
    expect(w[0].message).toMatch(/Nederlands/);
    expect(w[0].message).toMatch(/English/);
  });

  it('flags a kind fallback', () => {
    const w = deriveWarnings(
      { ...req, kind: 'RESIDENTIAL_FURNISHED' },
      response()
    );
    expect(w[0].message).toMatch(/Residential furnished/);
  });

  it('flags example text', () => {
    const w = deriveWarnings(
      req,
      response({ source: 'EXAMPLE_TEXT', kindUsed: 'LEGACY' })
    );
    expect(w.map((x) => x.id)).toContain('example-text');
    expect(w.find((x) => x.id === 'example-text')?.message).toMatch(
      /no country document yet/
    );
  });

  it('reports unavailable countries without a document', () => {
    const w = deriveWarnings(
      req,
      response({
        availability: 'UNAVAILABLE_COUNTRY',
        html: null,
        languageUsed: null,
        kindUsed: null,
        source: null,
      })
    );
    expect(w).toHaveLength(1);
    expect(w[0].id).toBe('unavailable');
    expect(w[0].message).toMatch(/NL/);
    const none = deriveWarnings(
      req,
      response({ availability: 'UNAVAILABLE_NO_COUNTRY', html: null })
    );
    expect(none[0].id).toBe('unavailable');
  });
});

describe('previewErrorMessage', () => {
  const err = (status: number, detail?: string) => ({
    response: { status, data: detail ? { detail } : {} },
  });

  it('maps statuses to friendly messages with the detail', () => {
    expect(previewErrorMessage(err(400, 'amount must be positive'))).toMatch(
      /amount must be positive/
    );
    expect(previewErrorMessage(err(409, 'All clauses excluded'))).toMatch(
      /All clauses excluded/
    );
    expect(previewErrorMessage(err(403))).toMatch(/admin/i);
    expect(previewErrorMessage(err(500))).toMatch(/preview/i);
    expect(previewErrorMessage(new Error('boom'))).toMatch(/boom/);
  });
});
