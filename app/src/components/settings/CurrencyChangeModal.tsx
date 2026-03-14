import { useState } from 'react';
import { AlertTriangle, X } from 'lucide-react';
import { useChangeCurrency } from '@/hooks/useOnboarding';
import { useCurrencies, getCurrencySymbol } from '@/hooks/useCurrencies';
import { CurrencySelector } from '@/components/common/CurrencySelector';
import { getCurrencyFlag } from '@/utils/currencyFlags';

interface CurrencyChangeModalProps {
  teamIdentifier: string;
  currentCurrency: string;
  onClose: () => void;
  onSuccess: () => void;
}

type ChangeMode = 'RELABEL' | 'CONVERT';

export const CurrencyChangeModal = ({
  teamIdentifier,
  currentCurrency,
  onClose,
  onSuccess,
}: CurrencyChangeModalProps) => {
  const [newCurrency, setNewCurrency] = useState('');
  const [mode, setMode] = useState<ChangeMode>('RELABEL');
  const [conversionRate, setConversionRate] = useState('');
  const [confirmed, setConfirmed] = useState(false);
  const [showConfirmation, setShowConfirmation] = useState(false);

  const changeMutation = useChangeCurrency(teamIdentifier);
  const { data: currencies } = useCurrencies();

  const handleSubmit = () => {
    if (!showConfirmation) {
      setShowConfirmation(true);
      return;
    }

    changeMutation.mutate(
      {
        newCurrency,
        mode,
        conversionRate:
          mode === 'CONVERT' ? parseFloat(conversionRate) : undefined,
      },
      {
        onSuccess: () => {
          onSuccess();
          onClose();
        },
      }
    );
  };

  const canSubmit =
    newCurrency &&
    newCurrency !== currentCurrency &&
    (mode === 'RELABEL' ||
      (mode === 'CONVERT' && parseFloat(conversionRate) > 0));

  return (
    <div className="fixed inset-0 bg-black/40 backdrop-blur-sm z-50 flex items-center justify-center p-4">
      <div className="bg-surface-card rounded-lg shadow-xl max-w-md w-full">
        <div className="flex items-center justify-between px-6 pt-6">
          <h3 className="text-lg font-semibold text-text-primary">
            Change Team Currency
          </h3>
          <button
            onClick={onClose}
            className="text-text-secondary hover:text-text-primary"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="px-6 py-4 space-y-4">
          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">
              Current Currency
            </label>
            <div className="px-3 py-2 bg-surface-inset rounded-lg text-text-primary font-medium flex items-center gap-2">
              <span>{getCurrencyFlag(currentCurrency)}</span>
              {getCurrencySymbol(currencies, currentCurrency)} {currentCurrency}
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">
              New Currency
            </label>
            <CurrencySelector
              value={newCurrency || undefined}
              onChange={setNewCurrency}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-text-secondary mb-2">
              Change Mode
            </label>
            <div className="space-y-2">
              <button
                onClick={() => setMode('RELABEL')}
                className={`w-full text-left px-4 py-3 rounded-lg border transition-colors ${
                  mode === 'RELABEL'
                    ? 'border-primary-500 bg-primary-500/5'
                    : 'border-border-default'
                }`}
              >
                <div className="font-medium text-sm text-text-primary">
                  Relabel Only
                </div>
                <div className="text-xs text-text-secondary mt-0.5">
                  Change currency codes without modifying amounts. Use when
                  amounts were already entered in the target currency.
                </div>
              </button>
              <button
                onClick={() => setMode('CONVERT')}
                className={`w-full text-left px-4 py-3 rounded-lg border transition-colors ${
                  mode === 'CONVERT'
                    ? 'border-primary-500 bg-primary-500/5'
                    : 'border-border-default'
                }`}
              >
                <div className="font-medium text-sm text-text-primary">
                  Convert at Fixed Rate
                </div>
                <div className="text-xs text-text-secondary mt-0.5">
                  Multiply all amounts by a conversion rate. All existing
                  financial data will be recalculated.
                </div>
              </button>
            </div>
          </div>

          {mode === 'CONVERT' && (
            <div>
              <label className="block text-sm font-medium text-text-secondary mb-1">
                Conversion Rate (1 {currentCurrency} = {conversionRate || '?'}{' '}
                {newCurrency || '???'})
              </label>
              <input
                type="number"
                step="0.000001"
                min="0.000001"
                placeholder="e.g. 1.08"
                value={conversionRate}
                onChange={(e) => setConversionRate(e.target.value)}
                className="w-full px-3 py-2 border border-border-strong rounded-lg bg-surface-card text-text-primary focus:outline-none focus:ring-1 focus:ring-primary-500"
              />
            </div>
          )}

          {showConfirmation && (
            <div className="bg-warning-bg border border-warning-border rounded-lg p-4">
              <div className="flex items-start gap-3">
                <AlertTriangle className="h-5 w-5 text-warning-text flex-shrink-0 mt-0.5" />
                <div>
                  <div className="text-sm font-medium text-warning-text">
                    This action is irreversible
                  </div>
                  <div className="text-xs text-warning-text mt-1">
                    {mode === 'RELABEL'
                      ? `All currency codes will be changed from ${currentCurrency} to ${newCurrency}. Amounts will not be modified.`
                      : `All amounts will be multiplied by ${conversionRate} and currency codes changed from ${currentCurrency} to ${newCurrency}.`}
                  </div>
                  <label className="flex items-center gap-2 mt-3 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={confirmed}
                      onChange={(e) => setConfirmed(e.target.checked)}
                      className="rounded"
                    />
                    <span className="text-xs text-warning-text">
                      I understand this cannot be undone
                    </span>
                  </label>
                </div>
              </div>
            </div>
          )}
        </div>

        <div className="px-6 pb-6 flex justify-end gap-3">
          <button
            onClick={onClose}
            className="px-4 py-2 text-sm font-medium text-text-secondary hover:text-text-primary transition-colors"
          >
            Cancel
          </button>
          <button
            onClick={handleSubmit}
            disabled={
              !canSubmit ||
              (showConfirmation && !confirmed) ||
              changeMutation.isPending
            }
            className="px-4 py-2 bg-warning-text hover:opacity-90 text-white rounded-lg text-sm font-medium transition-colors disabled:opacity-50"
          >
            {changeMutation.isPending
              ? 'Changing...'
              : showConfirmation
                ? 'Confirm Change'
                : 'Change Currency'}
          </button>
        </div>
      </div>
    </div>
  );
};
