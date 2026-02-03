import { PropertySummary } from './property';

export interface FinancialOverviewRequest {
  startDate: string;
  endDate: string;
  propertyIds?: string[];
  currency?: string;
}

export interface FinancialOverviewResponse {
  period: {
    startDate: string;
    endDate: string;
  };
  income: {
    total: number;
    byProperty: PropertyFinancialSummary[];
  };
  expenses: {
    total: number;
    byCategory: CategoryExpenseSummary[];
    byProperty: PropertyFinancialSummary[];
  };
  netProfit: number;
  currency: string;
}

export interface PropertyFinancialSummary {
  property: PropertySummary;
  income: number;
  expenses: number;
  netProfit: number;
  occupancyDays: number;
}

export interface CategoryExpenseSummary {
  category: string;
  total: number;
  count: number;
  percentage: number;
}

export interface IncomeTrendResponse {
  dataPoints: IncomeTrendDataPoint[];
  currency: string;
}

export interface IncomeTrendDataPoint {
  period: string;
  income: number;
  expenses: number;
  netProfit: number;
}

export interface ExpenseBreakdownResponse {
  categories: ExpenseCategory[];
  total: number;
  currency: string;
}

export interface ExpenseCategory {
  name: string;
  value: number;
  color: string;
}

export interface PropertyComparisonResponse {
  properties: PropertyComparisonData[];
  currency: string;
}

export interface PropertyComparisonData {
  property: PropertySummary;
  income: number;
  expenses: number;
  netProfit: number;
}

export interface OccupancyTrendResponse {
  dataPoints: OccupancyDataPoint[];
}

export interface OccupancyDataPoint {
  period: string;
  occupancyRate: number;
  totalUnits: number;
  occupiedUnits: number;
}

export interface TaxSummaryResponse {
  year: number;
  totalIncome: number;
  totalExpenses: number;
  netIncome: number;
  expensesByCategory: CategoryExpenseSummary[];
  properties: PropertyFinancialSummary[];
  currency: string;
}
