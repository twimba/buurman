import { useState, useMemo } from 'react';
import { AlertTriangle } from 'lucide-react';
import type { RentIncreaseContractPreview } from '@/types/rentIncrease';
import type { RentIncreaseItem } from '@/types/rentIncrease';

type Strategy = 'maximum' | 'medium' | 'minimum' | 'custom';

interface PropertyAdjustmentStepProps {
  contracts: RentIncreaseContractPreview[];
  increases: RentIncreaseItem[];
  onIncreaseChange: (increases: RentIncreaseItem[]) => void;
  onNext: () => void;
  onBack: () => void;
}

function computeNewRent(currentRent: number, percentage: number): number {
  return Math.round(currentRent * (1 + percentage / 100) * 100) / 100;
}

export const PropertyAdjustmentStep = ({
  contracts,
  increases,
  onIncreaseChange,
  onNext,
  onBack,
}: PropertyAdjustmentStepProps) => {
  const [strategy, setStrategy] = useState<Strategy>('maximum');
  const [bulkEffectiveDate, setBulkEffectiveDate] = useState('');

  const strategies: { value: Strategy; label: string }[] = [
    { value: 'maximum', label: 'Maximum' },
    { value: 'medium', label: 'Medium' },
    { value: 'minimum', label: 'Minimum' },
    { value: 'custom', label: 'Custom' },
  ];

  const applyStrategy = (s: Strategy) => {
    setStrategy(s);
    if (s === 'custom') {
      return;
    }

    const updated = contracts.map((contract) => {
      const min = contract.regulationMinPercent ?? 0;
      const max = contract.regulationMaxPercent ?? 0;
      let pct: number;
      if (s === 'maximum') {
        pct = max;
      } else if (s === 'minimum') {
        pct = min;
      } else {
        pct = (min + max) / 2;
      }

      return {
        contractIdentifier: contract.contractIdentifier,
        increasePercentage: pct,
        newRentAmount: computeNewRent(contract.currentRentAmount, pct),
        effectiveDate:
          bulkEffectiveDate || contract.suggestedEffectiveDate || '',
      };
    });
    onIncreaseChange(updated);
  };

  const applyBulkDate = (date: string) => {
    setBulkEffectiveDate(date);
    const updated = increases.map((inc) => ({
      ...inc,
      effectiveDate: date,
    }));
    onIncreaseChange(updated);
  };

  const updateIncrease = (
    contractId: string,
    field: 'increasePercentage' | 'effectiveDate',
    value: string
  ) => {
    const contract = contracts.find((c) => c.contractIdentifier === contractId);
    if (!contract) {
      return;
    }

    const updated = increases.map((inc) => {
      if (inc.contractIdentifier !== contractId) {
        return inc;
      }
      if (field === 'increasePercentage') {
        const pct = parseFloat(value) || 0;
        return {
          ...inc,
          increasePercentage: pct,
          newRentAmount: computeNewRent(contract.currentRentAmount, pct),
        };
      }
      return { ...inc, effectiveDate: value };
    });
    onIncreaseChange(updated);
    setStrategy('custom');
  };

  const warnings = useMemo(() => {
    const result: Record<string, string> = {};
    contracts.forEach((contract) => {
      const inc = increases.find(
        (i) => i.contractIdentifier === contract.contractIdentifier
      );
      if (!inc) {
        return;
      }
      if (
        contract.regulationMaxPercent != null &&
        inc.increasePercentage > contract.regulationMaxPercent
      ) {
        result[contract.contractIdentifier] =
          `Exceeds maximum regulated increase of ${contract.regulationMaxPercent}%`;
      } else if (
        contract.regulationMinPercent != null &&
        inc.increasePercentage < contract.regulationMinPercent
      ) {
        result[contract.contractIdentifier] =
          `Below minimum regulated increase of ${contract.regulationMinPercent}%`;
      }
    });
    return result;
  }, [contracts, increases]);

  return (
    <div className="space-y-6">
      {/* Controls */}
      <div className="bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] p-4">
        <div className="flex flex-wrap items-end gap-4">
          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Strategy
            </label>
            <div className="flex gap-2">
              {strategies.map((s) => (
                <button
                  key={s.value}
                  onClick={() => applyStrategy(s.value)}
                  className={`px-3 py-1.5 rounded text-sm transition-colors ${
                    strategy === s.value
                      ? 'bg-[#5c7cfa] text-white'
                      : 'bg-[#f1f3f9] dark:bg-[#1e2130] text-[#3d4463] dark:text-[#c4c8db] hover:bg-[#e8ecf4] dark:hover:bg-[#3a3f54]'
                  }`}
                >
                  {s.label}
                </button>
              ))}
            </div>
          </div>
          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Bulk Effective Date
            </label>
            <input
              type="date"
              value={bulkEffectiveDate}
              onChange={(e) => applyBulkDate(e.target.value)}
              className="px-3 py-1.5 rounded border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#14161f] text-[#1a1d2e] dark:text-[#eef0f6] text-sm"
            />
          </div>
        </div>
      </div>

      {/* Table */}
      <div className="bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] overflow-hidden">
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
                  Regulated Range
                </th>
                <th className="text-right px-4 py-3 font-semibold text-[#3d4463] dark:text-[#c4c8db]">
                  Increase %
                </th>
                <th className="text-right px-4 py-3 font-semibold text-[#3d4463] dark:text-[#c4c8db]">
                  New Rent
                </th>
                <th className="text-left px-4 py-3 font-semibold text-[#3d4463] dark:text-[#c4c8db]">
                  Effective Date
                </th>
                <th className="px-4 py-3"></th>
              </tr>
            </thead>
            <tbody>
              {contracts.map((contract) => {
                const inc = increases.find(
                  (i) => i.contractIdentifier === contract.contractIdentifier
                );
                const warning = warnings[contract.contractIdentifier];

                return (
                  <tr
                    key={contract.contractIdentifier}
                    className="border-b border-[#e2e6f0] dark:border-[#2a2e3f] last:border-b-0 hover:bg-[#f8f9fc] dark:hover:bg-[#1a1c28]"
                  >
                    <td className="px-4 py-3">
                      <p className="font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                        {contract.propertyName}
                      </p>
                      <p className="text-xs text-[#6b7194] dark:text-[#8b90a8]">
                        {contract.propertyAddress}
                      </p>
                    </td>
                    <td className="px-4 py-3 text-right text-[#1a1d2e] dark:text-[#eef0f6]">
                      {contract.currency}{' '}
                      {contract.currentRentAmount.toLocaleString(undefined, {
                        minimumFractionDigits: 2,
                        maximumFractionDigits: 2,
                      })}
                    </td>
                    <td className="px-4 py-3 text-right text-[#6b7194] dark:text-[#8b90a8]">
                      {contract.regulationMinPercent != null &&
                      contract.regulationMaxPercent != null
                        ? `${contract.regulationMinPercent}% - ${contract.regulationMaxPercent}%`
                        : '-'}
                    </td>
                    <td className="px-4 py-3 text-right">
                      <input
                        type="number"
                        step="0.1"
                        value={inc?.increasePercentage ?? 0}
                        onChange={(e) =>
                          updateIncrease(
                            contract.contractIdentifier,
                            'increasePercentage',
                            e.target.value
                          )
                        }
                        className="w-20 px-2 py-1 rounded border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#14161f] text-right text-[#1a1d2e] dark:text-[#eef0f6]"
                      />
                    </td>
                    <td className="px-4 py-3 text-right font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                      {contract.currency}{' '}
                      {(
                        inc?.newRentAmount ?? contract.currentRentAmount
                      ).toLocaleString(undefined, {
                        minimumFractionDigits: 2,
                        maximumFractionDigits: 2,
                      })}
                    </td>
                    <td className="px-4 py-3">
                      <input
                        type="date"
                        value={inc?.effectiveDate ?? ''}
                        onChange={(e) =>
                          updateIncrease(
                            contract.contractIdentifier,
                            'effectiveDate',
                            e.target.value
                          )
                        }
                        className="px-2 py-1 rounded border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#14161f] text-[#1a1d2e] dark:text-[#eef0f6] text-sm"
                      />
                    </td>
                    <td className="px-4 py-3">
                      {warning && (
                        <div className="group relative">
                          <AlertTriangle className="h-4 w-4 text-amber-500" />
                          <div className="hidden group-hover:block absolute right-0 top-6 z-10 w-56 p-2 rounded bg-amber-50 dark:bg-amber-900/30 border border-amber-200 dark:border-amber-800 text-xs text-amber-700 dark:text-amber-400 shadow-lg">
                            {warning}
                          </div>
                        </div>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {/* Navigation */}
      <div className="flex justify-between">
        <button
          onClick={onBack}
          className="px-6 py-2 rounded border border-[#e2e6f0] dark:border-[#2a2e3f] text-[#3d4463] dark:text-[#c4c8db] hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] transition-colors"
        >
          Back
        </button>
        <button
          onClick={onNext}
          className="bg-[#5c7cfa] text-white px-6 py-2 rounded hover:bg-[#4c6ef5] transition-colors"
        >
          Next: Review
        </button>
      </div>
    </div>
  );
};
