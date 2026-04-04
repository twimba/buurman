import { useQuery } from '@tanstack/react-query';
import {
  getDataDateRange,
  getFinancialOverview,
  getIncomeTrend,
  getExpenseBreakdown,
  getPropertyComparison,
  getOccupancyTrend,
  getTaxSummary,
} from '@/api/reports';
import { queryKeys } from '../lib/queryKeys';

export const useDataDateRange = () => {
  return useQuery({
    queryKey: queryKeys.reports.dataDateRange(),
    queryFn: getDataDateRange,
    staleTime: 5 * 60 * 1000,
  });
};

export const useFinancialOverview = (
  startDate: string,
  endDate: string,
  propertyIds?: string[],
  currency?: string,
  enabled: boolean = true
) => {
  return useQuery({
    queryKey: queryKeys.reports.financialOverview(
      startDate,
      endDate,
      propertyIds,
      currency
    ),
    queryFn: () =>
      getFinancialOverview(startDate, endDate, propertyIds, currency),
    enabled,
  });
};

export const useIncomeTrend = (
  startDate?: string,
  endDate?: string,
  propertyIds?: string[],
  enabled: boolean = true
) => {
  return useQuery({
    queryKey: queryKeys.reports.incomeTrend(startDate, endDate, propertyIds),
    queryFn: () => getIncomeTrend(startDate, endDate, 12, propertyIds),
    enabled,
  });
};

export const useExpenseBreakdown = (
  startDate: string,
  endDate: string,
  propertyIds?: string[],
  enabled: boolean = true
) => {
  return useQuery({
    queryKey: queryKeys.reports.expenseBreakdown(
      startDate,
      endDate,
      propertyIds
    ),
    queryFn: () => getExpenseBreakdown(startDate, endDate, propertyIds),
    enabled,
  });
};

export const usePropertyComparison = (
  startDate: string,
  endDate: string,
  propertyIds?: string[],
  enabled: boolean = true
) => {
  return useQuery({
    queryKey: queryKeys.reports.propertyComparison(
      startDate,
      endDate,
      propertyIds
    ),
    queryFn: () => getPropertyComparison(startDate, endDate, propertyIds),
    enabled,
  });
};

export const useOccupancyTrend = (
  startDate?: string,
  endDate?: string,
  enabled: boolean = true
) => {
  return useQuery({
    queryKey: queryKeys.reports.occupancyTrend(startDate, endDate),
    queryFn: () => getOccupancyTrend(startDate, endDate),
    enabled,
  });
};

export const useTaxSummary = (year: number, enabled: boolean = true) => {
  return useQuery({
    queryKey: queryKeys.reports.taxSummary(year),
    queryFn: () => getTaxSummary(year),
    enabled,
  });
};
