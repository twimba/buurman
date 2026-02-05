export interface ApiResponse<T> {
  data: T;
  message?: string;
}

export interface ErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  numberOfElements: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface PageParams {
  page?: number;
  size?: number;
  sort?: string;
  direction?: 'asc' | 'desc';
}

export interface PaymentStatsResponse {
  pendingCount: number;
  pendingAmount: number;
  overdueCount: number;
  overdueAmount: number;
  currency: string;
  monthlyTrend: MonthlyTrend[];
}

export interface ExpenseStatsResponse {
  totalAmount: number;
  currency: string;
  topCategories: CategoryTotal[];
  monthlyTrend: MonthlyTrend[];
}

export interface MonthlyTrend {
  month: string;
  total: number;
}

export interface CategoryTotal {
  category: string;
  total: number;
  count: number;
}
