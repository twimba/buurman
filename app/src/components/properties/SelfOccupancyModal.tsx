import { useState } from 'react';
import { Button } from '@/components/ui';
import { RichTextEditor } from '@/components/common/RichTextEditor';
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
      <div className="bg-white dark:bg-[#14161f] rounded-xl p-6 max-w-lg w-full mx-4">
        <div className="flex items-center justify-between mb-6">
          <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
            Mark as Self-Occupied
          </h3>
          <button
            onClick={onClose}
            className="text-[#6b7194] hover:text-[#1a1d2e] dark:hover:text-[#eef0f6]"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
                Start Date *
              </label>
              <input
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                required
                className="w-full px-3 py-2 rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#0c0d14] text-[#1a1d2e] dark:text-[#eef0f6] focus:outline-none focus:ring-2 focus:ring-[#5c7cfa]"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
                End Date
              </label>
              <input
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                min={startDate}
                className="w-full px-3 py-2 rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#0c0d14] text-[#1a1d2e] dark:text-[#eef0f6] focus:outline-none focus:ring-2 focus:ring-[#5c7cfa]"
              />
            </div>
          </div>

          {hasTakenPeriods && (
            <div className="bg-amber-50 dark:bg-amber-900/10 border border-amber-200 dark:border-amber-800/40 rounded-lg p-3">
              <div className="flex items-center gap-1.5 text-xs font-semibold text-amber-700 dark:text-amber-400 mb-2">
                <Info className="h-3.5 w-3.5 flex-shrink-0" />
                Already taken periods
              </div>
              <div className="space-y-1">
                {existingPeriods.map((p) => (
                  <div
                    key={p.identifier}
                    className="flex items-center gap-1.5 text-xs text-amber-700 dark:text-amber-500"
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
                    className="flex items-center gap-1.5 text-xs text-amber-700 dark:text-amber-500"
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
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Occupancy Type *
            </label>
            <select
              value={type}
              onChange={(e) => setType(e.target.value as OccupancyType)}
              required
              className="w-full px-3 py-2 rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#0c0d14] text-[#1a1d2e] dark:text-[#eef0f6] focus:outline-none focus:ring-2 focus:ring-[#5c7cfa]"
            >
              {Object.values(OccupancyType).map((t) => (
                <option key={t} value={t}>
                  {OCCUPANCY_TYPE_LABELS[t]}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Occupant Name
            </label>
            <input
              type="text"
              value={occupantName}
              onChange={(e) => setOccupantName(e.target.value)}
              placeholder="e.g. Owner, Family member"
              maxLength={255}
              className="w-full px-3 py-2 rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#0c0d14] text-[#1a1d2e] dark:text-[#eef0f6] focus:outline-none focus:ring-2 focus:ring-[#5c7cfa]"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Monthly Imputed Rent
            </label>
            <input
              type="number"
              value={monthlyImputedRent}
              onChange={(e) => setMonthlyImputedRent(e.target.value)}
              placeholder="0.00"
              min="0"
              step="0.01"
              className="w-full px-3 py-2 rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#0c0d14] text-[#1a1d2e] dark:text-[#eef0f6] focus:outline-none focus:ring-2 focus:ring-[#5c7cfa]"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
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
