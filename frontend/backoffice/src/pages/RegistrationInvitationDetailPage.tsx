import { useParams, useNavigate } from 'react-router-dom';
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
} from 'lucide-react';
import {
  RefreshButton,
  ConfirmDialog,
  RichTextEditor,
  RichTextDisplay,
} from '@buurman/ui';
import { formatDateTime } from '../utils/dateFormatting';
import { useState } from 'react';
import {
  useRegistrationInvitation,
  useRevokeRegistrationInvitation,
  useUpdateRegistrationInvitationNote,
} from '../hooks/useRegistrationInvitations';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { SendRegistrationInvitationModal } from '../components/SendRegistrationInvitationModal';

const STATUS_STYLES: Record<string, string> = {
  ACTIVE: 'bg-success-bg text-success-text border-success-border',
  EXPIRED: 'bg-warning-bg text-warning-text border-warning-border',
  EXHAUSTED: 'bg-surface-inset text-text-secondary border-border-default',
  REVOKED: 'bg-error-bg text-error-text border-error-border',
};

export function RegistrationInvitationDetailPage() {
  const { identifier } = useParams<{ identifier: string }>();
  const navigate = useNavigate();
  const {
    data: invitation,
    isLoading,
    refetch,
  } = useRegistrationInvitation(identifier ?? '');
  const revokeMutation = useRevokeRegistrationInvitation();
  const updateNoteMutation = useUpdateRegistrationInvitationNote();
  const [showRevoke, setShowRevoke] = useState(false);
  const [showSend, setShowSend] = useState(false);
  const [editingNote, setEditingNote] = useState(false);
  const [noteValue, setNoteValue] = useState('');

  const handleCopyLink = () => {
    if (!invitation) {
      return;
    }
    const url = `${window.location.protocol}//app.${window.location.hostname.replace(/^backoffice\./, '')}/register?code=${invitation.code}`;
    navigator.clipboard.writeText(url);
  };

  const handleRevoke = async () => {
    if (!identifier) {
      return;
    }
    await revokeMutation.mutateAsync(identifier);
    setShowRevoke(false);
    refetch();
  };

  if (isLoading) {
    return <LoadingSpinner />;
  }
  if (!invitation) {
    return <div className="text-text-secondary">Invitation not found</div>;
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate('/registration-invitations')}
            className="p-1.5 rounded-md hover:bg-surface-inset transition-colors"
          >
            <ArrowLeft className="h-5 w-5 text-text-secondary" />
          </button>
          <Ticket className="h-6 w-6 text-primary-500" />
          <div>
            <h1 className="text-2xl font-bold font-mono text-text-primary">
              {invitation.code}
            </h1>
            <span
              className={`inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-medium mt-1 ${STATUS_STYLES[invitation.status] ?? ''}`}
            >
              {invitation.status}
            </span>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <RefreshButton onClick={() => refetch()} />
          <button
            onClick={handleCopyLink}
            className="inline-flex items-center gap-2 rounded-md border border-border-default px-3 py-2 text-sm font-medium text-text-primary hover:bg-surface-inset transition-colors"
          >
            <Copy className="h-4 w-4" />
            Copy Link
          </button>
          {invitation.status === 'ACTIVE' && (
            <>
              <button
                onClick={() => setShowSend(true)}
                className="inline-flex items-center gap-2 rounded-md bg-primary-600 px-3 py-2 text-sm font-medium text-white hover:bg-primary-700 transition-colors"
              >
                <Send className="h-4 w-4" />
                Send
              </button>
              <button
                onClick={() => setShowRevoke(true)}
                className="inline-flex items-center gap-2 rounded-md border border-error-border px-3 py-2 text-sm font-medium text-error-text hover:bg-error-bg transition-colors"
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
        <div className="rounded-lg border border-border-default bg-surface-card p-4">
          <div className="flex items-center gap-2 text-text-secondary text-xs font-medium mb-1">
            <Users className="h-3.5 w-3.5" />
            Usages
          </div>
          <div className="text-xl font-bold font-mono text-text-primary">
            {invitation.usageCount}{' '}
            <span className="text-text-muted text-sm font-normal">
              / {invitation.maxUsages ?? '\u221E'}
            </span>
          </div>
        </div>
        <div className="rounded-lg border border-border-default bg-surface-card p-4">
          <div className="flex items-center gap-2 text-text-secondary text-xs font-medium mb-1">
            <Clock className="h-3.5 w-3.5" />
            Expires
          </div>
          <div className="text-sm font-medium text-text-primary">
            {invitation.expiresAt
              ? formatDateTime(invitation.expiresAt)
              : 'Never'}
          </div>
        </div>
        <div className="rounded-lg border border-border-default bg-surface-card p-4">
          <div className="flex items-center gap-2 text-text-secondary text-xs font-medium mb-1">
            <User className="h-3.5 w-3.5" />
            Created By
          </div>
          <div className="text-sm font-medium text-text-primary truncate">
            {invitation.createdBy}
          </div>
        </div>
        <div className="rounded-lg border border-border-default bg-surface-card p-4">
          <div className="flex items-center gap-2 text-text-secondary text-xs font-medium mb-1">
            <Calendar className="h-3.5 w-3.5" />
            Created At
          </div>
          <div className="text-sm font-medium text-text-primary">
            {formatDateTime(invitation.createdAt)}
          </div>
        </div>
      </div>

      {/* Revocation Info */}
      {invitation.revokedAt && (
        <div className="rounded-lg border border-error-border bg-error-bg p-4">
          <div className="text-sm text-error-text">
            <strong>Revoked</strong> by {invitation.revokedBy} on{' '}
            {formatDateTime(invitation.revokedAt)}
          </div>
        </div>
      )}

      {/* Internal Note */}
      <div className="rounded-lg border border-border-default bg-surface-card p-5">
        <div className="flex items-center justify-between mb-3">
          <h2 className="text-lg font-semibold text-text-primary">
            Internal Note
          </h2>
          {!editingNote ? (
            <button
              onClick={() => {
                setNoteValue(invitation.note ?? '');
                setEditingNote(true);
              }}
              className="text-sm text-primary-600 hover:text-primary-800 font-medium transition-colors"
            >
              {invitation.note ? 'Edit' : 'Add Note'}
            </button>
          ) : (
            <div className="flex gap-2">
              <button
                onClick={() => setEditingNote(false)}
                className="text-sm text-text-secondary hover:text-text-primary font-medium transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={async () => {
                  if (!identifier) {
                    return;
                  }
                  await updateNoteMutation.mutateAsync({
                    identifier,
                    data: { note: noteValue },
                  });
                  setEditingNote(false);
                }}
                disabled={updateNoteMutation.isPending}
                className="text-sm text-white bg-primary-600 hover:bg-primary-700 disabled:opacity-50 px-3 py-1 rounded-md font-medium transition-colors"
              >
                {updateNoteMutation.isPending ? 'Saving...' : 'Save'}
              </button>
            </div>
          )}
        </div>
        {editingNote ? (
          <RichTextEditor
            value={noteValue}
            onChange={setNoteValue}
            placeholder="Add an internal note about this invitation..."
          />
        ) : invitation.note ? (
          <RichTextDisplay content={invitation.note} />
        ) : (
          <p className="text-sm text-text-muted italic">No note added</p>
        )}
      </div>

      {/* Usage History */}
      <div>
        <h2 className="text-lg font-semibold text-text-primary mb-3">
          Usage History
        </h2>
        <div className="overflow-hidden rounded-lg border border-border-default bg-surface-card">
          <table className="min-w-full divide-y divide-border-default">
            <thead className="bg-surface-inset">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                  User
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                  Email
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                  Used At
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border-subtle">
              {invitation.usages?.map((usage, idx) => (
                <tr key={idx} className="hover:bg-surface-inset/50">
                  <td className="px-4 py-3 text-sm text-text-primary">
                    {usage.userName}
                  </td>
                  <td className="px-4 py-3 text-sm text-text-secondary">
                    {usage.userEmail}
                  </td>
                  <td className="px-4 py-3 text-sm text-text-secondary">
                    {formatDateTime(usage.usedAt)}
                  </td>
                </tr>
              ))}
              {(!invitation.usages || invitation.usages.length === 0) && (
                <tr>
                  <td
                    colSpan={3}
                    className="px-4 py-8 text-center text-sm text-text-secondary"
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
