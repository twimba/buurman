import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  usePayment,
  useDeletePayment,
  useUpdatePayment,
  useMarkPaymentAsPaid,
} from '@/hooks/usePaymentHooks';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { ErrorMessage } from '@/components/ErrorMessage';
import { PaymentStatusBadge } from '@/components/payments/PaymentStatusBadge';
import { PaymentForm } from '@/components/payments/PaymentForm';
import {
  ArrowLeft,
  Edit,
  Trash2,
  Receipt,
  Calendar,
  DollarSign,
  Home,
  User,
  FileText,
  CheckCircle,
  X,
} from 'lucide-react';
import { format } from 'date-fns';
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
  const [paymentDate, setPaymentDate] = useState(
    new Date().toISOString().split('T')[0]
  );
  const [markPaidNotes, setMarkPaidNotes] = useState('');

  const { data: payment, isLoading, error } = usePayment(id);
  const deletePaymentMutation = useDeletePayment();
  const updatePaymentMutation = useUpdatePayment(id!);
  const markPaidMutation = useMarkPaymentAsPaid();

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

  const canEdit = payment.status !== PaymentStatus.PAID;
  const canDelete = payment.status !== PaymentStatus.PAID;
  const canMarkPaid = payment.status === PaymentStatus.PENDING;

  return (
    <div className="min-h-screen bg-background">
      <div className="max-w-6xl mx-auto px-4 py-8">
        {/* Header */}
        <div className="bg-white border-b border-gray-200 -mx-4 px-4 py-4 mb-6">
          <div className="flex items-center justify-between max-w-6xl mx-auto">
            <div className="flex items-center gap-4">
              <button
                onClick={() => navigate('/payments')}
                className="p-2 hover:bg-gray-100 rounded transition-colors"
              >
                <ArrowLeft className="h-5 w-5" />
              </button>
              <div>
                <h1 className="text-xl font-bold text-gray-900">
                  Payment #{payment.identifier}
                </h1>
                <p className="text-xs text-gray-500">
                  Contract #{payment.contract.identifier}
                </p>
              </div>
              <div className="ml-2">
                <PaymentStatusBadge status={payment.status} />
              </div>
            </div>

            <div className="flex items-center gap-2">
              {canMarkPaid && (
                <button
                  onClick={() => setShowMarkPaidModal(true)}
                  className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-green-600 border border-transparent rounded-md hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500 transition-colors"
                >
                  <CheckCircle className="h-4 w-4" />
                  Mark as Paid
                </button>
              )}
              {canEdit && !isEditing && (
                <button
                  onClick={() => setIsEditing(true)}
                  className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors"
                >
                  <Edit className="h-4 w-4" />
                  Edit
                </button>
              )}
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

        {/* Content */}
        {isEditing ? (
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
                        {format(new Date(payment.paymentDate), 'MMMM d, yyyy')}
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
                      onClick={() => navigate(`/tenants/${payment.tenant.id}`)}
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
                <p className="text-gray-700 whitespace-pre-wrap">
                  {payment.notes}
                </p>
              </div>
            )}

            {/* Metadata */}
            <div className="bg-white rounded-lg shadow p-6 lg:col-span-2">
              <h2 className="text-lg font-semibold text-gray-900 mb-4">
                Record Information
              </h2>
              <div className="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <p className="text-gray-500">Created</p>
                  <p className="text-gray-900">
                    {format(new Date(payment.createdAt), 'PPpp')}
                  </p>
                </div>
                <div>
                  <p className="text-gray-500">Last Updated</p>
                  <p className="text-gray-900">
                    {format(new Date(payment.updatedAt), 'PPpp')}
                  </p>
                </div>
              </div>
            </div>
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
              <button
                onClick={() => setShowDeleteModal(false)}
                className="px-4 py-2 border border-gray-300 rounded hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                onClick={handleDelete}
                className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700"
                disabled={deletePaymentMutation.isPending}
              >
                {deletePaymentMutation.isPending ? 'Deleting...' : 'Delete'}
              </button>
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
              <button
                onClick={() => setShowMarkPaidModal(false)}
                className="px-4 py-2 border border-gray-300 rounded hover:bg-gray-50 flex items-center gap-2"
              >
                <X className="h-4 w-4" />
                Cancel
              </button>
              <button
                onClick={handleMarkPaid}
                className="px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700 flex items-center gap-2"
                disabled={markPaidMutation.isPending}
              >
                <CheckCircle className="h-4 w-4" />
                {markPaidMutation.isPending ? 'Saving...' : 'Mark as Paid'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
