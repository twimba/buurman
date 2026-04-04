import { useState } from 'react';
import { Button } from '@buurman/ui';
import { RichTextEditor } from '@buurman/ui';
import {
  useCreateOccupancyPeriod,
  useOccupancyPeriods,
} from '@/hooks/useOccupancyPeriodHooks';
import { useContracts } from '@/hooks/useContractHooks';
import { OccupancyType, OCCUPANCY_TYPE_LABELS } from '@/types/occupancyPeriod';
import { ContractStatus } from '@/types/contract';
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
  const createMutation = useCreateOccupancyPeriod(propertyIdentifier);
  const { formatDate } = useFormatDate();
  const { data: existingPeriods = [] } =
    useOccupancyPeriods(propertyIdentifier);
  const { data: contractsData } = useContracts({ propertyIdentifier });
  const takenContracts = (contractsData?.content ?? []).filter(
    (c) =>
      c.status !== ContractStatus.DRAFT &&
      c.status !== ContractStatus.PENDING_SIGNATURE
  );
  const hasTakenPeriods =
    existingPeriods.length > 0 || takenContracts.length > 0;

  const [startDate, setStartDate] = useState(
    new Date().toISOString().split('T')[0]
  );
  const [endDate, setEndDate] = useState('');
  const [type, setType] = useState<OccupancyType>(OccupancyType.PERSONAL);
  const [occupantName, setOccupantName] = useState('');
  const [monthlyImputedRent, setMonthlyImputedRent] = useState('');
  const [notes, setNotes] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
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
      },
      { onSuccess: onClose }
    );
  };

  return (
    <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50">
      <div className="bg-surface-card rounded-lg p-6 max-w-lg w-full mx-4">
        <div className="flex items-center justify-between mb-6">
          <h3 className="text-lg font-semibold text-text-primary">
            Mark as Self-Occupied
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
                Start Date *
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
                End Date
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

          {hasTakenPeriods && (
            <div className="bg-warning-bg border border-warning-border rounded-lg p-3">
              <div className="flex items-center gap-1.5 text-xs font-semibold text-warning-text mb-2">
                <Info className="h-3.5 w-3.5 flex-shrink-0" />
                Already taken periods
              </div>
              <div className="space-y-1">
                {existingPeriods.map((p) => (
                  <div
                    key={p.identifier}
                    className="flex items-center gap-1.5 text-xs text-warning-text"
                  >
                    <span className="w-1.5 h-1.5 rounded-full bg-indigo-400 flex-shrink-0" />
                    <span>
                      Self-use: {formatDate(p.startDate)} —{' '}
                      {p.endDate ? formatDate(p.endDate) : 'Ongoing'}
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
                      Contract {c.identifier}: {formatDate(c.startDate)} —{' '}
                      {c.endDate ? formatDate(c.endDate) : 'Ongoing'}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}

          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">
              Occupancy Type *
            </label>
            <select
              value={type}
              onChange={(e) => setType(e.target.value as OccupancyType)}
              required
              className="w-full px-3 py-2 rounded-lg border border-border-default bg-surface-card text-text-primary focus:outline-none focus:ring-2 focus:ring-primary-500"
            >
              {Object.values(OccupancyType).map((t) => (
                <option key={t} value={t}>
                  {OCCUPANCY_TYPE_LABELS[t]}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">
              Occupant Name
            </label>
            <input
              type="text"
              value={occupantName}
              onChange={(e) => setOccupantName(e.target.value)}
              placeholder="e.g. Owner, Family member"
              maxLength={255}
              className="w-full px-3 py-2 rounded-lg border border-border-default bg-surface-card text-text-primary focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">
              Monthly Imputed Rent
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
              Notes
            </label>
            <RichTextEditor
              value={notes}
              onChange={setNotes}
              placeholder="Additional notes..."
            />
          </div>

          <div className="flex gap-3 justify-end pt-2">
            <Button
              variant="secondary"
              onClick={onClose}
              disabled={createMutation.isPending}
            >
              Cancel
            </Button>
            <Button
              variant="primary"
              type="submit"
              isLoading={createMutation.isPending}
            >
              Confirm
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};
