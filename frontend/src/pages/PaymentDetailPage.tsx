import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  usePayment,
  useDeletePayment,
  useUpdatePayment,
  useMarkPaymentAsPaid,
  usePaymentAuditLog,
  usePaymentDocuments,
  useDeletePaymentDocument,
} from '@/hooks/usePaymentHooks';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { ErrorMessage } from '@/components/ErrorMessage';
import { PaymentStatusBadge } from '@/components/payments/PaymentStatusBadge';
import { PaymentForm } from '@/components/payments/PaymentForm';
import { RichTextDisplay } from '@/components/common/RichTextDisplay';
import { Button, PageHeader } from '@/components/ui';
import {
  Edit,
  Trash2,
  Calendar,
  DollarSign,
  Home,
  User,
  FileText,
  CheckCircle,
  History,
} from 'lucide-react';
import { format, formatDistanceToNow } from 'date-fns';
import {
  PaymentStatus,
  UpdatePaymentRequest,
  MarkPaidRequest,
  CreatePaymentRequest,
} from '@/types/payment';

export const PaymentDetailPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [isEditing, setIsEditing] = useState(false);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [showMarkPaidModal, setShowMarkPaidModal] = useState(false);
  const [activeTab, setActiveTab] = useState<
    'details' | 'documents' | 'history'
  >('details');
  const [expandedAuditItems, setExpandedAuditItems] = useState<Set<string>>(
    new Set()
  );
  const [paymentDate, setPaymentDate] = useState(
    new Date().toISOString().split('T')[0]
  );
  const [markPaidNotes, setMarkPaidNotes] = useState('');

  const { data: payment, isLoading, error } = usePayment(id);
  const {
    data: auditLog = [],
    isLoading: auditLoading,
    error: auditError,
  } = usePaymentAuditLog(id);
  const {
    data: documents = [],
    isLoading: docsLoading,
    error: docsError,
  } = usePaymentDocuments(id);
  const deletePaymentMutation = useDeletePayment();
  const updatePaymentMutation = useUpdatePayment(id!);
  const markPaidMutation = useMarkPaymentAsPaid();
  const deleteDocumentMutation = useDeletePaymentDocument(id!);

  const handleDelete = async () => {
    if (!id) return;
    try {
      await deletePaymentMutation.mutateAsync(id);
      navigate('/payments');
    } catch (err) {
      console.error('Failed to delete payment:', err);
    }
  };

  const handleUpdate = async (data: CreatePaymentRequest) => {
    const updateData: UpdatePaymentRequest = {
      amount: data.amount,
      currency: data.currency,
      dueDate: data.dueDate,
      notes: data.notes,
    };
    await updatePaymentMutation.mutateAsync(updateData);
    setIsEditing(false);
  };

  const handleMarkPaid = async () => {
    if (!id) return;
    const data: MarkPaidRequest = {
      paymentDate,
      notes: markPaidNotes || undefined,
    };
    try {
      await markPaidMutation.mutateAsync({ id, data });
      setShowMarkPaidModal(false);
    } catch (err) {
      console.error('Failed to mark payment as paid:', err);
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <LoadingSpinner />
      </div>
    );
  }

  if (error || !payment) {
    return (
      <div className="min-h-screen bg-background p-8">
        <ErrorMessage message="Failed to load payment" />
      </div>
    );
  }

  const canEdit = true; // Allow editing all payments
  const canDelete = payment.status !== PaymentStatus.PAID;
  const canMarkPaid = payment.status === PaymentStatus.PENDING;

  return (
    <div className="min-h-screen bg-background">
      <div className="max-w-6xl mx-auto px-4 py-8">
        {/* Header */}
        <PageHeader
          title={`Payment #${payment.identifier}`}
          subtitle={`Contract #${payment.contract.identifier}`}
          backTo="/payments"
          badge={<PaymentStatusBadge status={payment.status} />}
          actions={
            <>
              {canMarkPaid && (
                <Button
                  variant="success"
                  leftIcon={<CheckCircle />}
                  onClick={() => setShowMarkPaidModal(true)}
                >
                  Mark as Paid
                </Button>
              )}
              {canEdit && !isEditing && (
                <Button
                  variant="secondary"
                  leftIcon={<Edit />}
                  onClick={() => setIsEditing(true)}
                >
                  Edit
                </Button>
              )}
              {canDelete && (
                <Button
                  variant="danger"
                  leftIcon={<Trash2 />}
                  onClick={() => setShowDeleteModal(true)}
                >
                  Delete
                </Button>
              )}
            </>
          }
        />

        {/* Tabs */}
        <div className="border-b border-gray-200 mb-6">
          <div className="flex gap-6">
            <button
              onClick={() => setActiveTab('details')}
              className={`pb-3 px-1 font-medium transition-colors ${
                activeTab === 'details'
                  ? 'border-b-2 border-blue-600 text-blue-600'
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              Details
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
              className={`pb-3 px-1 font-medium transition-colors flex items-center gap-2 ${
                activeTab === 'history'
                  ? 'border-b-2 border-blue-600 text-blue-600'
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              <History className="h-4 w-4" />
              History {auditLog.length > 0 && `(${auditLog.length})`}
            </button>
          </div>
        </div>

        {/* Content */}
        {activeTab === 'details' &&
          (isEditing ? (
            <div className="bg-white rounded-lg shadow p-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-4">
                Edit Payment
              </h2>
              <PaymentForm
                payment={payment}
                onSubmit={handleUpdate}
                onCancel={() => setIsEditing(false)}
                isLoading={updatePaymentMutation.isPending}
                contractId={payment.contract.id}
              />
            </div>
          ) : (
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              {/* Payment Details */}
              <div className="bg-white rounded-lg shadow p-6">
                <h2 className="text-lg font-semibold text-gray-900 mb-4">
                  Payment Details
                </h2>
                <div className="space-y-4">
                  <div className="flex items-center gap-3">
                    <DollarSign className="h-5 w-5 text-gray-400" />
                    <div>
                      <p className="text-sm text-gray-500">Amount</p>
                      <p className="font-medium text-gray-900 text-lg">
                        {payment.currency} {payment.amount.toFixed(2)}
                      </p>
                    </div>
                  </div>
                  <div className="flex items-center gap-3">
                    <Calendar className="h-5 w-5 text-gray-400" />
                    <div>
                      <p className="text-sm text-gray-500">Due Date</p>
                      <p className="font-medium text-gray-900">
                        {format(new Date(payment.dueDate), 'MMMM d, yyyy')}
                      </p>
                    </div>
                  </div>
                  {payment.paymentDate && (
                    <div className="flex items-center gap-3">
                      <CheckCircle className="h-5 w-5 text-green-600" />
                      <div>
                        <p className="text-sm text-gray-500">Payment Date</p>
                        <p className="font-medium text-green-700">
                          {format(
                            new Date(payment.paymentDate),
                            'MMMM d, yyyy'
                          )}
                        </p>
                      </div>
                    </div>
                  )}
                </div>
              </div>

              {/* Contract & Parties */}
              <div className="bg-white rounded-lg shadow p-6">
                <h2 className="text-lg font-semibold text-gray-900 mb-4">
                  Related Information
                </h2>
                <div className="space-y-4">
                  <div className="flex items-start gap-3">
                    <FileText className="h-5 w-5 text-gray-400 mt-1" />
                    <div className="flex-1">
                      <p className="text-sm text-gray-500">Contract</p>
                      <button
                        onClick={() =>
                          navigate(`/contracts/${payment.contract.id}`)
                        }
                        className="font-medium text-blue-600 hover:underline text-left"
                      >
                        Contract #{payment.contract.identifier}
                      </button>
                    </div>
                  </div>
                  <div className="flex items-start gap-3">
                    <Home className="h-5 w-5 text-gray-400 mt-1" />
                    <div className="flex-1">
                      <p className="text-sm text-gray-500">Property</p>
                      <button
                        onClick={() =>
                          navigate(`/properties/${payment.property.id}`)
                        }
                        className="font-medium text-blue-600 hover:underline text-left"
                      >
                        {payment.property.street}, {payment.property.city}
                      </button>
                    </div>
                  </div>
                  <div className="flex items-start gap-3">
                    <User className="h-5 w-5 text-gray-400 mt-1" />
                    <div className="flex-1">
                      <p className="text-sm text-gray-500">Tenant</p>
                      <button
                        onClick={() =>
                          navigate(`/tenants/${payment.tenant.id}`)
                        }
                        className="font-medium text-blue-600 hover:underline text-left"
                      >
                        {payment.tenant.firstName} {payment.tenant.lastName}
                      </button>
                    </div>
                  </div>
                </div>
              </div>

              {/* Notes */}
              {payment.notes && (
                <div className="bg-white rounded-lg shadow p-6 lg:col-span-2">
                  <h2 className="text-lg font-semibold text-gray-900 mb-4">
                    Notes
                  </h2>
                  <RichTextDisplay content={payment.notes} />
                </div>
              )}

              {/* Metadata */}
              <div className="bg-white rounded-lg shadow p-6 lg:col-span-2">
                <h2 className="text-lg font-semibold text-gray-900 mb-4">
                  Metadata
                </h2>
                <div className="grid grid-cols-2 gap-4 text-sm">
                  <div>
                    <span className="text-gray-600">Created:</span>{' '}
                    <span className="text-gray-900">
                      {new Date(payment.createdAt).toLocaleDateString()} at{' '}
                      {new Date(payment.createdAt).toLocaleTimeString()}
                    </span>
                  </div>
                  <div>
                    <span className="text-gray-600">Last Updated:</span>{' '}
                    <span className="text-gray-900">
                      {new Date(payment.updatedAt).toLocaleDateString()} at{' '}
                      {new Date(payment.updatedAt).toLocaleTimeString()}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          ))}

        {activeTab === 'documents' && (
          <div className="bg-white rounded-lg shadow p-6">
            <h2 className="text-xl font-semibold text-gray-900 mb-4">
              Documents ({documents.length})
            </h2>
            {docsLoading ? (
              <LoadingSpinner />
            ) : docsError ? (
              <ErrorMessage message="Failed to load documents" />
            ) : documents.length === 0 ? (
              <div className="text-center py-12">
                <FileText className="h-12 w-12 text-gray-300 mx-auto mb-3" />
                <p className="text-gray-600">
                  No documents attached to this payment
                </p>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {documents.map((doc) => (
                  <div
                    key={doc.id}
                    className="border border-gray-200 rounded-lg p-4 hover:border-blue-300 transition-colors"
                  >
                    <div className="flex items-start gap-3">
                      <FileText className="h-5 w-5 text-gray-400 mt-1" />
                      <div className="flex-1 min-w-0">
                        <p className="font-medium text-gray-900 truncate">
                          {doc.title || doc.fileName}
                        </p>
                        <p className="text-xs text-gray-500 truncate">
                          {doc.fileName}
                        </p>
                        <div className="flex gap-2 mt-2">
                          <a
                            href={doc.downloadUrl || undefined}
                            download
                            className="text-xs text-blue-600 hover:underline"
                          >
                            Download
                          </a>
                          <button
                            onClick={() =>
                              deleteDocumentMutation.mutate(doc.id)
                            }
                            className="text-xs text-red-600 hover:underline"
                          >
                            Delete
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {activeTab === 'history' && (
          <div className="bg-white rounded-lg shadow p-6">
            <h2 className="text-xl font-semibold text-gray-900 mb-4">
              Payment History
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
                            {Object.entries(activity.changedFields!).map(
                              ([field]) => (
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
                              )
                            )}
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
                  Changes to this payment will appear here
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
              Delete Payment
            </h2>
            <p className="text-gray-700 mb-6">
              Are you sure you want to delete this payment? This action cannot
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
                isLoading={deletePaymentMutation.isPending}
              >
                Delete
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Mark Paid Modal */}
      {showMarkPaidModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-xl max-w-md w-full mx-4 p-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">
              Mark Payment as Paid
            </h2>
            <div className="space-y-4 mb-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Payment Date <span className="text-red-500">*</span>
                </label>
                <input
                  type="date"
                  value={paymentDate}
                  onChange={(e) => setPaymentDate(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Notes (Optional)
                </label>
                <textarea
                  value={markPaidNotes}
                  onChange={(e) => setMarkPaidNotes(e.target.value)}
                  rows={3}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md"
                  placeholder="Add any notes about this payment..."
                />
              </div>
            </div>
            <div className="flex justify-end gap-3">
              <Button
                variant="secondary"
                onClick={() => setShowMarkPaidModal(false)}
              >
                Cancel
              </Button>
              <Button
                variant="success"
                leftIcon={<CheckCircle />}
                onClick={handleMarkPaid}
                isLoading={markPaidMutation.isPending}
              >
                Mark as Paid
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
