import { useState } from 'react';
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
  RefreshCw,
  RotateCcw,
  Copy,
} from 'lucide-react';
import { format, formatDistanceToNow } from 'date-fns';
import { ChangeContractStatusRequest, ContractStatus } from '@/types/contract';

export const ContractDetailPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState<
    'overview' | 'documents' | 'history'
  >('overview');
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [showStatusModal, setShowStatusModal] = useState(false);
  const [expandedAuditItems, setExpandedAuditItems] = useState<Set<string>>(
    new Set()
  );

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

  const deleteContractMutation = useDeleteContract();
  const uploadDocumentMutation = useUploadContractDocument(id!);
  const deleteDocumentMutation = useDeleteContractDocument(id!);
  const changeStatusMutation = useChangeContractStatus(id!);
  const reopenContractMutation = useReopenContract(id!);
  const duplicateContractMutation = useDuplicateContract();

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
            <h2 className="text-lg font-semibold text-gray-900 mb-4">
              Audit History
            </h2>
            {auditLoading ? (
              <LoadingSpinner />
            ) : auditError ? (
              <ErrorMessage message="Failed to load audit history" />
            ) : auditLog.length === 0 ? (
              <p className="text-gray-600">No history available</p>
            ) : (
              <div className="space-y-3">
                {auditLog.map((item) => {
                  const isExpanded = expandedAuditItems.has(item.id);
                  const hasChanges =
                    item.changedFields &&
                    Object.keys(item.changedFields).length > 0;

                  return (
                    <div
                      key={item.id}
                      className="border border-gray-200 rounded-lg"
                    >
                      <div
                        className={`p-4 ${
                          hasChanges ? 'cursor-pointer hover:bg-gray-50' : ''
                        }`}
                        onClick={() => hasChanges && toggleAuditItem(item.id)}
                      >
                        <div className="flex items-start justify-between">
                          <div className="flex-1">
                            <div className="flex items-center gap-2">
                              <span className="font-medium text-gray-900">
                                {item.action}
                              </span>
                              <span className="text-gray-500">·</span>
                              <span className="text-sm text-gray-600">
                                {item.entityName || 'Contract'}
                              </span>
                            </div>
                            <p className="text-sm text-gray-600 mt-1">
                              {item.description}
                            </p>
                            <div className="flex items-center gap-2 mt-2 text-xs text-gray-500">
                              <span>{item.userName || 'System'}</span>
                              <span>·</span>
                              <span
                                title={format(
                                  new Date(item.timestamp),
                                  "PPpp 'UTC'"
                                )}
                                className="cursor-help"
                              >
                                {formatDistanceToNow(new Date(item.timestamp), {
                                  addSuffix: true,
                                })}
                              </span>
                            </div>
                          </div>
                          {hasChanges && (
                            <ChevronDown
                              className={`h-5 w-5 text-gray-400 transition-transform ${
                                isExpanded ? 'rotate-180' : ''
                              }`}
                            />
                          )}
                        </div>
                      </div>
                      {isExpanded && hasChanges && (
                        <div className="border-t border-gray-200 p-4 bg-gray-50">
                          <h4 className="text-sm font-medium text-gray-900 mb-2">
                            Changes
                          </h4>
                          <div className="space-y-2">
                            {Object.entries(item.changedFields!)
                              .filter(([field]) => field !== 'updatedAt')
                              .map(([field, value]) => {
                                // Skip internal fields for document operations
                                if (field === 'documentCount') return null;

                                // Special handling for document operations
                                if (
                                  field === 'documentAdded' ||
                                  field === 'documentRemoved'
                                ) {
                                  const category = item.changedFields?.category;
                                  const title = item.changedFields?.title;
                                  return (
                                    <div key={field} className="text-sm">
                                      <div className="font-medium text-gray-700 mb-1">
                                        File Name
                                      </div>
                                      <div className="ml-4 text-gray-900">
                                        {String(value)}
                                      </div>
                                      {title ? (
                                        <>
                                          <div className="font-medium text-gray-700 mb-1 mt-2">
                                            Title
                                          </div>
                                          <div className="ml-4 text-gray-900">
                                            {String(title)}
                                          </div>
                                        </>
                                      ) : null}
                                      <div className="font-medium text-gray-700 mb-1 mt-2">
                                        Type
                                      </div>
                                      <div className="ml-4 text-gray-900">
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
                                  (item.changedFields?.documentAdded ||
                                    item.changedFields?.documentRemoved)
                                ) {
                                  return null;
                                }

                                const isRichText =
                                  field === 'notes' ||
                                  field === 'termsAndConditions' ||
                                  field === 'statusChangeReason';
                                const oldValue = item.oldValues?.[field];
                                const newValue = item.newValues?.[field];

                                // For statusChangeReason, only show the new value (it's metadata, not a change)
                                if (field === 'statusChangeReason') {
                                  return (
                                    <div key={field} className="text-sm">
                                      <span className="font-medium text-gray-700">
                                        Reason:
                                      </span>
                                      <div className="ml-4 mt-1">
                                        <div className="text-gray-900">
                                          {value &&
                                          typeof value === 'string' ? (
                                            <RichTextDisplay content={value} />
                                          ) : (
                                            <span>{String(value ?? '')}</span>
                                          )}
                                        </div>
                                      </div>
                                    </div>
                                  );
                                }

                                return (
                                  <div key={field} className="text-sm">
                                    <span className="font-medium text-gray-700">
                                      {field}:
                                    </span>
                                    <div className="ml-4 mt-1">
                                      {oldValue !== undefined && (
                                        <div className="text-red-600">
                                          <div className="flex gap-1">
                                            <span>-</span>
                                            {isRichText && oldValue ? (
                                              <div className="flex-1">
                                                <RichTextDisplay
                                                  content={String(oldValue)}
                                                />
                                              </div>
                                            ) : (
                                              <span>
                                                {String(oldValue ?? '')}
                                              </span>
                                            )}
                                          </div>
                                        </div>
                                      )}
                                      {newValue !== undefined && (
                                        <div className="text-green-600">
                                          <div className="flex gap-1">
                                            <span>+</span>
                                            {isRichText && newValue ? (
                                              <div className="flex-1">
                                                <RichTextDisplay
                                                  content={String(newValue)}
                                                />
                                              </div>
                                            ) : (
                                              <span>
                                                {String(newValue ?? '')}
                                              </span>
                                            )}
                                          </div>
                                        </div>
                                      )}
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
