import { useEffect, useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Bell,
  Mail,
  Phone,
  RotateCcw,
  Eye,
  CheckCircle,
  AlertTriangle,
  Clock,
  Info,
  ShieldOff,
} from 'lucide-react';
import { useTeam } from '@/context/TeamContext';
import { useTranslation } from 'react-i18next';
import { useFeatureFlags } from '@/context/FeatureFlagContext';
import { FeatureFlags } from '@/constants/featureFlags';
import {
  useNotifications,
  useNotificationStats,
  useResendNotification,
  useRefreshNotificationStatus,
} from '@/hooks/useNotificationHooks';
import {
  NotificationResponse,
  NotificationFilterParams,
  NotificationChannel,
  NotificationStatus,
} from '@/types/notification';
import { getNotification } from '@/generated/api/notifications/notifications';
import { NotificationStatusBadge } from '@/components/notifications/NotificationStatusBadge';
import { NotificationFilters } from '@/components/notifications/NotificationFilters';
import { NotificationDetailModal } from '@/components/notifications/NotificationDetailModal';
import { ConfirmDialog, Pagination, RefreshButton } from '@buurman/ui';

export const AdminNotificationsPage = () => {
  const { t } = useTranslation('admin');
  const { canEditTeamSettings, isLoading: teamLoading } = useTeam();
  const navigate = useNavigate();

  const typeLabels: Record<string, string> = useMemo(
    () => ({
      WELCOME: t('notifications.typeLabels.welcome'),
      VERIFICATION_CODE: t('notifications.typeLabels.verification'),
      TEAM_INVITATION: t('notifications.typeLabels.invitation'),
      INVITATION_ACCEPTED: t('notifications.typeLabels.accepted'),
      PASSWORD_CHANGED: t('notifications.typeLabels.password'),
      PAYMENT_REMINDER: t('notifications.typeLabels.payment'),
      CONTRACT_EXPIRY: t('notifications.typeLabels.contract'),
      PROPERTY_CREATED: t('notifications.typeLabels.property'),
      CONTRACT_CREATED: t('notifications.typeLabels.newContract'),
      CONTRACT_STATUS_CHANGED: t('notifications.typeLabels.statusChange'),
      CONTRACT_REOPENED: t('notifications.typeLabels.reopened'),
      PAYMENT_PAID: t('notifications.typeLabels.paid'),
      PAYMENT_RECEIVAL: t('notifications.typeLabels.receival'),
      EXPENSE_CREATED: t('notifications.typeLabels.expense'),
    }),
    [t]
  );
  const { isEnabled } = useFeatureFlags();

  const emailBlocked = isEnabled(FeatureFlags.BLOCK_EMAIL_NOTIFICATIONS);
  const smsBlocked = isEnabled(FeatureFlags.BLOCK_SMS_NOTIFICATIONS);
  const isDeliveryBlocked = emailBlocked || smsBlocked;

  const [page, setPage] = useState(0);
  const [size, setSize] = useState(25);
  const [filters, setFilters] = useState<NotificationFilterParams>({});
  const [selectedNotification, setSelectedNotification] =
    useState<NotificationResponse | null>(null);
  const [resendTarget, setResendTarget] = useState<string | null>(null);

  const {
    data: notifications,
    isLoading: notifLoading,
    isFetching: notifFetching,
    refetch: refetchNotifications,
  } = useNotifications({
    ...filters,
    page,
    size,
    sort: 'createdAt',
    direction: 'DESC',
  });
  const { data: stats, refetch: refetchStats } = useNotificationStats();
  const resendMutation = useResendNotification();
  const refreshStatusMutation = useRefreshNotificationStatus();

  useEffect(() => {
    if (!teamLoading && !canEditTeamSettings) {
      navigate('/dashboard', { replace: true });
    }
  }, [teamLoading, canEditTeamSettings, navigate]);

  const handleFilterChange = (newFilters: NotificationFilterParams) => {
    setFilters(newFilters);
    setPage(0);
  };

  const handleResendConfirm = () => {
    if (resendTarget) {
      resendMutation.mutate(resendTarget, {
        onSuccess: () => {
          setResendTarget(null);
          setSelectedNotification(null);
        },
      });
    }
  };

  const handleRefreshStatus = (identifier: string) => {
    refreshStatusMutation.mutate(identifier, {
      onSuccess: (updated) => {
        setSelectedNotification(updated);
      },
    });
  };

  const handleViewNotification = async (identifier: string) => {
    try {
      const notif = await getNotification(identifier);
      setSelectedNotification(notif);
    } catch {
      // Notification may have been deleted
    }
  };

  if (teamLoading || !canEditTeamSettings) {
    return null;
  }

  const formatDate = (dateStr: string) => {
    const date = new Date(dateStr);
    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  return (
    <div className="min-h-full bg-background">
      <div className="px-4 py-8">
        {/* Header */}
        <div className="flex items-center justify-between mb-8">
          <div>
            <div className="flex items-center gap-3 mb-1">
              <Bell className="h-8 w-8 text-primary-500 dark:text-primary-300" />
              <h1 className="text-3xl font-bold text-text-primary">
                {t('notifications.title')}
              </h1>
            </div>
            <p className="text-text-secondary ml-11">
              {t('notifications.subtitle')}
            </p>
          </div>
          <RefreshButton
            onClick={() => {
              refetchNotifications();
              refetchStats();
            }}
            isRefreshing={notifFetching}
          />
        </div>

        {/* Demo Banner */}
        {isDeliveryBlocked && (
          <div className="flex items-start gap-3 rounded-lg bg-violet-50 dark:bg-violet-900/20 border border-violet-200 dark:border-violet-800 p-4 mb-6">
            <Info className="h-5 w-5 text-violet-500 mt-0.5 shrink-0" />
            <div>
              <p className="font-medium text-violet-800 dark:text-violet-200">
                {t('notifications.demoBanner.title')}
              </p>
              <p className="text-sm text-violet-600 dark:text-violet-400 mt-0.5">
                {emailBlocked && smsBlocked
                  ? t('notifications.demoBanner.emailAndSms')
                  : emailBlocked
                    ? t('notifications.demoBanner.emailOnly')
                    : t('notifications.demoBanner.smsOnly')}
              </p>
            </div>
          </div>
        )}

        {/* Stats Cards */}
        {stats && (
          <div className="grid grid-cols-2 md:grid-cols-5 gap-4 mb-6">
            <div className="bg-surface-card rounded-lg border border-border-default p-4">
              <div className="flex items-center gap-2 text-text-secondary text-sm mb-1">
                <Mail className="h-4 w-4" />
                {t('notifications.stats.total')}
              </div>
              <div className="text-2xl font-bold text-text-primary">
                {stats.total}
              </div>
            </div>
            <div className="bg-surface-card rounded-lg border border-border-default p-4">
              <div className="flex items-center gap-2 text-text-secondary text-sm mb-1">
                <CheckCircle className="h-4 w-4 text-success-text" />
                {t('notifications.stats.delivered')}
              </div>
              <div className="text-2xl font-bold text-success-text">
                {stats.byStatus[NotificationStatus.DELIVERED] ?? 0}
              </div>
            </div>
            <div className="bg-surface-card rounded-lg border border-border-default p-4">
              <div className="flex items-center gap-2 text-text-secondary text-sm mb-1">
                <Clock className="h-4 w-4 text-info-text" />
                {t('notifications.stats.pending')}
              </div>
              <div className="text-2xl font-bold text-info-text">
                {stats.byStatus[NotificationStatus.PENDING] ?? 0}
              </div>
            </div>
            <div className="bg-surface-card rounded-lg border border-border-default p-4">
              <div className="flex items-center gap-2 text-text-secondary text-sm mb-1">
                <AlertTriangle className="h-4 w-4 text-error-text" />
                {t('notifications.stats.failed')}
              </div>
              <div className="text-2xl font-bold text-error-text">
                {stats.byStatus[NotificationStatus.FAILED] ?? 0}
              </div>
            </div>
            <div className="bg-surface-card rounded-lg border border-border-default p-4">
              <div className="flex items-center gap-2 text-text-secondary text-sm mb-1">
                <Phone className="h-4 w-4" />
                {t('notifications.stats.byChannel')}
              </div>
              <div className="text-sm text-text-primary">
                {Object.entries(stats.byChannel).map(([ch, count]) => (
                  <span key={ch} className="mr-3">
                    {ch}: <span className="font-bold">{count}</span>
                  </span>
                ))}
              </div>
            </div>
            {(stats.byStatus[NotificationStatus.DEMO_BLOCKED] ?? 0) > 0 && (
              <div className="bg-surface-card rounded-lg border border-border-default p-4">
                <div className="flex items-center gap-2 text-text-secondary text-sm mb-1">
                  <ShieldOff className="h-4 w-4 text-violet-500" />
                  {t('notifications.stats.demoBlocked')}
                </div>
                <div className="text-2xl font-bold text-violet-600 dark:text-violet-400">
                  {stats.byStatus[NotificationStatus.DEMO_BLOCKED] ?? 0}
                </div>
              </div>
            )}
          </div>
        )}

        {/* Filters */}
        <div className="mb-4">
          <NotificationFilters
            filters={filters}
            onFilterChange={handleFilterChange}
          />
        </div>

        {/* Table */}
        <div className="bg-surface-card rounded-lg border border-border-default overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border-default bg-surface-page dark:bg-surface-card">
                  <th className="text-left px-4 py-3 font-medium text-text-secondary">
                    {t('notifications.table.type')}
                  </th>
                  <th className="text-left px-4 py-3 font-medium text-text-secondary">
                    {t('notifications.table.channel')}
                  </th>
                  <th className="text-left px-4 py-3 font-medium text-text-secondary">
                    {t('notifications.table.recipient')}
                  </th>
                  <th className="text-left px-4 py-3 font-medium text-text-secondary">
                    {t('notifications.table.subjectBody')}
                  </th>
                  <th className="text-left px-4 py-3 font-medium text-text-secondary">
                    {t('notifications.table.status')}
                  </th>
                  <th className="text-left px-4 py-3 font-medium text-text-secondary">
                    {t('notifications.table.date')}
                  </th>
                  <th className="text-right px-4 py-3 font-medium text-text-secondary">
                    {t('notifications.table.actions')}
                  </th>
                </tr>
              </thead>
              <tbody>
                {notifLoading ? (
                  <tr>
                    <td
                      colSpan={7}
                      className="px-4 py-12 text-center text-text-secondary"
                    >
                      {t('notifications.loading')}
                    </td>
                  </tr>
                ) : !notifications?.content?.length ? (
                  <tr>
                    <td
                      colSpan={7}
                      className="px-4 py-12 text-center text-text-secondary"
                    >
                      {t('notifications.noResults')}
                    </td>
                  </tr>
                ) : (
                  notifications.content.map((notif) => (
                    <tr
                      key={notif.identifier}
                      className="border-b border-border-default hover:bg-surface-page dark:hover:bg-surface-card cursor-pointer transition-colors"
                      onClick={() => setSelectedNotification(notif)}
                    >
                      <td className="px-4 py-3 text-text-primary font-medium">
                        {typeLabels[notif.notificationType] ??
                          notif.notificationType}
                      </td>
                      <td className="px-4 py-3">
                        <span className="inline-flex items-center gap-1 text-text-secondary">
                          {notif.channel === NotificationChannel.EMAIL ? (
                            <Mail className="h-3.5 w-3.5" />
                          ) : (
                            <Phone className="h-3.5 w-3.5" />
                          )}
                          {notif.channel}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-text-secondary max-w-[200px] truncate">
                        {notif.channel === NotificationChannel.SMS
                          ? notif.recipientPhone || notif.recipientEmail || '-'
                          : notif.recipientEmail || '-'}
                      </td>
                      <td className="px-4 py-3 text-text-secondary max-w-[250px] truncate">
                        {notif.channel === NotificationChannel.EMAIL
                          ? notif.subject || '-'
                          : notif.body || notif.subject || '-'}
                      </td>
                      <td className="px-4 py-3">
                        <NotificationStatusBadge status={notif.status} />
                      </td>
                      <td className="px-4 py-3 text-text-secondary whitespace-nowrap">
                        {formatDate(notif.createdAt)}
                      </td>
                      <td className="px-4 py-3 text-right">
                        <div className="inline-flex items-center gap-3">
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              setSelectedNotification(notif);
                            }}
                            className="inline-flex items-center gap-1 text-xs font-medium text-text-secondary hover:text-text-secondary transition-colors"
                            title={t('notifications.viewDetails')}
                          >
                            <Eye className="h-3.5 w-3.5" />
                            {t('notifications.view')}
                          </button>
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              setResendTarget(notif.identifier);
                            }}
                            className="inline-flex items-center gap-1 text-xs font-medium text-primary-500 hover:text-primary-600 transition-colors"
                            title={t('notifications.resendNotification')}
                          >
                            <RotateCcw className="h-3.5 w-3.5" />
                            {t('notifications.resend')}
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          {notifications && (notifications.totalPages ?? 0) > 0 && (
            <div className="px-4 py-3 border-t border-border-default">
              <Pagination
                page={page}
                totalPages={notifications.totalPages ?? 0}
                totalElements={notifications.totalElements ?? 0}
                size={size}
                onPageChange={setPage}
                onSizeChange={(newSize) => {
                  setSize(newSize);
                  setPage(0);
                }}
              />
            </div>
          )}
        </div>
      </div>

      {/* Detail Modal */}
      {selectedNotification && (
        <NotificationDetailModal
          notification={selectedNotification}
          onClose={() => setSelectedNotification(null)}
          onResend={(id) => setResendTarget(id)}
          onRefreshStatus={handleRefreshStatus}
          onViewNotification={handleViewNotification}
          isResending={resendMutation.isPending}
          isRefreshing={refreshStatusMutation.isPending}
        />
      )}

      {/* Resend Confirm Dialog */}
      {resendTarget && (
        <ConfirmDialog
          title={t('notifications.resendConfirm.title')}
          message={t('notifications.resendConfirm.message')}
          confirmLabel={t('notifications.resend')}
          onConfirm={handleResendConfirm}
          onCancel={() => setResendTarget(null)}
          isLoading={resendMutation.isPending}
        />
      )}
    </div>
  );
};
