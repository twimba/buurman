import { useState, useMemo } from 'react';
import {
  AlertTriangle,
  TrendingUp,
  TrendingDown,
  Minus,
  ArrowRight,
  CalendarDays,
  Sparkles,
  ChevronsUp,
  ChevronsDown,
  Equal,
  SlidersHorizontal,
} from 'lucide-react';
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

const formatMoney = (amount: number, currency: string) =>
  amount.toLocaleString(undefined, {
    style: 'currency',
    currency,
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });

const formatPct = (pct: number) => {
  const prefix = pct > 0 ? '+' : '';
  return `${prefix}${pct.toFixed(1)}%`;
};

const pctColor = (pct: number) => {
  if (pct > 0) {
    return 'text-emerald-600 dark:text-emerald-400';
  }
  if (pct < 0) {
    return 'text-red-500 dark:text-red-400';
  }
  return 'text-[#6b7194] dark:text-[#8b90a8]';
};

const pctBgColor = (pct: number) => {
  if (pct > 0) {
    return 'bg-emerald-50 dark:bg-emerald-500/10 ring-1 ring-emerald-200 dark:ring-emerald-500/25';
  }
  if (pct < 0) {
    return 'bg-red-50 dark:bg-red-500/10 ring-1 ring-red-200 dark:ring-red-500/25';
  }
  return 'bg-[#f1f3f9] dark:bg-[#1e2130] ring-1 ring-[#e2e6f0] dark:ring-[#2a2e3f]';
};

const PctIcon = ({ pct }: { pct: number }) => {
  if (pct > 0) {
    return <TrendingUp className="h-3.5 w-3.5" />;
  }
  if (pct < 0) {
    return <TrendingDown className="h-3.5 w-3.5" />;
  }
  return <Minus className="h-3.5 w-3.5" />;
};

const TH =
  'px-5 py-3.5 text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8]';

export const PropertyAdjustmentStep = ({
  contracts,
  increases,
  onIncreaseChange,
  onNext,
  onBack,
}: PropertyAdjustmentStepProps) => {
  const [strategy, setStrategy] = useState<Strategy>('maximum');
  const [bulkEffectiveDate, setBulkEffectiveDate] = useState('');

  const strategies: {
    value: Strategy;
    label: string;
    icon: React.ReactNode;
    description: string;
  }[] = [
    {
      value: 'maximum',
      label: 'Maximum',
      icon: <ChevronsUp className="h-3.5 w-3.5" />,
      description: 'Apply max regulated %',
    },
    {
      value: 'medium',
      label: 'Medium',
      icon: <Equal className="h-3.5 w-3.5" />,
      description: 'Average of min & max',
    },
    {
      value: 'minimum',
      label: 'Minimum',
      icon: <ChevronsDown className="h-3.5 w-3.5" />,
      description: 'Apply min regulated %',
    },
    {
      value: 'custom',
      label: 'Custom',
      icon: <SlidersHorizontal className="h-3.5 w-3.5" />,
      description: 'Set per property',
    },
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
        pct = Math.round(((min + max) / 2) * 10) / 10;
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

  // Summary stats
  const summary = useMemo(() => {
    const active = increases.filter((i) => i.increasePercentage !== 0);
    const avgPct =
      active.length > 0
        ? active.reduce((sum, i) => sum + i.increasePercentage, 0) /
          active.length
        : 0;
    return {
      activeCount: active.length,
      avgPct,
      warningCount: Object.keys(warnings).length,
    };
  }, [increases, warnings]);

  return (
    <div className="space-y-5">
      {/* Controls toolbar */}
      <div className="bg-white dark:bg-[#14161f] rounded-xl border border-[#e2e6f0] dark:border-[#2a2e3f] shadow-sm">
        <div className="p-5">
          <div className="flex flex-wrap items-start gap-6">
            {/* Strategy selector */}
            <div className="flex-1 min-w-[280px]">
              <label className="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8] mb-2.5">
                <Sparkles className="h-3.5 w-3.5" />
                Adjustment Strategy
              </label>
              <div className="grid grid-cols-4 gap-1.5 p-1 rounded-lg bg-[#f1f3f9] dark:bg-[#0c0d14]">
                {strategies.map((s) => (
                  <button
                    key={s.value}
                    onClick={() => applyStrategy(s.value)}
                    className={`relative inline-flex items-center justify-center gap-1.5 px-3 py-2 rounded-md text-xs font-medium transition-all duration-150 ${
                      strategy === s.value
                        ? 'bg-white dark:bg-[#1e2130] text-[#1a1d2e] dark:text-[#eef0f6] shadow-sm ring-1 ring-[#e2e6f0] dark:ring-[#2a2e3f]'
                        : 'text-[#6b7194] dark:text-[#8b90a8] hover:text-[#3d4463] dark:hover:text-[#c4c8db]'
                    }`}
                  >
                    {s.icon}
                    {s.label}
                  </button>
                ))}
              </div>
            </div>

            {/* Bulk date */}
            <div className="min-w-[200px]">
              <label className="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8] mb-2.5">
                <CalendarDays className="h-3.5 w-3.5" />
                Bulk Effective Date
              </label>
              <input
                type="date"
                value={bulkEffectiveDate}
                onChange={(e) => applyBulkDate(e.target.value)}
                className="w-full h-9 px-3 rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#0c0d14] text-[#1a1d2e] dark:text-[#eef0f6] text-sm focus:outline-none focus:border-[#5c7cfa] focus:ring-2 focus:ring-[#5c7cfa]/20 transition-colors"
              />
            </div>
          </div>
        </div>

        {/* Mini summary bar */}
        <div className="flex items-center gap-5 px-5 py-2.5 border-t border-[#e2e6f0] dark:border-[#2a2e3f] bg-[#f8f9fc] dark:bg-[#0c0d14] rounded-b-xl">
          <span className="text-xs text-[#6b7194] dark:text-[#8b90a8]">
            <span className="font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
              {summary.activeCount}
            </span>{' '}
            of {contracts.length} contracts adjusted
          </span>
          <span className="w-px h-3.5 bg-[#e2e6f0] dark:bg-[#2a2e3f]" />
          <span className="text-xs text-[#6b7194] dark:text-[#8b90a8]">
            Avg change:{' '}
            <span className={`font-semibold ${pctColor(summary.avgPct)}`}>
              {formatPct(summary.avgPct)}
            </span>
          </span>
          {summary.warningCount > 0 && (
            <>
              <span className="w-px h-3.5 bg-[#e2e6f0] dark:bg-[#2a2e3f]" />
              <span className="inline-flex items-center gap-1 text-xs text-amber-600 dark:text-amber-400">
                <AlertTriangle className="h-3 w-3" />
                <span className="font-semibold">
                  {summary.warningCount}
                </span>{' '}
                warning{summary.warningCount !== 1 ? 's' : ''}
              </span>
            </>
          )}
        </div>
      </div>

      {/* Table */}
      <div className="bg-white dark:bg-[#14161f] rounded-xl border border-[#e2e6f0] dark:border-[#2a2e3f] shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b-2 border-[#e2e6f0] dark:border-[#2a2e3f] bg-[#f8f9fc] dark:bg-[#0c0d14]">
                <th className={`${TH} text-left`}>Property</th>
                <th className={`${TH} text-right`}>Current Rent</th>
                <th className={`${TH} text-center`}>Regulated Range</th>
                <th className={`${TH} text-center`}>Adjustment</th>
                <th className={`${TH} text-right`}>New Rent</th>
                <th className={`${TH} text-left`}>Effective Date</th>
                <th className={`${TH} w-10`} />
              </tr>
            </thead>
            <tbody className="divide-y divide-[#e2e6f0] dark:divide-[#2a2e3f]">
              {contracts.map((contract) => {
                const inc = increases.find(
                  (i) => i.contractIdentifier === contract.contractIdentifier
                );
                const warning = warnings[contract.contractIdentifier];
                const pct = inc?.increasePercentage ?? 0;
                const newRent =
                  inc?.newRentAmount ?? contract.currentRentAmount;
                const diff = newRent - contract.currentRentAmount;

                return (
                  <tr
                    key={contract.contractIdentifier}
                    className={`group transition-colors ${
                      warning
                        ? 'bg-amber-50/50 dark:bg-amber-500/[0.03] hover:bg-amber-50 dark:hover:bg-amber-500/[0.06]'
                        : 'hover:bg-[#f8f9fc] dark:hover:bg-[#1a1c28]'
                    }`}
                  >
                    {/* Property */}
                    <td className="px-5 py-4">
                      <p className="font-medium text-[#1a1d2e] dark:text-[#eef0f6] leading-tight">
                        {contract.propertyName}
                      </p>
                      <p className="text-xs text-[#9ca0b8] dark:text-[#5c6180] mt-0.5">
                        {contract.propertyAddress}
                      </p>
                    </td>

                    {/* Current rent */}
                    <td className="px-5 py-4 text-right">
                      <span className="font-mono text-sm text-[#3d4463] dark:text-[#c4c8db]">
                        {formatMoney(
                          contract.currentRentAmount,
                          contract.currency
                        )}
                      </span>
                    </td>

                    {/* Regulated range */}
                    <td className="px-5 py-4 text-center">
                      {contract.regulationMinPercent != null &&
                      contract.regulationMaxPercent != null ? (
                        <span className="inline-flex items-center justify-center gap-1 w-[120px] rounded-full px-2.5 py-1 text-xs font-medium bg-[#f1f3f9] dark:bg-[#1e2130] text-[#6b7194] dark:text-[#8b90a8] ring-1 ring-[#e2e6f0] dark:ring-[#2a2e3f]">
                          <span className="w-[32px] text-right">
                            {contract.regulationMinPercent}%
                          </span>
                          <ArrowRight className="h-3 w-3 flex-shrink-0 text-[#9ca0b8]" />
                          <span className="w-[32px] text-left">
                            {contract.regulationMaxPercent}%
                          </span>
                        </span>
                      ) : (
                        <span className="inline-flex items-center justify-center w-[120px] text-xs text-[#9ca0b8] dark:text-[#5c6180]">
                          No data
                        </span>
                      )}
                    </td>

                    {/* Adjustment — editable pill */}
                    <td className="px-5 py-4">
                      <div className="flex items-center justify-center">
                        <label
                          className={`inline-flex items-center gap-1 rounded-full pl-2 pr-1 py-0.5 text-xs font-semibold cursor-text ${pctColor(pct)} ${pctBgColor(pct)}`}
                        >
                          <PctIcon pct={pct} />
                          <div className="relative">
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
                              className="w-[48px] h-5 bg-transparent text-right text-xs font-semibold text-inherit focus:outline-none [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none"
                            />
                            <span className="pointer-events-none text-xs font-semibold">
                              %
                            </span>
                          </div>
                        </label>
                      </div>
                    </td>

                    {/* New rent */}
                    <td className="px-5 py-4 text-right">
                      <div>
                        <span className="font-mono text-sm font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                          {formatMoney(newRent, contract.currency)}
                        </span>
                        {pct !== 0 && (
                          <p
                            className={`text-xs font-medium mt-0.5 ${pctColor(pct)}`}
                          >
                            {diff > 0 ? '+' : ''}
                            {formatMoney(diff, contract.currency)}/mo
                          </p>
                        )}
                      </div>
                    </td>

                    {/* Effective date */}
                    <td className="px-5 py-4">
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
                        className="h-8 px-2.5 rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#0c0d14] text-[#1a1d2e] dark:text-[#eef0f6] text-sm focus:outline-none focus:border-[#5c7cfa] focus:ring-2 focus:ring-[#5c7cfa]/20 transition-colors"
                      />
                    </td>

                    {/* Warning */}
                    <td className="px-3 py-4">
                      {warning && (
                        <div className="group/tip relative flex items-center justify-center">
                          <div className="p-1 rounded-full bg-amber-100 dark:bg-amber-500/20">
                            <AlertTriangle className="h-3.5 w-3.5 text-amber-600 dark:text-amber-400" />
                          </div>
                          <div className="hidden group-hover/tip:block absolute right-0 top-full mt-1 z-10 w-60 p-3 rounded-lg bg-white dark:bg-[#1e2130] border border-amber-200 dark:border-amber-500/30 text-xs text-amber-700 dark:text-amber-300 shadow-lg shadow-amber-500/10">
                            <div className="flex items-start gap-2">
                              <AlertTriangle className="h-3.5 w-3.5 mt-0.5 flex-shrink-0" />
                              <span>{warning}</span>
                            </div>
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
      <div className="flex justify-between pt-1">
        <button
          onClick={onBack}
          className="inline-flex items-center h-10 px-5 rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] bg-white dark:bg-[#14161f] hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] hover:border-[#c9cfd9] dark:hover:border-[#3a3f54] shadow-sm transition-all"
        >
          Back
        </button>
        <button
          onClick={onNext}
          className="inline-flex items-center gap-2 h-10 px-6 rounded-lg text-sm font-medium bg-gradient-to-b from-[#5c7cfa] to-[#4c6ef5] text-white border border-[#4263eb] shadow-sm shadow-[#5c7cfa]/20 hover:from-[#4c6ef5] hover:to-[#4263eb] hover:shadow-md hover:shadow-[#5c7cfa]/30 transition-all"
        >
          Next: Review
          <ArrowRight className="h-4 w-4" />
        </button>
      </div>
    </div>
  );
};
