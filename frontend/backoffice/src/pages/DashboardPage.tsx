import { useNavigate } from 'react-router-dom';
import { Users, UserCog, Bell, TrendingUp, ArrowRight } from 'lucide-react';
import { RefreshButton } from '@buurman/ui';
import { useDashboardStats } from '../hooks/useDashboard';
import { LoadingSpinner } from '../components/LoadingSpinner';

export const DashboardPage = () => {
  const navigate = useNavigate();
  const {
    data: stats,
    isLoading,
    isFetching,
    error,
    refetch,
  } = useDashboardStats();

  if (isLoading) {
    return <LoadingSpinner message="Loading dashboard..." />;
  }

  if (error || !stats) {
    return (
      <div className="text-center py-12">
        <p className="text-error-text">Failed to load dashboard stats.</p>
      </div>
    );
  }

  const deliveryRate =
    stats.totalNotifications > 0
      ? Math.round(
          (stats.deliveredNotifications / stats.totalNotifications) * 100
        )
      : 0;

  const cards = [
    {
      title: 'Total Teams',
      value: stats.totalTeams,
      subtitle: 'Registered teams',
      icon: Users,
      color: 'blue',
      bgClass: 'bg-blue-50',
      iconClass: 'text-blue-600',
      valueClass: 'text-blue-700',
    },
    {
      title: 'Total Users',
      value: stats.totalUsers,
      subtitle: `${stats.disabledUsers} disabled`,
      icon: UserCog,
      color: 'emerald',
      bgClass: 'bg-emerald-50',
      iconClass: 'text-emerald-600',
      valueClass: 'text-emerald-700',
    },
    {
      title: 'Total Notifications',
      value: stats.totalNotifications,
      subtitle: `${stats.pendingNotifications} pending, ${stats.failedNotifications} failed`,
      icon: Bell,
      color: 'purple',
      bgClass: 'bg-purple-50',
      iconClass: 'text-purple-600',
      valueClass: 'text-purple-700',
    },
    {
      title: 'Delivery Rate',
      value: `${deliveryRate}%`,
      subtitle: `${stats.deliveredNotifications} delivered`,
      icon: TrendingUp,
      color: 'amber',
      bgClass: 'bg-amber-50',
      iconClass: 'text-amber-600',
      valueClass: 'text-amber-700',
    },
  ];

  const quickLinks = [
    { name: 'Teams', href: '/teams', description: 'Manage registered teams' },
    { name: 'Users', href: '/users', description: 'Manage platform users' },
    {
      name: 'Notifications',
      href: '/notifications',
      description: 'View notification logs',
    },
  ];

  return (
    <div>
      {/* Header */}
      <div
        className="mb-6"
        style={{
          display: 'flex',
          alignItems: 'flex-start',
          justifyContent: 'space-between',
        }}
      >
        <div>
          <h1 className="text-2xl font-bold text-text-primary">Dashboard</h1>
          <p className="text-sm text-text-secondary mt-1">
            Platform overview and key metrics.
          </p>
        </div>
        <RefreshButton onClick={() => refetch()} isRefreshing={isFetching} />
      </div>

      {/* Stat Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        {cards.map((card) => (
          <div
            key={card.title}
            className="bg-surface-card rounded-lg border border-border-default p-5 hover:shadow-md transition-shadow"
          >
            <div
              style={{
                display: 'flex',
                alignItems: 'flex-start',
                justifyContent: 'space-between',
              }}
            >
              <div>
                <p className="text-xs font-medium uppercase tracking-wider text-text-muted">
                  {card.title}
                </p>
                <p className={`text-2xl font-bold mt-1 ${card.valueClass}`}>
                  {card.value}
                </p>
                <p className="text-xs text-text-secondary mt-1">
                  {card.subtitle}
                </p>
              </div>
              <div
                className={`flex-shrink-0 w-10 h-10 rounded-lg ${card.bgClass} flex items-center justify-center`}
              >
                <card.icon className={`h-5 w-5 ${card.iconClass}`} />
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Channel Breakdown */}
      {Object.keys(stats.notificationsByChannel).length > 0 && (
        <div className="bg-surface-card rounded-lg border border-border-default p-5 mb-8">
          <h2 className="text-sm font-semibold text-text-primary mb-3">
            Notifications by Channel
          </h2>
          <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap' }}>
            {Object.entries(stats.notificationsByChannel).map(
              ([channel, count]) => (
                <div
                  key={channel}
                  className="bg-surface-page rounded-lg px-4 py-3 border border-border-default"
                >
                  <p className="text-xs font-medium uppercase tracking-wider text-text-muted">
                    {channel}
                  </p>
                  <p className="text-lg font-bold text-text-primary mt-0.5">
                    {count}
                  </p>
                </div>
              )
            )}
          </div>
        </div>
      )}

      {/* Quick Links */}
      <div>
        <h2 className="text-sm font-semibold text-text-primary mb-3">
          Quick Links
        </h2>
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          {quickLinks.map((link) => (
            <button
              key={link.href}
              onClick={() => navigate(link.href)}
              className="bg-surface-card rounded-lg border border-border-default p-4 text-left hover:shadow-md hover:border-primary-500/30 transition-all group"
            >
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                }}
              >
                <div>
                  <p className="text-sm font-semibold text-text-primary">
                    {link.name}
                  </p>
                  <p className="text-xs text-text-secondary mt-0.5">
                    {link.description}
                  </p>
                </div>
                <ArrowRight className="h-4 w-4 text-text-muted group-hover:text-primary-500 transition-colors" />
              </div>
            </button>
          ))}
        </div>
      </div>
    </div>
  );
};
