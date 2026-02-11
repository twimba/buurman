import { useNavigate } from "react-router-dom";
import { Users, UserCog, Bell, TrendingUp, ArrowRight } from "lucide-react";
import { RefreshButton } from "@buurman/ui";
import { useDashboardStats } from "../hooks/useDashboard";
import { LoadingSpinner } from "../components/LoadingSpinner";

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
        <p className="text-red-600 dark:text-red-400">
          Failed to load dashboard stats.
        </p>
      </div>
    );
  }

  const deliveryRate =
    stats.totalNotifications > 0
      ? Math.round(
          (stats.deliveredNotifications / stats.totalNotifications) * 100,
        )
      : 0;

  const cards = [
    {
      title: "Total Teams",
      value: stats.totalTeams,
      subtitle: "Registered teams",
      icon: Users,
      color: "blue",
      bgClass: "bg-blue-50 dark:bg-blue-900/20",
      iconClass: "text-blue-600 dark:text-blue-400",
      valueClass: "text-blue-700 dark:text-blue-300",
    },
    {
      title: "Total Users",
      value: stats.totalUsers,
      subtitle: `${stats.disabledUsers} disabled`,
      icon: UserCog,
      color: "emerald",
      bgClass: "bg-emerald-50 dark:bg-emerald-900/20",
      iconClass: "text-emerald-600 dark:text-emerald-400",
      valueClass: "text-emerald-700 dark:text-emerald-300",
    },
    {
      title: "Total Notifications",
      value: stats.totalNotifications,
      subtitle: `${stats.pendingNotifications} pending, ${stats.failedNotifications} failed`,
      icon: Bell,
      color: "purple",
      bgClass: "bg-purple-50 dark:bg-purple-900/20",
      iconClass: "text-purple-600 dark:text-purple-400",
      valueClass: "text-purple-700 dark:text-purple-300",
    },
    {
      title: "Delivery Rate",
      value: `${deliveryRate}%`,
      subtitle: `${stats.deliveredNotifications} delivered`,
      icon: TrendingUp,
      color: "amber",
      bgClass: "bg-amber-50 dark:bg-amber-900/20",
      iconClass: "text-amber-600 dark:text-amber-400",
      valueClass: "text-amber-700 dark:text-amber-300",
    },
  ];

  const quickLinks = [
    { name: "Teams", href: "/teams", description: "Manage registered teams" },
    { name: "Users", href: "/users", description: "Manage platform users" },
    {
      name: "Notifications",
      href: "/notifications",
      description: "View notification logs",
    },
  ];

  return (
    <div>
      {/* Header */}
      <div
        className="mb-6"
        style={{
          display: "flex",
          alignItems: "flex-start",
          justifyContent: "space-between",
        }}
      >
        <div>
          <h1 className="text-2xl font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
            Dashboard
          </h1>
          <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] mt-1">
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
            className="bg-white dark:bg-[#14161f] rounded-xl border border-[#e2e6f0] dark:border-[#2a2e3f] p-5 hover:shadow-md transition-shadow"
          >
            <div
              style={{
                display: "flex",
                alignItems: "flex-start",
                justifyContent: "space-between",
              }}
            >
              <div>
                <p className="text-xs font-medium uppercase tracking-wider text-[#9ca0b8] dark:text-[#5c6180]">
                  {card.title}
                </p>
                <p className={`text-2xl font-bold mt-1 ${card.valueClass}`}>
                  {card.value}
                </p>
                <p className="text-xs text-[#6b7194] dark:text-[#8b90a8] mt-1">
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
        <div className="bg-white dark:bg-[#14161f] rounded-xl border border-[#e2e6f0] dark:border-[#2a2e3f] p-5 mb-8">
          <h2 className="text-sm font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-3">
            Notifications by Channel
          </h2>
          <div style={{ display: "flex", gap: "1rem", flexWrap: "wrap" }}>
            {Object.entries(stats.notificationsByChannel).map(
              ([channel, count]) => (
                <div
                  key={channel}
                  className="bg-[#f8f9fc] dark:bg-[#1a1d28] rounded-lg px-4 py-3 border border-[#e2e6f0] dark:border-[#2a2e3f]"
                >
                  <p className="text-xs font-medium uppercase tracking-wider text-[#9ca0b8] dark:text-[#5c6180]">
                    {channel}
                  </p>
                  <p className="text-lg font-bold text-[#1a1d2e] dark:text-[#eef0f6] mt-0.5">
                    {count}
                  </p>
                </div>
              ),
            )}
          </div>
        </div>
      )}

      {/* Quick Links */}
      <div>
        <h2 className="text-sm font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-3">
          Quick Links
        </h2>
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          {quickLinks.map((link) => (
            <button
              key={link.href}
              onClick={() => navigate(link.href)}
              className="bg-white dark:bg-[#14161f] rounded-xl border border-[#e2e6f0] dark:border-[#2a2e3f] p-4 text-left hover:shadow-md hover:border-[#5c7cfa]/30 dark:hover:border-[#5c7cfa]/30 transition-all group"
            >
              <div
                style={{
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "space-between",
                }}
              >
                <div>
                  <p className="text-sm font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                    {link.name}
                  </p>
                  <p className="text-xs text-[#6b7194] dark:text-[#8b90a8] mt-0.5">
                    {link.description}
                  </p>
                </div>
                <ArrowRight className="h-4 w-4 text-[#9ca0b8] dark:text-[#5c6180] group-hover:text-[#5c7cfa] dark:group-hover:text-[#91a7ff] transition-colors" />
              </div>
            </button>
          ))}
        </div>
      </div>
    </div>
  );
};
