import { ExpenseCategory } from '@/types/expense';

interface ExpenseCategoryBadgeProps {
  category: ExpenseCategory;
  className?: string;
}

const categoryColors: Record<ExpenseCategory, string> = {
  MAINTENANCE: 'bg-blue-100 text-blue-800',
  REPAIR: 'bg-orange-100 text-orange-800',
  UTILITY: 'bg-cyan-100 text-cyan-800',
  TAX: 'bg-purple-100 text-purple-800',
  INSURANCE: 'bg-indigo-100 text-indigo-800',
  LEGAL: 'bg-pink-100 text-pink-800',
  MARKETING: 'bg-green-100 text-green-800',
  CLEANING: 'bg-teal-100 text-teal-800',
  LANDSCAPING: 'bg-lime-100 text-lime-800',
  PROPERTY_MANAGEMENT: 'bg-violet-100 text-violet-800',
  OTHER: 'bg-gray-100 text-gray-800',
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
