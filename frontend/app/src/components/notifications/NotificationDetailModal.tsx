import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { MessageBodyPreview } from '@/components/communications/MessageBodyPreview';
import {
  X,
  Mail,
  Phone,
  RotateCcw,
  RefreshCw,
  Loader2,
  Eye,
  MousePointerClick,
  Info,
} from 'lucide-react';
import {
  NotificationResponse,
  NotificationChannel,
} from '@/types/notification';
import { NotificationStatusBadge } from './NotificationStatusBadge';

const typeKeys: Record<string, string> = {
  WELCOME: 'notifications.types.welcome',
  VERIFICATION_CODE: 'notifications.types.verificationCode',
  TEAM_INVITATION: 'notifications.types.teamInvitation',
  INVITATION_ACCEPTED: 'notifications.types.invitationAccepted',
  PASSWORD_CHANGED: 'notifications.types.passwordChanged',
  PAYMENT_REMINDER: 'notifications.types.paymentReminder',
  CONTRACT_EXPIRY: 'notifications.types.contractExpiry',
  PROPERTY_CREATED: 'notifications.types.propertyCreated',
  CONTRACT_CREATED: 'notifications.types.contractCreated',
  CONTRACT_STATUS_CHANGED: 'notifications.types.contractStatusChanged',
  CONTRACT_REOPENED: 'notifications.types.contractReopened',
  PAYMENT_PAID: 'notifications.types.paymentPaid',
  PAYMENT_RECEIVAL: 'notifications.types.paymentReceival',
  EXPENSE_CREATED: 'notifications.types.expenseCreated',
};

interface NotificationDetailModalProps {
  notification: NotificationResponse;
  onClose: () => void;
  onResend: (identifier: string) => void;
  onRefreshStatus?: (identifier: string) => void;
  onViewNotification?: (identifier: string) => void;
  isResending: boolean;
  isRefreshing?: boolean;
}

export const NotificationDetailModal = ({
  notification,
  onClose,
  onResend,
  onRefreshStatus,
  onViewNotification,
  isResending,
  isRefreshing,
}: NotificationDetailModalProps) => {
  const { t } = useTranslation('admin');
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onClose]);

  const formatDate = (dateStr: string | null) => {
    if (!dateStr) {
      return '-';
    }
    return new Date(dateStr).toLocaleString();
  };

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto" onClick={onClose}>
      <div className="flex items-center justify-center min-h-[100dvh] px-4 py-8">
        <div className="fixed inset-0 bg-black/40 backdrop-blur-sm" />

        <div
          className="relative bg-surface-card rounded-lg shadow-xl dark:shadow-black/20 w-full max-w-2xl"
          onClick={(e) => e.stopPropagation()}
        >
          <div className="flex items-center justify-between px-6 pt-6 pb-4 border-b border-border-default">
            <h3 className="text-lg font-semibold text-text-primary">
              {t('notifications.details.title')}
            </h3>
            <button
              onClick={onClose}
              className="p-1 text-text-muted hover:text-text-secondary rounded transition-colors"
            >
              <X className="h-5 w-5" />
            </button>
          </div>

          <div className="px-6 py-4 space-y-4">
            <div className="flex items-center gap-3">
              <NotificationStatusBadge status={notification.status} />
              <span className="inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium bg-surface-inset text-text-secondary">
                {notification.channel === NotificationChannel.EMAIL ? (
                  <Mail className="h-3 w-3 mr-1" />
                ) : (
                  <Phone className="h-3 w-3 mr-1" />
                )}
                {notification.channel}
              </span>
            </div>

            {notification.demoBlocked && (
              <div className="flex items-start gap-2 rounded-lg bg-info-bg border border-info-border p-3 text-sm text-info-text">
                <Info className="h-4 w-4 mt-0.5 shrink-0" />
                <span>
                  {t('notifications.details.demoBlocked', {
                    channel: notification.channel.toLowerCase(),
                  })}
                </span>
              </div>
            )}

            <dl className="space-y-3 text-sm">
              <div>
                <dt className="text-text-secondary">
                  {t('notifications.details.type')}
                </dt>
                <dd className="font-medium text-text-primary">
                  {typeKeys[notification.notificationType]
                    ? t(typeKeys[notification.notificationType])
                    : notification.notificationType}
                </dd>
              </div>

              {notification.subject && (
                <div>
                  <dt className="text-text-secondary">
                    {t('notifications.details.subject')}
                  </dt>
                  <dd className="font-medium text-text-primary">
                    {notification.subject}
                  </dd>
                </div>
              )}

              {notification.body && (
                <div>
                  <dt className="text-text-secondary mb-2">
                    {t('notifications.details.content')}
                  </dt>
                  <dd>
                    <MessageBodyPreview
                      channel={notification.channel}
                      body={notification.body}
                    />
                  </dd>
                </div>
              )}

              <div>
                <dt className="text-text-secondary">
                  {t('notifications.details.recipient')}
                </dt>
                <dd className="font-medium text-text-primary">
                  {notification.recipientEmail}
                  {notification.recipientPhone && (
                    <span className="text-text-secondary ml-2">
                      ({notification.recipientPhone})
                    </span>
                  )}
                </dd>
              </div>

              <div>
                <dt className="text-text-secondary">
                  {t('notifications.details.sentAt')}
                </dt>
                <dd className="font-medium text-text-primary">
                  {formatDate(notification.createdAt)}
                </dd>
              </div>

              {notification.statusUpdatedAt && (
                <div>
                  <dt className="text-text-secondary">
                    {t('notifications.details.statusUpdated')}
                  </dt>
                  <dd className="font-medium text-text-primary">
                    {formatDate(notification.statusUpdatedAt)}
                  </dd>
                </div>
              )}

              {notification.providerStatus && (
                <div>
                  <dt className="text-text-secondary">
                    {t('notifications.details.providerStatus')}
                  </dt>
                  <dd className="flex items-center gap-2 font-medium text-text-primary">
                    {notification.providerStatus}
                    {onRefreshStatus && (
                      <button
                        onClick={() => onRefreshStatus(notification.identifier)}
                        disabled={isRefreshing}
                        className="inline-flex items-center gap-1 px-2 py-0.5 text-xs font-medium text-primary-500 hover:text-primary-600 hover:bg-surface-inset rounded transition-colors disabled:opacity-50"
                        title="Refresh status from provider"
                      >
                        {isRefreshing ? (
                          <Loader2 className="h-3 w-3 animate-spin" />
                        ) : (
                          <RefreshCw className="h-3 w-3" />
                        )}
                        {t('notifications.details.refresh')}
                      </button>
                    )}
                  </dd>
                </div>
              )}

              {notification.providerError && (
                <div>
                  <dt className="text-text-secondary">
                    {t('notifications.details.error')}
                  </dt>
                  <dd className="font-medium text-error-text">
                    {notification.providerError}
                  </dd>
                </div>
              )}

              {notification.channel === NotificationChannel.EMAIL &&
                (notification.openCount > 0 || notification.clickCount > 0) && (
                  <div>
                    <dt className="text-text-secondary mb-1">
                      {t('notifications.details.engagement')}
                    </dt>
                    <dd className="flex items-center gap-4">
                      <span className="inline-flex items-center gap-1.5 text-sm font-medium text-text-primary">
                        <Eye className="h-3.5 w-3.5 text-primary-500" />
                        {t('notifications.details.open', {
                          count: notification.openCount,
                        })}
                        {notification.firstOpenedAt && (
                          <span className="text-xs text-text-secondary ml-1">
                            (
                            {t('notifications.details.first', {
                              date: formatDate(notification.firstOpenedAt),
                            })}
                            )
                          </span>
                        )}
                      </span>
                      {notification.clickCount > 0 && (
                        <span className="inline-flex items-center gap-1.5 text-sm font-medium text-text-primary">
                          <MousePointerClick className="h-3.5 w-3.5 text-success-text" />
                          {t('notifications.details.click', {
                            count: notification.clickCount,
                          })}
                          {notification.firstClickedAt && (
                            <span className="text-xs text-text-secondary ml-1">
                              (
                              {t('notifications.details.first', {
                                date: formatDate(notification.firstClickedAt),
                              })}
                              )
                            </span>
                          )}
                        </span>
                      )}
                    </dd>
                  </div>
                )}

              {notification.resentFromIdentifier && (
                <div>
                  <dt className="text-text-secondary">
                    {t('notifications.details.resentFrom')}
                  </dt>
                  <dd className="font-mono text-xs">
                    {onViewNotification ? (
                      <button
                        onClick={() =>
                          onViewNotification(
                            notification.resentFromIdentifier ?? ''
                          )
                        }
                        className="text-primary-500 hover:text-primary-600 hover:underline transition-colors"
                      >
                        {notification.resentFromIdentifier}
                      </button>
                    ) : (
                      <span className="text-text-primary">
                        {notification.resentFromIdentifier}
                      </span>
                    )}
                  </dd>
                </div>
              )}

              {notification.resendReason && (
                <div>
                  <dt className="text-text-secondary">
                    {t('notifications.details.resendReason')}
                  </dt>
                  <dd className="font-medium text-text-primary">
                    {notification.resendReason}
                  </dd>
                </div>
              )}
            </dl>
          </div>

          <div className="px-6 py-4 border-t border-border-default flex justify-end gap-3">
            <button
              onClick={onClose}
              className="px-4 py-2 text-sm font-medium text-text-secondary bg-surface-card border border-border-strong rounded-md hover:bg-surface-inset transition-colors"
            >
              {t('buttons.close', { ns: 'common' })}
            </button>
            {!notification.demoBlocked && (
              <button
                onClick={() => onResend(notification.identifier)}
                disabled={isResending}
                className="inline-flex items-center gap-1.5 px-4 py-2 text-sm font-medium text-white bg-primary-500 hover:bg-primary-600 rounded-md transition-colors disabled:opacity-50"
              >
                <RotateCcw className="h-3.5 w-3.5" />
                {t('notifications.details.resend')}
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
