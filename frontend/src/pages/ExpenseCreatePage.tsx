import { useNavigate, useSearchParams } from 'react-router-dom';
import { useCreateExpense } from '@/hooks/useExpenseHooks';
import { ExpenseForm } from '@/components/expenses/ExpenseForm';
import { CreateExpenseRequest } from '@/types/expense';
import { ArrowLeft } from 'lucide-react';

export const ExpenseCreatePage = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const createExpenseMutation = useCreateExpense();

  const prefilledPropertyId = searchParams.get('propertyId') || undefined;

  const handleSubmit = async (data: CreateExpenseRequest) => {
    await createExpenseMutation.mutateAsync(data);
    navigate('/expenses');
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
          <h1 className="text-2xl font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
            Add New Expense
          </h1>
        </div>

        {/* Form */}
        <div className="bg-white dark:bg-[#14161f] rounded-lg shadow p-6">
          <ExpenseForm
            onSubmit={handleSubmit}
            onCancel={handleCancel}
            isLoading={createExpenseMutation.isPending}
            prefilledPropertyId={prefilledPropertyId}
          />
        </div>
      </div>
    </div>
  );
};
