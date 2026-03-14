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
  Info,
  CheckCircle2,
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
  appliedContractIds?: Set<string>;
}

function computeNewRent(currentRent: number, percentage: number): number {
  return Math.round(currentRent * (1 + percentage / 100) * 100) / 100;
}

function decimalPlaces(n: number): number {
  const dot = n.toString().indexOf('.');
  return dot === -1 ? 0 : n.toString().length - dot - 1;
}

function computeMedium(min: number, max: number): number {
  const precision = Math.max(decimalPlaces(min), decimalPlaces(max));
  const factor = Math.pow(10, precision);
  return Math.round(((min + max) / 2) * factor) / factor;
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
    return 'text-success-text';
  }
  if (pct < 0) {
    return 'text-error-text';
  }
  return 'text-text-secondary';
};

const pctBgColor = (pct: number) => {
  if (pct > 0) {
    return 'bg-success-bg ring-1 ring-success-border';
  }
  if (pct < 0) {
    return 'bg-error-bg ring-1 ring-error-border';
  }
  return 'bg-surface-inset ring-1 ring-border-default dark:ring-border-strong';
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
  'px-5 py-3.5 text-xs font-semibold uppercase tracking-wider text-text-secondary';

export const PropertyAdjustmentStep = ({
  contracts,
  increases,
  onIncreaseChange,
  onNext,
  onBack,
  appliedContractIds = new Set(),
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
        pct = computeMedium(min, max);
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
    const today = new Date().toISOString().split('T')[0];
    const active = increases.filter((i) => i.increasePercentage !== 0);
    const avgPct =
      active.length > 0
        ? active.reduce((sum, i) => sum + i.increasePercentage, 0) /
          active.length
        : 0;
    const retroactiveCount = increases.filter(
      (i) => i.effectiveDate && i.effectiveDate < today
    ).length;
    return {
      activeCount: active.length,
      avgPct,
      warningCount: Object.keys(warnings).length,
      retroactiveCount,
    };
  }, [increases, warnings]);

  return (
    <div className="space-y-5">
      {/* Controls toolbar */}
      <div className="bg-surface-card rounded-lg border border-border-default shadow-sm">
        <div className="p-5">
          <div className="flex flex-wrap items-start gap-6">
            {/* Strategy selector */}
            <div className="flex-1 min-w-[280px]">
              <label className="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider text-text-secondary mb-2.5">
                <Sparkles className="h-3.5 w-3.5" />
                Adjustment Strategy
              </label>
              <div className="grid grid-cols-4 gap-1.5 p-1 rounded-lg bg-surface-inset">
                {strategies.map((s) => (
                  <button
                    key={s.value}
                    onClick={() => applyStrategy(s.value)}
                    className={`relative inline-flex items-center justify-center gap-1.5 px-3 py-2 rounded-md text-xs font-medium transition-all duration-150 ${
                      strategy === s.value
                        ? 'bg-surface-card text-text-primary shadow-sm ring-1 ring-border-default dark:ring-border-strong'
                        : 'text-text-secondary hover:text-text-secondary'
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
              <label className="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider text-text-secondary mb-2.5">
                <CalendarDays className="h-3.5 w-3.5" />
                Bulk Effective Date
              </label>
              <input
                type="date"
                value={bulkEffectiveDate}
                onChange={(e) => applyBulkDate(e.target.value)}
                className="w-full h-9 px-3 rounded-lg border border-border-default bg-surface-card text-text-primary text-sm focus:outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20 transition-colors"
              />
            </div>
          </div>
        </div>

        {/* Mini summary bar */}
        <div className="flex items-center gap-5 px-5 py-2.5 border-t border-border-default bg-surface-page rounded-b-xl">
          <span className="text-xs text-text-secondary">
            <span className="font-semibold text-text-primary">
              {summary.activeCount}
            </span>{' '}
            of {contracts.length} contracts adjusted
          </span>
          <span className="w-px h-3.5 bg-border-default" />
          <span className="text-xs text-text-secondary">
            Avg change:{' '}
            <span className={`font-semibold ${pctColor(summary.avgPct)}`}>
              {formatPct(summary.avgPct)}
            </span>
          </span>
          {summary.warningCount > 0 && (
            <>
              <span className="w-px h-3.5 bg-border-default" />
              <span className="inline-flex items-center gap-1 text-xs text-warning-text">
                <AlertTriangle className="h-3 w-3" />
                <span className="font-semibold">
                  {summary.warningCount}
                </span>{' '}
                warning{summary.warningCount !== 1 ? 's' : ''}
              </span>
            </>
          )}
          {summary.retroactiveCount > 0 && (
            <>
              <span className="w-px h-3.5 bg-border-default" />
              <span className="group/retro relative inline-flex items-center gap-1 text-xs text-info-text cursor-help">
                <Info className="h-3 w-3" />
                <span className="font-semibold">
                  {summary.retroactiveCount}
                </span>{' '}
                retroactive
                <span className="hidden group-hover/retro:block absolute left-0 top-full mt-1 z-10 w-64 p-2.5 rounded-lg bg-surface-card border border-info-border text-xs text-info-text shadow-lg">
                  Adjustment payments will be created for any already settled
                  payments affected by retroactive date changes.
                </span>
              </span>
            </>
          )}
        </div>
      </div>

      {/* Table */}
      <div className="bg-surface-card rounded-lg border border-border-default shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b-2 border-border-default bg-surface-page">
                <th className={`${TH} text-left`}>Property</th>
                <th className={`${TH} text-right`}>Current Rent</th>
                <th className={`${TH} text-center`}>Regulated Range</th>
                <th className={`${TH} text-center`}>Adjustment</th>
                <th className={`${TH} text-right`}>New Rent</th>
                <th className={`${TH} text-left`}>Effective Date</th>
                <th className={`${TH} w-10`} />
              </tr>
            </thead>
            <tbody className="divide-y divide-border-default">
              {contracts.map((contract) => {
                const inc = increases.find(
                  (i) => i.contractIdentifier === contract.contractIdentifier
                );
                const warning = warnings[contract.contractIdentifier];
                const pct = inc?.increasePercentage ?? 0;
                const newRent =
                  inc?.newRentAmount ?? contract.currentRentAmount;
                const diff = newRent - contract.currentRentAmount;
                const isApplied = appliedContractIds.has(
                  contract.contractIdentifier
                );

                return (
                  <tr
                    key={contract.contractIdentifier}
                    className={`group transition-colors ${
                      isApplied
                        ? 'opacity-50'
                        : warning
                          ? 'bg-warning-bg/50 hover:bg-warning-bg'
                          : 'hover:bg-surface-page dark:hover:bg-surface-card'
                    }`}
                  >
                    {/* Property */}
                    <td className="px-5 py-4">
                      <div className="flex items-center gap-2">
                        <div>
                          <p className="font-medium text-text-primary leading-tight">
                            {contract.propertyName}
                          </p>
                          <p className="text-xs text-text-muted mt-0.5">
                            {contract.propertyAddress}
                          </p>
                        </div>
                        {isApplied && (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-success-bg text-success-text ring-1 ring-success-border whitespace-nowrap">
                            <CheckCircle2 className="h-3 w-3" />
                            Applied
                          </span>
                        )}
                      </div>
                    </td>

                    {/* Current rent */}
                    <td className="px-5 py-4 text-right">
                      <span className="font-mono text-sm text-text-secondary">
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
                        <span className="inline-flex items-center justify-center gap-1 w-[120px] rounded-full px-2.5 py-1 text-xs font-medium bg-surface-inset text-text-secondary ring-1 ring-border-default dark:ring-border-strong">
                          <span className="w-[32px] text-right">
                            {contract.regulationMinPercent}%
                          </span>
                          <ArrowRight className="h-3 w-3 flex-shrink-0 text-text-muted" />
                          <span className="w-[32px] text-left">
                            {contract.regulationMaxPercent}%
                          </span>
                        </span>
                      ) : (
                        <span className="inline-flex items-center justify-center w-[120px] text-xs text-text-muted">
                          No data
                        </span>
                      )}
                    </td>

                    {/* Adjustment — editable pill */}
                    <td className="px-5 py-4">
                      <div className="flex items-center justify-center">
                        <label
                          className={`inline-flex items-center gap-1 rounded-full pl-2 pr-1 py-0.5 text-xs font-semibold ${isApplied ? 'cursor-default' : 'cursor-text'} ${pctColor(pct)} ${pctBgColor(pct)}`}
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
                              disabled={isApplied}
                              className="w-[48px] h-5 bg-transparent text-right text-xs font-semibold text-inherit focus:outline-none disabled:cursor-default [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none"
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
                        <span className="font-mono text-sm font-semibold text-text-primary">
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
                        disabled={isApplied}
                        className="h-8 px-2.5 rounded-lg border border-border-default bg-surface-card text-text-primary text-sm focus:outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20 disabled:opacity-50 disabled:cursor-default transition-colors"
                      />
                    </td>

                    {/* Warning */}
                    <td className="px-3 py-4">
                      {warning && !isApplied && (
                        <div className="group/tip relative flex items-center justify-center">
                          <div className="p-1 rounded-full bg-warning-bg">
                            <AlertTriangle className="h-3.5 w-3.5 text-warning-text" />
                          </div>
                          <div className="hidden group-hover/tip:block absolute right-0 top-full mt-1 z-10 w-60 p-3 rounded-lg bg-surface-card border border-warning-border text-xs text-warning-text shadow-lg">
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
          className="inline-flex items-center h-10 px-5 rounded-lg border border-border-default text-sm font-medium text-text-secondary bg-surface-card hover:bg-surface-inset hover:border-border-strong shadow-sm transition-all"
        >
          Back
        </button>
        <button
          onClick={onNext}
          className="inline-flex items-center gap-2 h-10 px-6 rounded-lg text-sm font-medium bg-gradient-to-b from-primary-500 to-primary-600 text-white border border-primary-600 shadow-sm shadow-primary-500/20 hover:from-primary-400 hover:to-primary-600 hover:shadow-md hover:shadow-primary-500/30 transition-all"
        >
          Next: Review
          <ArrowRight className="h-4 w-4" />
        </button>
      </div>
    </div>
  );
};
