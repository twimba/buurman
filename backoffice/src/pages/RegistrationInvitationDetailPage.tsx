import { useParams, useNavigate } from "react-router-dom";
import {
  ArrowLeft,
  Copy,
  Send,
  Ban,
  Ticket,
  Clock,
  Users,
  User,
  Calendar,
} from "lucide-react";
import { RefreshButton, ConfirmDialog } from "@buurman/ui";
import { format } from "date-fns";
import { useState } from "react";
import {
  useRegistrationInvitation,
  useRevokeRegistrationInvitation,
} from "../hooks/useRegistrationInvitations";
import { LoadingSpinner } from "../components/LoadingSpinner";
import { SendRegistrationInvitationModal } from "../components/SendRegistrationInvitationModal";

const STATUS_STYLES: Record<string, string> = {
  ACTIVE:
    "bg-emerald-50 text-emerald-700 border-emerald-200 dark:bg-emerald-900/20 dark:text-emerald-400 dark:border-emerald-800",
  EXPIRED:
    "bg-amber-50 text-amber-700 border-amber-200 dark:bg-amber-900/20 dark:text-amber-400 dark:border-amber-800",
  EXHAUSTED:
    "bg-zinc-50 text-zinc-600 border-zinc-200 dark:bg-zinc-800/40 dark:text-zinc-400 dark:border-zinc-700",
  REVOKED:
    "bg-red-50 text-red-700 border-red-200 dark:bg-red-900/20 dark:text-red-400 dark:border-red-800",
};

export function RegistrationInvitationDetailPage() {
  const { identifier } = useParams<{ identifier: string }>();
  const navigate = useNavigate();
  const {
    data: invitation,
    isLoading,
    refetch,
  } = useRegistrationInvitation(identifier!);
  const revokeMutation = useRevokeRegistrationInvitation();
  const [showRevoke, setShowRevoke] = useState(false);
  const [showSend, setShowSend] = useState(false);

  const handleCopyLink = () => {
    if (!invitation) return;
    const url = `${window.location.protocol}//app.${window.location.hostname.replace(/^backoffice\./, "")}/register?code=${invitation.code}`;
    navigator.clipboard.writeText(url);
  };

  const handleRevoke = async () => {
    if (!identifier) return;
    await revokeMutation.mutateAsync(identifier);
    setShowRevoke(false);
    refetch();
  };

  if (isLoading) return <LoadingSpinner />;
  if (!invitation)
    return <div className="text-zinc-500">Invitation not found</div>;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate("/registration-invitations")}
            className="p-1.5 rounded-md hover:bg-zinc-100 dark:hover:bg-zinc-700 transition-colors"
          >
            <ArrowLeft className="h-5 w-5 text-zinc-500" />
          </button>
          <Ticket className="h-6 w-6 text-indigo-500" />
          <div>
            <h1 className="text-2xl font-bold font-mono text-zinc-900 dark:text-zinc-100">
              {invitation.code}
            </h1>
            <span
              className={`inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-medium mt-1 ${STATUS_STYLES[invitation.status] || ""}`}
            >
              {invitation.status}
            </span>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <RefreshButton onClick={() => refetch()} />
          <button
            onClick={handleCopyLink}
            className="inline-flex items-center gap-2 rounded-lg border border-zinc-200 dark:border-zinc-700 px-3 py-2 text-sm font-medium text-zinc-700 dark:text-zinc-300 hover:bg-zinc-50 dark:hover:bg-zinc-700 transition-colors"
          >
            <Copy className="h-4 w-4" />
            Copy Link
          </button>
          {invitation.status === "ACTIVE" && (
            <>
              <button
                onClick={() => setShowSend(true)}
                className="inline-flex items-center gap-2 rounded-lg bg-indigo-600 px-3 py-2 text-sm font-medium text-white hover:bg-indigo-700 transition-colors"
              >
                <Send className="h-4 w-4" />
                Send
              </button>
              <button
                onClick={() => setShowRevoke(true)}
                className="inline-flex items-center gap-2 rounded-lg border border-red-200 dark:border-red-800 px-3 py-2 text-sm font-medium text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20 transition-colors"
              >
                <Ban className="h-4 w-4" />
                Revoke
              </button>
            </>
          )}
        </div>
      </div>

      {/* Info Grid */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="rounded-xl border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-800/50 p-4">
          <div className="flex items-center gap-2 text-zinc-500 text-xs font-medium mb-1">
            <Users className="h-3.5 w-3.5" />
            Usages
          </div>
          <div className="text-xl font-bold font-mono text-zinc-900 dark:text-zinc-100">
            {invitation.usageCount}{" "}
            <span className="text-zinc-400 text-sm font-normal">
              / {invitation.maxUsages ?? "\u221E"}
            </span>
          </div>
        </div>
        <div className="rounded-xl border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-800/50 p-4">
          <div className="flex items-center gap-2 text-zinc-500 text-xs font-medium mb-1">
            <Clock className="h-3.5 w-3.5" />
            Expires
          </div>
          <div className="text-sm font-medium text-zinc-900 dark:text-zinc-100">
            {invitation.expiresAt
              ? format(new Date(invitation.expiresAt), "MMM d, yyyy HH:mm")
              : "Never"}
          </div>
        </div>
        <div className="rounded-xl border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-800/50 p-4">
          <div className="flex items-center gap-2 text-zinc-500 text-xs font-medium mb-1">
            <User className="h-3.5 w-3.5" />
            Created By
          </div>
          <div className="text-sm font-medium text-zinc-900 dark:text-zinc-100 truncate">
            {invitation.createdBy}
          </div>
        </div>
        <div className="rounded-xl border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-800/50 p-4">
          <div className="flex items-center gap-2 text-zinc-500 text-xs font-medium mb-1">
            <Calendar className="h-3.5 w-3.5" />
            Created At
          </div>
          <div className="text-sm font-medium text-zinc-900 dark:text-zinc-100">
            {format(new Date(invitation.createdAt), "MMM d, yyyy HH:mm")}
          </div>
        </div>
      </div>

      {/* Revocation Info */}
      {invitation.revokedAt && (
        <div className="rounded-xl border border-red-200 dark:border-red-800 bg-red-50 dark:bg-red-900/20 p-4">
          <div className="text-sm text-red-700 dark:text-red-400">
            <strong>Revoked</strong> by {invitation.revokedBy} on{" "}
            {format(new Date(invitation.revokedAt), "MMM d, yyyy HH:mm")}
          </div>
        </div>
      )}

      {/* Usage History */}
      <div>
        <h2 className="text-lg font-semibold text-zinc-900 dark:text-zinc-100 mb-3">
          Usage History
        </h2>
        <div className="overflow-hidden rounded-xl border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-800/50">
          <table className="min-w-full divide-y divide-zinc-200 dark:divide-zinc-700">
            <thead className="bg-zinc-50 dark:bg-zinc-800">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium text-zinc-500 uppercase tracking-wider">
                  User
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-zinc-500 uppercase tracking-wider">
                  Email
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-zinc-500 uppercase tracking-wider">
                  Used At
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-100 dark:divide-zinc-700/50">
              {invitation.usages?.map((usage, idx) => (
                <tr
                  key={idx}
                  className="hover:bg-zinc-50/50 dark:hover:bg-zinc-800/30"
                >
                  <td className="px-4 py-3 text-sm text-zinc-900 dark:text-zinc-100">
                    {usage.userName}
                  </td>
                  <td className="px-4 py-3 text-sm text-zinc-600 dark:text-zinc-400">
                    {usage.userEmail}
                  </td>
                  <td className="px-4 py-3 text-sm text-zinc-500 dark:text-zinc-400">
                    {format(new Date(usage.usedAt), "MMM d, yyyy HH:mm")}
                  </td>
                </tr>
              ))}
              {(!invitation.usages || invitation.usages.length === 0) && (
                <tr>
                  <td
                    colSpan={3}
                    className="px-4 py-8 text-center text-sm text-zinc-500"
                  >
                    No usages yet
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Revoke Confirm */}
      {showRevoke && (
        <ConfirmDialog
          title="Revoke Invitation"
          message={`Are you sure you want to revoke "${invitation.code}"? It will no longer be usable.`}
          confirmLabel="Revoke"
          variant="danger"
          isLoading={revokeMutation.isPending}
          onConfirm={handleRevoke}
          onCancel={() => setShowRevoke(false)}
        />
      )}

      {/* Send Modal */}
      {showSend && (
        <SendRegistrationInvitationModal
          invitation={invitation}
          onClose={() => setShowSend(false)}
        />
      )}
    </div>
  );
}
