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
} from '@/hooks/useContractHooks';
import { usePaymentsByContract } from '@/hooks/usePaymentHooks';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { ErrorMessage } from '@/components/ErrorMessage';
import { RichTextDisplay } from '@/components/common/RichTextDisplay';
import { DocumentList } from '@/components/properties/DocumentList';
import { ContractStatusBadge } from '@/components/contracts/ContractStatusBadge';
import { ChangeContractStatusModal } from '@/components/contracts/ChangeContractStatusModal';
import {
  ArrowLeft,
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
} from 'lucide-react';
import { format, formatDistanceToNow } from 'date-fns';
import { ChangeContractStatusRequest, ContractStatus } from '@/types/contract';
import { PaymentStatusBadge } from '@/components/payments/PaymentStatusBadge';
import { Plus } from 'lucide-react';

export const ContractDetailPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState<
    'overview' | 'payments' | 'documents' | 'history'
  >('overview');
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [showStatusModal, setShowStatusModal] = useState(false);
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
      let aVal: any, bVal: any;

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

  const toggleAuditItem = (itemId: string) => {
    const newExpanded = new Set(expandedAuditItems);
    if (newExpanded.has(itemId)) {
      newExpanded.delete(itemId);
    } else {
      newExpanded.add(itemId);
    }
    setExpandedAuditItems(newExpanded);
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
      <div className="max-w-6xl mx-auto px-4 py-8">
        {/* Header */}
        <div className="bg-white border-b border-gray-200 -mx-4 px-4 py-4 mb-6">
          <div className="flex items-center justify-between max-w-6xl mx-auto">
            <div className="flex items-center gap-4">
              <button
                onClick={() => navigate('/contracts')}
                className="p-2 hover:bg-gray-100 rounded transition-colors"
              >
                <ArrowLeft className="h-5 w-5" />
              </button>
              <div>
                <h1 className="text-xl font-bold text-gray-900">
                  Contract #{contract.identifier}
                </h1>
                <p className="text-xs text-gray-500 uppercase tracking-wide">
                  {contract.contractType.replace('_', ' ')}
                </p>
              </div>
              <div className="ml-2">
                <ContractStatusBadge status={contract.status} />
              </div>
            </div>

            <div className="flex items-center gap-2">
              {!canReopen && (
                <button
                  onClick={() => setShowStatusModal(true)}
                  className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-blue-600 border border-transparent rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors"
                >
                  <RefreshCw className="h-4 w-4" />
                  Change Status
                </button>
              )}
              {canReopen && (
                <button
                  onClick={handleReopen}
                  className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-orange-600 border border-transparent rounded-md hover:bg-orange-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-orange-500 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                  disabled={reopenContractMutation.isPending}
                  title="Reopen this contract to draft status"
                >
                  <RotateCcw className="h-4 w-4" />
                  {reopenContractMutation.isPending
                    ? 'Reopening...'
                    : 'Re-open'}
                </button>
              )}
              {canEdit && (
                <button
                  onClick={() => navigate(`/contracts/${id}/edit`)}
                  className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors"
                >
                  <Edit className="h-4 w-4" />
                  Edit
                </button>
              )}
              <button
                onClick={handleDuplicate}
                className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                disabled={duplicateContractMutation.isPending}
                title="Create a copy of this contract in draft status"
              >
                <Copy className="h-4 w-4" />
                {duplicateContractMutation.isPending
                  ? 'Duplicating...'
                  : 'Duplicate'}
              </button>
              {canDelete && (
                <button
                  onClick={() => setShowDeleteModal(true)}
                  className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-red-700 bg-white border border-gray-300 rounded-md hover:bg-red-50 hover:border-red-300 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500 transition-colors"
                >
                  <Trash2 className="h-4 w-4" />
                  Delete
                </button>
              )}
            </div>
          </div>
        </div>

        {/* Tabs */}
        <div className="border-b border-gray-200 mb-6">
          <div className="flex gap-6">
            <button
              onClick={() => setActiveTab('overview')}
              className={`pb-3 px-1 font-medium transition-colors ${
                activeTab === 'overview'
                  ? 'border-b-2 border-blue-600 text-blue-600'
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              Overview
            </button>
            <button
              onClick={() => setActiveTab('payments')}
              className={`pb-3 px-1 font-medium transition-colors flex items-center gap-2 ${
                activeTab === 'payments'
                  ? 'border-b-2 border-blue-600 text-blue-600'
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              <DollarSign className="h-4 w-4" />
              Payments {payments.length > 0 && `(${payments.length})`}
            </button>
            <button
              onClick={() => setActiveTab('documents')}
              className={`pb-3 px-1 font-medium transition-colors flex items-center gap-2 ${
                activeTab === 'documents'
                  ? 'border-b-2 border-blue-600 text-blue-600'
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              <FileText className="h-4 w-4" />
              Documents {documents.length > 0 && `(${documents.length})`}
            </button>
            <button
              onClick={() => setActiveTab('history')}
              className={`pb-3 px-1 font-medium transition-colors ${
                activeTab === 'history'
                  ? 'border-b-2 border-blue-600 text-blue-600'
                  : 'text-gray-600 hover:text-gray-900'
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
            <div className="bg-white rounded-lg shadow p-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-4">
                Contract Parties
              </h2>
              <div className="space-y-4">
                <div className="flex items-start gap-3">
                  <Home className="h-5 w-5 text-gray-400 mt-1" />
                  <div className="flex-1">
                    <p className="text-sm text-gray-500">Property</p>
                    <button
                      onClick={() =>
                        navigate(`/properties/${contract.property.id}`)
                      }
                      className="font-medium text-blue-600 hover:underline text-left"
                    >
                      {contract.property.street}, {contract.property.city}
                    </button>
                    <p className="text-xs text-gray-500">
                      #{contract.property.identifier}
                    </p>
                  </div>
                </div>
                <div className="flex items-start gap-3">
                  <User className="h-5 w-5 text-gray-400 mt-1" />
                  <div className="flex-1">
                    <p className="text-sm text-gray-500">Tenant</p>
                    <button
                      onClick={() => navigate(`/tenants/${contract.tenant.id}`)}
                      className="font-medium text-blue-600 hover:underline text-left"
                    >
                      {contract.tenant.firstName} {contract.tenant.lastName}
                    </button>
                    <p className="text-xs text-gray-500">
                      #{contract.tenant.identifier}
                    </p>
                  </div>
                </div>
              </div>
            </div>

            {/* Contract Dates */}
            <div className="bg-white rounded-lg shadow p-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-4">
                Important Dates
              </h2>
              <div className="space-y-4">
                <div className="flex items-center gap-3">
                  <Calendar className="h-5 w-5 text-gray-400" />
                  <div>
                    <p className="text-sm text-gray-500">Start Date</p>
                    <p className="font-medium text-gray-900">
                      {format(new Date(contract.startDate), 'MMMM d, yyyy')}
                    </p>
                  </div>
                </div>
                {contract.endDate && (
                  <div className="flex items-center gap-3">
                    <Calendar className="h-5 w-5 text-gray-400" />
                    <div>
                      <p className="text-sm text-gray-500">End Date</p>
                      <p className="font-medium text-gray-900">
                        {format(new Date(contract.endDate), 'MMMM d, yyyy')}
                      </p>
                    </div>
                  </div>
                )}
                {contract.signedDate && (
                  <div className="flex items-center gap-3">
                    <Calendar className="h-5 w-5 text-gray-400" />
                    <div>
                      <p className="text-sm text-gray-500">Signed Date</p>
                      <p className="font-medium text-gray-900">
                        {format(new Date(contract.signedDate), 'MMMM d, yyyy')}
                      </p>
                    </div>
                  </div>
                )}
              </div>
            </div>

            {/* Financial Terms */}
            <div className="bg-white rounded-lg shadow p-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-4">
                Financial Terms
              </h2>
              <div className="space-y-4">
                <div className="flex items-center gap-3">
                  <DollarSign className="h-5 w-5 text-gray-400" />
                  <div>
                    <p className="text-sm text-gray-500">Rent Amount</p>
                    <p className="font-medium text-gray-900">
                      {contract.currency} {contract.rentAmount.toFixed(2)} /{' '}
                      {contract.paymentFrequency.toLowerCase()}
                    </p>
                  </div>
                </div>
                {contract.depositAmount && (
                  <div className="flex items-center gap-3">
                    <DollarSign className="h-5 w-5 text-gray-400" />
                    <div>
                      <p className="text-sm text-gray-500">Deposit</p>
                      <p className="font-medium text-gray-900">
                        {contract.currency} {contract.depositAmount.toFixed(2)}
                      </p>
                    </div>
                  </div>
                )}
                {contract.securityDeposit && (
                  <div className="flex items-center gap-3">
                    <DollarSign className="h-5 w-5 text-gray-400" />
                    <div>
                      <p className="text-sm text-gray-500">Security Deposit</p>
                      <p className="font-medium text-gray-900">
                        {contract.currency}{' '}
                        {contract.securityDeposit.toFixed(2)}
                      </p>
                    </div>
                  </div>
                )}
                {contract.paymentDueDay && (
                  <div>
                    <p className="text-sm text-gray-500">Payment Due Day</p>
                    <p className="font-medium text-gray-900">
                      Day {contract.paymentDueDay} of each period
                    </p>
                  </div>
                )}
              </div>
            </div>

            {/* Additional Terms */}
            <div className="bg-white rounded-lg shadow p-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-4">
                Additional Terms
              </h2>
              <div className="space-y-3">
                <div>
                  <p className="text-sm text-gray-500">Auto-renewal</p>
                  <p className="font-medium text-gray-900">
                    {contract.autoRenewal ? 'Yes' : 'No'}
                  </p>
                </div>
                {contract.renewalNoticeDays && (
                  <div>
                    <p className="text-sm text-gray-500">Renewal Notice</p>
                    <p className="font-medium text-gray-900">
                      {contract.renewalNoticeDays} days
                    </p>
                  </div>
                )}
                {contract.terminationNoticeDays && (
                  <div>
                    <p className="text-sm text-gray-500">Termination Notice</p>
                    <p className="font-medium text-gray-900">
                      {contract.terminationNoticeDays} days
                    </p>
                  </div>
                )}
                {contract.lateFeePercentage && (
                  <div>
                    <p className="text-sm text-gray-500">Late Fee</p>
                    <p className="font-medium text-gray-900">
                      {contract.lateFeePercentage}%
                    </p>
                  </div>
                )}
              </div>
            </div>

            {/* Terms and Conditions */}
            {contract.termsAndConditions && (
              <div className="bg-white rounded-lg shadow p-6 lg:col-span-2">
                <h2 className="text-lg font-semibold text-gray-900 mb-4">
                  Terms and Conditions
                </h2>
                <RichTextDisplay content={contract.termsAndConditions} />
              </div>
            )}

            {/* Notes */}
            {contract.notes && (
              <div className="bg-white rounded-lg shadow p-6 lg:col-span-2">
                <h2 className="text-lg font-semibold text-gray-900 mb-4">
                  Notes
                </h2>
                <RichTextDisplay content={contract.notes} />
              </div>
            )}
          </div>
        )}

        {activeTab === 'payments' && (
          <div className="bg-white rounded-lg shadow p-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-xl font-semibold text-gray-900">
                Payments ({filteredAndSortedPayments.length})
              </h2>
              <button
                onClick={() => navigate(`/payments/new?contractId=${id}`)}
                className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 transition-colors flex items-center gap-2 text-sm"
              >
                <Plus className="h-4 w-4" />
                Add Payment
              </button>
            </div>

            {paymentsLoading ? (
              <LoadingSpinner />
            ) : paymentsError ? (
              <ErrorMessage message="Failed to load payments" />
            ) : payments.length === 0 ? (
              <div className="text-center py-12">
                <DollarSign className="h-12 w-12 text-gray-300 mx-auto mb-3" />
                <p className="text-gray-600 mb-4">
                  No payments for this contract
                </p>
                <button
                  onClick={() => navigate(`/payments/new?contractId=${id}`)}
                  className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 transition-colors inline-flex items-center gap-2"
                >
                  <Plus className="h-4 w-4" />
                  Create First Payment
                </button>
              </div>
            ) : (
              <>
                {/* Search Bar */}
                <div className="mb-4">
                  <div className="relative">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
                    <input
                      type="text"
                      placeholder="Search by payment #, status..."
                      value={paymentsSearchTerm}
                      onChange={(e) => {
                        setPaymentsSearchTerm(e.target.value);
                        setPaymentsCurrentPage(1);
                      }}
                      className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    />
                  </div>
                </div>

                {/* Table */}
                <div className="overflow-x-auto">
                  <table className="min-w-full divide-y divide-gray-200">
                    <thead className="bg-gray-50">
                      <tr>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                          Payment #
                        </th>
                        <th
                          className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
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
                          className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
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
                          className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
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
                          className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
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
                    <tbody className="bg-white divide-y divide-gray-200">
                      {paginatedPayments.length === 0 ? (
                        <tr>
                          <td
                            colSpan={5}
                            className="px-6 py-12 text-center text-gray-500"
                          >
                            No payments found matching your search
                          </td>
                        </tr>
                      ) : (
                        paginatedPayments.map((payment) => (
                          <tr
                            key={payment.id}
                            className="hover:bg-gray-50 cursor-pointer"
                            onClick={() => navigate(`/payments/${payment.id}`)}
                          >
                            <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-blue-600">
                              #{payment.identifier}
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                              {format(new Date(payment.dueDate), 'MMM d, yyyy')}
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap text-sm font-semibold text-gray-900">
                              {payment.currency} {payment.amount.toFixed(2)}
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap">
                              <PaymentStatusBadge status={payment.status} />
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                              {payment.paymentDate
                                ? format(
                                    new Date(payment.paymentDate),
                                    'MMM d, yyyy'
                                  )
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
                  <div className="flex items-center justify-between mt-4 pt-4 border-t border-gray-200">
                    <div className="text-sm text-gray-600">
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
                        className="px-3 py-1 border border-gray-300 rounded text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
                      >
                        Previous
                      </button>
                      <span className="px-3 py-1 text-sm text-gray-600">
                        Page {paymentsCurrentPage} of {paymentsTotalPages}
                      </span>
                      <button
                        onClick={() =>
                          setPaymentsCurrentPage(paymentsCurrentPage + 1)
                        }
                        disabled={paymentsCurrentPage === paymentsTotalPages}
                        className="px-3 py-1 border border-gray-300 rounded text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
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
          <div className="bg-white rounded-lg shadow p-6">
            <DocumentList
              documents={documents}
              onUpload={handleUploadDocument}
              onDelete={handleDeleteDocument}
              isLoading={docsLoading}
              error={docsError}
              isUploading={uploadDocumentMutation.isPending}
              isDeleting={deleteDocumentMutation.isPending}
            />
          </div>
        )}

        {activeTab === 'history' && (
          <div className="bg-white rounded-lg shadow p-6">
            <h2 className="text-xl font-semibold text-gray-900 mb-4">
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
                      className="border border-gray-200 rounded-lg overflow-hidden"
                    >
                      <div
                        className={`flex items-start gap-4 p-4 transition-colors ${
                          hasChanges ? 'cursor-pointer hover:bg-gray-50' : ''
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
                              ? 'bg-green-100'
                              : activity.action === 'UPDATE'
                                ? 'bg-blue-100'
                                : 'bg-red-100'
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
                          <p className="text-sm font-medium text-gray-900">
                            {activity.description}
                          </p>
                          <p className="text-xs text-gray-500 mt-1">
                            {formatDistanceToNow(new Date(activity.timestamp), {
                              addSuffix: true,
                            })}
                          </p>
                          {hasChanges && (
                            <p className="text-xs text-blue-600 mt-1">
                              {isExpanded
                                ? 'Click to hide changes'
                                : 'Click to view changes'}
                            </p>
                          )}
                        </div>
                      </div>

                      {isExpanded && hasChanges && (
                        <div className="bg-gray-50 px-4 py-3 border-t border-gray-200">
                          <h4 className="text-xs font-semibold text-gray-700 mb-2 uppercase">
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
                                  const category = activity.changedFields?.category;
                                  const title = activity.changedFields?.title;
                                  return (
                                    <div
                                      key={field}
                                      className="bg-white rounded p-2 text-xs"
                                    >
                                      <div className="font-semibold text-gray-700 mb-1">
                                        File Name
                                      </div>
                                      <div className="text-gray-900">
                                        {String(value)}
                                      </div>
                                      {title ? (
                                        <>
                                          <div className="font-semibold text-gray-700 mb-1 mt-2">
                                            Title
                                          </div>
                                          <div className="text-gray-900">
                                            {String(title)}
                                          </div>
                                        </>
                                      ) : null}
                                      <div className="font-semibold text-gray-700 mb-1 mt-2">
                                        Type
                                      </div>
                                      <div className="text-gray-900">
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
                                    className="bg-white rounded p-2 text-xs"
                                  >
                                    <div className="font-semibold text-gray-700 mb-1">
                                      {field
                                        .replace(/([A-Z])/g, ' $1')
                                        .replace(/^./, (str) => str.toUpperCase())
                                        .trim()}
                                    </div>
                                    <div className="grid grid-cols-2 gap-2">
                                      <div>
                                        <span className="text-gray-500">
                                          Old:{' '}
                                        </span>
                                        <span className="text-red-600 line-through">
                                          {String(
                                            activity.oldValues?.[field] ?? 'N/A'
                                          )}
                                        </span>
                                      </div>
                                      <div>
                                        <span className="text-gray-500">
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
                <History className="h-12 w-12 text-gray-300 mx-auto mb-3" />
                <p className="text-gray-500">No history available</p>
                <p className="text-sm text-gray-400 mt-1">
                  Changes to this contract will appear here
                </p>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Delete Confirmation Modal */}
      {showDeleteModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-xl max-w-md w-full mx-4 p-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">
              Delete Contract
            </h2>
            <p className="text-gray-700 mb-6">
              Are you sure you want to delete this contract? This action cannot
              be undone.
            </p>
            <div className="flex justify-end gap-3">
              <button
                onClick={() => setShowDeleteModal(false)}
                className="px-4 py-2 border border-gray-300 rounded hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                onClick={handleDelete}
                className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700"
                disabled={deleteContractMutation.isPending}
              >
                {deleteContractMutation.isPending ? 'Deleting...' : 'Delete'}
              </button>
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
