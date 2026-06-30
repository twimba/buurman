import { useState, useMemo, useEffect } from 'react';
import {
  useParams,
  useNavigate,
  useLocation,
  useSearchParams,
} from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useTabState } from '@/hooks/useTabState';
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
import { ErrorMessage } from '@/components/ErrorMessage';
import { PaymentStatusBadge } from '@/components/payments/PaymentStatusBadge';
import { PaymentForm } from '@/components/payments/PaymentForm';
import {
  Button,
  LoadingSpinner,
  PageHeader,
  RichTextDisplay,
  RichTextEditor,
} from '@buurman/ui';
import { DocumentList } from '@/components/properties/DocumentList';
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
  Eye,
} from 'lucide-react';
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
  const { t } = useTranslation('payments');
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editAmount, setEditAmount] = useState('');
  const [editDate, setEditDate] = useState('');
  const [editNotes, setEditNotes] = useState('');

  const startEdit = (r: PaymentReceivalResponse) => {
    setEditingId(r.identifier);
    setEditAmount(r.amount.toString());
    setEditDate(r.receivalDate);
    setEditNotes(r.notes ?? '');
  };

  const cancelEdit = () => {
    setEditingId(null);
  };

  const saveEdit = () => {
    if (!editingId || !editAmount || parseFloat(editAmount) <= 0) {
      return;
    }
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
      if (sortField === 'amount') {
        return (a.amount - b.amount) * mul;
      }
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
    'px-2 py-1 text-sm border border-border-strong rounded bg-surface-card text-text-primary focus:ring-1 focus:ring-blue-500';

  return (
    <div>
      {/* Search */}
      <div className="mb-4 relative">
        <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-text-muted " />
        <input
          type="text"
          placeholder={t('receivals.searchPlaceholder')}
          value={searchTerm}
          onChange={(e) => onSearchChange(e.target.value)}
          className="w-full pl-9 pr-4 py-2 text-sm border border-border-strong rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-surface-card text-text-primary"
        />
      </div>

      {/* Table */}
      <div className="overflow-hidden rounded-lg border border-border-default">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-border-default">
            <thead className="bg-surface-page">
              <tr>
                <th
                  className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
                  onClick={() => onSort('date')}
                >
                  <div className="flex items-center gap-1">
                    {t('receivals.date')}
                    <ArrowUpDown className="h-3 w-3" />
                  </div>
                </th>
                <th
                  className="px-4 py-3 text-right text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
                  onClick={() => onSort('amount')}
                >
                  <div className="flex items-center justify-end gap-1">
                    {t('receivals.amount')}
                    <ArrowUpDown className="h-3 w-3" />
                  </div>
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                  {t('receivals.notes')}
                </th>
                {canEdit && (
                  <th className="px-4 py-3 text-right text-xs font-medium text-text-secondary uppercase tracking-wider w-24">
                    {t('receivals.actions')}
                  </th>
                )}
              </tr>
            </thead>
            <tbody className="bg-surface-card divide-y divide-border-default">
              {paginated.map((receival) =>
                editingId === receival.identifier ? (
                  <tr key={receival.identifier} className="bg-info-bg">
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
                        placeholder={t('receivals.notes') + '...'}
                      />
                      <div className="flex justify-end gap-1 mt-2">
                        <button
                          onClick={saveEdit}
                          className="p-1.5 text-success-text hover:bg-success-bg rounded transition-colors"
                          title={t('tooltips.save')}
                        >
                          <Check className="h-4 w-4" />
                        </button>
                        <button
                          onClick={cancelEdit}
                          className="p-1.5 text-text-secondary hover:bg-surface-inset rounded transition-colors"
                          title={t('common:buttons.cancel')}
                        >
                          <X className="h-4 w-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ) : (
                  <tr
                    key={receival.identifier}
                    className="hover:bg-surface-inset"
                  >
                    <td className="px-4 py-3 whitespace-nowrap text-sm text-text-primary">
                      {formatDate(receival.receivalDate)}
                    </td>
                    <td className="px-4 py-3 whitespace-nowrap text-sm font-semibold text-right text-success-text">
                      {symbol} {receival.amount.toFixed(2)}
                    </td>
                    <td className="px-4 py-3 text-sm text-text-secondary max-w-xs">
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
                            className="p-1.5 text-primary-500 hover:bg-info-bg rounded transition-colors"
                            title={t('tooltips.editReceival')}
                          >
                            <Pencil className="h-4 w-4" />
                          </button>
                          <button
                            onClick={() => onDelete(receival.identifier)}
                            className="p-1.5 text-error-text hover:bg-error-bg rounded transition-colors"
                            title={t('tooltips.deleteReceival')}
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
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between mt-4 text-sm">
          <span className="text-text-secondary">
            {t('receivals.showing', {
              from: (currentPage - 1) * pageSize + 1,
              to: Math.min(currentPage * pageSize, filtered.length),
              total: filtered.length,
            })}
          </span>
          <div className="flex gap-2">
            <button
              onClick={() => onPageChange(currentPage - 1)}
              disabled={currentPage === 1}
              className="px-3 py-1 border border-border-strong rounded hover:bg-surface-inset disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-1 text-text-secondary"
            >
              <ChevronLeft className="h-4 w-4" />
              {t('common:pagination.previous')}
            </button>
            <button
              onClick={() => onPageChange(currentPage + 1)}
              disabled={currentPage === totalPages}
              className="px-3 py-1 border border-border-strong rounded hover:bg-surface-inset disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-1 text-text-secondary"
            >
              {t('common:pagination.next')}
              <ChevronRight className="h-4 w-4" />
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export const PaymentDetailPage = () => {
  const { id = '' } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const { t } = useTranslation('payments');
  const backTo = (location.state as { backTo?: string })?.backTo ?? '/payments';
  const { canEditData } = useTeam();
  const { formatDate, formatRelative } = useFormatDate();
  const [isEditing, setIsEditing] = useState(false);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [showMarkPaidModal, setShowMarkPaidModal] = useState(false);
  const [showReceivalModal, setShowReceivalModal] = useState(false);
  const [searchParams, setSearchParams] = useSearchParams();

  // Open the relevant action modal when arriving from an email deep link
  // (e.g. /payments/:id?action=mark-paid), then strip the param.
  useEffect(() => {
    const action = searchParams.get('action');
    if (!action || !canEditData) {
      return;
    }
    // One-shot sync of an email deep link into UI state; the param is stripped immediately
    // below so this runs once (not a derived-state smell).
    if (action === 'mark-paid') {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setShowMarkPaidModal(true);
    } else if (action === 'record-receival') {
      setShowReceivalModal(true);
    }
    setSearchParams(
      (prev) => {
        prev.delete('action');
        return prev;
      },
      { replace: true }
    );
  }, [searchParams, setSearchParams, canEditData]);
  const [activeTab, setActiveTab] = useTabState('details', [
    'details',
    'receivals',
    'documents',
    'history',
  ] as const);
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
  const updatePaymentMutation = useUpdatePayment(id);
  const markPaidMutation = useMarkPaymentAsPaid();
  const uploadDocumentMutation = useUploadPaymentDocument(id);
  const deleteDocumentMutation = useDeletePaymentDocument(id);
  const registerReceivalMutation = useRegisterReceival(id);
  const updateReceivalMutation = useUpdatePaymentReceival(id);
  const deleteReceivalMutation = useDeletePaymentReceival(id);

  const handleDelete = async () => {
    if (!id) {
      return;
    }
    try {
      await deletePaymentMutation.mutateAsync(id);
      navigate('/payments');
    } catch (err) {
      console.error('Failed to delete payment:', err);
    }
  };

  const handleUpdate = async (data: CreatePaymentRequest) => {
    const updateData: UpdatePaymentRequest = {
      contactIdentifier: data.contactIdentifier,
      amount: data.amount,
      currency: data.currency,
      dueDate: data.dueDate,
      notes: data.notes,
    };
    await updatePaymentMutation.mutateAsync(updateData);
    setIsEditing(false);
  };

  const handleMarkPaid = async () => {
    if (!id) {
      return;
    }
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
    if (!receivalAmount || parseFloat(receivalAmount) <= 0) {
      return;
    }
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
      <div className="min-h-full bg-background flex items-center justify-center">
        <LoadingSpinner />
      </div>
    );
  }

  if (error || !payment) {
    return (
      <div className="min-h-full bg-background p-8">
        <ErrorMessage message={t('errors.loadPaymentFailed')} />
      </div>
    );
  }

  const canEdit = true;
  const canDelete = true;
  const canMarkPaid =
    payment.status === PaymentStatus.PENDING ||
    payment.status === PaymentStatus.PARTIALLY_PAID ||
    payment.status === PaymentStatus.OVERDUE;
  const canRegisterReceival = canMarkPaid;
  const symbol = getCurrencySymbol(payment.currency);

  return (
    <div className="min-h-full bg-background">
      <div className="px-4 py-8">
        {/* Header */}
        <PageHeader
          title={t('detail.titlePrefix', { id: payment.identifier })}
          subtitle={t('detail.subtitlePrefix', {
            id: payment.contract.identifier,
          })}
          backTo={backTo}
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
                  {t('actions.registerReceival')}
                </Button>
              )}
              {canMarkPaid && (
                <Button
                  variant="success"
                  leftIcon={<CheckCircle />}
                  onClick={() => setShowMarkPaidModal(true)}
                  disabled={!canEditData}
                >
                  {t('actions.markAsPaid')}
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
                  {t('common:buttons.edit')}
                </Button>
              )}
              {canDelete && (
                <Button
                  variant="danger"
                  leftIcon={<Trash2 />}
                  onClick={() => setShowDeleteModal(true)}
                  disabled={!canEditData}
                >
                  {t('common:buttons.delete')}
                </Button>
              )}
            </>
          }
        />

        {/* Tabs */}
        <div className="border-b border-border-default mb-6">
          <div className="flex gap-6">
            <button
              onClick={() => setActiveTab('details')}
              className={`pb-3 px-1 font-medium transition-colors ${
                activeTab === 'details'
                  ? 'border-b-2 border-primary-500 text-primary-500'
                  : 'text-text-secondary hover:text-text-primary'
              }`}
            >
              {t('tabs.details')}
            </button>
            <button
              onClick={() => setActiveTab('receivals')}
              className={`pb-3 px-1 font-medium transition-colors flex items-center gap-2 ${
                activeTab === 'receivals'
                  ? 'border-b-2 border-primary-500 text-primary-500'
                  : 'text-text-secondary hover:text-text-primary'
              }`}
            >
              <ArrowDownCircle className="h-4 w-4" />
              {t('tabs.receivals')}{' '}
              {payment.receivals?.length > 0 && `(${payment.receivals.length})`}
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
              {t('tabs.documents')}{' '}
              {documents.length > 0 && `(${documents.length})`}
            </button>
            <button
              onClick={() => setActiveTab('history')}
              className={`pb-3 px-1 font-medium transition-colors flex items-center gap-2 ${
                activeTab === 'history'
                  ? 'border-b-2 border-primary-500 text-primary-500'
                  : 'text-text-secondary hover:text-text-primary'
              }`}
            >
              <History className="h-4 w-4" />
              {t('tabs.history')}{' '}
              {auditLog.length > 0 && `(${auditLog.length})`}
            </button>
          </div>
        </div>

        {/* Content */}
        {activeTab === 'details' &&
          (isEditing ? (
            <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
              <h2 className="text-lg font-semibold text-text-primary mb-4">
                {t('detail.editPayment')}
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
              <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
                <h2 className="text-lg font-semibold text-text-primary mb-4">
                  {t('detail.paymentDetails')}
                </h2>
                <div className="space-y-4">
                  <div className="flex items-center gap-3">
                    <DollarSign className="h-5 w-5 text-text-muted " />
                    <div>
                      <p className="text-sm text-text-secondary">
                        {t('detail.amount')}
                      </p>
                      <p className="font-medium text-text-primary text-lg">
                        {symbol} {payment.amount.toFixed(2)}
                      </p>
                    </div>
                  </div>

                  {/* Received / Balance */}
                  {payment.receivedAmount > 0 && (
                    <>
                      <div className="flex items-center gap-3">
                        <ArrowDownCircle className="h-5 w-5 text-success-text" />
                        <div>
                          <p className="text-sm text-text-secondary">
                            {t('detail.received')}
                          </p>
                          <p className="font-medium text-success-text text-lg">
                            {symbol} {payment.receivedAmount.toFixed(2)}
                          </p>
                        </div>
                      </div>
                      <div className="flex items-center gap-3">
                        <DollarSign className="h-5 w-5 text-warning-text" />
                        <div>
                          <p className="text-sm text-text-secondary">
                            {t('detail.balance')}
                          </p>
                          <p
                            className={`font-medium text-lg ${
                              payment.balance <= 0
                                ? 'text-success-text'
                                : 'text-warning-text'
                            }`}
                          >
                            {symbol} {payment.balance.toFixed(2)}
                          </p>
                        </div>
                      </div>
                    </>
                  )}

                  <div className="flex items-center gap-3">
                    <Calendar className="h-5 w-5 text-text-muted " />
                    <div>
                      <p className="text-sm text-text-secondary">
                        {t('detail.dueDate')}
                      </p>
                      <p className="font-medium text-text-primary">
                        {formatDate(payment.dueDate)}
                      </p>
                    </div>
                  </div>
                  {payment.paymentDate && (
                    <div className="flex items-center gap-3">
                      <CheckCircle className="h-5 w-5 text-success-text" />
                      <div>
                        <p className="text-sm text-text-secondary">
                          {t('detail.paymentDate')}
                        </p>
                        <p className="font-medium text-success-text">
                          {formatDate(payment.paymentDate)}
                        </p>
                      </div>
                    </div>
                  )}
                </div>
              </div>

              {/* Contract & Parties */}
              <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
                <h2 className="text-lg font-semibold text-text-primary mb-4">
                  {t('detail.relatedInformation')}
                </h2>
                <div className="space-y-4">
                  <div className="flex items-start gap-3">
                    <FileText className="h-5 w-5 text-text-muted mt-1" />
                    <div className="flex-1">
                      <p className="text-sm text-text-secondary">
                        {t('detail.contract')}
                      </p>
                      <button
                        onClick={() =>
                          navigate(`/contracts/${payment.contract.identifier}`)
                        }
                        className="font-medium text-primary-500 dark:text-primary-300 hover:underline text-left"
                      >
                        {t('detail.contractId', {
                          id: payment.contract.identifier,
                        })}
                      </button>
                    </div>
                  </div>
                  <div className="flex items-start gap-3">
                    <Home className="h-5 w-5 text-text-muted mt-1" />
                    <div className="flex-1">
                      <p className="text-sm text-text-secondary">
                        {t('detail.property')}
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
                    <User className="h-5 w-5 text-text-muted mt-1" />
                    <div className="flex-1">
                      <p className="text-sm text-text-secondary">
                        {t('detail.contact')}
                      </p>
                      <button
                        onClick={() =>
                          navigate(`/contacts/${payment.contact.identifier}`)
                        }
                        className="font-medium text-primary-500 dark:text-primary-300 hover:underline text-left"
                      >
                        {payment.contact.firstName} {payment.contact.lastName}
                      </button>
                    </div>
                  </div>
                </div>
              </div>

              {/* Notes */}
              {payment.notes && (
                <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6 lg:col-span-2">
                  <h2 className="text-lg font-semibold text-text-primary mb-4">
                    {t('detail.notes')}
                  </h2>
                  <RichTextDisplay content={payment.notes} />
                </div>
              )}

              {/* Metadata */}
              <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6 lg:col-span-2">
                <button
                  onClick={() => setIsMetadataExpanded(!isMetadataExpanded)}
                  className="w-full flex items-center justify-between text-left group"
                >
                  <h2 className="text-lg font-semibold text-text-primary">
                    {t('detail.metadata')}
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
                      <span className="text-text-secondary">
                        {t('detail.created')}
                      </span>{' '}
                      <span className="text-text-primary">
                        {formatDate(payment.createdAt)}{' '}
                        {new Date(payment.createdAt).toLocaleTimeString()}
                      </span>
                    </div>
                    <div>
                      <span className="text-text-secondary">
                        {t('detail.lastUpdated')}
                      </span>{' '}
                      <span className="text-text-primary">
                        {formatDate(payment.updatedAt)}{' '}
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
          <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-xl font-semibold text-text-primary">
                {t('receivals.title', {
                  count: payment.receivals?.length ?? 0,
                })}
              </h2>
              {canRegisterReceival && (
                <Button
                  variant="primary"
                  leftIcon={<PlusCircle />}
                  onClick={() => setShowReceivalModal(true)}
                  disabled={!canEditData}
                  size="sm"
                >
                  {t('actions.registerReceival')}
                </Button>
              )}
            </div>

            {/* Balance summary */}
            <div className="grid grid-cols-3 gap-4 mb-6 p-4 bg-surface-page rounded-lg">
              <div>
                <p className="text-xs text-text-secondary">
                  {t('receivals.totalAmount')}
                </p>
                <p className="text-lg font-semibold text-text-primary">
                  {symbol} {payment.amount.toFixed(2)}
                </p>
              </div>
              <div>
                <p className="text-xs text-text-secondary">
                  {t('receivals.received')}
                </p>
                <p className="text-lg font-semibold text-success-text">
                  {symbol} {payment.receivedAmount.toFixed(2)}
                </p>
              </div>
              <div>
                <p className="text-xs text-text-secondary">
                  {t('receivals.balance')}
                </p>
                <p
                  className={`text-lg font-semibold ${
                    payment.balance <= 0
                      ? 'text-success-text'
                      : 'text-warning-text'
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
                <ArrowDownCircle className="h-12 w-12 text-text-disabled mx-auto mb-3" />
                <p className="text-text-secondary">{t('receivals.empty')}</p>
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
          <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
            <h2 className="text-xl font-semibold text-text-primary mb-4">
              {t('history.title')}
            </h2>
            {auditLoading ? (
              <div className="flex items-center justify-center py-8">
                <LoadingSpinner />
              </div>
            ) : auditError ? (
              <ErrorMessage message={t('errors.loadHistoryFailed')} />
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
                              {formatRelative(activity.timestamp)}
                            </p>
                            {activity.impersonatedBy && (
                              <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-medium bg-warning-bg text-warning-text">
                                <Eye className="h-3 w-3" />
                                {t('history.impersonated')}
                              </span>
                            )}
                          </div>
                          {hasChanges && (
                            <p className="text-xs text-primary-500 mt-1">
                              {isExpanded
                                ? t('history.clickToHide')
                                : t('history.clickToView')}
                            </p>
                          )}
                        </div>
                      </div>

                      {isExpanded && hasChanges && (
                        <div className="bg-surface-page px-4 py-3 border-t border-border-default">
                          <h4 className="text-xs font-semibold text-text-secondary mb-2 uppercase">
                            {t('history.changedFields')}
                          </h4>
                          <div className="space-y-2">
                            {Object.entries(activity.changedFields ?? {}).map(
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
                                  return t('history.receivalOn', {
                                    amount: amt,
                                    date,
                                  });
                                };

                                const renderVal = (
                                  v: unknown,
                                  color: string,
                                  extra?: string
                                ) => {
                                  if (v == null) {
                                    return (
                                      <span
                                        className={`${color} ${extra ?? ''}`}
                                      >
                                        {t('history.na')}
                                      </span>
                                    );
                                  }
                                  if (Array.isArray(v)) {
                                    if (v.length === 0) {
                                      return (
                                        <span
                                          className={`${color} ${extra ?? ''}`}
                                        >
                                          {t('history.none')}
                                        </span>
                                      );
                                    }
                                    return (
                                      <ul
                                        className={`${color} ${extra ?? ''} list-disc list-inside`}
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
                                          className={`${color} ${extra ?? ''}`}
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
                                          className={`${color} ${extra ?? ''}`}
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
                                        className={`${color} ${extra ?? ''}`}
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
                                          .join(',')}
                                      </span>
                                    );
                                  }
                                  if (isHtml(v)) {
                                    return (
                                      <div
                                        className={`${color} ${extra ?? ''} mt-1`}
                                      >
                                        <RichTextDisplay
                                          content={String(v)}
                                          className="text-xs [&_p]:m-0"
                                        />
                                      </div>
                                    );
                                  }
                                  return (
                                    <span className={`${color} ${extra ?? ''}`}>
                                      {String(v)}
                                    </span>
                                  );
                                };

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
                                          {t('history.old')}{' '}
                                        </span>
                                        {renderVal(
                                          oldVal,
                                          'text-error-text',
                                          'line-through'
                                        )}
                                      </div>
                                      <div>
                                        <span className="text-text-secondary">
                                          {t('history.new')}{' '}
                                        </span>
                                        {renderVal(
                                          newVal,
                                          'text-success-text',
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
                <History className="h-12 w-12 text-text-disabled mx-auto mb-3" />
                <p className="text-text-secondary">{t('history.empty')}</p>
                <p className="text-sm text-text-muted mt-1">
                  {t('history.emptySubtitle')}
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
              {t('deleteDialog.title')}
            </h2>
            <p className="text-text-secondary mb-6">
              {t('deleteDialog.message')}
            </p>
            <div className="flex justify-end gap-3">
              <Button
                variant="secondary"
                onClick={() => setShowDeleteModal(false)}
              >
                {t('common:buttons.cancel')}
              </Button>
              <Button
                variant="danger"
                onClick={handleDelete}
                isLoading={deletePaymentMutation.isPending}
              >
                {t('common:buttons.delete')}
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
          <div className="bg-surface-card rounded-lg shadow-xl max-w-md w-full mx-4 p-6">
            <h2 className="text-lg font-semibold text-text-primary mb-4">
              {t('markPaidDialog.title')}
            </h2>
            {payment.balance > 0 && (
              <p className="text-sm text-text-secondary mb-4">
                {t('markPaidDialog.balanceMessage', {
                  amount: `${symbol} ${payment.balance.toFixed(2)}`,
                })}
              </p>
            )}
            <div className="space-y-4 mb-6">
              <div>
                <label className="block text-sm font-medium text-text-secondary mb-2">
                  {t('markPaidDialog.paymentDate')}{' '}
                  <span className="text-error-text">*</span>
                </label>
                <input
                  type="date"
                  value={paymentDate}
                  onChange={(e) => setPaymentDate(e.target.value)}
                  className="w-full px-3 py-2 border border-border-strong rounded-md"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-text-secondary mb-2">
                  {t('markPaidDialog.notesOptional')}
                </label>
                <RichTextEditor
                  value={markPaidNotes}
                  onChange={setMarkPaidNotes}
                  placeholder={t('markPaidDialog.notesPlaceholder')}
                  onSubmit={handleMarkPaid}
                />
              </div>
            </div>
            <div className="flex justify-end gap-3">
              <Button
                variant="secondary"
                onClick={() => setShowMarkPaidModal(false)}
              >
                {t('common:buttons.cancel')}
              </Button>
              <Button
                variant="success"
                leftIcon={<CheckCircle />}
                onClick={handleMarkPaid}
                isLoading={markPaidMutation.isPending}
              >
                {t('actions.markAsPaid')}
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
          <div className="bg-surface-card rounded-lg shadow-xl max-w-md w-full mx-4 p-6">
            <h2 className="text-lg font-semibold text-text-primary mb-4">
              {t('receivalDialog.title')}
            </h2>
            <p className="text-sm text-text-secondary mb-4">
              {t('receivalDialog.remainingBalance')}{' '}
              <span className="font-semibold text-text-primary">
                {symbol} {payment.balance.toFixed(2)}
              </span>
            </p>
            <div className="space-y-4 mb-6">
              <div>
                <label className="block text-sm font-medium text-text-secondary mb-2">
                  {t('receivalDialog.amount')}{' '}
                  <span className="text-error-text">*</span>
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
                <label className="block text-sm font-medium text-text-secondary mb-2">
                  {t('receivalDialog.receivalDate')}{' '}
                  <span className="text-error-text">*</span>
                </label>
                <input
                  type="date"
                  value={receivalDate}
                  onChange={(e) => setReceivalDate(e.target.value)}
                  className="w-full px-3 py-2 border border-border-strong rounded-md"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-text-secondary mb-2">
                  {t('receivalDialog.notesOptional')}
                </label>
                <RichTextEditor
                  value={receivalNotes}
                  onChange={setReceivalNotes}
                  placeholder={t('receivalDialog.notesPlaceholder')}
                  onSubmit={handleRegisterReceival}
                />
              </div>
            </div>
            <div className="flex justify-end gap-3">
              <Button
                variant="secondary"
                onClick={() => setShowReceivalModal(false)}
              >
                {t('common:buttons.cancel')}
              </Button>
              <Button
                variant="primary"
                leftIcon={<PlusCircle />}
                onClick={handleRegisterReceival}
                isLoading={registerReceivalMutation.isPending}
                disabled={!receivalAmount || parseFloat(receivalAmount) <= 0}
              >
                {t('receivalDialog.register')}
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
