// Response types — re-exported from generated (these are simple aggregation types)
export type {
  FinancialOverviewResponse,
  CategoryExpenseSummary,
  PropertyFinancialSummary,
  IncomeTrendResponse,
  ExpenseBreakdownResponse,
  PropertyComparisonResponse,
  OccupancyTrendResponse,
  TaxSummaryResponse,
} from '../generated/models';

export type { Category as ExpenseCategory } from '../generated/models';
export type { PropertyData as PropertyComparisonData } from '../generated/models';
export type { DataPoint as OccupancyDataPoint } from '../generated/models';

// Frontend-only query params
export interface FinancialOverviewRequest {
  startDate: string;
  endDate: string;
  propertyIdentifiers?: string[];
  currency?: string;
}

// Manual — generated DataPoint has occupancy-specific fields, not income fields
export interface IncomeTrendDataPoint {
  period: string;
  income: number;
  expenses: number;
  netProfit: number;
}
