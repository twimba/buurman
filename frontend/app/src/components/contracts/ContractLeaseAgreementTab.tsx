import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { ChevronDown, ChevronUp, FileSignature } from 'lucide-react';
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

  // Local clause order (templateIdentifiers) once the landlord has moved something; null means
  // "show the server order". Cleared on a successful save, like overrides.
  const [order, setOrder] = useState<string[] | null>(null);

  const isIncluded = (clause: ResolvedLeaseClauseResponse) =>
    clause.optional
      ? (overrides[clause.templateIdentifier] ?? clause.included)
      : true;

  const orderedClauses = (): ResolvedLeaseClauseResponse[] => {
    // Pinned clauses (parties, premises) always lead; the rest follow by sortOrder.
    const base = (clauses ?? [])
      .slice()
      .sort(
        (a, b) =>
          Number(b.pinned) - Number(a.pinned) || a.sortOrder - b.sortOrder
      );
    if (!order) {
      return base;
    }
    const byId = new Map(base.map((c) => [c.templateIdentifier, c]));
    const known = new Set(order);
    const ordered = order
      .map((id) => byId.get(id))
      .filter((c): c is ResolvedLeaseClauseResponse => !!c);
    // A refetch may add clauses the local order has never seen; keep them in server order.
    const unseen = base.filter((c) => !known.has(c.templateIdentifier));
    return [...ordered, ...unseen].sort(
      (a, b) => Number(b.pinned) - Number(a.pinned)
    );
  };

  const handleMove = (index: number, delta: -1 | 1) => {
    const current = orderedClauses();
    const target = current[index + delta];
    if (current[index].pinned || !target || target.pinned) {
      return;
    }
    const next = current.map((c) => c.templateIdentifier);
    [next[index], next[index + delta]] = [next[index + delta], next[index]];
    setOrder(next);
  };

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
      orderedClauses().map((clause, index) => ({
        templateIdentifier: clause.templateIdentifier,
        included: isIncluded(clause),
        // The server ignores a pinned clause's sortOrder, so it is sent unchanged.
        sortOrder: clause.pinned || !order ? clause.sortOrder : index + 1,
      })),
      {
        onSuccess: () => {
          setOverrides({});
          setOrder(null);
        },
      }
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

  const sortedClauses = orderedClauses();
  const articleNumbers = sortedClauses.map((clause, index) =>
    isIncluded(clause)
      ? sortedClauses.slice(0, index + 1).filter(isIncluded).length
      : null
  );

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
          {sortedClauses.map((clause, index) => {
            const checked = isIncluded(clause);
            const nextClause = sortedClauses[index + 1];
            const previousClause = sortedClauses[index - 1];
            const articleNumber = articleNumbers[index];
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
                    {articleNumber !== null && (
                      <span
                        data-testid="article-number"
                        className="text-sm font-semibold text-text-secondary"
                      >
                        {articleNumber}
                      </span>
                    )}
                    <span className="text-sm font-medium text-text-primary">
                      {clause.title}
                    </span>
                    {clause.pinned && (
                      <span className="text-xs text-text-muted">
                        {t('leaseAgreement.pinned')}
                      </span>
                    )}
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
                <div className="flex flex-col">
                  <button
                    type="button"
                    aria-label={t('leaseAgreement.moveUp', {
                      title: clause.title,
                    })}
                    disabled={
                      clause.pinned || !previousClause || previousClause.pinned
                    }
                    onClick={() => handleMove(index, -1)}
                    className="p-1 rounded text-text-secondary hover:bg-surface-hover disabled:opacity-40 disabled:cursor-not-allowed"
                  >
                    <ChevronUp className="h-4 w-4" />
                  </button>
                  <button
                    type="button"
                    aria-label={t('leaseAgreement.moveDown', {
                      title: clause.title,
                    })}
                    disabled={clause.pinned || !nextClause}
                    onClick={() => handleMove(index, 1)}
                    className="p-1 rounded text-text-secondary hover:bg-surface-hover disabled:opacity-40 disabled:cursor-not-allowed"
                  >
                    <ChevronDown className="h-4 w-4" />
                  </button>
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
