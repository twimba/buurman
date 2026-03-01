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
