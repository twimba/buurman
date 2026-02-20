import { useState, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  usePayment,
  useDeletePayment,
  useUpdatePayment,
  useMarkPaymentAsPaid,
  usePaymentAuditLog,
  usePaymentDocuments,
  useUploadPaymentDocument,
  useDeletePaymentDocument,
  useRegisterReceival,
  useUpdatePaymentReceival,
  useDeletePaymentReceival,
} from '@/hooks/usePaymentHooks';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { ErrorMessage } from '@/components/ErrorMessage';
import { PaymentStatusBadge } from '@/components/payments/PaymentStatusBadge';
import { PaymentForm } from '@/components/payments/PaymentForm';
import { RichTextDisplay } from '@/components/common/RichTextDisplay';
import { DocumentList } from '@/components/properties/DocumentList';
import { Button, PageHeader } from '@/components/ui';
import { useTeam } from '@/context/TeamContext';
import {
  Edit,
  Pencil,
  X,
  Check,
  ChevronUp,
  ChevronDown,
  Trash2,
  Calendar,
  DollarSign,
  Home,
  User,
  FileText,
  CheckCircle,
  History,
  PlusCircle,
  ArrowDownCircle,
  ArrowUpDown,
  Search,
  ChevronLeft,
  ChevronRight,
} from 'lucide-react';
import { formatDistanceToNow } from 'date-fns';
import { useFormatDate } from '@/hooks/useFormatDate';
import {
  PaymentStatus,
  UpdatePaymentRequest,
  MarkPaidRequest,
  CreatePaymentRequest,
  CreatePaymentReceivalRequest,
  PaymentReceivalResponse,
} from '@/types/payment';
import { getCurrencySymbol } from '@/utils/currencies';
import { MoneyInput } from '@/components/common/MoneyInput';
import { RichTextEditor } from '@/components/common/RichTextEditor';

interface ReceivalsTableProps {
  receivals: PaymentReceivalResponse[];
  symbol: string;
  currency: string;
  formatDate: (d: string) => string;
  canEdit: boolean;
  onEdit: (
    id: string,
    data: { amount: number; receivalDate: string; notes?: string }
  ) => void;
  onDelete: (id: string) => void;
  searchTerm: string;
  onSearchChange: (v: string) => void;
  sortField: 'date' | 'amount';
  sortOrder: 'asc' | 'desc';
  onSort: (field: 'date' | 'amount') => void;
  currentPage: number;
  pageSize: number;
  onPageChange: (page: number) => void;
}

const ReceivalsTable = ({
  receivals,
  symbol,
  currency,
  formatDate,
  canEdit,
  onEdit,
  onDelete,
  searchTerm,
  onSearchChange,
  sortField,
  sortOrder,
  onSort,
  currentPage,
  pageSize,
  onPageChange,
}: ReceivalsTableProps) => {
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editAmount, setEditAmount] = useState('');
  const [editDate, setEditDate] = useState('');
  const [editNotes, setEditNotes] = useState('');

  const startEdit = (r: PaymentReceivalResponse) => {
    setEditingId(r.identifier);
    setEditAmount(r.amount.toString());
    setEditDate(r.receivalDate);
    setEditNotes(r.notes || '');
  };

  const cancelEdit = () => {
    setEditingId(null);
  };

  const saveEdit = () => {
    if (!editingId || !editAmount || parseFloat(editAmount) <= 0) return;
    onEdit(editingId, {
      amount: parseFloat(editAmount),
      receivalDate: editDate,
      notes: editNotes || undefined,
    });
    setEditingId(null);
  };

  const filtered = useMemo(() => {
    let items = [...receivals];
    if (searchTerm) {
      const s = searchTerm.toLowerCase();
      items = items.filter(
        (r) =>
          r.amount.toFixed(2).includes(s) ||
          r.receivalDate.toLowerCase().includes(s) ||
          r.notes?.toLowerCase().includes(s)
      );
    }
    items.sort((a, b) => {
      const mul = sortOrder === 'asc' ? 1 : -1;
      if (sortField === 'amount') return (a.amount - b.amount) * mul;
      return a.receivalDate.localeCompare(b.receivalDate) * mul;
    });
    return items;
  }, [receivals, searchTerm, sortField, sortOrder]);

  const totalPages = Math.ceil(filtered.length / pageSize);
  const paginated = filtered.slice(
    (currentPage - 1) * pageSize,
    currentPage * pageSize
  );

  const inputClass =
    'px-2 py-1 text-sm border border-[#c9cfd9] dark:border-[#3a3f54] rounded bg-white dark:bg-[#1e2130] text-[#1a1d2e] dark:text-[#eef0f6] focus:ring-1 focus:ring-blue-500';

  return (
    <div>
      {/* Search */}
      <div className="mb-4 relative">
        <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-[#9ca0b8] dark:text-[#5c6180]" />
        <input
          type="text"
          placeholder="Search by amount, date, or notes..."
          value={searchTerm}
          onChange={(e) => onSearchChange(e.target.value)}
          className="w-full pl-9 pr-4 py-2 text-sm border border-[#c9cfd9] dark:border-[#3a3f54] rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-[#1e2130] text-[#1a1d2e] dark:text-[#eef0f6]"
        />
      </div>

      {/* Table */}
      <div className="overflow-hidden rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f]">
        <table className="min-w-full divide-y divide-[#edf0f7] dark:divide-[#2a2e3f]">
          <thead className="bg-[#f8f9fc] dark:bg-[#0c0d14]">
            <tr>
              <th
                className="px-4 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
                onClick={() => onSort('date')}
              >
                <div className="flex items-center gap-1">
                  Date
                  <ArrowUpDown className="h-3 w-3" />
                </div>
              </th>
              <th
                className="px-4 py-3 text-right text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
                onClick={() => onSort('amount')}
              >
                <div className="flex items-center justify-end gap-1">
                  Amount
                  <ArrowUpDown className="h-3 w-3" />
                </div>
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider">
                Notes
              </th>
              {canEdit && (
                <th className="px-4 py-3 text-right text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider w-24">
                  Actions
                </th>
              )}
            </tr>
          </thead>
          <tbody className="bg-white dark:bg-[#14161f] divide-y divide-[#edf0f7] dark:divide-[#2a2e3f]">
            {paginated.map((receival) =>
              editingId === receival.identifier ? (
                <tr
                  key={receival.identifier}
                  className="bg-blue-50/50 dark:bg-blue-900/10"
                >
                  <td className="px-4 py-2">
                    <input
                      type="date"
                      value={editDate}
                      onChange={(e) => setEditDate(e.target.value)}
                      className={inputClass}
                    />
                  </td>
                  <td className="px-4 py-2 text-right">
                    <MoneyInput
                      value={editAmount ? parseFloat(editAmount) : undefined}
                      onChange={(val) =>
                        setEditAmount(val !== undefined ? String(val) : '')
                      }
                      currency={currency}
                      className="w-28 text-right"
                    />
                  </td>
                  <td className="px-4 py-2" colSpan={canEdit ? 2 : 1}>
                    <RichTextEditor
                      value={editNotes}
                      onChange={setEditNotes}
                      placeholder="Notes..."
                    />
                    <div className="flex justify-end gap-1 mt-2">
                      <button
                        onClick={saveEdit}
                        className="p-1.5 text-green-600 dark:text-green-400 hover:bg-green-50 dark:hover:bg-[#1e2130] rounded transition-colors"
                        title="Save"
                      >
                        <Check className="h-4 w-4" />
                      </button>
                      <button
                        onClick={cancelEdit}
                        className="p-1.5 text-[#6b7194] dark:text-[#8b90a8] hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] rounded transition-colors"
                        title="Cancel"
                      >
                        <X className="h-4 w-4" />
                      </button>
                    </div>
                  </td>
                </tr>
              ) : (
                <tr
                  key={receival.identifier}
                  className="hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
                >
                  <td className="px-4 py-3 whitespace-nowrap text-sm text-[#1a1d2e] dark:text-[#eef0f6]">
                    {formatDate(receival.receivalDate)}
                  </td>
                  <td className="px-4 py-3 whitespace-nowrap text-sm font-semibold text-right text-green-600 dark:text-green-400">
                    {symbol} {receival.amount.toFixed(2)}
                  </td>
                  <td className="px-4 py-3 text-sm text-[#6b7194] dark:text-[#8b90a8] max-w-xs">
                    {receival.notes ? (
                      <RichTextDisplay
                        content={receival.notes}
                        className="text-sm [&_p]:m-0"
                      />
                    ) : (
                      '-'
                    )}
                  </td>
                  {canEdit && (
                    <td className="px-4 py-3 text-right">
                      <div className="flex justify-end gap-1">
                        <button
                          onClick={() => startEdit(receival)}
                          className="p-1.5 text-[#5c7cfa] hover:bg-blue-50 dark:hover:bg-[#1e2130] rounded transition-colors"
                          title="Edit receival"
                        >
                          <Pencil className="h-4 w-4" />
                        </button>
                        <button
                          onClick={() => onDelete(receival.identifier)}
                          className="p-1.5 text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-[#1e2130] rounded transition-colors"
                          title="Delete receival"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                    </td>
                  )}
                </tr>
              )
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between mt-4 text-sm">
          <span className="text-[#6b7194] dark:text-[#8b90a8]">
            Showing {(currentPage - 1) * pageSize + 1} to{' '}
            {Math.min(currentPage * pageSize, filtered.length)} of{' '}
            {filtered.length}
          </span>
          <div className="flex gap-2">
            <button
              onClick={() => onPageChange(currentPage - 1)}
              disabled={currentPage === 1}
              className="px-3 py-1 border border-[#c9cfd9] dark:border-[#3a3f54] rounded hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-1 text-[#3d4463] dark:text-[#c4c8db]"
            >
              <ChevronLeft className="h-4 w-4" />
              Previous
            </button>
            <button
              onClick={() => onPageChange(currentPage + 1)}
              disabled={currentPage === totalPages}
              className="px-3 py-1 border border-[#c9cfd9] dark:border-[#3a3f54] rounded hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-1 text-[#3d4463] dark:text-[#c4c8db]"
            >
              Next
              <ChevronRight className="h-4 w-4" />
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export const PaymentDetailPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { canEditData } = useTeam();
  const { formatDate } = useFormatDate();
  const [isEditing, setIsEditing] = useState(false);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [showMarkPaidModal, setShowMarkPaidModal] = useState(false);
  const [showReceivalModal, setShowReceivalModal] = useState(false);
  const [activeTab, setActiveTab] = useState<
    'details' | 'receivals' | 'documents' | 'history'
  >('details');
  const [expandedAuditItems, setExpandedAuditItems] = useState<Set<string>>(
    new Set()
  );
  const [isMetadataExpanded, setIsMetadataExpanded] = useState(false);
  const [paymentDate, setPaymentDate] = useState(
    new Date().toISOString().split('T')[0]
  );
  const [markPaidNotes, setMarkPaidNotes] = useState('');

  // Receival form state
  const [receivalAmount, setReceivalAmount] = useState('');
  const [receivalDate, setReceivalDate] = useState(
    new Date().toISOString().split('T')[0]
  );
  const [receivalNotes, setReceivalNotes] = useState('');

  // Receival table state
  const [receivalSearch, setReceivalSearch] = useState('');
  const [receivalSortField, setReceivalSortField] = useState<'date' | 'amount'>(
    'date'
  );
  const [receivalSortOrder, setReceivalSortOrder] = useState<'asc' | 'desc'>(
    'desc'
  );
  const [receivalPage, setReceivalPage] = useState(1);
  const RECEIVALS_PER_PAGE = 10;

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
  const uploadDocumentMutation = useUploadPaymentDocument(id!);
  const deleteDocumentMutation = useDeletePaymentDocument(id!);
  const registerReceivalMutation = useRegisterReceival(id!);
  const updateReceivalMutation = useUpdatePaymentReceival(id!);
  const deleteReceivalMutation = useDeletePaymentReceival(id!);

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

  const handleRegisterReceival = async () => {
    if (!receivalAmount || parseFloat(receivalAmount) <= 0) return;
    const data: CreatePaymentReceivalRequest = {
      amount: parseFloat(receivalAmount),
      receivalDate,
      notes: receivalNotes || undefined,
    };
    try {
      await registerReceivalMutation.mutateAsync(data);
      setShowReceivalModal(false);
      setReceivalAmount('');
      setReceivalDate(new Date().toISOString().split('T')[0]);
      setReceivalNotes('');
    } catch (err) {
      console.error('Failed to register receival:', err);
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

  const canEdit = true;
  const canDelete = payment.status !== PaymentStatus.PAID;
  const canMarkPaid =
    payment.status === PaymentStatus.PENDING ||
    payment.status === PaymentStatus.PARTIALLY_PAID ||
    payment.status === PaymentStatus.OVERDUE;
  const canRegisterReceival = canMarkPaid;
  const symbol = getCurrencySymbol(payment.currency);

  return (
    <div className="min-h-screen bg-background">
      <div className="px-4 py-8">
        {/* Header */}
        <PageHeader
          title={`Payment #${payment.identifier}`}
          subtitle={`Contract #${payment.contract.identifier}`}
          backTo="/payments"
          badge={<PaymentStatusBadge status={payment.status} />}
          actions={
            <>
              {canRegisterReceival && (
                <Button
                  variant="primary"
                  leftIcon={<PlusCircle />}
                  onClick={() => setShowReceivalModal(true)}
                  disabled={!canEditData}
                >
                  Register Receival
                </Button>
              )}
              {canMarkPaid && (
                <Button
                  variant="success"
                  leftIcon={<CheckCircle />}
                  onClick={() => setShowMarkPaidModal(true)}
                  disabled={!canEditData}
                >
                  Mark as Paid
                </Button>
              )}
              {canEdit && !isEditing && (
                <Button
                  variant="secondary"
                  leftIcon={<Edit />}
                  onClick={() => {
                    setActiveTab('details');
                    setIsEditing(true);
                  }}
                  disabled={!canEditData}
                >
                  Edit
                </Button>
              )}
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
        <div className="border-b border-[#e2e6f0] dark:border-[#2a2e3f] mb-6">
          <div className="flex gap-6">
            <button
              onClick={() => setActiveTab('details')}
              className={`pb-3 px-1 font-medium transition-colors ${
                activeTab === 'details'
                  ? 'border-b-2 border-[#5c7cfa] text-blue-600'
                  : 'text-[#6b7194] dark:text-[#8b90a8] hover:text-[#1a1d2e] dark:hover:text-[#c4c8db]'
              }`}
            >
              Details
            </button>
            <button
              onClick={() => setActiveTab('receivals')}
              className={`pb-3 px-1 font-medium transition-colors flex items-center gap-2 ${
                activeTab === 'receivals'
                  ? 'border-b-2 border-[#5c7cfa] text-blue-600'
                  : 'text-[#6b7194] dark:text-[#8b90a8] hover:text-[#1a1d2e] dark:hover:text-[#c4c8db]'
              }`}
            >
              <ArrowDownCircle className="h-4 w-4" />
              Receivals{' '}
              {payment.receivals?.length > 0 && `(${payment.receivals.length})`}
            </button>
            <button
              onClick={() => setActiveTab('documents')}
              className={`pb-3 px-1 font-medium transition-colors flex items-center gap-2 ${
                activeTab === 'documents'
                  ? 'border-b-2 border-[#5c7cfa] text-blue-600'
                  : 'text-[#6b7194] dark:text-[#8b90a8] hover:text-[#1a1d2e] dark:hover:text-[#c4c8db]'
              }`}
            >
              <FileText className="h-4 w-4" />
              Documents {documents.length > 0 && `(${documents.length})`}
            </button>
            <button
              onClick={() => setActiveTab('history')}
              className={`pb-3 px-1 font-medium transition-colors flex items-center gap-2 ${
                activeTab === 'history'
                  ? 'border-b-2 border-[#5c7cfa] text-blue-600'
                  : 'text-[#6b7194] dark:text-[#8b90a8] hover:text-[#1a1d2e] dark:hover:text-[#c4c8db]'
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
            <div className="bg-white dark:bg-[#14161f] rounded-lg shadow p-6">
              <h2 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
                Edit Payment
              </h2>
              <PaymentForm
                payment={payment}
                onSubmit={handleUpdate}
                onCancel={() => setIsEditing(false)}
                isLoading={updatePaymentMutation.isPending}
                contractIdentifier={payment.contract.identifier}
              />
            </div>
          ) : (
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              {/* Payment Details */}
              <div className="bg-white dark:bg-[#14161f] rounded-lg shadow p-6">
                <h2 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
                  Payment Details
                </h2>
                <div className="space-y-4">
                  <div className="flex items-center gap-3">
                    <DollarSign className="h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180]" />
                    <div>
                      <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                        Amount
                      </p>
                      <p className="font-medium text-[#1a1d2e] dark:text-[#eef0f6] text-lg">
                        {symbol} {payment.amount.toFixed(2)}
                      </p>
                    </div>
                  </div>

                  {/* Received / Balance */}
                  {payment.receivedAmount > 0 && (
                    <>
                      <div className="flex items-center gap-3">
                        <ArrowDownCircle className="h-5 w-5 text-green-500" />
                        <div>
                          <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                            Received
                          </p>
                          <p className="font-medium text-green-600 dark:text-green-400 text-lg">
                            {symbol} {payment.receivedAmount.toFixed(2)}
                          </p>
                        </div>
                      </div>
                      <div className="flex items-center gap-3">
                        <DollarSign className="h-5 w-5 text-amber-500" />
                        <div>
                          <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                            Balance
                          </p>
                          <p
                            className={`font-medium text-lg ${
                              payment.balance <= 0
                                ? 'text-green-600 dark:text-green-400'
                                : 'text-amber-600 dark:text-amber-400'
                            }`}
                          >
                            {symbol} {payment.balance.toFixed(2)}
                          </p>
                        </div>
                      </div>
                    </>
                  )}

                  <div className="flex items-center gap-3">
                    <Calendar className="h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180]" />
                    <div>
                      <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                        Due Date
                      </p>
                      <p className="font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                        {formatDate(payment.dueDate)}
                      </p>
                    </div>
                  </div>
                  {payment.paymentDate && (
                    <div className="flex items-center gap-3">
                      <CheckCircle className="h-5 w-5 text-green-600" />
                      <div>
                        <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                          Payment Date
                        </p>
                        <p className="font-medium text-green-700 dark:text-green-300">
                          {formatDate(payment.paymentDate)}
                        </p>
                      </div>
                    </div>
                  )}
                </div>
              </div>

              {/* Contract & Parties */}
              <div className="bg-white dark:bg-[#14161f] rounded-lg shadow p-6">
                <h2 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
                  Related Information
                </h2>
                <div className="space-y-4">
                  <div className="flex items-start gap-3">
                    <FileText className="h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180] mt-1" />
                    <div className="flex-1">
                      <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                        Contract
                      </p>
                      <button
                        onClick={() =>
                          navigate(`/contracts/${payment.contract.identifier}`)
                        }
                        className="font-medium text-primary-500 dark:text-primary-300 hover:underline text-left"
                      >
                        Contract #{payment.contract.identifier}
                      </button>
                    </div>
                  </div>
                  <div className="flex items-start gap-3">
                    <Home className="h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180] mt-1" />
                    <div className="flex-1">
                      <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                        Property
                      </p>
                      <button
                        onClick={() =>
                          navigate(`/properties/${payment.property.identifier}`)
                        }
                        className="font-medium text-primary-500 dark:text-primary-300 hover:underline text-left"
                      >
                        {payment.property.street}, {payment.property.city}
                      </button>
                    </div>
                  </div>
                  <div className="flex items-start gap-3">
                    <User className="h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180] mt-1" />
                    <div className="flex-1">
                      <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                        Tenant
                      </p>
                      <button
                        onClick={() =>
                          navigate(`/tenants/${payment.tenant.identifier}`)
                        }
                        className="font-medium text-primary-500 dark:text-primary-300 hover:underline text-left"
                      >
                        {payment.tenant.firstName} {payment.tenant.lastName}
                      </button>
                    </div>
                  </div>
                </div>
              </div>

              {/* Notes */}
              {payment.notes && (
                <div className="bg-white dark:bg-[#14161f] rounded-lg shadow p-6 lg:col-span-2">
                  <h2 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
                    Notes
                  </h2>
                  <RichTextDisplay content={payment.notes} />
                </div>
              )}

              {/* Metadata */}
              <div className="bg-white dark:bg-[#14161f] rounded-lg shadow p-6 lg:col-span-2">
                <button
                  onClick={() => setIsMetadataExpanded(!isMetadataExpanded)}
                  className="w-full flex items-center justify-between text-left group"
                >
                  <h2 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                    Metadata
                  </h2>
                  {isMetadataExpanded ? (
                    <ChevronUp className="h-5 w-5 text-[#6b7194] dark:text-[#8b90a8] group-hover:text-[#3d4463] dark:group-hover:text-[#c4c8db]" />
                  ) : (
                    <ChevronDown className="h-5 w-5 text-[#6b7194] dark:text-[#8b90a8] group-hover:text-[#3d4463] dark:group-hover:text-[#c4c8db]" />
                  )}
                </button>
                {isMetadataExpanded && (
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm mt-4">
                    <div>
                      <span className="text-[#6b7194] dark:text-[#8b90a8]">
                        Created:
                      </span>{' '}
                      <span className="text-[#1a1d2e] dark:text-[#eef0f6]">
                        {formatDate(payment.createdAt)} at{' '}
                        {new Date(payment.createdAt).toLocaleTimeString()}
                      </span>
                    </div>
                    <div>
                      <span className="text-[#6b7194] dark:text-[#8b90a8]">
                        Last Updated:
                      </span>{' '}
                      <span className="text-[#1a1d2e] dark:text-[#eef0f6]">
                        {formatDate(payment.updatedAt)} at{' '}
                        {new Date(payment.updatedAt).toLocaleTimeString()}
                      </span>
                    </div>
                  </div>
                )}
              </div>
            </div>
          ))}

        {/* Receivals Tab */}
        {activeTab === 'receivals' && (
          <div className="bg-white dark:bg-[#14161f] rounded-lg shadow p-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-xl font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                Receivals ({payment.receivals?.length || 0})
              </h2>
              {canRegisterReceival && (
                <Button
                  variant="primary"
                  leftIcon={<PlusCircle />}
                  onClick={() => setShowReceivalModal(true)}
                  disabled={!canEditData}
                  size="sm"
                >
                  Register Receival
                </Button>
              )}
            </div>

            {/* Balance summary */}
            <div className="grid grid-cols-3 gap-4 mb-6 p-4 bg-[#f8f9fc] dark:bg-[#0c0d14] rounded-lg">
              <div>
                <p className="text-xs text-[#6b7194] dark:text-[#8b90a8]">
                  Total Amount
                </p>
                <p className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                  {symbol} {payment.amount.toFixed(2)}
                </p>
              </div>
              <div>
                <p className="text-xs text-[#6b7194] dark:text-[#8b90a8]">
                  Received
                </p>
                <p className="text-lg font-semibold text-green-600 dark:text-green-400">
                  {symbol} {payment.receivedAmount.toFixed(2)}
                </p>
              </div>
              <div>
                <p className="text-xs text-[#6b7194] dark:text-[#8b90a8]">
                  Balance
                </p>
                <p
                  className={`text-lg font-semibold ${
                    payment.balance <= 0
                      ? 'text-green-600 dark:text-green-400'
                      : 'text-amber-600 dark:text-amber-400'
                  }`}
                >
                  {symbol} {payment.balance.toFixed(2)}
                </p>
              </div>
            </div>

            {payment.receivals?.length > 0 ? (
              <ReceivalsTable
                receivals={payment.receivals}
                symbol={symbol}
                currency={payment.currency}
                formatDate={formatDate}
                canEdit={canEditData}
                onEdit={(receivalId, data) =>
                  updateReceivalMutation.mutate({ receivalId, data })
                }
                onDelete={(receivalId) =>
                  deleteReceivalMutation.mutate(receivalId)
                }
                searchTerm={receivalSearch}
                onSearchChange={(v) => {
                  setReceivalSearch(v);
                  setReceivalPage(1);
                }}
                sortField={receivalSortField}
                sortOrder={receivalSortOrder}
                onSort={(field) => {
                  if (receivalSortField === field) {
                    setReceivalSortOrder((o) => (o === 'asc' ? 'desc' : 'asc'));
                  } else {
                    setReceivalSortField(field);
                    setReceivalSortOrder('desc');
                  }
                }}
                currentPage={receivalPage}
                pageSize={RECEIVALS_PER_PAGE}
                onPageChange={setReceivalPage}
              />
            ) : (
              <div className="text-center py-8">
                <ArrowDownCircle className="h-12 w-12 text-[#c9cfd9] dark:text-[#3a3f54] mx-auto mb-3" />
                <p className="text-[#6b7194] dark:text-[#8b90a8]">
                  No receivals registered yet
                </p>
              </div>
            )}
          </div>
        )}

        {activeTab === 'documents' && (
          <DocumentList
            documents={documents}
            isLoading={docsLoading}
            error={docsError}
            onUpload={async (file, title, notes) => {
              await uploadDocumentMutation.mutateAsync({ file, title, notes });
            }}
            onDelete={async (documentId) => {
              await deleteDocumentMutation.mutateAsync(documentId);
            }}
            isUploading={uploadDocumentMutation.isPending}
            isDeleting={deleteDocumentMutation.isPending}
            readOnly={!canEditData}
          />
        )}

        {activeTab === 'history' && (
          <div className="bg-white dark:bg-[#14161f] rounded-lg shadow p-6">
            <h2 className="text-xl font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
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
                  const activityKey = `${activity.entityType}-${activity.entityIdentifier}-${activity.timestamp}`;
                  const isExpanded = expandedAuditItems.has(activityKey);
                  const hasChanges =
                    activity.action === 'UPDATE' &&
                    activity.changedFields &&
                    Object.keys(activity.changedFields).length > 0;

                  return (
                    <div
                      key={activityKey}
                      className="border border-[#e2e6f0] rounded-lg overflow-hidden"
                    >
                      <div
                        className={`flex items-start gap-4 p-4 transition-colors ${
                          hasChanges
                            ? 'cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]'
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
                            {Object.entries(activity.changedFields!).map(
                              ([field]) => {
                                const oldVal = activity.oldValues?.[field];
                                const newVal = activity.newValues?.[field];
                                const isHtml = (v: unknown) =>
                                  typeof v === 'string' && v.includes('<');

                                const renderReceival = (
                                  r: Record<string, unknown>
                                ) => {
                                  const amt =
                                    r.amount != null
                                      ? Number(r.amount).toFixed(2)
                                      : '?';
                                  const date = r.receivalDate
                                    ? formatDate(String(r.receivalDate))
                                    : '';
                                  return `${amt} on ${date}`;
                                };

                                const renderVal = (
                                  v: unknown,
                                  color: string,
                                  extra?: string
                                ) => {
                                  if (v == null) {
                                    return (
                                      <span
                                        className={`${color} ${extra || ''}`}
                                      >
                                        N/A
                                      </span>
                                    );
                                  }
                                  if (Array.isArray(v)) {
                                    if (v.length === 0) {
                                      return (
                                        <span
                                          className={`${color} ${extra || ''}`}
                                        >
                                          None
                                        </span>
                                      );
                                    }
                                    return (
                                      <ul
                                        className={`${color} ${extra || ''} list-disc list-inside`}
                                      >
                                        {v.map((item, i) => (
                                          <li key={i}>
                                            {typeof item === 'object' &&
                                            item !== null &&
                                            'amount' in item
                                              ? renderReceival(
                                                  item as Record<
                                                    string,
                                                    unknown
                                                  >
                                                )
                                              : typeof item === 'object' &&
                                                  item !== null &&
                                                  ('name' in item ||
                                                    'identifier' in item)
                                                ? String(
                                                    (
                                                      item as Record<
                                                        string,
                                                        unknown
                                                      >
                                                    ).name ??
                                                      (
                                                        item as Record<
                                                          string,
                                                          unknown
                                                        >
                                                      ).identifier ??
                                                      JSON.stringify(item)
                                                  )
                                                : String(item)}
                                          </li>
                                        ))}
                                      </ul>
                                    );
                                  }
                                  if (typeof v === 'object' && v !== null) {
                                    const obj = v as Record<string, unknown>;
                                    if (
                                      'amount' in obj &&
                                      'receivalDate' in obj
                                    ) {
                                      return (
                                        <span
                                          className={`${color} ${extra || ''}`}
                                        >
                                          {renderReceival(obj)}
                                        </span>
                                      );
                                    }
                                    if (
                                      'name' in obj ||
                                      'street' in obj ||
                                      'identifier' in obj
                                    ) {
                                      return (
                                        <span
                                          className={`${color} ${extra || ''}`}
                                        >
                                          {String(
                                            obj.name ??
                                              obj.street ??
                                              obj.identifier
                                          )}
                                        </span>
                                      );
                                    }
                                    return (
                                      <span
                                        className={`${color} ${extra || ''}`}
                                      >
                                        {Object.entries(obj)
                                          .filter(
                                            ([k]) =>
                                              ![
                                                'id',
                                                'teamId',
                                                'createdAt',
                                                'updatedAt',
                                                'createdBy',
                                                'updatedBy',
                                                'deletedAt',
                                              ].includes(k)
                                          )
                                          .map(([k, val]) => `${k}: ${val}`)
                                          .join(', ')}
                                      </span>
                                    );
                                  }
                                  if (isHtml(v)) {
                                    return (
                                      <div
                                        className={`${color} ${extra || ''} mt-1`}
                                      >
                                        <RichTextDisplay
                                          content={String(v)}
                                          className="text-xs [&_p]:m-0"
                                        />
                                      </div>
                                    );
                                  }
                                  return (
                                    <span className={`${color} ${extra || ''}`}>
                                      {String(v)}
                                    </span>
                                  );
                                };

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
                                        {renderVal(
                                          oldVal,
                                          'text-red-600',
                                          'line-through'
                                        )}
                                      </div>
                                      <div>
                                        <span className="text-[#6b7194] dark:text-[#8b90a8]">
                                          New:{' '}
                                        </span>
                                        {renderVal(
                                          newVal,
                                          'text-green-600',
                                          'font-medium'
                                        )}
                                      </div>
                                    </div>
                                  </div>
                                );
                              }
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
                <History className="h-12 w-12 text-[#c9cfd9] dark:text-[#3a3f54] mx-auto mb-3" />
                <p className="text-[#6b7194] dark:text-[#8b90a8]">
                  No history available
                </p>
                <p className="text-sm text-[#9ca0b8] dark:text-[#5c6180] mt-1">
                  Changes to this payment will appear here
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
              Delete Payment
            </h2>
            <p className="text-[#3d4463] dark:text-[#c4c8db] mb-6">
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
        <div
          className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50"
          onKeyDown={(e) => {
            if ((e.metaKey || e.ctrlKey) && e.key === 'Enter') {
              e.preventDefault();
              handleMarkPaid();
            }
          }}
        >
          <div className="bg-white dark:bg-[#14161f] rounded-xl shadow-xl max-w-md w-full mx-4 p-6">
            <h2 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
              Mark Payment as Paid
            </h2>
            {payment.balance > 0 && (
              <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] mb-4">
                This will register a receival for the remaining balance of{' '}
                <span className="font-semibold">
                  {symbol} {payment.balance.toFixed(2)}
                </span>{' '}
                and mark the payment as fully paid.
              </p>
            )}
            <div className="space-y-4 mb-6">
              <div>
                <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-2">
                  Payment Date <span className="text-red-500">*</span>
                </label>
                <input
                  type="date"
                  value={paymentDate}
                  onChange={(e) => setPaymentDate(e.target.value)}
                  className="w-full px-3 py-2 border border-[#c9cfd9] dark:border-[#3a3f54] dark:bg-[#1e2130] dark:text-[#eef0f6] rounded-md"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-2">
                  Notes (Optional)
                </label>
                <RichTextEditor
                  value={markPaidNotes}
                  onChange={setMarkPaidNotes}
                  placeholder="Add any notes about this payment..."
                  onSubmit={handleMarkPaid}
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

      {/* Register Receival Modal */}
      {showReceivalModal && (
        <div
          className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50"
          onKeyDown={(e) => {
            if ((e.metaKey || e.ctrlKey) && e.key === 'Enter') {
              e.preventDefault();
              handleRegisterReceival();
            }
          }}
        >
          <div className="bg-white dark:bg-[#14161f] rounded-xl shadow-xl max-w-md w-full mx-4 p-6">
            <h2 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
              Register Receival
            </h2>
            <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] mb-4">
              Remaining balance:{' '}
              <span className="font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                {symbol} {payment.balance.toFixed(2)}
              </span>
            </p>
            <div className="space-y-4 mb-6">
              <div>
                <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-2">
                  Amount <span className="text-red-500">*</span>
                </label>
                <MoneyInput
                  value={
                    receivalAmount ? parseFloat(receivalAmount) : undefined
                  }
                  onChange={(val) =>
                    setReceivalAmount(val !== undefined ? String(val) : '')
                  }
                  currency={payment.currency}
                  max={payment.balance}
                  placeholder={`Max: ${payment.balance.toFixed(2)}`}
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-2">
                  Receival Date <span className="text-red-500">*</span>
                </label>
                <input
                  type="date"
                  value={receivalDate}
                  onChange={(e) => setReceivalDate(e.target.value)}
                  className="w-full px-3 py-2 border border-[#c9cfd9] dark:border-[#3a3f54] dark:bg-[#1e2130] dark:text-[#eef0f6] rounded-md"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-2">
                  Notes (Optional)
                </label>
                <RichTextEditor
                  value={receivalNotes}
                  onChange={setReceivalNotes}
                  placeholder="Add notes about this receival..."
                  onSubmit={handleRegisterReceival}
                />
              </div>
            </div>
            <div className="flex justify-end gap-3">
              <Button
                variant="secondary"
                onClick={() => setShowReceivalModal(false)}
              >
                Cancel
              </Button>
              <Button
                variant="primary"
                leftIcon={<PlusCircle />}
                onClick={handleRegisterReceival}
                isLoading={registerReceivalMutation.isPending}
                disabled={!receivalAmount || parseFloat(receivalAmount) <= 0}
              >
                Register
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
