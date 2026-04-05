import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import { bulkCreateExpenses } from '@/api/expenses';
import { useCreateExpense } from '@/hooks/useExpenseHooks';
import { ExpenseForm } from '@/components/expenses/ExpenseForm';
import { PropertySelector } from '@/components/common/PropertySelector';
import { CurrencySelector } from '@/components/common/CurrencySelector';
import {
  BulkDataGrid,
  type BulkColumnDef,
  type RowData,
} from '@/components/common/BulkDataGrid';
import { useTeamDefaults } from '@/hooks/useTeamDefaults';
import {
  CreateExpenseRequest,
  ExpenseCategory,
  formatExpenseCategory,
} from '@/types/expense';
import { ArrowLeft, CheckCircle } from 'lucide-react';
import { getErrorMessage } from '@/utils/errorMessages';

type Mode = 'single' | 'bulk';

const CATEGORY_OPTIONS = Object.values(ExpenseCategory).map((cat) => ({
  value: cat,
  label: formatExpenseCategory(cat),
}));

const getBulkColumns = (t: (key: string) => string): BulkColumnDef[] => [
  {
    key: 'date',
    label: t('table.date'),
    type: 'date',
    required: true,
    placeholder: 'YYYY-MM-DD',
  },
  {
    key: 'amount',
    label: t('table.amount'),
    type: 'number',
    required: true,
    placeholder: '0.00',
  },
  {
    key: 'description',
    label: t('table.description'),
    type: 'text',
    required: true,
    placeholder: t('form.descriptionPlaceholder'),
  },
  {
    key: 'category',
    label: t('table.category'),
    type: 'select',
    required: true,
    options: CATEGORY_OPTIONS,
    defaultValue: ExpenseCategory.MAINTENANCE,
  },
];

export const ExpenseCreatePage = () => {
  const { t } = useTranslation('expenses');
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [searchParams] = useSearchParams();
  const createExpenseMutation = useCreateExpense();
  const { defaultCurrency, defaultDateFormat } = useTeamDefaults();

  const prefilledPropertyId = searchParams.get('propertyId') ?? undefined;

  const [continueAdding, setContinueAdding] = useState(false);
  const [resetKey, setResetKey] = useState(0);
  const [addedCount, setAddedCount] = useState(0);
  const [mode, setMode] = useState<Mode>('single');
  const [bulkPropertyId, setBulkPropertyId] = useState(
    prefilledPropertyId ?? ''
  );
  const [bulkCurrency, setBulkCurrency] = useState(defaultCurrency || '');
  const [bulkSubmitting, setBulkSubmitting] = useState(false);

  const handleSubmit = async (data: CreateExpenseRequest) => {
    await createExpenseMutation.mutateAsync(data);
    if (continueAdding) {
      setAddedCount((c) => c + 1);
      setResetKey((k) => k + 1);
    } else {
      navigate(-1);
    }
  };

  const handleCancel = () => {
    navigate(-1);
  };

  const handleBulkSubmit = (
    rows: RowData[],
    callbacks: {
      onRowStart: (index: number) => void;
      onRowSuccess: (index: number) => void;
      onRowError: (index: number, error: string) => void;
      onComplete: () => void;
    }
  ) => {
    const currency = bulkCurrency || defaultCurrency || '';
    setBulkSubmitting(true);

    const items: CreateExpenseRequest[] = rows.map((row) => ({
      propertyIdentifier: bulkPropertyId,
      category:
        (row.category as ExpenseCategory) || ExpenseCategory.MAINTENANCE,
      amount: parseFloat(row.amount),
      currency,
      expenseDate: row.date,
      description: row.description,
    }));

    // Mark all rows as submitting
    rows.forEach((_, i) => callbacks.onRowStart(i));

    bulkCreateExpenses(items)
      .then((results) => {
        let hasErrors = false;
        for (const result of results) {
          if (result.error) {
            hasErrors = true;
            callbacks.onRowError(result.index, result.error);
          } else {
            callbacks.onRowSuccess(result.index);
          }
        }

        queryClient.invalidateQueries({ queryKey: ['expenses'] });
        queryClient.invalidateQueries({ queryKey: ['expenseStats'] });
        queryClient.invalidateQueries({ queryKey: ['properties'] });
        queryClient.invalidateQueries({ queryKey: ['dashboard'] });
        queryClient.invalidateQueries({ queryKey: ['propertyDashboard'] });
        queryClient.invalidateQueries({ queryKey: ['financial-overview'] });
        queryClient.invalidateQueries({ queryKey: ['income-trend'] });
        queryClient.invalidateQueries({ queryKey: ['expense-breakdown'] });
        queryClient.invalidateQueries({ queryKey: ['property-comparison'] });

        setBulkSubmitting(false);
        callbacks.onComplete();
        if (!hasErrors) {
          navigate(-1);
        }
      })
      .catch((e) => {
        rows.forEach((_, i) => callbacks.onRowError(i, getErrorMessage(e)));
        setBulkSubmitting(false);
        callbacks.onComplete();
      });
  };

  return (
    <div className="min-h-screen bg-surface-page">
      <div className="max-w-4xl mx-auto px-4 py-8">
        {/* Header */}
        <div className="flex items-center gap-4 mb-6">
          <button
            onClick={() => navigate(-1)}
            className="p-2 hover:bg-neutral-100 rounded transition-colors"
          >
            <ArrowLeft className="h-5 w-5" />
          </button>
          <div className="flex-1">
            <h1 className="text-2xl font-bold text-text-primary">
              {t('create.title')}
            </h1>
            {addedCount > 0 && (
              <p className="flex items-center gap-1.5 text-sm text-success-text mt-1">
                <CheckCircle className="h-3.5 w-3.5" />
                {t('create.addedCount', { count: addedCount })}
              </p>
            )}
          </div>
        </div>

        {/* Mode toggle */}
        <div className="flex gap-1 mb-4 p-1 bg-neutral-100 rounded-lg w-fit">
          {(['single', 'bulk'] as Mode[]).map((m) => (
            <button
              key={m}
              type="button"
              onClick={() => setMode(m)}
              className={`px-4 py-1.5 text-sm font-medium rounded-md transition-colors ${
                mode === m
                  ? 'bg-surface-card text-text-primary shadow-sm'
                  : 'text-text-secondary hover:text-text-secondary'
              }`}
            >
              {m === 'single' ? t('create.single') : t('create.bulk')}
            </button>
          ))}
        </div>

        {/* Form */}
        <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
          {mode === 'single' && (
            <ExpenseForm
              onSubmit={handleSubmit}
              onCancel={handleCancel}
              isLoading={createExpenseMutation.isPending}
              prefilledPropertyId={prefilledPropertyId}
              resetKey={resetKey}
              continueAdding={continueAdding}
              onContinueAddingChange={setContinueAdding}
            />
          )}

          {mode === 'bulk' && (
            <>
              {/* Fixed fields */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-6">
                <div>
                  <label className="block text-sm font-medium text-text-secondary mb-2">
                    {t('form.property')} <span className="text-error-text">*</span>
                  </label>
                  <PropertySelector
                    value={bulkPropertyId}
                    onChange={(val) => setBulkPropertyId((val as string) ?? '')}
                    disabled={bulkSubmitting}
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-text-secondary mb-2">
                    {t('form.currency')} <span className="text-error-text">*</span>
                  </label>
                  <CurrencySelector
                    value={bulkCurrency || defaultCurrency || ''}
                    onChange={setBulkCurrency}
                    disabled={bulkSubmitting}
                  />
                </div>
              </div>

              <BulkDataGrid
                columns={getBulkColumns(t)}
                onSubmit={handleBulkSubmit}
                isSubmitting={bulkSubmitting}
                disabled={!bulkPropertyId || !(bulkCurrency || defaultCurrency)}
                dateFormat={
                  defaultDateFormat as
                    | 'DD/MM/YYYY'
                    | 'MM/DD/YYYY'
                    | 'YYYY-MM-DD'
                }
              />
            </>
          )}
        </div>
      </div>
    </div>
  );
};
