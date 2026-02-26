import { useState } from 'react';
import { Button } from '@/components/ui';
import { RichTextEditor } from '@/components/common/RichTextEditor';
import { useEndOccupancyPeriod } from '@/hooks/useOccupancyPeriodHooks';
import {
  OccupancyEndReason,
  OCCUPANCY_END_REASON_LABELS,
} from '@/types/occupancyPeriod';
import { X } from 'lucide-react';

interface EndSelfOccupancyModalProps {
  propertyIdentifier: string;
  periodIdentifier: string;
  onClose: () => void;
}

export const EndSelfOccupancyModal = ({
  propertyIdentifier,
  periodIdentifier,
  onClose,
}: EndSelfOccupancyModalProps) => {
  const endMutation = useEndOccupancyPeriod(propertyIdentifier);
  const [endDate, setEndDate] = useState(
    new Date().toISOString().split('T')[0]
  );
  const [endReason, setEndReason] = useState<OccupancyEndReason | ''>('');
  const [notes, setNotes] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    endMutation.mutate(
      {
        periodIdentifier,
        data: {
          endDate,
          ...(endReason ? { endReason: endReason as OccupancyEndReason } : {}),
          ...(notes ? { notes } : {}),
        },
      },
      { onSuccess: onClose }
    );
  };

  return (
    <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50">
      <div className="bg-white dark:bg-[#14161f] rounded-xl p-6 max-w-md w-full mx-4">
        <div className="flex items-center justify-between mb-6">
          <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
            End Self-Occupancy
          </h3>
          <button
            onClick={onClose}
            className="text-[#6b7194] hover:text-[#1a1d2e] dark:hover:text-[#eef0f6]"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              End Date *
            </label>
            <input
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              required
              className="w-full px-3 py-2 rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#0c0d14] text-[#1a1d2e] dark:text-[#eef0f6] focus:outline-none focus:ring-2 focus:ring-[#5c7cfa]"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Reason
            </label>
            <select
              value={endReason}
              onChange={(e) =>
                setEndReason(e.target.value as OccupancyEndReason | '')
              }
              className="w-full px-3 py-2 rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#0c0d14] text-[#1a1d2e] dark:text-[#eef0f6] focus:outline-none focus:ring-2 focus:ring-[#5c7cfa]"
            >
              <option value="">Select a reason...</option>
              {Object.values(OccupancyEndReason).map((r) => (
                <option key={r} value={r}>
                  {OCCUPANCY_END_REASON_LABELS[r]}
                </option>
              ))}
            </select>
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
              disabled={endMutation.isPending}
            >
              Cancel
            </Button>
            <Button
              variant="primary"
              type="submit"
              isLoading={endMutation.isPending}
            >
              End Occupancy
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};
