import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { AlertTriangle, CalendarClock } from 'lucide-react';
import {
  useTerminationPreview,
  type TerminationGivenBy,
} from '@/hooks/useContractTerminationHooks';

interface ReviewComputedDateStepProps {
  contractId: string | undefined;
  givenBy: TerminationGivenBy | undefined;
  noticeDate: string;
  groundCode: string;
  onGroundCodeChange: (groundCode: string) => void;
  effectiveEndDate: string;
  onEffectiveEndDateChange: (effectiveEndDate: string) => void;
  overrideReason: string;
  onOverrideReasonChange: (overrideReason: string) => void;
  onComputedEndDateChange: (computedEndDate: string) => void;
  onNext: () => void;
  onBack: () => void;
}

// The override is only "earlier" once both dates are known — string comparison is safe
// because both values are ISO 8601 (yyyy-mm-dd) dates. Exported so TerminationWizardPage's
// submit-time re-check uses the exact same condition instead of a second, hand-copied one.
export const isOverrideEarlier = (
  effectiveEndDate: string,
  computedEndDate: string | undefined
) =>
  !!effectiveEndDate && !!computedEndDate && effectiveEndDate < computedEndDate;

export const ReviewComputedDateStep = ({
  contractId,
  givenBy,
  noticeDate,
  groundCode,
  onGroundCodeChange,
  effectiveEndDate,
  onEffectiveEndDateChange,
  overrideReason,
  onOverrideReasonChange,
  onComputedEndDateChange,
  onNext,
  onBack,
}: ReviewComputedDateStepProps) => {
  const { t } = useTranslation('contracts');

  const {
    data: preview,
    isLoading,
    isError,
  } = useTerminationPreview(contractId, givenBy, noticeDate);

  useEffect(() => {
    if (preview?.computedEndDate) {
      onComputedEndDateChange(preview.computedEndDate);
    }
    // Only re-run when the computed date itself changes.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [preview?.computedEndDate]);

  const overrideIsEarlier = isOverrideEarlier(
    effectiveEndDate,
    preview?.computedEndDate
  );

  useEffect(() => {
    // The reason field is only shown while overrideIsEarlier is true. Once the landlord edits
    // the date back to on/after the computed date (or clears it), any previously-typed reason
    // is stale — clear it proactively so it can never silently ride along in the submitted
    // request, and so the field is genuinely empty if the landlord re-reveals it later.
    if (!overrideIsEarlier && overrideReason) {
      onOverrideReasonChange('');
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [overrideIsEarlier]);

  useEffect(() => {
    // The valid ground codes depend on the contract/givenBy/noticeDate the preview resolved for
    // — if any of those change (e.g. the landlord navigates back and edits an earlier step),
    // a previously-picked code may no longer be in the new list. The <select>'s value then
    // matches no <option> and renders blank while groundCode still silently holds the stale,
    // now-invalid value, which would otherwise ride along into the submitted request.
    if (
      preview?.groundsCodes &&
      groundCode &&
      !preview.groundsCodes.includes(groundCode)
    ) {
      onGroundCodeChange('');
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [preview?.groundsCodes]);

  const groundMissing =
    !!preview?.groundsRequired && groundCode.trim().length === 0;
  const overrideReasonMissing = overrideIsEarlier && !overrideReason.trim();

  const canProceed =
    !isLoading && !isError && !groundMissing && !overrideReasonMissing;

  return (
    <div className="space-y-6">
      <div className="bg-surface-card rounded-lg border border-border-default p-6 space-y-4">
        {isLoading && (
          <p className="text-sm text-text-secondary">
            {t('termination.review.loading')}
          </p>
        )}

        {isError && (
          <div className="flex items-center gap-2 text-error-text text-sm">
            <AlertTriangle className="h-4 w-4" />
            {t('termination.review.error')}
          </div>
        )}

        {preview && (
          <>
            <div className="flex items-center gap-3">
              <CalendarClock className="h-8 w-8 text-primary-500" />
              <div>
                <p className="text-sm text-text-secondary">
                  {t('termination.review.computedEndDateLabel')}
                </p>
                <p className="text-2xl font-bold text-text-primary">
                  {preview.computedEndDate}
                </p>
                <p className="text-sm text-text-secondary">
                  {t('termination.review.noticeDaysLabel')}:{' '}
                  {t('termination.review.noticeDaysValue', {
                    count: preview.noticeDays,
                  })}
                </p>
              </div>
            </div>

            <div className="p-3 rounded-lg bg-surface-page dark:bg-surface-inset text-sm text-text-secondary">
              <span className="font-medium text-text-primary">
                {t('termination.review.sourceLabel')}:
              </span>{' '}
              {t(`termination.review.source.${preview.source}`)}
            </div>

            {preview.groundsRequired && (
              <div>
                <label
                  htmlFor="review-ground-code"
                  className="block text-sm font-medium text-text-secondary mb-1"
                >
                  {t('termination.review.groundCodeLabel')}
                </label>
                {groundMissing && (
                  <p className="text-sm text-warning-text mb-1">
                    {t('termination.review.groundRequiredWarning')}
                  </p>
                )}
                {preview.groundsCodes && preview.groundsCodes.length > 0 ? (
                  <select
                    id="review-ground-code"
                    value={groundCode}
                    onChange={(e) => onGroundCodeChange(e.target.value)}
                    className="w-full max-w-xs px-3 py-2 border border-border-strong rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500 bg-surface-card text-text-primary"
                  >
                    <option value="">
                      {t('termination.review.selectGround')}
                    </option>
                    {preview.groundsCodes.map((code) => (
                      <option key={code} value={code}>
                        {code}
                      </option>
                    ))}
                  </select>
                ) : (
                  <input
                    id="review-ground-code"
                    type="text"
                    value={groundCode}
                    onChange={(e) => onGroundCodeChange(e.target.value)}
                    className="w-full max-w-xs px-3 py-2 border border-border-strong rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500 bg-surface-card text-text-primary"
                  />
                )}
              </div>
            )}

            <div className="border-t border-border-default pt-4">
              <label
                htmlFor="effective-end-date"
                className="block text-sm font-medium text-text-secondary mb-1"
              >
                {t('termination.review.overrideToggle')}
              </label>
              <input
                id="effective-end-date"
                type="date"
                value={effectiveEndDate}
                onChange={(e) => onEffectiveEndDateChange(e.target.value)}
                className="w-full max-w-xs px-3 py-2 border border-border-strong rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500 bg-surface-card text-text-primary"
              />
              <p className="text-xs text-text-muted mt-1">
                {t('termination.review.overrideHelp')}
              </p>

              {overrideIsEarlier && (
                <div className="mt-4">
                  <label
                    htmlFor="override-reason"
                    className="block text-sm font-medium text-text-secondary mb-1"
                  >
                    {t('termination.review.overrideReasonLabel')}
                  </label>
                  <textarea
                    id="override-reason"
                    value={overrideReason}
                    onChange={(e) => onOverrideReasonChange(e.target.value)}
                    placeholder={t(
                      'termination.review.overrideReasonPlaceholder'
                    )}
                    rows={3}
                    className="w-full px-3 py-2 border border-border-strong rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500 bg-surface-card text-text-primary"
                  />
                  {overrideReasonMissing && (
                    <p className="text-sm text-warning-text mt-1">
                      {t('termination.review.overrideReasonRequired')}
                    </p>
                  )}
                </div>
              )}
            </div>
          </>
        )}
      </div>

      <div className="flex justify-between">
        <button
          onClick={onBack}
          className="px-6 py-2 rounded border border-border-default text-text-secondary hover:bg-surface-inset transition-colors"
        >
          {t('common:buttons.back')}
        </button>
        <button
          onClick={onNext}
          disabled={!canProceed}
          className="bg-primary-500 text-white px-6 py-2 rounded hover:bg-primary-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {t('termination.review.next')}
        </button>
      </div>
    </div>
  );
};
