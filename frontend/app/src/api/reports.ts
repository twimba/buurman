import client from './client';
import type {
  FinancialOverviewResponse,
  IncomeTrendResponse,
  ExpenseBreakdownResponse,
  PropertyComparisonResponse,
  OccupancyTrendResponse,
  TaxSummaryResponse,
} from '../generated/models';

export interface DataDateRangeResponse {
  earliestDate?: string;
}

export const getDataDateRange = async (): Promise<DataDateRangeResponse> => {
  const response = await client.get('/reports/date-range');
  return response.data;
};

export const getFinancialOverview = async (
  startDate: string,
  endDate: string,
  propertyIdentifiers?: string[],
  currency?: string
): Promise<FinancialOverviewResponse> => {
  const response = await client.get('/reports/financial-overview', {
    params: {
      startDate,
      endDate,
      propertyIdentifiers: propertyIdentifiers?.join(','),
      currency,
    },
  });
  return response.data;
};

export const getIncomeTrend = async (
  startDate?: string,
  endDate?: string,
  months: number = 12,
  propertyIdentifiers?: string[]
): Promise<IncomeTrendResponse> => {
  const response = await client.get('/reports/charts/income-trend', {
    params: {
      ...(startDate && endDate ? { startDate, endDate } : { months }),
      propertyIdentifiers: propertyIdentifiers?.join(','),
    },
  });
  return response.data;
};

export const getExpenseBreakdown = async (
  startDate: string,
  endDate: string,
  propertyIdentifiers?: string[]
): Promise<ExpenseBreakdownResponse> => {
  const response = await client.get('/reports/charts/expense-breakdown', {
    params: {
      startDate,
      endDate,
      propertyIdentifiers: propertyIdentifiers?.join(','),
    },
  });
  return response.data;
};

export const getPropertyComparison = async (
  startDate: string,
  endDate: string,
  propertyIdentifiers?: string[]
): Promise<PropertyComparisonResponse> => {
  const response = await client.get('/reports/charts/property-comparison', {
    params: {
      startDate,
      endDate,
      propertyIdentifiers: propertyIdentifiers?.join(','),
    },
  });
  return response.data;
};

export const getOccupancyTrend = async (
  startDate?: string,
  endDate?: string,
  months: number = 12
): Promise<OccupancyTrendResponse> => {
  const response = await client.get('/reports/charts/occupancy-trend', {
    params: startDate && endDate ? { startDate, endDate } : { months },
  });
  return response.data;
};

export const getTaxSummary = async (
  year: number
): Promise<TaxSummaryResponse> => {
  const response = await client.get('/reports/tax-summary', {
    params: { year },
  });
  return response.data;
};

export const exportTransactionsCSV = async (
  startDate?: string,
  endDate?: string
): Promise<Blob> => {
  const response = await client.get('/reports/export/transactions/csv', {
    params: { startDate, endDate },
    responseType: 'blob',
  });
  return response.data;
};

export const exportTransactionsPDF = async (
  startDate?: string,
  endDate?: string
): Promise<Blob> => {
  const response = await client.get('/reports/export/transactions/pdf', {
    params: { startDate, endDate },
    responseType: 'blob',
  });
  return response.data;
};

export const exportTransactionsExcel = async (
  startDate?: string,
  endDate?: string
): Promise<Blob> => {
  const response = await client.get('/reports/export/transactions/excel', {
    params: { startDate, endDate },
    responseType: 'blob',
  });
  return response.data;
};
