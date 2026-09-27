import { useCallback, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft } from 'lucide-react';
import { Button, Skeleton } from '@buurman/ui';
import { UnitCharacteristicsForm } from '@/components/units/UnitCharacteristicsForm';
import {
  useUnit,
  useUpdateUnit,
  useUnitResidentialDetails,
  useUpdateUnitResidentialDetails,
  useUnitAmenities,
  useUpdateUnitAmenities,
} from '@/hooks/useUnitHooks';
import { useAmenities, useProperty } from '@/hooks/usePropertyHooks';
import { unitToUpdateRequest } from '@/utils/unitRequests';
import { UnitType } from '@/types/unit';
import type {
  PropertyIdentifier,
  UnitIdentifier,
  UpdateUnitRequest,
  UpdateUnitResidentialDetailsRequest,
} from '@/types/unit';
import { useTeam } from '@/context/TeamContext';
import { ErrorMessage } from '@/components/ErrorMessage';

function humanize(value: string): string {
  return value
    .replace(/_/g, ' ')
    .toLowerCase()
    .replace(/\b\w/g, (c) => c.toUpperCase());
}

/**
 * Bedrooms/bathrooms/furnished/pet policy -- a separate PUT endpoint from the main unit
 * record, only meaningful for an APARTMENT. Follows the same derived-overrides pattern as
 * `PropertyDetailPage`'s single-unit card: no `useEffect` syncing query data into local
 * state, the draft is always `saved + overrides`.
 */
const ResidentialDetailsSection = ({
  unitIdentifier,
  disabled,
}: {
  unitIdentifier: UnitIdentifier;
  disabled: boolean;
}) => {
  const { t } = useTranslation(['units', 'common']);
  const { data: details } = useUnitResidentialDetails(unitIdentifier);
  const updateDetails = useUpdateUnitResidentialDetails(unitIdentifier);
  const [overrides, setOverrides] = useState<
    Partial<UpdateUnitResidentialDetailsRequest>
  >({});

  if (!details) {
    return null;
  }

  const saved: UpdateUnitResidentialDetailsRequest = {
    bedrooms: details.bedrooms,
    bathrooms: details.bathrooms,
    furnished: details.furnished,
    petPolicy: details.petPolicy,
  };
  const draft: UpdateUnitResidentialDetailsRequest = {
    ...saved,
    ...overrides,
  };
  const isDirty = Object.keys(overrides).length > 0;

  return (
    <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
      <h3 className="text-sm font-semibold text-text-secondary uppercase tracking-wide mb-4">
        {t('units:detail.residential')}
      </h3>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label
            htmlFor="unit-bedrooms"
            className="block text-sm font-medium text-text-secondary mb-1"
          >
            {t('units:fields.bedrooms')}
          </label>
          <input
            id="unit-bedrooms"
            type="number"
            min={0}
            value={draft.bedrooms ?? ''}
            disabled={disabled}
            onChange={(e) =>
              setOverrides((prev) => ({
                ...prev,
                bedrooms:
                  e.target.value === '' ? undefined : Number(e.target.value),
              }))
            }
            className="w-full border border-border-strong rounded px-3 py-2 focus:border-primary-500 focus:ring-1 focus:ring-primary-500 disabled:opacity-60 disabled:cursor-not-allowed"
          />
        </div>
        <div>
          <label
            htmlFor="unit-bathrooms"
            className="block text-sm font-medium text-text-secondary mb-1"
          >
            {t('units:fields.bathrooms')}
          </label>
          <input
            id="unit-bathrooms"
            type="number"
            min={0}
            value={draft.bathrooms ?? ''}
            disabled={disabled}
            onChange={(e) =>
              setOverrides((prev) => ({
                ...prev,
                bathrooms:
                  e.target.value === '' ? undefined : Number(e.target.value),
              }))
            }
            className="w-full border border-border-strong rounded px-3 py-2 focus:border-primary-500 focus:ring-1 focus:ring-primary-500 disabled:opacity-60 disabled:cursor-not-allowed"
          />
        </div>
        <div>
          <label className="flex items-center gap-2 text-sm text-text-secondary">
            <input
              type="checkbox"
              checked={draft.furnished}
              disabled={disabled}
              onChange={(e) =>
                setOverrides((prev) => ({
                  ...prev,
                  furnished: e.target.checked,
                }))
              }
            />
            {t('units:fields.furnished')}
          </label>
        </div>
        <div>
          <label
            htmlFor="unit-pet-policy"
            className="block text-sm font-medium text-text-secondary mb-1"
          >
            {t('units:fields.petPolicy')}
          </label>
          <input
            id="unit-pet-policy"
            type="text"
            value={draft.petPolicy ?? ''}
            disabled={disabled}
            onChange={(e) =>
              setOverrides((prev) => ({
                ...prev,
                petPolicy: e.target.value || undefined,
              }))
            }
            className="w-full border border-border-strong rounded px-3 py-2 focus:border-primary-500 focus:ring-1 focus:ring-primary-500 disabled:opacity-60 disabled:cursor-not-allowed"
          />
        </div>
      </div>
      {!disabled && isDirty && (
        <div className="flex justify-end gap-2 mt-4">
          <Button
            variant="ghost"
            onClick={() => setOverrides({})}
            disabled={updateDetails.isPending}
          >
            {t('common:buttons.discardChanges')}
          </Button>
          <Button
            variant="primary"
            onClick={() =>
              updateDetails.mutate(draft, {
                onSuccess: () => setOverrides({}),
              })
            }
            isLoading={updateDetails.isPending}
          >
            {t('common:buttons.saveChanges')}
          </Button>
        </div>
      )}
    </div>
  );
};

/**
 * The full amenities catalog (grouped by category) with the unit's current selection
 * checked. Selection is a derived Set, `null` until the landlord touches a checkbox --
 * same derived-overrides shape as the rest of this page, no effect-driven sync.
 */
const AmenitiesSection = ({
  unitIdentifier,
  disabled,
}: {
  unitIdentifier: UnitIdentifier;
  disabled: boolean;
}) => {
  const { t } = useTranslation(['units', 'common']);
  const { data: catalog = {} } = useAmenities();
  const { data: current } = useUnitAmenities(unitIdentifier);
  const updateAmenities = useUpdateUnitAmenities(unitIdentifier);
  const [selected, setSelected] = useState<Set<string> | null>(null);

  if (!current) {
    return null;
  }

  const savedIds = new Set(current.map((amenity) => amenity.identifier));
  const activeSelection = selected ?? savedIds;
  const isDirty = selected !== null;

  const toggle = (identifier: string) => {
    const next = new Set(activeSelection);
    if (next.has(identifier)) {
      next.delete(identifier);
    } else {
      next.add(identifier);
    }
    setSelected(next);
  };

  return (
    <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
      <h3 className="text-sm font-semibold text-text-secondary uppercase tracking-wide mb-4">
        {t('units:detail.amenities')}
      </h3>
      <div className="space-y-4">
        {Object.entries(catalog).map(([category, amenities]) => (
          <div key={category}>
            <div className="text-xs font-medium text-text-secondary uppercase tracking-wide mb-2">
              {humanize(category)}
            </div>
            <div className="flex flex-wrap gap-x-6 gap-y-2">
              {amenities.map((amenity) => (
                <label
                  key={amenity.identifier}
                  className="flex items-center gap-2 text-sm text-text-primary"
                >
                  <input
                    type="checkbox"
                    checked={activeSelection.has(amenity.identifier)}
                    disabled={disabled}
                    onChange={() => toggle(amenity.identifier)}
                  />
                  {amenity.name}
                </label>
              ))}
            </div>
          </div>
        ))}
      </div>
      {!disabled && isDirty && (
        <div className="flex justify-end gap-2 mt-4">
          <Button
            variant="ghost"
            onClick={() => setSelected(null)}
            disabled={updateAmenities.isPending}
          >
            {t('common:buttons.discardChanges')}
          </Button>
          <Button
            variant="primary"
            onClick={() =>
              updateAmenities.mutate(
                { amenityIdentifiers: Array.from(activeSelection) },
                { onSuccess: () => setSelected(null) }
              )
            }
            isLoading={updateAmenities.isPending}
          >
            {t('common:buttons.saveChanges')}
          </Button>
        </div>
      )}
    </div>
  );
};

export const UnitDetailPage = () => {
  const { t } = useTranslation(['units', 'common']);
  const { id = '', unitId = '' } = useParams<{ id: string; unitId: string }>();
  const navigate = useNavigate();
  const { canEditData } = useTeam();

  const propertyIdentifier = id as PropertyIdentifier;
  const unitIdentifier = unitId as UnitIdentifier;

  const { data: property } = useProperty(id);
  const { data: unit, isLoading, error } = useUnit(unitIdentifier);
  const updateUnit = useUpdateUnit(propertyIdentifier, unitIdentifier);
  const [overrides, setOverrides] = useState<Partial<UpdateUnitRequest>>({});

  const handleChange = useCallback(
    <K extends keyof UpdateUnitRequest>(
      field: K,
      value: UpdateUnitRequest[K]
    ) => {
      setOverrides((prev) => ({ ...prev, [field]: value }));
    },
    []
  );

  if (isLoading) {
    return (
      <div className="px-4 py-8 space-y-4">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-48 w-full rounded-lg" />
      </div>
    );
  }

  if (error || !unit) {
    return (
      <div className="px-4 py-8">
        <ErrorMessage message={t('common:errors.notFound')} />
      </div>
    );
  }

  const savedRequest = unitToUpdateRequest(unit);
  const draft: UpdateUnitRequest = { ...savedRequest, ...overrides };
  const isDirty = Object.keys(overrides).length > 0;

  return (
    <div className="min-h-full bg-background">
      <div className="px-4 py-8 space-y-6">
        <button
          type="button"
          onClick={() => navigate(`/properties/${id}`)}
          className="inline-flex items-center gap-1.5 text-sm text-primary-500 hover:text-primary-600 transition-colors"
        >
          <ArrowLeft className="h-4 w-4" />
          {property
            ? t('units:detail.backToProperty', { street: property.street })
            : t('common:buttons.back')}
        </button>

        <h1 className="text-2xl font-bold text-text-primary">
          {unit.name || t('units:detail.title', { number: unit.unitNumber })}
        </h1>

        <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
          <h3 className="text-sm font-semibold text-text-secondary uppercase tracking-wide mb-4">
            {t('units:detail.characteristics')}
          </h3>
          <UnitCharacteristicsForm
            value={draft}
            onChange={handleChange}
            disabled={!canEditData || updateUnit.isPending}
          />
          {canEditData && (
            <div className="flex justify-end gap-2 mt-4">
              {isDirty && (
                <Button
                  variant="ghost"
                  onClick={() => setOverrides({})}
                  disabled={updateUnit.isPending}
                >
                  {t('common:buttons.discardChanges')}
                </Button>
              )}
              <Button
                variant="primary"
                onClick={() =>
                  updateUnit.mutate(draft, {
                    onSuccess: () => setOverrides({}),
                  })
                }
                disabled={!isDirty || updateUnit.isPending}
              >
                {t('common:buttons.saveChanges')}
              </Button>
            </div>
          )}
        </div>

        {unit.unitType === UnitType.APARTMENT && (
          <ResidentialDetailsSection
            unitIdentifier={unitIdentifier}
            disabled={!canEditData}
          />
        )}

        <AmenitiesSection
          unitIdentifier={unitIdentifier}
          disabled={!canEditData}
        />
      </div>
    </div>
  );
};
