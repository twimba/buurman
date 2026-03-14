import { useState, useMemo } from 'react';
import { X, TrendingUp, TrendingDown, Info } from 'lucide-react';
import { MoneyInput } from '@/components/common/MoneyInput';
import { RichTextEditor } from '@/components/common/RichTextEditor';

interface AdjustRentModalProps {
  currentRent: number;
  currency: string;
  onClose: () => void;
  onConfirm: (
    rentAmount: number,
    effectiveFrom: string,
    notes?: string
  ) => void;
  isLoading?: boolean;
}

function getFirstDayOfNextMonth(): string {
  const now = new Date();
  const next = new Date(now.getFullYear(), now.getMonth() + 1, 1);
  return next.toISOString().split('T')[0];
}

export const AdjustRentModal = ({
  currentRent,
  currency,
  onClose,
  onConfirm,
  isLoading = false,
}: AdjustRentModalProps) => {
  const [rentAmount, setRentAmount] = useState('');
  const [effectiveFrom, setEffectiveFrom] = useState(getFirstDayOfNextMonth());
  const [notes, setNotes] = useState('');

  const parsedAmount = useMemo(() => {
    const val = parseFloat(rentAmount);
    return isNaN(val) ? null : val;
  }, [rentAmount]);

  const isRetroactive = useMemo(() => {
    if (!effectiveFrom) {
      return false;
    }
    const today = new Date().toISOString().split('T')[0];
    return effectiveFrom < today;
  }, [effectiveFrom]);

  const percentageChange = useMemo(() => {
    if (parsedAmount === null || currentRent <= 0) {
      return null;
    }
    return ((parsedAmount - currentRent) / currentRent) * 100;
  }, [parsedAmount, currentRent]);

  const submitForm = () => {
    if (parsedAmount === null || parsedAmount <= 0) {
      return;
    }
    onConfirm(parsedAmount, effectiveFrom, notes || undefined);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    submitForm();
  };

  const handleCmdEnter = (e: React.KeyboardEvent) => {
    if ((e.metaKey || e.ctrlKey) && e.key === 'Enter') {
      e.preventDefault();
      submitForm();
    }
  };

  return (
    <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50">
      <div className="bg-surface-card rounded-lg shadow-xl dark:shadow-black/20 max-w-md w-full mx-4">
        {/* Header */}
        <div className="flex items-center justify-between p-4 border-b border-border-default">
          <h2 className="text-lg font-semibold text-text-primary">
            Adjust Rent
          </h2>
          <button
            onClick={onClose}
            className="text-text-muted hover:text-text-secondary"
            disabled={isLoading}
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Body */}
        <form onSubmit={handleSubmit} onKeyDown={handleCmdEnter}>
          <div className="p-4 space-y-4">
            {/* Current Rent */}
            <div>
              <label className="block text-sm font-medium text-text-secondary mb-1">
                Current Rent
              </label>
              <p className="text-sm text-text-primary">
                {currency} {currentRent.toFixed(2)}
              </p>
            </div>

            {/* New Rent Amount */}
            <div>
              <label
                htmlFor="rentAmount"
                className="block text-sm font-medium text-text-secondary mb-1"
              >
                New Rent Amount
              </label>
              <div className="relative">
                <MoneyInput
                  id="rentAmount"
                  value={parsedAmount ?? undefined}
                  onChange={(val) =>
                    setRentAmount(val !== undefined ? String(val) : '')
                  }
                  currency={currency}
                  disabled={isLoading}
                  min={0.01}
                  className="pr-20"
                />
                {percentageChange !== null && (
                  <div className="absolute right-3 top-1/2 -translate-y-1/2 flex items-center gap-1">
                    {percentageChange > 0 ? (
                      <>
                        <TrendingUp className="h-4 w-4 text-success-text" />
                        <span className="text-sm font-medium text-success-text">
                          +{percentageChange.toFixed(1)}%
                        </span>
                      </>
                    ) : percentageChange < 0 ? (
                      <>
                        <TrendingDown className="h-4 w-4 text-error-text" />
                        <span className="text-sm font-medium text-error-text">
                          {percentageChange.toFixed(1)}%
                        </span>
                      </>
                    ) : (
                      <span className="text-sm text-text-muted">0%</span>
                    )}
                  </div>
                )}
              </div>
            </div>

            {/* Effective From */}
            <div>
              <label
                htmlFor="effectiveFrom"
                className="block text-sm font-medium text-text-secondary mb-1"
              >
                Effective From
              </label>
              <input
                id="effectiveFrom"
                type="date"
                value={effectiveFrom}
                onChange={(e) => setEffectiveFrom(e.target.value)}
                className="w-full px-3 py-2 border border-border-strong rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500 bg-surface-card text-text-primary"
                disabled={isLoading}
                required
              />
              {isRetroactive ? (
                <div className="mt-2 flex items-start gap-1.5 text-xs text-info-text">
                  <Info className="h-3.5 w-3.5 mt-0.5 flex-shrink-0" />
                  <span>
                    This is a retroactive adjustment. Pending payments will be
                    updated. For already settled payments, an adjustment payment
                    will be created for the difference.
                  </span>
                </div>
              ) : (
                <p className="mt-1 text-xs text-text-muted">
                  Pending payments from this date will be updated to the new
                  amount.
                </p>
              )}
            </div>

            {/* Notes */}
            <div>
              <label className="block text-sm font-medium text-text-secondary mb-1">
                Notes (Optional)
              </label>
              <RichTextEditor
                value={notes}
                onChange={setNotes}
                placeholder="Reason for adjustment (e.g., annual CPI increase)..."
                onSubmit={submitForm}
              />
            </div>
          </div>

          {/* Footer */}
          <div className="flex items-center justify-end gap-3 p-4 border-t border-border-default">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-sm font-medium text-text-secondary bg-surface-card border border-border-strong rounded-md hover:bg-surface-inset"
              disabled={isLoading}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-4 py-2 text-sm font-medium text-white bg-primary-500 rounded-md hover:bg-primary-600 disabled:opacity-50"
              disabled={isLoading || parsedAmount === null || parsedAmount <= 0}
            >
              {isLoading ? 'Saving...' : 'Adjust Rent'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
