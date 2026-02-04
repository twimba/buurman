import { useState, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
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
import { usePaymentsByContract } from '@/hooks/usePaymentHooks';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { ErrorMessage } from '@/components/ErrorMessage';
import { RichTextDisplay } from '@/components/common/RichTextDisplay';
import { DocumentList } from '@/components/properties/DocumentList';
import { ContractStatusBadge } from '@/components/contracts/ContractStatusBadge';
import { ChangeContractStatusModal } from '@/components/contracts/ChangeContractStatusModal';
import GeneratePaymentsModal from '@/components/contracts/GeneratePaymentsModal';
import { Button, PageHeader } from '@/components/ui';
import { useTeam } from '@/context/TeamContext';
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
} from 'lucide-react';
import { formatDistanceToNow } from 'date-fns';
import { useFormatDate } from '@/hooks/useFormatDate';
import { ChangeContractStatusRequest, ContractStatus } from '@/types/contract';
import { PaymentStatusBadge } from '@/components/payments/PaymentStatusBadge';

export const ContractDetailPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { canEditData } = useTeam();
  const { formatDate } = useFormatDate();
  const [activeTab, setActiveTab] = useState<
    'overview' | 'payments' | 'documents' | 'history'
  >('overview');
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [showStatusModal, setShowStatusModal] = useState(false);
  const [showGeneratePaymentsModal, setShowGeneratePaymentsModal] =
    useState(false);
  const [expandedAuditItems, setExpandedAuditItems] = useState<Set<string>>(
    new Set()
  );

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

  const deleteContractMutation = useDeleteContract();
  const uploadDocumentMutation = useUploadContractDocument(id!);
  const deleteDocumentMutation = useDeleteContractDocument(id!);
  const changeStatusMutation = useChangeContractStatus(id!);
  const reopenContractMutation = useReopenContract(id!);
  const duplicateContractMutation = useDuplicateContract();
  const generatePaymentsMutation = useGenerateContractPayments(id!);

  // Payments filtering, sorting, and pagination
  const filteredAndSortedPayments = useMemo(() => {
    if (!payments) return [];

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

      if (aVal < bVal) return paymentsSortOrder === 'asc' ? -1 : 1;
      if (aVal > bVal) return paymentsSortOrder === 'asc' ? 1 : -1;
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
    if (!id) return;
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
    if (!id) return;
    try {
      const newContract = await duplicateContractMutation.mutateAsync(id);
      navigate(`/contracts/${newContract.id}`);
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

  const canDelete = contract.status !== ContractStatus.ACTIVE;
  const canEdit = contract.status !== ContractStatus.ACTIVE;
  const canReopen =
    contract.status === ContractStatus.TERMINATED ||
    contract.status === ContractStatus.EXPIRED;

  return (
    <div className="min-h-screen bg-background">
      <div className="px-4 py-8">
        {/* Header */}
        <PageHeader
          title={`Contract #${contract.identifier}`}
          subtitle={contract.contractType.replace('_', ' ')}
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
                      `/reports/export/contract/${id}/report`,
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
                    link.download = 'contract-report.pdf';
                    document.body.appendChild(link);
                    link.click();
                    document.body.removeChild(link);
                    window.URL.revokeObjectURL(url);
                  } catch (error) {
                    console.error('Failed to download report:', error);
                    alert('Failed to download report. Please try again.');
                  }
                }}
              >
                Report
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
        <div className="border-b border-[#e2e6f0] mb-6">
          <div className="flex gap-6">
            <button
              onClick={() => setActiveTab('overview')}
              className={`pb-3 px-1 font-medium transition-colors ${
                activeTab === 'overview'
                  ? 'border-b-2 border-[#5c7cfa] text-blue-600'
                  : 'text-[#6b7194] dark:text-[#8b90a8] hover:text-[#1a1d2e] dark:text-[#eef0f6] dark:hover:text-[#c4c8db]'
              }`}
            >
              Overview
            </button>
            <button
              onClick={() => setActiveTab('payments')}
              className={`pb-3 px-1 font-medium transition-colors flex items-center gap-2 ${
                activeTab === 'payments'
                  ? 'border-b-2 border-[#5c7cfa] text-blue-600'
                  : 'text-[#6b7194] dark:text-[#8b90a8] hover:text-[#1a1d2e] dark:text-[#eef0f6] dark:hover:text-[#c4c8db]'
              }`}
            >
              <DollarSign className="h-4 w-4" />
              Payments {payments.length > 0 && `(${payments.length})`}
            </button>
            <button
              onClick={() => setActiveTab('documents')}
              className={`pb-3 px-1 font-medium transition-colors flex items-center gap-2 ${
                activeTab === 'documents'
                  ? 'border-b-2 border-[#5c7cfa] text-blue-600'
                  : 'text-[#6b7194] dark:text-[#8b90a8] hover:text-[#1a1d2e] dark:text-[#eef0f6] dark:hover:text-[#c4c8db]'
              }`}
            >
              <FileText className="h-4 w-4" />
              Documents {documents.length > 0 && `(${documents.length})`}
            </button>
            <button
              onClick={() => setActiveTab('history')}
              className={`pb-3 px-1 font-medium transition-colors ${
                activeTab === 'history'
                  ? 'border-b-2 border-[#5c7cfa] text-blue-600'
                  : 'text-[#6b7194] dark:text-[#8b90a8] hover:text-[#1a1d2e] dark:text-[#eef0f6] dark:hover:text-[#c4c8db]'
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
            <div className="bg-white dark:bg-[#14161f] rounded-lg shadow p-6">
              <h2 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
                Contract Parties
              </h2>
              <div className="space-y-4">
                <div className="flex items-start gap-3">
                  <Home className="h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180] mt-1" />
                  <div className="flex-1">
                    <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">Property</p>
                    <button
                      onClick={() =>
                        navigate(`/properties/${contract.property.id}`)
                      }
                      className="font-medium text-[#5c7cfa] hover:underline text-left"
                    >
                      {contract.property.street}, {contract.property.city}
                    </button>
                    <p className="text-xs text-[#6b7194] dark:text-[#8b90a8]">
                      #{contract.property.identifier}
                    </p>
                  </div>
                </div>
                <div className="flex items-start gap-3">
                  <User className="h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180] mt-1" />
                  <div className="flex-1">
                    <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">Tenant</p>
                    <button
                      onClick={() => navigate(`/tenants/${contract.tenant.id}`)}
                      className="font-medium text-[#5c7cfa] hover:underline text-left"
                    >
                      {contract.tenant.firstName} {contract.tenant.lastName}
                    </button>
                    <p className="text-xs text-[#6b7194] dark:text-[#8b90a8]">
                      #{contract.tenant.identifier}
                    </p>
                  </div>
                </div>
              </div>
            </div>

            {/* Contract Dates */}
            <div className="bg-white dark:bg-[#14161f] rounded-lg shadow p-6">
              <h2 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
                Important Dates
              </h2>
              <div className="space-y-4">
                <div className="flex items-center gap-3">
                  <Calendar className="h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180]" />
                  <div>
                    <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">Start Date</p>
                    <p className="font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                      {formatDate(contract.startDate)}
                    </p>
                  </div>
                </div>
                {contract.endDate && (
                  <div className="flex items-center gap-3">
                    <Calendar className="h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180]" />
                    <div>
                      <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">End Date</p>
                      <p className="font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                        {formatDate(contract.endDate)}
                      </p>
                    </div>
                  </div>
                )}
                {contract.signedDate && (
                  <div className="flex items-center gap-3">
                    <Calendar className="h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180]" />
                    <div>
                      <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">Signed Date</p>
                      <p className="font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                        {formatDate(contract.signedDate)}
                      </p>
                    </div>
                  </div>
                )}
              </div>
            </div>

            {/* Financial Terms */}
            <div className="bg-white dark:bg-[#14161f] rounded-lg shadow p-6">
              <h2 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
                Financial Terms
              </h2>
              <div className="space-y-4">
                <div className="flex items-center gap-3">
                  <DollarSign className="h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180]" />
                  <div>
                    <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">Rent Amount</p>
                    <p className="font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                      {contract.currency} {contract.rentAmount.toFixed(2)} /{' '}
                      {contract.paymentFrequency.toLowerCase()}
                    </p>
                  </div>
                </div>
                {contract.depositAmount && (
                  <div className="flex items-center gap-3">
                    <DollarSign className="h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180]" />
                    <div>
                      <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">Deposit</p>
                      <p className="font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                        {contract.currency} {contract.depositAmount.toFixed(2)}
                      </p>
                    </div>
                  </div>
                )}
                {contract.securityDeposit && (
                  <div className="flex items-center gap-3">
                    <DollarSign className="h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180]" />
                    <div>
                      <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">Security Deposit</p>
                      <p className="font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                        {contract.currency}{' '}
                        {contract.securityDeposit.toFixed(2)}
                      </p>
                    </div>
                  </div>
                )}
                {contract.paymentDueDay && (
                  <div>
                    <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">Payment Due Day</p>
                    <p className="font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                      Day {contract.paymentDueDay} of each period
                    </p>
                  </div>
                )}
              </div>
            </div>

            {/* Additional Terms */}
            <div className="bg-white dark:bg-[#14161f] rounded-lg shadow p-6">
              <h2 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
                Additional Terms
              </h2>
              <div className="space-y-3">
                <div>
                  <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">Auto-renewal</p>
                  <p className="font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                    {contract.autoRenewal ? 'Yes' : 'No'}
                  </p>
                </div>
                {contract.renewalNoticeDays && (
                  <div>
                    <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">Renewal Notice</p>
                    <p className="font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                      {contract.renewalNoticeDays} days
                    </p>
                  </div>
                )}
                {contract.terminationNoticeDays && (
                  <div>
                    <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">Termination Notice</p>
                    <p className="font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                      {contract.terminationNoticeDays} days
                    </p>
                  </div>
                )}
                {contract.lateFeePercentage && (
                  <div>
                    <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">Late Fee</p>
                    <p className="font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                      {contract.lateFeePercentage}%
                    </p>
                  </div>
                )}
              </div>
            </div>

            {/* Terms and Conditions */}
            {contract.termsAndConditions && (
              <div className="bg-white dark:bg-[#14161f] rounded-lg shadow p-6 lg:col-span-2">
                <h2 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
                  Terms and Conditions
                </h2>
                <RichTextDisplay content={contract.termsAndConditions} />
              </div>
            )}

            {/* Notes */}
            {contract.notes && (
              <div className="bg-white dark:bg-[#14161f] rounded-lg shadow p-6 lg:col-span-2">
                <h2 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
                  Notes
                </h2>
                <RichTextDisplay content={contract.notes} />
              </div>
            )}
          </div>
        )}

        {activeTab === 'payments' && (
          <div className="bg-white dark:bg-[#14161f] rounded-lg shadow p-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-xl font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
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
                    Generate Payments
                  </Button>
                )}
                <Button
                  variant="primary"
                  leftIcon={<Plus />}
                  onClick={() => navigate(`/payments/new?contractId=${id}`)}
                  disabled={!canEditData}
                >
                  Add Payment
                </Button>
              </div>
            </div>

            {paymentsLoading ? (
              <LoadingSpinner />
            ) : paymentsError ? (
              <ErrorMessage message="Failed to load payments" />
            ) : payments.length === 0 ? (
              <div className="text-center py-12">
                <DollarSign className="h-12 w-12 text-[#c9cfd9] dark:text-[#3a3f54] mx-auto mb-3" />
                <p className="text-[#6b7194] dark:text-[#8b90a8] mb-4">
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
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180]" />
                    <input
                      type="text"
                      placeholder="Search by payment #, status..."
                      value={paymentsSearchTerm}
                      onChange={(e) => {
                        setPaymentsSearchTerm(e.target.value);
                        setPaymentsCurrentPage(1);
                      }}
                      className="w-full pl-10 pr-4 py-2 border border-[#c9cfd9] rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    />
                  </div>
                </div>

                {/* Table */}
                <div className="overflow-x-auto">
                  <table className="min-w-full divide-y divide-[#edf0f7] dark:divide-[#2a2e3f]">
                    <thead className="bg-[#f8f9fc] dark:bg-[#0c0d14]">
                      <tr>
                        <th className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider">
                          Payment #
                        </th>
                        <th
                          className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
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
                          className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
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
                          className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
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
                          className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
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
                      </tr>
                    </thead>
                    <tbody className="bg-white dark:bg-[#14161f] divide-y divide-[#edf0f7] dark:divide-[#2a2e3f]">
                      {paginatedPayments.length === 0 ? (
                        <tr>
                          <td
                            colSpan={5}
                            className="px-6 py-12 text-center text-[#6b7194] dark:text-[#8b90a8]"
                          >
                            No payments found matching your search
                          </td>
                        </tr>
                      ) : (
                        paginatedPayments.map((payment) => (
                          <tr
                            key={payment.id}
                            className="hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] cursor-pointer"
                            onClick={() => navigate(`/payments/${payment.id}`)}
                          >
                            <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-[#5c7cfa] dark:text-[#91a7ff]">
                              #{payment.identifier}
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap text-sm text-[#1a1d2e] dark:text-[#eef0f6]">
                              {formatDate(payment.dueDate)}
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap text-sm font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                              {payment.currency} {payment.amount.toFixed(2)}
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap">
                              <PaymentStatusBadge status={payment.status} />
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap text-sm text-[#1a1d2e] dark:text-[#eef0f6]">
                              {payment.paymentDate
                                ? formatDate(payment.paymentDate)
                                : '-'}
                            </td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>

                {/* Pagination */}
                {paymentsTotalPages > 1 && (
                  <div className="flex items-center justify-between mt-4 pt-4 border-t border-[#e2e6f0] dark:border-[#2a2e3f]">
                    <div className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
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
                        className="px-3 py-1 border border-[#c9cfd9] rounded text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
                      >
                        Previous
                      </button>
                      <span className="px-3 py-1 text-sm text-[#6b7194] dark:text-[#8b90a8]">
                        Page {paymentsCurrentPage} of {paymentsTotalPages}
                      </span>
                      <button
                        onClick={() =>
                          setPaymentsCurrentPage(paymentsCurrentPage + 1)
                        }
                        disabled={paymentsCurrentPage === paymentsTotalPages}
                        className="px-3 py-1 border border-[#c9cfd9] rounded text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
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

        {activeTab === 'documents' && (
          <div className="bg-white dark:bg-[#14161f] rounded-lg shadow p-6">
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
          <div className="bg-white dark:bg-[#14161f] rounded-lg shadow p-6">
            <h2 className="text-xl font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
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
                  const isExpanded = expandedAuditItems.has(activity.id);
                  const hasChanges =
                    activity.action === 'UPDATE' &&
                    activity.changedFields &&
                    Object.keys(activity.changedFields).length > 0;

                  return (
                    <div
                      key={activity.id}
                      className="border border-[#e2e6f0] rounded-lg overflow-hidden"
                    >
                      <div
                        className={`flex items-start gap-4 p-4 transition-colors ${
                          hasChanges ? 'cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]' : ''
                        }`}
                        onClick={() =>
                          hasChanges &&
                          setExpandedAuditItems((prev) => {
                            const newSet = new Set(prev);
                            if (newSet.has(activity.id)) {
                              newSet.delete(activity.id);
                            } else {
                              newSet.add(activity.id);
                            }
                            return newSet;
                          })
                        }
                      >
                        <div
                          className={`flex-shrink-0 w-10 h-10 rounded-full flex items-center justify-center ${
                            activity.action === 'CREATE'
                              ? 'bg-green-100 dark:bg-green-900/30'
                              : activity.action === 'UPDATE'
                                ? 'bg-blue-100 dark:bg-blue-900/30'
                                : 'bg-red-100 dark:bg-red-900/30'
                          }`}
                        >
                          <span
                            className={`text-xs font-semibold ${
                              activity.action === 'CREATE'
                                ? 'text-green-700'
                                : activity.action === 'UPDATE'
                                  ? 'text-blue-700'
                                  : 'text-red-700'
                            }`}
                          >
                            {activity.action.charAt(0)}
                          </span>
                        </div>
                        <div className="flex-1 min-w-0">
                          <p className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                            {activity.description}
                          </p>
                          <p className="text-xs text-[#6b7194] dark:text-[#8b90a8] mt-1">
                            {formatDistanceToNow(new Date(activity.timestamp), {
                              addSuffix: true,
                            })}
                          </p>
                          {hasChanges && (
                            <p className="text-xs text-[#5c7cfa] mt-1">
                              {isExpanded
                                ? 'Click to hide changes'
                                : 'Click to view changes'}
                            </p>
                          )}
                        </div>
                      </div>

                      {isExpanded && hasChanges && (
                        <div className="bg-[#f8f9fc] dark:bg-[#0c0d14] px-4 py-3 border-t border-[#e2e6f0] dark:border-[#2a2e3f]">
                          <h4 className="text-xs font-semibold text-[#3d4463] dark:text-[#c4c8db] mb-2 uppercase">
                            Changed Fields
                          </h4>
                          <div className="space-y-2">
                            {Object.entries(activity.changedFields!)
                              .filter(([field]) => field !== 'updatedAt')
                              .map(([field, value]) => {
                                // Skip internal fields for document operations
                                if (field === 'documentCount') return null;

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
                                      className="bg-white dark:bg-[#14161f] dark:text-[#eef0f6] rounded p-2 text-xs"
                                    >
                                      <div className="font-semibold text-[#3d4463] dark:text-[#c4c8db] mb-1">
                                        File Name
                                      </div>
                                      <div className="text-[#1a1d2e] dark:text-[#eef0f6]">
                                        {String(value)}
                                      </div>
                                      {title ? (
                                        <>
                                          <div className="font-semibold text-[#3d4463] dark:text-[#c4c8db] mb-1 mt-2">
                                            Title
                                          </div>
                                          <div className="text-[#1a1d2e] dark:text-[#eef0f6]">
                                            {String(title)}
                                          </div>
                                        </>
                                      ) : null}
                                      <div className="font-semibold text-[#3d4463] dark:text-[#c4c8db] mb-1 mt-2">
                                        Type
                                      </div>
                                      <div className="text-[#1a1d2e] dark:text-[#eef0f6]">
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
                                    className="bg-white dark:bg-[#14161f] dark:text-[#eef0f6] rounded p-2 text-xs"
                                  >
                                    <div className="font-semibold text-[#3d4463] dark:text-[#c4c8db] mb-1">
                                      {field
                                        .replace(/([A-Z])/g, ' $1')
                                        .replace(/^./, (str) =>
                                          str.toUpperCase()
                                        )
                                        .trim()}
                                    </div>
                                    <div className="grid grid-cols-2 gap-2">
                                      <div>
                                        <span className="text-[#6b7194] dark:text-[#8b90a8]">
                                          Old:{' '}
                                        </span>
                                        <span className="text-red-600 line-through">
                                          {String(
                                            activity.oldValues?.[field] ?? 'N/A'
                                          )}
                                        </span>
                                      </div>
                                      <div>
                                        <span className="text-[#6b7194] dark:text-[#8b90a8]">
                                          New:{' '}
                                        </span>
                                        <span className="text-green-600 font-medium">
                                          {String(
                                            activity.newValues?.[field] ?? 'N/A'
                                          )}
                                        </span>
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
                <History className="h-12 w-12 text-[#c9cfd9] dark:text-[#3a3f54] mx-auto mb-3" />
                <p className="text-[#6b7194] dark:text-[#8b90a8]">No history available</p>
                <p className="text-sm text-[#9ca0b8] dark:text-[#5c6180] mt-1">
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
          <div className="bg-white dark:bg-[#14161f] rounded-xl shadow-xl max-w-md w-full mx-4 p-6">
            <h2 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
              Delete Contract
            </h2>
            <p className="text-[#3d4463] dark:text-[#c4c8db] mb-6">
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

      {/* Generate Payments Modal */}
      <GeneratePaymentsModal
        isOpen={showGeneratePaymentsModal}
        onClose={() => setShowGeneratePaymentsModal(false)}
        onSubmit={async (count) => {
          await generatePaymentsMutation.mutateAsync(count);
        }}
        isLoading={generatePaymentsMutation.isPending}
      />
    </div>
  );
};
