/**
 * A property can now hold several independently-let units (BUUR-106), so there is no single
 * well-defined "status" at the property level anymore. This derives a coarse, honest display
 * from unit counts instead: fully vacant, fully occupied, fully unavailable (a status other than
 * OCCUPIED/VACANT -- MAINTENANCE, UNDER_RENOVATION, SELF_OCCUPIED, FALLOW, LISTED), or a mixed
 * three-way breakdown.
 *
 * `occupiedUnitCount + vacantUnitCount` does NOT necessarily equal `unitCount` -- a unit under
 * MAINTENANCE/UNDER_RENOVATION/etc. is neither -- so a naive "{{occupied}}/{{total}} occupied"
 * fraction reads as "the rest are available", which is false (PREAMBLE amendment A6). Every
 * branch below names all three buckets it knows about rather than implying the remainder.
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
  occupiedUnitCount: number,
  vacantUnitCount: number
): UnitOccupancySummary {
  if (unitCount <= 0) {
    return {
      label: t('unitOccupancy.noUnits', { defaultValue: 'No units' }),
      colorClass: 'bg-surface-inset text-text-primary',
      dotColorClass: 'bg-text-disabled',
    };
  }

  // The overwhelming majority of properties hold exactly one unit -- these three branches also
  // cover that case, reading as a single natural status ("Vacant"/"Occupied"/"Unavailable"),
  // never a fraction.
  if (occupiedUnitCount >= unitCount) {
    return {
      label: t('unitOccupancy.allOccupied', { defaultValue: 'Occupied' }),
      colorClass: 'bg-info-bg text-info-text',
      dotColorClass: 'bg-info-text',
    };
  }
  if (vacantUnitCount >= unitCount) {
    return {
      label: t('unitOccupancy.allVacant', { defaultValue: 'Vacant' }),
      colorClass: 'bg-success-bg text-success-text',
      dotColorClass: 'bg-success-text',
    };
  }
  const unavailableUnitCount = Math.max(
    0,
    unitCount - occupiedUnitCount - vacantUnitCount
  );
  if (unavailableUnitCount >= unitCount) {
    return {
      label: t('unitOccupancy.allUnavailable', { defaultValue: 'Unavailable' }),
      colorClass: 'bg-warning-bg text-warning-text',
      dotColorClass: 'bg-warning-text',
    };
  }

  // A genuine mix across more than one unit: name all three counts rather than a fraction.
  return {
    label: t('unitOccupancy.mixed', {
      occupied: occupiedUnitCount,
      vacant: vacantUnitCount,
      unavailable: unavailableUnitCount,
      defaultValue: `${occupiedUnitCount} let · ${vacantUnitCount} vacant · ${unavailableUnitCount} unavailable`,
    }),
    colorClass: 'bg-warning-bg text-warning-text',
    dotColorClass: 'bg-warning-text',
  };
}
