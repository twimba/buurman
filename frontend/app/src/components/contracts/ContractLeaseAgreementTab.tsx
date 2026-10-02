import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { FileSignature } from 'lucide-react';
import { Button, LoadingSpinner } from '@buurman/ui';
import { ErrorMessage } from '@/components/ErrorMessage';
import {
  useLeaseClauses,
  useUpdateLeaseClauses,
  useGenerateLeaseAgreement,
} from '@/hooks/useLeaseAgreementHooks';
import type { ResolvedLeaseClauseResponse } from '@/generated/models';

interface ContractLeaseAgreementTabProps {
  contractId: string;
}

export const ContractLeaseAgreementTab = ({
  contractId,
}: ContractLeaseAgreementTabProps) => {
  const { t } = useTranslation('contracts');
  const { data: clauses, isLoading, isError } = useLeaseClauses(contractId);
  const updateMutation = useUpdateLeaseClauses(contractId);
  const generateMutation = useGenerateLeaseAgreement(contractId);

  // Local overrides for the optional clauses the landlord has toggled this session. Only
  // deviations from the server's `included` are stored, so there is nothing to re-sync when the
  // query refetches — unset entries fall back to `clause.included` below. Cleared on a successful
  // save so the overrides don't linger once the server reflects them.
  const [overrides, setOverrides] = useState<Record<string, boolean>>({});

  const handleToggle = (clause: ResolvedLeaseClauseResponse) => {
    // Non-optional clauses cannot be excluded — enforced here as well as server-side, since this
    // is data integrity for a legal document, not just UX polish.
    if (!clause.optional) {
      return;
    }
    setOverrides((prev) => ({
      ...prev,
      [clause.templateIdentifier]: !(
        prev[clause.templateIdentifier] ?? clause.included
      ),
    }));
  };

  const handleSave = () => {
    if (!clauses) {
      return;
    }
    updateMutation.mutate(
      clauses.map((clause) => ({
        templateIdentifier: clause.templateIdentifier,
        included: clause.optional
          ? (overrides[clause.templateIdentifier] ?? clause.included)
          : true,
        sortOrder: clause.sortOrder,
      })),
      { onSuccess: () => setOverrides({}) }
    );
  };

  const handleGenerate = () => {
    generateMutation.mutate();
  };

  if (isLoading) {
    return <LoadingSpinner className="p-0" />;
  }

  if (isError || !clauses) {
    return <ErrorMessage message={t('leaseAgreement.loadError')} />;
  }

  const sortedClauses = clauses
    .slice()
    .sort((a, b) => a.sortOrder - b.sortOrder);

  return (
    <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6 space-y-6">
      <div>
        <h2 className="text-xl font-semibold text-text-primary mb-1 flex items-center gap-2">
          <FileSignature className="h-5 w-5" />
          {t('leaseAgreement.title')}
        </h2>
        <p className="text-sm text-text-secondary">
          {t('leaseAgreement.description')}
        </p>
      </div>

      {sortedClauses.length === 0 ? (
        <p className="text-sm text-text-secondary">
          {t('leaseAgreement.empty')}
        </p>
      ) : (
        <ul className="space-y-3">
          {sortedClauses.map((clause) => {
            const checked = clause.optional
              ? (overrides[clause.templateIdentifier] ?? clause.included)
              : true;
            return (
              <li
                key={clause.templateIdentifier}
                className="flex items-start gap-3 p-3 rounded-lg border border-border-default"
              >
                <input
                  type="checkbox"
                  checked={checked}
                  disabled={!clause.optional}
                  onChange={() => handleToggle(clause)}
                  className="mt-1 rounded border-border-default text-primary-500 focus:ring-primary-500/20 disabled:opacity-60"
                  aria-label={clause.title}
                />
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-medium text-text-primary">
                      {clause.title}
                    </span>
                    {!clause.optional && (
                      <span className="text-xs text-text-muted">
                        {t('leaseAgreement.required')}
                      </span>
                    )}
                  </div>
                  <p className="text-xs text-text-secondary mt-1 line-clamp-2">
                    {clause.body}
                  </p>
                </div>
              </li>
            );
          })}
        </ul>
      )}

      <div className="flex gap-3">
        <Button
          variant="secondary"
          onClick={handleSave}
          isLoading={updateMutation.isPending}
          disabled={sortedClauses.length === 0}
        >
          {t('leaseAgreement.saveSelection')}
        </Button>
        <Button
          variant="primary"
          leftIcon={<FileSignature />}
          onClick={handleGenerate}
          isLoading={generateMutation.isPending}
        >
          {t('leaseAgreement.generate')}
        </Button>
      </div>
    </div>
  );
};
