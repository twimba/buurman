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

export const useDataDateRange = () => {
  return useQuery({
    queryKey: ['data-date-range'],
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
    queryKey: ['financial-overview', startDate, endDate, propertyIds, currency],
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
    queryKey: ['income-trend', startDate, endDate, propertyIds],
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
    queryKey: ['expense-breakdown', startDate, endDate, propertyIds],
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
    queryKey: ['property-comparison', startDate, endDate, propertyIds],
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
    queryKey: ['occupancy-trend', startDate, endDate],
    queryFn: () => getOccupancyTrend(startDate, endDate),
    enabled,
  });
};

export const useTaxSummary = (year: number, enabled: boolean = true) => {
  return useQuery({
    queryKey: ['tax-summary', year],
    queryFn: () => getTaxSummary(year),
    enabled,
  });
};
