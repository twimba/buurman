import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useCreateExpense } from '@/hooks/useExpenseHooks';
import { ExpenseForm } from '@/components/expenses/ExpenseForm';
import { CreateExpenseRequest } from '@/types/expense';
import { ArrowLeft, CheckCircle } from 'lucide-react';

export const ExpenseCreatePage = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const createExpenseMutation = useCreateExpense();

  const prefilledPropertyId = searchParams.get('propertyId') || undefined;

  const [continueAdding, setContinueAdding] = useState(false);
  const [resetKey, setResetKey] = useState(0);
  const [addedCount, setAddedCount] = useState(0);

  const handleSubmit = async (data: CreateExpenseRequest) => {
    await createExpenseMutation.mutateAsync(data);
    if (continueAdding) {
      setAddedCount((c) => c + 1);
      setResetKey((k) => k + 1);
    } else {
      navigate('/expenses');
    }
  };

  const handleCancel = () => {
    navigate('/expenses');
  };

  return (
    <div className="min-h-screen bg-[#f8f9fc] dark:bg-[#0c0d14]">
      <div className="max-w-4xl mx-auto px-4 py-8">
        {/* Header */}
        <div className="flex items-center gap-4 mb-6">
          <button
            onClick={() => navigate('/expenses')}
            className="p-2 hover:bg-[#e8ecf4] dark:bg-[#1e2130] dark:hover:bg-[#1e2130] rounded transition-colors"
          >
            <ArrowLeft className="h-5 w-5" />
          </button>
          <div className="flex-1">
            <h1 className="text-2xl font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
              Add New Expense
            </h1>
            {addedCount > 0 && (
              <p className="flex items-center gap-1.5 text-sm text-emerald-600 dark:text-emerald-400 mt-1">
                <CheckCircle className="h-3.5 w-3.5" />
                {addedCount} expense{addedCount !== 1 ? 's' : ''} added this
                session
              </p>
            )}
          </div>
        </div>

        {/* Form */}
        <div className="bg-white dark:bg-[#14161f] rounded-lg shadow p-6">
          {/* Continue adding checkbox */}
          <label className="flex items-center gap-2 mb-6 cursor-pointer select-none">
            <input
              type="checkbox"
              checked={continueAdding}
              onChange={(e) => setContinueAdding(e.target.checked)}
              className="h-4 w-4 rounded border-[#c9cfd9] dark:border-[#3a3f54] text-[#5c7cfa] focus:ring-[#5c7cfa]"
            />
            <span className="text-sm text-[#3d4463] dark:text-[#c4c8db]">
              Continue adding more
            </span>
          </label>

          <ExpenseForm
            onSubmit={handleSubmit}
            onCancel={handleCancel}
            isLoading={createExpenseMutation.isPending}
            prefilledPropertyId={prefilledPropertyId}
            resetKey={resetKey}
          />
        </div>
      </div>
    </div>
  );
};
