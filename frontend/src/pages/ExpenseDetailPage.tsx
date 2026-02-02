import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  useExpense,
  useDeleteExpense,
  useUpdateExpense,
} from '@/hooks/useExpenseHooks';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { ErrorMessage } from '@/components/ErrorMessage';
import { ExpenseCategoryBadge } from '@/components/expenses/ExpenseCategoryBadge';
import { ExpenseForm } from '@/components/expenses/ExpenseForm';
import {
  ArrowLeft,
  Edit,
  Trash2,
  Receipt,
  Calendar,
  DollarSign,
  Home,
  FileText,
  Package,
} from 'lucide-react';
import { format } from 'date-fns';
import { UpdateExpenseRequest, CreateExpenseRequest } from '@/types/expense';

export const ExpenseDetailPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [isEditing, setIsEditing] = useState(false);
  const [showDeleteModal, setShowDeleteModal] = useState(false);

  const { data: expense, isLoading, error } = useExpense(id);
  const deleteExpenseMutation = useDeleteExpense();
  const updateExpenseMutation = useUpdateExpense(id!);

  const handleDelete = async () => {
    if (!id) return;
    try {
      await deleteExpenseMutation.mutateAsync(id);
      navigate('/expenses');
    } catch (err) {
      console.error('Failed to delete expense:', err);
    }
  };

  const handleUpdate = async (data: CreateExpenseRequest) => {
    const updateData: UpdateExpenseRequest = {
      category: data.category,
      amount: data.amount,
      currency: data.currency,
      expenseDate: data.expenseDate,
      description: data.description,
      notes: data.notes,
    };
    await updateExpenseMutation.mutateAsync(updateData);
    setIsEditing(false);
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <LoadingSpinner />
      </div>
    );
  }

  if (error || !expense) {
    return (
      <div className="min-h-screen bg-background p-8">
        <ErrorMessage message="Failed to load expense" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <div className="max-w-6xl mx-auto px-4 py-8">
        {/* Header */}
        <div className="bg-white border-b border-gray-200 -mx-4 px-4 py-4 mb-6">
          <div className="flex items-center justify-between max-w-6xl mx-auto">
            <div className="flex items-center gap-4">
              <button
                onClick={() => navigate('/expenses')}
                className="p-2 hover:bg-gray-100 rounded transition-colors"
              >
                <ArrowLeft className="h-5 w-5" />
              </button>
              <div>
                <h1 className="text-xl font-bold text-gray-900">
                  Expense #{expense.identifier}
                </h1>
                <p className="text-xs text-gray-500">{expense.description}</p>
              </div>
              <div className="ml-2">
                <ExpenseCategoryBadge category={expense.category} />
              </div>
            </div>

            <div className="flex items-center gap-2">
              {!isEditing && (
                <button
                  onClick={() => setIsEditing(true)}
                  className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors"
                >
                  <Edit className="h-4 w-4" />
                  Edit
                </button>
              )}
              <button
                onClick={() => setShowDeleteModal(true)}
                className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-red-700 bg-white border border-gray-300 rounded-md hover:bg-red-50 hover:border-red-300 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500 transition-colors"
              >
                <Trash2 className="h-4 w-4" />
                Delete
              </button>
            </div>
          </div>
        </div>

        {/* Content */}
        {isEditing ? (
          <div className="bg-white rounded-lg shadow p-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">
              Edit Expense
            </h2>
            <ExpenseForm
              expense={expense}
              onSubmit={handleUpdate}
              onCancel={() => setIsEditing(false)}
              isLoading={updateExpenseMutation.isPending}
            />
          </div>
        ) : (
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Expense Details */}
            <div className="bg-white rounded-lg shadow p-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-4">
                Expense Details
              </h2>
              <div className="space-y-4">
                <div className="flex items-center gap-3">
                  <DollarSign className="h-5 w-5 text-gray-400" />
                  <div>
                    <p className="text-sm text-gray-500">Amount</p>
                    <p className="font-medium text-gray-900 text-lg">
                      {expense.currency} {expense.amount.toFixed(2)}
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <Calendar className="h-5 w-5 text-gray-400" />
                  <div>
                    <p className="text-sm text-gray-500">Expense Date</p>
                    <p className="font-medium text-gray-900">
                      {format(new Date(expense.expenseDate), 'MMMM d, yyyy')}
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <Package className="h-5 w-5 text-gray-400" />
                  <div>
                    <p className="text-sm text-gray-500">Category</p>
                    <p className="font-medium text-gray-900">
                      {expense.category.replace('_', ' ')}
                    </p>
                  </div>
                </div>
                <div className="flex items-start gap-3">
                  <Receipt className="h-5 w-5 text-gray-400 mt-1" />
                  <div>
                    <p className="text-sm text-gray-500">Description</p>
                    <p className="font-medium text-gray-900">
                      {expense.description}
                    </p>
                  </div>
                </div>
              </div>
            </div>

            {/* Property Info */}
            <div className="bg-white rounded-lg shadow p-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-4">
                Property Information
              </h2>
              <div className="space-y-4">
                <div className="flex items-start gap-3">
                  <Home className="h-5 w-5 text-gray-400 mt-1" />
                  <div className="flex-1">
                    <p className="text-sm text-gray-500">Property</p>
                    <button
                      onClick={() =>
                        navigate(`/properties/${expense.property.id}`)
                      }
                      className="font-medium text-blue-600 hover:underline text-left"
                    >
                      {expense.property.street}, {expense.property.city}
                    </button>
                    <p className="text-xs text-gray-500 mt-1">
                      #{expense.property.identifier}
                    </p>
                  </div>
                </div>
              </div>
            </div>

            {/* Documents */}
            {expense.documents && expense.documents.length > 0 && (
              <div className="bg-white rounded-lg shadow p-6 lg:col-span-2">
                <h2 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
                  <FileText className="h-5 w-5" />
                  Attached Documents ({expense.documents.length})
                </h2>
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                  {expense.documents.map((doc) => (
                    <div
                      key={doc.id}
                      className="border border-gray-200 rounded-lg p-4 hover:border-blue-300 transition-colors"
                    >
                      <div className="flex items-start gap-3">
                        <FileText className="h-5 w-5 text-gray-400 mt-1" />
                        <div className="flex-1 min-w-0">
                          <p className="font-medium text-gray-900 truncate">
                            {doc.title || doc.fileName}
                          </p>
                          <p className="text-xs text-gray-500 truncate">
                            {doc.fileName}
                          </p>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Notes */}
            {expense.notes && (
              <div className="bg-white rounded-lg shadow p-6 lg:col-span-2">
                <h2 className="text-lg font-semibold text-gray-900 mb-4">
                  Notes
                </h2>
                <p className="text-gray-700 whitespace-pre-wrap">
                  {expense.notes}
                </p>
              </div>
            )}

            {/* Metadata */}
            <div className="bg-white rounded-lg shadow p-6 lg:col-span-2">
              <h2 className="text-lg font-semibold text-gray-900 mb-4">
                Record Information
              </h2>
              <div className="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <p className="text-gray-500">Created</p>
                  <p className="text-gray-900">
                    {format(new Date(expense.createdAt), 'PPpp')}
                  </p>
                </div>
                <div>
                  <p className="text-gray-500">Last Updated</p>
                  <p className="text-gray-900">
                    {format(new Date(expense.updatedAt), 'PPpp')}
                  </p>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Delete Confirmation Modal */}
      {showDeleteModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-xl max-w-md w-full mx-4 p-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">
              Delete Expense
            </h2>
            <p className="text-gray-700 mb-6">
              Are you sure you want to delete this expense? This action cannot
              be undone.
            </p>
            <div className="flex justify-end gap-3">
              <button
                onClick={() => setShowDeleteModal(false)}
                className="px-4 py-2 border border-gray-300 rounded hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                onClick={handleDelete}
                className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700"
                disabled={deleteExpenseMutation.isPending}
              >
                {deleteExpenseMutation.isPending ? 'Deleting...' : 'Delete'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
