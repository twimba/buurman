import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Plus } from 'lucide-react';
import { Button, Skeleton } from '@buurman/ui';
import { UnitGrid } from './UnitGrid';
import { useUnits, useCreateUnit } from '@/hooks/useUnitHooks';
import { UnitStatus, UnitType } from '@/types/unit';
import type { PropertyIdentifier, UnitGridRow } from '@/types/unit';
import { useTeam } from '@/context/TeamContext';
import { FeatureGate } from '@/components/FeatureGate';
import { FeatureFlags } from '@/constants/featureFlags';
import { ErrorMessage } from '@/components/ErrorMessage';

interface PropertyUnitsTabProps {
  propertyIdentifier: PropertyIdentifier;
}

/**
 * Owns the single `useUnits` call for the tab -- `UnitGrid` itself fires no requests, so a
 * 50-unit building still costs exactly one request to render this tab.
 */
export const PropertyUnitsTab = ({
  propertyIdentifier,
}: PropertyUnitsTabProps) => {
  const { t } = useTranslation(['units', 'common']);
  const navigate = useNavigate();
  const { canEditData } = useTeam();
  const { data: rows = [], isLoading, error } = useUnits(propertyIdentifier);
  const createUnit = useCreateUnit(propertyIdentifier);

  const occupied = rows.filter(
    (row) => row.status === UnitStatus.OCCUPIED
  ).length;

  const handleAddUnit = () => {
    createUnit.mutate({
      unitNumber: String(rows.length + 1),
      unitType: UnitType.APARTMENT,
    });
  };

  const handleRowClick = (row: UnitGridRow) => {
    navigate(`/properties/${propertyIdentifier}/units/${row.identifier}`);
  };

  if (error) {
    return <ErrorMessage message={t('common:errors.generic')} />;
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between gap-4">
        {isLoading ? (
          <Skeleton className="h-5 w-40" />
        ) : (
          <p className="text-sm text-text-secondary">
            {t('grid.occupancySummary', {
              occupied,
              total: rows.length,
            })}
          </p>
        )}
        <FeatureGate flag={FeatureFlags.MULTI_UNIT}>
          <Button
            variant="secondary"
            leftIcon={<Plus />}
            onClick={handleAddUnit}
            disabled={!canEditData || createUnit.isPending}
          >
            {t('grid.addUnit')}
          </Button>
        </FeatureGate>
      </div>
      <UnitGrid
        propertyIdentifier={propertyIdentifier}
        rows={rows}
        onRowClick={handleRowClick}
        loading={isLoading}
      />
    </div>
  );
};
