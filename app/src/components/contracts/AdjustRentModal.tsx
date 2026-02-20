import { useState, useMemo } from 'react';
import { X, TrendingUp, TrendingDown } from 'lucide-react';
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

  const percentageChange = useMemo(() => {
    if (parsedAmount === null || currentRent <= 0) return null;
    return ((parsedAmount - currentRent) / currentRent) * 100;
  }, [parsedAmount, currentRent]);

  const submitForm = () => {
    if (parsedAmount === null || parsedAmount <= 0) return;
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
      <div className="bg-white dark:bg-[#14161f] rounded-lg shadow-xl dark:shadow-black/20 max-w-md w-full mx-4">
        {/* Header */}
        <div className="flex items-center justify-between p-4 border-b border-[#e2e6f0] dark:border-[#2a2e3f]">
          <h2 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
            Adjust Rent
          </h2>
          <button
            onClick={onClose}
            className="text-[#9ca0b8] dark:text-[#5c6180] hover:text-[#6b7194] dark:hover:text-[#c4c8db]"
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
              <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
                Current Rent
              </label>
              <p className="text-sm text-[#1a1d2e] dark:text-[#eef0f6]">
                {currency} {currentRent.toFixed(2)}
              </p>
            </div>

            {/* New Rent Amount */}
            <div>
              <label
                htmlFor="rentAmount"
                className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1"
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
                        <TrendingUp className="h-4 w-4 text-emerald-500" />
                        <span className="text-sm font-medium text-emerald-500">
                          +{percentageChange.toFixed(1)}%
                        </span>
                      </>
                    ) : percentageChange < 0 ? (
                      <>
                        <TrendingDown className="h-4 w-4 text-red-500" />
                        <span className="text-sm font-medium text-red-500">
                          {percentageChange.toFixed(1)}%
                        </span>
                      </>
                    ) : (
                      <span className="text-sm text-[#9ca0b8]">0%</span>
                    )}
                  </div>
                )}
              </div>
            </div>

            {/* Effective From */}
            <div>
              <label
                htmlFor="effectiveFrom"
                className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1"
              >
                Effective From
              </label>
              <input
                id="effectiveFrom"
                type="date"
                value={effectiveFrom}
                onChange={(e) => setEffectiveFrom(e.target.value)}
                className="w-full px-3 py-2 border border-[#c9cfd9] dark:border-[#3a3f54] rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white dark:bg-[#1e2130] text-[#1a1d2e] dark:text-[#eef0f6]"
                disabled={isLoading}
                required
              />
              <p className="mt-1 text-xs text-[#9ca0b8] dark:text-[#5c6180]">
                Pending payments from this date will be updated to the new
                amount.
              </p>
            </div>

            {/* Notes */}
            <div>
              <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
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
          <div className="flex items-center justify-end gap-3 p-4 border-t border-[#e2e6f0] dark:border-[#2a2e3f]">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] bg-white dark:bg-[#1e2130] border border-[#c9cfd9] dark:border-[#3a3f54] rounded-md hover:bg-[#f1f3f9] dark:hover:bg-[#3a3f54]"
              disabled={isLoading}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-4 py-2 text-sm font-medium text-white bg-[#5c7cfa] rounded-md hover:bg-[#4c6ef5] disabled:opacity-50"
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
