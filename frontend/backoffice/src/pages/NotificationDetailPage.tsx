import { useState, useEffect, useRef } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { format } from "date-fns";
import {
  Mail,
  Phone,
  Smartphone,
  RefreshCw,
  Eye,
  MousePointerClick,
} from "lucide-react";
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
      className: "bg-slate-50 text-slate-700 ring-1 ring-slate-200",
    },
    QUEUED: {
      label: "Queued",
      className: "bg-blue-50 text-blue-700 ring-1 ring-blue-200",
    },
    SENT: {
      label: "Sent",
      className: "bg-sky-50 text-sky-700 ring-1 ring-sky-200",
    },
    DELIVERED: {
      label: "Delivered",
      className: "bg-success-bg text-success-text ring-1 ring-success-border",
    },
    FAILED: {
      label: "Failed",
      className: "bg-error-bg text-error-text ring-1 ring-error-border",
    },
    BOUNCED: {
      label: "Bounced",
      className: "bg-warning-bg text-warning-text ring-1 ring-warning-border",
    },
    REJECTED: {
      label: "Rejected",
      className: "bg-error-bg text-error-text ring-1 ring-error-border",
    },
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
        iframe.style.height = doc.body.scrollHeight + "px";
      }
    };

    iframe.addEventListener("load", handleLoad);
    return () => iframe.removeEventListener("load", handleLoad);
  }, [body]);

  return (
    <div className="rounded-md border border-border-default overflow-hidden">
      <div className="flex items-center gap-1.5 px-3 py-1.5 bg-surface-page border-b border-border-default text-[10px] text-text-muted">
        <Mail className="h-3 w-3" />
        Email Preview
      </div>
      <iframe
        ref={iframeRef}
        srcDoc={body}
        sandbox="allow-same-origin"
        className="w-full border-0 bg-surface-card min-h-[120px] max-h-[500px]"
        title="Email content"
      />
    </div>
  );
};

const SmsBodyPreview = ({ body }: { body: string }) => (
  <div className="flex justify-center">
    <div className="w-[300px] rounded-2xl bg-neutral-900 p-4 shadow-inner">
      <div className="flex items-center justify-center gap-1.5 mb-3 text-[10px] text-text-secondary">
        <Smartphone className="h-3 w-3" />
        SMS Message
      </div>
      <div className="flex justify-start">
        <div className="relative max-w-[240px] bg-surface-inset rounded-2xl rounded-bl-sm px-3.5 py-2.5">
          <p className="text-sm text-text-primary whitespace-pre-wrap break-words leading-relaxed">
            {body}
          </p>
        </div>
      </div>
      <div className="text-right mt-1.5 text-[10px] text-text-secondary">
        Delivered
      </div>
    </div>
  </div>
);

export const NotificationDetailPage = () => {
  const { identifier = "" } = useParams<{ identifier: string }>();
  const navigate = useNavigate();
  const { data: notif, isLoading, error } = useNotification(identifier);
  const resendMutation = useResendNotification();
  const [showResendDialog, setShowResendDialog] = useState(false);

  if (isLoading) {
    return <LoadingSpinner message="Loading notification..." />;
  }

  if (error || !notif) {
    return (
      <div className="text-center py-12">
        <p className="text-error-text">Failed to load notification.</p>
        <button
          onClick={() => navigate("/notifications")}
          className="mt-4 text-sm text-primary-500 hover:underline"
        >
          Back to notifications
        </button>
      </div>
    );
  }

  const canResend = ["FAILED", "BOUNCED", "REJECTED"].includes(notif.status);

  const handleResend = () => {
    resendMutation.mutate(identifier, {
      onSuccess: () => setShowResendDialog(false),
    });
  };

  const statusConfig = statusBadgeConfig[notif.status] ?? {
    label: notif.status,
    className: "bg-slate-50 text-slate-700 ring-1 ring-slate-200",
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
      <div className="bg-surface-card rounded-lg border border-border-default p-6 mb-6">
        <h2 className="text-sm font-semibold text-text-primary mb-4">
          Details
        </h2>
        <dl className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
          <div>
            <dt className="text-text-secondary">Type</dt>
            <dd className="font-medium text-text-primary mt-0.5">
              {typeLabels[notif.notificationType] ?? notif.notificationType}
            </dd>
          </div>
          <div>
            <dt className="text-text-secondary">Channel</dt>
            <dd className="font-medium text-text-primary mt-0.5">
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
            <dt className="text-text-secondary">Status</dt>
            <dd className="mt-0.5">
              <span
                className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${statusConfig.className}`}
              >
                {statusConfig.label}
              </span>
            </dd>
          </div>
          <div>
            <dt className="text-text-secondary">Team</dt>
            <dd className="font-medium text-text-primary mt-0.5">
              {notif.teamName}
              <span className="text-text-secondary text-xs ml-1.5">
                ({notif.teamIdentifier})
              </span>
            </dd>
          </div>
          {notif.recipientEmail && (
            <div>
              <dt className="text-text-secondary">Recipient Email</dt>
              <dd className="font-medium text-text-primary mt-0.5">
                {notif.recipientEmail}
              </dd>
            </div>
          )}
          {notif.recipientPhone && (
            <div>
              <dt className="text-text-secondary">Recipient Phone</dt>
              <dd className="font-medium text-text-primary mt-0.5">
                {notif.recipientPhone}
              </dd>
            </div>
          )}
          <div>
            <dt className="text-text-secondary">Created</dt>
            <dd className="font-medium text-text-primary mt-0.5">
              {format(new Date(notif.createdAt), "dd MMM yyyy HH:mm")}
            </dd>
          </div>
          {notif.statusUpdatedAt && (
            <div>
              <dt className="text-text-secondary">Status Updated</dt>
              <dd className="font-medium text-text-primary mt-0.5">
                {format(new Date(notif.statusUpdatedAt), "dd MMM yyyy HH:mm")}
              </dd>
            </div>
          )}
        </dl>
      </div>

      {/* Subject */}
      {notif.subject && (
        <div className="bg-surface-card rounded-lg border border-border-default p-6 mb-6">
          <h2 className="text-sm font-semibold text-text-primary mb-2">
            Subject
          </h2>
          <p className="text-sm text-text-secondary">{notif.subject}</p>
        </div>
      )}

      {/* Body */}
      {notif.body && (
        <div className="bg-surface-card rounded-lg border border-border-default p-6 mb-6">
          <h2 className="text-sm font-semibold text-text-primary mb-3">
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
        <div className="bg-surface-card rounded-lg border border-border-default p-6 mb-6">
          <h2 className="text-sm font-semibold text-text-primary mb-4">
            Provider Information
          </h2>
          <dl className="space-y-3 text-sm">
            {notif.providerStatus && (
              <div>
                <dt className="text-text-secondary">Provider Status</dt>
                <dd className="font-medium text-text-primary mt-0.5">
                  {notif.providerStatus}
                </dd>
              </div>
            )}
            {notif.providerError && (
              <div>
                <dt className="text-text-secondary">Provider Error</dt>
                <dd className="font-medium text-error-text mt-0.5">
                  {notif.providerError}
                </dd>
              </div>
            )}
          </dl>
        </div>
      )}

      {/* Engagement Tracking */}
      {notif.channel === "EMAIL" &&
        (notif.openCount > 0 || notif.clickCount > 0) && (
          <div className="bg-surface-card rounded-lg border border-border-default p-6 mb-6">
            <h2 className="text-sm font-semibold text-text-primary mb-4">
              Engagement Tracking
            </h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="flex items-center gap-3 p-3 rounded-lg bg-surface-page">
                <div className="p-2 rounded-full bg-blue-50">
                  <Eye className="h-4 w-4 text-blue-600" />
                </div>
                <div>
                  <p className="text-lg font-semibold text-text-primary">
                    {notif.openCount}
                  </p>
                  <p className="text-xs text-text-secondary">
                    {notif.openCount === 1 ? "Open" : "Opens"}
                    {notif.firstOpenedAt && (
                      <span className="ml-1">
                        &middot; First:{" "}
                        {format(
                          new Date(notif.firstOpenedAt),
                          "dd MMM yyyy HH:mm",
                        )}
                      </span>
                    )}
                  </p>
                </div>
              </div>
              {notif.clickCount > 0 && (
                <div className="flex items-center gap-3 p-3 rounded-lg bg-surface-page">
                  <div className="p-2 rounded-full bg-emerald-50">
                    <MousePointerClick className="h-4 w-4 text-emerald-600" />
                  </div>
                  <div>
                    <p className="text-lg font-semibold text-text-primary">
                      {notif.clickCount}
                    </p>
                    <p className="text-xs text-text-secondary">
                      {notif.clickCount === 1 ? "Click" : "Clicks"}
                      {notif.firstClickedAt && (
                        <span className="ml-1">
                          &middot; First:{" "}
                          {format(
                            new Date(notif.firstClickedAt),
                            "dd MMM yyyy HH:mm",
                          )}
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
        <div className="bg-surface-card rounded-lg border border-border-default p-6 mb-6">
          <h2 className="text-sm font-semibold text-text-primary mb-4">
            Resend Information
          </h2>
          <dl className="space-y-3 text-sm">
            {notif.resentFromIdentifier && (
              <div>
                <dt className="text-text-secondary">Resent From</dt>
                <dd className="mt-0.5">
                  <button
                    onClick={() =>
                      navigate(`/notifications/${notif.resentFromIdentifier}`)
                    }
                    className="text-primary-500 hover:text-primary-600 hover:underline font-mono text-xs transition-colors"
                  >
                    {notif.resentFromIdentifier}
                  </button>
                </dd>
              </div>
            )}
            {notif.resendReason && (
              <div>
                <dt className="text-text-secondary">Resend Reason</dt>
                <dd className="font-medium text-text-primary mt-0.5">
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
