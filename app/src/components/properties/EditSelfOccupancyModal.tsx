import { useState } from 'react';
import { Button } from '@/components/ui';
import { RichTextEditor } from '@/components/common/RichTextEditor';
import { useUpdateOccupancyPeriod } from '@/hooks/useOccupancyPeriodHooks';
import {
  OccupancyType,
  OCCUPANCY_TYPE_LABELS,
  OccupancyPeriodResponse,
} from '@/types/occupancyPeriod';
import { X } from 'lucide-react';

interface EditSelfOccupancyModalProps {
  propertyIdentifier: string;
  period: OccupancyPeriodResponse;
  onClose: () => void;
}

export const EditSelfOccupancyModal = ({
  propertyIdentifier,
  period,
  onClose,
}: EditSelfOccupancyModalProps) => {
  const updateMutation = useUpdateOccupancyPeriod(propertyIdentifier);

  const [startDate, setStartDate] = useState(period.startDate);
  const [endDate, setEndDate] = useState(period.endDate ?? '');
  const [type, setType] = useState<OccupancyType>(period.type);
  const [occupantName, setOccupantName] = useState(period.occupantName ?? '');
  const [monthlyImputedRent, setMonthlyImputedRent] = useState(
    period.monthlyImputedRent != null ? String(period.monthlyImputedRent) : ''
  );
  const [notes, setNotes] = useState(period.notes ?? '');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    updateMutation.mutate(
      {
        periodIdentifier: period.identifier,
        data: {
          startDate,
          type,
          ...(endDate ? { endDate } : {}),
          ...(occupantName ? { occupantName } : {}),
          ...(monthlyImputedRent
            ? { monthlyImputedRent: parseFloat(monthlyImputedRent) }
            : {}),
          ...(notes ? { notes } : {}),
        },
      },
      { onSuccess: onClose }
    );
  };

  return (
    <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50">
      <div className="bg-white dark:bg-[#14161f] rounded-xl p-6 max-w-lg w-full mx-4 max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between mb-6">
          <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
            Edit Self-Occupancy
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
              disabled={updateMutation.isPending}
            >
              Cancel
            </Button>
            <Button
              variant="primary"
              type="submit"
              isLoading={updateMutation.isPending}
            >
              Save Changes
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};
