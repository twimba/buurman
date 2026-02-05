import { ExpenseResponse } from '@/types/expense';
import { ExpenseCategoryBadge } from './ExpenseCategoryBadge';
import { Receipt, Calendar, DollarSign, MapPin } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useFormatDate } from '@/hooks/useFormatDate';

interface ExpenseCardProps {
  expense: ExpenseResponse;
}

export const ExpenseCard = ({ expense }: ExpenseCardProps) => {
  const navigate = useNavigate();
  const { formatDate } = useFormatDate();

  return (
    <div
      className="bg-white dark:bg-[#14161f] rounded-xl shadow-sm hover:shadow-md transition-shadow cursor-pointer overflow-hidden"
      onClick={() => navigate(`/expenses/${expense.id}`)}
    >
      <div className="p-4">
        {/* Header */}
        <div className="flex items-start justify-between mb-3 gap-2">
          <div className="flex items-start gap-2 min-w-0 flex-1">
            <Receipt className="h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180] flex-shrink-0 mt-0.5" />
            <div className="min-w-0 flex-1">
              <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                Expense #{expense.identifier}
              </h3>
              <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] truncate">
                {expense.description}
              </p>
            </div>
          </div>
          <div className="flex-shrink-0">
            <ExpenseCategoryBadge category={expense.category} />
          </div>
        </div>

        {/* Property */}
        {expense.property && (
          <div className="mb-3">
            <div className="flex items-start gap-2">
              <MapPin className="h-4 w-4 text-[#9ca0b8] dark:text-[#5c6180] mt-0.5" />
              <div>
                <p className="text-xs text-[#6b7194] dark:text-[#8b90a8]">
                  Property
                </p>
                <p className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                  {expense.property.street}, {expense.property.city}
                </p>
              </div>
            </div>
          </div>
        )}

        {/* Amount and Date */}
        <div className="grid grid-cols-2 gap-3 pt-3 border-t border-[#e2e6f0] dark:border-[#2a2e3f]">
          <div className="flex items-center gap-2">
            <DollarSign className="h-4 w-4 text-[#9ca0b8] dark:text-[#5c6180]" />
            <div>
              <p className="text-xs text-[#6b7194] dark:text-[#8b90a8]">
                Amount
              </p>
              <p className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                {expense.currency} {expense.amount.toFixed(2)}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <Calendar className="h-4 w-4 text-[#9ca0b8] dark:text-[#5c6180]" />
            <div>
              <p className="text-xs text-[#6b7194] dark:text-[#8b90a8]">
                Expense Date
              </p>
              <p className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                {formatDate(expense.expenseDate)}
              </p>
            </div>
          </div>
        </div>

        {/* Documents Count */}
        {expense.documents && expense.documents.length > 0 && (
          <div className="mt-2 pt-2 border-t border-[#edf0f7] dark:border-[#2a2e3f]">
            <p className="text-xs text-[#6b7194] dark:text-[#8b90a8]">
              {expense.documents.length} document(s) attached
            </p>
          </div>
        )}
      </div>
    </div>
  );
};
