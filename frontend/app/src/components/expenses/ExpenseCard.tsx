import { ExpenseResponse } from '@/types/expense';
import { ExpenseCategoryBadge } from './ExpenseCategoryBadge';
import { Receipt, Calendar, DollarSign, MapPin, User } from 'lucide-react';
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
      className="bg-surface-card rounded-lg shadow-sm hover:shadow-md transition-shadow cursor-pointer overflow-hidden"
      onClick={() => navigate(`/expenses/${expense.identifier}`)}
    >
      <div className="p-4">
        {/* Header */}
        <div className="flex items-start justify-between mb-3 gap-2">
          <div className="flex items-start gap-2 min-w-0 flex-1">
            <Receipt className="h-5 w-5 text-text-muted flex-shrink-0 mt-0.5" />
            <div className="min-w-0 flex-1">
              <h3 className="text-lg font-semibold text-text-primary">
                Expense #{expense.identifier}
              </h3>
              <p className="text-sm text-text-secondary truncate">
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
              <MapPin className="h-4 w-4 text-text-muted mt-0.5" />
              <div>
                <p className="text-xs text-text-secondary">Property</p>
                <p className="text-sm font-medium text-text-primary">
                  {expense.property.street}, {expense.property.city}
                </p>
              </div>
            </div>
          </div>
        )}

        {/* Contact */}
        {expense.contact && (
          <div className="mb-3">
            <div className="flex items-start gap-2">
              <User className="h-4 w-4 text-text-muted mt-0.5" />
              <div>
                <p className="text-xs text-text-secondary">Contact</p>
                <p className="text-sm font-medium text-text-primary">
                  {expense.contact.firstName} {expense.contact.lastName}
                </p>
              </div>
            </div>
          </div>
        )}

        {/* Amount and Date */}
        <div className="grid grid-cols-2 gap-3 pt-3 border-t border-border-default">
          <div className="flex items-center gap-2">
            <DollarSign className="h-4 w-4 text-text-muted " />
            <div>
              <p className="text-xs text-text-secondary">Amount</p>
              <p className="text-sm font-medium text-text-primary">
                {expense.currency} {expense.amount.toFixed(2)}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <Calendar className="h-4 w-4 text-text-muted " />
            <div>
              <p className="text-xs text-text-secondary">Expense Date</p>
              <p className="text-sm font-medium text-text-primary">
                {formatDate(expense.expenseDate)}
              </p>
            </div>
          </div>
        </div>

        {/* Documents Count */}
        {expense.documents && expense.documents.length > 0 && (
          <div className="mt-2 pt-2 border-t border-border-default">
            <p className="text-xs text-text-secondary">
              {expense.documents.length} document(s) attached
            </p>
          </div>
        )}
      </div>
    </div>
  );
};
