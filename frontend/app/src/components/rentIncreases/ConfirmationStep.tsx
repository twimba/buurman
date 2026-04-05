import { useMemo } from 'react';
import { CheckCircle, XCircle } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import type {
  ApplyRentIncreasesResponse,
  RentIncreaseContractPreview,
  RentIncreaseItem,
} from '@/types/rentIncrease';
import { useTranslation } from 'react-i18next';

interface ConfirmationStepProps {
  response: ApplyRentIncreasesResponse;
  contracts: RentIncreaseContractPreview[];
  increases: RentIncreaseItem[];
  onBack?: () => void;
}

export const ConfirmationStep = ({
  response,
  contracts,
  increases,
  onBack,
}: ConfirmationStepProps) => {
  const { t } = useTranslation('contracts');
  const navigate = useNavigate();
  const { results, summary } = response;

  const successCount = results.filter((r) => r.success).length;

  const totalsByCurrency = useMemo(() => {
    const totals: Record<
      string,
      { previousTotal: number; newTotal: number; count: number }
    > = {};
    results.forEach((result) => {
      if (!result.success) {
        return;
      }
      const contract = contracts.find(
        (c) => c.contractIdentifier === result.contractIdentifier
      );
      const currency = contract?.currency ?? 'EUR';
      if (!totals[currency]) {
        totals[currency] = { previousTotal: 0, newTotal: 0, count: 0 };
      }
      totals[currency].previousTotal += result.previousRentAmount;
      totals[currency].newTotal += result.newRentAmount;
      totals[currency].count += 1;
    });
    return totals;
  }, [results, contracts]);

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
      {/* Summary */}
      <div className="bg-surface-card rounded-lg border border-border-default p-6">
        <div className="flex items-center gap-3 mb-4">
          {summary.totalFailed === 0 ? (
            <CheckCircle className="h-8 w-8 text-success-text" />
          ) : (
            <XCircle className="h-8 w-8 text-warning-text" />
          )}
          <div>
            <h3 className="text-xl font-bold text-text-primary">
              {summary.totalFailed === 0
                ? t('rentIncrease.allSuccess')
                : t('rentIncrease.partialSuccess', {
                    success: successCount,
                    total: results.length,
                  })}
            </h3>
            {summary.totalFailed > 0 && (
              <p className="text-sm text-warning-text">
                {t('rentIncrease.increasesFailed', {
                  count: summary.totalFailed,
                })}
              </p>
            )}
            {summary.totalPaymentsGenerated > 0 && (
              <p className="text-sm text-info-text">
                {t('rentIncrease.adjustmentPayments', {
                  count: summary.totalPaymentsGenerated,
                })}
              </p>
            )}
          </div>
        </div>

        {/* Currency Totals */}
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {Object.entries(totalsByCurrency).map(([currency, totals]) => (
            <div
              key={currency}
              className="p-4 rounded-lg bg-surface-page dark:bg-surface-card"
            >
              <p className="text-sm text-text-secondary mb-2">
                {currency} (
                {t('rentIncrease.contract', {
                  count: totals.count,
                })}
                )
              </p>
              <div className="space-y-1">
                <div className="flex justify-between">
                  <span className="text-sm text-text-secondary">
                    {t('rentIncrease.previous')}
                  </span>
                  <span className="font-medium text-text-primary">
                    {formatMoney(totals.previousTotal, currency)}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-sm text-text-secondary">
                    {t('rentIncrease.new')}
                  </span>
                  <span className="font-semibold text-text-primary">
                    {formatMoney(totals.newTotal, currency)}
                  </span>
                </div>
                <div className="flex justify-between border-t border-border-default pt-1">
                  <span className="text-sm text-text-secondary">
                    {t('rentIncrease.increase')}
                  </span>
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

      {/* Results Table */}
      <div className="bg-surface-card rounded-lg border border-border-default overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border-default">
                <th className="text-left px-4 py-3 font-semibold text-text-secondary">
                  {t('rentIncrease.status')}
                </th>
                <th className="text-left px-4 py-3 font-semibold text-text-secondary">
                  {t('rentIncrease.tableHeaders.property')}
                </th>
                <th className="text-right px-4 py-3 font-semibold text-text-secondary">
                  {t('rentIncrease.previous')}
                </th>
                <th className="text-right px-4 py-3 font-semibold text-text-secondary">
                  {t('rentIncrease.new')}
                </th>
                <th className="text-right px-4 py-3 font-semibold text-text-secondary">
                  {t('rentIncrease.increase')}
                </th>
                <th className="text-left px-4 py-3 font-semibold text-text-secondary">
                  {t('rentIncrease.tableHeaders.effectiveDate')}
                </th>
              </tr>
            </thead>
            <tbody>
              {results.map((result) => {
                const contract = contracts.find(
                  (c) => c.contractIdentifier === result.contractIdentifier
                );
                const inc = increases.find(
                  (i) => i.contractIdentifier === result.contractIdentifier
                );
                const currency = contract?.currency ?? 'EUR';
                const pct = inc?.increasePercentage ?? 0;

                return (
                  <tr
                    key={result.contractIdentifier}
                    className={`border-b border-border-default last:border-b-0 ${
                      !result.success ? 'bg-error-bg/50' : ''
                    }`}
                  >
                    <td className="px-4 py-3">
                      {result.success ? (
                        <CheckCircle className="h-4 w-4 text-success-text" />
                      ) : (
                        <div className="flex items-center gap-1">
                          <XCircle className="h-4 w-4 text-error-text" />
                          {result.error && (
                            <span className="text-xs text-error-text">
                              {result.error}
                            </span>
                          )}
                        </div>
                      )}
                    </td>
                    <td className="px-4 py-3 font-medium text-text-primary">
                      {result.propertyName}
                    </td>
                    <td className="px-4 py-3 text-right text-text-secondary">
                      {formatMoney(result.previousRentAmount, currency)}
                    </td>
                    <td className="px-4 py-3 text-right font-semibold text-text-primary">
                      {formatMoney(result.newRentAmount, currency)}
                    </td>
                    <td
                      className={`px-4 py-3 text-right font-medium ${
                        pct > 0
                          ? 'text-success-text'
                          : pct < 0
                            ? 'text-error-text'
                            : 'text-text-secondary'
                      }`}
                    >
                      {pct > 0 ? '+' : ''}
                      {pct.toFixed(1)}%
                    </td>
                    <td className="px-4 py-3 text-text-secondary">
                      {result.effectiveDate}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {/* Actions */}
      <div className="flex justify-between">
        <div>
          {summary.totalFailed > 0 && onBack && (
            <button
              onClick={onBack}
              className="px-6 py-2 rounded border border-border-default text-text-secondary hover:bg-surface-inset transition-colors"
            >
              {t('rentIncrease.backToAdjust')}
            </button>
          )}
        </div>
        <div className="flex gap-3">
          <button
            onClick={() => navigate('/rent-regulations')}
            className="px-6 py-2 rounded border border-border-default text-text-secondary hover:bg-surface-inset transition-colors"
          >
            {t('rentRegulations.title')}
          </button>
          <button
            onClick={() => navigate('/contracts')}
            className="bg-primary-500 text-white px-6 py-2 rounded hover:bg-primary-600 transition-colors"
          >
            {t('list.title')}
          </button>
        </div>
      </div>
    </div>
  );
};
