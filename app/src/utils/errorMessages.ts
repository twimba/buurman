import { AxiosError } from 'axios';

interface ApiError {
  message?: string;
  error?: string;
  errors?: Record<string, string[]>;
}

export const getErrorMessage = (error: unknown): string => {
  if (error instanceof AxiosError) {
    const data = error.response?.data as ApiError;

    // Check for validation errors
    if (data?.errors) {
      const errorMessages = Object.entries(data.errors)
        .map(([field, messages]) => {
          const fieldName =
            field.charAt(0).toUpperCase() +
            field.slice(1).replace(/([A-Z])/g, ' $1');
          return `${fieldName}: ${messages.join(', ')}`;
        })
        .join('. ');
      return errorMessages || 'Please check your input and try again.';
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
