import { CollapsibleSection } from './CollapsibleSection';
import { MoneyInput } from '@/components/common/MoneyInput';
import { MetricHint } from '@/components/common/MetricHint';
import {
  CreatePropertyRequest,
  MortgageType,
  DepreciationMethod,
  MORTGAGE_TYPE_LABELS,
  DEPRECIATION_METHOD_LABELS,
} from '@/types/property';
import { useTeamDefaults } from '@/hooks/useTeamDefaults';

const labelCls =
  'block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1';
const inputCls =
  'w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 bg-white dark:bg-[#1e2130] text-[#1a1d2e] dark:text-[#eef0f6] focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa] disabled:bg-[#f1f3f9] disabled:dark:bg-[#1a1d28] disabled:text-[#9ca0b8] disabled:dark:text-[#5c6180] disabled:cursor-not-allowed';

function countFilled(values: (unknown | null | undefined)[]): number {
  return values.filter((v) => v !== null && v !== undefined && v !== '').length;
}

interface PropertyFinancialFormProps {
  formData: CreatePropertyRequest;
  onChange: (field: keyof CreatePropertyRequest, value: unknown) => void;
  errors?: Record<string, string>;
}

export const PropertyFinancialForm = ({
  formData,
  onChange,
  errors,
}: PropertyFinancialFormProps) => {
  const { defaultCurrency } = useTeamDefaults();
  const currencyFallback = defaultCurrency || '';
  const isMortgageNone = formData.mortgageType === MortgageType.NONE;
  const isDepreciationNone =
    formData.depreciationMethod === DepreciationMethod.NONE;

  const purchaseFields = [
    formData.purchasePrice,
    formData.purchaseDate,
    formData.currentMarketValue,
    formData.marketValueDate,
  ];

  const mortgageFields = [
    formData.mortgageType,
    formData.mortgageAmount,
    formData.mortgageInterestRate,
    formData.mortgageStartDate,
    formData.mortgageEndDate,
    formData.monthlyMortgagePayment,
  ];

  const operatingFields = [
    formData.annualPropertyTax,
    formData.annualInsurance,
    formData.annualHoaFee,
    formData.annualManagementFee,
    formData.annualMaintenanceReserve,
  ];

  const depreciationFields = [
    formData.depreciationMethod,
    formData.depreciationYears,
    formData.landValue,
  ];

  return (
    <div className="space-y-2">
      <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-3">
        Investment & Financial
      </h3>

      {/* Purchase & Valuation */}
      <CollapsibleSection
        title="Purchase & Valuation"
        filledCount={countFilled(purchaseFields)}
        totalCount={purchaseFields.length}
      >
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className={labelCls}>Purchase Price</label>
            <MoneyInput
              value={formData.purchasePrice ?? undefined}
              onChange={(val) => onChange('purchasePrice', val ?? null)}
              currency={formData.purchasePriceCurrency || currencyFallback}
              onCurrencyChange={(v) => onChange('purchasePriceCurrency', v)}
              error={!!errors?.purchasePriceCurrency}
            />
            {errors?.purchasePriceCurrency && (
              <p className="text-red-600 text-sm mt-1">
                {errors.purchasePriceCurrency}
              </p>
            )}
          </div>
          <div>
            <label className={labelCls}>Purchase Date</label>
            <input
              type="date"
              value={formData.purchaseDate ?? ''}
              onChange={(e) => onChange('purchaseDate', e.target.value || null)}
              className={inputCls}
            />
          </div>
          <div>
            <label className={labelCls}>Current Market Value</label>
            <MoneyInput
              value={formData.currentMarketValue ?? undefined}
              onChange={(val) => onChange('currentMarketValue', val ?? null)}
              currency={formData.currentMarketValueCurrency || currencyFallback}
              onCurrencyChange={(v) =>
                onChange('currentMarketValueCurrency', v)
              }
              error={!!errors?.currentMarketValueCurrency}
            />
            {errors?.currentMarketValueCurrency && (
              <p className="text-red-600 text-sm mt-1">
                {errors.currentMarketValueCurrency}
              </p>
            )}
          </div>
          <div>
            <label className={labelCls}>Market Value Date</label>
            <input
              type="date"
              value={formData.marketValueDate ?? ''}
              onChange={(e) =>
                onChange('marketValueDate', e.target.value || null)
              }
              className={inputCls}
            />
          </div>
        </div>
      </CollapsibleSection>

      {/* Mortgage */}
      <CollapsibleSection
        title="Mortgage"
        filledCount={countFilled(mortgageFields)}
        totalCount={mortgageFields.length}
      >
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className={labelCls}>Mortgage Type</label>
            <select
              value={formData.mortgageType ?? ''}
              onChange={(e) => {
                const val = (e.target.value as MortgageType) || null;
                onChange('mortgageType', val);
                if (val === MortgageType.NONE) {
                  onChange('mortgageAmount', null);
                  onChange('mortgageInterestRate', null);
                  onChange('monthlyMortgagePayment', null);
                  onChange('mortgageStartDate', null);
                  onChange('mortgageEndDate', null);
                }
              }}
              className={inputCls}
            >
              <option value="">-- Select --</option>
              {Object.entries(MORTGAGE_TYPE_LABELS).map(([k, v]) => (
                <option key={k} value={k}>
                  {v}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className={labelCls}>Mortgage Amount</label>
            <MoneyInput
              value={formData.mortgageAmount ?? undefined}
              onChange={(val) => onChange('mortgageAmount', val ?? null)}
              currency={formData.mortgageAmountCurrency || currencyFallback}
              onCurrencyChange={(v) => onChange('mortgageAmountCurrency', v)}
              disabled={isMortgageNone}
              error={!!errors?.mortgageAmountCurrency}
            />
            {errors?.mortgageAmountCurrency && (
              <p className="text-red-600 text-sm mt-1">
                {errors.mortgageAmountCurrency}
              </p>
            )}
          </div>
          <div>
            <label className={labelCls}>Interest Rate</label>
            <div className="relative">
              <input
                type="number"
                step="0.001"
                min="0"
                max="100"
                value={formData.mortgageInterestRate ?? ''}
                onChange={(e) =>
                  onChange(
                    'mortgageInterestRate',
                    e.target.value ? parseFloat(e.target.value) : null
                  )
                }
                className={inputCls}
                disabled={isMortgageNone}
              />
              <span className="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-[#9ca0b8] dark:text-[#5c6180]">
                %
              </span>
            </div>
          </div>
          <div>
            <label className={labelCls}>Monthly Payment</label>
            <MoneyInput
              value={formData.monthlyMortgagePayment ?? undefined}
              onChange={(val) =>
                onChange('monthlyMortgagePayment', val ?? null)
              }
              currency={
                formData.monthlyMortgagePaymentCurrency || currencyFallback
              }
              onCurrencyChange={(v) =>
                onChange('monthlyMortgagePaymentCurrency', v)
              }
              disabled={isMortgageNone}
              error={!!errors?.monthlyMortgagePaymentCurrency}
            />
            {errors?.monthlyMortgagePaymentCurrency && (
              <p className="text-red-600 text-sm mt-1">
                {errors.monthlyMortgagePaymentCurrency}
              </p>
            )}
          </div>
          <div>
            <label className={labelCls}>Start Date</label>
            <input
              type="date"
              value={formData.mortgageStartDate ?? ''}
              onChange={(e) =>
                onChange('mortgageStartDate', e.target.value || null)
              }
              className={inputCls}
              disabled={isMortgageNone}
            />
          </div>
          <div>
            <label className={labelCls}>End Date</label>
            <input
              type="date"
              value={formData.mortgageEndDate ?? ''}
              onChange={(e) =>
                onChange('mortgageEndDate', e.target.value || null)
              }
              className={inputCls}
              disabled={isMortgageNone}
            />
            {errors?.mortgageEndDate && (
              <p className="text-red-600 text-sm mt-1">
                {errors.mortgageEndDate}
              </p>
            )}
          </div>
        </div>
      </CollapsibleSection>

      {/* Operating Costs */}
      <CollapsibleSection
        title="Annual Operating Costs"
        filledCount={countFilled(operatingFields)}
        totalCount={operatingFields.length}
      >
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className={labelCls}>Property Tax</label>
            <MoneyInput
              value={formData.annualPropertyTax ?? undefined}
              onChange={(val) => onChange('annualPropertyTax', val ?? null)}
              currency={formData.annualPropertyTaxCurrency || currencyFallback}
              onCurrencyChange={(v) => onChange('annualPropertyTaxCurrency', v)}
              error={!!errors?.annualPropertyTaxCurrency}
            />
            {errors?.annualPropertyTaxCurrency && (
              <p className="text-red-600 text-sm mt-1">
                {errors.annualPropertyTaxCurrency}
              </p>
            )}
          </div>
          <div>
            <label className={labelCls}>Insurance</label>
            <MoneyInput
              value={formData.annualInsurance ?? undefined}
              onChange={(val) => onChange('annualInsurance', val ?? null)}
              currency={formData.annualInsuranceCurrency || currencyFallback}
              onCurrencyChange={(v) => onChange('annualInsuranceCurrency', v)}
              error={!!errors?.annualInsuranceCurrency}
            />
            {errors?.annualInsuranceCurrency && (
              <p className="text-red-600 text-sm mt-1">
                {errors.annualInsuranceCurrency}
              </p>
            )}
          </div>
          <div>
            <label className={labelCls}>
              <MetricHint label="HOA Fee" />
            </label>
            <MoneyInput
              value={formData.annualHoaFee ?? undefined}
              onChange={(val) => onChange('annualHoaFee', val ?? null)}
              currency={formData.annualHoaFeeCurrency || currencyFallback}
              onCurrencyChange={(v) => onChange('annualHoaFeeCurrency', v)}
              error={!!errors?.annualHoaFeeCurrency}
            />
            {errors?.annualHoaFeeCurrency && (
              <p className="text-red-600 text-sm mt-1">
                {errors.annualHoaFeeCurrency}
              </p>
            )}
          </div>
          <div>
            <label className={labelCls}>Management Fee</label>
            <MoneyInput
              value={formData.annualManagementFee ?? undefined}
              onChange={(val) => onChange('annualManagementFee', val ?? null)}
              currency={
                formData.annualManagementFeeCurrency || currencyFallback
              }
              onCurrencyChange={(v) =>
                onChange('annualManagementFeeCurrency', v)
              }
              error={!!errors?.annualManagementFeeCurrency}
            />
            {errors?.annualManagementFeeCurrency && (
              <p className="text-red-600 text-sm mt-1">
                {errors.annualManagementFeeCurrency}
              </p>
            )}
          </div>
          <div>
            <label className={labelCls}>
              <MetricHint label="Maintenance Reserve" />
            </label>
            <MoneyInput
              value={formData.annualMaintenanceReserve ?? undefined}
              onChange={(val) =>
                onChange('annualMaintenanceReserve', val ?? null)
              }
              currency={
                formData.annualMaintenanceReserveCurrency || currencyFallback
              }
              onCurrencyChange={(v) =>
                onChange('annualMaintenanceReserveCurrency', v)
              }
              error={!!errors?.annualMaintenanceReserveCurrency}
            />
            {errors?.annualMaintenanceReserveCurrency && (
              <p className="text-red-600 text-sm mt-1">
                {errors.annualMaintenanceReserveCurrency}
              </p>
            )}
          </div>
        </div>
      </CollapsibleSection>

      {/* Depreciation */}
      <CollapsibleSection
        title="Depreciation"
        filledCount={countFilled(depreciationFields)}
        totalCount={depreciationFields.length}
      >
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className={labelCls}>Method</label>
            <select
              value={formData.depreciationMethod ?? ''}
              onChange={(e) => {
                const val = (e.target.value as DepreciationMethod) || null;
                onChange('depreciationMethod', val);
                if (val === DepreciationMethod.NONE) {
                  onChange('depreciationYears', null);
                  onChange('landValue', null);
                }
              }}
              className={inputCls}
            >
              <option value="">-- Select --</option>
              {Object.entries(DEPRECIATION_METHOD_LABELS).map(([k, v]) => (
                <option key={k} value={k}>
                  {v}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className={labelCls}>
              <MetricHint label="Useful Life" />
            </label>
            <div className="relative">
              <input
                type="number"
                min="1"
                value={formData.depreciationYears ?? ''}
                onChange={(e) =>
                  onChange(
                    'depreciationYears',
                    e.target.value ? parseInt(e.target.value, 10) : null
                  )
                }
                className={inputCls}
                disabled={isDepreciationNone}
              />
              <span className="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-[#9ca0b8] dark:text-[#5c6180]">
                years
              </span>
            </div>
          </div>
          <div>
            <label className={labelCls}>
              <MetricHint label="Land Value" />
            </label>
            <MoneyInput
              value={formData.landValue ?? undefined}
              onChange={(val) => onChange('landValue', val ?? null)}
              currency={formData.landValueCurrency || currencyFallback}
              onCurrencyChange={(v) => onChange('landValueCurrency', v)}
              disabled={isDepreciationNone}
              error={!!errors?.landValueCurrency}
            />
            {errors?.landValueCurrency && (
              <p className="text-red-600 text-sm mt-1">
                {errors.landValueCurrency}
              </p>
            )}
          </div>
        </div>
      </CollapsibleSection>
    </div>
  );
};
