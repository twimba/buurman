import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button, RichTextEditor } from '@buurman/ui';
import { useEndOccupancyPeriod } from '@/hooks/useOccupancyPeriodHooks';
import { OccupancyEndReason } from '@/types/occupancyPeriod';
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
  const { t } = useTranslation('properties');
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
      <div className="bg-surface-card rounded-lg p-6 max-w-md w-full mx-4">
        <div className="flex items-center justify-between mb-6">
          <h3 className="text-lg font-semibold text-text-primary">
            {t('selfOccupancy.endTitle')}
          </h3>
          <button
            onClick={onClose}
            className="text-text-secondary hover:text-text-primary"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">
              {t('selfOccupancy.form.endDateRequired')}
            </label>
            <input
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              required
              className="w-full px-3 py-2 rounded-lg border border-border-default bg-surface-card text-text-primary focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">
              {t('selfOccupancy.form.reason')}
            </label>
            <select
              value={endReason}
              onChange={(e) =>
                setEndReason(e.target.value as OccupancyEndReason | '')
              }
              className="w-full px-3 py-2 rounded-lg border border-border-default bg-surface-card text-text-primary focus:outline-none focus:ring-2 focus:ring-primary-500"
            >
              <option value="">
                {t('selfOccupancy.form.reasonPlaceholder')}
              </option>
              {Object.values(OccupancyEndReason).map((r) => (
                <option key={r} value={r}>
                  {t(`selfOccupancy.endReasons.${r}`)}
                </option>
              ))}
            </select>
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
              disabled={endMutation.isPending}
            >
              {t('buttons.cancel', { ns: 'common' })}
            </Button>
            <Button
              variant="primary"
              type="submit"
              isLoading={endMutation.isPending}
            >
              {t('selfOccupancy.endOccupancy')}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};
