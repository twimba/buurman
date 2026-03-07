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

  const formatMoney = (amount: number) =>
    amount.toLocaleString(undefined, {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    });

  return (
    <div className="space-y-6">
      {/* Portfolio Totals */}
      <div className="bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] p-6">
        <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
          Portfolio Totals for {year}
        </h3>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {Object.entries(totalsByCurrency).map(([currency, totals]) => (
            <div
              key={currency}
              className="p-4 rounded-lg bg-[#f8f9fc] dark:bg-[#1a1c28]"
            >
              <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] mb-2">
                {currency} ({totals.count}{' '}
                {totals.count === 1 ? 'contract' : 'contracts'})
              </p>
              <div className="space-y-1">
                <div className="flex justify-between">
                  <span className="text-sm text-[#3d4463] dark:text-[#c4c8db]">
                    Previous
                  </span>
                  <span className="font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                    {formatMoney(totals.previousTotal)}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-sm text-[#3d4463] dark:text-[#c4c8db]">
                    New
                  </span>
                  <span className="font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                    {formatMoney(totals.newTotal)}
                  </span>
                </div>
                <div className="flex justify-between border-t border-[#e2e6f0] dark:border-[#2a2e3f] pt-1">
                  <span className="text-sm text-[#3d4463] dark:text-[#c4c8db]">
                    Increase
                  </span>
                  <span
                    className={`font-semibold ${
                      totals.newTotal >= totals.previousTotal
                        ? 'text-emerald-600 dark:text-emerald-400'
                        : 'text-red-500 dark:text-red-400'
                    }`}
                  >
                    {totals.newTotal >= totals.previousTotal ? '+' : ''}
                    {formatMoney(totals.newTotal - totals.previousTotal)}
                  </span>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Changed Contracts */}
      <div className="bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] overflow-hidden">
        <div className="px-4 py-3 border-b border-[#e2e6f0] dark:border-[#2a2e3f]">
          <h3 className="font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
            Contracts to Update ({changedIncreases.length})
          </h3>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-[#e2e6f0] dark:border-[#2a2e3f]">
                <th className="text-left px-4 py-3 font-semibold text-[#3d4463] dark:text-[#c4c8db]">
                  Property
                </th>
                <th className="text-right px-4 py-3 font-semibold text-[#3d4463] dark:text-[#c4c8db]">
                  Current Rent
                </th>
                <th className="text-right px-4 py-3 font-semibold text-[#3d4463] dark:text-[#c4c8db]">
                  Increase
                </th>
                <th className="text-right px-4 py-3 font-semibold text-[#3d4463] dark:text-[#c4c8db]">
                  New Rent
                </th>
                <th className="text-left px-4 py-3 font-semibold text-[#3d4463] dark:text-[#c4c8db]">
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
                    className="border-b border-[#e2e6f0] dark:border-[#2a2e3f] last:border-b-0"
                  >
                    <td className="px-4 py-3">
                      <p className="font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                        {contract.propertyName}
                      </p>
                      <p className="text-xs text-[#6b7194] dark:text-[#8b90a8]">
                        {contract.propertyAddress}
                      </p>
                    </td>
                    <td className="px-4 py-3 text-right text-[#3d4463] dark:text-[#c4c8db]">
                      {contract.currency}{' '}
                      {formatMoney(contract.currentRentAmount)}
                    </td>
                    <td
                      className={`px-4 py-3 text-right font-medium ${
                        inc.increasePercentage > 0
                          ? 'text-emerald-600 dark:text-emerald-400'
                          : inc.increasePercentage < 0
                            ? 'text-red-500 dark:text-red-400'
                            : 'text-[#6b7194] dark:text-[#8b90a8]'
                      }`}
                    >
                      {inc.increasePercentage > 0 ? '+' : ''}
                      {inc.increasePercentage.toFixed(1)}%
                    </td>
                    <td className="px-4 py-3 text-right font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                      {contract.currency} {formatMoney(inc.newRentAmount)}
                    </td>
                    <td className="px-4 py-3 text-[#3d4463] dark:text-[#c4c8db]">
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
        <div className="bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] p-4">
          <h3 className="font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-3">
            Unchanged Contracts ({unchangedContracts.length})
          </h3>
          <div className="space-y-2">
            {unchangedContracts.map((contract) => (
              <div
                key={contract.contractIdentifier}
                className="flex justify-between items-center py-2 px-3 rounded bg-[#f8f9fc] dark:bg-[#1a1c28]"
              >
                <div>
                  <p className="text-sm font-medium text-[#3d4463] dark:text-[#c4c8db]">
                    {contract.propertyName}
                  </p>
                  <p className="text-xs text-[#9ca0b8] dark:text-[#5c6180]">
                    {contract.propertyAddress}
                  </p>
                </div>
                <span className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                  {contract.currency} {formatMoney(contract.currentRentAmount)}
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
          className="px-6 py-2 rounded border border-[#e2e6f0] dark:border-[#2a2e3f] text-[#3d4463] dark:text-[#c4c8db] hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] transition-colors"
        >
          Back
        </button>
        <button
          onClick={onApply}
          disabled={isApplying || changedIncreases.length === 0}
          className="bg-green-600 text-white px-6 py-2 rounded hover:bg-green-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {isApplying
            ? 'Applying...'
            : `Apply ${changedIncreases.length} Increase${changedIncreases.length !== 1 ? 's' : ''}`}
        </button>
      </div>
    </div>
  );
};
