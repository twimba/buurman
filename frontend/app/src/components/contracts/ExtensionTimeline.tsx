import { useState, useMemo, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import DOMPurify from 'dompurify';
import {
  Plus,
  Check,
  CheckCircle,
  XCircle,
  Ban,
  TrendingUp,
  TrendingDown,
  Zap,
  User,
  FileText,
  FileDown,
  FolderDown,
  RefreshCw,
  Shield,
  Trash2,
} from 'lucide-react';
import * as extensionsApi from '@/api/contractExtensions';
import {
  GenerateDocumentsModal,
  COUNTRY_OFFICIAL_LANGUAGES,
} from './GenerateDocumentsModal';
import { ExtensionStatusBadge } from './ExtensionStatusBadge';
import { CreateExtensionModal } from './CreateExtensionModal';
import { DocumentPreviewModal } from '@/components/documents/DocumentPreviewModal';
import { LoadingSpinner } from '@buurman/ui';
import { useFormatDate } from '@/hooks/useFormatDate';
import { useTeam } from '@/context/TeamContext';
import { useContractDocuments } from '@/hooks/useContractHooks';
import {
  useContractExtensions,
  useCreateExtension,
  useActivateExtension,
  useConfirmExtension,
  useDeclineExtension,
  useCancelExtension,
} from '@/hooks/useContractExtensionHooks';
import type {
  ContractExtensionResponse,
  RentAdjustmentType,
} from '@/types/contractExtension';
import type { ContractResponseStatus } from '@/generated/models';
import type { DocumentResponse } from '@/types/property';
import { useTranslation } from 'react-i18next';

interface ExtensionTimelineProps {
  contractIdentifier: string;
  contractStatus: ContractResponseStatus;
  currency: string;
  currentRentAmount: number;
  currentEndDate?: string;
  renewalTermMonths?: number;
  rentAdjustmentType?: RentAdjustmentType;
  rentAdjustmentValue?: number;
  documentLanguages?: string[];
  countryCode?: string;
}

export const ExtensionTimeline = ({
  contractIdentifier,
  contractStatus,
  currency,
  currentRentAmount,
  currentEndDate,
  renewalTermMonths,
  rentAdjustmentType,
  rentAdjustmentValue,
  documentLanguages,
  countryCode,
}: ExtensionTimelineProps) => {
  const { t } = useTranslation('contracts');
  const { canEditData } = useTeam();
  const { formatDate } = useFormatDate();
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [declineTarget, setDeclineTarget] = useState<string | null>(null);
  const [cancelTarget, setCancelTarget] = useState<string | null>(null);
  const [deleteDocsOnCancel, setDeleteDocsOnCancel] = useState(false);
  const [generateTarget, setGenerateTarget] = useState<{
    extension: ContractExtensionResponse;
    regenerate: boolean;
  } | null>(null);
  const [declineReason, setDeclineReason] = useState('');

  const { data: extensionsPage, isLoading } =
    useContractExtensions(contractIdentifier);
  const { data: allDocuments } = useContractDocuments(contractIdentifier);

  /** Map extension number -> documents matching that extension's filename pattern */
  const documentsByExtension = useMemo(() => {
    const map = new Map<number, DocumentResponse[]>();
    if (!allDocuments) {
      return map;
    }
    for (const doc of allDocuments) {
      const match = doc.fileName.match(/-(\d+)-[a-z]{2}\.pdf$/);
      if (match) {
        const extNum = parseInt(match[1], 10);
        const list = map.get(extNum) ?? [];
        list.push(doc);
        map.set(extNum, list);
      }
    }
    return map;
  }, [allDocuments]);
  const createExtension = useCreateExtension(contractIdentifier);
  const activateExtension = useActivateExtension(contractIdentifier);
  const confirmExtension = useConfirmExtension(contractIdentifier);
  const declineExtension = useDeclineExtension(contractIdentifier);
  const cancelExtension = useCancelExtension(contractIdentifier);

  const extensions = extensionsPage?.content ?? [];
  const canCreate = canEditData && contractStatus === 'ACTIVE';

  const [searchParams, setSearchParams] = useSearchParams();

  // Open the create-extension modal when arriving from an email deep link
  // (e.g. /contracts/:id?tab=extensions&action=renew), then strip the param.
  useEffect(() => {
    if (searchParams.get('action') !== 'renew' || !canCreate) {
      return;
    }
    // One-shot sync of an email deep link into UI state; the param is stripped immediately
    // below so this runs once (not a derived-state smell).
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setShowCreateModal(true);
    setSearchParams(
      (prev) => {
        prev.delete('action');
        return prev;
      },
      { replace: true }
    );
  }, [searchParams, setSearchParams, canCreate]);

  const downloadPdf = async (
    fetcher: () => Promise<Blob>,
    filename: string
  ) => {
    try {
      const blob = await fetcher();
      const url = window.URL.createObjectURL(
        new Blob([blob], { type: 'application/pdf' })
      );
      const link = document.createElement('a');
      link.href = url;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch {
      alert(t('extensions.downloadFailed'));
    }
  };

  const handleCreate = (
    request: Parameters<typeof createExtension.mutate>[0]
  ) => {
    createExtension.mutate(request, {
      onSuccess: () => setShowCreateModal(false),
    });
  };

  const handleActivate = (extensionId: string) => {
    activateExtension.mutate(extensionId);
  };

  const handleConfirm = (extensionId: string) => {
    confirmExtension.mutate(extensionId);
  };

  const handleDecline = () => {
    if (!declineTarget) {
      return;
    }
    declineExtension.mutate(
      {
        extensionId: declineTarget,
        request: { reason: declineReason || undefined },
      },
      {
        onSuccess: () => {
          setDeclineTarget(null);
          setDeclineReason('');
        },
      }
    );
  };

  const handleCancel = () => {
    if (!cancelTarget) {
      return;
    }
    cancelExtension.mutate(
      { extensionId: cancelTarget, deleteDocuments: deleteDocsOnCancel },
      {
        onSuccess: () => {
          setCancelTarget(null);
          setDeleteDocsOnCancel(false);
        },
      }
    );
  };

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h3 className="text-lg font-semibold text-text-primary">
          {t('extensions.title')}
          {extensions.length > 0 && (
            <span className="ml-2 text-sm font-normal text-text-secondary">
              ({extensions.length})
            </span>
          )}
        </h3>
        {canCreate && (
          <button
            onClick={() => setShowCreateModal(true)}
            className="flex items-center gap-1 px-3 py-1.5 text-xs font-medium text-primary-500 bg-primary-500/10 rounded-md hover:bg-primary-500/20 transition-colors"
          >
            <Plus className="h-3.5 w-3.5" />
            {t('extensions.newExtension')}
          </button>
        )}
      </div>

      {/* Content */}
      {isLoading ? (
        <div className="flex justify-center py-8">
          <LoadingSpinner />
        </div>
      ) : extensions.length === 0 ? (
        <div className="text-center py-8">
          <p className="text-sm text-text-secondary">{t('extensions.empty')}</p>
        </div>
      ) : (
        <div className="space-y-3">
          {extensions.map((ext) => (
            <ExtensionCard
              key={ext.identifier}
              extension={ext}
              documents={documentsByExtension.get(ext.extensionNumber) ?? []}
              countryCode={countryCode}
              formatDate={formatDate}
              canEdit={canEditData}
              onActivate={() => handleActivate(ext.identifier)}
              onConfirm={() => handleConfirm(ext.identifier)}
              onDecline={() => setDeclineTarget(ext.identifier)}
              onCancel={() => setCancelTarget(ext.identifier)}
              onDownloadAddendum={() =>
                downloadPdf(
                  () =>
                    extensionsApi.downloadAddendum(
                      contractIdentifier,
                      ext.identifier
                    ),
                  `addendum-${ext.extensionNumber}.pdf`
                )
              }
              onDownloadLetter={() =>
                downloadPdf(
                  () =>
                    extensionsApi.downloadRentIncreaseLetter(
                      contractIdentifier,
                      ext.identifier
                    ),
                  `rent-increase-letter-${ext.extensionNumber}.pdf`
                )
              }
              onGenerateDocuments={() =>
                setGenerateTarget({ extension: ext, regenerate: false })
              }
              onRegenerateDocuments={() =>
                setGenerateTarget({ extension: ext, regenerate: true })
              }
              isActivating={activateExtension.isPending}
              isConfirming={confirmExtension.isPending}
            />
          ))}
        </div>
      )}

      {/* Create Extension Modal */}
      {showCreateModal && (
        <CreateExtensionModal
          currentRentAmount={currentRentAmount}
          currency={currency}
          currentEndDate={currentEndDate}
          renewalTermMonths={renewalTermMonths}
          rentAdjustmentType={rentAdjustmentType}
          rentAdjustmentValue={rentAdjustmentValue}
          onClose={() => setShowCreateModal(false)}
          onConfirm={handleCreate}
          isLoading={createExtension.isPending}
        />
      )}

      {/* Decline Confirmation */}
      {declineTarget && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50">
          <div className="bg-surface-card rounded-lg shadow-xl dark:shadow-black/20 max-w-md w-full mx-4">
            <div className="p-4 border-b border-border-default">
              <h3 className="text-lg font-semibold text-text-primary">
                {t('extensions.declineTitle')}
              </h3>
            </div>
            <form
              onSubmit={(e) => {
                e.preventDefault();
                handleDecline();
              }}
              onKeyDown={(e) => {
                if (e.key === 'Escape') {
                  setDeclineTarget(null);
                  setDeclineReason('');
                }
              }}
            >
              <div className="p-4 space-y-3">
                <p className="text-sm text-text-secondary">
                  {t('detail.declineConfirmMessage')}
                </p>
                <div>
                  <label
                    htmlFor="declineReason"
                    className="block text-sm font-medium text-text-secondary mb-1"
                  >
                    {t('extensions.reasonOptional')}
                  </label>
                  <input
                    id="declineReason"
                    type="text"
                    value={declineReason}
                    onChange={(e) => setDeclineReason(e.target.value)}
                    className="w-full px-3 py-2 border border-border-strong rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500 bg-surface-card text-text-primary"
                    placeholder={t('extensions.reasonPlaceholder')}
                    autoFocus
                  />
                </div>
              </div>
              <div className="flex items-center justify-end gap-3 p-4 border-t border-border-default">
                <button
                  type="button"
                  onClick={() => {
                    setDeclineTarget(null);
                    setDeclineReason('');
                  }}
                  className="px-4 py-2 text-sm font-medium text-text-secondary bg-surface-card border border-border-strong rounded-md hover:bg-surface-inset"
                >
                  {t('common:buttons.cancel')}
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 text-sm font-medium text-white bg-error-bg-strong rounded-md hover:opacity-90 disabled:opacity-50"
                  disabled={declineExtension.isPending}
                >
                  {declineExtension.isPending
                    ? t('extensions.declining')
                    : t('extensions.decline')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Cancel Confirmation */}
      {cancelTarget && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50">
          <div className="bg-surface-card rounded-lg shadow-xl dark:shadow-black/20 max-w-md w-full mx-4">
            <div className="p-4 border-b border-border-default">
              <h3 className="text-lg font-semibold text-text-primary">
                {t('extensions.cancelTitle')}
              </h3>
            </div>
            <div className="p-4 space-y-3">
              <p className="text-sm text-text-secondary">
                {t('detail.cancelConfirmMessage')}
              </p>
              <label className="flex items-start gap-2 p-3 rounded-md border border-border-default hover:border-border-strong cursor-pointer transition-colors">
                <input
                  type="checkbox"
                  checked={deleteDocsOnCancel}
                  onChange={(e) => setDeleteDocsOnCancel(e.target.checked)}
                  className="mt-0.5 rounded border-border-strong text-primary-500 focus:ring-primary-500"
                />
                <div className="flex-1 min-w-0">
                  <span className="flex items-center gap-1.5 text-sm font-medium text-text-primary">
                    <Trash2 className="h-3.5 w-3.5 text-error-text" />
                    {t('extensions.deleteGeneratedDocs')}
                  </span>
                  <p className="text-xs text-text-muted mt-0.5">
                    {t('extensions.deleteGeneratedDocsHelp')}
                  </p>
                </div>
              </label>
            </div>
            <div className="flex items-center justify-end gap-3 p-4 border-t border-border-default">
              <button
                type="button"
                onClick={() => {
                  setCancelTarget(null);
                  setDeleteDocsOnCancel(false);
                }}
                className="px-4 py-2 text-sm font-medium text-text-secondary bg-surface-card border border-border-strong rounded-md hover:bg-surface-inset"
                disabled={cancelExtension.isPending}
              >
                {t('detail.keepExtension')}
              </button>
              <button
                type="button"
                onClick={handleCancel}
                className="px-4 py-2 text-sm font-medium text-white bg-error-bg-strong rounded-md hover:opacity-90 disabled:opacity-50"
                disabled={cancelExtension.isPending}
              >
                {cancelExtension.isPending
                  ? t('common:buttons.loading')
                  : t('detail.cancelExtension')}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Generate Documents Modal */}
      {generateTarget && (
        <GenerateDocumentsModal
          contractIdentifier={contractIdentifier}
          extensionIdentifier={generateTarget.extension.identifier}
          extensionNumber={generateTarget.extension.extensionNumber}
          defaultLanguages={documentLanguages ?? ['en']}
          countryCode={countryCode}
          regenerate={generateTarget.regenerate}
          onClose={() => setGenerateTarget(null)}
        />
      )}
    </div>
  );
};

/** Groups documents by type (addendum / letter) and extracts the language code from filename */
function groupDocsByType(documents: DocumentResponse[]) {
  const addendums: DocumentResponse[] = [];
  const letters: DocumentResponse[] = [];
  for (const doc of documents) {
    if (doc.fileName.includes('extension-addendum')) {
      addendums.push(doc);
    } else if (doc.fileName.includes('rent-increase-letter')) {
      letters.push(doc);
    }
  }
  return { addendums, letters };
}

function extractLang(fileName: string): string {
  const match = fileName.match(/-([a-z]{2})\.pdf$/);
  return match ? match[1] : '??';
}

function ExtensionCard({
  extension,
  documents,
  countryCode,
  formatDate,
  canEdit,
  onActivate,
  onConfirm,
  onDecline,
  onCancel,
  onDownloadAddendum,
  onDownloadLetter,
  onGenerateDocuments,
  onRegenerateDocuments,
  isActivating,
  isConfirming,
}: {
  extension: ContractExtensionResponse;
  documents: DocumentResponse[];
  countryCode?: string;
  formatDate: (date: string) => string;
  canEdit: boolean;
  onActivate: () => void;
  onConfirm: () => void;
  onDecline: () => void;
  onCancel: () => void;
  onDownloadAddendum: () => void;
  onDownloadLetter: () => void;
  onGenerateDocuments: () => void;
  onRegenerateDocuments: () => void;
  isActivating: boolean;
  isConfirming: boolean;
}) {
  const { t } = useTranslation('contracts');
  const rentChange =
    extension.previousRentAmount > 0
      ? ((extension.newRentAmount - extension.previousRentAmount) /
          extension.previousRentAmount) *
        100
      : 0;

  const isDraft = extension.status === 'DRAFT';
  const hasDocuments = documents.length > 0;
  const { addendums, letters } = groupDocsByType(documents);
  const officialLangs = countryCode
    ? (COUNTRY_OFFICIAL_LANGUAGES[countryCode.toUpperCase()] ?? [])
    : [];
  const [previewIndex, setPreviewIndex] = useState<number | null>(null);

  return (
    <div className="border border-border-default rounded-lg p-4 bg-surface-card">
      <div className="flex items-start justify-between gap-3">
        {/* Left: Info */}
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 flex-wrap mb-2">
            <span className="text-sm font-semibold text-text-primary">
              {t('extensions.extensionNumber', {
                number: extension.extensionNumber,
              })}
            </span>
            <ExtensionStatusBadge status={extension.status} />
            {extension.triggerType === 'AUTO' ? (
              <span className="inline-flex items-center gap-0.5 px-1.5 py-0.5 text-[10px] font-medium rounded-full bg-info-bg text-info-text">
                <Zap className="h-2.5 w-2.5" />
                {t('extensions.auto')}
              </span>
            ) : (
              <span className="inline-flex items-center gap-0.5 px-1.5 py-0.5 text-[10px] font-medium rounded-full bg-surface-inset text-text-secondary">
                <User className="h-2.5 w-2.5" />
                {t('extensions.manual')}
              </span>
            )}
          </div>

          {/* Date range */}
          <div className="text-sm text-text-secondary mb-1">
            {formatDate(extension.previousEndDate)}
            {extension.newEndDate && (
              <span className="text-text-primary font-medium">
                {' '}
                &rarr; {formatDate(extension.newEndDate)}
              </span>
            )}
          </div>

          {/* Rent change */}
          <div className="flex items-center gap-2 text-sm">
            <span className="text-text-secondary">
              {extension.previousRentCurrency}{' '}
              {extension.previousRentAmount.toFixed(2)}
            </span>
            <span className="text-text-primary font-medium">
              &rarr; {extension.newRentCurrency}{' '}
              {extension.newRentAmount.toFixed(2)}
            </span>
            {rentChange !== 0 && <RentChangeBadge change={rentChange} />}
          </div>

          {/* Notes */}
          {extension.notes && (
            <div
              className="text-xs text-text-muted mt-2 prose prose-xs dark:prose-invert max-w-none"
              dangerouslySetInnerHTML={{
                __html: DOMPurify.sanitize(extension.notes),
              }}
            />
          )}

          {/* Declined reason */}
          {extension.declinedReason && (
            <p className="text-xs text-error-text mt-1">
              {t('extensions.declined', { reason: extension.declinedReason })}
            </p>
          )}

          {/* Timestamps */}
          <p className="text-[11px] text-text-muted mt-2">
            {t('extensions.created', { date: formatDate(extension.createdAt) })}
            {extension.activatedAt &&
              ` \u00b7 ${t('extensions.activated', { date: formatDate(extension.activatedAt) })}`}
            {extension.confirmedAt &&
              ` \u00b7 ${t('extensions.confirmed', { date: formatDate(extension.confirmedAt) })}`}
          </p>

          {/* Generated documents — compact language badges per type */}
          {hasDocuments ? (
            <div className="mt-3 pt-3 border-t border-border-default/60 flex items-center gap-4 flex-wrap">
              {addendums.length > 0 && (
                <DocTypeLangRow
                  icon={FileText}
                  label={t('extensions.addendum')}
                  accent="text-primary-500"
                  docs={addendums}
                  officialLangs={officialLangs}
                  onClickDoc={(doc) => setPreviewIndex(documents.indexOf(doc))}
                />
              )}
              {letters.length > 0 && (
                <DocTypeLangRow
                  icon={FileDown}
                  label={t('extensions.rentLetter')}
                  accent="text-amber-600 dark:text-amber-400"
                  docs={letters}
                  officialLangs={officialLangs}
                  onClickDoc={(doc) => setPreviewIndex(documents.indexOf(doc))}
                />
              )}
              <button
                onClick={onRegenerateDocuments}
                className="flex items-center gap-1 px-2 py-1 text-[11px] font-medium text-text-muted rounded hover:text-primary-500 hover:bg-primary-500/5 transition-colors ml-auto"
              >
                <RefreshCw className="h-3 w-3" />
                {t('extensions.regenerate')}
              </button>
            </div>
          ) : (
            <div className="flex items-center gap-2 mt-2">
              <button
                onClick={onDownloadAddendum}
                className="flex items-center gap-1 px-2 py-1 text-[11px] font-medium text-text-secondary bg-surface-inset rounded hover:bg-border-default transition-colors"
                title={t('extensions.addendum')}
              >
                <FileText className="h-3 w-3" />
                {t('extensions.addendum')}
              </button>
              <button
                onClick={onDownloadLetter}
                className="flex items-center gap-1 px-2 py-1 text-[11px] font-medium text-text-secondary bg-surface-inset rounded hover:bg-border-default transition-colors"
                title={t('extensions.rentLetter')}
              >
                <FileDown className="h-3 w-3" />
                {t('extensions.rentLetter')}
              </button>
              <button
                onClick={onGenerateDocuments}
                className="flex items-center gap-1 px-2 py-1 text-[11px] font-medium text-primary-500 bg-primary-500/10 rounded hover:bg-primary-500/20 transition-colors"
                title={t('extensions.generateAndSave')}
              >
                <FolderDown className="h-3 w-3" />
                {t('extensions.generateAndSave')}
              </button>
            </div>
          )}

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

        {/* Right: Actions */}
        {canEdit && isDraft && (
          <div className="flex items-center gap-1.5 shrink-0">
            <button
              onClick={onActivate}
              disabled={isActivating}
              className="flex items-center gap-1 px-2.5 py-1.5 text-xs font-medium text-success-text bg-success-bg rounded-md hover:opacity-80 transition-colors disabled:opacity-50"
              title={t('extensions.activate')}
            >
              <Check className="h-3.5 w-3.5" />
              {t('extensions.activate')}
            </button>
            {!extension.confirmedAt && (
              <button
                onClick={onConfirm}
                disabled={isConfirming}
                className="flex items-center gap-1 px-2.5 py-1.5 text-xs font-medium text-indigo-700 bg-indigo-50 rounded-md hover:opacity-80 transition-colors disabled:opacity-50 dark:text-indigo-300 dark:bg-indigo-500/10"
                title={t('extensions.confirm')}
              >
                <CheckCircle className="h-3.5 w-3.5" />
                {t('extensions.confirm')}
              </button>
            )}
            <button
              onClick={onDecline}
              className="flex items-center gap-1 px-2.5 py-1.5 text-xs font-medium text-warning-text bg-warning-bg rounded-md hover:opacity-80 transition-colors"
              title={t('extensions.decline')}
            >
              <XCircle className="h-3.5 w-3.5" />
              {t('extensions.decline')}
            </button>
            <button
              onClick={onCancel}
              className="flex items-center gap-1 px-2.5 py-1.5 text-xs font-medium text-error-text bg-error-bg rounded-md hover:opacity-80 transition-colors"
              title={t('extensions.cancel')}
            >
              <Ban className="h-3.5 w-3.5" />
              {t('extensions.cancel')}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

function DocTypeLangRow({
  icon: Icon,
  label,
  accent,
  docs,
  officialLangs,
  onClickDoc,
}: {
  icon: React.ComponentType<{ className?: string }>;
  label: string;
  accent: string;
  docs: DocumentResponse[];
  officialLangs: string[];
  onClickDoc: (doc: DocumentResponse) => void;
}) {
  // Sort docs so official languages come first, then alphabetically
  const sorted = [...docs].sort((a, b) => {
    const langA = extractLang(a.fileName);
    const langB = extractLang(b.fileName);
    const aOfficial = officialLangs.includes(langA) ? 0 : 1;
    const bOfficial = officialLangs.includes(langB) ? 0 : 1;
    if (aOfficial !== bOfficial) {
      return aOfficial - bOfficial;
    }
    return langA.localeCompare(langB);
  });

  return (
    <div className="flex items-center gap-2 text-[11px]">
      <Icon className={`h-3.5 w-3.5 ${accent} shrink-0`} />
      <span className="font-medium text-text-secondary shrink-0">{label}</span>
      <div className="flex items-center gap-1 flex-wrap">
        {sorted.map((doc) => {
          const lang = extractLang(doc.fileName);
          const isOfficial = officialLangs.includes(lang);
          return (
            <button
              key={doc.identifier}
              type="button"
              onClick={() => onClickDoc(doc)}
              className={`inline-flex items-center gap-0.5 px-1.5 py-0.5 rounded font-mono font-medium uppercase transition-colors cursor-pointer ${
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
    </div>
  );
}

function RentChangeBadge({ change }: { change: number }) {
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
