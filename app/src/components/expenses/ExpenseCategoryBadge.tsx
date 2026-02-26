import { ExpenseCategory } from '@/types/expense';

interface ExpenseCategoryBadgeProps {
  category: ExpenseCategory;
  className?: string;
}

const categoryColors: Record<ExpenseCategory, string> = {
  MAINTENANCE:
    'bg-blue-50 text-blue-700 ring-1 ring-inset ring-blue-600/20 dark:bg-blue-500/10 dark:text-blue-400 dark:ring-blue-500/20',
  REPAIR:
    'bg-orange-50 text-orange-700 ring-1 ring-inset ring-orange-600/20 dark:bg-orange-500/10 dark:text-orange-400 dark:ring-orange-500/20',
  UTILITY:
    'bg-cyan-50 text-cyan-700 ring-1 ring-inset ring-cyan-600/20 dark:bg-cyan-500/10 dark:text-cyan-400 dark:ring-cyan-500/20',
  TAX: 'bg-purple-50 text-purple-700 ring-1 ring-inset ring-purple-600/20 dark:bg-purple-500/10 dark:text-purple-400 dark:ring-purple-500/20',
  INSURANCE:
    'bg-indigo-50 text-indigo-700 ring-1 ring-inset ring-indigo-600/20 dark:bg-indigo-500/10 dark:text-indigo-400 dark:ring-indigo-500/20',
  LEGAL:
    'bg-pink-50 text-pink-700 ring-1 ring-inset ring-pink-600/20 dark:bg-pink-500/10 dark:text-pink-400 dark:ring-pink-500/20',
  MARKETING:
    'bg-emerald-50 text-emerald-700 ring-1 ring-inset ring-emerald-600/20 dark:bg-emerald-500/10 dark:text-emerald-400 dark:ring-emerald-500/20',
  CLEANING:
    'bg-teal-50 text-teal-700 ring-1 ring-inset ring-teal-600/20 dark:bg-teal-500/10 dark:text-teal-400 dark:ring-teal-500/20',
  LANDSCAPING:
    'bg-lime-50 text-lime-700 ring-1 ring-inset ring-lime-600/20 dark:bg-lime-500/10 dark:text-lime-400 dark:ring-lime-500/20',
  PROPERTY_MANAGEMENT:
    'bg-violet-50 text-violet-700 ring-1 ring-inset ring-violet-600/20 dark:bg-violet-500/10 dark:text-violet-400 dark:ring-violet-500/20',
  FEES: 'bg-amber-50 text-amber-700 ring-1 ring-inset ring-amber-600/20 dark:bg-amber-500/10 dark:text-amber-400 dark:ring-amber-500/20',
  PROPERTY_TAX:
    'bg-rose-50 text-rose-700 ring-1 ring-inset ring-rose-600/20 dark:bg-rose-500/10 dark:text-rose-400 dark:ring-rose-500/20',
  OTHER:
    'bg-slate-100 text-slate-600 ring-1 ring-inset ring-slate-500/20 dark:bg-slate-500/10 dark:text-slate-400 dark:ring-slate-500/20',
};

const categoryLabels: Record<ExpenseCategory, string> = {
  MAINTENANCE: 'Maintenance',
  REPAIR: 'Repair',
  UTILITY: 'Utility',
  TAX: 'Tax',
  INSURANCE: 'Insurance',
  LEGAL: 'Legal',
  MARKETING: 'Marketing',
  CLEANING: 'Cleaning',
  LANDSCAPING: 'Landscaping',
  PROPERTY_MANAGEMENT: 'Property Management',
  FEES: 'Fees',
  PROPERTY_TAX: 'Property Taxes',
  OTHER: 'Other',
};

export const ExpenseCategoryBadge = ({
  category,
  className = '',
}: ExpenseCategoryBadgeProps) => {
  return (
    <span
      className={`px-2.5 py-1 rounded-lg text-xs font-semibold ${categoryColors[category]} ${className}`}
    >
      {categoryLabels[category]}
    </span>
  );
};
