import { useMemo } from 'react';
import { Plus, Trash2 } from 'lucide-react';
import { MoneyInput } from '@/components/common/MoneyInput';
import { RentComponentFormItem, RentComponentType } from '@/types/contract';
import { useTranslation } from 'react-i18next';

interface RentBreakdownProps {
  value: number | '';
  onValueChange: (val: number | '') => void;
  components: RentComponentFormItem[];
  onComponentsChange: (components: RentComponentFormItem[]) => void;
  onTotalChange: (total: number) => void;
  currency: string;
  disabled?: boolean;
  error?: boolean;
  breakdownMode: boolean;
  onBreakdownModeChange: (mode: boolean) => void;
}

const ALL_COMPONENT_TYPES = Object.values(
  RentComponentType
) as RentComponentType[];

export const RentBreakdown = ({
  value,
  onValueChange,
  components,
  onComponentsChange,
  onTotalChange,
  currency,
  disabled = false,
  error = false,
  breakdownMode,
  onBreakdownModeChange,
}: RentBreakdownProps) => {
  const { t } = useTranslation('contracts');
  const total = useMemo(
    () =>
      components.reduce(
        (sum, c) => sum + (typeof c.amount === 'number' ? c.amount : 0),
        0
      ),
    [components]
  );

  const availableTypes = useMemo(() => {
    const usedTypes = new Set(components.map((c) => c.componentType));
    return ALL_COMPONENT_TYPES.filter(
      (t) => t === RentComponentType.OTHER || !usedTypes.has(t)
    );
  }, [components]);

  const formattedTotal = useMemo(() => {
    try {
      return new Intl.NumberFormat(undefined, {
        style: 'currency',
        currency: currency || 'EUR',
      }).format(total);
    } catch {
      return `${currency} ${total.toFixed(2)}`;
    }
  }, [total, currency]);

  const handleToggleBreakdown = () => {
    if (breakdownMode) {
      // Switching from breakdown to simple: set value to total
      onValueChange(total || '');
      onBreakdownModeChange(false);
    } else {
      // Switching from simple to breakdown: create BASE_RENT with current value
      const initialAmount = typeof value === 'number' ? value : '';
      onComponentsChange([
        {
          componentType: RentComponentType.BASE_RENT,
          amount: initialAmount,
        },
      ]);
      onBreakdownModeChange(true);
    }
  };

  const handleComponentAmountChange = (
    index: number,
    newAmount: number | undefined
  ) => {
    const updated = components.map((c, i) =>
      i === index ? { ...c, amount: (newAmount ?? '') as number | '' } : c
    );
    onComponentsChange(updated);
    const newTotal = updated.reduce(
      (sum, c) => sum + (typeof c.amount === 'number' ? c.amount : 0),
      0
    );
    onTotalChange(newTotal);
  };

  const handleComponentDescriptionChange = (
    index: number,
    description: string
  ) => {
    const updated = components.map((c, i) =>
      i === index ? { ...c, description } : c
    );
    onComponentsChange(updated);
  };

  const handleAddComponent = (componentType: RentComponentType) => {
    const updated = [...components, { componentType, amount: '' as const }];
    onComponentsChange(updated);
  };

  const handleRemoveComponent = (index: number) => {
    const updated = components.filter((_, i) => i !== index);
    onComponentsChange(updated);
    const newTotal = updated.reduce(
      (sum, c) => sum + (typeof c.amount === 'number' ? c.amount : 0),
      0
    );
    onTotalChange(newTotal);
  };

  if (!breakdownMode) {
    return (
      <div>
        <label className="block text-sm font-medium text-text-secondary mb-1">
          Rent Amount <span className="text-error-text">*</span>
        </label>
        <MoneyInput
          value={typeof value === 'number' ? value : undefined}
          onChange={(val) => onValueChange(val ?? '')}
          currency={currency}
          disabled={disabled}
          error={error}
        />
        <button
          type="button"
          onClick={handleToggleBreakdown}
          className="mt-1 text-xs text-primary-500 hover:text-primary-600 transition-colors"
          disabled={disabled}
        >
          Break down rent into components
        </button>
      </div>
    );
  }

  return (
    <div className="col-span-1 lg:col-span-2">
      <div className="flex items-center justify-between mb-2">
        <label className="block text-sm font-medium text-text-secondary">
          Rent Breakdown <span className="text-error-text">*</span>
        </label>
        <button
          type="button"
          onClick={handleToggleBreakdown}
          className="text-xs text-primary-500 hover:text-primary-600 transition-colors"
          disabled={disabled}
        >
          Use single amount
        </button>
      </div>

      <div className="space-y-3">
        {components.map((comp, index) => {
          const isBaseRent = comp.componentType === RentComponentType.BASE_RENT;
          const isOther = comp.componentType === RentComponentType.OTHER;

          return (
            <div
              key={`${comp.componentType}-${index}`}
              className="flex items-start gap-2"
            >
              <div className="flex-1">
                <div className="flex items-center gap-2">
                  <span className="text-sm text-text-secondary min-w-[140px]">
                    {t(`enums.rentComponents.${comp.componentType}`)}
                  </span>
                  <div className="flex-1">
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
                      disabled={disabled}
                    />
                  </div>
                  {!isBaseRent && (
                    <button
                      type="button"
                      onClick={() => handleRemoveComponent(index)}
                      className="p-1.5 text-text-muted hover:text-error-text transition-colors"
                      disabled={disabled}
                      title={t('adjustRent.removeComponent')}
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  )}
                  {isBaseRent && <div className="w-[30px]" />}
                </div>
                {isOther && (
                  <div className="mt-1 ml-[148px]">
                    <input
                      type="text"
                      value={comp.description ?? ''}
                      onChange={(e) =>
                        handleComponentDescriptionChange(index, e.target.value)
                      }
                      placeholder={t('adjustRent.customLabel')}
                      className="w-full border border-border-strong rounded px-3 py-1.5 text-sm bg-surface-card text-text-primary focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
                      disabled={disabled}
                    />
                  </div>
                )}
              </div>
            </div>
          );
        })}

        {/* Total row */}
        <div className="flex items-center gap-2 pt-2 border-t border-border-default">
          <span className="text-sm font-semibold text-text-primary min-w-[140px]">
            Total
          </span>
          <span className="flex-1 text-sm font-semibold text-text-primary">
            {formattedTotal}
          </span>
          <div className="w-[30px]" />
        </div>

        {/* Add component */}
        {availableTypes.length > 0 && (
          <div className="pt-1">
            <AddComponentButton
              availableTypes={availableTypes}
              onAdd={handleAddComponent}
              disabled={disabled}
            />
          </div>
        )}
      </div>
    </div>
  );
};

function AddComponentButton({
  availableTypes,
  onAdd,
  disabled,
}: {
  availableTypes: RentComponentType[];
  onAdd: (type: RentComponentType) => void;
  disabled: boolean;
}) {
  return (
    <div className="relative inline-block">
      <select
        onChange={(e) => {
          if (e.target.value) {
            onAdd(e.target.value as RentComponentType);
            e.target.value = '';
          }
        }}
        className="appearance-none bg-transparent text-xs text-primary-500 hover:text-primary-600 cursor-pointer border-none focus:outline-none focus:ring-0 p-0 pr-4"
        disabled={disabled}
        value=""
      >
        <option value="" disabled>
          {t('adjustRent.addComponent')}
        </option>
        {availableTypes.map((type) => (
          <option key={type} value={type}>
            {t(`enums.rentComponents.${type}`)}
          </option>
        ))}
      </select>
      <Plus className="h-3 w-3 absolute left-0 top-1/2 -translate-y-1/2 pointer-events-none text-primary-500 hidden" />
    </div>
  );
}
