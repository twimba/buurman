/**
 * Extract a human-readable message from an Axios/unknown error. Covers the backend's RFC-7807
 * `detail`, plus `message`/`error`, then the Axios error message.
 */
export const errorMessage = (e: unknown): string => {
  const ax = e as {
    response?: { data?: { detail?: string; message?: string; error?: string } };
    message?: string;
  };
  const data = ax?.response?.data;
  return (
    data?.detail ||
    data?.message ||
    data?.error ||
    ax?.message ||
    'Request failed'
  );
};
