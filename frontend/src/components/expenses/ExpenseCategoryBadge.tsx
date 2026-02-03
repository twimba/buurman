import { ExpenseCategory } from '@/types/expense';

interface ExpenseCategoryBadgeProps {
  category: ExpenseCategory;
  className?: string;
}

const categoryColors: Record<ExpenseCategory, string> = {
  MAINTENANCE: 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200',
  REPAIR:
    'bg-orange-100 text-orange-800 dark:bg-orange-900 dark:text-orange-200',
  UTILITY: 'bg-cyan-100 text-cyan-800 dark:bg-cyan-900 dark:text-cyan-200',
  TAX: 'bg-purple-100 text-purple-800 dark:bg-purple-900 dark:text-purple-200',
  INSURANCE:
    'bg-indigo-100 text-indigo-800 dark:bg-indigo-900 dark:text-indigo-200',
  LEGAL: 'bg-pink-100 text-pink-800 dark:bg-pink-900 dark:text-pink-200',
  MARKETING:
    'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200',
  CLEANING: 'bg-teal-100 text-teal-800 dark:bg-teal-900 dark:text-teal-200',
  LANDSCAPING: 'bg-lime-100 text-lime-800 dark:bg-lime-900 dark:text-lime-200',
  PROPERTY_MANAGEMENT:
    'bg-violet-100 text-violet-800 dark:bg-violet-900 dark:text-violet-200',
  OTHER: 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-200',
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
  OTHER: 'Other',
};

export const ExpenseCategoryBadge = ({
  category,
  className = '',
}: ExpenseCategoryBadgeProps) => {
  return (
    <span
      className={`px-3 py-1 rounded-full text-xs font-semibold ${categoryColors[category]} ${className}`}
    >
      {categoryLabels[category]}
    </span>
  );
};
