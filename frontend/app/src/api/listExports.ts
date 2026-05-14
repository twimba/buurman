import client from './client';

/**
 * CSV / Excel download helpers for the entity-list export endpoints (Properties, Payments,
 * Expenses, Contracts). Google Sheets variants live in `./googleSheetsExport.ts`.
 */

const csv = (path: string) =>
  client.get<Blob>(path, { responseType: 'blob' }).then((r) => r.data);

const xlsx = (path: string) =>
  client.get<Blob>(path, { responseType: 'blob' }).then((r) => r.data);

export const exportPropertiesCsv = () => csv('/booklets/properties/csv');
export const exportPropertiesXlsx = () => xlsx('/booklets/properties/xlsx');

export const exportPaymentsCsv = () => csv('/booklets/payments/csv');
export const exportPaymentsXlsx = () => xlsx('/booklets/payments/xlsx');

export const exportExpensesCsv = () => csv('/booklets/expenses/csv');
export const exportExpensesXlsx = () => xlsx('/booklets/expenses/xlsx');

export const exportContractsCsv = () => csv('/booklets/contracts/csv');
export const exportContractsXlsx = () => xlsx('/booklets/contracts/xlsx');

/** Triggers a browser download of a blob. */
export const downloadBlob = (blob: Blob, filename: string) => {
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  window.URL.revokeObjectURL(url);
};
