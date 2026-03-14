import { useMemo } from 'react';
import type { RentIncreaseContractPreview } from '@/types/rentIncrease';
import type { RentIncreaseItem } from '@/types/rentIncrease';

interface ReviewStepProps {
  contracts: RentIncreaseContractPreview[];
  increases: RentIncreaseItem[];
  year: number;
  onBack: () => void;
  onApply: () => void;
  isApplying: boolean;
}

export const ReviewStep = ({
  contracts,
  increases,
  year,
  onBack,
  onApply,
  isApplying,
}: ReviewStepProps) => {
  const changedIncreases = useMemo(
    () => increases.filter((inc) => inc.increasePercentage !== 0),
    [increases]
  );

  const unchangedContracts = useMemo(
    () =>
      contracts.filter(
        (c) =>
          !changedIncreases.some(
            (inc) => inc.contractIdentifier === c.contractIdentifier
          )
      ),
    [contracts, changedIncreases]
  );

  const totalsByCurrency = useMemo(() => {
    const totals: Record<
      string,
      { previousTotal: number; newTotal: number; count: number }
    > = {};

    changedIncreases.forEach((inc) => {
      const contract = contracts.find(
        (c) => c.contractIdentifier === inc.contractIdentifier
      );
      if (!contract) {
        return;
      }
      if (!totals[contract.currency]) {
        totals[contract.currency] = {
          previousTotal: 0,
          newTotal: 0,
          count: 0,
        };
      }
      totals[contract.currency].previousTotal += contract.currentRentAmount;
      totals[contract.currency].newTotal += inc.newRentAmount;
      totals[contract.currency].count += 1;
    });

    return totals;
  }, [changedIncreases, contracts]);

  const formatMoney = (amount: number, currency?: string) =>
    currency
      ? amount.toLocaleString(undefined, {
          style: 'currency',
          currency,
          minimumFractionDigits: 2,
          maximumFractionDigits: 2,
        })
      : amount.toLocaleString(undefined, {
          minimumFractionDigits: 2,
          maximumFractionDigits: 2,
        });

  return (
    <div className="space-y-6">
      {/* Portfolio Totals */}
      <div className="bg-surface-card rounded-lg border border-border-default p-6">
        <h3 className="text-lg font-semibold text-text-primary mb-4">
          Portfolio Totals for {year}
        </h3>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {Object.entries(totalsByCurrency).map(([currency, totals]) => (
            <div
              key={currency}
              className="p-4 rounded-lg bg-surface-page dark:bg-surface-card"
            >
              <p className="text-sm text-text-secondary mb-2">
                {currency} ({totals.count}{' '}
                {totals.count === 1 ? 'contract' : 'contracts'})
              </p>
              <div className="space-y-1">
                <div className="flex justify-between">
                  <span className="text-sm text-text-secondary">Previous</span>
                  <span className="font-medium text-text-primary">
                    {formatMoney(totals.previousTotal, currency)}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-sm text-text-secondary">New</span>
                  <span className="font-semibold text-text-primary">
                    {formatMoney(totals.newTotal, currency)}
                  </span>
                </div>
                <div className="flex justify-between border-t border-border-default pt-1">
                  <span className="text-sm text-text-secondary">Increase</span>
                  <span
                    className={`font-semibold ${
                      totals.newTotal >= totals.previousTotal
                        ? 'text-success-text'
                        : 'text-error-text'
                    }`}
                  >
                    {totals.newTotal >= totals.previousTotal ? '+' : ''}
                    {formatMoney(
                      totals.newTotal - totals.previousTotal,
                      currency
                    )}
                  </span>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Changed Contracts */}
      <div className="bg-surface-card rounded-lg border border-border-default overflow-hidden">
        <div className="px-4 py-3 border-b border-border-default">
          <h3 className="font-semibold text-text-primary">
            Contracts to Update ({changedIncreases.length})
          </h3>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border-default">
                <th className="text-left px-4 py-3 font-semibold text-text-secondary">
                  Property
                </th>
                <th className="text-right px-4 py-3 font-semibold text-text-secondary">
                  Current Rent
                </th>
                <th className="text-right px-4 py-3 font-semibold text-text-secondary">
                  Increase
                </th>
                <th className="text-right px-4 py-3 font-semibold text-text-secondary">
                  New Rent
                </th>
                <th className="text-left px-4 py-3 font-semibold text-text-secondary">
                  Effective Date
                </th>
              </tr>
            </thead>
            <tbody>
              {changedIncreases.map((inc) => {
                const contract = contracts.find(
                  (c) => c.contractIdentifier === inc.contractIdentifier
                );
                if (!contract) {
                  return null;
                }
                return (
                  <tr
                    key={inc.contractIdentifier}
                    className="border-b border-border-default last:border-b-0"
                  >
                    <td className="px-4 py-3">
                      <p className="font-medium text-text-primary">
                        {contract.propertyName}
                      </p>
                      <p className="text-xs text-text-secondary">
                        {contract.propertyAddress}
                      </p>
                    </td>
                    <td className="px-4 py-3 text-right text-text-secondary">
                      {formatMoney(
                        contract.currentRentAmount,
                        contract.currency
                      )}
                    </td>
                    <td
                      className={`px-4 py-3 text-right font-medium ${
                        inc.increasePercentage > 0
                          ? 'text-success-text'
                          : inc.increasePercentage < 0
                            ? 'text-error-text'
                            : 'text-text-secondary'
                      }`}
                    >
                      {inc.increasePercentage > 0 ? '+' : ''}
                      {inc.increasePercentage.toFixed(1)}%
                    </td>
                    <td className="px-4 py-3 text-right font-semibold text-text-primary">
                      {formatMoney(inc.newRentAmount, contract.currency)}
                    </td>
                    <td className="px-4 py-3 text-text-secondary">
                      {inc.effectiveDate}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {/* Unchanged Contracts */}
      {unchangedContracts.length > 0 && (
        <div className="bg-surface-card rounded-lg border border-border-default p-4">
          <h3 className="font-semibold text-text-primary mb-3">
            Unchanged Contracts ({unchangedContracts.length})
          </h3>
          <div className="space-y-2">
            {unchangedContracts.map((contract) => (
              <div
                key={contract.contractIdentifier}
                className="flex justify-between items-center py-2 px-3 rounded bg-surface-page dark:bg-surface-card"
              >
                <div>
                  <p className="text-sm font-medium text-text-secondary">
                    {contract.propertyName}
                  </p>
                  <p className="text-xs text-text-muted">
                    {contract.propertyAddress}
                  </p>
                </div>
                <span className="text-sm text-text-secondary">
                  {formatMoney(contract.currentRentAmount, contract.currency)}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Navigation */}
      <div className="flex justify-between">
        <button
          onClick={onBack}
          className="px-6 py-2 rounded border border-border-default text-text-secondary hover:bg-surface-inset transition-colors"
        >
          Back
        </button>
        <button
          onClick={onApply}
          disabled={isApplying || changedIncreases.length === 0}
          className="bg-success-text text-white px-6 py-2 rounded hover:opacity-90 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {isApplying
            ? 'Applying...'
            : `Apply ${changedIncreases.length} Adjustment${changedIncreases.length !== 1 ? 's' : ''}`}
        </button>
      </div>
    </div>
  );
};
