/**
 * A property can now hold several independently-let units (BUUR-106), so there is no single
 * well-defined "status" at the property level anymore. This derives a coarse, honest display
 * from unit counts instead: fully vacant, fully occupied, or a partial "X/Y occupied" count.
 */
export interface UnitOccupancySummary {
  label: string;
  /** Pill background + text, for a badge. */
  colorClass: string;
  /** Solid fill, for a small status dot. */
  dotColorClass: string;
}

export function describePropertyOccupancy(
  t: (key: string, options?: Record<string, unknown>) => string,
  unitCount: number,
  occupiedUnitCount: number
): UnitOccupancySummary {
  if (unitCount <= 0) {
    return {
      label: t('unitOccupancy.noUnits', { defaultValue: 'No units' }),
      colorClass: 'bg-surface-inset text-text-primary',
      dotColorClass: 'bg-text-disabled',
    };
  }
  if (occupiedUnitCount <= 0) {
    return {
      label: t('unitOccupancy.allVacant', { defaultValue: 'Vacant' }),
      colorClass: 'bg-success-bg text-success-text',
      dotColorClass: 'bg-success-text',
    };
  }
  if (occupiedUnitCount >= unitCount) {
    return {
      label: t('unitOccupancy.allOccupied', { defaultValue: 'Occupied' }),
      colorClass: 'bg-info-bg text-info-text',
      dotColorClass: 'bg-info-text',
    };
  }
  return {
    label: t('unitOccupancy.partiallyOccupied', {
      occupied: occupiedUnitCount,
      total: unitCount,
      defaultValue: `${occupiedUnitCount}/${unitCount} occupied`,
    }),
    colorClass: 'bg-warning-bg text-warning-text',
    dotColorClass: 'bg-warning-text',
  };
}
