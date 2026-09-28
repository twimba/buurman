import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { CommunicationsTimeline } from '@/components/communications/CommunicationsTimeline';
import {
  useContractCommunications,
  useResendCommunication,
} from '@/hooks/useCommunications';
import { useTranslation } from 'react-i18next';
import { useTabState } from '@/hooks/useTabState';
import {
  useContract,
  useDeleteContract,
  useChangeContractStatus,
  useReopenContract,
  useDuplicateContract,
} from '@/hooks/useContractHooks';
import {
  exportContractBooklet,
  getContractSummary,
} from '@/generated/api/booklets/booklets';
import { downloadBlob } from '@/utils/downloadBlob';
import { DocumentDownloadMenu } from '@/components/common/DocumentDownloadMenu';
import { ErrorMessage } from '@/components/ErrorMessage';
import { Button, PageHeader, Skeleton } from '@buurman/ui';
import { ContractStatusBadge } from '@/components/contracts/ContractStatusBadge';
import { ChangeContractStatusModal } from '@/components/contracts/ChangeContractStatusModal';
import { ContractOverviewTab } from '@/components/contracts/ContractOverviewTab';
import { ContractPaymentsTab } from '@/components/contracts/ContractPaymentsTab';
import { ContractExtensionsTab } from '@/components/contracts/ContractExtensionsTab';
import { ContractDocumentsTab } from '@/components/contracts/ContractDocumentsTab';
import { ContractHistoryTab } from '@/components/contracts/ContractHistoryTab';
import { useTeam } from '@/context/TeamContext';
import { trackEvent } from '@/utils/analytics';
import { AnalyticsEvent } from '@/constants/analyticsEvents';
import {
  Edit,
  Trash2,
  FileText,
  DollarSign,
  RefreshCw,
  RotateCcw,
  Copy,
  Repeat,
} from 'lucide-react';
import { ChangeContractStatusRequest, ContractStatus } from '@/types/contract';

export const ContractDetailPage = () => {
  const { t } = useTranslation('contracts');
  const { id = '' } = useParams<{ id: string }>();
  const {
    data: communications = [],
    isLoading: communicationsLoading,
    isError: communicationsError,
  } = useContractCommunications(id);
  const resendCommunicationMutation = useResendCommunication([
    'contracts',
    id,
    'communications',
  ]);
  const navigate = useNavigate();
  const { canEditData } = useTeam();
  const [activeTab, setActiveTab] = useTabState('overview', [
    'overview',
    'payments',
    'extensions',
    'documents',
    'history',
  ] as const);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [showStatusModal, setShowStatusModal] = useState(false);

  const { data: contract, isLoading, error } = useContract(id);
  const contractIdentifier = contract?.identifier;

  useEffect(() => {
    if (contractIdentifier) {
      trackEvent(AnalyticsEvent.CONTRACT_VIEWED);
    }
  }, [contractIdentifier]);

  const deleteContractMutation = useDeleteContract();
  const changeStatusMutation = useChangeContractStatus(id);
  const reopenContractMutation = useReopenContract(id);
  const duplicateContractMutation = useDuplicateContract();

  const handleDelete = async () => {
    if (!id) {
      return;
    }
    try {
      await deleteContractMutation.mutateAsync(id);
      navigate('/contracts');
    } catch (err) {
      console.error('Failed to delete contract:', err);
    }
  };

  const handleChangeStatus = async (
    newStatus: ContractStatus,
    reason?: string
  ) => {
    const request: ChangeContractStatusRequest = {
      status: newStatus,
      reason,
    };
    try {
      await changeStatusMutation.mutateAsync(request);
      setShowStatusModal(false);
    } catch (err) {
      console.error('Failed to change contract status:', err);
    }
  };

  const handleReopen = async () => {
    try {
      await reopenContractMutation.mutateAsync();
    } catch (err) {
      console.error('Failed to reopen contract:', err);
    }
  };

  const handleDuplicate = async () => {
    if (!id) {
      return;
    }
    try {
      const newContract = await duplicateContractMutation.mutateAsync(id);
      navigate(`/contracts/${newContract.identifier}`);
    } catch (err) {
      console.error('Failed to duplicate contract:', err);
    }
  };

  const handleDownloadBooklet = async (lang: string) => {
    downloadBlob(
      await exportContractBooklet(id, { lang } as Parameters<
        typeof exportContractBooklet
      >[1]),
      `contract-booklet-${lang}.pdf`
    );
  };

  const handleDownloadSummary = async (lang: string) => {
    downloadBlob(
      await getContractSummary(id, { lang } as Parameters<
        typeof getContractSummary
      >[1]),
      `contract-${id}-summary-${lang}.pdf`
    );
  };

  if (isLoading) {
    return (
      <div className="min-h-full bg-background">
        <div className="px-4 py-8 space-y-6">
          {/* Header skeleton */}
          <div className="flex items-center gap-4">
            <Skeleton className="h-8 w-56" />
            <Skeleton className="h-6 w-20 rounded-full" />
          </div>
          <Skeleton className="h-4 w-32" />
          {/* Action buttons skeleton */}
          <div className="flex gap-2">
            {Array.from({ length: 5 }).map((_, i) => (
              <Skeleton key={i} className="h-10 w-28 rounded" />
            ))}
          </div>
          {/* Tab bar skeleton */}
          <div className="flex gap-6 border-b border-border-default pb-3">
            {Array.from({ length: 5 }).map((_, i) => (
              <Skeleton key={i} className="h-5 w-20" />
            ))}
          </div>
          {/* Tab content skeleton */}
          <div className="space-y-4">
            <Skeleton className="h-64 w-full rounded-lg" />
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              <Skeleton className="h-40 w-full rounded-lg" />
              <Skeleton className="h-40 w-full rounded-lg" />
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (error || !contract) {
    return (
      <div className="min-h-full bg-background p-8">
        <ErrorMessage message={t('detail.notFound')} />
      </div>
    );
  }

  const isLocked =
    contract.status === ContractStatus.ACTIVE ||
    contract.status === ContractStatus.TERMINATED ||
    contract.status === ContractStatus.EXPIRED;
  const canDelete = !isLocked;
  const canEdit = !isLocked;
  const canReopen =
    contract.status === ContractStatus.TERMINATED ||
    contract.status === ContractStatus.EXPIRED;

  return (
    <div className="min-h-full bg-background">
      <div className="px-4 py-8">
        {/* Header */}
        <PageHeader
          title={t('detail.title', { id: contract.identifier })}
          subtitle={t(`detail.contractTypes.${contract.contractType}`)}
          backTo="/contracts"
          badge={<ContractStatusBadge status={contract.status} />}
          actions={
            <>
              <DocumentDownloadMenu
                onDownloadBooklet={handleDownloadBooklet}
                onDownloadSummary={handleDownloadSummary}
              />
              {!canReopen && (
                <Button
                  variant="primary"
                  leftIcon={<RefreshCw />}
                  onClick={() => setShowStatusModal(true)}
                  disabled={!canEditData}
                >
                  {t('detail.changeStatus')}
                </Button>
              )}
              {canReopen && (
                <Button
                  variant="primary"
                  leftIcon={<RotateCcw />}
                  onClick={handleReopen}
                  isLoading={reopenContractMutation.isPending}
                  title={t('detail.reopenTitle')}
                  disabled={!canEditData}
                >
                  {t('detail.reopen')}
                </Button>
              )}
              {canEdit && (
                <Button
                  variant="secondary"
                  leftIcon={<Edit />}
                  onClick={() => navigate(`/contracts/${id}/edit`)}
                  disabled={!canEditData}
                >
                  {t('detail.edit')}
                </Button>
              )}
              <Button
                variant="secondary"
                leftIcon={<Copy />}
                onClick={handleDuplicate}
                isLoading={duplicateContractMutation.isPending}
                title={t('detail.duplicateTitle')}
                disabled={!canEditData}
              >
                {t('detail.duplicate')}
              </Button>
              {canDelete && (
                <Button
                  variant="danger"
                  leftIcon={<Trash2 />}
                  onClick={() => setShowDeleteModal(true)}
                  disabled={!canEditData}
                >
                  {t('detail.delete')}
                </Button>
              )}
            </>
          }
        />

        {/* Tabs */}
        <div className="border-b border-border-default mb-6">
          <div className="flex gap-6">
            <button
              onClick={() => setActiveTab('overview')}
              className={`pb-3 px-1 font-medium transition-colors ${
                activeTab === 'overview'
                  ? 'border-b-2 border-primary-500 text-primary-500'
                  : 'text-text-secondary hover:text-text-primary'
              }`}
            >
              {t('detail.tabs.overview')}
            </button>
            <button
              onClick={() => setActiveTab('payments')}
              className={`pb-3 px-1 font-medium transition-colors flex items-center gap-2 ${
                activeTab === 'payments'
                  ? 'border-b-2 border-primary-500 text-primary-500'
                  : 'text-text-secondary hover:text-text-primary'
              }`}
            >
              <DollarSign className="h-4 w-4" />
              {t('detail.tabs.payments')}
            </button>
            <button
              onClick={() => setActiveTab('extensions')}
              className={`pb-3 px-1 font-medium transition-colors flex items-center gap-2 ${
                activeTab === 'extensions'
                  ? 'border-b-2 border-primary-500 text-primary-500'
                  : 'text-text-secondary hover:text-text-primary'
              }`}
            >
              <Repeat className="h-4 w-4" />
              {t('detail.tabs.extensions')}
              {contract.extensionCount != null &&
                contract.extensionCount > 0 &&
                ` (${contract.extensionCount})`}
            </button>
            <button
              onClick={() => setActiveTab('documents')}
              className={`pb-3 px-1 font-medium transition-colors flex items-center gap-2 ${
                activeTab === 'documents'
                  ? 'border-b-2 border-primary-500 text-primary-500'
                  : 'text-text-secondary hover:text-text-primary'
              }`}
            >
              <FileText className="h-4 w-4" />
              {t('detail.tabs.documents')}
            </button>
            <button
              onClick={() => setActiveTab('history')}
              className={`pb-3 px-1 font-medium transition-colors ${
                activeTab === 'history'
                  ? 'border-b-2 border-primary-500 text-primary-500'
                  : 'text-text-secondary hover:text-text-primary'
              }`}
            >
              {t('detail.tabs.history')}
            </button>
          </div>
        </div>

        {/* Tab Content */}
        {activeTab === 'overview' && (
          <>
            <ContractOverviewTab contract={contract} contractId={id} />
            <section className="mt-6 bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
              <h2 className="text-xl font-semibold text-text-primary mb-4">
                {t('common:communications.title')}
              </h2>
              <CommunicationsTimeline
                communications={communications}
                isLoading={communicationsLoading}
                isError={communicationsError}
                resendingIdentifier={
                  resendCommunicationMutation.isPending
                    ? resendCommunicationMutation.variables
                    : undefined
                }
                onResend={
                  canEditData
                    ? (communicationId) =>
                        resendCommunicationMutation.mutate(communicationId)
                    : undefined
                }
              />
            </section>
          </>
        )}

        {activeTab === 'payments' && (
          <ContractPaymentsTab
            contractId={id}
            contractStatus={contract.status}
          />
        )}

        {activeTab === 'extensions' && (
          <ContractExtensionsTab contract={contract} contractId={id} />
        )}

        {activeTab === 'documents' && <ContractDocumentsTab contractId={id} />}

        {activeTab === 'history' && <ContractHistoryTab contractId={id} />}
      </div>

      {/* Delete Confirmation Modal */}
      {showDeleteModal && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50">
          <div className="bg-surface-card rounded-lg shadow-xl max-w-md w-full mx-4 p-6">
            <h2 className="text-lg font-semibold text-text-primary mb-4">
              {t('detail.deleteTitle')}
            </h2>
            <p className="text-text-secondary mb-6">
              {t('detail.deleteMessage')}
            </p>
            <div className="flex justify-end gap-3">
              <Button
                variant="secondary"
                onClick={() => setShowDeleteModal(false)}
              >
                Cancel
              </Button>
              <Button
                variant="danger"
                onClick={handleDelete}
                isLoading={deleteContractMutation.isPending}
              >
                Delete
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Status Change Modal */}
      {showStatusModal && (
        <ChangeContractStatusModal
          currentStatus={contract.status}
          onClose={() => setShowStatusModal(false)}
          onConfirm={handleChangeStatus}
          isLoading={changeStatusMutation.isPending}
        />
      )}
    </div>
  );
};
