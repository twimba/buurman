import { useState, useMemo } from 'react';
import { useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { FileWarning, ChevronRight } from 'lucide-react';
import { useContract, useContractDeposit } from '@/hooks/useContractHooks';
import {
  useTerminateContract,
  type TerminationGivenBy,
} from '@/hooks/useContractTerminationHooks';
import type { ContractTerminationResponse } from '@/generated/models';
import { WhoGivesNoticeStep } from '@/components/contractTermination/WhoGivesNoticeStep';
import { NoticeDateAndGroundStep } from '@/components/contractTermination/NoticeDateAndGroundStep';
import {
  ReviewComputedDateStep,
  isOverrideEarlier,
} from '@/components/contractTermination/ReviewComputedDateStep';
import { LetterPreviewStep } from '@/components/contractTermination/LetterPreviewStep';
import { ConfirmationStep } from '@/components/contractTermination/ConfirmationStep';

type WizardStep =
  | 'who-gives-notice'
  | 'notice-date-ground'
  | 'review'
  | 'letter-preview'
  | 'confirmation';

const today = () => new Date().toISOString().slice(0, 10);

export const TerminationWizardPage = () => {
  const { t } = useTranslation('contracts');
  const { id = '' } = useParams<{ id: string }>();

  const { data: contract } = useContract(id);
  const { data: deposit, isLoading: isDepositLoading } =
    useContractDeposit(id);

  const STEPS = useMemo(
    () => [
      {
        key: 'who-gives-notice' as WizardStep,
        label: t('termination.steps.whoGivesNotice'),
      },
      {
        key: 'notice-date-ground' as WizardStep,
        label: t('termination.steps.noticeDateGround'),
      },
      { key: 'review' as WizardStep, label: t('termination.steps.review') },
      {
        key: 'letter-preview' as WizardStep,
        label: t('termination.steps.letterPreview'),
      },
      {
        key: 'confirmation' as WizardStep,
        label: t('termination.steps.confirmation'),
      },
    ],
    [t]
  );

  const [step, setStep] = useState<WizardStep>('who-gives-notice');
  const [givenBy, setGivenBy] = useState<TerminationGivenBy | undefined>(
    undefined
  );
  const [noticeDate, setNoticeDate] = useState(today());
  const [groundCode, setGroundCode] = useState('');
  const [effectiveEndDate, setEffectiveEndDate] = useState('');
  const [overrideReason, setOverrideReason] = useState('');
  const [computedEndDate, setComputedEndDate] = useState('');
  const [inspectionDate, setInspectionDate] = useState('');
  const [result, setResult] = useState<ContractTerminationResponse | null>(
    null
  );

  const terminateMutation = useTerminateContract(id);

  const handleConfirm = () => {
    if (!givenBy) {
      return;
    }
    // Belt-and-suspenders: ReviewComputedDateStep already clears overrideReason as soon as
    // the date stops being earlier than computed, but re-checking the live condition here
    // means a stale reason can never ride along in the submitted request even if that clear
    // were ever bypassed (e.g. the date field's value came from somewhere other than the
    // review step's own input in a future change).
    const effectiveEndDateIsEarlier = isOverrideEarlier(
      effectiveEndDate,
      computedEndDate
    );

    terminateMutation.mutate(
      {
        givenBy,
        noticeDate,
        groundCode: groundCode || undefined,
        effectiveEndDate: effectiveEndDate || undefined,
        overrideReason: effectiveEndDateIsEarlier
          ? overrideReason || undefined
          : undefined,
        inspectionDate: inspectionDate || undefined,
      },
      {
        onSuccess: (data) => {
          setResult(data);
          setStep('confirmation');
        },
      }
    );
  };

  const stepIndex = STEPS.findIndex((s) => s.key === step);

  return (
    <div className="min-h-full bg-background">
      <div className="px-4 py-8">
        {/* Header */}
        <div className="mb-6">
          <div className="flex items-center gap-3 mb-1">
            <FileWarning className="h-8 w-8 text-primary-500 dark:text-primary-300" />
            <h1 className="text-3xl font-bold text-text-primary">
              {t('termination.title')}
            </h1>
          </div>
          <p className="text-text-secondary ml-11">
            {t('termination.subtitle')}
            {contract ? ` — ${contract.property.street}` : ''}
          </p>
        </div>

        {/* Step Indicator */}
        <div className="mb-8">
          <div className="flex items-center gap-2">
            {STEPS.map((s, i) => (
              <div key={s.key} className="flex items-center">
                <div
                  className={`flex items-center gap-2 px-3 py-1.5 rounded-full text-sm ${
                    i === stepIndex
                      ? 'bg-primary-500 text-white font-semibold'
                      : i < stepIndex
                        ? 'bg-success-bg text-success-text'
                        : 'bg-surface-inset text-text-secondary'
                  }`}
                >
                  <span className="w-5 h-5 rounded-full flex items-center justify-center text-xs font-bold border border-current">
                    {i + 1}
                  </span>
                  <span className="hidden sm:inline">{s.label}</span>
                </div>
                {i < STEPS.length - 1 && (
                  <ChevronRight className="h-4 w-4 mx-1 text-text-muted " />
                )}
              </div>
            ))}
          </div>
        </div>

        {/* Step Content */}
        {step === 'who-gives-notice' && (
          <WhoGivesNoticeStep
            givenBy={givenBy}
            onGivenByChange={setGivenBy}
            onNext={() => setStep('notice-date-ground')}
          />
        )}

        {step === 'notice-date-ground' && (
          <NoticeDateAndGroundStep
            noticeDate={noticeDate}
            onNoticeDateChange={setNoticeDate}
            groundCode={groundCode}
            onGroundCodeChange={setGroundCode}
            onNext={() => setStep('review')}
            onBack={() => setStep('who-gives-notice')}
          />
        )}

        {step === 'review' && (
          <ReviewComputedDateStep
            contractId={id}
            givenBy={givenBy}
            noticeDate={noticeDate}
            groundCode={groundCode}
            onGroundCodeChange={setGroundCode}
            effectiveEndDate={effectiveEndDate}
            onEffectiveEndDateChange={setEffectiveEndDate}
            overrideReason={overrideReason}
            onOverrideReasonChange={setOverrideReason}
            onComputedEndDateChange={setComputedEndDate}
            onNext={() => setStep('letter-preview')}
            onBack={() => setStep('notice-date-ground')}
          />
        )}

        {step === 'letter-preview' && (
          <LetterPreviewStep
            givenBy={givenBy}
            noticeDate={noticeDate}
            groundCode={groundCode}
            computedEndDate={computedEndDate}
            effectiveEndDate={effectiveEndDate}
            overrideReason={overrideReason}
            inspectionDate={inspectionDate}
            onInspectionDateChange={setInspectionDate}
            onBack={() => setStep('review')}
            onConfirm={handleConfirm}
            isSubmitting={terminateMutation.isPending}
          />
        )}

        {step === 'confirmation' && result && (
          // While the deposit query is still loading, !!deposit alone would false-negative to
          // "no deposit" — assume one might exist until we actually know otherwise, since
          // silently omitting the deposit-return reminder is worse than a brief over-inclusive
          // render.
          <ConfirmationStep
            response={result}
            hasDeposit={isDepositLoading || !!deposit}
          />
        )}
      </div>
    </div>
  );
};
