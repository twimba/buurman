import client from './client';

/**
 * Result of a successful Google Sheets export. Matches `GoogleSheetExportResponse` in the OpenAPI
 * spec.
 */
export interface GoogleSheetExportResult {
  spreadsheetId: string;
  url: string;
}

interface GoogleSheetExportRequestBody {
  accessToken: string;
}

/**
 * Per-entity export: backend hits Google Drive (folder lookup + title check + move) plus Sheets
 * (create + values batchUpdate + formatting batchUpdate) — 5-7 sequential Google API calls.
 * Each can take 0.5-2s, so we override the default 15s axios timeout to a generous 60s.
 */
const ENTITY_EXPORT_TIMEOUT_MS = 60_000;

/**
 * Takeout export: same flow but with 22 tabs (one per entity) plus a row dump per table —
 * comfortably the heaviest export we offer. 3 min upper bound.
 */
const TAKEOUT_EXPORT_TIMEOUT_MS = 180_000;

export const exportContactsGoogleSheet = async (
  accessToken: string
): Promise<GoogleSheetExportResult> => {
  const body: GoogleSheetExportRequestBody = { accessToken };
  const response = await client.post<GoogleSheetExportResult>(
    '/booklets/contacts/google-sheet',
    body,
    { timeout: ENTITY_EXPORT_TIMEOUT_MS }
  );
  return response.data;
};

export const exportTransactionsGoogleSheet = async (
  accessToken: string,
  startDate?: string,
  endDate?: string
): Promise<GoogleSheetExportResult> => {
  const body: GoogleSheetExportRequestBody = { accessToken };
  const response = await client.post<GoogleSheetExportResult>(
    '/reports/export/transactions/google-sheet',
    body,
    { params: { startDate, endDate }, timeout: ENTITY_EXPORT_TIMEOUT_MS }
  );
  return response.data;
};

export const exportPortfolioDashboardGoogleSheet = async (
  accessToken: string,
  options?: { months?: number; startDate?: string; endDate?: string }
): Promise<GoogleSheetExportResult> => {
  const body: GoogleSheetExportRequestBody = { accessToken };
  const response = await client.post<GoogleSheetExportResult>(
    '/portfolio/dashboard/export/google-sheet',
    body,
    { params: options, timeout: ENTITY_EXPORT_TIMEOUT_MS }
  );
  return response.data;
};

export const exportPropertyDashboardGoogleSheet = async (
  propertyIdentifier: string,
  accessToken: string,
  months?: number
): Promise<GoogleSheetExportResult> => {
  const body: GoogleSheetExportRequestBody = { accessToken };
  const response = await client.post<GoogleSheetExportResult>(
    `/properties/${propertyIdentifier}/dashboard/export/google-sheet`,
    body,
    { params: { months }, timeout: ENTITY_EXPORT_TIMEOUT_MS }
  );
  return response.data;
};

export const exportTakeoutGoogleSheet = async (
  accessToken: string
): Promise<GoogleSheetExportResult> => {
  const body: GoogleSheetExportRequestBody = { accessToken };
  const response = await client.post<GoogleSheetExportResult>(
    '/takeouts/google-sheet',
    body,
    { timeout: TAKEOUT_EXPORT_TIMEOUT_MS }
  );
  return response.data;
};

export const exportPropertiesGoogleSheet = async (
  accessToken: string
): Promise<GoogleSheetExportResult> => {
  const body: GoogleSheetExportRequestBody = { accessToken };
  const response = await client.post<GoogleSheetExportResult>(
    '/booklets/properties/google-sheet',
    body,
    { timeout: ENTITY_EXPORT_TIMEOUT_MS }
  );
  return response.data;
};

export const exportPaymentsGoogleSheet = async (
  accessToken: string
): Promise<GoogleSheetExportResult> => {
  const body: GoogleSheetExportRequestBody = { accessToken };
  const response = await client.post<GoogleSheetExportResult>(
    '/booklets/payments/google-sheet',
    body,
    { timeout: ENTITY_EXPORT_TIMEOUT_MS }
  );
  return response.data;
};

export const exportExpensesGoogleSheet = async (
  accessToken: string
): Promise<GoogleSheetExportResult> => {
  const body: GoogleSheetExportRequestBody = { accessToken };
  const response = await client.post<GoogleSheetExportResult>(
    '/booklets/expenses/google-sheet',
    body,
    { timeout: ENTITY_EXPORT_TIMEOUT_MS }
  );
  return response.data;
};

export const exportContractsGoogleSheet = async (
  accessToken: string
): Promise<GoogleSheetExportResult> => {
  const body: GoogleSheetExportRequestBody = { accessToken };
  const response = await client.post<GoogleSheetExportResult>(
    '/booklets/contracts/google-sheet',
    body,
    { timeout: ENTITY_EXPORT_TIMEOUT_MS }
  );
  return response.data;
};
