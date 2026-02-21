import { useMemo, useState } from 'react';
import { useDashboardStats, useRecentActivities } from '@/hooks/useDashboard';
import { usePayments, useMarkPaymentAsPaid } from '@/hooks/usePaymentHooks';
import { useTeam } from '@/context/TeamContext';
import { LoadingSpinner } from './LoadingSpinner';
import { MetricHint } from '@/components/common/MetricHint';
import { ErrorMessage } from './ErrorMessage';
import { PropertyStatusChart } from './PropertyStatusChart';
import { PendingInvitationsPanel } from './dashboard/PendingInvitationsPanel';
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
    if (!allPayments) return [];
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

      {/* Statistics Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {/* Total Properties */}
        <div className="bg-white dark:bg-[#14161f] rounded-xl shadow-sm p-6 hover:shadow-lg transition-shadow duration-300 border border-[#edf0f7] dark:border-[#2a2e3f] hover:border-blue-200">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-medium text-[#6b7194] dark:text-[#8b90a8]">
              Total Properties
            </h3>
            <div className="p-2 bg-primary-100 dark:bg-primary-500/10 rounded-lg">
              <Home className="h-5 w-5 text-primary-500 dark:text-primary-300" />
            </div>
          </div>
          <div className="text-3xl font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
            {stats?.totalProperties || 0}
          </div>
          <div className="text-sm text-[#6b7194] dark:text-[#8b90a8] mt-2">
            Active properties in portfolio
          </div>
        </div>

        {/* Occupied Units */}
        <div className="bg-white dark:bg-[#14161f] rounded-xl shadow-sm p-6 hover:shadow-lg transition-shadow duration-300 border border-[#edf0f7] dark:border-[#2a2e3f] hover:border-green-200">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-medium text-[#6b7194] dark:text-[#8b90a8]">
              Occupied
            </h3>
            <div className="p-2 bg-green-100 dark:bg-green-900/30 rounded-lg">
              <Users className="h-5 w-5 text-green-600 dark:text-green-400" />
            </div>
          </div>
          <div className="text-3xl font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
            {stats?.occupiedUnits || 0}
          </div>
          <div className="text-sm text-[#6b7194] dark:text-[#8b90a8] mt-2">
            {stats?.vacantUnits || 0} vacant, {stats?.maintenanceUnits || 0} in
            maintenance, {stats?.unavailableUnits || 0} unavailable
          </div>
        </div>

        {/* Occupancy Rate */}
        <div className="bg-white dark:bg-[#14161f] rounded-xl shadow-sm p-6 hover:shadow-lg transition-shadow duration-300 border border-[#edf0f7] dark:border-[#2a2e3f] hover:border-purple-200">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-medium text-[#6b7194] dark:text-[#8b90a8]">
              <MetricHint label="Occupancy Rate" />
            </h3>
            <div className="p-2 bg-purple-100 dark:bg-purple-900/30 rounded-lg">
              <TrendingUp className="h-5 w-5 text-purple-600 dark:text-purple-400" />
            </div>
          </div>
          <div className="text-3xl font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
            {stats?.occupancyRate?.toFixed(1) || 0}%
          </div>
          <div className="text-sm text-[#6b7194] dark:text-[#8b90a8] mt-2">
            Current occupancy level
          </div>
        </div>

        {/* Monthly Income */}
        <div className="bg-white dark:bg-[#14161f] rounded-xl shadow-sm p-6 hover:shadow-lg transition-shadow duration-300 border border-[#edf0f7] dark:border-[#2a2e3f] hover:border-emerald-200">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-medium text-[#6b7194] dark:text-[#8b90a8]">
              Monthly Income
            </h3>
            <div className="p-2 bg-emerald-100 dark:bg-emerald-900/30 rounded-lg">
              <DollarSign className="h-5 w-5 text-emerald-600 dark:text-emerald-400" />
            </div>
          </div>
          <div className="text-3xl font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
            {stats?.monthlyIncome?.currency
              ? new Intl.NumberFormat('nl-NL', {
                  style: 'currency',
                  currency: stats.monthlyIncome.currency,
                  minimumFractionDigits: 0,
                  maximumFractionDigits: 0,
                }).format(stats?.monthlyIncome?.amount || 0)
              : (stats?.monthlyIncome?.amount || 0).toLocaleString('nl-NL', {
                  minimumFractionDigits: 0,
                  maximumFractionDigits: 0,
                })}
          </div>
          <div className="text-sm text-[#6b7194] dark:text-[#8b90a8] mt-2">
            Expected monthly revenue
          </div>
        </div>
      </div>

      {/* Property Status Distribution */}
      {stats && stats.totalProperties > 0 && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Status Cards */}
          <div className="bg-white dark:bg-[#14161f] rounded-xl shadow-sm p-6">
            <h2 className="text-xl font-semibold text-[#1a1d2e] dark:text-[#c4c8db] mb-4">
              Property Status Breakdown
            </h2>
            <div className="space-y-3">
              <div className="flex items-center justify-between p-4 bg-green-50 dark:bg-green-900/30 rounded-lg border border-green-200 dark:border-green-800">
                <div>
                  <div className="text-sm font-medium text-green-900 dark:text-green-100">
                    Occupied
                  </div>
                  <div className="text-2xl font-bold text-green-600 dark:text-green-400 mt-1">
                    {stats.occupiedUnits}
                  </div>
                </div>
                <div className="text-sm text-green-700 dark:text-green-300">
                  {(
                    (stats.occupiedUnits / stats.totalProperties) *
                    100
                  ).toFixed(0)}
                  %
                </div>
              </div>
              <div className="flex items-center justify-between p-4 bg-yellow-50 dark:bg-yellow-900/30 rounded-lg border border-yellow-200 dark:border-yellow-800">
                <div>
                  <div className="text-sm font-medium text-yellow-900 dark:text-yellow-100">
                    Vacant
                  </div>
                  <div className="text-2xl font-bold text-yellow-600 dark:text-yellow-400 mt-1">
                    {stats.vacantUnits}
                  </div>
                </div>
                <div className="text-sm text-yellow-700 dark:text-yellow-300">
                  {((stats.vacantUnits / stats.totalProperties) * 100).toFixed(
                    0
                  )}
                  %
                </div>
              </div>
              <div className="flex items-center justify-between p-4 bg-orange-50 dark:bg-orange-900/30 rounded-lg border border-orange-200 dark:border-orange-800">
                <div>
                  <div className="text-sm font-medium text-orange-900 dark:text-orange-100">
                    Maintenance
                  </div>
                  <div className="text-2xl font-bold text-orange-600 dark:text-orange-400 mt-1">
                    {stats.maintenanceUnits}
                  </div>
                </div>
                <div className="text-sm text-orange-700 dark:text-orange-300">
                  {(
                    (stats.maintenanceUnits / stats.totalProperties) *
                    100
                  ).toFixed(0)}
                  %
                </div>
              </div>
              <div className="flex items-center justify-between p-4 bg-[#f8f9fc] dark:bg-[#0c0d14] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f]">
                <div>
                  <div className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                    Unavailable
                  </div>
                  <div className="text-2xl font-bold text-[#6b7194] dark:text-[#8b90a8] mt-1">
                    {stats.unavailableUnits}
                  </div>
                </div>
                <div className="text-sm text-[#3d4463] dark:text-[#c4c8db]">
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
          <div className="bg-white dark:bg-[#14161f] rounded-xl shadow-sm p-6">
            <h2 className="text-xl font-semibold text-[#1a1d2e] dark:text-[#c4c8db] mb-4">
              Distribution Overview
            </h2>
            <PropertyStatusChart
              occupied={stats.occupiedUnits}
              vacant={stats.vacantUnits}
              maintenance={stats.maintenanceUnits}
              unavailable={stats.unavailableUnits}
            />
          </div>
        </div>
      )}

      {/* Unpaid Payments */}
      <div className="bg-white dark:bg-[#14161f] rounded-xl shadow-sm p-6">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-3">
            <h2 className="text-xl font-semibold text-[#1a1d2e] dark:text-[#c4c8db]">
              Unpaid Payments
            </h2>
            {unpaidPayments.length > 0 && (
              <span className="px-2.5 py-0.5 rounded-full text-sm font-medium bg-amber-100 dark:bg-amber-900/30 text-amber-800 dark:text-amber-300">
                {unpaidPayments.length}
              </span>
            )}
          </div>
          <div className="flex items-center gap-4">
            {unpaidPayments.length > 0 && (
              <div className="text-right">
                <div className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                  Total pending
                </div>
                <div className="text-lg font-bold text-amber-600">
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
              className="text-sm text-[#5c7cfa] hover:text-[#4263eb] font-medium flex items-center gap-1"
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
          <div className="divide-y divide-[#edf0f7] dark:divide-[#2a2e3f]">
            {unpaidPayments.slice(0, 10).map((payment) => {
              const isOverdue = payment.status === 'OVERDUE';
              return (
                <div
                  key={payment.identifier}
                  className="flex items-center justify-between py-3 gap-4"
                >
                  <div className="flex items-center gap-3 min-w-0 flex-1">
                    <div
                      className={`flex-shrink-0 p-2 rounded-lg ${isOverdue ? 'bg-red-100 dark:bg-red-900/30' : 'bg-amber-100 dark:bg-amber-900/30'}`}
                    >
                      {isOverdue ? (
                        <AlertTriangle className="h-4 w-4 text-red-600" />
                      ) : (
                        <Clock className="h-4 w-4 text-amber-600" />
                      )}
                    </div>
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center gap-2">
                        <button
                          onClick={() =>
                            navigate(`/payments/${payment.identifier}`)
                          }
                          className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6] hover:text-[#5c7cfa] truncate"
                        >
                          {payment.property?.street || 'Payment'} &mdash;{' '}
                          {payment.tenant?.firstName} {payment.tenant?.lastName}
                        </button>
                        <span
                          className={`flex-shrink-0 px-2 py-0.5 rounded text-xs font-medium ${isOverdue ? 'bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-300' : 'bg-amber-100 dark:bg-amber-900/30 text-amber-700 dark:text-amber-300'}`}
                        >
                          {payment.status}
                        </span>
                      </div>
                      <p className="text-xs text-[#6b7194] dark:text-[#8b90a8] mt-0.5">
                        Due {formatDate(payment.dueDate)}
                        {isOverdue && (
                          <span className="text-red-500 ml-1">
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
                    <span className="text-sm font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
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
                        className="inline-flex items-center gap-1 px-3 py-1.5 text-xs font-medium rounded-lg bg-green-50 dark:bg-green-900/20 text-green-700 dark:text-green-300 hover:bg-green-100 dark:hover:bg-green-900/30 border border-green-200 dark:border-green-800 transition-colors disabled:opacity-50"
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
                  className="text-sm text-[#5c7cfa] hover:text-[#4263eb] font-medium"
                >
                  +{unpaidPayments.length - 10} more unpaid payments
                </button>
              </div>
            )}
          </div>
        ) : (
          <div className="text-center py-8">
            <CheckCircle className="h-10 w-10 text-green-400 mx-auto mb-2" />
            <p className="text-[#6b7194] dark:text-[#8b90a8]">
              All payments are up to date
            </p>
          </div>
        )}
      </div>

      {/* Recent Activities */}
      <div className="bg-white dark:bg-[#14161f] rounded-xl shadow-sm p-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-semibold text-[#1a1d2e] dark:text-[#c4c8db]">
            Recent Activities
          </h2>
          {activities && activities.length > 0 && (
            <button
              onClick={() => navigate('/admin/activity-log')}
              className="text-sm text-[#5c7cfa] hover:text-[#4263eb] font-medium flex items-center gap-1"
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
                className="flex items-start gap-4 p-4 hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] rounded-lg transition-colors"
              >
                <div
                  className={`
                    flex-shrink-0 w-10 h-10 rounded-full flex items-center justify-center
                    ${
                      activity.action === 'CREATE'
                        ? 'bg-green-100 dark:bg-green-900/30'
                        : activity.action === 'UPDATE'
                          ? 'bg-blue-100 dark:bg-blue-900/30'
                          : 'bg-red-100 dark:bg-red-900/30'
                    }
                  `}
                >
                  <span
                    className={`
                      text-xs font-semibold
                      ${
                        activity.action === 'CREATE'
                          ? 'text-green-700'
                          : activity.action === 'UPDATE'
                            ? 'text-blue-700'
                            : 'text-red-700'
                      }
                    `}
                  >
                    {activity.action.charAt(0)}
                  </span>
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm text-[#1a1d2e] dark:text-[#eef0f6]">
                    {activity.description}
                  </p>
                  <p className="text-xs text-[#6b7194] dark:text-[#8b90a8] mt-1">
                    {formatRelative(activity.timestamp)}
                  </p>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="text-center py-8">
            <p className="text-[#6b7194] dark:text-[#8b90a8]">
              No recent activities
            </p>
            <p className="text-sm text-[#9ca0b8] dark:text-[#5c6180] mt-1">
              Activities will appear here as you use the system
            </p>
          </div>
        )}
      </div>
    </div>
  );
};
