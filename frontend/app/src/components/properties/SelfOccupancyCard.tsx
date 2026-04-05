import { useTranslation } from 'react-i18next';
import { Button, RichTextDisplay } from '@buurman/ui';
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
  const { t } = useTranslation('properties');
  const { formatDate } = useFormatDate();
  const isActive = !period.endDate || new Date(period.endDate) >= new Date();

  return (
    <div className="bg-info-bg rounded-lg p-6 border border-info-border">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <Home className="h-5 w-5 text-info-text" />
          <h3 className="text-lg font-semibold text-info-text">
            {t('selfOccupancy.title')}
          </h3>
          {isActive && (
            <span className="px-2 py-0.5 rounded-full text-xs font-medium bg-info-bg text-info-text">
              {t('selfOccupancy.active')}
            </span>
          )}
        </div>
        <div className="flex items-center gap-2">
          {isActive && canEdit && (
            <Button variant="secondary" size="sm" onClick={onEnd}>
              {t('selfOccupancy.endOccupancy')}
            </Button>
          )}
          {canAdmin && (
            <Button
              variant="danger"
              size="sm"
              leftIcon={<Trash2 />}
              onClick={onDelete}
            >
              {t('buttons.delete', { ns: 'common' })}
            </Button>
          )}
        </div>
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div className="flex items-center gap-2">
          <Calendar className="h-4 w-4 text-info-text" />
          <div>
            <div className="text-xs text-text-secondary">
              {t('selfOccupancy.period')}
            </div>
            <div className="text-sm text-text-primary">
              {formatDate(period.startDate)} &mdash;{' '}
              {period.endDate
                ? formatDate(period.endDate)
                : t('timeline.ongoing')}
            </div>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <Home className="h-4 w-4 text-info-text" />
          <div>
            <div className="text-xs text-text-secondary">
              {t('selfOccupancy.type')}
            </div>
            <div className="text-sm text-text-primary">
              {OCCUPANCY_TYPE_LABELS[period.type]}
            </div>
          </div>
        </div>

        {period.occupantName && (
          <div className="flex items-center gap-2">
            <User className="h-4 w-4 text-info-text" />
            <div>
              <div className="text-xs text-text-secondary">
                {t('selfOccupancy.occupant')}
              </div>
              <div className="text-sm text-text-primary">
                {period.occupantName}
              </div>
            </div>
          </div>
        )}

        {period.monthlyImputedRent != null && (
          <div className="flex items-center gap-2">
            <DollarSign className="h-4 w-4 text-info-text" />
            <div>
              <div className="text-xs text-text-secondary">
                {t('selfOccupancy.imputedRent')}
              </div>
              <div className="text-sm text-text-primary">
                {period.monthlyImputedRent.toLocaleString('nl-NL', {
                  minimumFractionDigits: 2,
                })}{' '}
                {t('selfOccupancy.perMonth')}
              </div>
            </div>
          </div>
        )}

        {period.endReason && (
          <div className="flex items-center gap-2">
            <FileText className="h-4 w-4 text-info-text" />
            <div>
              <div className="text-xs text-text-secondary">
                {t('selfOccupancy.endReason')}
              </div>
              <div className="text-sm text-text-primary">
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
        <div className="mt-4 pt-4 border-t border-info-border">
          <div className="text-xs text-text-secondary mb-1">
            {t('selfOccupancy.notes')}
          </div>
          <RichTextDisplay
            content={period.notes}
            className="text-sm text-text-primary"
          />
        </div>
      )}
    </div>
  );
};
