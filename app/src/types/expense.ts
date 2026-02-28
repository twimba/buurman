import { PropertySummary, DocumentResponse } from './property';

// Enum — re-exported from generated
export {
  ExpenseResponseCategory as ExpenseCategory,
  type ExpenseResponseCategory,
} from '../generated/models';

// Interfaces — kept manual (generated adds | null to optional fields)

import { ExpenseResponseCategory } from '../generated/models';

// Request interfaces — manual (generated adds | null to all optional fields)

export interface CreateExpenseRequest {
  propertyIdentifier: string;
  category: ExpenseResponseCategory;
  amount: number;
  currency?: string;
  expenseDate: string;
  description: string;
  notes?: string;
}

export interface UpdateExpenseRequest {
  category?: ExpenseResponseCategory;
  amount?: number;
  currency?: string;
  expenseDate?: string;
  description?: string;
  notes?: string;
}

export interface ExpenseResponse {
  identifier: string;
  property: PropertySummary;
  category: ExpenseResponseCategory;
  amount: number;
  currency: string;
  expenseDate: string;
  description: string;
  notes?: string;
  documents: DocumentResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface ExpenseSummaryResponse {
  period: string;
  byCategory: CategoryTotal[];
  grandTotal: number;
  currency: string;
}

export interface CategoryTotal {
  category: ExpenseResponseCategory;
  total: number;
  count: number;
}

export interface GetExpensesParams {
  category?: ExpenseResponseCategory;
  propertyIdentifier?: string;
  dateFrom?: string;
  dateTo?: string;
}

export const formatExpenseCategory = (
  category: ExpenseResponseCategory
): string => {
  const categoryLabels: Record<ExpenseResponseCategory, string> = {
    [ExpenseResponseCategory.MAINTENANCE]: 'Maintenance',
    [ExpenseResponseCategory.REPAIR]: 'Repair',
    [ExpenseResponseCategory.UTILITY]: 'Utility',
    [ExpenseResponseCategory.TAX]: 'Tax',
    [ExpenseResponseCategory.INSURANCE]: 'Insurance',
    [ExpenseResponseCategory.LEGAL]: 'Legal',
    [ExpenseResponseCategory.MARKETING]: 'Marketing',
    [ExpenseResponseCategory.CLEANING]: 'Cleaning',
    [ExpenseResponseCategory.LANDSCAPING]: 'Landscaping',
    [ExpenseResponseCategory.PROPERTY_MANAGEMENT]: 'Property Management',
    [ExpenseResponseCategory.FEES]: 'Fees',
    [ExpenseResponseCategory.PROPERTY_TAX]: 'Property Taxes',
    [ExpenseResponseCategory.OTHER]: 'Other',
  };
  return categoryLabels[category];
};
