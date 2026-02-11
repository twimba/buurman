import { useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { format } from "date-fns";
import {
  Mail,
  User,
  Phone,
  CheckCircle,
  XCircle,
  Shield,
  Users,
  Calendar,
} from "lucide-react";
import { PageHeader, Button, ConfirmDialog } from "@buurman/ui";
import {
  useUser,
  useDisableUser,
  useEnableUser,
  useResetPassword,
} from "../hooks/useUsers";
import { LoadingSpinner } from "../components/LoadingSpinner";

export const UserDetailPage = () => {
  const { identifier } = useParams<{ identifier: string }>();
  const navigate = useNavigate();
  const { data: user, isLoading, error } = useUser(identifier!);
  const disableUser = useDisableUser();
  const enableUser = useEnableUser();
  const resetPassword = useResetPassword();

  const [showDisableDialog, setShowDisableDialog] = useState(false);
  const [showResetDialog, setShowResetDialog] = useState(false);

  if (isLoading) {
    return <LoadingSpinner message="Loading user..." />;
  }

  if (error || !user) {
    return (
      <div className="text-center py-12">
        <p className="text-red-600 dark:text-red-400">Failed to load user.</p>
        <button
          onClick={() => navigate("/users")}
          className="mt-4 text-sm text-[#5c7cfa] hover:underline"
        >
          Back to users
        </button>
      </div>
    );
  }

  const handleToggleDisable = () => {
    const mutation = user.disabled ? enableUser : disableUser;
    mutation.mutate(identifier!, {
      onSuccess: () => setShowDisableDialog(false),
    });
  };

  const handleResetPassword = () => {
    resetPassword.mutate(identifier!, {
      onSuccess: () => setShowResetDialog(false),
    });
  };

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
      valueClass: user.emailVerified
        ? "text-emerald-600 dark:text-emerald-400"
        : "text-red-600 dark:text-red-400",
    },
    {
      label: "Status",
      value: user.disabled ? "Disabled" : "Active",
      icon: Shield,
      valueClass: user.disabled
        ? "text-red-600 dark:text-red-400"
        : "text-emerald-600 dark:text-emerald-400",
    },
    {
      label: "Teams",
      value: String(user.teamCount),
      icon: Users,
    },
    {
      label: "Created",
      value: format(new Date(user.createdAt), "dd MMM yyyy HH:mm"),
      icon: Calendar,
    },
    {
      label: "Updated",
      value: format(new Date(user.updatedAt), "dd MMM yyyy HH:mm"),
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
      <div className="bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] p-6">
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
          {infoItems.map((item) => (
            <div key={item.label} className="flex items-start gap-3">
              <div className="flex-shrink-0 w-10 h-10 rounded-lg bg-[#f0f4ff] dark:bg-[#5c7cfa]/10 flex items-center justify-center">
                <item.icon className="h-5 w-5 text-[#5c7cfa] dark:text-[#91a7ff]" />
              </div>
              <div>
                <p className="text-xs font-medium uppercase tracking-wider text-[#9ca0b8] dark:text-[#5c6180]">
                  {item.label}
                </p>
                <p
                  className={`text-sm font-medium ${item.valueClass || "text-[#1a1d2e] dark:text-[#eef0f6]"}`}
                >
                  {item.value}
                </p>
              </div>
            </div>
          ))}
        </div>
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
    </div>
  );
};
