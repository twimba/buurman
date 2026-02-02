import { ExpenseResponse } from '@/types/expense';
import { ExpenseCategoryBadge } from './ExpenseCategoryBadge';
import { Receipt, Calendar, DollarSign, MapPin } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { format } from 'date-fns';

interface ExpenseCardProps {
  expense: ExpenseResponse;
}

export const ExpenseCard = ({ expense }: ExpenseCardProps) => {
  const navigate = useNavigate();

  return (
    <div
      className="bg-white rounded-lg shadow-sm hover:shadow-md transition-shadow cursor-pointer overflow-hidden"
      onClick={() => navigate(`/expenses/${expense.id}`)}
    >
      <div className="p-4">
        {/* Header */}
        <div className="flex items-start justify-between mb-3 gap-2">
          <div className="flex items-start gap-2 min-w-0 flex-1">
            <Receipt className="h-5 w-5 text-gray-400 flex-shrink-0 mt-0.5" />
            <div className="min-w-0 flex-1">
              <h3 className="text-lg font-semibold text-gray-900">
                Expense #{expense.identifier}
              </h3>
              <p className="text-sm text-gray-500 truncate">
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
              <MapPin className="h-4 w-4 text-gray-400 mt-0.5" />
              <div>
                <p className="text-xs text-gray-500">Property</p>
                <p className="text-sm font-medium text-gray-900">
                  {expense.property.street}, {expense.property.city}
                </p>
              </div>
            </div>
          </div>
        )}

        {/* Amount and Date */}
        <div className="grid grid-cols-2 gap-3 pt-3 border-t border-gray-200">
          <div className="flex items-center gap-2">
            <DollarSign className="h-4 w-4 text-gray-400" />
            <div>
              <p className="text-xs text-gray-500">Amount</p>
              <p className="text-sm font-medium text-gray-900">
                {expense.currency} {expense.amount.toFixed(2)}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <Calendar className="h-4 w-4 text-gray-400" />
            <div>
              <p className="text-xs text-gray-500">Expense Date</p>
              <p className="text-sm font-medium text-gray-900">
                {format(new Date(expense.expenseDate), 'MMM d, yyyy')}
              </p>
            </div>
          </div>
        </div>

        {/* Documents Count */}
        {expense.documents && expense.documents.length > 0 && (
          <div className="mt-2 pt-2 border-t border-gray-100">
            <p className="text-xs text-gray-500">
              {expense.documents.length} document(s) attached
            </p>
          </div>
        )}
      </div>
    </div>
  );
};
