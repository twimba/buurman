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

    // 409 CONFLICT = BusinessRuleException: always user-facing, translate or show as-is
    if (error.response?.status === 409 && data?.detail) {
      return formatBusinessRuleError(data.detail, t);
    }

    // 400 BAD REQUEST with detail = application validation error, always user-facing
    if (error.response?.status === 400 && data?.detail) {
      return data.detail;
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

// Maps exact BusinessRuleException messages (backend) to i18n keys.
// All 409 CONFLICT responses come from BusinessRuleException and are user-facing by design.
const BUSINESS_RULE_KEYS: Record<string, string> = {
  'Extensions can only be created for fixed-term contracts':
    'errors.businessRules.extensionsFixedTermOnly',
  'Extensions can only be created for active contracts':
    'errors.businessRules.extensionsActiveOnly',
  'Contract already has a pending draft extension':
    'errors.businessRules.extensionPendingDraft',
  'Contract already has a pending or active extension':
    'errors.businessRules.extensionPendingOrActive',
  'Contract already has an active extension':
    'errors.businessRules.extensionAlreadyActive',
  'Cannot extend indefinite contract':
    'errors.businessRules.cannotExtendIndefinite',
  'Contact confirmation required before activation':
    'errors.businessRules.contactConfirmationRequired',
  'Maximum number of renewals reached':
    'errors.businessRules.maxRenewalsReached',
  'Can only generate payments for ACTIVE contracts':
    'errors.businessRules.paymentsActiveContractOnly',
  'Payment is already marked as paid':
    'errors.businessRules.paymentAlreadyPaid',
  'Payment is already fully paid':
    'errors.businessRules.paymentAlreadyFullyPaid',
  'Cannot mark a cancelled payment as paid':
    'errors.businessRules.cannotMarkCancelledPaid',
  'Cannot register receival on a cancelled payment':
    'errors.businessRules.cannotReceiveCancelled',
  'Can only edit rent periods that have not yet taken effect':
    'errors.businessRules.cannotEditActivePeriod',
  'Effective date cannot be before the contract start date':
    'errors.businessRules.effectiveDateBeforeStart',
  'Effective date cannot be after the contract end date':
    'errors.businessRules.effectiveDateAfterEnd',
  'End date cannot be before start date':
    'errors.businessRules.endDateBeforeStart',
  'Amenity already linked to this property':
    'errors.businessRules.amenityAlreadyLinked',
  'A user with this email or username already exists':
    'errors.businessRules.userAlreadyExists',
  'User already member of this team': 'errors.businessRules.userAlreadyMember',
  'Cannot remove yourself': 'errors.businessRules.cannotRemoveYourself',
  'Cannot change your own role': 'errors.businessRules.cannotChangeOwnRole',
  'Cannot transfer ownership to yourself':
    'errors.businessRules.cannotTransferToSelf',
  'Cannot leave team you own. Transfer ownership first.':
    'errors.businessRules.cannotLeaveOwnedTeam',
  'Cannot send an invalid invitation': 'errors.businessRules.invalidInvitation',
  'Invalid invitation code': 'errors.businessRules.invalidInvitationCode',
  'Invitation already accepted': 'errors.businessRules.invitationAccepted',
  'Invitation code is no longer valid':
    'errors.businessRules.invitationExpiredCode',
  'Invitation does not belong to this team':
    'errors.businessRules.invitationWrongTeam',
  'Invitation email does not match':
    'errors.businessRules.invitationEmailMismatch',
  'Invitation expired': 'errors.businessRules.invitationExpired',
  'Invitation is already revoked': 'errors.businessRules.invitationRevoked',
  'Email sending is not configured': 'errors.businessRules.emailNotConfigured',
  'SMS sending is not configured': 'errors.businessRules.smsNotConfigured',
};

const formatBusinessRuleError = (message: string, t?: TFunction): string => {
  if (t) {
    const key = BUSINESS_RULE_KEYS[message];
    if (key) {
      return t(key);
    }
  }
  return formatErrorMessage(message, t);
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
