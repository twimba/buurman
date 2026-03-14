import { AxiosError } from 'axios';

interface ApiError {
  detail?: string;
  message?: string;
  error?: string;
  errors?: Record<string, string[]>;
  fieldErrors?: Record<string, string>;
}

const FIELD_LABELS: Record<string, string> = {
  firstName: 'First name',
  lastName: 'Last name',
  email: 'Email',
  phone: 'Phone',
  taxNumber: 'Tax number',
  idNumber: 'ID number',
  rentAmount: 'Rent amount',
  depositAmount: 'Deposit amount',
  securityDeposit: 'Security deposit',
  startDate: 'Start date',
  endDate: 'End date',
  signedDate: 'Signed date',
  paymentDueDay: 'Payment due day',
  contractType: 'Contract type',
  paymentFrequency: 'Payment frequency',
  propertyIdentifier: 'Property',
  currency: 'Currency',
  rentAmountCurrency: 'Rent currency',
  depositAmountCurrency: 'Deposit currency',
  securityDepositCurrency: 'Security deposit currency',
};

function formatFieldName(field: string): string {
  // Handle nested paths like "parties[0].newTenant.phone" → "Phone"
  const lastPart = field.includes('.')
    ? (field.split('.').pop() ?? field)
    : field;
  // Strip array indices like "parties[0]" → "parties"
  const clean = lastPart.replace(/\[\d+\]/g, '');
  return (
    FIELD_LABELS[clean] ||
    clean.charAt(0).toUpperCase() + clean.slice(1).replace(/([A-Z])/g, ' $1')
  );
}

export const getErrorMessage = (error: unknown): string => {
  if (error instanceof AxiosError) {
    const data = error.response?.data as ApiError;

    // Check for ProblemDetail fieldErrors (Spring Boot validation)
    if (data?.fieldErrors && Object.keys(data.fieldErrors).length > 0) {
      const messages = Object.entries(data.fieldErrors).map(
        ([field, message]) => `${formatFieldName(field)}: ${message}`
      );
      return messages.join('\n');
    }

    // Check for validation errors (legacy format)
    if (data?.errors) {
      const errorMessages = Object.entries(data.errors)
        .map(([field, messages]) => {
          return `${formatFieldName(field)}: ${messages.join(', ')}`;
        })
        .join('. ');
      return errorMessages || 'Please check your input and try again.';
    }

    // Check for ProblemDetail format (RFC 7807)
    if (data?.detail) {
      return formatErrorMessage(data.detail);
    }

    // Check for general error message
    if (data?.message) {
      return formatErrorMessage(data.message);
    }

    if (data?.error) {
      return formatErrorMessage(data.error);
    }

    // Handle HTTP status codes
    switch (error.response?.status) {
      case 400:
        return 'Invalid request. Please check your input and try again.';
      case 401:
        return 'You are not authorized. Please log in again.';
      case 403:
        return 'You do not have permission to perform this action.';
      case 404:
        return 'The requested resource was not found.';
      case 409:
        return 'This action conflicts with existing data. Please check and try again.';
      case 422:
        return 'The data provided is invalid. Please check your input.';
      case 500:
        return 'A server error occurred. Please try again later.';
      default:
        return 'An unexpected error occurred. Please try again.';
    }
  }

  if (error instanceof Error) {
    return error.message;
  }

  return 'An unexpected error occurred. Please try again.';
};

const formatErrorMessage = (message: string): string => {
  // Convert technical error messages to user-friendly ones
  const errorMappings: Record<string, string> = {
    'duplicate key': 'This record already exists.',
    'foreign key':
      'This action cannot be completed because it is linked to other records.',
    'not found': 'The requested item was not found.',
    'already exists': 'A record with this information already exists.',
    'invalid format': 'The format of the provided data is invalid.',
    'required field': 'Please fill in all required fields.',
    'constraint violation': 'This action violates data constraints.',
  };

  const lowerMessage = message.toLowerCase();
  for (const [key, value] of Object.entries(errorMappings)) {
    if (lowerMessage.includes(key)) {
      return value;
    }
  }

  // Return the original message if no mapping found
  return message;
};
