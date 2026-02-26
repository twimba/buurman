import { Button } from '@/components/ui';
import { RichTextDisplay } from '@/components/common/RichTextDisplay';
import {
  OccupancyPeriodResponse,
  OCCUPANCY_TYPE_LABELS,
  OCCUPANCY_END_REASON_LABELS,
  OccupancyEndReason,
} from '@/types/occupancyPeriod';
import {
  Home,
  Calendar,
  User,
  FileText,
  DollarSign,
  Trash2,
} from 'lucide-react';
import { useFormatDate } from '@/hooks/useFormatDate';

interface SelfOccupancyCardProps {
  period: OccupancyPeriodResponse;
  canEdit: boolean;
  canAdmin: boolean;
  onEnd: () => void;
  onDelete: () => void;
}

export const SelfOccupancyCard = ({
  period,
  canEdit,
  canAdmin,
  onEnd,
  onDelete,
}: SelfOccupancyCardProps) => {
  const { formatDate } = useFormatDate();
  const isActive = !period.endDate || new Date(period.endDate) >= new Date();

  return (
    <div className="bg-indigo-50 dark:bg-indigo-900/20 rounded-xl p-6 border border-indigo-200 dark:border-indigo-800">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <Home className="h-5 w-5 text-indigo-600 dark:text-indigo-400" />
          <h3 className="text-lg font-semibold text-indigo-900 dark:text-indigo-100">
            Self-Occupancy
          </h3>
          {isActive && (
            <span className="px-2 py-0.5 rounded-full text-xs font-medium bg-indigo-100 dark:bg-indigo-800 text-indigo-700 dark:text-indigo-300">
              Active
            </span>
          )}
        </div>
        <div className="flex items-center gap-2">
          {isActive && canEdit && (
            <Button variant="secondary" size="sm" onClick={onEnd}>
              End Occupancy
            </Button>
          )}
          {canAdmin && (
            <Button
              variant="danger"
              size="sm"
              leftIcon={<Trash2 />}
              onClick={onDelete}
            >
              Delete
            </Button>
          )}
        </div>
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div className="flex items-center gap-2">
          <Calendar className="h-4 w-4 text-indigo-500 dark:text-indigo-400" />
          <div>
            <div className="text-xs text-[#6b7194] dark:text-[#8b90a8]">
              Period
            </div>
            <div className="text-sm text-[#1a1d2e] dark:text-[#eef0f6]">
              {formatDate(period.startDate)} &mdash;{' '}
              {period.endDate ? formatDate(period.endDate) : 'Ongoing'}
            </div>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <Home className="h-4 w-4 text-indigo-500 dark:text-indigo-400" />
          <div>
            <div className="text-xs text-[#6b7194] dark:text-[#8b90a8]">
              Type
            </div>
            <div className="text-sm text-[#1a1d2e] dark:text-[#eef0f6]">
              {OCCUPANCY_TYPE_LABELS[period.type]}
            </div>
          </div>
        </div>

        {period.occupantName && (
          <div className="flex items-center gap-2">
            <User className="h-4 w-4 text-indigo-500 dark:text-indigo-400" />
            <div>
              <div className="text-xs text-[#6b7194] dark:text-[#8b90a8]">
                Occupant
              </div>
              <div className="text-sm text-[#1a1d2e] dark:text-[#eef0f6]">
                {period.occupantName}
              </div>
            </div>
          </div>
        )}

        {period.monthlyImputedRent != null && (
          <div className="flex items-center gap-2">
            <DollarSign className="h-4 w-4 text-indigo-500 dark:text-indigo-400" />
            <div>
              <div className="text-xs text-[#6b7194] dark:text-[#8b90a8]">
                Imputed Rent
              </div>
              <div className="text-sm text-[#1a1d2e] dark:text-[#eef0f6]">
                {period.monthlyImputedRent.toLocaleString('nl-NL', {
                  minimumFractionDigits: 2,
                })}{' '}
                / month
              </div>
            </div>
          </div>
        )}

        {period.endReason && (
          <div className="flex items-center gap-2">
            <FileText className="h-4 w-4 text-indigo-500 dark:text-indigo-400" />
            <div>
              <div className="text-xs text-[#6b7194] dark:text-[#8b90a8]">
                End Reason
              </div>
              <div className="text-sm text-[#1a1d2e] dark:text-[#eef0f6]">
                {
                  OCCUPANCY_END_REASON_LABELS[
                    period.endReason as OccupancyEndReason
                  ]
                }
              </div>
            </div>
          </div>
        )}
      </div>

      {period.notes && (
        <div className="mt-4 pt-4 border-t border-indigo-200 dark:border-indigo-800">
          <div className="text-xs text-[#6b7194] dark:text-[#8b90a8] mb-1">
            Notes
          </div>
          <RichTextDisplay
            content={period.notes}
            className="text-sm text-[#1a1d2e] dark:text-[#eef0f6]"
          />
        </div>
      )}
    </div>
  );
};
