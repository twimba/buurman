import { useMemo, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useDashboardStats } from '@/hooks/useDashboard';
import { usePayments, useMarkPaymentAsPaid } from '@/hooks/usePaymentHooks';
import { usePendingExtensions } from '@/hooks/useContractExtensionHooks';
import * as extensionsApi from '@/api/contractExtensions';
import { useTeam } from '@/context/TeamContext';
import { useToast } from '@/context/ToastContext';
import { LoadingSpinner } from './LoadingSpinner';
import { MetricHint } from '@/components/common/MetricHint';
import { ErrorMessage } from './ErrorMessage';
import { PendingInvitationsPanel } from './dashboard/PendingInvitationsPanel';
import { PendingExtensionsPanel } from './dashboard/PendingExtensionsPanel';
import { PortfolioDashboard } from './dashboard/PortfolioDashboard';
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
import { formatDistanceToNow } from 'date-fns';
import { useFormatDate } from '@/hooks/useFormatDate';

export const DashboardPage = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { formatDate } = useFormatDate();
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
      showToast('Extension activated successfully', 'success');
    },
    onError: () => {
      showToast('Failed to activate extension', 'error');
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
      showToast('Extension declined', 'success');
    },
    onError: () => {
      showToast('Failed to decline extension', 'error');
    },
  });

  const unpaidPayments = useMemo(() => {
    if (!allPayments) {
      return [];
    }
    return allPayments
      .filter((p) => p.status === 'PENDING' || p.status === 'OVERDUE' || p.status === 'LATE')
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
      <div className="flex items-center justify-center min-h-[400px]">
        <LoadingSpinner />
      </div>
    );
  }

  if (statsError) {
    return (
      <div className="p-8">
        <ErrorMessage message="Failed to load dashboard statistics. Please try again." />
      </div>
    );
  }

  return (
    <div className="space-y-8">
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

      {/* Statistics Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {/* Total Properties */}
        <div className="bg-surface-card rounded-lg shadow-sm p-6 hover:shadow-lg transition-shadow duration-300 border border-border-default hover:border-info-border">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-medium text-text-secondary">
              Total Properties
            </h3>
            <div className="p-2 bg-primary-100 rounded-lg">
              <Home className="h-5 w-5 text-primary-500" />
            </div>
          </div>
          <div className="text-3xl font-bold text-text-primary">
            {stats?.totalProperties ?? 0}
          </div>
          <div className="text-sm text-text-secondary mt-2">
            Active properties in portfolio
          </div>
        </div>

        {/* Occupied Units */}
        <div className="bg-surface-card rounded-lg shadow-sm p-6 hover:shadow-lg transition-shadow duration-300 border border-border-default hover:border-success-border">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-medium text-text-secondary">
              Occupied
            </h3>
            <div className="p-2 bg-success-bg rounded-lg">
              <Users className="h-5 w-5 text-success-text" />
            </div>
          </div>
          <div className="text-3xl font-bold text-text-primary">
            {stats?.occupiedUnits ?? 0}
          </div>
          <div className="text-sm text-text-secondary mt-2">
            {stats?.selfOccupiedUnits
              ? `${stats.selfOccupiedUnits} self-occupied,`
              : ''}
            {stats?.vacantUnits ?? 0} vacant, {stats?.maintenanceUnits ?? 0} in
            maintenance
          </div>
        </div>

        {/* Occupancy Rate */}
        <div className="bg-surface-card rounded-lg shadow-sm p-6 hover:shadow-lg transition-shadow duration-300 border border-border-default hover:border-primary-200">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-medium text-text-secondary">
              <MetricHint label="Occupancy Rate" />
            </h3>
            <div className="p-2 bg-primary-50 rounded-lg">
              <TrendingUp className="h-5 w-5 text-primary-700" />
            </div>
          </div>
          <div className="text-3xl font-bold text-text-primary">
            {stats?.occupancyRate?.toFixed(1) ?? 0}%
          </div>
          <div className="text-sm text-text-secondary mt-2">
            Current occupancy level
          </div>
        </div>

        {/* Monthly Income */}
        <div className="bg-surface-card rounded-lg shadow-sm p-6 hover:shadow-lg transition-shadow duration-300 border border-border-default hover:border-success-border">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-medium text-text-secondary">
              Monthly Income
            </h3>
            <div className="p-2 bg-success-bg rounded-lg">
              <DollarSign className="h-5 w-5 text-success-text" />
            </div>
          </div>
          <div className="text-3xl font-bold text-text-primary">
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
            Expected monthly revenue
          </div>
        </div>
      </div>

      {/* Property Status Breakdown */}
      {stats &&
        stats.totalProperties > 0 &&
        (() => {
          const statuses = [
            {
              label: 'Occupied',
              count: stats.occupiedUnits,
              color: 'bg-success',
              dotColor: 'bg-success',
              textColor: 'text-success-text',
            },
            ...(stats.selfOccupiedUnits > 0
              ? [
                  {
                    label: 'Self-Occupied',
                    count: stats.selfOccupiedUnits,
                    color: 'bg-info',
                    dotColor: 'bg-info',
                    textColor: 'text-info-text',
                  },
                ]
              : []),
            {
              label: 'Vacant',
              count: stats.vacantUnits,
              color: 'bg-warning',
              dotColor: 'bg-warning',
              textColor: 'text-warning-text',
            },
            {
              label: 'Maintenance',
              count: stats.maintenanceUnits,
              color: 'bg-amber-500',
              dotColor: 'bg-amber-500',
              textColor: 'text-text-secondary',
            },
            {
              label: 'Unavailable',
              count: stats.unavailableUnits,
              color: 'bg-neutral-300',
              dotColor: 'bg-neutral-400',
              textColor: 'text-text-muted',
            },
          ];

          return (
            <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
              <h2 className="text-base font-semibold text-text-primary mb-5">
                Property Status
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
              Unpaid Payments
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
                <div className="text-sm text-text-secondary">Total pending</div>
                <div className="text-lg font-bold text-warning-text">
                  {pendingCurrency}
                  {totalPending.toLocaleString(undefined, {
                    minimumFractionDigits: 2,
                    maximumFractionDigits: 2,
                  })}
                </div>
              </div>
            )}
            <button
              onClick={() => navigate('/payments')}
              className="text-sm text-primary-500 hover:text-primary-600 font-medium flex items-center gap-1"
            >
              View all
              <ArrowRight className="h-4 w-4" />
            </button>
          </div>
        </div>

        {paymentsLoading ? (
          <div className="flex items-center justify-center py-8">
            <LoadingSpinner />
          </div>
        ) : unpaidPayments.length > 0 ? (
          <div className="divide-y divide-border-default">
            {unpaidPayments.slice(0, 10).map((payment) => {
              const isOverdue = payment.status === 'OVERDUE' || payment.status === 'LATE';
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
                          {payment.property?.street || 'Payment'} &mdash;{' '}
                          {payment.tenant?.firstName} {payment.tenant?.lastName}
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
                            {formatDistanceToNow(new Date(payment.dueDate), {
                              addSuffix: false,
                            })}{' '}
                            overdue)
                          </span>
                        )}
                      </p>
                    </div>
                  </div>
                  <div className="flex items-center gap-3 flex-shrink-0">
                    <span className="text-sm font-semibold text-text-primary">
                      {payment.currency}
                      {payment.amount.toLocaleString(undefined, {
                        minimumFractionDigits: 2,
                        maximumFractionDigits: 2,
                      })}
                    </span>
                    {canEditData && (
                      <button
                        onClick={() => handleMarkPaid(payment.identifier)}
                        disabled={markingPaidId === payment.identifier}
                        className="inline-flex items-center gap-1 px-3 py-1.5 text-xs font-medium rounded-lg bg-success-bg text-success-text hover:bg-success-bg/80 border border-success-border transition-colors disabled:opacity-50"
                      >
                        <CheckCircle className="h-3.5 w-3.5" />
                        {markingPaidId === payment.identifier
                          ? 'Saving...'
                          : 'Mark Paid'}
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
                  +{unpaidPayments.length - 10} more unpaid payments
                </button>
              </div>
            )}
          </div>
        ) : (
          <div className="text-center py-8">
            <CheckCircle className="h-10 w-10 text-success-text mx-auto mb-2" />
            <p className="text-text-secondary">All payments are up to date</p>
          </div>
        )}
      </div>
    </div>
  );
};
