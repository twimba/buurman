import type { MonthlyDataPoint, OccupancyDataPoint } from './property';

export interface PortfolioDashboardResponse {
  summary: PortfolioSummary;
  cashFlow: { months: MonthlyDataPoint[] };
  propertyComparison: PropertyPerformance[];
  allocation: AllocationData;
  occupancy: { months: OccupancyDataPoint[] };
  equityComposition: EquityCompositionData;
  propertiesWithFinancialData: number;
  totalProperties: number;
  currency?: string;
}

export interface PortfolioSummary {
  totalPortfolioValue?: number;
  totalEquity?: number;
  monthlyCashFlow?: number;
  annualNoi?: number;
  weightedCapRate?: number;
  weightedCashOnCash?: number;
  portfolioOccupancy?: number;
  debtToEquity?: number;
  portfolioDscr?: number;
  incomeConcentration?: number;
  dataCompleteness?: number;
}

export interface PropertyPerformance {
  identifier: string;
  address: string;
  category: string;
  monthlyCashFlow?: number;
  annualNoi?: number;
  capRate?: number;
  cashOnCash?: number;
  occupancyRate?: number;
  completenessPercent: number;
  currency?: string;
  currencyMismatch: boolean;
}

export interface AllocationData {
  byCategory: AllocationSlice[];
  byCountry: AllocationSlice[];
}

export interface AllocationSlice {
  label: string;
  value: number;
  percentage: number;
}

export interface EquityCompositionData {
  properties: PropertyEquity[];
}

export interface PropertyEquity {
  identifier: string;
  address: string;
  equity?: number;
  mortgage?: number;
  marketValue?: number;
}
