import { useState, useMemo, useRef } from 'react';
import {
  X,
  TrendingUp,
  TrendingDown,
  Info,
  ChevronDown,
  ChevronRight,
  Trash2,
  RotateCcw,
} from 'lucide-react';
import { MoneyInput } from '@/components/common/MoneyInput';
import { RichTextEditor } from '@/components/common/RichTextEditor';
import {
  RentComponentFormItem,
  RentComponentResponseItem,
  RentComponentType,
  RentPeriodResponse,
  RENT_COMPONENT_LABELS,
} from '@/types/contract';

interface AdjustRentModalProps {
  currentRent: number;
  currency: string;
  currentComponents: RentComponentResponseItem[];
  rentPeriods: RentPeriodResponse[];
  onClose: () => void;
  onConfirm: (
    rentAmount: number,
    effectiveFrom: string,
    notes?: string,
    components?: RentComponentFormItem[]
  ) => void;
  isLoading?: boolean;
}

function getFirstDayOfNextMonth(): string {
  const now = new Date();
  const next = new Date(now.getFullYear(), now.getMonth() + 1, 1);
  return next.toISOString().split('T')[0];
}

const ALL_COMPONENT_TYPES = Object.values(
  RentComponentType
) as RentComponentType[];

function formatCurrency(amount: number, currency: string): string {
  try {
    return new Intl.NumberFormat(undefined, {
      style: 'currency',
      currency: currency || 'EUR',
    }).format(amount);
  } catch {
    return `${currency} ${amount.toFixed(2)}`;
  }
}

function pctChange(newVal: number, oldVal: number): number | null {
  if (oldVal <= 0) {
    return null;
  }
  return ((newVal - oldVal) / oldVal) * 100;
}

function getPeriodAtDate(
  date: string,
  periods: RentPeriodResponse[]
): RentPeriodResponse | undefined {
  if (!date || periods.length === 0) {
    return undefined;
  }
  return periods.find(
    (p) =>
      p.effectiveFrom <= date &&
      (p.effectiveTo == null || p.effectiveTo >= date)
  );
}

export const AdjustRentModal = ({
  currentRent,
  currency,
  currentComponents,
  rentPeriods,
  onClose,
  onConfirm,
  isLoading = false,
}: AdjustRentModalProps) => {
  const [rentAmount, setRentAmount] = useState('');
  const [effectiveFrom, setEffectiveFrom] = useState(getFirstDayOfNextMonth());
  const [notes, setNotes] = useState('');
  const [adjustComponents, setAdjustComponents] = useState(false);

  // Resolve the period active at the selected date for reference rent + components
  const referencePeriod = useMemo(
    () => getPeriodAtDate(effectiveFrom, rentPeriods),
    [effectiveFrom, rentPeriods]
  );
  const referenceComponents: RentComponentResponseItem[] = useMemo(
    () => referencePeriod?.components ?? currentComponents,
    [referencePeriod, currentComponents]
  );

  const [components, setComponents] = useState<RentComponentFormItem[]>(() =>
    currentComponents.map((c) => ({
      componentType: c.componentType,
      amount: c.amount,
      description: c.description,
    }))
  );
  // Track removed original components to show as struck-through
  const [removedOriginals, setRemovedOriginals] = useState<
    RentComponentResponseItem[]
  >([]);

  // Reset component form when the reference period changes.
  // setState during render is the React-recommended pattern for syncing derived state
  // (avoids the setState-in-effect lint error). See: react.dev/learn/you-might-not-need-an-effect
  const prevReferenceRef = useRef(referenceComponents);
  if (prevReferenceRef.current !== referenceComponents) {
    prevReferenceRef.current = referenceComponents;
    setComponents(
      referenceComponents.map((c) => ({
        componentType: c.componentType,
        amount: c.amount,
        description: c.description,
      }))
    );
    setRemovedOriginals([]);
  }

  const hasCurrentComponents = referenceComponents.length > 0;

  const componentsTotal = useMemo(
    () =>
      components.reduce(
        (sum, c) => sum + (typeof c.amount === 'number' ? c.amount : 0),
        0
      ),
    [components]
  );

  // When adjusting components, rent amount is derived from components total
  const parsedAmount = useMemo(() => {
    if (adjustComponents) {
      return componentsTotal > 0 ? componentsTotal : null;
    }
    const val = parseFloat(rentAmount);
    return isNaN(val) ? null : val;
  }, [adjustComponents, componentsTotal, rentAmount]);

  // Rent that was/will be active at the selected effective date
  const referenceRent = useMemo(
    () => referencePeriod?.rentAmount ?? currentRent,
    [referencePeriod, currentRent]
  );

  const isRetroactive = useMemo(() => {
    if (!effectiveFrom) {
      return false;
    }
    const today = new Date().toISOString().split('T')[0];
    return effectiveFrom < today;
  }, [effectiveFrom]);

  const percentageChange = useMemo(() => {
    if (parsedAmount === null || referenceRent <= 0) {
      return null;
    }
    return ((parsedAmount - referenceRent) / referenceRent) * 100;
  }, [parsedAmount, referenceRent]);

  const currentComponentsTotal = useMemo(
    () => referenceComponents.reduce((sum, c) => sum + c.amount, 0),
    [referenceComponents]
  );

  const componentsTotalPctChange = pctChange(
    componentsTotal,
    currentComponentsTotal
  );

  const availableTypes = useMemo(() => {
    const usedTypes = new Set(components.map((c) => c.componentType));
    return ALL_COMPONENT_TYPES.filter(
      (t) => t === RentComponentType.OTHER || !usedTypes.has(t)
    );
  }, [components]);

  const handleComponentAmountChange = (
    index: number,
    newAmount: number | undefined
  ) => {
    setComponents((prev) =>
      prev.map((c, i) =>
        i === index ? { ...c, amount: (newAmount ?? '') as number | '' } : c
      )
    );
  };

  const handleComponentDescriptionChange = (
    index: number,
    description: string
  ) => {
    setComponents((prev) =>
      prev.map((c, i) => (i === index ? { ...c, description } : c))
    );
  };

  const handleRemoveComponent = (index: number) => {
    const removed = components[index];
    // If this was an original component, track it for the struck-through display
    const original = referenceComponents.find(
      (c) =>
        c.componentType === removed.componentType &&
        (removed.componentType !== RentComponentType.OTHER ||
          c.description === removed.description)
    );
    if (original) {
      setRemovedOriginals((prev) => [...prev, original]);
    }
    setComponents((prev) => prev.filter((_, i) => i !== index));
  };

  const handleRestoreComponent = (originalIndex: number) => {
    const original = removedOriginals[originalIndex];
    setRemovedOriginals((prev) => prev.filter((_, i) => i !== originalIndex));
    setComponents((prev) => [
      ...prev,
      {
        componentType: original.componentType,
        amount: original.amount,
        description: original.description,
      },
    ]);
  };

  const handleAddComponent = (componentType: RentComponentType) => {
    setComponents((prev) => [...prev, { componentType, amount: '' as const }]);
  };

  const submitForm = () => {
    const amount = adjustComponents ? componentsTotal : parsedAmount;
    if (amount === null || amount <= 0) {
      return;
    }
    onConfirm(
      amount,
      effectiveFrom,
      notes || undefined,
      adjustComponents ? components : undefined
    );
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

  const findCurrentComponent = (
    type: RentComponentType,
    description?: string
  ): RentComponentResponseItem | undefined => {
    return referenceComponents.find(
      (c) =>
        c.componentType === type &&
        (type !== RentComponentType.OTHER || c.description === description)
    );
  };

  // Column widths for table-like alignment
  const labelCol = 'w-[140px] shrink-0';
  const actionCol = 'w-[28px] shrink-0';

  return (
    <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50">
      <div className="bg-surface-card rounded-lg shadow-xl dark:shadow-black/20 max-w-lg w-full mx-4 max-h-[90vh] flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between p-4 border-b border-border-default shrink-0">
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
        <form
          onSubmit={handleSubmit}
          onKeyDown={handleCmdEnter}
          className="flex flex-col min-h-0"
        >
          <div className="p-4 space-y-4 overflow-y-auto">
            {/* Effective From — first so the reference rent updates below */}
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

            {/* Reference Rent at selected date */}
            <div>
              <label className="block text-sm font-medium text-text-secondary mb-1">
                {referenceRent !== currentRent
                  ? 'Rent at Selected Date'
                  : 'Current Rent'}
              </label>
              <p className="text-sm text-text-primary">
                {formatCurrency(referenceRent, currency)}
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
                  disabled={isLoading || adjustComponents}
                  min={0.01}
                  className="pr-24"
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
              {adjustComponents && (
                <p className="mt-1 text-xs text-text-muted">
                  Auto-calculated from component totals below.
                </p>
              )}
            </div>

            {/* Rent Components Section */}
            {hasCurrentComponents && (
              <div className="border border-border-default rounded-lg">
                <button
                  type="button"
                  onClick={() => setAdjustComponents(!adjustComponents)}
                  className="w-full flex items-center gap-2 px-3 py-2.5 text-sm font-medium text-text-secondary hover:text-text-primary transition-colors"
                  disabled={isLoading}
                >
                  {adjustComponents ? (
                    <ChevronDown className="h-4 w-4" />
                  ) : (
                    <ChevronRight className="h-4 w-4" />
                  )}
                  Adjust Rent Components
                </button>

                {adjustComponents && (
                  <div className="px-3 pb-3 space-y-0">
                    {/* Active component rows */}
                    {components.map((comp, index) => {
                      const current = findCurrentComponent(
                        comp.componentType,
                        comp.description
                      );
                      const oldAmount = current?.amount ?? 0;
                      const newAmount =
                        typeof comp.amount === 'number' ? comp.amount : 0;
                      const compPct = pctChange(newAmount, oldAmount);
                      const isOther =
                        comp.componentType === RentComponentType.OTHER;

                      return (
                        <div
                          key={`${comp.componentType}-${index}`}
                          className="py-1.5"
                        >
                          <div className="flex items-center gap-2">
                            <div className={labelCol}>
                              <span className="text-xs text-text-secondary truncate block">
                                {RENT_COMPONENT_LABELS[comp.componentType]}
                              </span>
                              {current && (
                                <span className="text-[10px] text-text-muted">
                                  was {formatCurrency(oldAmount, currency)}
                                </span>
                              )}
                            </div>
                            <div className="flex-1 min-w-0 overflow-hidden relative">
                              <MoneyInput
                                value={
                                  typeof comp.amount === 'number'
                                    ? comp.amount
                                    : undefined
                                }
                                onChange={(val) =>
                                  handleComponentAmountChange(index, val)
                                }
                                currency={currency}
                                disabled={isLoading}
                                className={compPct !== null && compPct !== 0 ? 'pr-16' : ''}
                              />
                              {compPct !== null && compPct !== 0 && (
                                <span
                                  className={`absolute right-3 top-1/2 -translate-y-1/2 text-xs font-medium whitespace-nowrap ${
                                    compPct > 0
                                      ? 'text-success-text'
                                      : 'text-error-text'
                                  }`}
                                >
                                  {compPct > 0 ? '+' : ''}
                                  {compPct.toFixed(1)}%
                                </span>
                              )}
                            </div>
                            <div className={actionCol}>
                              <button
                                type="button"
                                onClick={() => handleRemoveComponent(index)}
                                className="p-1 text-text-muted hover:text-error-text transition-colors"
                                disabled={isLoading}
                                title="Remove component"
                              >
                                <Trash2 className="h-3.5 w-3.5" />
                              </button>
                            </div>
                          </div>
                          {isOther && (
                            <div className="mt-1 pl-[148px]">
                              <input
                                type="text"
                                value={comp.description ?? ''}
                                onChange={(e) =>
                                  handleComponentDescriptionChange(
                                    index,
                                    e.target.value
                                  )
                                }
                                placeholder="Custom label"
                                className="w-full border border-border-strong rounded px-2 py-1 text-xs bg-surface-card text-text-primary focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
                                disabled={isLoading}
                              />
                            </div>
                          )}
                        </div>
                      );
                    })}

                    {/* Removed (struck-through) component rows */}
                    {removedOriginals.map((orig, index) => (
                      <div
                        key={`removed-${orig.componentType}-${index}`}
                        className="py-1.5 opacity-50"
                      >
                        <div className="flex items-center gap-2">
                          <div className={labelCol}>
                            <span className="text-xs text-text-muted line-through truncate block">
                              {RENT_COMPONENT_LABELS[orig.componentType]}
                            </span>
                          </div>
                          <div className="flex-1 min-w-0 overflow-hidden relative">
                            <div className="px-3 py-2 pr-16 text-sm text-text-muted line-through border border-border-default rounded-md bg-surface-inset">
                              {formatCurrency(orig.amount, currency)}
                            </div>
                            <span className="absolute right-3 top-1/2 -translate-y-1/2 text-xs font-medium text-error-text whitespace-nowrap no-underline" style={{ textDecoration: 'none' }}>
                              removed
                            </span>
                          </div>
                          <div className={actionCol}>
                            <button
                              type="button"
                              onClick={() => handleRestoreComponent(index)}
                              className="p-1 text-text-muted hover:text-primary-500 transition-colors"
                              disabled={isLoading}
                              title="Restore component"
                            >
                              <RotateCcw className="h-3.5 w-3.5" />
                            </button>
                          </div>
                        </div>
                      </div>
                    ))}

                    {/* Total row */}
                    <div className="flex items-center gap-2 pt-2 mt-1 border-t border-border-default">
                      <div className={labelCol}>
                        <span className="text-xs font-semibold text-text-primary">
                          Components Total
                        </span>
                      </div>
                      <div className="flex-1 min-w-0 overflow-hidden relative">
                        <span className="text-sm font-semibold text-text-primary pl-3">
                          {formatCurrency(componentsTotal, currency)}
                        </span>
                        {componentsTotalPctChange !== null &&
                          componentsTotalPctChange !== 0 && (
                            <span
                              className={`absolute right-3 top-1/2 -translate-y-1/2 text-xs font-medium whitespace-nowrap ${
                                componentsTotalPctChange > 0
                                  ? 'text-success-text'
                                  : 'text-error-text'
                              }`}
                            >
                              {componentsTotalPctChange > 0 ? '+' : ''}
                              {componentsTotalPctChange.toFixed(1)}%
                            </span>
                          )}
                      </div>
                      <div className={actionCol} />
                    </div>

                    {/* Add component */}
                    {availableTypes.length > 0 && (
                      <div className="pt-2">
                        <select
                          onChange={(e) => {
                            if (e.target.value) {
                              handleAddComponent(
                                e.target.value as RentComponentType
                              );
                              e.target.value = '';
                            }
                          }}
                          className="appearance-none bg-transparent text-xs text-primary-500 hover:text-primary-600 cursor-pointer border-none focus:outline-none focus:ring-0 p-0"
                          disabled={isLoading}
                          value=""
                        >
                          <option value="" disabled>
                            + Add component
                          </option>
                          {availableTypes.map((type) => (
                            <option key={type} value={type}>
                              {RENT_COMPONENT_LABELS[type]}
                            </option>
                          ))}
                        </select>
                      </div>
                    )}
                  </div>
                )}
              </div>
            )}

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
          <div className="flex items-center justify-end gap-3 p-4 border-t border-border-default shrink-0">
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
