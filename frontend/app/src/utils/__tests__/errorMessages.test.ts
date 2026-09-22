import { AxiosError, AxiosHeaders } from 'axios';
import type { TFunction } from 'i18next';
import { getErrorMessage } from '../errorMessages';

function createAxiosError(status: number, data?: unknown): AxiosError {
  const error = new AxiosError(
    'Request failed',
    'ERR_BAD_REQUEST',
    undefined,
    undefined,
    {
      status,
      data,
      statusText: '',
      headers: {},
      config: { headers: new AxiosHeaders() },
    }
  );
  return error;
}

describe('getErrorMessage', () => {
  describe('ProblemDetail fieldErrors (Spring Boot validation)', () => {
    it('returns formatted field errors with known labels', () => {
      const error = createAxiosError(422, {
        fieldErrors: {
          firstName: 'must not be blank',
          email: 'must be a valid email',
        },
      });
      const result = getErrorMessage(error);
      expect(result).toContain('First name: must not be blank');
      expect(result).toContain('Email: must be a valid email');
    });

    it('returns formatted field errors with unknown field names', () => {
      const error = createAxiosError(422, {
        fieldErrors: {
          customField: 'is required',
        },
      });
      const result = getErrorMessage(error);
      // camelCase split keeps each word's capital: "Custom Field"
      expect(result).toBe('Custom Field: is required');
    });

    it('ignores empty fieldErrors and falls through', () => {
      const error = createAxiosError(400, {
        fieldErrors: {},
        detail: 'Some detail',
      });
      const result = getErrorMessage(error);
      // Should fall through to detail since fieldErrors is empty
      expect(result).not.toContain('fieldErrors');
    });
  });

  describe('legacy validation errors format', () => {
    it('returns formatted legacy errors with multiple messages per field', () => {
      const error = createAxiosError(422, {
        errors: {
          rentAmount: ['must be positive', 'must be less than 999999'],
          startDate: ['must be in the future'],
        },
      });
      const result = getErrorMessage(error);
      expect(result).toContain(
        'Rent amount: must be positive, must be less than 999999'
      );
      expect(result).toContain('Start date: must be in the future');
    });

    it('returns fallback when errors object is empty', () => {
      const error = createAxiosError(422, {
        errors: {},
      });
      const result = getErrorMessage(error);
      expect(result).toBe('Please check your input and try again.');
    });
  });

  describe('ProblemDetail format (RFC 7807)', () => {
    // detail is shown verbatim by design (see #702): the backend owns the
    // wording, so the client must not second-guess or rewrite it.
    it('returns detail verbatim', () => {
      const error = createAxiosError(404, {
        detail: 'Property not found with identifier xyz',
      });
      expect(getErrorMessage(error)).toBe(
        'Property not found with identifier xyz'
      );
    });

    it('returns detail verbatim regardless of status', () => {
      const error = createAxiosError(500, {
        detail: 'Rent cannot be scheduled before the contract starts',
      });
      expect(getErrorMessage(error)).toBe(
        'Rent cannot be scheduled before the contract starts'
      );
    });

    it('translates a known business rule detail on 409 when t is supplied', () => {
      const error = createAxiosError(409, {
        detail: 'Extensions can only be created for fixed-term contracts',
      });
      const t = ((key: string) => `translated:${key}`) as unknown as TFunction;
      expect(getErrorMessage(error, t)).toBe(
        'translated:errors.businessRules.extensionsFixedTermOnly'
      );
    });

    it('falls back to the raw detail on 409 when the rule is unknown', () => {
      const error = createAxiosError(409, {
        detail: 'duplicate key value violates unique constraint',
      });
      const t = ((key: string) => `translated:${key}`) as unknown as TFunction;
      expect(getErrorMessage(error, t)).toBe(
        'duplicate key value violates unique constraint'
      );
    });
  });

  describe('message and error string fields', () => {
    it('maps message field through formatErrorMessage', () => {
      const error = createAxiosError(409, {
        message: 'A record with this email already exists',
      });
      expect(getErrorMessage(error)).toBe(
        'A record with this information already exists.'
      );
    });

    it('maps error field through formatErrorMessage', () => {
      const error = createAxiosError(400, {
        error: 'foreign key constraint violated',
      });
      expect(getErrorMessage(error)).toBe(
        'This action cannot be completed because it is linked to other records.'
      );
    });

    it('prefers fieldErrors over detail', () => {
      const error = createAxiosError(422, {
        fieldErrors: { email: 'invalid' },
        detail: 'Validation failed',
      });
      expect(getErrorMessage(error)).toBe('Email: invalid');
    });

    it('prefers detail over message, and returns it verbatim', () => {
      const error = createAxiosError(400, {
        detail: 'Something already exists here',
        message: 'Bad request',
      });
      expect(getErrorMessage(error)).toBe('Something already exists here');
    });
  });

  describe('HTTP status code fallbacks', () => {
    it('returns message for 400', () => {
      const error = createAxiosError(400, {});
      expect(getErrorMessage(error)).toBe(
        'Invalid request. Please check your input and try again.'
      );
    });

    it('returns message for 401', () => {
      const error = createAxiosError(401, {});
      expect(getErrorMessage(error)).toBe(
        'You are not authorized. Please log in again.'
      );
    });

    it('returns message for 403', () => {
      const error = createAxiosError(403, {});
      expect(getErrorMessage(error)).toBe(
        'You do not have permission to perform this action.'
      );
    });

    it('returns message for 404', () => {
      const error = createAxiosError(404, {});
      expect(getErrorMessage(error)).toBe(
        'The requested resource was not found.'
      );
    });

    it('returns message for 409', () => {
      const error = createAxiosError(409, {});
      expect(getErrorMessage(error)).toBe(
        'This action conflicts with existing data. Please check and try again.'
      );
    });

    it('returns message for 422', () => {
      const error = createAxiosError(422, {});
      expect(getErrorMessage(error)).toBe(
        'The data provided is invalid. Please check your input.'
      );
    });

    it('returns message for 500', () => {
      const error = createAxiosError(500, {});
      expect(getErrorMessage(error)).toBe(
        'A server error occurred. Please try again later.'
      );
    });

    it('returns generic message for unknown status codes', () => {
      const error = createAxiosError(418, {});
      expect(getErrorMessage(error)).toBe(
        'An unexpected error occurred. Please try again.'
      );
    });
  });

  describe('non-Axios errors', () => {
    it('returns message from plain Error', () => {
      const error = new Error('Network timeout');
      expect(getErrorMessage(error)).toBe('Network timeout');
    });

    it('returns generic message for string errors', () => {
      expect(getErrorMessage('something broke')).toBe(
        'An unexpected error occurred. Please try again.'
      );
    });

    it('returns generic message for null', () => {
      expect(getErrorMessage(null)).toBe(
        'An unexpected error occurred. Please try again.'
      );
    });

    it('returns generic message for undefined', () => {
      expect(getErrorMessage(undefined)).toBe(
        'An unexpected error occurred. Please try again.'
      );
    });
  });
});

describe('formatFieldName (tested indirectly via getErrorMessage)', () => {
  it('handles nested dot paths by using the last segment', () => {
    const error = createAxiosError(422, {
      fieldErrors: {
        'parties[0].newContact.phone': 'is required',
      },
    });
    expect(getErrorMessage(error)).toBe('Phone: is required');
  });

  it('strips array indices from field names', () => {
    const error = createAxiosError(422, {
      fieldErrors: {
        'parties[0]': 'invalid party',
      },
    });
    // "parties" is unknown, so it gets camelCase-to-space formatting
    expect(getErrorMessage(error)).toBe('Parties: invalid party');
  });

  it('applies camelCase-to-space conversion for unknown fields', () => {
    const error = createAxiosError(422, {
      fieldErrors: {
        myCustomFieldName: 'bad value',
      },
    });
    // camelCase split preserves capitals: "My Custom Field Name"
    expect(getErrorMessage(error)).toBe('My Custom Field Name: bad value');
  });

  it('looks up known labels from FIELD_LABELS', () => {
    const error = createAxiosError(422, {
      fieldErrors: {
        securityDeposit: 'must be positive',
        paymentFrequency: 'is required',
      },
    });
    const result = getErrorMessage(error);
    expect(result).toContain('Security deposit: must be positive');
    expect(result).toContain('Payment frequency: is required');
  });
});
