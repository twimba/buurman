import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Bell, Mail, Phone, RotateCcw, CheckCircle, AlertTriangle, Clock } from 'lucide-react';
import { useTeam } from '@/context/TeamContext';
import {
  useNotifications,
  useNotificationStats,
  useResendNotification,
} from '@/hooks/useNotificationHooks';
import {
  NotificationResponse,
  NotificationFilterParams,
  NotificationChannel,
  NotificationStatus,
} from '@/types/notification';
import { NotificationStatusBadge } from '@/components/notifications/NotificationStatusBadge';
import { NotificationFilters } from '@/components/notifications/NotificationFilters';
import { NotificationDetailModal } from '@/components/notifications/NotificationDetailModal';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { Pagination } from '@/components/ui/Pagination';

const typeLabels: Record<string, string> = {
  WELCOME: 'Welcome',
  VERIFICATION_CODE: 'Verification',
  TEAM_INVITATION: 'Invitation',
  INVITATION_ACCEPTED: 'Accepted',
  PASSWORD_CHANGED: 'Password',
  PAYMENT_REMINDER: 'Payment',
  CONTRACT_EXPIRY: 'Contract',
};

export const AdminNotificationsPage = () => {
  const { canEditTeamSettings, isLoading: teamLoading } = useTeam();
  const navigate = useNavigate();

  const [page, setPage] = useState(0);
  const [size, setSize] = useState(25);
  const [filters, setFilters] = useState<NotificationFilterParams>({});
  const [selectedNotification, setSelectedNotification] =
    useState<NotificationResponse | null>(null);
  const [resendTarget, setResendTarget] = useState<string | null>(null);

  const { data: notifications, isLoading: notifLoading } = useNotifications({
    ...filters,
    page,
    size,
    sort: 'createdAt',
    direction: 'desc',
  });
  const { data: stats } = useNotificationStats();
  const resendMutation = useResendNotification();

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

  if (teamLoading || !canEditTeamSettings) return null;

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
    <div className="min-h-screen bg-background">
      <div className="px-4 py-8">
        {/* Header */}
        <div className="mb-8">
          <div className="flex items-center gap-3 mb-1">
            <Bell className="h-8 w-8 text-primary-500 dark:text-primary-300" />
            <h1 className="text-3xl font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
              Notifications
            </h1>
          </div>
          <p className="text-[#6b7194] dark:text-[#8b90a8] ml-11">
            Track and manage all notifications sent across your team
          </p>
        </div>

        {/* Stats Cards */}
        {stats && (
          <div className="grid grid-cols-2 md:grid-cols-5 gap-4 mb-6">
            <div className="bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] p-4">
              <div className="flex items-center gap-2 text-[#6b7194] dark:text-[#8b90a8] text-sm mb-1">
                <Mail className="h-4 w-4" />
                Total
              </div>
              <div className="text-2xl font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
                {stats.totalCount}
              </div>
            </div>
            <div className="bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] p-4">
              <div className="flex items-center gap-2 text-[#6b7194] dark:text-[#8b90a8] text-sm mb-1">
                <CheckCircle className="h-4 w-4 text-emerald-500" />
                Delivered
              </div>
              <div className="text-2xl font-bold text-emerald-600 dark:text-emerald-400">
                {stats.deliveredCount}
              </div>
            </div>
            <div className="bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] p-4">
              <div className="flex items-center gap-2 text-[#6b7194] dark:text-[#8b90a8] text-sm mb-1">
                <Clock className="h-4 w-4 text-blue-500" />
                Pending
              </div>
              <div className="text-2xl font-bold text-blue-600 dark:text-blue-400">
                {stats.pendingCount}
              </div>
            </div>
            <div className="bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] p-4">
              <div className="flex items-center gap-2 text-[#6b7194] dark:text-[#8b90a8] text-sm mb-1">
                <AlertTriangle className="h-4 w-4 text-red-500" />
                Failed
              </div>
              <div className="text-2xl font-bold text-red-600 dark:text-red-400">
                {stats.failedCount}
              </div>
            </div>
            <div className="bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] p-4">
              <div className="flex items-center gap-2 text-[#6b7194] dark:text-[#8b90a8] text-sm mb-1">
                <Phone className="h-4 w-4" />
                By Channel
              </div>
              <div className="text-sm text-[#1a1d2e] dark:text-[#eef0f6]">
                {Object.entries(stats.byChannel).map(([ch, count]) => (
                  <span key={ch} className="mr-3">
                    {ch}: <span className="font-bold">{count}</span>
                  </span>
                ))}
              </div>
            </div>
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
        <div className="bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-[#e2e6f0] dark:border-[#2a2e3f] bg-[#f8f9fc] dark:bg-[#1a1d2e]">
                  <th className="text-left px-4 py-3 font-medium text-[#6b7194] dark:text-[#8b90a8]">
                    Type
                  </th>
                  <th className="text-left px-4 py-3 font-medium text-[#6b7194] dark:text-[#8b90a8]">
                    Channel
                  </th>
                  <th className="text-left px-4 py-3 font-medium text-[#6b7194] dark:text-[#8b90a8]">
                    Recipient
                  </th>
                  <th className="text-left px-4 py-3 font-medium text-[#6b7194] dark:text-[#8b90a8]">
                    Subject
                  </th>
                  <th className="text-left px-4 py-3 font-medium text-[#6b7194] dark:text-[#8b90a8]">
                    Status
                  </th>
                  <th className="text-left px-4 py-3 font-medium text-[#6b7194] dark:text-[#8b90a8]">
                    Date
                  </th>
                  <th className="text-right px-4 py-3 font-medium text-[#6b7194] dark:text-[#8b90a8]">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody>
                {notifLoading ? (
                  <tr>
                    <td
                      colSpan={7}
                      className="px-4 py-12 text-center text-[#6b7194] dark:text-[#8b90a8]"
                    >
                      Loading notifications...
                    </td>
                  </tr>
                ) : !notifications?.content?.length ? (
                  <tr>
                    <td
                      colSpan={7}
                      className="px-4 py-12 text-center text-[#6b7194] dark:text-[#8b90a8]"
                    >
                      No notifications found
                    </td>
                  </tr>
                ) : (
                  notifications.content.map((notif) => (
                    <tr
                      key={notif.identifier}
                      className="border-b border-[#e2e6f0] dark:border-[#2a2e3f] hover:bg-[#f8f9fc] dark:hover:bg-[#1a1d2e] cursor-pointer transition-colors"
                      onClick={() => setSelectedNotification(notif)}
                    >
                      <td className="px-4 py-3 text-[#1a1d2e] dark:text-[#eef0f6] font-medium">
                        {typeLabels[notif.notificationType] ??
                          notif.notificationType}
                      </td>
                      <td className="px-4 py-3">
                        <span className="inline-flex items-center gap-1 text-[#3d4463] dark:text-[#c4c8db]">
                          {notif.channel === NotificationChannel.EMAIL ? (
                            <Mail className="h-3.5 w-3.5" />
                          ) : (
                            <Phone className="h-3.5 w-3.5" />
                          )}
                          {notif.channel}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-[#3d4463] dark:text-[#c4c8db] max-w-[200px] truncate">
                        {notif.recipientEmail}
                      </td>
                      <td className="px-4 py-3 text-[#3d4463] dark:text-[#c4c8db] max-w-[200px] truncate">
                        {notif.subject || '-'}
                      </td>
                      <td className="px-4 py-3">
                        <NotificationStatusBadge status={notif.status} />
                      </td>
                      <td className="px-4 py-3 text-[#6b7194] dark:text-[#8b90a8] whitespace-nowrap">
                        {formatDate(notif.createdAt)}
                      </td>
                      <td className="px-4 py-3 text-right">
                        {(notif.status === NotificationStatus.FAILED ||
                          notif.status === NotificationStatus.BOUNCED ||
                          notif.status === NotificationStatus.REJECTED) && (
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              setResendTarget(notif.identifier);
                            }}
                            className="inline-flex items-center gap-1 text-xs font-medium text-[#5c7cfa] hover:text-[#4c6ef5] transition-colors"
                            title="Resend notification"
                          >
                            <RotateCcw className="h-3.5 w-3.5" />
                            Resend
                          </button>
                        )}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          {notifications && notifications.totalPages > 0 && (
            <div className="px-4 py-3 border-t border-[#e2e6f0] dark:border-[#2a2e3f]">
              <Pagination
                page={page}
                totalPages={notifications.totalPages}
                totalElements={notifications.totalElements}
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
          isResending={resendMutation.isPending}
        />
      )}

      {/* Resend Confirm Dialog */}
      {resendTarget && (
        <ConfirmDialog
          title="Resend Notification"
          message="This will create a new notification and attempt delivery again. The original notification will remain in the log."
          confirmLabel="Resend"
          onConfirm={handleResendConfirm}
          onCancel={() => setResendTarget(null)}
          isLoading={resendMutation.isPending}
        />
      )}
    </div>
  );
};
