import { useTranslation } from 'react-i18next';
import { FileText } from 'lucide-react';
import type { TerminationGivenBy } from '@/hooks/useContractTerminationHooks';

interface LetterPreviewStepProps {
  givenBy: TerminationGivenBy | undefined;
  noticeDate: string;
  groundCode: string;
  computedEndDate: string;
  effectiveEndDate: string;
  overrideReason: string;
  inspectionDate: string;
  onInspectionDateChange: (inspectionDate: string) => void;
  onBack: () => void;
  onConfirm: () => void;
  isSubmitting: boolean;
}

export const LetterPreviewStep = ({
  givenBy,
  noticeDate,
  groundCode,
  computedEndDate,
  effectiveEndDate,
  overrideReason,
  inspectionDate,
  onInspectionDateChange,
  onBack,
  onConfirm,
  isSubmitting,
}: LetterPreviewStepProps) => {
  const { t } = useTranslation('contracts');
  const notProvided = t('termination.letterPreview.notProvided');
  const finalEffectiveEndDate = effectiveEndDate || computedEndDate;

  const rows: { label: string; value: string }[] = [
    {
      label: t('termination.letterPreview.givenByLabel'),
      value: givenBy ? t(`termination.givenByValues.${givenBy}`) : notProvided,
    },
    {
      label: t('termination.letterPreview.noticeDateLabel'),
      value: noticeDate || notProvided,
    },
    {
      label: t('termination.letterPreview.groundCodeLabel'),
      value: groundCode || notProvided,
    },
    {
      label: t('termination.letterPreview.computedEndDateLabel'),
      value: computedEndDate || notProvided,
    },
    {
      label: t('termination.letterPreview.effectiveEndDateLabel'),
      value: finalEffectiveEndDate || notProvided,
    },
    {
      label: t('termination.letterPreview.overrideReasonLabel'),
      value: overrideReason || notProvided,
    },
  ];

  return (
    <div className="space-y-6">
      <div className="bg-surface-card rounded-lg border border-border-default p-6">
        <div className="flex items-center gap-3 mb-4">
          <FileText className="h-6 w-6 text-primary-500" />
          <div>
            <h3 className="text-lg font-semibold text-text-primary">
              {t('termination.letterPreview.title')}
            </h3>
            <p className="text-sm text-text-secondary">
              {t('termination.letterPreview.description')}
            </p>
          </div>
        </div>

        <dl className="grid gap-3 sm:grid-cols-2">
          {rows.map((row) => (
            <div key={row.label}>
              <dt className="text-sm text-text-secondary">{row.label}</dt>
              <dd className="font-medium text-text-primary">{row.value}</dd>
            </div>
          ))}
        </dl>

        <div className="mt-4 border-t border-border-default pt-4">
          <label
            htmlFor="inspection-date"
            className="block text-sm font-medium text-text-secondary mb-1"
          >
            {t('termination.letterPreview.inspectionDateLabel')}
          </label>
          <input
            id="inspection-date"
            type="date"
            value={inspectionDate}
            onChange={(e) => onInspectionDateChange(e.target.value)}
            className="w-full max-w-xs px-3 py-2 border border-border-strong rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500 bg-surface-card text-text-primary"
          />
        </div>
      </div>

      <div className="flex justify-between">
        <button
          onClick={onBack}
          disabled={isSubmitting}
          className="px-6 py-2 rounded border border-border-default text-text-secondary hover:bg-surface-inset transition-colors disabled:opacity-50"
        >
          {t('common:buttons.back')}
        </button>
        <button
          onClick={onConfirm}
          disabled={isSubmitting}
          className="bg-primary-500 text-white px-6 py-2 rounded hover:bg-primary-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {isSubmitting
            ? t('termination.letterPreview.submitting')
            : t('termination.letterPreview.confirm')}
        </button>
      </div>
    </div>
  );
};
