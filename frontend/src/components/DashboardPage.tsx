import { useDashboardStats, useRecentActivities } from '@/hooks/useDashboard';
import { LoadingSpinner } from './LoadingSpinner';
import { ErrorMessage } from './ErrorMessage';
import { PropertyStatusChart } from './PropertyStatusChart';
import {
  Home,
  Users,
  DollarSign,
  TrendingUp,
  Plus,
  ArrowRight,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { formatDistanceToNow } from 'date-fns';

export const DashboardPage = () => {
  const navigate = useNavigate();
  const {
    data: stats,
    isLoading: statsLoading,
    error: statsError,
  } = useDashboardStats();
  const { data: activities, isLoading: activitiesLoading } =
    useRecentActivities(10);

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
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Dashboard</h1>
          <p className="text-gray-600 mt-1">
            Welcome back! Here&apos;s an overview of your properties.
          </p>
        </div>
        <button
          onClick={() => navigate('/properties/new')}
          className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
        >
          <Plus className="h-5 w-5" />
          Add Property
        </button>
      </div>

      {/* Statistics Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {/* Total Properties */}
        <div className="bg-white rounded-lg shadow p-6 hover:shadow-lg transition-shadow duration-300 border border-gray-100 hover:border-blue-200">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-medium text-gray-600">
              Total Properties
            </h3>
            <div className="p-2 bg-blue-100 rounded-lg">
              <Home className="h-5 w-5 text-blue-600" />
            </div>
          </div>
          <div className="text-3xl font-bold text-gray-900">
            {stats?.totalProperties || 0}
          </div>
          <div className="text-sm text-gray-500 mt-2">
            Active properties in portfolio
          </div>
        </div>

        {/* Occupied Units */}
        <div className="bg-white rounded-lg shadow p-6 hover:shadow-lg transition-shadow duration-300 border border-gray-100 hover:border-green-200">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-medium text-gray-600">Occupied</h3>
            <div className="p-2 bg-green-100 rounded-lg">
              <Users className="h-5 w-5 text-green-600" />
            </div>
          </div>
          <div className="text-3xl font-bold text-gray-900">
            {stats?.occupiedUnits || 0}
          </div>
          <div className="text-sm text-gray-500 mt-2">
            {stats?.vacantUnits || 0} vacant, {stats?.maintenanceUnits || 0} in
            maintenance
          </div>
        </div>

        {/* Occupancy Rate */}
        <div className="bg-white rounded-lg shadow p-6 hover:shadow-lg transition-shadow duration-300 border border-gray-100 hover:border-purple-200">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-medium text-gray-600">
              Occupancy Rate
            </h3>
            <div className="p-2 bg-purple-100 rounded-lg">
              <TrendingUp className="h-5 w-5 text-purple-600" />
            </div>
          </div>
          <div className="text-3xl font-bold text-gray-900">
            {stats?.occupancyRate?.toFixed(1) || 0}%
          </div>
          <div className="text-sm text-gray-500 mt-2">
            Current occupancy level
          </div>
        </div>

        {/* Monthly Income */}
        <div className="bg-white rounded-lg shadow p-6 hover:shadow-lg transition-shadow duration-300 border border-gray-100 hover:border-emerald-200">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-medium text-gray-600">
              Monthly Income
            </h3>
            <div className="p-2 bg-emerald-100 rounded-lg">
              <DollarSign className="h-5 w-5 text-emerald-600" />
            </div>
          </div>
          <div className="text-3xl font-bold text-gray-900">
            €{stats?.monthlyIncome?.amount?.toFixed(0) || 0}
          </div>
          <div className="text-sm text-gray-500 mt-2">
            Expected monthly revenue
          </div>
        </div>
      </div>

      {/* Property Status Distribution */}
      {stats && stats.totalProperties > 0 && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Status Cards */}
          <div className="bg-white rounded-lg shadow p-6">
            <h2 className="text-xl font-semibold text-gray-800 mb-4">
              Property Status Breakdown
            </h2>
            <div className="space-y-3">
              <div className="flex items-center justify-between p-4 bg-green-50 rounded-lg border border-green-200">
                <div>
                  <div className="text-sm font-medium text-green-900">
                    Occupied
                  </div>
                  <div className="text-2xl font-bold text-green-600 mt-1">
                    {stats.occupiedUnits}
                  </div>
                </div>
                <div className="text-sm text-green-700">
                  {((stats.occupiedUnits / stats.totalProperties) * 100).toFixed(
                    0
                  )}
                  %
                </div>
              </div>
              <div className="flex items-center justify-between p-4 bg-yellow-50 rounded-lg border border-yellow-200">
                <div>
                  <div className="text-sm font-medium text-yellow-900">
                    Vacant
                  </div>
                  <div className="text-2xl font-bold text-yellow-600 mt-1">
                    {stats.vacantUnits}
                  </div>
                </div>
                <div className="text-sm text-yellow-700">
                  {((stats.vacantUnits / stats.totalProperties) * 100).toFixed(0)}
                  %
                </div>
              </div>
              <div className="flex items-center justify-between p-4 bg-orange-50 rounded-lg border border-orange-200">
                <div>
                  <div className="text-sm font-medium text-orange-900">
                    Maintenance
                  </div>
                  <div className="text-2xl font-bold text-orange-600 mt-1">
                    {stats.maintenanceUnits}
                  </div>
                </div>
                <div className="text-sm text-orange-700">
                  {(
                    (stats.maintenanceUnits / stats.totalProperties) *
                    100
                  ).toFixed(0)}
                  %
                </div>
              </div>
            </div>
          </div>

          {/* Status Chart */}
          <div className="bg-white rounded-lg shadow p-6">
            <h2 className="text-xl font-semibold text-gray-800 mb-4">
              Distribution Overview
            </h2>
            <PropertyStatusChart
              occupied={stats.occupiedUnits}
              vacant={stats.vacantUnits}
              maintenance={stats.maintenanceUnits}
            />
          </div>
        </div>
      )}

      {/* Recent Activities */}
      <div className="bg-white rounded-lg shadow p-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-semibold text-gray-800">
            Recent Activities
          </h2>
          {activities && activities.length > 0 && (
            <button
              onClick={() => navigate('/audit-log')}
              className="text-sm text-blue-600 hover:text-blue-700 font-medium flex items-center gap-1"
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
                key={activity.id}
                className="flex items-start gap-4 p-4 hover:bg-gray-50 rounded-lg transition-colors"
              >
                <div
                  className={`
                    flex-shrink-0 w-10 h-10 rounded-full flex items-center justify-center
                    ${
                      activity.action === 'CREATE'
                        ? 'bg-green-100'
                        : activity.action === 'UPDATE'
                          ? 'bg-blue-100'
                          : 'bg-red-100'
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
                  <p className="text-sm text-gray-900">
                    {activity.description}
                  </p>
                  <p className="text-xs text-gray-500 mt-1">
                    {formatDistanceToNow(new Date(activity.timestamp), {
                      addSuffix: true,
                    })}
                  </p>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="text-center py-8">
            <p className="text-gray-500">No recent activities</p>
            <p className="text-sm text-gray-400 mt-1">
              Activities will appear here as you use the system
            </p>
          </div>
        )}
      </div>

      {/* Quick Actions */}
      {stats && stats.totalProperties === 0 && (
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-6">
          <h3 className="text-lg font-semibold text-blue-900 mb-2">
            Get Started with Buurman
          </h3>
          <p className="text-blue-800 mb-4">
            You haven&apos;t added any properties yet. Start by adding your
            first property to begin managing your rental portfolio.
          </p>
          <button
            onClick={() => navigate('/properties/new')}
            className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
          >
            <Plus className="h-5 w-5" />
            Add Your First Property
          </button>
        </div>
      )}
    </div>
  );
};
