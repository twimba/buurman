import { useTranslation } from 'react-i18next';
import { DataTable, EmptyState, type ColumnDef } from '@buurman/ui';
import { UnitCell } from './UnitCell';
import { formatMoney } from '@/utils/formatMoney';
import type { UnitGridRow } from '@/types/unit';

interface UnitGridProps {
  propertyIdentifier: string;
  rows: UnitGridRow[];
  onRowClick?: (row: UnitGridRow) => void;
  loading?: boolean;
}

/**
 * Plain table over rows supplied by the caller -- fires no requests of its own, so a
 * 50-unit building still costs exactly one `useUnits` call (owned by `PropertyUnitsTab`).
 * `DataTable` already handles the horizontal scroll on phone, matching the tab bar.
 */
export const UnitGrid = ({ rows, onRowClick, loading }: UnitGridProps) => {
  const { t } = useTranslation('units');

  const columns: ColumnDef<UnitGridRow>[] = [
    {
      id: 'unit',
      header: t('grid.unit'),
      cell: (row) => <UnitCell row={row} />,
    },
    {
      id: 'tenant',
      header: t('grid.tenant'),
      cell: (row) => row.tenantName || t('grid.noTenant'),
      hideOnMobile: true,
    },
    {
      id: 'rent',
      header: t('grid.rent'),
      align: 'right',
      cell: (row) =>
        row.monthlyRent != null && row.monthlyRentCurrency
          ? formatMoney(row.monthlyRent, row.monthlyRentCurrency)
          : '',
    },
    {
      id: 'status',
      header: t('grid.status'),
      cell: (row) => t(`status.${row.status}`),
    },
    {
      id: 'vacancyDays',
      header: t('grid.vacancyDays'),
      align: 'right',
      // Only unlet units carry a vacancyDays figure -- the API leaves it unset once a
      // unit is OCCUPIED/SELF_OCCUPIED/etc, so gating on null here is enough.
      cell: (row) =>
        row.vacancyDays != null
          ? t('grid.vacancyDaysValue', { count: row.vacancyDays })
          : '',
      hideOnMobile: true,
    },
  ];

  return (
    <DataTable
      columns={columns}
      data={rows}
      // Identifiers are unique in real data; appending unitNumber only guards against
      // fixture rows in tests that reuse one identifier across several unitNumbers.
      rowKey={(row) => `${row.identifier}-${row.unitNumber}`}
      onRowClick={onRowClick}
      loading={loading}
      emptyState={<EmptyState title={t('grid.empty')} variant="inline" />}
      aria-label={t('tab')}
    />
  );
};
