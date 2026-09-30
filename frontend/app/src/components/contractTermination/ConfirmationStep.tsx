import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { CheckCircle } from 'lucide-react';
import type { ContractTerminationResponse } from '@/generated/models';

interface ConfirmationStepProps {
  response: ContractTerminationResponse;
  hasDeposit?: boolean;
}

export const ConfirmationStep = ({
  response,
  hasDeposit = false,
}: ConfirmationStepProps) => {
  const { t } = useTranslation('contracts');
  const navigate = useNavigate();

  return (
    <div className="space-y-6">
      <div className="bg-surface-card rounded-lg border border-border-default p-6">
        <div className="flex items-center gap-3 mb-4">
          <CheckCircle className="h-8 w-8 text-success-text" />
          <div>
            <h3 className="text-xl font-bold text-text-primary">
              {t('termination.confirmation.title')}
            </h3>
            <p className="text-sm text-text-secondary">
              {t('termination.confirmation.description')}
            </p>
          </div>
        </div>

        <dl className="grid gap-3 sm:grid-cols-2">
          <div>
            <dt className="text-sm text-text-secondary">
              {t('termination.confirmation.effectiveEndDateLabel')}
            </dt>
            <dd className="font-medium text-text-primary">
              {response.effectiveEndDate}
            </dd>
          </div>
          {response.noticeLetterDocumentIdentifier && (
            <div>
              <dt className="text-sm text-text-secondary">
                {t('termination.confirmation.noticeLetterLabel')}
              </dt>
              <dd className="font-medium text-text-primary">
                {response.noticeLetterDocumentIdentifier}
              </dd>
            </div>
          )}
        </dl>

        {hasDeposit && (
          <p className="text-sm text-info-text mt-4">
            {t('termination.confirmation.depositNote')}
          </p>
        )}
      </div>

      <div className="flex justify-end gap-3">
        <button
          onClick={() =>
            navigate(`/contracts/${response.contractIdentifier}?tab=documents`)
          }
          className="px-6 py-2 rounded border border-border-default text-text-secondary hover:bg-surface-inset transition-colors"
        >
          {t('termination.confirmation.viewDocument')}
        </button>
        <button
          onClick={() => navigate(`/contracts/${response.contractIdentifier}`)}
          className="bg-primary-500 text-white px-6 py-2 rounded hover:bg-primary-600 transition-colors"
        >
          {t('termination.confirmation.backToContract')}
        </button>
      </div>
    </div>
  );
};
