import { useState, useMemo } from 'react';
import { X, TrendingUp, TrendingDown } from 'lucide-react';
import { addMonths, format } from 'date-fns';
import { MoneyInput } from '@/components/common/MoneyInput';
import type {
  RentAdjustmentType,
  CreateContractExtensionRequest,
} from '@/types/contractExtension';

interface CreateExtensionModalProps {
  currentRentAmount: number;
  currency: string;
  currentEndDate?: string;
  renewalTermMonths?: number;
  rentAdjustmentType?: RentAdjustmentType;
  rentAdjustmentValue?: number;
  onClose: () => void;
  onConfirm: (request: CreateContractExtensionRequest) => void;
  isLoading?: boolean;
}

function computeNewEndDate(
  currentEndDate: string,
  termMonths: number
): string {
  // date-fns addMonths handles month overflow correctly
  // (e.g., Jan 31 + 1 month = Feb 28, not Mar 3)
  return format(addMonths(new Date(currentEndDate), termMonths), 'yyyy-MM-dd');
}

function computeAdjustedRent(
  currentRent: number,
  adjustmentType: RentAdjustmentType,
  adjustmentValue?: number
): number | undefined {
  if (adjustmentValue === undefined || adjustmentValue === null) {
    return undefined;
  }
  switch (adjustmentType) {
    case 'FIXED_PERCENTAGE':
      return Math.round(currentRent * (1 + adjustmentValue / 100) * 100) / 100;
    case 'FIXED_AMOUNT':
      return Math.round((currentRent + adjustmentValue) * 100) / 100;
    default:
      return undefined;
  }
}

const ADJUSTMENT_TYPE_LABELS: Record<RentAdjustmentType, string> = {
  NONE: 'No adjustment',
  FIXED_PERCENTAGE: 'Fixed percentage',
  FIXED_AMOUNT: 'Fixed amount',
  MANUAL: 'Manual',
};

export const CreateExtensionModal = ({
  currentRentAmount,
  currency,
  currentEndDate,
  renewalTermMonths,
  rentAdjustmentType: defaultAdjustmentType = 'NONE',
  rentAdjustmentValue: defaultAdjustmentValue,
  onClose,
  onConfirm,
  isLoading = false,
}: CreateExtensionModalProps) => {
  const defaultNewEnd =
    currentEndDate && renewalTermMonths
      ? computeNewEndDate(currentEndDate, renewalTermMonths)
      : '';

  const defaultRent = computeAdjustedRent(
    currentRentAmount,
    defaultAdjustmentType,
    defaultAdjustmentValue
  );

  const [newEndDate, setNewEndDate] = useState(defaultNewEnd);
  const [newRentAmount, setNewRentAmount] = useState<string>(
    defaultRent !== undefined ? String(defaultRent) : ''
  );
  const [adjustmentType, setAdjustmentType] =
    useState<RentAdjustmentType>(defaultAdjustmentType);
  const [adjustmentValue, setAdjustmentValue] = useState<string>(
    defaultAdjustmentValue !== undefined ? String(defaultAdjustmentValue) : ''
  );
  const [notes, setNotes] = useState('');
  const [dateError, setDateError] = useState('');

  const parsedRent = useMemo(() => {
    const val = parseFloat(newRentAmount);
    return isNaN(val) ? null : val;
  }, [newRentAmount]);

  const percentageChange = useMemo(() => {
    if (parsedRent === null || currentRentAmount <= 0) {
      return null;
    }
    return ((parsedRent - currentRentAmount) / currentRentAmount) * 100;
  }, [parsedRent, currentRentAmount]);

  // Recompute rent when adjustment type/value changes
  const handleAdjustmentTypeChange = (type: RentAdjustmentType) => {
    setAdjustmentType(type);
    const parsedVal = parseFloat(adjustmentValue);
    if (!isNaN(parsedVal)) {
      const computed = computeAdjustedRent(currentRentAmount, type, parsedVal);
      if (computed !== undefined) {
        setNewRentAmount(String(computed));
      }
    }
  };

  const handleAdjustmentValueChange = (val: string) => {
    setAdjustmentValue(val);
    const parsedVal = parseFloat(val);
    if (!isNaN(parsedVal)) {
      const computed = computeAdjustedRent(
        currentRentAmount,
        adjustmentType,
        parsedVal
      );
      if (computed !== undefined) {
        setNewRentAmount(String(computed));
      }
    }
  };

  const submitForm = () => {
    // Validate new end date is after current end date
    if (newEndDate && currentEndDate && newEndDate <= currentEndDate) {
      setDateError('New end date must be after the current end date');
      return;
    }
    setDateError('');

    const request: CreateContractExtensionRequest = {
      newEndDate: newEndDate || undefined,
      newRentAmount: parsedRent ?? undefined,
      rentAdjustmentType: adjustmentType !== 'NONE' ? adjustmentType : undefined,
      rentAdjustmentValue: adjustmentValue
        ? parseFloat(adjustmentValue)
        : undefined,
      notes: notes || undefined,
    };
    onConfirm(request);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    submitForm();
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if ((e.metaKey || e.ctrlKey) && e.key === 'Enter') {
      e.preventDefault();
      submitForm();
    }
    if (e.key === 'Escape') {
      onClose();
    }
  };

  return (
    <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50">
      <div className="bg-surface-card rounded-lg shadow-xl dark:shadow-black/20 max-w-lg w-full mx-4 max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between p-4 border-b border-border-default">
          <h2 className="text-lg font-semibold text-text-primary">
            Create Contract Extension
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
        <form onSubmit={handleSubmit} onKeyDown={handleKeyDown}>
          <div className="p-4 space-y-4">
            {/* Current Rent */}
            <div>
              <label className="block text-sm font-medium text-text-secondary mb-1">
                Current Rent
              </label>
              <p className="text-sm text-text-primary">
                {currency} {currentRentAmount.toFixed(2)}
              </p>
            </div>

            {/* New End Date */}
            <div>
              <label
                htmlFor="newEndDate"
                className="block text-sm font-medium text-text-secondary mb-1"
              >
                New End Date
              </label>
              <input
                id="newEndDate"
                type="date"
                value={newEndDate}
                onChange={(e) => {
                  setNewEndDate(e.target.value);
                  setDateError('');
                }}
                min={currentEndDate}
                className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500 bg-surface-card text-text-primary ${dateError ? 'border-error-border' : 'border-border-strong'}`}
                disabled={isLoading}
              />
              {dateError && (
                <p className="mt-1 text-xs text-error-text">{dateError}</p>
              )}
              {currentEndDate && !dateError && (
                <p className="mt-1 text-xs text-text-muted">
                  Current end date: {currentEndDate}
                </p>
              )}
            </div>

            {/* Rent Adjustment Type */}
            <div>
              <label
                htmlFor="adjustmentType"
                className="block text-sm font-medium text-text-secondary mb-1"
              >
                Rent Adjustment Type
              </label>
              <select
                id="adjustmentType"
                value={adjustmentType}
                onChange={(e) =>
                  handleAdjustmentTypeChange(
                    e.target.value as RentAdjustmentType
                  )
                }
                className="w-full px-3 py-2 border border-border-strong rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500 bg-surface-card text-text-primary"
                disabled={isLoading}
              >
                {Object.entries(ADJUSTMENT_TYPE_LABELS).map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </select>
            </div>

            {/* Adjustment Value */}
            {(adjustmentType === 'FIXED_PERCENTAGE' ||
              adjustmentType === 'FIXED_AMOUNT') && (
              <div>
                <label
                  htmlFor="adjustmentValue"
                  className="block text-sm font-medium text-text-secondary mb-1"
                >
                  Adjustment Value
                  {adjustmentType === 'FIXED_PERCENTAGE' ? ' (%)' : ` (${currency})`}
                </label>
                <input
                  id="adjustmentValue"
                  type="number"
                  step="0.01"
                  value={adjustmentValue}
                  onChange={(e) => handleAdjustmentValueChange(e.target.value)}
                  className="w-full px-3 py-2 border border-border-strong rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500 bg-surface-card text-text-primary"
                  disabled={isLoading}
                  placeholder={
                    adjustmentType === 'FIXED_PERCENTAGE'
                      ? 'e.g. 3.5'
                      : 'e.g. 50.00'
                  }
                />
              </div>
            )}

            {/* New Rent Amount */}
            <div>
              <label
                htmlFor="newRentAmount"
                className="block text-sm font-medium text-text-secondary mb-1"
              >
                New Rent Amount
              </label>
              <div className="relative">
                <MoneyInput
                  id="newRentAmount"
                  value={parsedRent ?? undefined}
                  onChange={(val) =>
                    setNewRentAmount(val !== undefined ? String(val) : '')
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
              {adjustmentType !== 'NONE' && adjustmentType !== 'MANUAL' && (
                <p className="mt-1 text-xs text-text-muted">
                  Computed from current rent with {adjustmentType.toLowerCase().replace('_', ' ')}
                </p>
              )}
            </div>

            {/* Notes */}
            <div>
              <label
                htmlFor="extensionNotes"
                className="block text-sm font-medium text-text-secondary mb-1"
              >
                Notes (Optional)
              </label>
              <textarea
                id="extensionNotes"
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                rows={3}
                className="w-full px-3 py-2 border border-border-strong rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500 bg-surface-card text-text-primary resize-none"
                disabled={isLoading}
                placeholder="Additional notes about this extension..."
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
              disabled={isLoading}
            >
              {isLoading ? 'Creating...' : 'Create Extension'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
