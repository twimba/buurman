import { useMemo, useState } from 'react';
import { useDashboardStats, useRecentActivities } from '@/hooks/useDashboard';
import { usePayments, useMarkPaymentAsPaid } from '@/hooks/usePaymentHooks';
import { useTeam } from '@/context/TeamContext';
import { LoadingSpinner } from './LoadingSpinner';
import { MetricHint } from '@/components/common/MetricHint';
import { ErrorMessage } from './ErrorMessage';
import { PropertyStatusChart } from './PropertyStatusChart';
import { PendingInvitationsPanel } from './dashboard/PendingInvitationsPanel';
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
  const { formatDate, formatRelative } = useFormatDate();
  const { canEditData } = useTeam();
  const {
    data: stats,
    isLoading: statsLoading,
    error: statsError,
  } = useDashboardStats();
  const { data: activities, isLoading: activitiesLoading } =
    useRecentActivities(10);
  const { data: allPaymentsData, isLoading: paymentsLoading } = usePayments();
  const allPayments = allPaymentsData?.content;
  const markPaidMutation = useMarkPaymentAsPaid();
  const [markingPaidId, setMarkingPaidId] = useState<string | null>(null);

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

      {/* Property Status Distribution */}
      {stats && stats.totalProperties > 0 && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Status Cards */}
          <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
            <h2 className="text-xl font-semibold text-text-primary mb-4">
              Property Status Breakdown
            </h2>
            <div className="space-y-3">
              <div className="flex items-center justify-between p-4 bg-success-bg rounded-lg border border-success-border">
                <div>
                  <div className="text-sm font-medium text-success-text">
                    Occupied
                  </div>
                  <div className="text-2xl font-bold text-success-text mt-1">
                    {stats.occupiedUnits}
                  </div>
                </div>
                <div className="text-sm text-success-text">
                  {(
                    (stats.occupiedUnits / stats.totalProperties) *
                    100
                  ).toFixed(0)}
                  %
                </div>
              </div>
              {stats.selfOccupiedUnits > 0 && (
                <div className="flex items-center justify-between p-4 bg-info-bg rounded-lg border border-info-border">
                  <div>
                    <div className="text-sm font-medium text-info-text">
                      Self-Occupied
                    </div>
                    <div className="text-2xl font-bold text-info-text mt-1">
                      {stats.selfOccupiedUnits}
                    </div>
                  </div>
                  <div className="text-sm text-info-text">
                    {(
                      (stats.selfOccupiedUnits / stats.totalProperties) *
                      100
                    ).toFixed(0)}
                    %
                  </div>
                </div>
              )}
              <div className="flex items-center justify-between p-4 bg-warning-bg rounded-lg border border-warning-border">
                <div>
                  <div className="text-sm font-medium text-warning-text">
                    Vacant
                  </div>
                  <div className="text-2xl font-bold text-warning-text mt-1">
                    {stats.vacantUnits}
                  </div>
                </div>
                <div className="text-sm text-warning-text">
                  {((stats.vacantUnits / stats.totalProperties) * 100).toFixed(
                    0
                  )}
                  %
                </div>
              </div>
              <div className="flex items-center justify-between p-4 bg-warning-bg rounded-lg border border-warning-border">
                <div>
                  <div className="text-sm font-medium text-warning-text">
                    Maintenance
                  </div>
                  <div className="text-2xl font-bold text-warning-text mt-1">
                    {stats.maintenanceUnits}
                  </div>
                </div>
                <div className="text-sm text-warning-text">
                  {(
                    (stats.maintenanceUnits / stats.totalProperties) *
                    100
                  ).toFixed(0)}
                  %
                </div>
              </div>
              <div className="flex items-center justify-between p-4 bg-surface-page rounded-lg border border-border-default">
                <div>
                  <div className="text-sm font-medium text-text-primary">
                    Unavailable
                  </div>
                  <div className="text-2xl font-bold text-text-secondary mt-1">
                    {stats.unavailableUnits}
                  </div>
                </div>
                <div className="text-sm text-text-secondary">
                  {(
                    (stats.unavailableUnits / stats.totalProperties) *
                    100
                  ).toFixed(0)}
                  %
                </div>
              </div>
            </div>
          </div>

          {/* Status Chart */}
          <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
            <h2 className="text-xl font-semibold text-text-primary mb-4">
              Distribution Overview
            </h2>
            <PropertyStatusChart
              occupied={stats.occupiedUnits}
              selfOccupied={stats.selfOccupiedUnits}
              vacant={stats.vacantUnits}
              maintenance={stats.maintenanceUnits}
              unavailable={stats.unavailableUnits}
            />
          </div>
        </div>
      )}

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

      {/* Recent Activities */}
      <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-semibold text-text-primary">
            Recent Activities
          </h2>
          {activities && activities.length > 0 && (
            <button
              onClick={() => navigate('/admin/activity-log')}
              className="text-sm text-primary-500 hover:text-primary-600 font-medium flex items-center gap-1"
            >
              View all
              <ArrowRight className="h-4 w-4" />
            </button>
          )}
        </div>

        {activitiesLoading ? (
          <div className="flex items-center justify-center py-8">
            <LoadingSpinner />
          </div>
        ) : activities && activities.length > 0 ? (
          <div className="space-y-4">
            {activities.map((activity) => (
              <div
                key={`${activity.entityType}-${activity.entityIdentifier}-${activity.timestamp}`}
                className="flex items-start gap-4 p-4 hover:bg-surface-inset rounded-lg transition-colors"
              >
                <div
                  className={`
                    flex-shrink-0 w-10 h-10 rounded-full flex items-center justify-center
                    ${
                      activity.action === 'CREATE'
                        ? 'bg-success-bg'
                        : activity.action === 'UPDATE'
                          ? 'bg-info-bg'
                          : 'bg-error-bg'
                    }
                  `}
                >
                  <span
                    className={`
                      text-xs font-semibold
                      ${
                        activity.action === 'CREATE'
                          ? 'text-success-text'
                          : activity.action === 'UPDATE'
                            ? 'text-info-text'
                            : 'text-error-text'
                      }
                    `}
                  >
                    {activity.action.charAt(0)}
                  </span>
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm text-text-primary">
                    {activity.description}
                  </p>
                  <p className="text-xs text-text-secondary mt-1">
                    {formatRelative(activity.timestamp)}
                  </p>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="text-center py-8">
            <p className="text-text-secondary">No recent activities</p>
            <p className="text-sm text-text-muted mt-1">
              Activities will appear here as you use the system
            </p>
          </div>
        )}
      </div>
    </div>
  );
};
