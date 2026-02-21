import { PropertySummary, DocumentResponse } from './property';

export enum ExpenseCategory {
  MAINTENANCE = 'MAINTENANCE',
  REPAIR = 'REPAIR',
  UTILITY = 'UTILITY',
  TAX = 'TAX',
  INSURANCE = 'INSURANCE',
  LEGAL = 'LEGAL',
  MARKETING = 'MARKETING',
  CLEANING = 'CLEANING',
  LANDSCAPING = 'LANDSCAPING',
  PROPERTY_MANAGEMENT = 'PROPERTY_MANAGEMENT',
  FEES = 'FEES',
  PROPERTY_TAX = 'PROPERTY_TAX',
  OTHER = 'OTHER',
}

export interface ExpenseResponse {
  identifier: string;
  property: PropertySummary;
  category: ExpenseCategory;
  amount: number;
  currency: string;
  expenseDate: string;
  description: string;
  notes?: string;
  documents: DocumentResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateExpenseRequest {
  propertyIdentifier: string;
  category: ExpenseCategory;
  amount: number;
  currency?: string;
  expenseDate: string;
  description: string;
  notes?: string;
}

export interface UpdateExpenseRequest {
  category?: ExpenseCategory;
  amount?: number;
  currency?: string;
  expenseDate?: string;
  description?: string;
  notes?: string;
}

export interface ExpenseSummaryResponse {
  period: string;
  byCategory: CategoryTotal[];
  grandTotal: number;
  currency: string;
}

export interface CategoryTotal {
  category: ExpenseCategory;
  total: number;
  count: number;
}

export interface GetExpensesParams {
  category?: ExpenseCategory;
  propertyIdentifier?: string;
}

export const formatExpenseCategory = (category: ExpenseCategory): string => {
  const categoryLabels: Record<ExpenseCategory, string> = {
    [ExpenseCategory.MAINTENANCE]: 'Maintenance',
    [ExpenseCategory.REPAIR]: 'Repair',
    [ExpenseCategory.UTILITY]: 'Utility',
    [ExpenseCategory.TAX]: 'Tax',
    [ExpenseCategory.INSURANCE]: 'Insurance',
    [ExpenseCategory.LEGAL]: 'Legal',
    [ExpenseCategory.MARKETING]: 'Marketing',
    [ExpenseCategory.CLEANING]: 'Cleaning',
    [ExpenseCategory.LANDSCAPING]: 'Landscaping',
    [ExpenseCategory.PROPERTY_MANAGEMENT]: 'Property Management',
    [ExpenseCategory.FEES]: 'Fees',
    [ExpenseCategory.PROPERTY_TAX]: 'Property Taxes',
    [ExpenseCategory.OTHER]: 'Other',
  };
  return categoryLabels[category];
};
