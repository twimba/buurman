import client from './client';
import {
  FinancialOverviewResponse,
  IncomeTrendResponse,
  ExpenseBreakdownResponse,
  PropertyComparisonResponse,
  OccupancyTrendResponse,
  TaxSummaryResponse,
} from '../types/report';

export const getFinancialOverview = async (
  startDate: string,
  endDate: string,
  propertyIds?: string[],
  currency?: string
): Promise<FinancialOverviewResponse> => {
  const response = await client.get('/reports/financial-overview', {
    params: {
      startDate,
      endDate,
      propertyIds: propertyIds?.join(','),
      currency,
    },
  });
  return response.data;
};

export const getIncomeTrend = async (
  months: number = 12
): Promise<IncomeTrendResponse> => {
  const response = await client.get('/reports/charts/income-trend', {
    params: { months },
  });
  return response.data;
};

export const getExpenseBreakdown = async (
  startDate: string,
  endDate: string
): Promise<ExpenseBreakdownResponse> => {
  const response = await client.get('/reports/charts/expense-breakdown', {
    params: { startDate, endDate },
  });
  return response.data;
};

export const getPropertyComparison = async (
  startDate: string,
  endDate: string
): Promise<PropertyComparisonResponse> => {
  const response = await client.get('/reports/charts/property-comparison', {
    params: { startDate, endDate },
  });
  return response.data;
};

export const getOccupancyTrend = async (
  months: number = 12
): Promise<OccupancyTrendResponse> => {
  const response = await client.get('/reports/charts/occupancy-trend', {
    params: { months },
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
