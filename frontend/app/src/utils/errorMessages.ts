import { AxiosError } from 'axios';
import type { TFunction } from 'i18next';

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

const FIELD_LABEL_KEYS: Record<string, string> = {
  firstName: 'fieldLabels.firstName',
  lastName: 'fieldLabels.lastName',
  email: 'fieldLabels.email',
  phone: 'fieldLabels.phone',
  taxNumber: 'fieldLabels.taxNumber',
  idNumber: 'fieldLabels.idNumber',
  rentAmount: 'fieldLabels.rentAmount',
  depositAmount: 'fieldLabels.depositAmount',
  securityDeposit: 'fieldLabels.securityDeposit',
  startDate: 'fieldLabels.startDate',
  endDate: 'fieldLabels.endDate',
  signedDate: 'fieldLabels.signedDate',
  paymentDueDay: 'fieldLabels.paymentDueDay',
  contractType: 'fieldLabels.contractType',
  paymentFrequency: 'fieldLabels.paymentFrequency',
  propertyIdentifier: 'fieldLabels.property',
  currency: 'fieldLabels.currency',
  rentAmountCurrency: 'fieldLabels.rentAmountCurrency',
  depositAmountCurrency: 'fieldLabels.depositAmountCurrency',
  securityDepositCurrency: 'fieldLabels.securityDepositCurrency',
};

function formatFieldName(field: string, t?: TFunction): string {
  // Handle nested paths like "parties[0].newContact.phone" -> "Phone"
  const lastPart = field.includes('.')
    ? (field.split('.').pop() ?? field)
    : field;
  // Strip array indices like "parties[0]" -> "parties"
  const clean = lastPart.replace(/\[\d+\]/g, '');

  if (t && FIELD_LABEL_KEYS[clean]) {
    return t(FIELD_LABEL_KEYS[clean]);
  }

  return (
    FIELD_LABELS[clean] ||
    clean.charAt(0).toUpperCase() + clean.slice(1).replace(/([A-Z])/g, ' $1')
  );
}

export const getErrorMessage = (error: unknown, t?: TFunction): string => {
  if (error instanceof AxiosError) {
    const data = error.response?.data as ApiError;

    // Check for ProblemDetail fieldErrors (Spring Boot validation)
    if (data?.fieldErrors && Object.keys(data.fieldErrors).length > 0) {
      const messages = Object.entries(data.fieldErrors).map(
        ([field, message]) => `${formatFieldName(field, t)}: ${message}`
      );
      return messages.join('\n');
    }

    // Check for validation errors (legacy format)
    if (data?.errors) {
      const errorMessages = Object.entries(data.errors)
        .map(([field, messages]) => {
          return `${formatFieldName(field, t)}: ${messages.join(', ')}`;
        })
        .join('. ');
      return (
        errorMessages ||
        (t
          ? t('errors.invalidInput')
          : 'Please check your input and try again.')
      );
    }

    // Check for ProblemDetail format (RFC 7807)
    if (data?.detail) {
      return formatErrorMessage(data.detail, t);
    }

    // Check for general error message
    if (data?.message) {
      return formatErrorMessage(data.message, t);
    }

    if (data?.error) {
      return formatErrorMessage(data.error, t);
    }

    // Handle HTTP status codes
    switch (error.response?.status) {
      case 400:
        return t
          ? t('errors.badRequest')
          : 'Invalid request. Please check your input and try again.';
      case 401:
        return t
          ? t('errors.unauthorized')
          : 'You are not authorized. Please log in again.';
      case 403:
        return t
          ? t('errors.forbidden')
          : 'You do not have permission to perform this action.';
      case 404:
        return t
          ? t('errors.notFound')
          : 'The requested resource was not found.';
      case 409:
        return t
          ? t('errors.conflict')
          : 'This action conflicts with existing data. Please check and try again.';
      case 422:
        return t
          ? t('errors.invalidData')
          : 'The data provided is invalid. Please check your input.';
      case 500:
        return t
          ? t('errors.serverError')
          : 'A server error occurred. Please try again later.';
      default:
        return t
          ? t('errors.generic')
          : 'An unexpected error occurred. Please try again.';
    }
  }

  if (error instanceof Error) {
    return error.message;
  }

  return t
    ? t('errors.generic')
    : 'An unexpected error occurred. Please try again.';
};

const ERROR_MAPPING_KEYS: Record<string, string> = {
  'duplicate key': 'errors.duplicateRecord',
  'foreign key': 'errors.linkedRecord',
  'not found': 'errors.notFound',
  'already exists': 'errors.alreadyExists',
  'invalid format': 'errors.invalidFormat',
  'required field': 'errors.requiredFields',
  'constraint violation': 'errors.constraintViolation',
};

const ERROR_MAPPINGS: Record<string, string> = {
  'duplicate key': 'This record already exists.',
  'foreign key':
    'This action cannot be completed because it is linked to other records.',
  'not found': 'The requested item was not found.',
  'already exists': 'A record with this information already exists.',
  'invalid format': 'The format of the provided data is invalid.',
  'required field': 'Please fill in all required fields.',
  'constraint violation': 'This action violates data constraints.',
};

const formatErrorMessage = (message: string, t?: TFunction): string => {
  const lowerMessage = message.toLowerCase();
  for (const [key, i18nKey] of Object.entries(ERROR_MAPPING_KEYS)) {
    if (lowerMessage.includes(key)) {
      if (t) {
        return t(i18nKey);
      }
      return ERROR_MAPPINGS[key];
    }
  }

  // Return a generic message for unmapped backend errors to avoid leaking internals
  return t
    ? t('errors.genericWithSupport')
    : 'An error occurred. Please try again or contact support.';
};
