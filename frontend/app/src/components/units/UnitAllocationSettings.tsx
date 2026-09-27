import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { AllocationBasis } from '@/types/allocation';
import type { UpdateAllocationRequest } from '@/types/allocation';
import { formatMoney } from '@/utils/formatMoney';

/** The subset of a unit's fields this component needs, independent of `UnitResponse`'s full shape. */
export interface UnitAllocationUnit {
  identifier: string;
  unitNumber: string;
  name?: string | null;
  /** Floor area, when set. AREA basis cannot honestly compute a share without it. */
  areaValue?: number | null;
  /** Current CUSTOM share (0-100), when set. */
  sharePct?: number | null;
}

interface UnitAllocationSettingsProps {
  propertyIdentifier: string;
  basis: AllocationBasis;
  units: UnitAllocationUnit[];
  onSave: (request: UpdateAllocationRequest) => void;
  /** Sample amount the preview splits, in the property's major currency unit. Defaults to 1000. */
  sampleAmount?: number;
  currency?: string;
  isSaving?: boolean;
  disabled?: boolean;
}

const labelCls = 'block text-sm font-medium text-text-secondary mb-1';
const inputCls =
  'w-full border border-border-strong rounded px-3 py-2 focus:border-primary-500 focus:ring-1 focus:ring-primary-500 disabled:opacity-60 disabled:cursor-not-allowed';

/**
 * Splits `totalMinor` (an integer count of minor units) across `weights` using the same
 * largest-remainder method the backend uses, so the preview lands on the same numbers the
 * backend would compute for an honest-looking preview. It is still only a preview — the
 * stored allocation rows returned by `GET /expenses/{id}/allocations` are the source of truth.
 * A weight of 0 (e.g. AREA basis with no floor area) always gets 0 and never participates in
 * remainder distribution.
 */
function largestRemainderSplit(
  totalMinor: number,
  weights: number[]
): number[] {
  const totalWeight = weights.reduce((sum, w) => sum + w, 0);
  if (totalWeight <= 0) {
    return weights.map(() => 0);
  }
  const raw = weights.map((w) => (w / totalWeight) * totalMinor);
  const floors = raw.map(Math.floor);
  const distributed = floors.reduce((sum, f) => sum + f, 0);
  let remainder = totalMinor - distributed;
  const byFraction = raw
    .map((r, index) => ({
      index,
      fraction: r - Math.floor(r),
      weight: weights[index],
    }))
    .filter((entry) => entry.weight > 0)
    .sort((a, b) => b.fraction - a.fraction);

  const result = [...floors];
  for (let i = 0; i < byFraction.length && remainder > 0; i++) {
    result[byFraction[i].index] += 1;
    remainder--;
  }
  return result;
}

export const UnitAllocationSettings = ({
  basis,
  units,
  onSave,
  sampleAmount = 1000,
  currency = 'EUR',
  isSaving = false,
  disabled = false,
}: UnitAllocationSettingsProps) => {
  const { t } = useTranslation('units');
  const [selectedBasis, setSelectedBasis] = useState<AllocationBasis>(basis);
  const [shares, setShares] = useState<Record<string, string>>(() =>
    Object.fromEntries(
      units.map((u) => [u.identifier, String(u.sharePct ?? 0)])
    )
  );

  const customTotal = units.reduce(
    (sum, u) => sum + (parseFloat(shares[u.identifier]) || 0),
    0
  );
  const customTotalRounded = Math.round(customTotal * 100) / 100;
  const customTotalValid = Math.abs(customTotalRounded - 100) < 0.005;

  const unitsMissingArea = units.filter((u) => u.areaValue == null);
  const areaBasisValid =
    selectedBasis !== AllocationBasis.AREA || unitsMissingArea.length === 0;

  const canSave =
    !disabled &&
    !isSaving &&
    (selectedBasis !== AllocationBasis.CUSTOM || customTotalValid) &&
    areaBasisValid;

  const weights = units.map((u) => {
    if (selectedBasis === AllocationBasis.AREA) {
      return u.areaValue ?? 0;
    }
    if (selectedBasis === AllocationBasis.CUSTOM) {
      return parseFloat(shares[u.identifier]) || 0;
    }
    return 1;
  });
  const previewMinor = largestRemainderSplit(
    Math.round(sampleAmount * 100),
    weights
  );

  const handleSave = () => {
    if (!canSave) {
      return;
    }
    const request: UpdateAllocationRequest = {
      basis: selectedBasis,
      shares:
        selectedBasis === AllocationBasis.CUSTOM
          ? units.map((u) => ({
              unitIdentifier: u.identifier,
              sharePct: parseFloat(shares[u.identifier]) || 0,
            }))
          : undefined,
    };
    onSave(request);
  };

  const unitLabel = (u: UnitAllocationUnit) =>
    u.name || t('detail.title', { number: u.unitNumber });

  return (
    <div className="space-y-4">
      <h3 className="text-lg font-semibold text-text-primary">
        {t('allocation.title')}
      </h3>

      <div>
        <label htmlFor="allocation-basis" className={labelCls}>
          {t('allocation.basis')}
        </label>
        <select
          id="allocation-basis"
          className={inputCls}
          value={selectedBasis}
          disabled={disabled || isSaving}
          onChange={(e) => setSelectedBasis(e.target.value as AllocationBasis)}
        >
          <option value={AllocationBasis.EQUAL}>
            {t('allocation.basisOption.EQUAL')}
          </option>
          <option value={AllocationBasis.AREA}>
            {t('allocation.basisOption.AREA')}
          </option>
          <option value={AllocationBasis.CUSTOM}>
            {t('allocation.basisOption.CUSTOM')}
          </option>
        </select>
      </div>

      {selectedBasis === AllocationBasis.CUSTOM && (
        <div className="space-y-2">
          {units.map((u) => (
            <div key={u.identifier} className="flex items-center gap-3">
              <span className="flex-1 text-sm text-text-primary">
                {unitLabel(u)}
              </span>
              <div className="w-28">
                <label
                  htmlFor={`allocation-share-${u.identifier}`}
                  className="sr-only"
                >
                  {t('allocation.share')}
                </label>
                <input
                  id={`allocation-share-${u.identifier}`}
                  type="number"
                  className={inputCls}
                  value={shares[u.identifier] ?? '0'}
                  disabled={disabled || isSaving}
                  onChange={(e) =>
                    setShares((prev) => ({
                      ...prev,
                      [u.identifier]: e.target.value,
                    }))
                  }
                />
              </div>
              <span className="text-sm text-text-secondary">%</span>
            </div>
          ))}
          {!customTotalValid && (
            <p className="text-sm text-error-text">
              {t('allocation.customTotalError', {
                total: customTotalRounded,
              })}
            </p>
          )}
        </div>
      )}

      {selectedBasis === AllocationBasis.AREA &&
        unitsMissingArea.length > 0 && (
          <p className="text-sm text-error-text">
            {unitsMissingArea
              .map((u) => `${unitLabel(u)}: ${t('allocation.noArea')}`)
              .join(', ')}
          </p>
        )}

      <div className="rounded border border-border-default p-3 space-y-1 bg-surface-inset">
        <p className="text-sm font-medium text-text-secondary">
          {t('allocation.preview', {
            amount: formatMoney(sampleAmount, currency),
          })}
        </p>
        <ul className="text-sm text-text-primary space-y-0.5">
          {units.map((u, index) => (
            <li key={u.identifier} className="flex justify-between">
              <span>{unitLabel(u)}</span>
              <span>
                {selectedBasis === AllocationBasis.AREA && u.areaValue == null
                  ? t('allocation.noArea')
                  : formatMoney(previewMinor[index] / 100, currency)}
              </span>
            </li>
          ))}
        </ul>
      </div>

      <button
        type="button"
        onClick={handleSave}
        disabled={!canSave}
        className="px-4 py-2 text-white bg-primary-500 rounded-md hover:bg-primary-600 disabled:opacity-50 disabled:cursor-not-allowed"
      >
        {t('common:buttons.save')}
      </button>
    </div>
  );
};
