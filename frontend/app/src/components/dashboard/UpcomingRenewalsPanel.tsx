import { useNavigate } from 'react-router-dom';
import { CalendarClock } from 'lucide-react';
import { useUpcomingRenewals } from '@/hooks/useContractExtensionHooks';
import { useFormatDate } from '@/hooks/useFormatDate';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import type { RenewalMode } from '@/types/contractExtension';

const RENEWAL_MODE_LABELS: Record<RenewalMode, string> = {
  NONE: 'None',
  AUTOMATIC: 'Auto',
  MANUAL: 'Manual',
};

function getDaysUntilColor(days: number): string {
  if (days <= 30) {
    return 'text-error-text';
  }
  if (days <= 90) {
    return 'text-warning-text';
  }
  return 'text-text-secondary';
}

export const UpcomingRenewalsPanel = () => {
  const navigate = useNavigate();
  const { formatDate } = useFormatDate();
  const { data: renewals, isLoading } = useUpcomingRenewals();

  if (isLoading) {
    return (
      <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
        <div className="flex items-center gap-3 mb-4">
          <CalendarClock className="h-5 w-5 text-primary-500" />
          <h2 className="text-lg font-semibold text-text-primary">
            Upcoming Renewals
          </h2>
        </div>
        <div className="flex justify-center py-4">
          <LoadingSpinner />
        </div>
      </div>
    );
  }

  if (!renewals || renewals.length === 0) {
    return null;
  }

  const displayed = renewals.slice(0, 10);

  return (
    <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
      <div className="flex items-center gap-3 mb-4">
        <CalendarClock className="h-5 w-5 text-primary-500" />
        <h2 className="text-lg font-semibold text-text-primary">
          Upcoming Renewals
        </h2>
        <span className="text-sm text-text-secondary">({renewals.length})</span>
      </div>

      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-border-default">
          <thead>
            <tr>
              <th className="px-3 py-2 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                Property
              </th>
              <th className="px-3 py-2 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                Contact
              </th>
              <th className="px-3 py-2 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                End Date
              </th>
              <th className="px-3 py-2 text-right text-xs font-medium text-text-secondary uppercase tracking-wider">
                Days Left
              </th>
              <th className="px-3 py-2 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                Mode
              </th>
              <th className="px-3 py-2 text-right text-xs font-medium text-text-secondary uppercase tracking-wider">
                Rent
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border-default">
            {displayed.map((renewal) => (
              <tr
                key={renewal.contractIdentifier}
                className="hover:bg-surface-inset cursor-pointer"
                onClick={() =>
                  navigate(
                    `/contracts/${renewal.contractIdentifier}?tab=extensions`
                  )
                }
              >
                <td className="px-3 py-2 text-sm text-text-primary whitespace-nowrap">
                  {renewal.propertyName ?? '-'}
                </td>
                <td className="px-3 py-2 text-sm text-text-primary whitespace-nowrap">
                  {renewal.contactName ?? '-'}
                </td>
                <td className="px-3 py-2 text-sm text-text-primary whitespace-nowrap">
                  {formatDate(renewal.effectiveEndDate)}
                </td>
                <td
                  className={`px-3 py-2 text-sm font-medium text-right whitespace-nowrap ${getDaysUntilColor(renewal.daysUntilExpiry)}`}
                >
                  {renewal.daysUntilExpiry}d
                </td>
                <td className="px-3 py-2 text-sm whitespace-nowrap">
                  <span
                    className={`px-1.5 py-0.5 rounded text-xs font-medium ${
                      renewal.renewalMode === 'AUTOMATIC'
                        ? 'bg-info-bg text-info-text'
                        : renewal.renewalMode === 'MANUAL'
                          ? 'bg-surface-inset text-text-secondary'
                          : 'bg-surface-inset text-text-muted'
                    }`}
                  >
                    {RENEWAL_MODE_LABELS[renewal.renewalMode]}
                  </span>
                </td>
                <td className="px-3 py-2 text-sm text-text-primary text-right whitespace-nowrap">
                  {renewal.currency} {renewal.currentRentAmount.toFixed(2)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};
