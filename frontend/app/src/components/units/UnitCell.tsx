import { useTranslation } from 'react-i18next';
import { UnitStatus } from '@/types/unit';
import type { UnitGridRow } from '@/types/unit';

// Mirrors PropertyCell's status-dot + label layout, scaled down for a single grid cell
// rather than a full clickable list row (the grid itself owns the click behavior).
const statusDotColors: Record<string, string> = {
  [UnitStatus.VACANT]: 'bg-success-text',
  [UnitStatus.OCCUPIED]: 'bg-info-text',
  [UnitStatus.SELF_OCCUPIED]: 'bg-info-text',
  [UnitStatus.MAINTENANCE]: 'bg-warning-text',
  [UnitStatus.UNAVAILABLE]: 'bg-neutral-400',
  [UnitStatus.UNDER_RENOVATION]: 'bg-warning-text',
  [UnitStatus.FALLOW]: 'bg-neutral-500',
  [UnitStatus.LISTED]: 'bg-info-text',
};

interface UnitCellProps {
  row: Pick<UnitGridRow, 'unitNumber' | 'name' | 'unitType' | 'status'>;
}

export const UnitCell = ({ row }: UnitCellProps) => {
  const { t } = useTranslation('units');

  return (
    <div className="flex items-center gap-2 min-w-0">
      <span
        className={`w-2 h-2 rounded-full flex-shrink-0 ${statusDotColors[row.status] ?? 'bg-neutral-400'}`}
        title={t(`status.${row.status}`)}
      />
      <div className="min-w-0">
        <div className="text-sm font-medium text-text-primary truncate">
          {row.name || t('detail.title', { number: row.unitNumber })}
        </div>
        <div className="text-xs text-text-secondary truncate">
          {t(`type.${row.unitType}`)}
        </div>
      </div>
    </div>
  );
};
