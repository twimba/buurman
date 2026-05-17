import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useDashboardStats } from '@/hooks/useDashboard';
import { usePayments, useMarkPaymentAsPaid } from '@/hooks/usePaymentHooks';
import { usePendingExtensions } from '@/hooks/useContractExtensionHooks';
import * as extensionsApi from '@/api/contractExtensions';
import { useTeam } from '@/context/TeamContext';
import { Skeleton, useToast } from '@buurman/ui';
import { MetricHint } from '@/components/common/MetricHint';
import { ErrorMessage } from './ErrorMessage';
import { PendingInvitationsPanel } from './dashboard/PendingInvitationsPanel';
import { PendingExtensionsPanel } from './dashboard/PendingExtensionsPanel';
import { PortfolioDashboard } from './dashboard/PortfolioDashboard';
import { MobileDashboardSummary } from './dashboard/MobileDashboardSummary';
import {
  Home,
  Users,
  DollarSign,
  TrendingUp,
  ArrowRight,
  AlertTriangle,
  CheckCircle,
  Clock,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useFormatDate } from '@/hooks/useFormatDate';
import { formatMoney } from '@/utils/formatMoney';

export const DashboardPage = () => {
  const { t } = useTranslation('common');
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { formatDate, formatRelative } = useFormatDate();
  const { canEditData } = useTeam();
  const { showToast } = useToast();
  const {
    data: stats,
    isLoading: statsLoading,
    error: statsError,
  } = useDashboardStats();
  const { data: allPaymentsData, isLoading: paymentsLoading } = usePayments();
  const allPayments = allPaymentsData?.content;
  const markPaidMutation = useMarkPaymentAsPaid();
  const [markingPaidId, setMarkingPaidId] = useState<string | null>(null);

  const { data: pendingExtensions, isLoading: extensionsLoading } =
    usePendingExtensions();

  const invalidateExtensionQueries = () => {
    queryClient.invalidateQueries({ queryKey: ['pendingExtensions'] });
    queryClient.invalidateQueries({ queryKey: ['contractExtensions'] });
    queryClient.invalidateQueries({ queryKey: ['contracts'] });
    queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    queryClient.invalidateQueries({ queryKey: ['upcomingRenewals'] });
  };

  const activateExtensionMutation = useMutation({
    mutationFn: ({
      contractId,
      extensionId,
    }: {
      contractId: string;
      extensionId: string;
    }) => extensionsApi.activateExtension(contractId, extensionId),
    onSuccess: () => {
      invalidateExtensionQueries();
      showToast(t('dashboard.extensionActivated'), 'success');
    },
    onError: () => {
      showToast(t('dashboard.extensionActivateFailed'), 'error');
    },
  });

  const declineExtensionMutation = useMutation({
    mutationFn: ({
      contractId,
      extensionId,
    }: {
      contractId: string;
      extensionId: string;
    }) => extensionsApi.declineExtension(contractId, extensionId, {}),
    onSuccess: () => {
      invalidateExtensionQueries();
      showToast(t('dashboard.extensionDeclined'), 'success');
    },
    onError: () => {
      showToast(t('dashboard.extensionDeclineFailed'), 'error');
    },
  });

  const unpaidPayments = useMemo(() => {
    if (!allPayments) {
      return [];
    }
    return allPayments
      .filter((p) => p.status === 'PENDING' || p.status === 'OVERDUE')
      .sort(
        (a, b) => new Date(a.dueDate).getTime() - new Date(b.dueDate).getTime()
      );
  }, [allPayments]);

  const totalPending = useMemo(() => {
    return unpaidPayments.reduce((sum, p) => sum + p.amount, 0);
  }, [unpaidPayments]);

  const pendingCurrency = unpaidPayments[0]?.currency || '€';

  const handleMarkPaid = (paymentId: string) => {
    setMarkingPaidId(paymentId);
    markPaidMutation.mutate(
      {
        id: paymentId,
        data: { paymentDate: new Date().toISOString().split('T')[0] },
      },
      { onSettled: () => setMarkingPaidId(null) }
    );
  };

  if (statsLoading) {
    return (
      <div className="space-y-8">
        {/* Summary cards skeleton */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {Array.from({ length: 4 }).map((_, i) => (
            <div
              key={i}
              className="bg-surface-card rounded-lg border border-border-default p-6 space-y-3"
            >
              <div className="flex items-center justify-between">
                <Skeleton className="h-4 w-28" />
                <Skeleton className="h-8 w-8 rounded" />
              </div>
              <Skeleton className="h-8 w-20" />
              <Skeleton className="h-4 w-36" />
            </div>
          ))}
        </div>
        {/* Payments table skeleton */}
        <div className="bg-surface-card rounded-lg border border-border-default p-6 space-y-4">
          <Skeleton className="h-6 w-48" />
          <div className="space-y-2">
            {Array.from({ length: 5 }).map((_, i) => (
              <Skeleton key={i} className="h-12 w-full rounded" />
            ))}
          </div>
        </div>
        {/* Charts skeleton */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <div className="bg-surface-card rounded-lg border border-border-default p-6 space-y-4">
            <Skeleton className="h-6 w-40" />
            <Skeleton className="h-48 w-full rounded" />
          </div>
          <div className="bg-surface-card rounded-lg border border-border-default p-6 space-y-4">
            <Skeleton className="h-6 w-40" />
            <Skeleton className="h-48 w-full rounded" />
          </div>
        </div>
      </div>
    );
  }

  if (statsError) {
    return (
      <div className="p-8">
        <ErrorMessage message={t('dashboard.failedToLoad')} />
      </div>
    );
  }

  const overdueCount = unpaidPayments.filter(
    (p) => p.status === 'OVERDUE' || p.status === 'LATE'
  ).length;

  return (
    <div className="space-y-8">
      {/* Phone-only above-the-fold summary: alerts + hero KPI + KPI rail. */}
      <MobileDashboardSummary
        stats={stats}
        overdueCount={overdueCount}
        pendingExtensionsCount={pendingExtensions?.length ?? 0}
      />

      {/* Pending Invitations */}
      <PendingInvitationsPanel />

      {/* Pending Extensions */}
      <PendingExtensionsPanel
        extensions={pendingExtensions ?? []}
        isLoading={extensionsLoading}
        onActivate={(contractId, extensionId) =>
          activateExtensionMutation.mutate({ contractId, extensionId })
        }
        onDecline={(contractId, extensionId) =>
          declineExtensionMutation.mutate({ contractId, extensionId })
        }
        isActivating={activateExtensionMutation.isPending}
      />

      {/* Portfolio Dashboard */}
      <PortfolioDashboard />

      {/* Statistics Cards — phone shows MobileDashboardSummary above instead. */}
      <div className="hidden md:grid md:grid-cols-2 lg:grid-cols-4 gap-6">
        {/* Total Properties */}
        <div className="bg-surface-card rounded-lg shadow-sm p-6 hover:shadow-lg transition-shadow duration-300 border border-border-default hover:border-info-border">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-medium text-text-secondary">
              {t('dashboard.totalProperties')}
            </h3>
            <div className="p-2 bg-primary-100 rounded-lg">
              <Home className="h-5 w-5 text-primary-500" />
            </div>
          </div>
          <div className="text-2xl md:text-3xl font-bold text-text-primary">
            {stats?.totalProperties ?? 0}
          </div>
          <div className="text-sm text-text-secondary mt-2">
            {t('dashboard.activeProperties')}
          </div>
        </div>

        {/* Occupied Units */}
        <div className="bg-surface-card rounded-lg shadow-sm p-6 hover:shadow-lg transition-shadow duration-300 border border-border-default hover:border-success-border">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-medium text-text-secondary">
              {t('dashboard.occupied')}
            </h3>
            <div className="p-2 bg-success-bg rounded-lg">
              <Users className="h-5 w-5 text-success-text" />
            </div>
          </div>
          <div className="text-2xl md:text-3xl font-bold text-text-primary">
            {stats?.occupiedUnits ?? 0}
          </div>
          <div className="text-sm text-text-secondary mt-2">
            {stats?.selfOccupiedUnits
              ? `${stats.selfOccupiedUnits} ${t('dashboard.selfOccupied')},`
              : ''}
            {stats?.vacantUnits ?? 0} {t('dashboard.vacant')},{' '}
            {stats?.maintenanceUnits ?? 0} {t('dashboard.inMaintenance')}
          </div>
        </div>

        {/* Occupancy Rate */}
        <div className="bg-surface-card rounded-lg shadow-sm p-6 hover:shadow-lg transition-shadow duration-300 border border-border-default hover:border-primary-200">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-medium text-text-secondary">
              <MetricHint label={t('dashboard.occupancyRate')} />
            </h3>
            <div className="p-2 bg-primary-50 rounded-lg">
              <TrendingUp className="h-5 w-5 text-primary-700" />
            </div>
          </div>
          <div className="text-2xl md:text-3xl font-bold text-text-primary">
            {stats?.occupancyRate?.toFixed(1) ?? 0}%
          </div>
          <div className="text-sm text-text-secondary mt-2">
            {t('dashboard.currentOccupancy')}
          </div>
        </div>

        {/* Monthly Income */}
        <div className="bg-surface-card rounded-lg shadow-sm p-6 hover:shadow-lg transition-shadow duration-300 border border-border-default hover:border-success-border">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-medium text-text-secondary">
              {t('dashboard.monthlyIncome')}
            </h3>
            <div className="p-2 bg-success-bg rounded-lg">
              <DollarSign className="h-5 w-5 text-success-text" />
            </div>
          </div>
          <div className="text-2xl md:text-3xl font-bold text-text-primary">
            {stats?.monthlyIncome?.currency
              ? new Intl.NumberFormat('nl-NL', {
                  style: 'currency',
                  currency: stats.monthlyIncome.currency,
                  minimumFractionDigits: 0,
                  maximumFractionDigits: 0,
                }).format(stats?.monthlyIncome?.amount ?? 0)
              : (stats?.monthlyIncome?.amount ?? 0).toLocaleString('nl-NL', {
                  minimumFractionDigits: 0,
                  maximumFractionDigits: 0,
                })}
          </div>
          <div className="text-sm text-text-secondary mt-2">
            {t('dashboard.expectedRevenue')}
          </div>
        </div>
      </div>

      {/* Property Status Breakdown */}
      {stats &&
        stats.totalProperties > 0 &&
        (() => {
          const statuses = [
            {
              label: t('dashboard.occupied'),
              count: stats.occupiedUnits,
              color: 'bg-success',
              dotColor: 'bg-success',
              textColor: 'text-success-text',
            },
            ...(stats.selfOccupiedUnits > 0
              ? [
                  {
                    label: t('dashboard.selfOccupied'),
                    count: stats.selfOccupiedUnits,
                    color: 'bg-info',
                    dotColor: 'bg-info',
                    textColor: 'text-info-text',
                  },
                ]
              : []),
            {
              label: t('dashboard.vacant'),
              count: stats.vacantUnits,
              color: 'bg-warning',
              dotColor: 'bg-warning',
              textColor: 'text-warning-text',
            },
            {
              label: t('dashboard.maintenance'),
              count: stats.maintenanceUnits,
              color: 'bg-amber-500',
              dotColor: 'bg-amber-500',
              textColor: 'text-text-secondary',
            },
            {
              label: t('dashboard.unavailable'),
              count: stats.unavailableUnits,
              color: 'bg-neutral-300',
              dotColor: 'bg-neutral-400',
              textColor: 'text-text-muted',
            },
          ];

          return (
            <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
              <h2 className="text-base font-semibold text-text-primary mb-5">
                {t('dashboard.propertyStatus')}
              </h2>

              {/* Stacked horizontal bar */}
              <div className="flex h-3 rounded-full overflow-hidden mb-6">
                {statuses.map((s) =>
                  s.count > 0 ? (
                    <div
                      key={s.label}
                      className={`${s.color} first:rounded-l-full last:rounded-r-full`}
                      style={{
                        width: `${(s.count / stats.totalProperties) * 100}%`,
                      }}
                    />
                  ) : null
                )}
              </div>

              {/* Legend rows */}
              <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-4">
                {statuses.map((s) => {
                  const pct = ((s.count / stats.totalProperties) * 100).toFixed(
                    0
                  );
                  return (
                    <div key={s.label} className="flex flex-col gap-1">
                      <div className="flex items-center gap-2">
                        <span
                          className={`h-2.5 w-2.5 rounded-full ${s.dotColor} shrink-0`}
                        />
                        <span className="text-xs font-medium text-text-secondary">
                          {s.label}
                        </span>
                      </div>
                      <div className="pl-[18px]">
                        <span className="text-lg font-bold text-text-primary tabular-nums">
                          {s.count}
                        </span>
                        <span className="text-xs text-text-muted ml-1.5">
                          {pct}%
                        </span>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          );
        })()}

      {/* Unpaid Payments */}
      <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-3">
            <h2 className="text-xl font-semibold text-text-primary">
              {t('dashboard.unpaidPayments')}
            </h2>
            {unpaidPayments.length > 0 && (
              <span className="px-2.5 py-0.5 rounded-full text-sm font-medium bg-warning-bg text-warning-text">
                {unpaidPayments.length}
              </span>
            )}
          </div>
          <div className="flex items-center gap-4">
            {unpaidPayments.length > 0 && (
              <div className="text-right">
                <div className="text-sm text-text-secondary">
                  {t('dashboard.totalPending')}
                </div>
                <div className="text-lg font-bold text-warning-text">
                  {formatMoney(totalPending, pendingCurrency)}
                </div>
              </div>
            )}
            <button
              onClick={() => navigate('/payments')}
              className="text-sm text-primary-500 hover:text-primary-600 font-medium flex items-center gap-1"
            >
              {t('dashboard.viewAll')}
              <ArrowRight className="h-4 w-4" />
            </button>
          </div>
        </div>

        {paymentsLoading ? (
          <div className="space-y-3 py-4">
            {Array.from({ length: 3 }).map((_, i) => (
              <Skeleton key={i} className="h-12 w-full rounded" />
            ))}
          </div>
        ) : unpaidPayments.length > 0 ? (
          <div className="divide-y divide-border-default">
            {unpaidPayments.slice(0, 10).map((payment) => {
              const isOverdue = payment.status === 'OVERDUE';
              return (
                <div
                  key={payment.identifier}
                  className="flex items-center justify-between py-3 gap-4"
                >
                  <div className="flex items-center gap-3 min-w-0 flex-1">
                    <div
                      className={`flex-shrink-0 p-2 rounded-lg ${isOverdue ? 'bg-error-bg' : 'bg-warning-bg'}`}
                    >
                      {isOverdue ? (
                        <AlertTriangle className="h-4 w-4 text-error-text" />
                      ) : (
                        <Clock className="h-4 w-4 text-warning-text" />
                      )}
                    </div>
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center gap-2">
                        <button
                          onClick={() =>
                            navigate(`/payments/${payment.identifier}`)
                          }
                          className="text-sm font-medium text-text-primary hover:text-primary-500 truncate"
                        >
                          {payment.property?.street || t('dashboard.payment')}{' '}
                          &mdash; {payment.contact?.firstName}{' '}
                          {payment.contact?.lastName}
                        </button>
                        <span
                          className={`flex-shrink-0 px-2 py-0.5 rounded text-xs font-medium ${isOverdue ? 'bg-error-bg text-error-text' : 'bg-warning-bg text-warning-text'}`}
                        >
                          {payment.status}
                        </span>
                      </div>
                      <p className="text-xs text-text-secondary mt-0.5">
                        Due {formatDate(payment.dueDate)}
                        {isOverdue && (
                          <span className="text-error-text ml-1">
                            (
                            {formatRelative(payment.dueDate).replace(
                              / ago$/,
                              ''
                            )}{' '}
                            overdue)
                          </span>
                        )}
                      </p>
                    </div>
                  </div>
                  <div className="flex items-center gap-3 flex-shrink-0">
                    <span className="text-sm font-semibold text-text-primary">
                      {formatMoney(payment.amount, payment.currency)}
                    </span>
                    {canEditData && (
                      <button
                        onClick={() => handleMarkPaid(payment.identifier)}
                        disabled={markingPaidId === payment.identifier}
                        className="inline-flex items-center gap-1 px-3 py-1.5 text-xs font-medium rounded-lg bg-success-bg text-success-text hover:bg-success-bg/80 border border-success-border transition-colors disabled:opacity-50"
                      >
                        <CheckCircle className="h-3.5 w-3.5" />
                        {markingPaidId === payment.identifier
                          ? t('dashboard.saving')
                          : t('dashboard.markPaid')}
                      </button>
                    )}
                  </div>
                </div>
              );
            })}
            {unpaidPayments.length > 10 && (
              <div className="pt-3 text-center">
                <button
                  onClick={() => navigate('/payments')}
                  className="text-sm text-primary-500 hover:text-primary-600 font-medium"
                >
                  {t('dashboard.moreUnpaid', {
                    count: unpaidPayments.length - 10,
                  })}
                </button>
              </div>
            )}
          </div>
        ) : (
          <div className="text-center py-8">
            <CheckCircle className="h-10 w-10 text-success-text mx-auto mb-2" />
            <p className="text-text-secondary">{t('dashboard.allUpToDate')}</p>
          </div>
        )}
      </div>
    </div>
  );
};

DashboardPage.displayName = 'DashboardPage';
