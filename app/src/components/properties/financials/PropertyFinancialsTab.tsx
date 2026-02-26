import React, { useState } from 'react';
import {
  TrendingUp,
  Building2,
  Shield,
  Receipt,
  Wallet,
  Landmark,
  PiggyBank,
  Calendar,
  RefreshCw,
  Plus,
  Pencil,
  Trash2,
} from 'lucide-react';
import {
  useFinancialSummary,
  useDeleteValuation,
  useDeleteFinancing,
  useDeleteInsurance,
  useDeleteTax,
  useDeleteFee,
} from '@/hooks/usePropertyFinancialsHooks';
import { useFormatDate } from '@/hooks/useFormatDate';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { RichTextDisplay } from '@/components/common/RichTextDisplay';
import type {
  PropertyFinancialSummaryResponse,
  PropertyAcquisitionResponse,
  PropertyValuationResponse,
  PropertyFinancingResponse,
  PropertyInsuranceResponse,
  PropertyTaxResponse,
  PropertyFeeResponse,
} from '@/types/propertyFinancials';
import {
  FinancingStatus,
  InsuranceStatus,
  TaxStatus,
  FeeStatus,
  formatAcquisitionType,
  formatDepreciationMethod,
  formatValuationType,
  formatFinancingType,
  formatRateType,
  formatFinancingStatus,
  formatInsuranceType,
  formatInsuranceStatus,
  formatTaxType,
  formatTaxStatus,
  formatFeeType,
  formatFeeStatus,
  formatPaymentFrequency,
} from '@/types/propertyFinancials';
import {
  AcquisitionFormModal,
  ValuationFormModal,
  FinancingFormModal,
  InsuranceFormModal,
  TaxFormModal,
  FeeFormModal,
  DeleteFinancialConfirmDialog,
} from './modals';

// ============================================================
// Props
// ============================================================

interface PropertyFinancialsTabProps {
  propertyId: string;
}

// ============================================================
// Modal state types
// ============================================================

type ModalState =
  | { type: 'acquisition'; data?: PropertyAcquisitionResponse }
  | { type: 'valuation'; data?: PropertyValuationResponse }
  | { type: 'financing'; data?: PropertyFinancingResponse }
  | { type: 'insurance'; data?: PropertyInsuranceResponse }
  | { type: 'tax'; data?: PropertyTaxResponse }
  | { type: 'fee'; data?: PropertyFeeResponse }
  | null;

type DeleteState = {
  entity: 'valuation' | 'financing' | 'insurance' | 'tax' | 'fee';
  identifier: string;
  label: string;
} | null;

// ============================================================
// Helpers
// ============================================================

const formatMoney = (
  amount: number | undefined | null,
  currency?: string | null
): string => {
  if (amount == null) {
    return 'Not set';
  }
  try {
    return new Intl.NumberFormat(undefined, {
      style: 'currency',
      currency: currency || 'EUR',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(amount);
  } catch {
    return `${currency || 'EUR'} ${amount.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  }
};

const formatPercent = (rate: number | undefined | null): string => {
  if (rate == null) {
    return 'N/A';
  }
  return `${rate.toFixed(2)}%`;
};

type StatusVariant = 'green' | 'gray' | 'red' | 'yellow';

const STATUS_COLORS: Record<StatusVariant, string> = {
  green: 'bg-green-100 dark:bg-green-900/30 text-green-800 dark:text-green-300',
  gray: 'bg-gray-100 dark:bg-gray-800/40 text-gray-700 dark:text-gray-300',
  red: 'bg-red-100 dark:bg-red-900/30 text-red-800 dark:text-red-300',
  yellow:
    'bg-yellow-100 dark:bg-yellow-900/30 text-yellow-800 dark:text-yellow-300',
};

const financingStatusVariant = (status: FinancingStatus): StatusVariant => {
  switch (status) {
    case FinancingStatus.ACTIVE:
      return 'green';
    case FinancingStatus.PAID_OFF:
      return 'gray';
    case FinancingStatus.REFINANCED:
      return 'yellow';
    case FinancingStatus.DEFAULTED:
      return 'red';
  }
};

const insuranceStatusVariant = (status: InsuranceStatus): StatusVariant => {
  switch (status) {
    case InsuranceStatus.ACTIVE:
      return 'green';
    case InsuranceStatus.EXPIRED:
      return 'gray';
    case InsuranceStatus.CANCELLED:
      return 'red';
  }
};

const taxStatusVariant = (status: TaxStatus): StatusVariant => {
  switch (status) {
    case TaxStatus.ACTIVE:
      return 'green';
    case TaxStatus.EXPIRED:
      return 'gray';
    case TaxStatus.EXEMPT:
      return 'yellow';
  }
};

const feeStatusVariant = (status: FeeStatus): StatusVariant => {
  switch (status) {
    case FeeStatus.ACTIVE:
      return 'green';
    case FeeStatus.EXPIRED:
      return 'gray';
    case FeeStatus.CANCELLED:
      return 'red';
  }
};

/** Sort recurring cost items: active first, then by startDate desc, endDate desc. */
function sortRecurringCosts<
  T extends { status: string; startDate?: string; endDate?: string },
>(items: T[]): T[] {
  return [...items].sort((a, b) => {
    // Active items first
    const aActive = a.status === 'ACTIVE' ? 0 : 1;
    const bActive = b.status === 'ACTIVE' ? 0 : 1;
    if (aActive !== bActive) {
      return aActive - bActive;
    }
    // Then by startDate descending (newest first)
    const aStart = a.startDate || '';
    const bStart = b.startDate || '';
    if (aStart !== bStart) {
      return bStart.localeCompare(aStart);
    }
    // Then by endDate descending
    const aEnd = a.endDate || '';
    const bEnd = b.endDate || '';
    return bEnd.localeCompare(aEnd);
  });
}

// ============================================================
// Sub-components
// ============================================================

const ActionButton = ({
  icon: Icon,
  label,
  onClick,
  variant = 'default',
}: {
  icon: React.ElementType;
  label: string;
  onClick: () => void;
  variant?: 'default' | 'danger';
}) => (
  <button
    type="button"
    onClick={onClick}
    title={label}
    className={`p-1 rounded transition-colors ${
      variant === 'danger'
        ? 'text-[#9ca0b8] dark:text-[#5c6180] hover:text-red-600 dark:hover:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20'
        : 'text-[#9ca0b8] dark:text-[#5c6180] hover:text-[#5c7cfa] dark:hover:text-[#91a7ff] hover:bg-[#f1f3f9] dark:hover:bg-[#2a2e3f]'
    }`}
  >
    <Icon className="h-3.5 w-3.5" />
  </button>
);

const StatusBadge = ({
  label,
  variant,
}: {
  label: string;
  variant: StatusVariant;
}) => (
  <span
    className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${STATUS_COLORS[variant]}`}
  >
    {label}
  </span>
);

const DetailRow = ({
  label,
  value,
}: {
  label: string;
  value: React.ReactNode;
}) => (
  <div className="flex justify-between py-2 border-b border-[#e2e6f0] dark:border-[#2a2e3f] last:border-0">
    <span className="text-sm text-[#6b7194] dark:text-[#8b90a8]">{label}</span>
    <span className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
      {value ?? <span className="text-[#6b7194] dark:text-[#8b90a8]">N/A</span>}
    </span>
  </div>
);

const SectionHeader = ({
  icon: Icon,
  title,
  action,
}: {
  icon: React.ElementType;
  title: string;
  action?: React.ReactNode;
}) => (
  <div className="flex items-center justify-between mb-4">
    <div className="flex items-center gap-2">
      <Icon className="h-5 w-5 text-[#5c7cfa] dark:text-[#91a7ff]" />
      <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
        {title}
      </h3>
    </div>
    {action}
  </div>
);

const AddButton = ({
  label,
  onClick,
}: {
  label: string;
  onClick: () => void;
}) => (
  <button
    type="button"
    onClick={onClick}
    className="inline-flex items-center gap-1 text-xs font-medium text-[#5c7cfa] dark:text-[#91a7ff] hover:text-[#4c6ef5] dark:hover:text-[#b8c9ff] transition-colors"
  >
    <Plus className="h-3.5 w-3.5" />
    {label}
  </button>
);

const EmptyState = ({ message }: { message: string }) => (
  <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] italic py-4">
    {message}
  </p>
);

// ============================================================
// Section: Key Metrics
// ============================================================

const MetricCard = ({
  icon: Icon,
  label,
  value,
  muted,
}: {
  icon: React.ElementType;
  label: string;
  value: string;
  muted?: boolean;
}) => (
  <div className="bg-white dark:bg-[#1e2235] rounded-xl border border-[#e2e6f0] dark:border-[#2a2e3f] p-5">
    <div className="flex items-center gap-2 mb-2">
      <Icon className="h-4 w-4 text-[#6b7194] dark:text-[#8b90a8]" />
      <span className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
        {label}
      </span>
    </div>
    <p
      className={`text-xl font-semibold ${
        muted
          ? 'text-[#6b7194] dark:text-[#8b90a8]'
          : 'text-[#1a1d2e] dark:text-[#eef0f6]'
      }`}
    >
      {value}
    </p>
  </div>
);

const KeyMetricsSection = ({
  summary,
}: {
  summary: PropertyFinancialSummaryResponse;
}) => {
  const currency = summary.currency;
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      <MetricCard
        icon={TrendingUp}
        label="Net Worth"
        value={
          summary.netWorth != null
            ? formatMoney(summary.netWorth, currency)
            : 'Not set'
        }
        muted={summary.netWorth == null}
      />
      <MetricCard
        icon={Receipt}
        label="Total Annual Costs"
        value={
          summary.totalAnnualCosts != null
            ? formatMoney(summary.totalAnnualCosts, currency)
            : 'Not set'
        }
        muted={summary.totalAnnualCosts == null}
      />
      <MetricCard
        icon={Landmark}
        label="Total Financing Balance"
        value={
          summary.totalFinancingBalance != null
            ? formatMoney(summary.totalFinancingBalance, currency)
            : 'Not set'
        }
        muted={summary.totalFinancingBalance == null}
      />
      <MetricCard
        icon={Building2}
        label="Latest Valuation"
        value={
          summary.latestValuation
            ? formatMoney(
                summary.latestValuation.amount,
                summary.latestValuation.currency
              )
            : 'Not set'
        }
        muted={!summary.latestValuation}
      />
    </div>
  );
};

// ============================================================
// Section: Acquisition
// ============================================================

const AcquisitionSection = ({
  acquisition,
  formatDate,
  onEdit,
}: {
  acquisition?: PropertyAcquisitionResponse;
  formatDate: (d: string | Date) => string;
  onEdit: () => void;
}) => (
  <div>
    <SectionHeader
      icon={PiggyBank}
      title="Acquisition"
      action={
        <AddButton label={acquisition ? 'Edit' : 'Add'} onClick={onEdit} />
      }
    />
    {acquisition ? (
      <div className="bg-white dark:bg-[#1e2235] rounded-xl border border-[#e2e6f0] dark:border-[#2a2e3f] p-6">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-x-8">
          <div>
            <DetailRow
              label="Acquisition Type"
              value={formatAcquisitionType(acquisition.acquisitionType)}
            />
            {acquisition.acquisitionDate && (
              <DetailRow
                label="Date"
                value={formatDate(acquisition.acquisitionDate)}
              />
            )}
            {acquisition.purchasePrice != null && (
              <DetailRow
                label="Purchase Price"
                value={formatMoney(
                  acquisition.purchasePrice,
                  acquisition.purchasePriceCurrency
                )}
              />
            )}
            {acquisition.closingCosts != null && (
              <DetailRow
                label="Closing Costs"
                value={formatMoney(
                  acquisition.closingCosts,
                  acquisition.closingCostsCurrency
                )}
              />
            )}
          </div>
          <div>
            {acquisition.renovationCosts != null && (
              <DetailRow
                label="Renovation Costs"
                value={formatMoney(
                  acquisition.renovationCosts,
                  acquisition.renovationCostsCurrency
                )}
              />
            )}
            {acquisition.landValue != null && (
              <DetailRow
                label="Land Value"
                value={formatMoney(
                  acquisition.landValue,
                  acquisition.landValueCurrency
                )}
              />
            )}
            {acquisition.depreciationMethod && (
              <DetailRow
                label="Depreciation Method"
                value={formatDepreciationMethod(acquisition.depreciationMethod)}
              />
            )}
            {acquisition.depreciationYears != null && (
              <DetailRow
                label="Depreciation Years"
                value={`${acquisition.depreciationYears} years`}
              />
            )}
          </div>
        </div>
        {acquisition.notes && (
          <div className="mt-4 pt-4 border-t border-[#e2e6f0] dark:border-[#2a2e3f]">
            <p className="text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] mb-1">
              Notes
            </p>
            <RichTextDisplay
              content={acquisition.notes}
              className="text-sm text-[#3d4463] dark:text-[#c4c8db]"
            />
          </div>
        )}
      </div>
    ) : (
      <EmptyState message="No acquisition data recorded." />
    )}
  </div>
);

// ============================================================
// Section: Valuation
// ============================================================

const ValuationSection = ({
  latest,
  history,
  formatDate,
  onAdd,
  onEdit,
  onDelete,
}: {
  latest?: PropertyValuationResponse;
  history: PropertyValuationResponse[];
  formatDate: (d: string | Date) => string;
  onAdd: () => void;
  onEdit: (v: PropertyValuationResponse) => void;
  onDelete: (v: PropertyValuationResponse) => void;
}) => (
  <div>
    <SectionHeader
      icon={Building2}
      title="Valuations"
      action={<AddButton label="Add" onClick={onAdd} />}
    />
    {latest ? (
      <div className="space-y-4">
        <div className="bg-white dark:bg-[#1e2235] rounded-xl border border-[#e2e6f0] dark:border-[#2a2e3f] p-6">
          <div className="flex items-center justify-between mb-3">
            <h4 className="text-sm font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
              Latest Valuation
            </h4>
            <div className="flex items-center gap-1">
              <ActionButton
                icon={Pencil}
                label="Edit"
                onClick={() => onEdit(latest)}
              />
              <ActionButton
                icon={Trash2}
                label="Delete"
                onClick={() => onDelete(latest)}
                variant="danger"
              />
            </div>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-x-8">
            <div>
              <DetailRow
                label="Type"
                value={formatValuationType(latest.valuationType)}
              />
              <DetailRow
                label="Date"
                value={formatDate(latest.valuationDate)}
              />
            </div>
            <div>
              <DetailRow
                label="Amount"
                value={formatMoney(latest.amount, latest.currency)}
              />
              {latest.source && (
                <DetailRow label="Source" value={latest.source} />
              )}
            </div>
          </div>
          {latest.notes && (
            <div className="mt-4 pt-4 border-t border-[#e2e6f0] dark:border-[#2a2e3f]">
              <p className="text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] mb-1">
                Notes
              </p>
              <RichTextDisplay
                content={latest.notes}
                className="text-sm text-[#3d4463] dark:text-[#c4c8db]"
              />
            </div>
          )}
        </div>

        {history.length > 1 && (
          <div className="bg-white dark:bg-[#1e2235] rounded-xl border border-[#e2e6f0] dark:border-[#2a2e3f] p-6">
            <h4 className="text-sm font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-3">
              Valuation History
            </h4>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-left text-[#6b7194] dark:text-[#8b90a8] border-b border-[#e2e6f0] dark:border-[#2a2e3f]">
                    <th className="pb-2 pr-4 font-medium">Date</th>
                    <th className="pb-2 pr-4 font-medium">Type</th>
                    <th className="pb-2 pr-4 font-medium text-right">Amount</th>
                    <th className="pb-2 pr-4 font-medium">Source</th>
                    <th className="pb-2 font-medium w-16" />
                  </tr>
                </thead>
                <tbody>
                  {history.map((v) => (
                    <React.Fragment key={v.identifier}>
                      <tr
                        className={`border-b border-[#e2e6f0] dark:border-[#2a2e3f] last:border-0 ${v.notes ? 'border-b-0' : ''}`}
                      >
                        <td className="py-2 pr-4 text-[#1a1d2e] dark:text-[#eef0f6]">
                          {formatDate(v.valuationDate)}
                        </td>
                        <td className="py-2 pr-4 text-[#1a1d2e] dark:text-[#eef0f6]">
                          {formatValuationType(v.valuationType)}
                        </td>
                        <td className="py-2 pr-4 text-right font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                          {formatMoney(v.amount, v.currency)}
                        </td>
                        <td className="py-2 pr-4 text-[#6b7194] dark:text-[#8b90a8]">
                          {v.source || 'N/A'}
                        </td>
                        <td className="py-2">
                          <div className="flex items-center gap-1 justify-end">
                            <ActionButton
                              icon={Pencil}
                              label="Edit"
                              onClick={() => onEdit(v)}
                            />
                            <ActionButton
                              icon={Trash2}
                              label="Delete"
                              onClick={() => onDelete(v)}
                              variant="danger"
                            />
                          </div>
                        </td>
                      </tr>
                      {v.notes && (
                        <tr className="border-b border-[#e2e6f0] dark:border-[#2a2e3f] last:border-0">
                          <td colSpan={5} className="pb-2 pt-0">
                            <RichTextDisplay
                              content={v.notes}
                              className="text-xs text-[#6b7194] dark:text-[#8b90a8]"
                            />
                          </td>
                        </tr>
                      )}
                    </React.Fragment>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>
    ) : (
      <EmptyState message="No valuation data recorded." />
    )}
  </div>
);

// ============================================================
// Section: Financings
// ============================================================

const FinancingCard = ({
  financing,
  formatDate,
  onEdit,
  onDelete,
}: {
  financing: PropertyFinancingResponse;
  formatDate: (d: string | Date) => string;
  onEdit: () => void;
  onDelete: () => void;
}) => (
  <div className="bg-white dark:bg-[#1e2235] rounded-xl border border-[#e2e6f0] dark:border-[#2a2e3f] p-6">
    <div className="flex items-center justify-between mb-4">
      <div className="flex items-center gap-3">
        <h4 className="text-sm font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
          {formatFinancingType(financing.financingType)}
        </h4>
        {financing.lenderName && (
          <span className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
            &middot; {financing.lenderName}
          </span>
        )}
      </div>
      <div className="flex items-center gap-2">
        <StatusBadge
          label={formatFinancingStatus(financing.status)}
          variant={financingStatusVariant(financing.status)}
        />
        <div className="flex items-center gap-1">
          <ActionButton icon={Pencil} label="Edit" onClick={onEdit} />
          <ActionButton
            icon={Trash2}
            label="Delete"
            onClick={onDelete}
            variant="danger"
          />
        </div>
      </div>
    </div>
    <div className="grid grid-cols-1 md:grid-cols-2 gap-x-8">
      <div>
        <DetailRow
          label="Rate Type"
          value={formatRateType(financing.rateType)}
        />
        <DetailRow
          label="Original Amount"
          value={formatMoney(
            financing.originalAmount,
            financing.originalAmountCurrency
          )}
        />
        <DetailRow
          label="Current Balance"
          value={formatMoney(
            financing.currentBalance,
            financing.currentBalanceCurrency
          )}
        />
        <DetailRow
          label="Interest Rate"
          value={formatPercent(financing.interestRate)}
        />
      </div>
      <div>
        <DetailRow
          label="Monthly Payment"
          value={formatMoney(
            financing.monthlyPayment,
            financing.monthlyPaymentCurrency
          )}
        />
        <DetailRow label="Start Date" value={formatDate(financing.startDate)} />
        {financing.endDate && (
          <DetailRow label="End Date" value={formatDate(financing.endDate)} />
        )}
        {financing.termMonths != null && (
          <DetailRow label="Term" value={`${financing.termMonths} months`} />
        )}
      </div>
    </div>
    {financing.notes && (
      <div className="mt-4 pt-4 border-t border-[#e2e6f0] dark:border-[#2a2e3f]">
        <p className="text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] mb-1">
          Notes
        </p>
        <RichTextDisplay
          content={financing.notes}
          className="text-sm text-[#3d4463] dark:text-[#c4c8db]"
        />
      </div>
    )}
  </div>
);

const FinancingsSection = ({
  financings,
  formatDate,
  onAdd,
  onEdit,
  onDelete,
}: {
  financings: PropertyFinancingResponse[];
  formatDate: (d: string | Date) => string;
  onAdd: () => void;
  onEdit: (f: PropertyFinancingResponse) => void;
  onDelete: (f: PropertyFinancingResponse) => void;
}) => (
  <div>
    <SectionHeader
      icon={Landmark}
      title="Financings"
      action={<AddButton label="Add" onClick={onAdd} />}
    />
    {financings.length > 0 ? (
      <div className="space-y-4">
        {financings.map((f) => (
          <FinancingCard
            key={f.identifier}
            financing={f}
            formatDate={formatDate}
            onEdit={() => onEdit(f)}
            onDelete={() => onDelete(f)}
          />
        ))}
      </div>
    ) : (
      <EmptyState message="No financing instruments recorded." />
    )}
  </div>
);

// ============================================================
// Section: Recurring Costs (Insurances, Taxes, Fees)
// ============================================================

const InsuranceCard = ({
  insurance,
  formatDate,
  onEdit,
  onDelete,
}: {
  insurance: PropertyInsuranceResponse;
  formatDate: (d: string | Date) => string;
  onEdit: () => void;
  onDelete: () => void;
}) => (
  <div className="bg-white dark:bg-[#1e2235] rounded-xl border border-[#e2e6f0] dark:border-[#2a2e3f] p-4">
    <div className="flex items-center justify-between mb-3">
      <h5 className="text-sm font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
        {formatInsuranceType(insurance.insuranceType)}
      </h5>
      <div className="flex items-center gap-2">
        <StatusBadge
          label={formatInsuranceStatus(insurance.status)}
          variant={insuranceStatusVariant(insurance.status)}
        />
        <div className="flex items-center gap-1">
          <ActionButton icon={Pencil} label="Edit" onClick={onEdit} />
          <ActionButton
            icon={Trash2}
            label="Delete"
            onClick={onDelete}
            variant="danger"
          />
        </div>
      </div>
    </div>
    {insurance.provider && (
      <DetailRow label="Provider" value={insurance.provider} />
    )}
    <DetailRow
      label="Annual Premium"
      value={formatMoney(
        insurance.annualPremium,
        insurance.annualPremiumCurrency
      )}
    />
    <DetailRow
      label="Frequency"
      value={formatPaymentFrequency(insurance.paymentFrequency)}
    />
    {insurance.startDate && (
      <DetailRow label="Start Date" value={formatDate(insurance.startDate)} />
    )}
    {insurance.endDate && (
      <DetailRow label="End Date" value={formatDate(insurance.endDate)} />
    )}
    {insurance.notes && (
      <div className="mt-3 pt-3 border-t border-[#e2e6f0] dark:border-[#2a2e3f]">
        <p className="text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] mb-1">
          Notes
        </p>
        <RichTextDisplay
          content={insurance.notes}
          className="text-sm text-[#3d4463] dark:text-[#c4c8db]"
        />
      </div>
    )}
  </div>
);

const TaxCard = ({
  tax,
  formatDate,
  onEdit,
  onDelete,
}: {
  tax: PropertyTaxResponse;
  formatDate: (d: string | Date) => string;
  onEdit: () => void;
  onDelete: () => void;
}) => (
  <div className="bg-white dark:bg-[#1e2235] rounded-xl border border-[#e2e6f0] dark:border-[#2a2e3f] p-4">
    <div className="flex items-center justify-between mb-3">
      <h5 className="text-sm font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
        {formatTaxType(tax.taxType)}
      </h5>
      <div className="flex items-center gap-2">
        <StatusBadge
          label={formatTaxStatus(tax.status)}
          variant={taxStatusVariant(tax.status)}
        />
        <div className="flex items-center gap-1">
          <ActionButton icon={Pencil} label="Edit" onClick={onEdit} />
          <ActionButton
            icon={Trash2}
            label="Delete"
            onClick={onDelete}
            variant="danger"
          />
        </div>
      </div>
    </div>
    {tax.authority && <DetailRow label="Authority" value={tax.authority} />}
    <DetailRow
      label="Annual Amount"
      value={formatMoney(tax.annualAmount, tax.currency)}
    />
    <DetailRow
      label="Frequency"
      value={formatPaymentFrequency(tax.paymentFrequency)}
    />
    {tax.taxYear != null && (
      <DetailRow label="Tax Year" value={String(tax.taxYear)} />
    )}
    {tax.startDate && (
      <DetailRow label="Start Date" value={formatDate(tax.startDate)} />
    )}
    {tax.endDate && (
      <DetailRow label="End Date" value={formatDate(tax.endDate)} />
    )}
    {tax.notes && (
      <div className="mt-3 pt-3 border-t border-[#e2e6f0] dark:border-[#2a2e3f]">
        <p className="text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] mb-1">
          Notes
        </p>
        <RichTextDisplay
          content={tax.notes}
          className="text-sm text-[#3d4463] dark:text-[#c4c8db]"
        />
      </div>
    )}
  </div>
);

const FeeCard = ({
  fee,
  formatDate,
  onEdit,
  onDelete,
}: {
  fee: PropertyFeeResponse;
  formatDate: (d: string | Date) => string;
  onEdit: () => void;
  onDelete: () => void;
}) => (
  <div className="bg-white dark:bg-[#1e2235] rounded-xl border border-[#e2e6f0] dark:border-[#2a2e3f] p-4">
    <div className="flex items-center justify-between mb-3">
      <h5 className="text-sm font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
        {formatFeeType(fee.feeType)}
      </h5>
      <div className="flex items-center gap-2">
        <StatusBadge
          label={formatFeeStatus(fee.status)}
          variant={feeStatusVariant(fee.status)}
        />
        <div className="flex items-center gap-1">
          <ActionButton icon={Pencil} label="Edit" onClick={onEdit} />
          <ActionButton
            icon={Trash2}
            label="Delete"
            onClick={onDelete}
            variant="danger"
          />
        </div>
      </div>
    </div>
    {fee.name && <DetailRow label="Name" value={fee.name} />}
    <DetailRow
      label="Annual Amount"
      value={formatMoney(fee.annualAmount, fee.currency)}
    />
    <DetailRow
      label="Frequency"
      value={formatPaymentFrequency(fee.paymentFrequency)}
    />
    {fee.startDate && (
      <DetailRow label="Start Date" value={formatDate(fee.startDate)} />
    )}
    {fee.endDate && (
      <DetailRow label="End Date" value={formatDate(fee.endDate)} />
    )}
    {fee.notes && (
      <div className="mt-3 pt-3 border-t border-[#e2e6f0] dark:border-[#2a2e3f]">
        <p className="text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] mb-1">
          Notes
        </p>
        <RichTextDisplay
          content={fee.notes}
          className="text-sm text-[#3d4463] dark:text-[#c4c8db]"
        />
      </div>
    )}
  </div>
);

const RecurringCostsSection = ({
  insurances,
  taxes,
  fees,
  summary,
  formatDate,
  onAddInsurance,
  onAddTax,
  onAddFee,
  onEditInsurance,
  onDeleteInsurance,
  onEditTax,
  onDeleteTax,
  onEditFee,
  onDeleteFee,
}: {
  insurances: PropertyInsuranceResponse[];
  taxes: PropertyTaxResponse[];
  fees: PropertyFeeResponse[];
  summary: PropertyFinancialSummaryResponse;
  formatDate: (d: string | Date) => string;
  onAddInsurance: () => void;
  onAddTax: () => void;
  onAddFee: () => void;
  onEditInsurance: (i: PropertyInsuranceResponse) => void;
  onDeleteInsurance: (i: PropertyInsuranceResponse) => void;
  onEditTax: (t: PropertyTaxResponse) => void;
  onDeleteTax: (t: PropertyTaxResponse) => void;
  onEditFee: (f: PropertyFeeResponse) => void;
  onDeleteFee: (f: PropertyFeeResponse) => void;
}) => (
  <div>
    <SectionHeader icon={Calendar} title="Recurring Costs" />
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
      {/* Insurances */}
      <div>
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <Shield className="h-4 w-4 text-[#6b7194] dark:text-[#8b90a8]" />
            <h4 className="text-sm font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
              Insurances
            </h4>
          </div>
          <div className="flex items-center gap-2">
            {summary.totalAnnualInsurance != null && (
              <span className="text-xs font-medium text-[#6b7194] dark:text-[#8b90a8]">
                {formatMoney(summary.totalAnnualInsurance, summary.currency)}
                /yr
              </span>
            )}
            <AddButton label="Add" onClick={onAddInsurance} />
          </div>
        </div>
        {insurances.length > 0 ? (
          <div className="space-y-3">
            {sortRecurringCosts(insurances).map((i) => (
              <InsuranceCard
                key={i.identifier}
                insurance={i}
                formatDate={formatDate}
                onEdit={() => onEditInsurance(i)}
                onDelete={() => onDeleteInsurance(i)}
              />
            ))}
          </div>
        ) : (
          <EmptyState message="No insurances." />
        )}
      </div>

      {/* Taxes */}
      <div>
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <Receipt className="h-4 w-4 text-[#6b7194] dark:text-[#8b90a8]" />
            <h4 className="text-sm font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
              Taxes
            </h4>
          </div>
          <div className="flex items-center gap-2">
            {summary.totalAnnualTaxes != null && (
              <span className="text-xs font-medium text-[#6b7194] dark:text-[#8b90a8]">
                {formatMoney(summary.totalAnnualTaxes, summary.currency)}/yr
              </span>
            )}
            <AddButton label="Add" onClick={onAddTax} />
          </div>
        </div>
        {taxes.length > 0 ? (
          <div className="space-y-3">
            {sortRecurringCosts(taxes).map((t) => (
              <TaxCard
                key={t.identifier}
                tax={t}
                formatDate={formatDate}
                onEdit={() => onEditTax(t)}
                onDelete={() => onDeleteTax(t)}
              />
            ))}
          </div>
        ) : (
          <EmptyState message="No taxes." />
        )}
      </div>

      {/* Fees */}
      <div>
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <Wallet className="h-4 w-4 text-[#6b7194] dark:text-[#8b90a8]" />
            <h4 className="text-sm font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
              Fees
            </h4>
          </div>
          <div className="flex items-center gap-2">
            {summary.totalAnnualFees != null && (
              <span className="text-xs font-medium text-[#6b7194] dark:text-[#8b90a8]">
                {formatMoney(summary.totalAnnualFees, summary.currency)}/yr
              </span>
            )}
            <AddButton label="Add" onClick={onAddFee} />
          </div>
        </div>
        {fees.length > 0 ? (
          <div className="space-y-3">
            {sortRecurringCosts(fees).map((f) => (
              <FeeCard
                key={f.identifier}
                fee={f}
                formatDate={formatDate}
                onEdit={() => onEditFee(f)}
                onDelete={() => onDeleteFee(f)}
              />
            ))}
          </div>
        ) : (
          <EmptyState message="No fees." />
        )}
      </div>
    </div>
  </div>
);

// ============================================================
// Main component
// ============================================================

export const PropertyFinancialsTab = ({
  propertyId,
}: PropertyFinancialsTabProps) => {
  const {
    data: summary,
    isLoading,
    error,
    refetch,
  } = useFinancialSummary(propertyId);
  const { formatDate } = useFormatDate();

  // Modal state
  const [modal, setModal] = useState<ModalState>(null);
  const [deleteState, setDeleteState] = useState<DeleteState>(null);

  // Delete mutations
  const deleteValuation = useDeleteValuation(propertyId);
  const deleteFinancing = useDeleteFinancing(propertyId);
  const deleteInsurance = useDeleteInsurance(propertyId);
  const deleteTax = useDeleteTax(propertyId);
  const deleteFee = useDeleteFee(propertyId);

  const handleDelete = () => {
    if (!deleteState) {
      return;
    }
    const onSuccess = () => setDeleteState(null);
    switch (deleteState.entity) {
      case 'valuation':
        deleteValuation.mutate(deleteState.identifier, { onSuccess });
        break;
      case 'financing':
        deleteFinancing.mutate(deleteState.identifier, { onSuccess });
        break;
      case 'insurance':
        deleteInsurance.mutate(deleteState.identifier, { onSuccess });
        break;
      case 'tax':
        deleteTax.mutate(deleteState.identifier, { onSuccess });
        break;
      case 'fee':
        deleteFee.mutate(deleteState.identifier, { onSuccess });
        break;
    }
  };

  const isDeleting =
    deleteValuation.isPending ||
    deleteFinancing.isPending ||
    deleteInsurance.isPending ||
    deleteTax.isPending ||
    deleteFee.isPending;

  if (isLoading) {
    return <LoadingSpinner />;
  }

  if (error) {
    return (
      <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-400 px-4 py-3 rounded-lg text-sm">
        Failed to load financial data.{' '}
        <button
          onClick={() => refetch()}
          className="underline hover:no-underline font-medium inline-flex items-center gap-1"
        >
          <RefreshCw className="h-3 w-3" />
          Retry
        </button>
      </div>
    );
  }

  if (!summary) {
    return null;
  }

  return (
    <div className="space-y-8">
      {/* Section A: Key Metrics */}
      <KeyMetricsSection summary={summary} />

      {/* Section B: Acquisition */}
      <AcquisitionSection
        acquisition={summary.acquisition}
        formatDate={formatDate}
        onEdit={() =>
          setModal({ type: 'acquisition', data: summary.acquisition })
        }
      />

      {/* Section C: Valuations */}
      <ValuationSection
        latest={summary.latestValuation}
        history={summary.valuationHistory}
        formatDate={formatDate}
        onAdd={() => setModal({ type: 'valuation' })}
        onEdit={(v) => setModal({ type: 'valuation', data: v })}
        onDelete={(v) =>
          setDeleteState({
            entity: 'valuation',
            identifier: v.identifier,
            label: `${formatValuationType(v.valuationType)} valuation`,
          })
        }
      />

      {/* Section D: Financings */}
      <FinancingsSection
        financings={summary.financings}
        formatDate={formatDate}
        onAdd={() => setModal({ type: 'financing' })}
        onEdit={(f) => setModal({ type: 'financing', data: f })}
        onDelete={(f) =>
          setDeleteState({
            entity: 'financing',
            identifier: f.identifier,
            label: `${formatFinancingType(f.financingType)}${f.lenderName ? ` (${f.lenderName})` : ''}`,
          })
        }
      />

      {/* Section E: Recurring Costs */}
      <RecurringCostsSection
        insurances={summary.insurances}
        taxes={summary.taxes}
        fees={summary.fees}
        summary={summary}
        formatDate={formatDate}
        onAddInsurance={() => setModal({ type: 'insurance' })}
        onAddTax={() => setModal({ type: 'tax' })}
        onAddFee={() => setModal({ type: 'fee' })}
        onEditInsurance={(i) => setModal({ type: 'insurance', data: i })}
        onDeleteInsurance={(i) =>
          setDeleteState({
            entity: 'insurance',
            identifier: i.identifier,
            label: `${formatInsuranceType(i.insuranceType)} insurance`,
          })
        }
        onEditTax={(t) => setModal({ type: 'tax', data: t })}
        onDeleteTax={(t) =>
          setDeleteState({
            entity: 'tax',
            identifier: t.identifier,
            label: `${formatTaxType(t.taxType)} tax`,
          })
        }
        onEditFee={(f) => setModal({ type: 'fee', data: f })}
        onDeleteFee={(f) =>
          setDeleteState({
            entity: 'fee',
            identifier: f.identifier,
            label: `${formatFeeType(f.feeType)} fee`,
          })
        }
      />

      {/* Modals */}
      {modal?.type === 'acquisition' && (
        <AcquisitionFormModal
          propertyId={propertyId}
          existing={modal.data}
          onClose={() => setModal(null)}
        />
      )}
      {modal?.type === 'valuation' && (
        <ValuationFormModal
          propertyId={propertyId}
          existing={modal.data}
          onClose={() => setModal(null)}
        />
      )}
      {modal?.type === 'financing' && (
        <FinancingFormModal
          propertyId={propertyId}
          existing={modal.data}
          onClose={() => setModal(null)}
        />
      )}
      {modal?.type === 'insurance' && (
        <InsuranceFormModal
          propertyId={propertyId}
          existing={modal.data}
          onClose={() => setModal(null)}
        />
      )}
      {modal?.type === 'tax' && (
        <TaxFormModal
          propertyId={propertyId}
          existing={modal.data}
          onClose={() => setModal(null)}
        />
      )}
      {modal?.type === 'fee' && (
        <FeeFormModal
          propertyId={propertyId}
          existing={modal.data}
          onClose={() => setModal(null)}
        />
      )}
      {deleteState && (
        <DeleteFinancialConfirmDialog
          title={`Delete ${deleteState.label}?`}
          message={`Are you sure you want to delete this ${deleteState.label}? This action cannot be undone.`}
          onConfirm={handleDelete}
          onCancel={() => setDeleteState(null)}
          isLoading={isDeleting}
        />
      )}
    </div>
  );
};
