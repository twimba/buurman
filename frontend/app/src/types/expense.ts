import { PropertySummary, DocumentResponse } from './property';
import { ContactSummary } from './contact';

// Enum — re-exported from generated
export {
  ExpenseResponseCategory as ExpenseCategory,
  type ExpenseResponseCategory,
} from '../generated/models';

// Interfaces — kept manual (generated adds to optional fields)

import { ExpenseResponseCategory } from '../generated/models';

// Request interfaces — manual (generated adds to all optional fields)

export interface CreateExpenseRequest {
  propertyIdentifier: string;
  contactIdentifier?: string;
  category: ExpenseResponseCategory;
  amount: number;
  currency?: string;
  expenseDate: string;
  description: string;
  notes?: string;
}

export interface UpdateExpenseRequest {
  contactIdentifier?: string;
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
  contact?: ContactSummary;
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
  contactIdentifier?: string;
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
