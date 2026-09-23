import { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { PaymentStatus } from '@/types/payment';
import {
  usePayments,
  usePaymentStats,
  useDeletePayment,
  useMarkPaymentAsPaid,
  usePaymentArrears,
  useBulkMarkPaymentsAsPaid,
  useBulkSendPaymentReminders,
  useBulkCancelPayments,
} from '@/hooks/usePaymentHooks';
import { PaymentTypeBadge } from '@/components/payments/PaymentTypeBadge';
import { ReasonDialog } from '@/components/payments/ReasonDialog';
import { CreatePaymentPlanDialog } from '@/components/payments/CreatePaymentPlanDialog';
import { usePaymentSelection } from '@/hooks/usePaymentSelection';
import { ArrearsPanel } from '@/components/payments/ArrearsPanel';
import { BulkMarkPaidDialog } from '@/components/payments/BulkMarkPaidDialog';
import { SendReminderDialog } from '@/components/payments/SendReminderDialog';
import {
  ConfirmDialog,
  DataList,
  EmptyState,
  FilterSelectPopover,
  ListPageHeader,
  Pagination,
  RefreshButton,
  SelectionBar,
  Skeleton,
  SwipeAction,
  type ListPageHeaderAction,
  type SelectionBarAction,
  type SwipeActionItem,
} from '@buurman/ui';
import { MobileMenuButton } from '@/components/MobileMenuButton';
import { RefreshCw } from 'lucide-react';
import { usePagination } from '@/hooks/usePagination';
import { PaymentStatusBadge } from '@/components/payments/PaymentStatusBadge';
import { ContractCell } from '@/components/contracts/ContractCell';
import { ErrorMessage } from '@/components/ErrorMessage';
import { ContractSelector } from '@/components/common/ContractSelector';
import { PropertySelector } from '@/components/common/PropertySelector';
import {
  PeriodFilter,
  PeriodDateRange,
} from '@/components/common/PeriodFilter';
import {
  Plus,
  CalendarCheck,
  DollarSign,
  AlertTriangle,
  ArrowUpDown,
  Clock,
  CheckCircle,
  TrendingUp,
  Eye,
  Trash2,
  CircleDot,
  X,
  CheckSquare,
  Square,
  Send,
  ListChecks,
} from 'lucide-react';
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';
import { useTeam } from '@/context/TeamContext';
import { useFormatDate } from '@/hooks/useFormatDate';
import { EntityExportControls } from '@/components/common/EntityExportControls';
import {
  exportPaymentsCsv,
  exportPaymentsXlsx,
  exportPaymentsGoogleSheet,
} from '@/generated/api/booklets/booklets';
import { GOOGLE_SHEET_EXPORT_TIMEOUT_MS } from '@/utils/googleSheetExport';

export const PaymentsPage = () => {
  const navigate = useNavigate();
  const { t } = useTranslation('payments');
  const { canEditData } = useTeam();
  const { formatDate } = useFormatDate();
  const deletePaymentMutation = useDeletePayment();
  const markPaidMutation = useMarkPaymentAsPaid();
  const bulkMarkPaidMutation = useBulkMarkPaymentsAsPaid();
  const bulkRemindersMutation = useBulkSendPaymentReminders();
  const bulkCancelMutation = useBulkCancelPayments();
  const [showBulkCancel, setShowBulkCancel] = useState(false);
  const [showPlanDialog, setShowPlanDialog] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<string | null>(null);
  const [selectionMode, setSelectionMode] = useState(false);
  const [showBulkMarkPaid, setShowBulkMarkPaid] = useState(false);
  const [reminderTargets, setReminderTargets] = useState<string[] | null>(
    null
  );
  const [statusFilter, setStatusFilter] = useState<PaymentStatus | undefined>(
    undefined
  );
  const [contractFilter, setContractFilter] = useState<string | undefined>(
    undefined
  );
  const [propertyFilter, setPropertyFilter] = useState<string | undefined>(
    undefined
  );
  const [periodRange, setPeriodRange] = useState<PeriodDateRange | null>(null);

  const statusOptions = useMemo(
    () => [
      { value: PaymentStatus.PENDING, label: t('status.pending') },
      { value: PaymentStatus.PARTIALLY_PAID, label: t('status.partiallyPaid') },
      { value: PaymentStatus.PAID, label: t('status.paid') },
      { value: PaymentStatus.OVERDUE, label: t('status.overdue') },
      { value: PaymentStatus.CANCELLED, label: t('status.cancelled') },
    ],
    [t]
  );

  const {
    pageParams,
    page,
    size,
    handlePageChange,
    handleSizeChange,
    handleSortChange,
    resetPage,
  } = usePagination({ defaultSort: 'dueDate' });

  const {
    data: paymentsData,
    isLoading,
    isFetching,
    refetch,
    error,
  } = usePayments({
    status: statusFilter,
    contractIdentifier: contractFilter,
    propertyIdentifier: propertyFilter,
    dateFrom: periodRange?.startDate,
    dateTo: periodRange?.endDate,
    ...pageParams,
  });

  const { data: paymentStats } = usePaymentStats();
  const { data: arrears } = usePaymentArrears();

  const isActionable = (status: PaymentStatus) =>
    status !== PaymentStatus.PAID && status !== PaymentStatus.CANCELLED;
  const actionableIds = useMemo(
    () =>
      (paymentsData?.content ?? [])
        .filter((p) => isActionable(p.status))
        .map((p) => p.identifier),
    [paymentsData]
  );
  const { selected, toggle, toggleAll, allVisibleSelected, clear } =
    usePaymentSelection(actionableIds);
  const selectedIds = Array.from(selected);
  const hasSelection = selectedIds.length > 0;

  const exitSelection = () => {
    clear();
    setSelectionMode(false);
  };

  const handleBulkMarkPaid = async (paymentDate: string, notes?: string) => {
    await bulkMarkPaidMutation.mutateAsync({
      identifiers: selectedIds,
      paymentDate,
      notes,
    });
    setShowBulkMarkPaid(false);
    exitSelection();
  };

  const handleSendReminders = async (notes?: string) => {
    if (!reminderTargets || reminderTargets.length === 0) {
      return;
    }
    await bulkRemindersMutation.mutateAsync({
      identifiers: reminderTargets,
      notes,
    });
    setReminderTargets(null);
    exitSelection();
  };

  const selectedPayments = (paymentsData?.content ?? []).filter((p) =>
    selected.has(p.identifier)
  );
  const planContract =
    selectedPayments.length > 0 &&
    selectedPayments.every(
      (p) =>
        p.contract?.identifier === selectedPayments[0].contract?.identifier &&
        p.paymentType !== 'INSTALMENT'
    )
      ? selectedPayments[0].contract?.identifier
      : undefined;
  const planOutstanding = selectedPayments.reduce(
    (acc, p) => acc + (p.balance ?? p.amount),
    0
  );

  const handleBulkCancel = async (reason: string) => {
    await bulkCancelMutation.mutateAsync({ identifiers: selectedIds, reason });
    setShowBulkCancel(false);
    exitSelection();
  };

  const bulkActions: SelectionBarAction[] = [
    {
      label: t('selection.markPaid'),
      icon: CheckCircle,
      onClick: () => setShowBulkMarkPaid(true),
      disabled: bulkMarkPaidMutation.isPending,
    },
    {
      label: t('selection.sendReminders'),
      icon: Send,
      onClick: () => setReminderTargets(selectedIds),
      disabled: bulkRemindersMutation.isPending,
    },
    {
      label: t('selection.paymentPlan'),
      icon: ListChecks,
      onClick: () => setShowPlanDialog(true),
      disabled: !planContract,
    },
    {
      label: t('selection.cancel'),
      icon: X,
      tone: 'danger',
      onClick: () => setShowBulkCancel(true),
      disabled: bulkCancelMutation.isPending,
    },
  ];

  const statsCurrency = paymentStats?.currency ?? '';

  const fmtMoney = (value: number, currency: string) => {
    if (!currency) {
      return value.toFixed(2);
    }
    try {
      return new Intl.NumberFormat(undefined, {
        style: 'currency',
        currency,
      }).format(value);
    } catch {
      return `${currency} ${value.toFixed(2)}`;
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-full bg-background">
        <div className="px-4 py-8 space-y-6">
          {/* Header skeleton */}
          <div className="flex justify-between items-center">
            <div className="space-y-2">
              <Skeleton className="h-8 w-48" />
              <Skeleton className="h-4 w-56" />
            </div>
            <div className="flex items-center gap-2">
              <Skeleton className="h-10 w-36 rounded" />
              <Skeleton className="h-10 w-40 rounded" />
            </div>
          </div>
          {/* Stats cards skeleton */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {Array.from({ length: 3 }).map((_, i) => (
              <div
                key={i}
                className="bg-surface-card rounded-lg border border-border-default p-6 space-y-3"
              >
                <div className="flex items-center justify-between">
                  <Skeleton className="h-4 w-32" />
                  <Skeleton className="h-5 w-5 rounded" />
                </div>
                <Skeleton className="h-8 w-40" />
                <Skeleton className="h-4 w-24" />
              </div>
            ))}
          </div>
          {/* Filter bar skeleton */}
          <Skeleton className="h-32 w-full rounded-lg" />
          {/* Table rows skeleton */}
          <div className="space-y-2">
            <Skeleton className="h-10 w-full rounded" />
            {Array.from({ length: 8 }).map((_, i) => (
              <Skeleton key={i} className="h-14 w-full rounded" />
            ))}
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-full bg-background p-8">
        <ErrorMessage message={t('errors.loadFailed')} />
      </div>
    );
  }

  const headerActions: ListPageHeaderAction[] = [
    {
      label: t('common:refresh', 'Refresh'),
      icon: RefreshCw,
      onClick: () => refetch(),
      showOn: 'mobile',
    },
    {
      label: t('actions.schedulePayment'),
      icon: Plus,
      onClick: () => navigate('/payments/new'),
      showOn: 'mobile',
      disabled: !canEditData,
    },
    {
      label: selectionMode ? t('selection.done') : t('selection.select'),
      icon: ListChecks,
      onClick: () => (selectionMode ? exitSelection() : setSelectionMode(true)),
      showOn: 'mobile',
      disabled: !canEditData || actionableIds.length === 0,
    },
    {
      label: 'desktop-actions',
      showOn: 'desktop',
      render: () => (
        <div className="flex items-center gap-2 flex-wrap">
          <RefreshButton onClick={() => refetch()} isRefreshing={isFetching} />
          <EntityExportControls
            filenameStem="payments"
            csv={() => exportPaymentsCsv()}
            xlsx={() => exportPaymentsXlsx()}
            googleSheet={(accessToken) =>
              exportPaymentsGoogleSheet(
                { accessToken },
                { timeout: GOOGLE_SHEET_EXPORT_TIMEOUT_MS }
              )
            }
          />
          <button
            onClick={() => navigate('/payments/new')}
            disabled={!canEditData}
            className="text-text-secondary border border-border-strong px-4 py-2 rounded hover:bg-surface-inset transition-colors flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <Plus className="h-5 w-5" />
            {t('actions.schedulePayment')}
          </button>
        </div>
      ),
    },
  ];

  return (
    <div className="min-h-full bg-background">
      <div className="px-4 py-4 md:py-8">
        <ListPageHeader
          title={t('page.title')}
          subtitle={t('page.subtitle')}
          icon={DollarSign}
          mobileLeading={<MobileMenuButton />}
          actions={headerActions}
          primaryAction={{
            label: t('actions.registerPayment'),
            icon: Plus,
            onClick: () => navigate('/payments/new?register=true'),
            disabled: !canEditData,
          }}
        />

        {/* Metrics Dashboard — phone shows Pending + Overdue in a 50/50
            row; the 6-month sparkline collapses to lg+ (the data lives in
            the dashboard's Portfolio analytics for power users anyway). */}
        {paymentStats && (
          <div className="mb-6 grid grid-cols-2 lg:grid-cols-3 gap-3 md:gap-6">
            {/* Pending Payments */}
            <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-3 md:p-6">
              <div className="flex items-center justify-between mb-2">
                <h3 className="text-xs md:text-sm font-medium text-text-secondary">
                  {t('stats.pendingPayments')}
                </h3>
                <Clock className="h-5 w-5 text-warning-text" />
              </div>
              <p className="text-xl md:text-3xl font-bold text-text-primary tabular-nums">
                {fmtMoney(paymentStats.pendingAmount, statsCurrency)}
              </p>
              <p className="text-xs md:text-sm text-text-secondary mt-1">
                {t('stats.pendingCount', { count: paymentStats.pendingCount })}
              </p>
            </div>

            {/* Overdue Payments */}
            <div
              className={`rounded-lg shadow-sm border border-border-default p-3 md:p-6 transition-colors ${
                paymentStats.overdueCount > 0
                  ? 'bg-error-bg border-2 border-error-border'
                  : 'bg-surface-card'
              }`}
            >
              <div className="flex items-center justify-between mb-2">
                <h3
                  className={`text-xs md:text-sm font-medium ${
                    paymentStats.overdueCount > 0
                      ? 'text-error-text'
                      : 'text-text-secondary'
                  }`}
                >
                  {t('stats.overduePayments')}
                </h3>
                {paymentStats.overdueCount > 0 ? (
                  <AlertTriangle className="h-5 w-5 text-error-text animate-pulse" />
                ) : (
                  <CheckCircle className="h-5 w-5 text-success-text" />
                )}
              </div>
              {paymentStats.overdueCount > 0 ? (
                <>
                  <p className="text-xl md:text-3xl font-bold text-error-text tabular-nums">
                    {fmtMoney(paymentStats.overdueAmount, statsCurrency)}
                  </p>
                  <p className="text-xs md:text-sm text-error-text mt-1 font-medium">
                    {t('stats.overdueCount', {
                      count: paymentStats.overdueCount,
                    })}
                  </p>
                  <p className="hidden md:block text-xs text-error-text mt-2">
                    {t('stats.overdueAction')}
                  </p>
                </>
              ) : (
                <>
                  <p className="text-xl md:text-3xl font-bold text-success-text tabular-nums">
                    {fmtMoney(0, statsCurrency)}
                  </p>
                  <p className="text-xs md:text-sm text-text-secondary mt-1">
                    {t('stats.allCaughtUp')}
                  </p>
                  <p className="hidden md:block text-xs text-text-secondary mt-2">
                    {t('stats.noOverdue')}
                  </p>
                </>
              )}
            </div>

            {/* 6-Month Revenue Chart — lg+ only on phone the chart at
                this size is unreadable; deeper trend lives in /reports. */}
            <div className="hidden lg:block bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-sm font-medium text-text-secondary">
                  {t('stats.lastSixMonths')}
                </h3>
                <TrendingUp className="h-5 w-5 text-success-text" />
              </div>
              <ResponsiveContainer width="100%" height={80}>
                <AreaChart data={paymentStats.monthlyTrend}>
                  <defs>
                    <linearGradient id="colorTotal" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#10b981" stopOpacity={0.3} />
                      <stop offset="95%" stopColor="#10b981" stopOpacity={0} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                  <XAxis
                    dataKey="month"
                    tick={{ fontSize: 10 }}
                    stroke="#9ca3af"
                  />
                  <YAxis hide />
                  <Tooltip
                    formatter={(value) => [
                      typeof value === 'number'
                        ? fmtMoney(value, statsCurrency)
                        : 'N/A',
                      t('stats.tooltipReceived'),
                    ]}
                    contentStyle={{ fontSize: 12 }}
                  />
                  <Area
                    type="monotone"
                    dataKey="total"
                    stroke="#10b981"
                    strokeWidth={2}
                    fillOpacity={1}
                    fill="url(#colorTotal)"
                  />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          </div>
        )}

        {/* Arrears — who is behind, by how much, and for how long */}
        {arrears && (
          <ArrearsPanel
            arrears={arrears}
            formatMoney={fmtMoney}
            canEdit={canEditData}
            onSendReminders={(ids) => setReminderTargets(ids)}
            sendingFor={bulkRemindersMutation.isPending ? reminderTargets : null}
          />
        )}

        {/* Toolbar — period, property, contract, and status on one tidy row */}
        <div className="flex flex-wrap items-center gap-2 mb-4">
          <PeriodFilter
            presets={['month', 'quarter', 'year', 'all', 'custom']}
            defaultPreset="all"
            onChange={(range) => {
              setPeriodRange(range);
              resetPage();
            }}
          />
          <div className="w-full sm:w-56">
            <PropertySelector
              value={propertyFilter ?? ''}
              onChange={(id) => {
                setPropertyFilter(id || undefined);
                resetPage();
              }}
              clearable
              placeholder={t('filters.allProperties')}
            />
          </div>
          <div className="w-full sm:w-56">
            <ContractSelector
              value={contractFilter ?? ''}
              onChange={(id) => {
                setContractFilter(id || undefined);
                resetPage();
              }}
              status={undefined}
              clearable
              placeholder={t('filters.allContracts')}
            />
          </div>
          <FilterSelectPopover
            icon={CircleDot}
            label={t('filters.status')}
            options={statusOptions}
            value={statusFilter}
            onChange={(value) => {
              setStatusFilter(value);
              resetPage();
            }}
            allLabel={t('filters.allStatuses')}
          />
        </div>

        {/* Active status chip — period/property/contract surface their own
            selection inline, so only status needs a removable chip here. */}
        {statusFilter && (
          <div className="flex flex-wrap items-center gap-2 mb-4">
            <span className="inline-flex items-center gap-1 px-2.5 py-1 text-xs font-medium rounded-full bg-primary-50 text-primary-700 dark:bg-primary-950 dark:text-primary-300">
              {statusOptions.find((o) => o.value === statusFilter)?.label}
              <button
                onClick={() => {
                  setStatusFilter(undefined);
                  resetPage();
                }}
                aria-label={t('common:buttons.clear', 'Clear')}
                className="rounded-full hover:text-primary-900 focus-ring"
              >
                <X className="h-3 w-3" />
              </button>
            </span>
          </div>
        )}

        {/* Phone-only sticky selection bar (overlays bottom tab bar) */}
        <SelectionBar
          open={hasSelection}
          count={selectedIds.length}
          label={t('selection.selected', { count: selectedIds.length }).replace(
            String(selectedIds.length),
            '{{count}}'
          )}
          onCancel={exitSelection}
          actions={bulkActions}
        />

        {/* Payments — Mobile card list (<md). md+ shows the existing table below. */}
        {paymentsData?.content && paymentsData.content.length > 0 && (
          <ul className="md:hidden space-y-3 mb-4">
            {paymentsData.content.map((payment) => {
              const isUnpaid =
                payment.status !== PaymentStatus.PAID &&
                payment.status !== PaymentStatus.CANCELLED;
              const leftActions: SwipeActionItem[] = [];
              if (canEditData && isUnpaid) {
                leftActions.push({
                  label: t('actions.markPaid', { defaultValue: 'Mark Paid' }),
                  icon: CheckCircle,
                  tone: 'success',
                  onAction: () =>
                    markPaidMutation.mutate({
                      id: payment.identifier,
                      data: {
                        paymentDate: new Date().toISOString().split('T')[0],
                      },
                    }),
                });
              }
              if (canEditData) {
                leftActions.push({
                  label: t('actions.delete', { defaultValue: 'Delete' }),
                  icon: Trash2,
                  tone: 'danger',
                  onAction: () => setDeleteTarget(payment.identifier),
                });
              }
              const isSelected = selected.has(payment.identifier);
              return (
                <li key={`m-${payment.identifier}`}>
                  <SwipeAction
                    leftActions={selectionMode ? [] : leftActions}
                    onClick={() =>
                      selectionMode
                        ? isUnpaid && toggle(payment.identifier)
                        : navigate(`/payments/${payment.identifier}`)
                    }
                  >
                    <div
                      className={`bg-surface-card border p-4 min-h-touch ${
                        isSelected
                          ? 'border-primary-500 bg-primary-50 dark:bg-primary-950'
                          : 'border-border-default'
                      }`}
                    >
                      <DataList
                        leading={
                          selectionMode ? (
                            isSelected ? (
                              <CheckSquare className="h-5 w-5 text-primary-500" />
                            ) : (
                              <Square
                                className={`h-5 w-5 ${
                                  isUnpaid ? 'text-text-muted' : 'text-border-default'
                                }`}
                              />
                            )
                          ) : undefined
                        }
                        title={`${payment.property?.street}`}
                        trailing={
                          <span className="inline-flex items-center gap-1">
                            <PaymentTypeBadge type={payment.paymentType} />
                            <PaymentStatusBadge status={payment.status} />
                          </span>
                        }
                        items={[
                          {
                            label: t('table.dueDate'),
                            value: formatDate(payment.dueDate),
                          },
                          {
                            label: 'Contact',
                            value:
                              (payment.contact?.firstName ??
                                payment.contact?.displayName) +
                              (payment.contact?.lastName
                                ? ' ' + payment.contact.lastName
                                : ''),
                          },
                          {
                            label: t('table.amount'),
                            value: (
                              <span className="font-semibold text-text-primary">
                                {fmtMoney(payment.amount, payment.currency)}
                              </span>
                            ),
                            align: 'right',
                          },
                        ]}
                      />
                    </div>
                  </SwipeAction>
                </li>
              );
            })}
          </ul>
        )}

        {/* Payments Table — md+ */}
        {paymentsData?.content && paymentsData.content.length > 0 ? (
          <>
            <div className="hidden md:block bg-surface-card rounded-lg shadow-sm overflow-hidden mb-4">
              {canEditData && hasSelection && (
                <div className="px-6 py-3 border-b border-border-default bg-primary-50 dark:bg-primary-950 flex items-center justify-between gap-3">
                  <span className="text-sm font-medium text-text-primary">
                    {t('selection.selected', { count: selectedIds.length })}
                  </span>
                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => setShowBulkMarkPaid(true)}
                      disabled={bulkMarkPaidMutation.isPending}
                      className="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium text-white bg-success rounded-md hover:opacity-90 transition-colors disabled:opacity-50 focus-ring"
                    >
                      <CheckCircle className="h-4 w-4" />
                      {t('selection.markPaid')}
                    </button>
                    <button
                      onClick={() => setReminderTargets(selectedIds)}
                      disabled={bulkRemindersMutation.isPending}
                      className="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium text-text-secondary bg-surface-card border border-border-strong rounded-md hover:bg-surface-inset transition-colors disabled:opacity-50 focus-ring"
                    >
                      <Send className="h-4 w-4" />
                      {t('selection.sendReminders')}
                    </button>
                    <button
                      onClick={() => setShowPlanDialog(true)}
                      disabled={!planContract}
                      title={planContract ? undefined : t('selection.paymentPlanHint')}
                      className="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium text-text-secondary bg-surface-card border border-border-strong rounded-md hover:bg-surface-inset transition-colors disabled:opacity-50 focus-ring"
                    >
                      <ListChecks className="h-4 w-4" />
                      {t('selection.paymentPlan')}
                    </button>
                    <button
                      onClick={() => setShowBulkCancel(true)}
                      disabled={bulkCancelMutation.isPending}
                      className="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium text-error-text bg-surface-card border border-error-border rounded-md hover:bg-error-bg transition-colors disabled:opacity-50 focus-ring"
                    >
                      <X className="h-4 w-4" />
                      {t('selection.cancel')}
                    </button>
                    <button
                      onClick={exitSelection}
                      className="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium text-text-secondary rounded-md hover:bg-surface-inset transition-colors focus-ring"
                    >
                      <X className="h-4 w-4" />
                      {t('common:buttons.cancel')}
                    </button>
                  </div>
                </div>
              )}
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-border-default">
                  <thead className="bg-surface-page">
                    <tr>
                      {canEditData && (
                        <th className="pl-6 pr-2 py-3 w-10">
                          <button
                            type="button"
                            onClick={toggleAll}
                            disabled={actionableIds.length === 0}
                            aria-label={t('selection.selectAll')}
                            className="text-text-secondary hover:text-primary-500 disabled:opacity-40 focus-ring rounded"
                          >
                            {allVisibleSelected ? (
                              <CheckSquare className="h-5 w-5 text-primary-500" />
                            ) : (
                              <Square className="h-5 w-5" />
                            )}
                          </button>
                        </th>
                      )}
                      <th
                        className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
                        onClick={() => handleSortChange('dueDate')}
                      >
                        <div className="flex items-center gap-1">
                          {t('table.dueDate')}
                          <ArrowUpDown className="h-4 w-4" />
                        </div>
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                        {t('table.paymentNumber')}
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider min-w-[280px]">
                        {t('table.contract')}
                      </th>
                      <th
                        className="px-6 py-3 text-right text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
                        onClick={() => handleSortChange('amount')}
                      >
                        <div className="flex items-center justify-end gap-1">
                          {t('table.amount')}
                          <ArrowUpDown className="h-4 w-4" />
                        </div>
                      </th>
                      <th
                        className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
                        onClick={() => handleSortChange('status')}
                      >
                        <div className="flex items-center gap-1">
                          {t('table.status')}
                          <ArrowUpDown className="h-4 w-4" />
                        </div>
                      </th>
                      <th className="px-4 py-3 text-right text-xs font-medium text-text-secondary uppercase tracking-wider"></th>
                    </tr>
                  </thead>
                  <tbody className="bg-surface-card divide-y divide-border-default">
                    {paymentsData.content.map((payment) => (
                      <tr
                        key={payment.identifier}
                        className={`cursor-pointer ${
                          selected.has(payment.identifier)
                            ? 'bg-primary-50 dark:bg-primary-950'
                            : 'hover:bg-primary-50'
                        }`}
                        onClick={() =>
                          navigate(`/payments/${payment.identifier}`)
                        }
                      >
                        {canEditData && (
                          <td className="pl-6 pr-2 py-4 w-10">
                            <button
                              type="button"
                              onClick={(e) => {
                                e.stopPropagation();
                                toggle(payment.identifier);
                              }}
                              disabled={!isActionable(payment.status)}
                              aria-label={t('selection.selectRow', {
                                id: payment.identifier,
                              })}
                              className="text-text-secondary hover:text-primary-500 disabled:opacity-30 focus-ring rounded"
                            >
                              {selected.has(payment.identifier) ? (
                                <CheckSquare className="h-5 w-5 text-primary-500" />
                              ) : (
                                <Square className="h-5 w-5" />
                              )}
                            </button>
                          </td>
                        )}
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-text-primary">
                          {formatDate(payment.dueDate)}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <span className="text-sm font-medium text-text-primary inline-flex items-center gap-2">
                            #{payment.identifier}
                            <PaymentTypeBadge type={payment.paymentType} />
                          </span>
                        </td>
                        <td className="px-6 py-3">
                          {payment.contract ? (
                            <ContractCell
                              contractIdentifier={payment.contract.identifier}
                              contractStatus={payment.contract.status}
                              propertyStreet={payment.property?.street ?? ''}
                              propertyCity={payment.property?.city ?? ''}
                              contactFirstName={
                                payment.contact?.firstName ??
                                payment.contact?.displayName ??
                                ''
                              }
                              contactLastName={payment.contact?.lastName}
                            />
                          ) : (
                            <span className="text-sm text-text-muted">—</span>
                          )}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-right">
                          <div>
                            <span className="text-sm font-semibold text-text-primary">
                              {fmtMoney(payment.amount, payment.currency)}
                            </span>
                            {(payment.receivedAmount ?? 0) > 0 &&
                              payment.status !== PaymentStatus.PAID && (
                                <p className="text-xs text-text-secondary">
                                  {t('table.balance', {
                                    amount: fmtMoney(
                                      payment.balance ?? 0,
                                      payment.currency
                                    ),
                                  })}
                                </p>
                              )}
                          </div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <PaymentStatusBadge status={payment.status} />
                        </td>
                        <td className="px-4 py-4 whitespace-nowrap text-right">
                          <div className="flex items-center justify-end gap-1">
                            <button
                              onClick={(e) => {
                                e.stopPropagation();
                                navigate(`/payments/${payment.identifier}`);
                              }}
                              className="p-1.5 rounded hover:bg-surface-inset text-text-secondary hover:text-primary-500 transition-colors"
                              title={t('tooltips.viewPayment')}
                            >
                              <Eye className="h-4 w-4" />
                            </button>
                            {canEditData && (
                              <button
                                onClick={(e) => {
                                  e.stopPropagation();
                                  setDeleteTarget(payment.identifier);
                                }}
                                className="p-1.5 rounded hover:bg-error-bg text-text-secondary hover:text-error-text transition-colors"
                                title={t('tooltips.deletePayment')}
                              >
                                <Trash2 className="h-4 w-4" />
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

            {paymentsData && (
              <Pagination
                page={page}
                totalPages={paymentsData.totalPages ?? 0}
                totalElements={paymentsData.totalElements ?? 0}
                size={size}
                onPageChange={handlePageChange}
                onSizeChange={handleSizeChange}
              />
            )}
          </>
        ) : (
          <div className="bg-surface-card rounded-lg border border-border-default">
            <EmptyState
              variant="page"
              icon={<DollarSign className="h-12 w-12" />}
              title={t('empty.title')}
              description={
                statusFilter || propertyFilter || contractFilter
                  ? t('empty.filtered')
                  : t('empty.noData')
              }
              actions={
                !statusFilter && !propertyFilter && !contractFilter ? (
                  <div className="flex flex-wrap items-center gap-2 justify-center">
                    <button
                      onClick={() => navigate('/payments/new')}
                      disabled={!canEditData}
                      className="text-text-secondary border border-border-strong px-4 py-2 rounded min-h-touch hover:bg-surface-inset transition-colors inline-flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed focus-ring"
                    >
                      <Plus className="h-5 w-5" />
                      {t('actions.schedulePayment')}
                    </button>
                    <button
                      onClick={() => navigate('/payments/new?register=true')}
                      disabled={!canEditData}
                      className="bg-primary-500 text-white px-4 py-2 rounded min-h-touch hover:bg-primary-600 transition-colors inline-flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-primary-500 focus-ring"
                    >
                      <CalendarCheck className="h-5 w-5" />
                      {t('actions.registerPayment')}
                    </button>
                  </div>
                ) : undefined
              }
            />
          </div>
        )}
      </div>

      {showBulkMarkPaid && (
        <BulkMarkPaidDialog
          open={showBulkMarkPaid}
          count={selectedIds.length}
          isLoading={bulkMarkPaidMutation.isPending}
          onConfirm={handleBulkMarkPaid}
          onClose={() => setShowBulkMarkPaid(false)}
        />
      )}

      {showPlanDialog && planContract && (
        <CreatePaymentPlanDialog
          open={showPlanDialog}
          contractIdentifier={planContract}
          paymentIdentifiers={selectedIds}
          totalOutstanding={planOutstanding}
          currency={selectedPayments[0]?.currency ?? statsCurrency}
          formatMoney={fmtMoney}
          onCreated={() => {
            setShowPlanDialog(false);
            exitSelection();
          }}
          onClose={() => setShowPlanDialog(false)}
        />
      )}

      {showBulkCancel && (
        <ReasonDialog
          open={showBulkCancel}
          title={t('cancelDialog.titleBulk', { count: selectedIds.length })}
          message={t('cancelDialog.messageBulk')}
          reasonLabel={t('cancelDialog.reason')}
          reasonPlaceholder={t('cancelDialog.reasonPlaceholder')}
          confirmLabel={t('cancelDialog.confirmBulk', { count: selectedIds.length })}
          variant="danger"
          isLoading={bulkCancelMutation.isPending}
          onConfirm={handleBulkCancel}
          onClose={() => setShowBulkCancel(false)}
        />
      )}

      {reminderTargets && (
        <SendReminderDialog
          open={reminderTargets.length > 0}
          count={reminderTargets.length}
          isLoading={bulkRemindersMutation.isPending}
          onConfirm={handleSendReminders}
          onClose={() => setReminderTargets(null)}
        />
      )}

      {deleteTarget && (
        <ConfirmDialog
          title={t('deleteDialog.title')}
          message={t('deleteDialog.messageWithRelated')}
          confirmLabel={t('common:buttons.delete')}
          variant="danger"
          isLoading={deletePaymentMutation.isPending}
          onConfirm={async () => {
            await deletePaymentMutation.mutateAsync(deleteTarget);
            setDeleteTarget(null);
          }}
          onCancel={() => setDeleteTarget(null)}
        />
      )}
    </div>
  );
};
