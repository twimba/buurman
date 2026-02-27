import { useEffect, useRef } from 'react';
import {
  X,
  Mail,
  Phone,
  RotateCcw,
  RefreshCw,
  Smartphone,
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

const typeLabels: Record<string, string> = {
  WELCOME: 'Welcome',
  VERIFICATION_CODE: 'Verification Code',
  TEAM_INVITATION: 'Team Invitation',
  INVITATION_ACCEPTED: 'Invitation Accepted',
  PASSWORD_CHANGED: 'Password Changed',
  PAYMENT_REMINDER: 'Payment Reminder',
  CONTRACT_EXPIRY: 'Contract Expiry',
  PROPERTY_CREATED: 'Property Created',
  CONTRACT_CREATED: 'Contract Created',
  CONTRACT_STATUS_CHANGED: 'Contract Status Changed',
  CONTRACT_REOPENED: 'Contract Reopened',
  PAYMENT_PAID: 'Payment Paid',
  PAYMENT_RECEIVAL: 'Payment Receival',
  EXPENSE_CREATED: 'Expense Created',
};

const EmailBodyPreview = ({ body }: { body: string }) => {
  const iframeRef = useRef<HTMLIFrameElement>(null);

  useEffect(() => {
    const iframe = iframeRef.current;
    if (!iframe) {
      return;
    }

    const handleLoad = () => {
      const doc = iframe.contentDocument;
      if (doc?.body) {
        iframe.style.height = doc.body.scrollHeight + 'px';
      }
    };

    iframe.addEventListener('load', handleLoad);
    return () => iframe.removeEventListener('load', handleLoad);
  }, [body]);

  return (
    <div className="rounded-md border border-[#e2e6f0] dark:border-[#2a2e3f] overflow-hidden">
      <div className="flex items-center gap-1.5 px-3 py-1.5 bg-[#f8f9fc] dark:bg-[#1a1d2e] border-b border-[#e2e6f0] dark:border-[#2a2e3f] text-[10px] text-[#9ca0b8] dark:text-[#5c6180]">
        <Mail className="h-3 w-3" />
        Email Preview
      </div>
      <iframe
        ref={iframeRef}
        srcDoc={body}
        sandbox="allow-same-origin"
        className="w-full border-0 bg-white min-h-[120px] max-h-[400px]"
        title="Email content"
      />
    </div>
  );
};

const SmsBodyPreview = ({ body }: { body: string }) => (
  <div className="flex justify-center">
    <div className="w-[300px] rounded-2xl bg-[#1a1d2e] dark:bg-[#0c0e14] p-4 shadow-inner">
      {/* Phone header */}
      <div className="flex items-center justify-center gap-1.5 mb-3 text-[10px] text-[#6b7194]">
        <Smartphone className="h-3 w-3" />
        SMS Message
      </div>
      {/* Message bubble */}
      <div className="flex justify-start">
        <div className="relative max-w-[240px] bg-[#e2e6f0] dark:bg-[#2a2e3f] rounded-2xl rounded-bl-sm px-3.5 py-2.5">
          <p className="text-sm text-[#1a1d2e] dark:text-[#eef0f6] whitespace-pre-wrap break-words leading-relaxed">
            {body}
          </p>
        </div>
      </div>
      {/* Timestamp */}
      <div className="text-right mt-1.5 text-[10px] text-[#6b7194]">
        Delivered
      </div>
    </div>
  </div>
);

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
      <div className="flex items-center justify-center min-h-screen px-4 py-8">
        <div className="fixed inset-0 bg-black/40 backdrop-blur-sm" />

        <div
          className="relative bg-white dark:bg-[#14161f] rounded-lg shadow-xl dark:shadow-black/20 w-full max-w-2xl"
          onClick={(e) => e.stopPropagation()}
        >
          <div className="flex items-center justify-between px-6 pt-6 pb-4 border-b border-[#e2e6f0] dark:border-[#2a2e3f]">
            <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
              Notification Details
            </h3>
            <button
              onClick={onClose}
              className="p-1 text-[#9ca0b8] dark:text-[#5c6180] hover:text-[#3d4463] dark:hover:text-[#c4c8db] rounded transition-colors"
            >
              <X className="h-5 w-5" />
            </button>
          </div>

          <div className="px-6 py-4 space-y-4">
            <div className="flex items-center gap-3">
              <NotificationStatusBadge status={notification.status} />
              <span className="inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium bg-[#f1f3f9] dark:bg-[#1e2130] text-[#3d4463] dark:text-[#c4c8db]">
                {notification.channel === NotificationChannel.EMAIL ? (
                  <Mail className="h-3 w-3 mr-1" />
                ) : (
                  <Phone className="h-3 w-3 mr-1" />
                )}
                {notification.channel}
              </span>
            </div>

            {notification.demoBlocked && (
              <div className="flex items-start gap-2 rounded-lg bg-violet-50 dark:bg-violet-900/20 border border-violet-200 dark:border-violet-800 p-3 text-sm text-violet-700 dark:text-violet-300">
                <Info className="h-4 w-4 mt-0.5 shrink-0" />
                <span>
                  This notification was not delivered because this is a demo
                  account. In a real account, it would be sent via{' '}
                  {notification.channel.toLowerCase()}.
                </span>
              </div>
            )}

            <dl className="space-y-3 text-sm">
              <div>
                <dt className="text-[#6b7194] dark:text-[#8b90a8]">Type</dt>
                <dd className="font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                  {typeLabels[notification.notificationType] ??
                    notification.notificationType}
                </dd>
              </div>

              {notification.subject && (
                <div>
                  <dt className="text-[#6b7194] dark:text-[#8b90a8]">
                    Subject
                  </dt>
                  <dd className="font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                    {notification.subject}
                  </dd>
                </div>
              )}

              {notification.body && (
                <div>
                  <dt className="text-[#6b7194] dark:text-[#8b90a8] mb-2">
                    Content
                  </dt>
                  <dd>
                    {notification.channel === NotificationChannel.EMAIL ? (
                      <EmailBodyPreview body={notification.body} />
                    ) : (
                      <SmsBodyPreview body={notification.body} />
                    )}
                  </dd>
                </div>
              )}

              <div>
                <dt className="text-[#6b7194] dark:text-[#8b90a8]">
                  Recipient
                </dt>
                <dd className="font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                  {notification.recipientEmail}
                  {notification.recipientPhone && (
                    <span className="text-[#6b7194] dark:text-[#8b90a8] ml-2">
                      ({notification.recipientPhone})
                    </span>
                  )}
                </dd>
              </div>

              <div>
                <dt className="text-[#6b7194] dark:text-[#8b90a8]">Sent at</dt>
                <dd className="font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                  {formatDate(notification.createdAt)}
                </dd>
              </div>

              {notification.statusUpdatedAt && (
                <div>
                  <dt className="text-[#6b7194] dark:text-[#8b90a8]">
                    Status updated
                  </dt>
                  <dd className="font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                    {formatDate(notification.statusUpdatedAt)}
                  </dd>
                </div>
              )}

              {notification.providerStatus && (
                <div>
                  <dt className="text-[#6b7194] dark:text-[#8b90a8]">
                    Provider status
                  </dt>
                  <dd className="flex items-center gap-2 font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                    {notification.providerStatus}
                    {onRefreshStatus && (
                      <button
                        onClick={() => onRefreshStatus(notification.identifier)}
                        disabled={isRefreshing}
                        className="inline-flex items-center gap-1 px-2 py-0.5 text-xs font-medium text-[#5c7cfa] hover:text-[#4c6ef5] hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] rounded transition-colors disabled:opacity-50"
                        title="Refresh status from provider"
                      >
                        {isRefreshing ? (
                          <Loader2 className="h-3 w-3 animate-spin" />
                        ) : (
                          <RefreshCw className="h-3 w-3" />
                        )}
                        Refresh
                      </button>
                    )}
                  </dd>
                </div>
              )}

              {notification.providerError && (
                <div>
                  <dt className="text-[#6b7194] dark:text-[#8b90a8]">Error</dt>
                  <dd className="font-medium text-red-600 dark:text-red-400">
                    {notification.providerError}
                  </dd>
                </div>
              )}

              {notification.channel === NotificationChannel.EMAIL &&
                (notification.openCount > 0 || notification.clickCount > 0) && (
                  <div>
                    <dt className="text-[#6b7194] dark:text-[#8b90a8] mb-1">
                      Engagement
                    </dt>
                    <dd className="flex items-center gap-4">
                      <span className="inline-flex items-center gap-1.5 text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                        <Eye className="h-3.5 w-3.5 text-[#5c7cfa]" />
                        {notification.openCount}{' '}
                        {notification.openCount === 1 ? 'open' : 'opens'}
                        {notification.firstOpenedAt && (
                          <span className="text-xs text-[#6b7194] dark:text-[#8b90a8] ml-1">
                            (first: {formatDate(notification.firstOpenedAt)})
                          </span>
                        )}
                      </span>
                      {notification.clickCount > 0 && (
                        <span className="inline-flex items-center gap-1.5 text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                          <MousePointerClick className="h-3.5 w-3.5 text-emerald-500" />
                          {notification.clickCount}{' '}
                          {notification.clickCount === 1 ? 'click' : 'clicks'}
                          {notification.firstClickedAt && (
                            <span className="text-xs text-[#6b7194] dark:text-[#8b90a8] ml-1">
                              (first: {formatDate(notification.firstClickedAt)})
                            </span>
                          )}
                        </span>
                      )}
                    </dd>
                  </div>
                )}

              {notification.resentFromIdentifier && (
                <div>
                  <dt className="text-[#6b7194] dark:text-[#8b90a8]">
                    Resent from
                  </dt>
                  <dd className="font-mono text-xs">
                    {onViewNotification ? (
                      <button
                        onClick={() =>
                          onViewNotification(notification.resentFromIdentifier!)
                        }
                        className="text-[#5c7cfa] hover:text-[#4c6ef5] hover:underline transition-colors"
                      >
                        {notification.resentFromIdentifier}
                      </button>
                    ) : (
                      <span className="text-[#1a1d2e] dark:text-[#eef0f6]">
                        {notification.resentFromIdentifier}
                      </span>
                    )}
                  </dd>
                </div>
              )}

              {notification.resendReason && (
                <div>
                  <dt className="text-[#6b7194] dark:text-[#8b90a8]">
                    Resend reason
                  </dt>
                  <dd className="font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                    {notification.resendReason}
                  </dd>
                </div>
              )}
            </dl>
          </div>

          <div className="px-6 py-4 border-t border-[#e2e6f0] dark:border-[#2a2e3f] flex justify-end gap-3">
            <button
              onClick={onClose}
              className="px-4 py-2 text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] bg-white dark:bg-[#1e2130] border border-[#c9cfd9] dark:border-[#3a3f54] rounded-md hover:bg-[#f1f3f9] dark:hover:bg-[#2a2e3f] transition-colors"
            >
              Close
            </button>
            {!notification.demoBlocked && (
              <button
                onClick={() => onResend(notification.identifier)}
                disabled={isResending}
                className="inline-flex items-center gap-1.5 px-4 py-2 text-sm font-medium text-white bg-[#5c7cfa] hover:bg-[#4c6ef5] rounded-md transition-colors disabled:opacity-50"
              >
                <RotateCcw className="h-3.5 w-3.5" />
                Resend
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
