import { useState, useMemo } from 'react';
import {
  ChevronDown,
  ChevronUp,
  TrendingUp,
  TrendingDown,
  DollarSign,
  Plus,
  Trash2,
  FileText,
  FolderDown,
  RefreshCw,
  Shield,
} from 'lucide-react';
import {
  RentPeriodResponse,
  RentComponentResponseItem,
  RentComponentFormItem,
  ContractStatus,
} from '@/types/contract';
import {
  useRentPeriods,
  useDeleteRentPeriod,
  useAddRentPeriod,
} from '@/hooks/useRentPeriodHooks';
import { useContractDocuments } from '@/hooks/useContractHooks';
import { AdjustRentModal } from './AdjustRentModal';
import { GenerateRentChangeModal } from './GenerateRentChangeModal';
import { COUNTRY_OFFICIAL_LANGUAGES } from './GenerateDocumentsModal';
import { DocumentPreviewModal } from '@/components/documents/DocumentPreviewModal';
import { useFormatDate } from '@/hooks/useFormatDate';
import { RichTextDisplay } from '@buurman/ui';
import { useTeam } from '@/context/TeamContext';
import type { DocumentResponse } from '@/types/property';
import { useTranslation } from 'react-i18next';

interface RentTimelineProps {
  contractIdentifier: string;
  contractStatus: ContractStatus;
  currency: string;
  currentRentAmount: number;
  currentComponents: RentComponentResponseItem[];
  paymentFrequency: string;
  documentLanguages?: string[];
  countryCode?: string;
}

function isFuturePeriod(effectiveFrom: string): boolean {
  return new Date(effectiveFrom) > new Date();
}

function extractLang(fileName: string): string {
  const match = fileName.match(/-([a-z]{2})\.pdf$/);
  return match ? match[1] : '??';
}

export const RentTimeline = ({
  contractIdentifier,
  contractStatus,
  currency,
  currentRentAmount,
  currentComponents,
  paymentFrequency,
  documentLanguages,
  countryCode,
}: RentTimelineProps) => {
  const { t } = useTranslation('contracts');
  const { canEditData } = useTeam();
  const { formatDate } = useFormatDate();
  const [isExpanded, setIsExpanded] = useState(false);
  const [showAdjustModal, setShowAdjustModal] = useState(false);
  const [generateTarget, setGenerateTarget] = useState<{
    period: RentPeriodResponse;
    regenerate: boolean;
  } | null>(null);

  const { data: periods = [], isLoading } = useRentPeriods(contractIdentifier);
  const { data: allDocuments } = useContractDocuments(contractIdentifier);
  const addRentPeriod = useAddRentPeriod(contractIdentifier);
  const deleteRentPeriod = useDeleteRentPeriod(contractIdentifier);

  /** Map period identifier -> documents matching that period's filename pattern */
  const documentsByPeriod = useMemo(() => {
    const map = new Map<string, DocumentResponse[]>();
    if (!allDocuments) {
      return map;
    }
    for (const doc of allDocuments) {
      if (!doc.fileName.startsWith('rent-change-')) {
        continue;
      }
      // Match rent-change-{periodIdentifier}-{lang}.pdf
      for (const period of periods) {
        if (doc.fileName.startsWith(`rent-change-${period.identifier}-`)) {
          const list = map.get(period.identifier) ?? [];
          list.push(doc);
          map.set(period.identifier, list);
          break;
        }
      }
    }
    return map;
  }, [allDocuments, periods]);

  const handleAddRentPeriod = (
    rentAmount: number,
    effectiveFrom: string,
    notes?: string,
    components?: RentComponentFormItem[]
  ) => {
    addRentPeriod.mutate(
      { rentAmount, effectiveFrom, notes, components },
      { onSuccess: () => setShowAdjustModal(false) }
    );
  };

  const handleDeletePeriod = (periodIdentifier: string) => {
    if (window.confirm('Are you sure you want to delete this rent period?')) {
      deleteRentPeriod.mutate(periodIdentifier);
    }
  };

  const canAdjustRent = canEditData && contractStatus === ContractStatus.ACTIVE;
  const hasHistory = periods.length >= 1;

  return (
    <>
      {/* Current Rent */}
      <div className="flex items-center gap-3">
        <DollarSign className="h-5 w-5 text-text-muted " />
        <div className="flex-1">
          <p className="text-sm text-text-secondary">
            {t('overview.rentDetails')}
          </p>
          <p className="font-medium text-text-primary">
            {currency} {currentRentAmount.toFixed(2)} /{' '}
            {paymentFrequency.toLowerCase()}
          </p>
        </div>
        {canAdjustRent && (
          <button
            onClick={() => setShowAdjustModal(true)}
            className="flex items-center gap-1 px-3 py-1.5 text-xs font-medium text-primary-500 bg-primary-500/10 rounded-md hover:bg-primary-500/20 transition-colors"
          >
            <Plus className="h-3.5 w-3.5" />
            {t('rentTimeline.adjustButton')}
          </button>
        )}
      </div>

      {/* Rent History Toggle */}
      {hasHistory && (
        <div className="mt-2">
          <button
            onClick={() => setIsExpanded(!isExpanded)}
            className="flex items-center gap-1.5 text-sm text-text-secondary hover:text-text-secondary transition-colors"
          >
            {isExpanded ? (
              <ChevronUp className="h-4 w-4" />
            ) : (
              <ChevronDown className="h-4 w-4" />
            )}
            Rent History ({periods.length} period
            {periods.length !== 1 ? 's' : ''})
          </button>

          {isExpanded && (
            <div className="mt-3 space-y-2">
              {isLoading ? (
                <p className="text-sm text-text-muted">Loading...</p>
              ) : (
                periods.map((period) => (
                  <RentPeriodRow
                    key={period.identifier}
                    period={period}
                    documents={documentsByPeriod.get(period.identifier) ?? []}
                    countryCode={countryCode}
                    currency={currency}
                    formatDate={formatDate}
                    canEdit={canEditData}
                    onDelete={
                      canEditData && isFuturePeriod(period.effectiveFrom)
                        ? () => handleDeletePeriod(period.identifier)
                        : undefined
                    }
                    onGenerateDocuments={() =>
                      setGenerateTarget({ period, regenerate: false })
                    }
                    onRegenerateDocuments={() =>
                      setGenerateTarget({ period, regenerate: true })
                    }
                  />
                ))
              )}
            </div>
          )}
        </div>
      )}

      {/* Adjust Rent Modal */}
      {showAdjustModal && (
        <AdjustRentModal
          currentRent={currentRentAmount}
          currency={currency}
          currentComponents={currentComponents}
          rentPeriods={periods}
          onClose={() => setShowAdjustModal(false)}
          onConfirm={handleAddRentPeriod}
          isLoading={addRentPeriod.isPending}
        />
      )}

      {/* Generate Documents Modal */}
      {generateTarget && (
        <GenerateRentChangeModal
          contractIdentifier={contractIdentifier}
          periodIdentifier={generateTarget.period.identifier}
          defaultLanguages={documentLanguages ?? ['en']}
          countryCode={countryCode}
          regenerate={generateTarget.regenerate}
          onClose={() => setGenerateTarget(null)}
        />
      )}
    </>
  );
};

function RentPeriodRow({
  period,
  documents,
  countryCode,
  currency,
  formatDate,
  canEdit,
  onDelete,
  onGenerateDocuments,
  onRegenerateDocuments,
}: {
  period: RentPeriodResponse;
  documents: DocumentResponse[];
  countryCode?: string;
  currency: string;
  formatDate: (date: string) => string;
  canEdit: boolean;
  onDelete?: () => void;
  onGenerateDocuments: () => void;
  onRegenerateDocuments: () => void;
}) {
  const hasDocuments = documents.length > 0;
  const officialLangs = useMemo(
    () =>
      countryCode
        ? (COUNTRY_OFFICIAL_LANGUAGES[countryCode.toUpperCase()] ?? [])
        : [],
    [countryCode]
  );
  const [previewIndex, setPreviewIndex] = useState<number | null>(null);

  // Sort docs: official languages first, then alphabetically
  const sortedDocs = useMemo(() => {
    return [...documents].sort((a, b) => {
      const langA = extractLang(a.fileName);
      const langB = extractLang(b.fileName);
      const aOfficial = officialLangs.includes(langA) ? 0 : 1;
      const bOfficial = officialLangs.includes(langB) ? 0 : 1;
      if (aOfficial !== bOfficial) {
        return aOfficial - bOfficial;
      }
      return langA.localeCompare(langB);
    });
  }, [documents, officialLangs]);

  return (
    <div className="flex items-start gap-3 p-3 bg-surface-page rounded-lg">
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2 flex-wrap">
          <span className="font-medium text-sm text-text-primary">
            {currency} {period.rentAmount.toFixed(2)}
          </span>
          {period.percentageChange != null && (
            <PercentageChangeBadge change={period.percentageChange} />
          )}
        </div>
        <p className="text-xs text-text-muted mt-0.5">
          {formatDate(period.effectiveFrom)}
          {period.effectiveTo
            ? ` - ${formatDate(period.effectiveTo)}`
            : ' - Present'}
        </p>
        {period.notes && (
          <div className="mt-1 text-xs text-text-secondary">
            <RichTextDisplay content={period.notes} />
          </div>
        )}

        {/* Generated documents — compact language badges */}
        {hasDocuments ? (
          <div className="mt-2 flex items-center gap-2 flex-wrap">
            <FileText className="h-3.5 w-3.5 text-primary-500 shrink-0" />
            <span className="text-[11px] font-medium text-text-secondary shrink-0">
              Rent Change
            </span>
            <div className="flex items-center gap-1 flex-wrap">
              {sortedDocs.map((doc) => {
                const lang = extractLang(doc.fileName);
                const isOfficial = officialLangs.includes(lang);
                return (
                  <button
                    key={doc.identifier}
                    type="button"
                    onClick={() => setPreviewIndex(documents.indexOf(doc))}
                    className={`inline-flex items-center gap-0.5 px-1.5 py-0.5 rounded font-mono text-[11px] font-medium uppercase transition-colors cursor-pointer ${
                      isOfficial
                        ? 'bg-amber-500/10 text-amber-700 dark:text-amber-400 border border-amber-400/40 hover:bg-amber-500/20'
                        : 'bg-surface-inset text-text-secondary border border-transparent hover:bg-border-default hover:text-text-primary'
                    }`}
                    title={`${doc.title ?? doc.fileName}${isOfficial ? ' (official)' : ''}`}
                  >
                    {lang}
                    {isOfficial && <Shield className="h-2.5 w-2.5" />}
                  </button>
                );
              })}
            </div>
            {canEdit && (
              <button
                onClick={onRegenerateDocuments}
                className="flex items-center gap-1 px-2 py-1 text-[11px] font-medium text-text-muted rounded hover:text-primary-500 hover:bg-primary-500/5 transition-colors ml-auto"
              >
                <RefreshCw className="h-3 w-3" />
                Regenerate
              </button>
            )}
          </div>
        ) : canEdit ? (
          <div className="mt-2">
            <button
              onClick={onGenerateDocuments}
              className="flex items-center gap-1 px-2 py-1 text-[11px] font-medium text-primary-500 bg-primary-500/10 rounded hover:bg-primary-500/20 transition-colors"
              title="Generate rent change document"
            >
              <FolderDown className="h-3 w-3" />
              Generate Document
            </button>
          </div>
        ) : null}

        {/* Document preview modal */}
        {previewIndex !== null && documents[previewIndex] && (
          <DocumentPreviewModal
            document={documents[previewIndex]}
            onClose={() => setPreviewIndex(null)}
            onPrevious={
              previewIndex > 0
                ? () => setPreviewIndex(previewIndex - 1)
                : undefined
            }
            onNext={
              previewIndex < documents.length - 1
                ? () => setPreviewIndex(previewIndex + 1)
                : undefined
            }
            currentIndex={previewIndex}
            totalCount={documents.length}
          />
        )}
      </div>
      {onDelete && (
        <button
          onClick={onDelete}
          className="p-1 text-text-muted hover:text-error-text transition-colors shrink-0"
          title="Delete rent period"
        >
          <Trash2 className="h-3.5 w-3.5" />
        </button>
      )}
    </div>
  );
}

function PercentageChangeBadge({ change }: { change: number }) {
  if (change > 0) {
    return (
      <span className="inline-flex items-center gap-0.5 px-1.5 py-0.5 text-xs font-medium rounded-full bg-success-bg text-success-text">
        <TrendingUp className="h-3 w-3" />+{change.toFixed(1)}%
      </span>
    );
  }
  if (change < 0) {
    return (
      <span className="inline-flex items-center gap-0.5 px-1.5 py-0.5 text-xs font-medium rounded-full bg-error-bg text-error-text">
        <TrendingDown className="h-3 w-3" />
        {change.toFixed(1)}%
      </span>
    );
  }
  return null;
}
