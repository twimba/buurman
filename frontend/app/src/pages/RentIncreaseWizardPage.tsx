import { useState, useCallback, useEffect, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { Scale, ChevronRight } from 'lucide-react';
import {
  useRentIncreasePreview,
  useApplyRentIncreases,
} from '@/hooks/useRentIncreaseHooks';
import type {
  RentIncreasePreviewResponse,
  RentIncreaseItem,
  ApplyRentIncreasesResponse,
} from '@/types/rentIncrease';
import { YearSelectionStep } from '@/components/rentIncreases/YearSelectionStep';
import { PropertyAdjustmentStep } from '@/components/rentIncreases/PropertyAdjustmentStep';
import { ReviewStep } from '@/components/rentIncreases/ReviewStep';
import { ConfirmationStep } from '@/components/rentIncreases/ConfirmationStep';

type WizardStep = 'year' | 'adjust' | 'review' | 'confirmation';

export const RentIncreaseWizardPage = () => {
  const { t } = useTranslation('contracts');

  const STEPS = useMemo(
    () => [
      { key: 'year' as WizardStep, label: t('rentIncrease.steps.selectYear') },
      {
        key: 'adjust' as WizardStep,
        label: t('rentIncrease.steps.adjustRents'),
      },
      { key: 'review' as WizardStep, label: t('rentIncrease.steps.review') },
      {
        key: 'confirmation' as WizardStep,
        label: t('rentIncrease.steps.confirmation'),
      },
    ],
    [t]
  );
  const [step, setStep] = useState<WizardStep>('year');
  const [year, setYear] = useState(new Date().getFullYear());
  const [preview, setPreview] = useState<RentIncreasePreviewResponse | null>(
    null
  );
  const [increases, setIncreases] = useState<RentIncreaseItem[]>([]);
  const [applyResult, setApplyResult] =
    useState<ApplyRentIncreasesResponse | null>(null);
  const [appliedContractIds, setAppliedContractIds] = useState<Set<string>>(
    new Set()
  );

  const previewMutation = useRentIncreasePreview();
  const applyMutation = useApplyRentIncreases();

  const fetchPreview = useCallback(
    (selectedYear: number) => {
      previewMutation.mutate(selectedYear, {
        onSuccess: (data) => {
          setPreview(data);
          // Initialize increases from preview data
          const initial = data.contracts.map((contract) => ({
            contractIdentifier: contract.contractIdentifier,
            increasePercentage: contract.regulationMaxPercent ?? 0,
            newRentAmount:
              Math.round(
                contract.currentRentAmount *
                  (1 + (contract.regulationMaxPercent ?? 0) / 100) *
                  100
              ) / 100,
            effectiveDate: contract.suggestedEffectiveDate ?? '',
          }));
          setIncreases(initial);
        },
      });
    },
    [previewMutation]
  );

  useEffect(() => {
    fetchPreview(year);
    // Only run on mount
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleYearChange = (newYear: number) => {
    setYear(newYear);
    fetchPreview(newYear);
  };

  const handleApply = () => {
    const changedIncreases = increases.filter(
      (inc) =>
        inc.increasePercentage > 0 &&
        !appliedContractIds.has(inc.contractIdentifier)
    );
    applyMutation.mutate(
      { year, increases: changedIncreases },
      {
        onSuccess: (data) => {
          setApplyResult(data);
          setStep('confirmation');
        },
      }
    );
  };

  const handleBackFromConfirmation = () => {
    if (!applyResult) {
      return;
    }
    const succeededIds = applyResult.results
      .filter((r) => r.success)
      .map((r) => r.contractIdentifier);
    setAppliedContractIds((prev) => new Set([...prev, ...succeededIds]));
    setApplyResult(null);
    setStep('adjust');
  };

  const stepIndex = STEPS.findIndex((s) => s.key === step);

  return (
    <div className="min-h-[100dvh] bg-background">
      <div className="px-4 py-8">
        {/* Header */}
        <div className="mb-6">
          <div className="flex items-center gap-3 mb-1">
            <Scale className="h-8 w-8 text-primary-500 dark:text-primary-300" />
            <h1 className="text-3xl font-bold text-text-primary">
              {t('rentIncrease.title')}
            </h1>
          </div>
          <p className="text-text-secondary ml-11">
            {t('rentIncrease.subtitle')}
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
        {step === 'year' && (
          <YearSelectionStep
            year={year}
            onYearChange={handleYearChange}
            countrySummaries={preview?.countrySummaries ?? []}
            onNext={() => setStep('adjust')}
            isLoading={previewMutation.isPending}
          />
        )}

        {step === 'adjust' && preview && (
          <PropertyAdjustmentStep
            contracts={preview.contracts}
            increases={increases}
            onIncreaseChange={setIncreases}
            onNext={() => setStep('review')}
            onBack={() => setStep('year')}
            appliedContractIds={appliedContractIds}
          />
        )}

        {step === 'review' && preview && (
          <ReviewStep
            contracts={preview.contracts}
            increases={increases}
            year={year}
            onBack={() => setStep('adjust')}
            onApply={handleApply}
            isApplying={applyMutation.isPending}
          />
        )}

        {step === 'confirmation' && applyResult && (
          <ConfirmationStep
            response={applyResult}
            contracts={preview?.contracts ?? []}
            increases={increases}
            onBack={handleBackFromConfirmation}
          />
        )}
      </div>
    </div>
  );
};
