import { useState, useEffect, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { formatDateTime } from "../utils/dateFormatting";
import {
  Mail,
  User,
  Phone,
  CheckCircle,
  XCircle,
  Shield,
  Users,
  Calendar,
  Eye,
  Pencil,
} from "lucide-react";
import { PageHeader, Button, ConfirmDialog, ModalWrapper } from "@buurman/ui";
import {
  useUser,
  useDisableUser,
  useEnableUser,
  useResetPassword,
} from "../hooks/useUsers";
import {
  useCreateImpersonation,
  useUserTeams,
} from "../hooks/useImpersonation";
import { LoadingSpinner } from "../components/LoadingSpinner";
import { UserFeatureFlags } from "../components/UserFeatureFlags";
import { RichTextEditor } from "../components/RichTextEditor";
import { PasswordConfirmationDialog } from "../components/PasswordConfirmationDialog";
import { trackEvent } from "../utils/analytics";
import { AnalyticsEvent } from "../constants/analyticsEvents";

const DURATION_PRESETS = [
  { label: "15 min", value: 15 },
  { label: "30 min", value: 30 },
  { label: "45 min", value: 45 },
  { label: "1 hour", value: 60 },
] as const;

interface ImpersonateFormData {
  teamIdentifier: string;
  reason: string;
  durationMinutes: number;
}

const defaultFormData: ImpersonateFormData = {
  teamIdentifier: "",
  reason: "",
  durationMinutes: 15,
};

/** Convert minutes to an ISO 8601 duration string */
const toIsoDuration = (minutes: number): string => {
  if (minutes >= 60 && minutes % 60 === 0) {
    return `PT${minutes / 60}H`;
  }
  return `PT${minutes}M`;
};

export const UserDetailPage = () => {
  const { identifier = "" } = useParams<{ identifier: string }>();
  const navigate = useNavigate();
  const { data: user, isLoading, error } = useUser(identifier);
  const disableUser = useDisableUser();
  const enableUser = useEnableUser();
  const resetPassword = useResetPassword();
  const createImpersonation = useCreateImpersonation();
  const { data: userTeams } = useUserTeams(identifier);

  const [showDisableDialog, setShowDisableDialog] = useState(false);
  const [showResetDialog, setShowResetDialog] = useState(false);
  const [showImpersonateDialog, setShowImpersonateDialog] = useState(false);
  const [impersonateForm, setImpersonateForm] =
    useState<ImpersonateFormData>(defaultFormData);
  const [pendingMode, setPendingMode] = useState<"READ_ONLY" | "FULL" | null>(
    null,
  );
  const [showPasswordDialog, setShowPasswordDialog] = useState(false);
  const [passwordError, setPasswordError] = useState<string | null>(null);

  const userIdentifier = user?.identifier;
  useEffect(() => {
    if (userIdentifier) {
      trackEvent(AnalyticsEvent.BO_USER_VIEWED);
    }
  }, [userIdentifier]);

  const openImpersonateDialog = useCallback(() => {
    const preselectedTeam =
      userTeams?.length === 1 ? userTeams[0].teamIdentifier : "";
    setImpersonateForm({ ...defaultFormData, teamIdentifier: preselectedTeam });
    setPendingMode(null);
    createImpersonation.reset();
    setShowImpersonateDialog(true);
  }, [userTeams, createImpersonation]);

  const handleImpersonate = useCallback((mode: "READ_ONLY" | "FULL") => {
    setPendingMode(mode);
    setPasswordError(null);
    setShowPasswordDialog(true);
  }, []);

  const handlePasswordConfirm = useCallback(
    (password: string) => {
      if (!pendingMode) {
        return;
      }

      // Pre-open window synchronously (user gesture) to avoid popup blockers.
      const newWindow = window.open("about:blank", "_blank");

      createImpersonation.mutate(
        {
          userIdentifier: identifier,
          teamIdentifier: impersonateForm.teamIdentifier,
          reason: impersonateForm.reason,
          password,
          mode: pendingMode,
          timeout: toIsoDuration(impersonateForm.durationMinutes),
        },
        {
          onSuccess: (data) => {
            setShowPasswordDialog(false);
            setShowImpersonateDialog(false);
            if (newWindow && !newWindow.closed) {
              newWindow.location.href = data.redirectUrl;
            } else {
              window.open(data.redirectUrl, "_blank");
            }
          },
          onError: (err) => {
            newWindow?.close();
            const axiosError = err as { response?: { status?: number } };
            if (axiosError.response?.status === 403) {
              setPasswordError("Invalid password. Please try again.");
            } else {
              setPasswordError("Failed to create impersonation session.");
            }
          },
        },
      );
    },
    [pendingMode, impersonateForm, identifier, createImpersonation],
  );

  if (isLoading) {
    return <LoadingSpinner message="Loading user..." />;
  }

  if (error || !user) {
    return (
      <div className="text-center py-12">
        <p className="text-error-text">Failed to load user.</p>
        <button
          onClick={() => navigate("/users")}
          className="mt-4 text-sm text-primary-500 hover:underline"
        >
          Back to users
        </button>
      </div>
    );
  }

  const handleToggleDisable = () => {
    const mutation = user.disabled ? enableUser : disableUser;
    const event = user.disabled
      ? AnalyticsEvent.BO_USER_ENABLED
      : AnalyticsEvent.BO_USER_DISABLED;
    mutation.mutate(identifier, {
      onSuccess: () => {
        trackEvent(event);
        setShowDisableDialog(false);
      },
    });
  };

  const handleResetPassword = () => {
    resetPassword.mutate(identifier, {
      onSuccess: () => setShowResetDialog(false),
    });
  };

  const isFormValid =
    impersonateForm.teamIdentifier.length > 0 &&
    impersonateForm.reason.trim().length > 0 &&
    impersonateForm.reason.replace(/<[^>]*>/g, "").trim().length > 0;

  const infoItems = [
    {
      label: "Email",
      value: user.email,
      icon: Mail,
    },
    {
      label: "Name",
      value: `${user.firstName} ${user.lastName}`,
      icon: User,
    },
    {
      label: "Phone",
      value: user.phone || "-",
      icon: Phone,
    },
    {
      label: "Email Verified",
      value: user.emailVerified ? "Verified" : "Not verified",
      icon: user.emailVerified ? CheckCircle : XCircle,
      valueClass: user.emailVerified ? "text-success-text" : "text-error-text",
    },
    {
      label: "Status",
      value: user.disabled ? "Disabled" : "Active",
      icon: Shield,
      valueClass: user.disabled ? "text-error-text" : "text-success-text",
    },
    {
      label: "Teams",
      value: String(user.teamCount),
      icon: Users,
    },
    {
      label: "Created",
      value: formatDateTime(user.createdAt),
      icon: Calendar,
    },
    {
      label: "Updated",
      value: formatDateTime(user.updatedAt),
      icon: Calendar,
    },
  ];

  return (
    <div>
      <PageHeader
        title={user.email}
        subtitle={`${user.firstName} ${user.lastName}`}
        backTo="/users"
        actions={
          <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
            <Button
              variant="secondary"
              onClick={openImpersonateDialog}
              disabled={user.disabled}
            >
              Impersonate
            </Button>
            <Button
              variant="secondary"
              onClick={() => setShowResetDialog(true)}
            >
              Reset Password
            </Button>
            {user.disabled ? (
              <Button
                variant="success"
                onClick={() => setShowDisableDialog(true)}
              >
                Enable
              </Button>
            ) : (
              <Button
                variant="danger"
                onClick={() => setShowDisableDialog(true)}
              >
                Disable
              </Button>
            )}
          </div>
        }
      />

      {/* User Info Card */}
      <div className="bg-surface-card rounded-lg border border-border-default p-6">
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
          {infoItems.map((item) => (
            <div key={item.label} className="flex items-start gap-3">
              <div className="flex-shrink-0 w-10 h-10 rounded-lg bg-primary-50 flex items-center justify-center">
                <item.icon className="h-5 w-5 text-primary-500" />
              </div>
              <div>
                <p className="text-xs font-medium uppercase tracking-wider text-text-muted">
                  {item.label}
                </p>
                <p
                  className={`text-sm font-medium ${item.valueClass ?? "text-text-primary "}`}
                >
                  {item.value}
                </p>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Feature Flags */}
      <div className="mt-6">
        <UserFeatureFlags userIdentifier={identifier} />
      </div>

      {/* Disable/Enable confirmation dialog */}
      {showDisableDialog && (
        <ConfirmDialog
          title={user.disabled ? "Enable User" : "Disable User"}
          message={
            user.disabled
              ? `Are you sure you want to enable "${user.email}"? They will be able to log in again.`
              : `Are you sure you want to disable "${user.email}"? They will no longer be able to log in.`
          }
          confirmLabel={user.disabled ? "Enable" : "Disable"}
          cancelLabel="Cancel"
          variant={user.disabled ? "default" : "danger"}
          isLoading={disableUser.isPending || enableUser.isPending}
          onConfirm={handleToggleDisable}
          onCancel={() => setShowDisableDialog(false)}
        />
      )}

      {/* Reset password confirmation dialog */}
      {showResetDialog && (
        <ConfirmDialog
          title="Reset Password"
          message="This will send a password reset email to the user."
          confirmLabel="Reset Password"
          cancelLabel="Cancel"
          variant="default"
          isLoading={resetPassword.isPending}
          onConfirm={handleResetPassword}
          onCancel={() => setShowResetDialog(false)}
        />
      )}

      {/* Impersonate dialog */}
      <ModalWrapper
        open={showImpersonateDialog}
        onClose={() => setShowImpersonateDialog(false)}
        title="Impersonate User"
        subtitle={`You are about to impersonate ${user.email}. This action is audited.`}
        size="lg"
        footer={
          <>
            <Button
              variant="secondary"
              onClick={() => setShowImpersonateDialog(false)}
              className="mr-auto"
            >
              Cancel
            </Button>
            <Button
              variant="primary"
              leftIcon={<Eye />}
              onClick={() => handleImpersonate("READ_ONLY")}
              disabled={!isFormValid || createImpersonation.isPending}
            >
              Start in Read Mode
            </Button>
            <Button
              variant="danger"
              leftIcon={<Pencil />}
              onClick={() => handleImpersonate("FULL")}
              disabled={!isFormValid || createImpersonation.isPending}
            >
              Start in Full Access
            </Button>
          </>
        }
      >
        <div className="space-y-4">
          {/* Team selector */}
          <div>
            <label className="block text-sm font-medium text-text-primary mb-1">
              Team *
            </label>
            {userTeams && userTeams.length > 0 ? (
              <select
                className="w-full px-3 py-2 border border-border-default rounded-md text-sm bg-surface-primary text-text-primary"
                value={impersonateForm.teamIdentifier}
                onChange={(e) =>
                  setImpersonateForm((f) => ({
                    ...f,
                    teamIdentifier: e.target.value,
                  }))
                }
              >
                <option value="">Select a team...</option>
                {userTeams.map((team) => (
                  <option key={team.teamIdentifier} value={team.teamIdentifier}>
                    {team.teamName} ({team.teamIdentifier})
                  </option>
                ))}
              </select>
            ) : (
              <input
                type="text"
                className="w-full px-3 py-2 border border-border-default rounded-md text-sm bg-surface-primary text-text-primary"
                placeholder="TEA01HQJK4B2X5M3N7P8Q9R0S1T2"
                value={impersonateForm.teamIdentifier}
                onChange={(e) =>
                  setImpersonateForm((f) => ({
                    ...f,
                    teamIdentifier: e.target.value,
                  }))
                }
              />
            )}
          </div>

          {/* Reason (rich text) */}
          <div>
            <label className="block text-sm font-medium text-text-primary mb-1">
              Reason *
            </label>
            <RichTextEditor
              value={impersonateForm.reason}
              onChange={(value) =>
                setImpersonateForm((f) => ({ ...f, reason: value }))
              }
              placeholder="Debugging issue reported in ticket..."
            />
          </div>

          {/* Session Duration */}
          <div>
            <label className="block text-sm font-medium text-text-primary mb-1.5">
              Session Duration
            </label>
            <div className="flex gap-2">
              {DURATION_PRESETS.map((preset) => (
                <button
                  key={preset.value}
                  type="button"
                  onClick={() =>
                    setImpersonateForm((f) => ({
                      ...f,
                      durationMinutes: preset.value,
                    }))
                  }
                  className={`flex-1 px-3 py-2 rounded-lg border text-sm font-medium transition-all ${
                    impersonateForm.durationMinutes === preset.value
                      ? "bg-primary-500 text-white border-primary-600 shadow-sm"
                      : "bg-surface-card text-text-secondary border-border-default hover:border-border-strong"
                  }`}
                >
                  {preset.label}
                </button>
              ))}
            </div>
            <p className="mt-1.5 text-xs text-text-muted">
              Session will automatically expire after the selected duration.
              Maximum is 1 hour.
            </p>
          </div>
        </div>
      </ModalWrapper>

      {/* Password confirmation for impersonation */}
      <PasswordConfirmationDialog
        open={showPasswordDialog}
        onClose={() => {
          setShowPasswordDialog(false);
          setPasswordError(null);
        }}
        onConfirm={handlePasswordConfirm}
        isLoading={createImpersonation.isPending}
        error={passwordError}
        title="Confirm your identity"
        subtitle="Enter your password to start the impersonation session."
      />
    </div>
  );
};
