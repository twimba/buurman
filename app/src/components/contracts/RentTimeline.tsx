import { useState } from 'react';
import {
  ChevronDown,
  ChevronUp,
  TrendingUp,
  TrendingDown,
  DollarSign,
  Plus,
  Trash2,
} from 'lucide-react';
import { RentPeriodResponse, ContractStatus } from '@/types/contract';
import {
  useRentPeriods,
  useDeleteRentPeriod,
} from '@/hooks/useRentPeriodHooks';
import { AdjustRentModal } from './AdjustRentModal';
import { useAddRentPeriod } from '@/hooks/useRentPeriodHooks';
import { useFormatDate } from '@/hooks/useFormatDate';
import { RichTextDisplay } from '@/components/common/RichTextDisplay';
import { useTeam } from '@/context/TeamContext';

interface RentTimelineProps {
  contractIdentifier: string;
  contractStatus: ContractStatus;
  currency: string;
  currentRentAmount: number;
  paymentFrequency: string;
}

function isFuturePeriod(effectiveFrom: string): boolean {
  return new Date(effectiveFrom) > new Date();
}

export const RentTimeline = ({
  contractIdentifier,
  contractStatus,
  currency,
  currentRentAmount,
  paymentFrequency,
}: RentTimelineProps) => {
  const { canEditData } = useTeam();
  const { formatDate } = useFormatDate();
  const [isExpanded, setIsExpanded] = useState(false);
  const [showAdjustModal, setShowAdjustModal] = useState(false);

  const { data: periods = [], isLoading } = useRentPeriods(contractIdentifier);
  const addRentPeriod = useAddRentPeriod(contractIdentifier);
  const deleteRentPeriod = useDeleteRentPeriod(contractIdentifier);

  const handleAddRentPeriod = (
    rentAmount: number,
    effectiveFrom: string,
    notes?: string
  ) => {
    addRentPeriod.mutate(
      { rentAmount, effectiveFrom, notes },
      { onSuccess: () => setShowAdjustModal(false) }
    );
  };

  const handleDeletePeriod = (periodIdentifier: string) => {
    if (window.confirm('Are you sure you want to delete this rent period?')) {
      deleteRentPeriod.mutate(periodIdentifier);
    }
  };

  const canAdjustRent = canEditData && contractStatus === ContractStatus.ACTIVE;
  const hasHistory = periods.length > 1;

  return (
    <>
      {/* Current Rent */}
      <div className="flex items-center gap-3">
        <DollarSign className="h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180]" />
        <div className="flex-1">
          <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
            Rent Amount
          </p>
          <p className="font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
            {currency} {currentRentAmount.toFixed(2)} /{' '}
            {paymentFrequency.toLowerCase()}
          </p>
        </div>
        {canAdjustRent && (
          <button
            onClick={() => setShowAdjustModal(true)}
            className="flex items-center gap-1 px-3 py-1.5 text-xs font-medium text-[#5c7cfa] bg-[#5c7cfa]/10 rounded-md hover:bg-[#5c7cfa]/20 transition-colors"
          >
            <Plus className="h-3.5 w-3.5" />
            Adjust
          </button>
        )}
      </div>

      {/* Rent History Toggle */}
      {hasHistory && (
        <div className="mt-2">
          <button
            onClick={() => setIsExpanded(!isExpanded)}
            className="flex items-center gap-1.5 text-sm text-[#6b7194] dark:text-[#8b90a8] hover:text-[#3d4463] dark:hover:text-[#c4c8db] transition-colors"
          >
            {isExpanded ? (
              <ChevronUp className="h-4 w-4" />
            ) : (
              <ChevronDown className="h-4 w-4" />
            )}
            Rent History ({periods.length} period
            {periods.length !== 1 ? 's' : ''})
          </button>

          {isExpanded && (
            <div className="mt-3 space-y-2">
              {isLoading ? (
                <p className="text-sm text-[#9ca0b8]">Loading...</p>
              ) : (
                periods.map((period) => (
                  <RentPeriodRow
                    key={period.identifier}
                    period={period}
                    currency={currency}
                    formatDate={formatDate}
                    onDelete={
                      canEditData && isFuturePeriod(period.effectiveFrom)
                        ? () => handleDeletePeriod(period.identifier)
                        : undefined
                    }
                  />
                ))
              )}
            </div>
          )}
        </div>
      )}

      {/* Adjust Rent Modal */}
      {showAdjustModal && (
        <AdjustRentModal
          currentRent={currentRentAmount}
          currency={currency}
          onClose={() => setShowAdjustModal(false)}
          onConfirm={handleAddRentPeriod}
          isLoading={addRentPeriod.isPending}
        />
      )}
    </>
  );
};

function RentPeriodRow({
  period,
  currency,
  formatDate,
  onDelete,
}: {
  period: RentPeriodResponse;
  currency: string;
  formatDate: (date: string) => string;
  onDelete?: () => void;
}) {
  return (
    <div className="flex items-start gap-3 p-3 bg-[#f8f9fc] dark:bg-[#1a1d28] rounded-lg">
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2 flex-wrap">
          <span className="font-medium text-sm text-[#1a1d2e] dark:text-[#eef0f6]">
            {currency} {period.rentAmount.toFixed(2)}
          </span>
          {period.percentageChange != null && (
            <PercentageChangeBadge change={period.percentageChange} />
          )}
        </div>
        <p className="text-xs text-[#9ca0b8] dark:text-[#5c6180] mt-0.5">
          {formatDate(period.effectiveFrom)}
          {period.effectiveTo
            ? ` - ${formatDate(period.effectiveTo)}`
            : ' - Present'}
        </p>
        {period.notes && (
          <div className="mt-1 text-xs text-[#6b7194] dark:text-[#8b90a8]">
            <RichTextDisplay content={period.notes} />
          </div>
        )}
      </div>
      {onDelete && (
        <button
          onClick={onDelete}
          className="p-1 text-[#9ca0b8] dark:text-[#5c6180] hover:text-red-500 dark:hover:text-red-400 transition-colors shrink-0"
          title="Delete rent period"
        >
          <Trash2 className="h-3.5 w-3.5" />
        </button>
      )}
    </div>
  );
}

function PercentageChangeBadge({ change }: { change: number }) {
  if (change > 0) {
    return (
      <span className="inline-flex items-center gap-0.5 px-1.5 py-0.5 text-xs font-medium rounded-full bg-emerald-50 text-emerald-600 dark:bg-emerald-500/10 dark:text-emerald-400">
        <TrendingUp className="h-3 w-3" />+{change.toFixed(1)}%
      </span>
    );
  }
  if (change < 0) {
    return (
      <span className="inline-flex items-center gap-0.5 px-1.5 py-0.5 text-xs font-medium rounded-full bg-red-50 text-red-600 dark:bg-red-500/10 dark:text-red-400">
        <TrendingDown className="h-3 w-3" />
        {change.toFixed(1)}%
      </span>
    );
  }
  return null;
}
