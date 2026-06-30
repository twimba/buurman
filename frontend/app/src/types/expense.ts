// Thin re-exports of generated models. Frontend-only enum + label map kept.

import { ExpenseResponseCategory } from '../generated/models';

export {
  ExpenseResponseCategory as ExpenseCategory,
  type ExpenseResponseCategory,
} from '../generated/models';

export type {
  ExpenseResponse,
  CreateExpenseRequest,
  UpdateExpenseRequest,
  GetExpensesParams,
} from '../generated/models';

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
