import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { ChevronDown, ChevronUp, FileSignature } from 'lucide-react';
import { Button, LoadingSpinner } from '@buurman/ui';
import { ErrorMessage } from '@/components/ErrorMessage';
import { DocumentLanguagePicker } from '@/components/common/DocumentLanguagePicker';
import {
  defaultDocumentLanguage,
  type DocumentLanguageCode,
} from '@/components/common/documentLanguages';
import { LeaseUnavailableState } from '@/components/contracts/LeaseUnavailableState';
import { AnalyticsEvent } from '@/constants/analyticsEvents';
import { trackEvent } from '@/utils/analytics';
import { formatCountryName } from '@/utils/countryName';
import {
  useLeaseClauses,
  useUpdateLeaseClauses,
  useGenerateLeaseAgreement,
} from '@/hooks/useLeaseAgreementHooks';
import {
  LeaseAvailability,
  type ResolvedLeaseClauseResponse,
} from '@/generated/models';
import {
  isClauseIncluded,
  isLeaseSelectionDirty,
  orderClauses,
  type ClauseOrder,
  type ClauseOverrides,
} from './leaseClauseSelection';

export interface ContractLeaseAgreementTabProps {
  contractId: string;
  onGoToDocuments: () => void;
  /** Omit when the user cannot edit the property; the action button is then hidden. */
  onEditProperty?: () => void;
}

export const ContractLeaseAgreementTab = ({
  contractId,
  onGoToDocuments,
  onEditProperty,
}: ContractLeaseAgreementTabProps) => {
  const { t, i18n } = useTranslation('contracts');
  const {
    data: envelope,
    isLoading,
    isError,
    refetch,
    isFetching,
  } = useLeaseClauses(contractId);
  const clauses = envelope ? (envelope.clauses ?? []) : undefined;
  const availability = envelope?.availability;
  const countryCode = envelope?.countryCode ?? undefined;
  const countryName = formatCountryName(
    countryCode,
    i18n.resolvedLanguage ?? i18n.language
  );
  const trackedReasons = useRef(new Set<string>());

  useEffect(() => {
    const reason =
      availability === LeaseAvailability.UNAVAILABLE_COUNTRY
        ? 'unsupported_country'
        : availability === LeaseAvailability.UNAVAILABLE_NO_COUNTRY
          ? 'no_country'
          : undefined;
    if (!reason || trackedReasons.current.has(reason)) {
      return;
    }
    trackedReasons.current.add(reason);
    trackEvent(AnalyticsEvent.LEASE_UNAVAILABLE_VIEWED, {
      countryCode,
      reason,
    });
  }, [availability, countryCode]);
  const updateMutation = useUpdateLeaseClauses(contractId);
  const generateMutation = useGenerateLeaseAgreement(contractId);

  // Local overrides for the optional clauses the landlord has toggled this session. Only
  // deviations from the server's `included` are stored, so there is nothing to re-sync when the
  // query refetches — unset entries fall back to `clause.included` below. Cleared on a successful
  // save so the overrides don't linger once the server reflects them.
  const [overrides, setOverrides] = useState<ClauseOverrides>({});

  // Local clause order (templateIdentifiers) once the landlord has moved something; null means
  // "show the server order". Cleared on a successful save, like overrides.
  const [order, setOrder] = useState<ClauseOrder>(null);

  // Same default as the booklet menu: the user's UI language (English when PDFs don't exist in
  // it). The server falls back to the country's national language when it has no document in it.
  const [language, setLanguage] = useState<DocumentLanguageCode>(() =>
    defaultDocumentLanguage(i18n.language)
  );

  // True while "Save and generate" runs, covering both requests so the button spins throughout.
  const [savingThenGenerating, setSavingThenGenerating] = useState(false);

  const isIncluded = (clause: ResolvedLeaseClauseResponse) =>
    isClauseIncluded(clause, overrides);

  const orderedClauses = (): ResolvedLeaseClauseResponse[] =>
    orderClauses(clauses ?? [], order);

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

  /**
   * Persists the local selection; rejects (after the hook's error toast) when the save fails.
   * `silent` skips the success toast when a generate (with its own toast) follows.
   */
  const saveSelection = async (silent = false) => {
    await updateMutation.mutateAsync({
      clauses: orderedClauses().map((clause, index) => ({
        templateIdentifier: clause.templateIdentifier,
        included: isIncluded(clause),
        // The server ignores a pinned clause's sortOrder, so it is sent unchanged.
        sortOrder: clause.pinned || !order ? clause.sortOrder : index + 1,
      })),
      silent,
    });
    setOverrides({});
    setOrder(null);
  };

  const handleSave = () => {
    if (!clauses) {
      return;
    }
    // The failure toast comes from the mutation hook; nothing else to do here.
    saveSelection().catch(() => {});
  };

  const isDirty = !!clauses && isLeaseSelectionDirty(clauses, overrides, order);

  const handleGenerate = async () => {
    if (isDirty) {
      // Generate only from the saved selection: wait for the save, and stop if it fails.
      setSavingThenGenerating(true);
      try {
        await saveSelection(true);
      } catch {
        setSavingThenGenerating(false);
        return;
      }
    }
    try {
      await generateMutation.mutateAsync(language);
    } catch {
      // The failure toast comes from the mutation hook.
    } finally {
      setSavingThenGenerating(false);
    }
  };

  if (isLoading) {
    return <LoadingSpinner className="p-0" />;
  }

  if (isError || !envelope || !clauses) {
    return (
      <ErrorMessage
        message={t('leaseAgreement.error.title')}
        description={t('leaseAgreement.error.description')}
        retryLabel={t('leaseAgreement.error.retry')}
        isRetrying={isFetching}
        onRetry={() => {
          refetch();
        }}
      />
    );
  }

  const isUnavailable =
    availability === LeaseAvailability.UNAVAILABLE_COUNTRY ||
    availability === LeaseAvailability.UNAVAILABLE_NO_COUNTRY;

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
        {!isUnavailable && (
          <p className="text-sm text-text-secondary">
            {t('leaseAgreement.description')}
          </p>
        )}
      </div>

      {availability === LeaseAvailability.UNAVAILABLE_COUNTRY && (
        <LeaseUnavailableState
          reason="country"
          countryName={countryName}
          onAction={onGoToDocuments}
        />
      )}

      {availability === LeaseAvailability.UNAVAILABLE_NO_COUNTRY && (
        <LeaseUnavailableState reason="no-country" onAction={onEditProperty} />
      )}

      {isUnavailable ? null : sortedClauses.length === 0 ? (
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

      {!isUnavailable && (
        <div className="flex flex-wrap gap-3">
          <Button
            variant="secondary"
            onClick={handleSave}
            isLoading={updateMutation.isPending && !savingThenGenerating}
            disabled={sortedClauses.length === 0 || savingThenGenerating}
          >
            {t('leaseAgreement.saveSelection')}
          </Button>
          <DocumentLanguagePicker
            value={language}
            onChange={setLanguage}
            label={t('leaseAgreement.language')}
            disabled={savingThenGenerating || generateMutation.isPending}
          />
          <Button
            variant="primary"
            leftIcon={<FileSignature />}
            onClick={handleGenerate}
            isLoading={savingThenGenerating || generateMutation.isPending}
            disabled={updateMutation.isPending && !savingThenGenerating}
          >
            {isDirty
              ? t('leaseAgreement.saveAndGenerate')
              : t('leaseAgreement.generate')}
          </Button>
        </div>
      )}
    </div>
  );
};
