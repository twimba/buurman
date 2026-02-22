import { useState, useEffect, useRef } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { format } from "date-fns";
import { Mail, Phone, Smartphone, RefreshCw, Eye, MousePointerClick } from "lucide-react";
import { PageHeader, Button, ConfirmDialog } from "@buurman/ui";
import {
  useNotification,
  useResendNotification,
} from "../hooks/useNotifications";
import { LoadingSpinner } from "../components/LoadingSpinner";

const typeLabels: Record<string, string> = {
  WELCOME: "Welcome",
  VERIFICATION_CODE: "Verification Code",
  TEAM_INVITATION: "Team Invitation",
  INVITATION_ACCEPTED: "Invitation Accepted",
  PASSWORD_CHANGED: "Password Changed",
  PAYMENT_REMINDER: "Payment Reminder",
  CONTRACT_EXPIRY: "Contract Expiry",
  PROPERTY_CREATED: "Property Created",
  CONTRACT_CREATED: "Contract Created",
  CONTRACT_STATUS_CHANGED: "Contract Status Changed",
  CONTRACT_REOPENED: "Contract Reopened",
  PAYMENT_PAID: "Payment Paid",
  PAYMENT_RECEIVAL: "Payment Receival",
  EXPENSE_CREATED: "Expense Created",
};

const statusBadgeConfig: Record<string, { label: string; className: string }> =
  {
    PENDING: {
      label: "Pending",
      className:
        "bg-slate-50 text-slate-700 ring-1 ring-slate-200 dark:bg-slate-900/30 dark:text-slate-300 dark:ring-slate-700",
    },
    QUEUED: {
      label: "Queued",
      className:
        "bg-blue-50 text-blue-700 ring-1 ring-blue-200 dark:bg-blue-900/30 dark:text-blue-300 dark:ring-blue-700",
    },
    SENT: {
      label: "Sent",
      className:
        "bg-sky-50 text-sky-700 ring-1 ring-sky-200 dark:bg-sky-900/30 dark:text-sky-300 dark:ring-sky-700",
    },
    DELIVERED: {
      label: "Delivered",
      className:
        "bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200 dark:bg-emerald-900/30 dark:text-emerald-300 dark:ring-emerald-700",
    },
    FAILED: {
      label: "Failed",
      className:
        "bg-red-50 text-red-700 ring-1 ring-red-200 dark:bg-red-900/30 dark:text-red-300 dark:ring-red-700",
    },
    BOUNCED: {
      label: "Bounced",
      className:
        "bg-amber-50 text-amber-700 ring-1 ring-amber-200 dark:bg-amber-900/30 dark:text-amber-300 dark:ring-amber-700",
    },
    REJECTED: {
      label: "Rejected",
      className:
        "bg-red-50 text-red-700 ring-1 ring-red-200 dark:bg-red-900/30 dark:text-red-300 dark:ring-red-700",
    },
  };

const EmailBodyPreview = ({ body }: { body: string }) => {
  const iframeRef = useRef<HTMLIFrameElement>(null);

  useEffect(() => {
    const iframe = iframeRef.current;
    if (!iframe) return;

    const handleLoad = () => {
      const doc = iframe.contentDocument;
      if (doc?.body) {
        iframe.style.height = doc.body.scrollHeight + "px";
      }
    };

    iframe.addEventListener("load", handleLoad);
    return () => iframe.removeEventListener("load", handleLoad);
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
        className="w-full border-0 bg-white min-h-[120px] max-h-[500px]"
        title="Email content"
      />
    </div>
  );
};

const SmsBodyPreview = ({ body }: { body: string }) => (
  <div className="flex justify-center">
    <div className="w-[300px] rounded-2xl bg-[#1a1d2e] dark:bg-[#0c0e14] p-4 shadow-inner">
      <div className="flex items-center justify-center gap-1.5 mb-3 text-[10px] text-[#6b7194]">
        <Smartphone className="h-3 w-3" />
        SMS Message
      </div>
      <div className="flex justify-start">
        <div className="relative max-w-[240px] bg-[#e2e6f0] dark:bg-[#2a2e3f] rounded-2xl rounded-bl-sm px-3.5 py-2.5">
          <p className="text-sm text-[#1a1d2e] dark:text-[#eef0f6] whitespace-pre-wrap break-words leading-relaxed">
            {body}
          </p>
        </div>
      </div>
      <div className="text-right mt-1.5 text-[10px] text-[#6b7194]">
        Delivered
      </div>
    </div>
  </div>
);

export const NotificationDetailPage = () => {
  const { identifier } = useParams<{ identifier: string }>();
  const navigate = useNavigate();
  const { data: notif, isLoading, error } = useNotification(identifier!);
  const resendMutation = useResendNotification();
  const [showResendDialog, setShowResendDialog] = useState(false);

  if (isLoading) {
    return <LoadingSpinner message="Loading notification..." />;
  }

  if (error || !notif) {
    return (
      <div className="text-center py-12">
        <p className="text-red-600 dark:text-red-400">
          Failed to load notification.
        </p>
        <button
          onClick={() => navigate("/notifications")}
          className="mt-4 text-sm text-[#5c7cfa] hover:underline"
        >
          Back to notifications
        </button>
      </div>
    );
  }

  const canResend = ["FAILED", "BOUNCED", "REJECTED"].includes(notif.status);

  const handleResend = () => {
    resendMutation.mutate(identifier!, {
      onSuccess: () => setShowResendDialog(false),
    });
  };

  const statusConfig = statusBadgeConfig[notif.status] ?? {
    label: notif.status,
    className:
      "bg-slate-50 text-slate-700 ring-1 ring-slate-200 dark:bg-slate-900/30 dark:text-slate-300 dark:ring-slate-700",
  };

  return (
    <div>
      <PageHeader
        title={typeLabels[notif.notificationType] ?? notif.notificationType}
        subtitle={`#${identifier}`}
        backTo="/notifications"
        badge={
          <span
            className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${statusConfig.className}`}
          >
            {statusConfig.label}
          </span>
        }
        actions={
          canResend ? (
            <Button
              variant="primary"
              leftIcon={<RefreshCw />}
              onClick={() => setShowResendDialog(true)}
            >
              Resend
            </Button>
          ) : undefined
        }
      />

      {/* Info Section */}
      <div className="bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] p-6 mb-6">
        <h2 className="text-sm font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
          Details
        </h2>
        <dl className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
          <div>
            <dt className="text-[#6b7194] dark:text-[#8b90a8]">Type</dt>
            <dd className="font-medium text-[#1a1d2e] dark:text-[#eef0f6] mt-0.5">
              {typeLabels[notif.notificationType] ?? notif.notificationType}
            </dd>
          </div>
          <div>
            <dt className="text-[#6b7194] dark:text-[#8b90a8]">Channel</dt>
            <dd className="font-medium text-[#1a1d2e] dark:text-[#eef0f6] mt-0.5">
              <span className="inline-flex items-center gap-1">
                {notif.channel === "EMAIL" ? (
                  <Mail className="h-3.5 w-3.5" />
                ) : (
                  <Phone className="h-3.5 w-3.5" />
                )}
                {notif.channel}
              </span>
            </dd>
          </div>
          <div>
            <dt className="text-[#6b7194] dark:text-[#8b90a8]">Status</dt>
            <dd className="mt-0.5">
              <span
                className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${statusConfig.className}`}
              >
                {statusConfig.label}
              </span>
            </dd>
          </div>
          <div>
            <dt className="text-[#6b7194] dark:text-[#8b90a8]">Team</dt>
            <dd className="font-medium text-[#1a1d2e] dark:text-[#eef0f6] mt-0.5">
              {notif.teamName}
              <span className="text-[#6b7194] dark:text-[#8b90a8] text-xs ml-1.5">
                ({notif.teamIdentifier})
              </span>
            </dd>
          </div>
          {notif.recipientEmail && (
            <div>
              <dt className="text-[#6b7194] dark:text-[#8b90a8]">
                Recipient Email
              </dt>
              <dd className="font-medium text-[#1a1d2e] dark:text-[#eef0f6] mt-0.5">
                {notif.recipientEmail}
              </dd>
            </div>
          )}
          {notif.recipientPhone && (
            <div>
              <dt className="text-[#6b7194] dark:text-[#8b90a8]">
                Recipient Phone
              </dt>
              <dd className="font-medium text-[#1a1d2e] dark:text-[#eef0f6] mt-0.5">
                {notif.recipientPhone}
              </dd>
            </div>
          )}
          <div>
            <dt className="text-[#6b7194] dark:text-[#8b90a8]">Created</dt>
            <dd className="font-medium text-[#1a1d2e] dark:text-[#eef0f6] mt-0.5">
              {format(new Date(notif.createdAt), "dd MMM yyyy HH:mm")}
            </dd>
          </div>
          {notif.statusUpdatedAt && (
            <div>
              <dt className="text-[#6b7194] dark:text-[#8b90a8]">
                Status Updated
              </dt>
              <dd className="font-medium text-[#1a1d2e] dark:text-[#eef0f6] mt-0.5">
                {format(new Date(notif.statusUpdatedAt), "dd MMM yyyy HH:mm")}
              </dd>
            </div>
          )}
        </dl>
      </div>

      {/* Subject */}
      {notif.subject && (
        <div className="bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] p-6 mb-6">
          <h2 className="text-sm font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-2">
            Subject
          </h2>
          <p className="text-sm text-[#3d4463] dark:text-[#c4c8db]">
            {notif.subject}
          </p>
        </div>
      )}

      {/* Body */}
      {notif.body && (
        <div className="bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] p-6 mb-6">
          <h2 className="text-sm font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-3">
            Content
          </h2>
          {notif.channel === "EMAIL" ? (
            <EmailBodyPreview body={notif.body} />
          ) : (
            <SmsBodyPreview body={notif.body} />
          )}
        </div>
      )}

      {/* Provider Info */}
      {(notif.providerStatus || notif.providerError) && (
        <div className="bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] p-6 mb-6">
          <h2 className="text-sm font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
            Provider Information
          </h2>
          <dl className="space-y-3 text-sm">
            {notif.providerStatus && (
              <div>
                <dt className="text-[#6b7194] dark:text-[#8b90a8]">
                  Provider Status
                </dt>
                <dd className="font-medium text-[#1a1d2e] dark:text-[#eef0f6] mt-0.5">
                  {notif.providerStatus}
                </dd>
              </div>
            )}
            {notif.providerError && (
              <div>
                <dt className="text-[#6b7194] dark:text-[#8b90a8]">
                  Provider Error
                </dt>
                <dd className="font-medium text-red-600 dark:text-red-400 mt-0.5">
                  {notif.providerError}
                </dd>
              </div>
            )}
          </dl>
        </div>
      )}

      {/* Engagement Tracking */}
      {notif.channel === "EMAIL" && (notif.openCount > 0 || notif.clickCount > 0) && (
        <div className="bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] p-6 mb-6">
          <h2 className="text-sm font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
            Engagement Tracking
          </h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="flex items-center gap-3 p-3 rounded-lg bg-[#f8f9fc] dark:bg-[#1a1d2e]">
              <div className="p-2 rounded-full bg-blue-50 dark:bg-blue-900/30">
                <Eye className="h-4 w-4 text-blue-600 dark:text-blue-400" />
              </div>
              <div>
                <p className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                  {notif.openCount}
                </p>
                <p className="text-xs text-[#6b7194] dark:text-[#8b90a8]">
                  {notif.openCount === 1 ? "Open" : "Opens"}
                  {notif.firstOpenedAt && (
                    <span className="ml-1">
                      &middot; First: {format(new Date(notif.firstOpenedAt), "dd MMM yyyy HH:mm")}
                    </span>
                  )}
                </p>
              </div>
            </div>
            {notif.clickCount > 0 && (
              <div className="flex items-center gap-3 p-3 rounded-lg bg-[#f8f9fc] dark:bg-[#1a1d2e]">
                <div className="p-2 rounded-full bg-emerald-50 dark:bg-emerald-900/30">
                  <MousePointerClick className="h-4 w-4 text-emerald-600 dark:text-emerald-400" />
                </div>
                <div>
                  <p className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                    {notif.clickCount}
                  </p>
                  <p className="text-xs text-[#6b7194] dark:text-[#8b90a8]">
                    {notif.clickCount === 1 ? "Click" : "Clicks"}
                    {notif.firstClickedAt && (
                      <span className="ml-1">
                        &middot; First: {format(new Date(notif.firstClickedAt), "dd MMM yyyy HH:mm")}
                      </span>
                    )}
                  </p>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Resent Info */}
      {(notif.resentFromIdentifier || notif.resendReason) && (
        <div className="bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] p-6 mb-6">
          <h2 className="text-sm font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
            Resend Information
          </h2>
          <dl className="space-y-3 text-sm">
            {notif.resentFromIdentifier && (
              <div>
                <dt className="text-[#6b7194] dark:text-[#8b90a8]">
                  Resent From
                </dt>
                <dd className="mt-0.5">
                  <button
                    onClick={() =>
                      navigate(`/notifications/${notif.resentFromIdentifier}`)
                    }
                    className="text-[#5c7cfa] hover:text-[#4c6ef5] hover:underline font-mono text-xs transition-colors"
                  >
                    {notif.resentFromIdentifier}
                  </button>
                </dd>
              </div>
            )}
            {notif.resendReason && (
              <div>
                <dt className="text-[#6b7194] dark:text-[#8b90a8]">
                  Resend Reason
                </dt>
                <dd className="font-medium text-[#1a1d2e] dark:text-[#eef0f6] mt-0.5">
                  {notif.resendReason}
                </dd>
              </div>
            )}
          </dl>
        </div>
      )}

      {/* Resend confirmation dialog */}
      {showResendDialog && (
        <ConfirmDialog
          title="Resend Notification"
          message="This will create a new notification and attempt delivery again. The original notification will remain in the log."
          confirmLabel="Resend"
          cancelLabel="Cancel"
          variant="default"
          isLoading={resendMutation.isPending}
          onConfirm={handleResend}
          onCancel={() => setShowResendDialog(false)}
        />
      )}
    </div>
  );
};
