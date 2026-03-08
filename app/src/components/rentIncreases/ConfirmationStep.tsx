import { useMemo } from 'react';
import { CheckCircle, XCircle } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import type {
  ApplyRentIncreasesResponse,
  RentIncreaseContractPreview,
  RentIncreaseItem,
} from '@/types/rentIncrease';

interface ConfirmationStepProps {
  response: ApplyRentIncreasesResponse;
  contracts: RentIncreaseContractPreview[];
  increases: RentIncreaseItem[];
}

export const ConfirmationStep = ({
  response,
  contracts,
  increases,
}: ConfirmationStepProps) => {
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
      <div className="bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] p-6">
        <div className="flex items-center gap-3 mb-4">
          {summary.totalFailed === 0 ? (
            <CheckCircle className="h-8 w-8 text-green-600 dark:text-green-400" />
          ) : (
            <XCircle className="h-8 w-8 text-amber-600 dark:text-amber-400" />
          )}
          <div>
            <h3 className="text-xl font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
              {summary.totalFailed === 0
                ? 'All rent adjustments applied successfully'
                : `${successCount} of ${results.length} adjustments applied`}
            </h3>
            {summary.totalFailed > 0 && (
              <p className="text-sm text-amber-600 dark:text-amber-400">
                {summary.totalFailed}{' '}
                {summary.totalFailed === 1 ? 'increase' : 'increases'} failed
              </p>
            )}
            {summary.totalPaymentsGenerated > 0 && (
              <p className="text-sm text-blue-600 dark:text-blue-400">
                {summary.totalPaymentsGenerated} adjustment{' '}
                {summary.totalPaymentsGenerated === 1
                  ? 'payment'
                  : 'payments'}{' '}
                created for retroactive changes
              </p>
            )}
          </div>
        </div>

        {/* Currency Totals */}
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
                    {formatMoney(totals.previousTotal, currency)}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-sm text-[#3d4463] dark:text-[#c4c8db]">
                    New
                  </span>
                  <span className="font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                    {formatMoney(totals.newTotal, currency)}
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
      <div className="bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-[#e2e6f0] dark:border-[#2a2e3f]">
                <th className="text-left px-4 py-3 font-semibold text-[#3d4463] dark:text-[#c4c8db]">
                  Status
                </th>
                <th className="text-left px-4 py-3 font-semibold text-[#3d4463] dark:text-[#c4c8db]">
                  Property
                </th>
                <th className="text-right px-4 py-3 font-semibold text-[#3d4463] dark:text-[#c4c8db]">
                  Previous
                </th>
                <th className="text-right px-4 py-3 font-semibold text-[#3d4463] dark:text-[#c4c8db]">
                  New
                </th>
                <th className="text-right px-4 py-3 font-semibold text-[#3d4463] dark:text-[#c4c8db]">
                  Increase
                </th>
                <th className="text-left px-4 py-3 font-semibold text-[#3d4463] dark:text-[#c4c8db]">
                  Effective Date
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
                    className={`border-b border-[#e2e6f0] dark:border-[#2a2e3f] last:border-b-0 ${
                      !result.success ? 'bg-red-50/50 dark:bg-red-900/10' : ''
                    }`}
                  >
                    <td className="px-4 py-3">
                      {result.success ? (
                        <CheckCircle className="h-4 w-4 text-green-600 dark:text-green-400" />
                      ) : (
                        <div className="flex items-center gap-1">
                          <XCircle className="h-4 w-4 text-red-600 dark:text-red-400" />
                          {result.error && (
                            <span className="text-xs text-red-600 dark:text-red-400">
                              {result.error}
                            </span>
                          )}
                        </div>
                      )}
                    </td>
                    <td className="px-4 py-3 font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                      {result.propertyName}
                    </td>
                    <td className="px-4 py-3 text-right text-[#3d4463] dark:text-[#c4c8db]">
                      {formatMoney(result.previousRentAmount, currency)}
                    </td>
                    <td className="px-4 py-3 text-right font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                      {formatMoney(result.newRentAmount, currency)}
                    </td>
                    <td
                      className={`px-4 py-3 text-right font-medium ${
                        pct > 0
                          ? 'text-emerald-600 dark:text-emerald-400'
                          : pct < 0
                            ? 'text-red-500 dark:text-red-400'
                            : 'text-[#6b7194] dark:text-[#8b90a8]'
                      }`}
                    >
                      {pct > 0 ? '+' : ''}
                      {pct.toFixed(1)}%
                    </td>
                    <td className="px-4 py-3 text-[#3d4463] dark:text-[#c4c8db]">
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
      <div className="flex justify-end gap-3">
        <button
          onClick={() => navigate('/rent-regulations')}
          className="px-6 py-2 rounded border border-[#e2e6f0] dark:border-[#2a2e3f] text-[#3d4463] dark:text-[#c4c8db] hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] transition-colors"
        >
          View Regulations
        </button>
        <button
          onClick={() => navigate('/contracts')}
          className="bg-[#5c7cfa] text-white px-6 py-2 rounded hover:bg-[#4c6ef5] transition-colors"
        >
          View Contracts
        </button>
      </div>
    </div>
  );
};
