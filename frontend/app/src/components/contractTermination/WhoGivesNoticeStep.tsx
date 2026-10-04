import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { Home, User } from 'lucide-react';
import type { TerminationGivenBy } from '@/hooks/useContractTerminationHooks';

interface WhoGivesNoticeStepProps {
  givenBy: TerminationGivenBy | undefined;
  onGivenByChange: (givenBy: TerminationGivenBy) => void;
  onNext: () => void;
}

export const WhoGivesNoticeStep = ({
  givenBy,
  onGivenByChange,
  onNext,
}: WhoGivesNoticeStepProps) => {
  const { t } = useTranslation('contracts');

  const options: { value: TerminationGivenBy; icon: ReactNode }[] = [
    { value: 'LANDLORD', icon: <Home className="h-6 w-6" /> },
    { value: 'TENANT', icon: <User className="h-6 w-6" /> },
  ];

  return (
    <div className="space-y-6">
      <div className="bg-surface-card rounded-lg border border-border-default p-6">
        <h3 className="text-lg font-semibold text-text-primary mb-4">
          {t('termination.whoGivesNotice.question')}
        </h3>
        <div className="grid gap-4 sm:grid-cols-2">
          {options.map((option) => (
            <button
              key={option.value}
              type="button"
              onClick={() => onGivenByChange(option.value)}
              className={`flex items-start gap-3 p-4 rounded-lg border text-left transition-colors ${
                givenBy === option.value
                  ? 'border-primary-500 bg-primary-50 dark:bg-primary-500/10'
                  : 'border-border-default hover:bg-surface-inset'
              }`}
            >
              <span className="text-primary-500">{option.icon}</span>
              <span>
                <span className="block font-medium text-text-primary">
                  {t(`termination.givenByValues.${option.value}`)}
                </span>
                <span className="block text-sm text-text-secondary">
                  {t(
                    `termination.whoGivesNotice.${option.value === 'LANDLORD' ? 'landlordDescription' : 'tenantDescription'}`
                  )}
                </span>
              </span>
            </button>
          ))}
        </div>
      </div>

      <div className="flex justify-end">
        <button
          onClick={onNext}
          disabled={!givenBy}
          className="bg-primary-500 text-white px-6 py-2 rounded hover:bg-primary-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {t('termination.whoGivesNotice.next')}
        </button>
      </div>
    </div>
  );
};
