import { useQuery } from '@tanstack/react-query';
import {
  getFinancialOverview,
  getIncomeTrend,
  getExpenseBreakdown,
  getPropertyComparison,
  getOccupancyTrend,
  getTaxSummary,
} from '@/api/reports';

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

export const useIncomeTrend = (months: number = 12) => {
  return useQuery({
    queryKey: ['income-trend', months],
    queryFn: () => getIncomeTrend(months),
  });
};

export const useExpenseBreakdown = (
  startDate: string,
  endDate: string,
  enabled: boolean = true
) => {
  return useQuery({
    queryKey: ['expense-breakdown', startDate, endDate],
    queryFn: () => getExpenseBreakdown(startDate, endDate),
    enabled,
  });
};

export const usePropertyComparison = (
  startDate: string,
  endDate: string,
  enabled: boolean = true
) => {
  return useQuery({
    queryKey: ['property-comparison', startDate, endDate],
    queryFn: () => getPropertyComparison(startDate, endDate),
    enabled,
  });
};

export const useOccupancyTrend = (months: number = 12) => {
  return useQuery({
    queryKey: ['occupancy-trend', months],
    queryFn: () => getOccupancyTrend(months),
  });
};

export const useTaxSummary = (year: number, enabled: boolean = true) => {
  return useQuery({
    queryKey: ['tax-summary', year],
    queryFn: () => getTaxSummary(year),
    enabled,
  });
};
