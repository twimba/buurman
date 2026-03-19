import { useState, useMemo, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTabState } from '@/hooks/useTabState';
import {
  useContract,
  useDeleteContract,
  useContractAuditLog,
  useContractDocuments,
  useUploadContractDocument,
  useDeleteContractDocument,
  useChangeContractStatus,
  useReopenContract,
  useDuplicateContract,
  useGenerateContractPayments,
} from '@/hooks/useContractHooks';
import {
  usePaymentsByContract,
  useDeletePayment,
} from '@/hooks/usePaymentHooks';
import { useContractExtensions } from '@/hooks/useContractExtensionHooks';
import { ConfirmDialog } from '@buurman/ui';
import { ContractPaymentInstructionSection } from '@/components/contracts/ContractPaymentInstructionSection';
import { CalendarFeedResponseFeedType as CalendarFeedType } from '@/generated/models';
import { CalendarFeedButton } from '@/components/common/CalendarFeedPopover';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { ErrorMessage } from '@/components/ErrorMessage';
import { RichTextDisplay } from '@/components/common/RichTextDisplay';
import { formatAuditValue } from '@/utils/formatAuditValue';
import { DocumentList } from '@/components/properties/DocumentList';
import { ContractStatusBadge } from '@/components/contracts/ContractStatusBadge';
import { ChangeContractStatusModal } from '@/components/contracts/ChangeContractStatusModal';
import GeneratePaymentsModal from '@/components/contracts/GeneratePaymentsModal';
import { RentTimeline } from '@/components/contracts/RentTimeline';
import { ExtensionTimeline } from '@/components/contracts/ExtensionTimeline';
import { Button, PageHeader } from '@buurman/ui';
import { useTeam } from '@/context/TeamContext';
import { trackEvent } from '@/utils/analytics';
import { AnalyticsEvent } from '@/constants/analyticsEvents';
import client from '@/api/client';
import {
  Edit,
  Trash2,
  FileText,
  Calendar,
  DollarSign,
  Home,
  User,
  ChevronDown,
  ChevronUp,
  RefreshCw,
  RotateCcw,
  Copy,
  Search,
  History,
  Plus,
  Download,
  Eye,
  Repeat,
} from 'lucide-react';
import { formatDistanceToNow } from 'date-fns';
import { useFormatDate } from '@/hooks/useFormatDate';
import {
  ChangeContractStatusRequest,
  ContractStatus,
  PARTY_ROLE_LABELS,
  ContractPartyRole,
} from '@/types/contract';
import { PaymentStatusBadge } from '@/components/payments/PaymentStatusBadge';
import { PaymentStatus } from '@/types/payment';
import CountryMetadataForm, {
  useCountryName,
} from '@/components/contracts/CountryMetadataForm';

export const ContractDetailPage = () => {
  const { id = '' } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { canEditData } = useTeam();
  const { formatDate } = useFormatDate();
  const [activeTab, setActiveTab] = useTabState('overview', [
    'overview',
    'payments',
    'extensions',
    'documents',
    'history',
  ] as const);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [showStatusModal, setShowStatusModal] = useState(false);
  const [showGeneratePaymentsModal, setShowGeneratePaymentsModal] =
    useState(false);
  const [deletePaymentTarget, setDeletePaymentTarget] = useState<string | null>(
    null
  );
  const deletePaymentMutation = useDeletePayment();
  const [expandedAuditItems, setExpandedAuditItems] = useState<Set<string>>(
    new Set()
  );
  const [isMetadataExpanded, setIsMetadataExpanded] = useState(false);

  // Payments table state
  const [paymentsSearchTerm, setPaymentsSearchTerm] = useState('');
  const [paymentsSortField, setPaymentsSortField] = useState<
    'dueDate' | 'amount' | 'status' | 'paymentDate'
  >('dueDate');
  const [paymentsSortOrder, setPaymentsSortOrder] = useState<'asc' | 'desc'>(
    'asc'
  );
  const [paymentsCurrentPage, setPaymentsCurrentPage] = useState(1);
  const paymentsPerPage = 10;

  const { data: contract, isLoading, error } = useContract(id);
  const contractIdentifier = contract?.identifier;

  useEffect(() => {
    if (contractIdentifier) {
      trackEvent(AnalyticsEvent.CONTRACT_VIEWED);
    }
  }, [contractIdentifier]);
  const countryName = useCountryName(contract?.countryCode);
  const {
    data: auditLog = [],
    isLoading: auditLoading,
    error: auditError,
  } = useContractAuditLog(id);
  const {
    data: documents = [],
    isLoading: docsLoading,
    error: docsError,
  } = useContractDocuments(id);
  const {
    data: payments = [],
    isLoading: paymentsLoading,
    error: paymentsError,
  } = usePaymentsByContract(id);

  const { data: extensionsPage } = useContractExtensions(id);
  const activeExtensions = useMemo(
    () =>
      (extensionsPage?.content ?? [])
        .filter((e) => e.status === 'ACTIVE' || e.status === 'SUPERSEDED')
        .sort((a, b) => a.extensionNumber - b.extensionNumber),
    [extensionsPage]
  );

  const deleteContractMutation = useDeleteContract();
  const uploadDocumentMutation = useUploadContractDocument(id);
  const deleteDocumentMutation = useDeleteContractDocument(id);
  const changeStatusMutation = useChangeContractStatus(id);
  const reopenContractMutation = useReopenContract(id);
  const duplicateContractMutation = useDuplicateContract();
  const generatePaymentsMutation = useGenerateContractPayments(id);

  // Payments filtering, sorting, and pagination
  const filteredAndSortedPayments = useMemo(() => {
    if (!payments) {
      return [];
    }

    let filtered = [...payments];

    // Apply search filter
    if (paymentsSearchTerm) {
      const search = paymentsSearchTerm.toLowerCase();
      filtered = filtered.filter(
        (payment) =>
          payment.identifier.toLowerCase().includes(search) ||
          payment.status.toLowerCase().includes(search)
      );
    }

    // Apply sorting
    filtered.sort((a, b) => {
      let aVal: string | number, bVal: string | number;

      switch (paymentsSortField) {
        case 'dueDate':
          aVal = new Date(a.dueDate).getTime();
          bVal = new Date(b.dueDate).getTime();
          break;
        case 'amount':
          aVal = a.amount;
          bVal = b.amount;
          break;
        case 'status':
          aVal = a.status;
          bVal = b.status;
          break;
        case 'paymentDate':
          aVal = a.paymentDate ? new Date(a.paymentDate).getTime() : 0;
          bVal = b.paymentDate ? new Date(b.paymentDate).getTime() : 0;
          break;
        default:
          return 0;
      }

      if (aVal < bVal) {
        return paymentsSortOrder === 'asc' ? -1 : 1;
      }
      if (aVal > bVal) {
        return paymentsSortOrder === 'asc' ? 1 : -1;
      }
      return 0;
    });

    return filtered;
  }, [payments, paymentsSearchTerm, paymentsSortField, paymentsSortOrder]);

  // Paginated payments
  const paginatedPayments = useMemo(() => {
    const startIndex = (paymentsCurrentPage - 1) * paymentsPerPage;
    const endIndex = startIndex + paymentsPerPage;
    return filteredAndSortedPayments.slice(startIndex, endIndex);
  }, [filteredAndSortedPayments, paymentsCurrentPage]);

  const paymentsTotalPages = Math.ceil(
    filteredAndSortedPayments.length / paymentsPerPage
  );

  const handlePaymentsSort = (field: typeof paymentsSortField) => {
    if (paymentsSortField === field) {
      setPaymentsSortOrder(paymentsSortOrder === 'asc' ? 'desc' : 'asc');
    } else {
      setPaymentsSortField(field);
      setPaymentsSortOrder('asc');
    }
  };

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

  const handleUploadDocument = async (
    file: File,
    title?: string,
    notes?: string
  ) => {
    await uploadDocumentMutation.mutateAsync({ file, title, notes });
  };

  const handleDeleteDocument = async (documentId: string) => {
    await deleteDocumentMutation.mutateAsync(documentId);
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

  if (isLoading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <LoadingSpinner />
      </div>
    );
  }

  if (error || !contract) {
    return (
      <div className="min-h-screen bg-background p-8">
        <ErrorMessage message="Failed to load contract" />
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
    <div className="min-h-screen bg-background">
      <div className="px-4 py-8">
        {/* Header */}
        <PageHeader
          title={`Contract #${contract.identifier}`}
          subtitle={contract.contractType.replace('_', '')}
          backTo="/contracts"
          badge={<ContractStatusBadge status={contract.status} />}
          actions={
            <>
              <Button
                variant="secondary"
                leftIcon={<Download />}
                onClick={async () => {
                  try {
                    const response = await client.get(
                      `/booklets/contract/${id}`,
                      {
                        responseType: 'blob',
                      }
                    );
                    const blob = new Blob([response.data], {
                      type: 'application/pdf',
                    });
                    const url = window.URL.createObjectURL(blob);
                    const link = document.createElement('a');
                    link.href = url;
                    link.download = 'contract-booklet.pdf';
                    document.body.appendChild(link);
                    link.click();
                    document.body.removeChild(link);
                    window.URL.revokeObjectURL(url);
                  } catch (error) {
                    console.error('Failed to download booklet:', error);
                    alert('Failed to download booklet. Please try again.');
                  }
                }}
              >
                Booklet
              </Button>
              {!canReopen && (
                <Button
                  variant="primary"
                  leftIcon={<RefreshCw />}
                  onClick={() => setShowStatusModal(true)}
                  disabled={!canEditData}
                >
                  Change Status
                </Button>
              )}
              {canReopen && (
                <Button
                  variant="primary"
                  leftIcon={<RotateCcw />}
                  onClick={handleReopen}
                  isLoading={reopenContractMutation.isPending}
                  title="Reopen this contract to draft status"
                  disabled={!canEditData}
                >
                  Re-open
                </Button>
              )}
              {canEdit && (
                <Button
                  variant="secondary"
                  leftIcon={<Edit />}
                  onClick={() => navigate(`/contracts/${id}/edit`)}
                  disabled={!canEditData}
                >
                  Edit
                </Button>
              )}
              <Button
                variant="secondary"
                leftIcon={<Copy />}
                onClick={handleDuplicate}
                isLoading={duplicateContractMutation.isPending}
                title="Create a copy of this contract in draft status"
                disabled={!canEditData}
              >
                Duplicate
              </Button>
              {canDelete && (
                <Button
                  variant="danger"
                  leftIcon={<Trash2 />}
                  onClick={() => setShowDeleteModal(true)}
                  disabled={!canEditData}
                >
                  Delete
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
              Overview
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
              Payments {payments.length > 0 && `(${payments.length})`}
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
              Extensions
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
              Documents {documents.length > 0 && `(${documents.length})`}
            </button>
            <button
              onClick={() => setActiveTab('history')}
              className={`pb-3 px-1 font-medium transition-colors ${
                activeTab === 'history'
                  ? 'border-b-2 border-primary-500 text-primary-500'
                  : 'text-text-secondary hover:text-text-primary'
              }`}
            >
              History {auditLog.length > 0 && `(${auditLog.length})`}
            </button>
          </div>
        </div>

        {/* Tab Content */}
        {activeTab === 'overview' && (
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Property and Tenant */}
            <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
              <h2 className="text-lg font-semibold text-text-primary mb-4">
                Contract Parties
              </h2>
              <div className="space-y-4">
                <div className="flex items-start gap-3">
                  <Home className="h-5 w-5 text-text-muted mt-1" />
                  <div className="flex-1">
                    <p className="text-sm text-text-secondary">Property</p>
                    <button
                      onClick={() =>
                        navigate(`/properties/${contract.property.identifier}`)
                      }
                      className="font-medium text-primary-500 hover:underline text-left"
                    >
                      {contract.property.street}, {contract.property.city}
                    </button>
                    <p className="text-xs text-text-secondary">
                      #{contract.property.identifier}
                    </p>
                  </div>
                </div>
                {/* Contract Parties */}
                {contract.parties?.map((party) => (
                  <div
                    key={party.identifier}
                    className="flex items-start gap-3"
                  >
                    <User className="h-5 w-5 text-text-muted mt-1" />
                    <div className="flex-1">
                      <p className="text-sm text-text-secondary">
                        {PARTY_ROLE_LABELS[party.role as ContractPartyRole] ??
                          party.role}
                      </p>
                      <button
                        onClick={() =>
                          navigate(`/tenants/${party.tenant.identifier}`)
                        }
                        className="font-medium text-primary-500 hover:underline text-left"
                      >
                        {party.tenant.firstName} {party.tenant.lastName}
                      </button>
                      <p className="text-xs text-text-secondary">
                        #{party.tenant.identifier}
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Contract Dates */}
            <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-lg font-semibold text-text-primary">
                  Important Dates
                </h2>
                {contract && (
                  <CalendarFeedButton
                    feedType={CalendarFeedType.CONTRACT}
                    entityIdentifier={contract.identifier}
                  />
                )}
              </div>
              <div className="space-y-4">
                <div className="flex items-center gap-3">
                  <Calendar className="h-5 w-5 text-text-muted" />
                  <div>
                    <p className="text-sm text-text-secondary">Start Date</p>
                    <p className="font-medium text-text-primary">
                      {formatDate(contract.startDate)}
                    </p>
                  </div>
                </div>
                {contract.signedDate && (
                  <div className="flex items-center gap-3">
                    <Calendar className="h-5 w-5 text-text-muted" />
                    <div>
                      <p className="text-sm text-text-secondary">Signed Date</p>
                      <p className="font-medium text-text-primary">
                        {formatDate(contract.signedDate)}
                      </p>
                    </div>
                  </div>
                )}
                {(contract.endDate || contract.effectiveEndDate) &&
                  activeExtensions.length === 0 && (
                    <div className="flex items-center gap-3">
                      <Calendar className="h-5 w-5 text-text-muted" />
                      <div>
                        <p className="text-sm text-text-secondary">End Date</p>
                        <p className="font-medium text-text-primary">
                          {formatDate(
                            (contract.endDate ?? contract.effectiveEndDate) as string
                          )}
                        </p>
                      </div>
                    </div>
                  )}
                {activeExtensions.length > 0 && contract.endDate && (
                  <div>
                    <div className="flex items-center gap-2 mb-3">
                      <Calendar className="h-5 w-5 text-text-muted" />
                      <p className="text-sm text-text-secondary">
                        End Date
                        <span className="ml-1.5 px-1.5 py-0.5 text-[10px] font-medium rounded-full bg-info-bg text-info-text">
                          Extended ({contract.extensionCount}x)
                        </span>
                      </p>
                    </div>
                    <div className="relative pl-4 ml-2.5">
                      {/* Vertical rail */}
                      <div className="absolute left-[5px] top-[6px] bottom-[6px] w-px bg-gradient-to-b from-border-strong via-info-text/30 to-info-text/60" />

                      {/* Original end date node */}
                      <div className="relative flex items-start gap-3 pb-4">
                        <div className="absolute left-[-13px] top-[5px] w-[7px] h-[7px] rounded-full border-2 border-border-strong bg-surface-card z-10" />
                        <div className="min-w-0">
                          <p className="text-[11px] font-medium uppercase tracking-wider text-text-muted">
                            Original
                          </p>
                          <p className="text-sm font-medium text-text-secondary line-through decoration-text-muted/40">
                            {formatDate(contract.endDate)}
                          </p>
                        </div>
                      </div>

                      {/* Extension nodes */}
                      {activeExtensions.map((ext, index) => {
                        const isLatest = index === activeExtensions.length - 1;
                        return (
                          <div
                            key={ext.identifier}
                            className={`relative flex items-start gap-3 ${isLatest ? '' : 'pb-4'}`}
                          >
                            {/* Node dot */}
                            <div
                              className={`absolute left-[-13px] z-10 ${
                                isLatest
                                  ? 'top-[3px] w-[11px] h-[11px] rounded-full bg-info-text shadow-[0_0_0_3px_var(--color-info-bg)]'
                                  : 'top-[5px] w-[7px] h-[7px] rounded-full bg-info-text/60 border-2 border-info-bg'
                              }`}
                            />
                            <div className="min-w-0 flex-1">
                              <div className="flex items-center gap-2">
                                <span className="text-[11px] font-medium uppercase tracking-wider text-text-muted">
                                  Extension #{ext.extensionNumber}
                                </span>
                                {ext.triggerType === 'AUTO' && (
                                  <span className="inline-flex items-center px-1 py-px text-[9px] font-semibold uppercase tracking-wider rounded bg-info-bg text-info-text">
                                    Auto
                                  </span>
                                )}
                              </div>
                              <p
                                className={`text-sm font-semibold ${isLatest ? 'text-text-primary' : 'text-text-secondary line-through decoration-text-muted/40'}`}
                              >
                                {ext.newEndDate
                                  ? formatDate(ext.newEndDate)
                                  : '—'}
                              </p>
                              {ext.activatedAt && (
                                <p className="text-[10px] text-text-muted mt-0.5">
                                  Activated {formatDate(ext.activatedAt)}
                                </p>
                              )}
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  </div>
                )}
              </div>
            </div>

            {/* Financial Terms */}
            <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
              <h2 className="text-lg font-semibold text-text-primary mb-4">
                Financial Terms
              </h2>
              <div className="space-y-4">
                <RentTimeline
                  contractIdentifier={id}
                  contractStatus={contract.status}
                  currency={contract.rentAmountCurrency}
                  currentRentAmount={contract.rentAmount}
                  paymentFrequency={contract.paymentFrequency}
                />
                {contract.rentComponents &&
                  contract.rentComponents.length > 0 && (
                    <div>
                      <h4 className="text-sm font-medium text-text-secondary mb-2">
                        Rent Breakdown
                      </h4>
                      <div className="bg-surface-secondary rounded-lg p-3 space-y-2">
                        {contract.rentComponents.map((comp) => (
                          <div
                            key={comp.identifier}
                            className="flex justify-between items-center text-sm"
                          >
                            <span className="text-text-secondary">
                              {comp.componentTypeDisplayName}
                              {comp.description && (
                                <span className="text-text-tertiary ml-1">
                                  ({comp.description})
                                </span>
                              )}
                            </span>
                            <span className="font-medium text-text-primary">
                              {comp.currency} {comp.amount.toFixed(2)}
                            </span>
                          </div>
                        ))}
                        <div className="flex justify-between items-center text-sm font-semibold pt-2 border-t border-border-default">
                          <span>Total</span>
                          <span>
                            {contract.rentAmountCurrency}{' '}
                            {contract.rentAmount.toFixed(2)}
                          </span>
                        </div>
                      </div>
                    </div>
                  )}
                {contract.depositAmount && (
                  <div className="flex items-center gap-3">
                    <DollarSign className="h-5 w-5 text-text-muted " />
                    <div>
                      <p className="text-sm text-text-secondary">Deposit</p>
                      <p className="font-medium text-text-primary">
                        {contract.depositAmountCurrency ??
                          contract.rentAmountCurrency}{' '}
                        {contract.depositAmount.toFixed(2)}
                      </p>
                    </div>
                  </div>
                )}
                {contract.securityDeposit && (
                  <div className="flex items-center gap-3">
                    <DollarSign className="h-5 w-5 text-text-muted " />
                    <div>
                      <p className="text-sm text-text-secondary">
                        Security Deposit
                      </p>
                      <p className="font-medium text-text-primary">
                        {contract.securityDepositCurrency ??
                          contract.rentAmountCurrency}{' '}
                        {contract.securityDeposit.toFixed(2)}
                      </p>
                    </div>
                  </div>
                )}
                {contract.paymentDueDay && (
                  <div>
                    <p className="text-sm text-text-secondary">
                      Payment Due Day
                    </p>
                    <p className="font-medium text-text-primary">
                      Day {contract.paymentDueDay} of each period
                    </p>
                  </div>
                )}
              </div>
            </div>

            {/* Payment Instructions */}
            <ContractPaymentInstructionSection
              contractIdentifier={id}
              contractStatus={contract.status}
              contractStartDate={contract.startDate}
              contractSignedDate={contract.signedDate}
            />

            {/* Renewal Configuration */}
            {contract.renewalMode && contract.renewalMode !== 'NONE' && (
              <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
                <div className="flex items-center gap-2 mb-4">
                  <Repeat className="h-5 w-5 text-text-muted" />
                  <h2 className="text-lg font-semibold text-text-primary">
                    Renewal Configuration
                  </h2>
                </div>
                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <p className="text-sm text-text-secondary">Mode</p>
                    <span
                      className={`inline-flex items-center px-2 py-0.5 text-xs font-medium rounded-full ${
                        contract.renewalMode === 'AUTOMATIC'
                          ? 'bg-success-bg text-success-text'
                          : 'bg-info-bg text-info-text'
                      }`}
                    >
                      {contract.renewalMode === 'AUTOMATIC'
                        ? 'Automatic'
                        : 'Manual'}
                    </span>
                  </div>
                  {contract.renewalTermMonths != null && (
                    <div className="flex items-center justify-between">
                      <p className="text-sm text-text-secondary">
                        Renewal Term
                      </p>
                      <p className="text-sm font-medium text-text-primary">
                        {contract.renewalTermMonths}{' '}
                        {contract.renewalTermMonths === 1 ? 'month' : 'months'}
                      </p>
                    </div>
                  )}
                  {contract.maxRenewals != null && (
                    <div className="flex items-center justify-between">
                      <p className="text-sm text-text-secondary">Extensions</p>
                      <p className="text-sm font-medium text-text-primary">
                        {contract.extensionsRemaining != null
                          ? `${contract.extensionsRemaining} of ${contract.maxRenewals} remaining`
                          : `${contract.maxRenewals} max`}
                      </p>
                    </div>
                  )}
                  {!contract.maxRenewals &&
                    contract.extensionsRemaining == null && (
                      <div className="flex items-center justify-between">
                        <p className="text-sm text-text-secondary">
                          Extensions
                        </p>
                        <p className="text-sm font-medium text-text-primary">
                          Unlimited
                        </p>
                      </div>
                    )}
                  {contract.rentAdjustmentType &&
                    contract.rentAdjustmentType !== 'NONE' && (
                      <div className="flex items-center justify-between">
                        <p className="text-sm text-text-secondary">
                          Rent Adjustment
                        </p>
                        <p className="text-sm font-medium text-text-primary">
                          {contract.rentAdjustmentType === 'FIXED_PERCENTAGE' &&
                          contract.rentAdjustmentValue != null
                            ? `+${contract.rentAdjustmentValue}%`
                            : contract.rentAdjustmentType === 'FIXED_AMOUNT' &&
                                contract.rentAdjustmentValue != null
                              ? `+${contract.rentAmountCurrency} ${contract.rentAdjustmentValue.toFixed(2)}`
                              : contract.rentAdjustmentType === 'MANUAL'
                                ? 'Manual'
                                : '—'}
                        </p>
                      </div>
                    )}
                  {(contract.landlordNoticeDays != null ||
                    contract.tenantNoticeDays != null) && (
                    <div className="pt-2 border-t border-border-default">
                      <p className="text-[11px] font-medium uppercase tracking-wider text-text-muted mb-2">
                        Notice Periods
                      </p>
                      <div className="space-y-2">
                        {contract.landlordNoticeDays != null && (
                          <div className="flex items-center justify-between">
                            <p className="text-sm text-text-secondary">
                              Landlord
                            </p>
                            <p className="text-sm font-medium text-text-primary">
                              {contract.landlordNoticeDays} days
                            </p>
                          </div>
                        )}
                        {contract.tenantNoticeDays != null && (
                          <div className="flex items-center justify-between">
                            <p className="text-sm text-text-secondary">
                              Tenant
                            </p>
                            <p className="text-sm font-medium text-text-primary">
                              {contract.tenantNoticeDays} days
                            </p>
                          </div>
                        )}
                      </div>
                    </div>
                  )}
                  {contract.requiresTenantConfirmation && (
                    <div className="flex items-center justify-between pt-2 border-t border-border-default">
                      <p className="text-sm text-text-secondary">
                        Tenant Confirmation
                      </p>
                      <span className="inline-flex items-center px-2 py-0.5 text-xs font-medium rounded-full bg-warning-bg text-warning-text">
                        Required
                      </span>
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* Additional Terms */}
            {(contract.terminationNoticeDays || contract.lateFeePercentage) && (
              <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
                <h2 className="text-lg font-semibold text-text-primary mb-4">
                  Additional Terms
                </h2>
                <div className="space-y-3">
                  {contract.terminationNoticeDays && (
                    <div>
                      <p className="text-sm text-text-secondary">
                        Termination Notice
                      </p>
                      <p className="font-medium text-text-primary">
                        {contract.terminationNoticeDays} days
                      </p>
                    </div>
                  )}
                  {contract.lateFeePercentage && (
                    <div>
                      <p className="text-sm text-text-secondary">Late Fee</p>
                      <p className="font-medium text-text-primary">
                        {contract.lateFeePercentage}%
                      </p>
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* Terms and Conditions */}
            {contract.termsAndConditions && (
              <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6 lg:col-span-2">
                <h2 className="text-lg font-semibold text-text-primary mb-4">
                  Terms and Conditions
                </h2>
                <RichTextDisplay content={contract.termsAndConditions} />
              </div>
            )}

            {/* Notes */}
            {contract.notes && (
              <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6 lg:col-span-2">
                <h2 className="text-lg font-semibold text-text-primary mb-4">
                  Notes
                </h2>
                <RichTextDisplay content={contract.notes} />
              </div>
            )}

            {/* Country-Specific Details */}
            {contract.countryCode &&
              contract.countryMetadata &&
              Object.keys(contract.countryMetadata).length > 0 && (
                <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6 lg:col-span-2">
                  <h2 className="text-lg font-semibold text-text-primary mb-4">
                    {countryName
                      ? `${countryName} Rental Details`
                      : 'Country-Specific Details'}
                  </h2>
                  <CountryMetadataForm
                    countryCode={contract.countryCode}
                    value={contract.countryMetadata}
                    onChange={() => {}}
                    currency={contract.rentAmountCurrency}
                    disabled={true}
                  />
                </div>
              )}

            {/* Metadata */}
            <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6 lg:col-span-2">
              <button
                onClick={() => setIsMetadataExpanded(!isMetadataExpanded)}
                className="w-full flex items-center justify-between text-left group"
              >
                <h2 className="text-lg font-semibold text-text-primary">
                  Metadata
                </h2>
                {isMetadataExpanded ? (
                  <ChevronUp className="h-5 w-5 text-text-secondary group-hover:text-text-secondary " />
                ) : (
                  <ChevronDown className="h-5 w-5 text-text-secondary group-hover:text-text-secondary " />
                )}
              </button>
              {isMetadataExpanded && (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm mt-4">
                  <div>
                    <span className="text-text-secondary">Created:</span>{' '}
                    <span className="text-text-primary">
                      {formatDate(contract.createdAt)} at{' '}
                      {new Date(contract.createdAt).toLocaleTimeString()}
                    </span>
                  </div>
                  <div>
                    <span className="text-text-secondary">Last Updated:</span>{' '}
                    <span className="text-text-primary">
                      {formatDate(contract.updatedAt)} at{' '}
                      {new Date(contract.updatedAt).toLocaleTimeString()}
                    </span>
                  </div>
                </div>
              )}
            </div>
          </div>
        )}

        {activeTab === 'payments' && (
          <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-xl font-semibold text-text-primary">
                Payments ({filteredAndSortedPayments.length})
              </h2>
              <div className="flex items-center gap-2">
                {contract?.status === ContractStatus.ACTIVE && (
                  <Button
                    variant="success"
                    leftIcon={<RefreshCw />}
                    onClick={() => setShowGeneratePaymentsModal(true)}
                    disabled={!canEditData}
                  >
                    Generate Future Payments
                  </Button>
                )}
                <Button
                  variant="secondary"
                  leftIcon={<Calendar />}
                  onClick={() => navigate(`/payments/new?contractId=${id}`)}
                  disabled={!canEditData}
                >
                  Schedule Payment
                </Button>
                <Button
                  variant="primary"
                  leftIcon={<Plus />}
                  onClick={() =>
                    navigate(`/payments/new?contractId=${id}&register=true`)
                  }
                  disabled={!canEditData}
                >
                  Register Payments
                </Button>
              </div>
            </div>

            {paymentsLoading ? (
              <LoadingSpinner />
            ) : paymentsError ? (
              <ErrorMessage message="Failed to load payments" />
            ) : payments.length === 0 ? (
              <div className="text-center py-12">
                <DollarSign className="h-12 w-12 text-text-disabled mx-auto mb-3" />
                <p className="text-text-secondary mb-4">
                  No payments for this contract
                </p>
                <Button
                  variant="primary"
                  leftIcon={<Plus />}
                  onClick={() => navigate(`/payments/new?contractId=${id}`)}
                  disabled={!canEditData}
                >
                  Create First Payment
                </Button>
              </div>
            ) : (
              <>
                {/* Search Bar */}
                <div className="mb-4">
                  <div className="relative">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-text-muted " />
                    <input
                      type="text"
                      placeholder="Search by payment #, status..."
                      value={paymentsSearchTerm}
                      onChange={(e) => {
                        setPaymentsSearchTerm(e.target.value);
                        setPaymentsCurrentPage(1);
                      }}
                      className="w-full pl-10 pr-4 py-2 border border-border-strong rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    />
                  </div>
                </div>

                {/* Table */}
                <div className="overflow-x-auto">
                  <table className="min-w-full divide-y divide-border-default">
                    <thead className="bg-surface-page">
                      <tr>
                        <th className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                          Payment #
                        </th>
                        <th
                          className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
                          onClick={() => handlePaymentsSort('dueDate')}
                        >
                          <div className="flex items-center gap-1">
                            Due Date
                            {paymentsSortField === 'dueDate' &&
                              (paymentsSortOrder === 'asc' ? (
                                <ChevronUp className="h-4 w-4" />
                              ) : (
                                <ChevronDown className="h-4 w-4" />
                              ))}
                          </div>
                        </th>
                        <th
                          className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
                          onClick={() => handlePaymentsSort('amount')}
                        >
                          <div className="flex items-center gap-1">
                            Amount
                            {paymentsSortField === 'amount' &&
                              (paymentsSortOrder === 'asc' ? (
                                <ChevronUp className="h-4 w-4" />
                              ) : (
                                <ChevronDown className="h-4 w-4" />
                              ))}
                          </div>
                        </th>
                        <th
                          className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
                          onClick={() => handlePaymentsSort('status')}
                        >
                          <div className="flex items-center gap-1">
                            Status
                            {paymentsSortField === 'status' &&
                              (paymentsSortOrder === 'asc' ? (
                                <ChevronUp className="h-4 w-4" />
                              ) : (
                                <ChevronDown className="h-4 w-4" />
                              ))}
                          </div>
                        </th>
                        <th
                          className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
                          onClick={() => handlePaymentsSort('paymentDate')}
                        >
                          <div className="flex items-center gap-1">
                            Payment Date
                            {paymentsSortField === 'paymentDate' &&
                              (paymentsSortOrder === 'asc' ? (
                                <ChevronUp className="h-4 w-4" />
                              ) : (
                                <ChevronDown className="h-4 w-4" />
                              ))}
                          </div>
                        </th>
                        <th className="px-4 py-3 text-right text-xs font-medium text-text-secondary uppercase tracking-wider">
                          Actions
                        </th>
                      </tr>
                    </thead>
                    <tbody className="bg-surface-card divide-y divide-border-default">
                      {paginatedPayments.length === 0 ? (
                        <tr>
                          <td
                            colSpan={5}
                            className="px-6 py-12 text-center text-text-secondary"
                          >
                            No payments found matching your search
                          </td>
                        </tr>
                      ) : (
                        paginatedPayments.map((payment) => (
                          <tr
                            key={payment.identifier}
                            className="hover:bg-primary-50 cursor-pointer"
                            onClick={() =>
                              navigate(`/payments/${payment.identifier}`)
                            }
                          >
                            <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-primary-500 dark:text-primary-300">
                              #{payment.identifier}
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap text-sm text-text-primary">
                              {formatDate(payment.dueDate)}
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap text-sm text-right">
                              <div>
                                <span className="font-semibold text-text-primary">
                                  {payment.currency} {payment.amount.toFixed(2)}
                                </span>
                                {payment.receivedAmount > 0 &&
                                  payment.status !== PaymentStatus.PAID && (
                                    <p className="text-xs text-text-secondary">
                                      Balance: {payment.currency}{' '}
                                      {(payment.balance ?? 0).toFixed(2)}
                                    </p>
                                  )}
                              </div>
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap">
                              <PaymentStatusBadge status={payment.status} />
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap text-sm text-text-primary">
                              {payment.paymentDate
                                ? formatDate(payment.paymentDate)
                                : '-'}
                            </td>
                            <td className="px-4 py-4 whitespace-nowrap text-right">
                              {canEditData && (
                                <button
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    setDeletePaymentTarget(payment.identifier);
                                  }}
                                  className="p-1.5 rounded hover:bg-error-bg text-text-secondary hover:text-error-text transition-colors"
                                  title="Delete payment"
                                >
                                  <Trash2 className="h-4 w-4" />
                                </button>
                              )}
                            </td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>

                {/* Pagination */}
                {paymentsTotalPages > 1 && (
                  <div className="flex items-center justify-between mt-4 pt-4 border-t border-border-default">
                    <div className="text-sm text-text-secondary">
                      Showing {(paymentsCurrentPage - 1) * paymentsPerPage + 1}{' '}
                      to{' '}
                      {Math.min(
                        paymentsCurrentPage * paymentsPerPage,
                        filteredAndSortedPayments.length
                      )}{' '}
                      of {filteredAndSortedPayments.length} payments
                    </div>
                    <div className="flex gap-2">
                      <button
                        onClick={() =>
                          setPaymentsCurrentPage(paymentsCurrentPage - 1)
                        }
                        disabled={paymentsCurrentPage === 1}
                        className="px-3 py-1 border border-border-strong rounded text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-surface-inset"
                      >
                        Previous
                      </button>
                      <span className="px-3 py-1 text-sm text-text-secondary">
                        Page {paymentsCurrentPage} of {paymentsTotalPages}
                      </span>
                      <button
                        onClick={() =>
                          setPaymentsCurrentPage(paymentsCurrentPage + 1)
                        }
                        disabled={paymentsCurrentPage === paymentsTotalPages}
                        className="px-3 py-1 border border-border-strong rounded text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-surface-inset"
                      >
                        Next
                      </button>
                    </div>
                  </div>
                )}
              </>
            )}
          </div>
        )}

        {activeTab === 'extensions' && (
          <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
            <ExtensionTimeline
              contractIdentifier={id}
              contractStatus={contract.status}
              currency={contract.rentAmountCurrency}
              currentRentAmount={contract.rentAmount}
              currentEndDate={contract.effectiveEndDate ?? contract.endDate}
              renewalTermMonths={contract.renewalTermMonths}
              rentAdjustmentType={contract.rentAdjustmentType}
              rentAdjustmentValue={contract.rentAdjustmentValue}
            />
          </div>
        )}

        {activeTab === 'documents' && (
          <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
            <DocumentList
              documents={documents}
              onUpload={handleUploadDocument}
              onDelete={handleDeleteDocument}
              isLoading={docsLoading}
              error={docsError}
              isUploading={uploadDocumentMutation.isPending}
              isDeleting={deleteDocumentMutation.isPending}
              readOnly={!canEditData}
            />
          </div>
        )}

        {activeTab === 'history' && (
          <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
            <h2 className="text-xl font-semibold text-text-primary mb-4">
              Contract History
            </h2>
            {auditLoading ? (
              <div className="flex items-center justify-center py-8">
                <LoadingSpinner />
              </div>
            ) : auditError ? (
              <ErrorMessage message="Failed to load history" />
            ) : auditLog.length > 0 ? (
              <div className="space-y-4">
                {auditLog.map((activity) => {
                  const activityKey = `${activity.entityType}-${activity.entityIdentifier}-${activity.timestamp}`;
                  const isExpanded = expandedAuditItems.has(activityKey);
                  const hasChanges =
                    activity.action === 'UPDATE' &&
                    activity.changedFields &&
                    Object.keys(activity.changedFields).length > 0;

                  return (
                    <div
                      key={activityKey}
                      className="border border-border-default rounded-lg overflow-hidden"
                    >
                      <div
                        className={`flex items-start gap-4 p-4 transition-colors ${
                          hasChanges
                            ? 'cursor-pointer hover:bg-surface-inset'
                            : ''
                        }`}
                        onClick={() =>
                          hasChanges &&
                          setExpandedAuditItems((prev) => {
                            const newSet = new Set(prev);
                            if (newSet.has(activityKey)) {
                              newSet.delete(activityKey);
                            } else {
                              newSet.add(activityKey);
                            }
                            return newSet;
                          })
                        }
                      >
                        <div
                          className={`flex-shrink-0 w-10 h-10 rounded-full flex items-center justify-center ${
                            activity.action === 'CREATE'
                              ? 'bg-success-bg'
                              : activity.action === 'UPDATE'
                                ? 'bg-info-bg'
                                : 'bg-error-bg'
                          }`}
                        >
                          <span
                            className={`text-xs font-semibold ${
                              activity.action === 'CREATE'
                                ? 'text-success-text'
                                : activity.action === 'UPDATE'
                                  ? 'text-info-text'
                                  : 'text-error-text'
                            }`}
                          >
                            {activity.action.charAt(0)}
                          </span>
                        </div>
                        <div className="flex-1 min-w-0">
                          <p className="text-sm font-medium text-text-primary">
                            {activity.description}
                          </p>
                          <div className="flex items-center gap-2 mt-1">
                            <p className="text-xs text-text-secondary">
                              {formatDistanceToNow(
                                new Date(activity.timestamp),
                                {
                                  addSuffix: true,
                                }
                              )}
                            </p>
                            {activity.impersonatedBy && (
                              <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-medium bg-warning-bg text-warning-text">
                                <Eye className="h-3 w-3" />
                                Impersonated
                              </span>
                            )}
                          </div>
                          {hasChanges && (
                            <p className="text-xs text-primary-500 mt-1">
                              {isExpanded
                                ? 'Click to hide changes'
                                : 'Click to view changes'}
                            </p>
                          )}
                        </div>
                      </div>

                      {isExpanded && hasChanges && (
                        <div className="bg-surface-page px-4 py-3 border-t border-border-default">
                          <h4 className="text-xs font-semibold text-text-secondary mb-2 uppercase">
                            Changed Fields
                          </h4>
                          <div className="space-y-2">
                            {Object.entries(activity.changedFields ?? {})
                              .filter(([field]) => field !== 'updatedAt')
                              .map(([field, value]) => {
                                // Skip internal fields for document operations
                                if (field === 'documentCount') {
                                  return null;
                                }

                                // Special handling for document operations
                                if (
                                  field === 'documentAdded' ||
                                  field === 'documentRemoved'
                                ) {
                                  const category =
                                    activity.changedFields?.category;
                                  const title = activity.changedFields?.title;
                                  return (
                                    <div
                                      key={field}
                                      className="bg-surface-card rounded p-2 text-xs"
                                    >
                                      <div className="font-semibold text-text-secondary mb-1">
                                        File Name
                                      </div>
                                      <div className="text-text-primary">
                                        {String(value)}
                                      </div>
                                      {title ? (
                                        <>
                                          <div className="font-semibold text-text-secondary mb-1 mt-2">
                                            Title
                                          </div>
                                          <div className="text-text-primary">
                                            {String(title)}
                                          </div>
                                        </>
                                      ) : null}
                                      <div className="font-semibold text-text-secondary mb-1 mt-2">
                                        Type
                                      </div>
                                      <div className="text-text-primary">
                                        {category === 'PHOTO'
                                          ? 'Photo'
                                          : 'Document'}
                                      </div>
                                    </div>
                                  );
                                }

                                // Skip category and title for document operations (already shown above)
                                if (
                                  (field === 'category' || field === 'title') &&
                                  (activity.changedFields?.documentAdded ||
                                    activity.changedFields?.documentRemoved)
                                ) {
                                  return null;
                                }

                                return (
                                  <div
                                    key={field}
                                    className="bg-surface-card rounded p-2 text-xs"
                                  >
                                    <div className="font-semibold text-text-secondary mb-1">
                                      {field
                                        .replace(/([A-Z])/g, ' $1')
                                        .replace(/^./, (str) =>
                                          str.toUpperCase()
                                        )
                                        .trim()}
                                    </div>
                                    <div className="grid grid-cols-2 gap-2">
                                      <div>
                                        <span className="text-text-secondary">
                                          Old:{' '}
                                        </span>
                                        {typeof activity.oldValues?.[field] ===
                                          'string' &&
                                        /<[a-z][\s\S]*>/i.test(
                                          activity.oldValues[field]
                                        ) ? (
                                          <RichTextDisplay
                                            content={activity.oldValues[field]}
                                            className="text-xs text-error-text line-through [&_p]:m-0 inline"
                                          />
                                        ) : (
                                          <span className="text-error-text line-through">
                                            {formatAuditValue(
                                              activity.oldValues?.[field]
                                            )}
                                          </span>
                                        )}
                                      </div>
                                      <div>
                                        <span className="text-text-secondary">
                                          New:{' '}
                                        </span>
                                        {typeof activity.newValues?.[field] ===
                                          'string' &&
                                        /<[a-z][\s\S]*>/i.test(
                                          activity.newValues[field]
                                        ) ? (
                                          <RichTextDisplay
                                            content={activity.newValues[field]}
                                            className="text-xs text-success-text font-medium [&_p]:m-0 inline"
                                          />
                                        ) : (
                                          <span className="text-success-text font-medium">
                                            {formatAuditValue(
                                              activity.newValues?.[field]
                                            )}
                                          </span>
                                        )}
                                      </div>
                                    </div>
                                  </div>
                                );
                              })}
                          </div>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            ) : (
              <div className="text-center py-8">
                <History className="h-12 w-12 text-text-disabled mx-auto mb-3" />
                <p className="text-text-secondary">No history available</p>
                <p className="text-sm text-text-muted mt-1">
                  Changes to this contract will appear here
                </p>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Delete Confirmation Modal */}
      {showDeleteModal && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50">
          <div className="bg-surface-card rounded-lg shadow-xl max-w-md w-full mx-4 p-6">
            <h2 className="text-lg font-semibold text-text-primary mb-4">
              Delete Contract
            </h2>
            <p className="text-text-secondary mb-6">
              Are you sure you want to delete this contract? This action cannot
              be undone.
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

      {/* Generate Future Payments Modal */}
      <GeneratePaymentsModal
        isOpen={showGeneratePaymentsModal}
        onClose={() => setShowGeneratePaymentsModal(false)}
        onSubmit={async (count, markAsPaid) => {
          await generatePaymentsMutation.mutateAsync({ count, markAsPaid });
        }}
        isLoading={generatePaymentsMutation.isPending}
      />

      {deletePaymentTarget && (
        <ConfirmDialog
          title="Delete Payment"
          message="Are you sure you want to delete this payment? All related data (receivals, documents) will also be deleted. This action cannot be undone."
          confirmLabel="Delete"
          variant="danger"
          isLoading={deletePaymentMutation.isPending}
          onConfirm={async () => {
            await deletePaymentMutation.mutateAsync(deletePaymentTarget);
            setDeletePaymentTarget(null);
          }}
          onCancel={() => setDeletePaymentTarget(null)}
        />
      )}
    </div>
  );
};
