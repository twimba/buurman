import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button, RichTextEditor } from '@buurman/ui';
import {
  useCreateOccupancyPeriod,
  useOccupancyPeriods,
} from '@/hooks/useOccupancyPeriodHooks';
import { useContracts } from '@/hooks/useContractHooks';
import { useUnits } from '@/hooks/useUnitHooks';
import { OccupancyType } from '@/types/occupancyPeriod';
import { ContractStatus } from '@/types/contract';
import type { UnitIdentifier } from '@/types/unit';
import { useFormatDate } from '@/hooks/useFormatDate';
import { X, Info } from 'lucide-react';

interface SelfOccupancyModalProps {
  propertyIdentifier: string;
  onClose: () => void;
}

export const SelfOccupancyModal = ({
  propertyIdentifier,
  onClose,
}: SelfOccupancyModalProps) => {
  const { t } = useTranslation('properties');
  const createMutation = useCreateOccupancyPeriod(propertyIdentifier);
  const { formatDate } = useFormatDate();
  const { data: existingPeriods = [] } =
    useOccupancyPeriods(propertyIdentifier);
  const { data: contractsData } = useContracts({ propertyIdentifier });
  const { data: units = [] } = useUnits(propertyIdentifier);
  // A single-unit property never asks — the backend only requires a choice when there is
  // genuinely more than one unit to choose from.
  const requiresUnitChoice = units.length > 1;

  const [startDate, setStartDate] = useState(
    new Date().toISOString().split('T')[0]
  );
  const [endDate, setEndDate] = useState('');
  const [type, setType] = useState<OccupancyType>(OccupancyType.PERSONAL);
  const [occupantName, setOccupantName] = useState('');
  const [monthlyImputedRent, setMonthlyImputedRent] = useState('');
  const [notes, setNotes] = useState('');
  const [unitIdentifier, setUnitIdentifier] = useState('');
  const [unitError, setUnitError] = useState(false);

  // Existing periods/contracts are read at the property level (the backend has no per-unit
  // filter), so once a unit is chosen, only warn about overlaps on that same unit — otherwise a
  // 6-unit building would falsely claim every unit is already taken.
  const relevantPeriods = requiresUnitChoice
    ? existingPeriods.filter((p) => p.unitIdentifier === unitIdentifier)
    : existingPeriods;
  const takenContracts = (contractsData?.content ?? [])
    .filter(
      (c) =>
        c.status !== ContractStatus.DRAFT &&
        c.status !== ContractStatus.PENDING_SIGNATURE
    )
    .filter((c) => !requiresUnitChoice || c.unitIdentifier === unitIdentifier);
  const hasTakenPeriods =
    (!requiresUnitChoice || !!unitIdentifier) &&
    (relevantPeriods.length > 0 || takenContracts.length > 0);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (requiresUnitChoice && !unitIdentifier) {
      setUnitError(true);
      return;
    }
    createMutation.mutate(
      {
        startDate,
        type,
        ...(endDate ? { endDate } : {}),
        ...(occupantName ? { occupantName } : {}),
        ...(monthlyImputedRent
          ? { monthlyImputedRent: parseFloat(monthlyImputedRent) }
          : {}),
        ...(notes ? { notes } : {}),
        ...(unitIdentifier
          ? { unitIdentifier: unitIdentifier as UnitIdentifier }
          : {}),
      },
      { onSuccess: onClose }
    );
  };

  return (
    <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50">
      <div className="bg-surface-card rounded-lg p-6 max-w-lg w-full mx-4">
        <div className="flex items-center justify-between mb-6">
          <h3 className="text-lg font-semibold text-text-primary">
            {t('selfOccupancy.markTitle')}
          </h3>
          <button
            onClick={onClose}
            className="text-text-secondary hover:text-text-primary"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-text-secondary mb-1">
                {t('selfOccupancy.form.startDate')}
              </label>
              <input
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                required
                className="w-full px-3 py-2 rounded-lg border border-border-default bg-surface-card text-text-primary focus:outline-none focus:ring-2 focus:ring-primary-500"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-text-secondary mb-1">
                {t('selfOccupancy.form.endDate')}
              </label>
              <input
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                min={startDate}
                className="w-full px-3 py-2 rounded-lg border border-border-default bg-surface-card text-text-primary focus:outline-none focus:ring-2 focus:ring-primary-500"
              />
            </div>
          </div>

          {requiresUnitChoice && (
            <div>
              <label
                htmlFor="self-occupancy-unit"
                className="block text-sm font-medium text-text-secondary mb-1"
              >
                {t('picker.label', { ns: 'units' })}
              </label>
              <select
                id="self-occupancy-unit"
                value={unitIdentifier}
                onChange={(e) => {
                  setUnitIdentifier(e.target.value);
                  setUnitError(false);
                }}
                className="w-full px-3 py-2 rounded-lg border border-border-default bg-surface-card text-text-primary focus:outline-none focus:ring-2 focus:ring-primary-500"
              >
                <option value="">
                  {t('picker.placeholder', { ns: 'units' })}
                </option>
                {units.map((u) => (
                  <option key={u.identifier} value={u.identifier}>
                    {u.name || u.unitNumber}
                  </option>
                ))}
              </select>
              {unitError && (
                <p className="mt-1 text-sm text-error-text">
                  {t('selfOccupancy.form.unitRequired')}
                </p>
              )}
            </div>
          )}

          {hasTakenPeriods && (
            <div className="bg-warning-bg border border-warning-border rounded-lg p-3">
              <div className="flex items-center gap-1.5 text-xs font-semibold text-warning-text mb-2">
                <Info className="h-3.5 w-3.5 flex-shrink-0" />
                {t('selfOccupancy.form.alreadyTakenPeriods')}
              </div>
              <div className="space-y-1">
                {relevantPeriods.map((p) => (
                  <div
                    key={p.identifier}
                    className="flex items-center gap-1.5 text-xs text-warning-text"
                  >
                    <span className="w-1.5 h-1.5 rounded-full bg-indigo-400 flex-shrink-0" />
                    <span>
                      {t('contracts.selfOccupancy.takenPeriods.selfUse', {
                        period: `${formatDate(p.startDate)} — ${p.endDate ? formatDate(p.endDate) : t('contracts.selfOccupancy.takenPeriods.ongoing')}`,
                      })}
                      {p.occupantName && ` (${p.occupantName})`}
                    </span>
                  </div>
                ))}
                {takenContracts.map((c) => (
                  <div
                    key={c.identifier}
                    className="flex items-center gap-1.5 text-xs text-warning-text"
                  >
                    <span className="w-1.5 h-1.5 rounded-full bg-blue-400 flex-shrink-0" />
                    <span>
                      {t('contracts.selfOccupancy.takenPeriods.contract', {
                        identifier: c.identifier,
                        period: `${formatDate(c.startDate)} — ${c.endDate ? formatDate(c.endDate) : t('contracts.selfOccupancy.takenPeriods.ongoing')}`,
                      })}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}

          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">
              {t('selfOccupancy.form.occupancyType')}
            </label>
            <select
              value={type}
              onChange={(e) => setType(e.target.value as OccupancyType)}
              required
              className="w-full px-3 py-2 rounded-lg border border-border-default bg-surface-card text-text-primary focus:outline-none focus:ring-2 focus:ring-primary-500"
            >
              {Object.values(OccupancyType).map((ot) => (
                <option key={ot} value={ot}>
                  {t(`selfOccupancy.occupancyTypes.${ot}`)}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">
              {t('selfOccupancy.form.occupantName')}
            </label>
            <input
              type="text"
              value={occupantName}
              onChange={(e) => setOccupantName(e.target.value)}
              placeholder={t('selfOccupancy.form.occupantNamePlaceholder')}
              maxLength={255}
              className="w-full px-3 py-2 rounded-lg border border-border-default bg-surface-card text-text-primary focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">
              {t('selfOccupancy.form.monthlyImputedRent')}
            </label>
            <input
              type="number"
              value={monthlyImputedRent}
              onChange={(e) => setMonthlyImputedRent(e.target.value)}
              placeholder="0.00"
              min="0"
              step="0.01"
              className="w-full px-3 py-2 rounded-lg border border-border-default bg-surface-card text-text-primary focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">
              {t('selfOccupancy.form.notes')}
            </label>
            <RichTextEditor
              value={notes}
              onChange={setNotes}
              placeholder={t('selfOccupancy.form.notesPlaceholder')}
            />
          </div>

          <div className="flex gap-3 justify-end pt-2">
            <Button
              variant="secondary"
              onClick={onClose}
              disabled={createMutation.isPending}
            >
              {t('buttons.cancel', { ns: 'common' })}
            </Button>
            <Button
              variant="primary"
              type="submit"
              isLoading={createMutation.isPending}
            >
              {t('buttons.confirm', { ns: 'common' })}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};
