import { useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { format } from "date-fns";
import { Users, Calendar, Mail } from "lucide-react";
import { PageHeader, Button, ConfirmDialog } from "@buurman/ui";
import { useTeam, useUpdateTeam, useDeleteTeam } from "../hooks/useTeams";
import { LoadingSpinner } from "../components/LoadingSpinner";

export const TeamDetailPage = () => {
  const { identifier } = useParams<{ identifier: string }>();
  const navigate = useNavigate();
  const { data: team, isLoading, error } = useTeam(identifier!);
  const updateTeam = useUpdateTeam();
  const deleteTeam = useDeleteTeam();

  const [isEditing, setIsEditing] = useState(false);
  const [editName, setEditName] = useState("");
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);

  if (isLoading) {
    return <LoadingSpinner message="Loading team..." />;
  }

  if (error || !team) {
    return (
      <div className="text-center py-12">
        <p className="text-red-600 dark:text-red-400">Failed to load team.</p>
        <button
          onClick={() => navigate("/teams")}
          className="mt-4 text-sm text-[#5c7cfa] hover:underline"
        >
          Back to teams
        </button>
      </div>
    );
  }

  const startEditing = () => {
    setEditName(team.teamName);
    setIsEditing(true);
  };

  const cancelEditing = () => {
    setIsEditing(false);
    setEditName("");
  };

  const handleSave = () => {
    if (!editName.trim()) return;
    updateTeam.mutate(
      { identifier: identifier!, data: { name: editName.trim() } },
      {
        onSuccess: () => setIsEditing(false),
      },
    );
  };

  const handleDelete = () => {
    deleteTeam.mutate(identifier!, {
      onSuccess: () => navigate("/teams"),
    });
  };

  return (
    <div>
      <PageHeader
        title={team.teamName}
        subtitle={`#${identifier}`}
        backTo="/teams"
        actions={
          <div className="flex items-center gap-2">
            {!isEditing && (
              <Button variant="secondary" onClick={startEditing}>
                Edit
              </Button>
            )}
            <Button variant="danger" onClick={() => setShowDeleteDialog(true)}>
              Delete
            </Button>
          </div>
        }
      />

      {/* Team Info Card */}
      <div className="bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] p-6">
        {isEditing ? (
          /* Edit Form */
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1.5">
                Team Name
              </label>
              <input
                type="text"
                value={editName}
                onChange={(e) => setEditName(e.target.value)}
                className="w-full max-w-md px-3 py-2.5 text-sm rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#14161f] text-[#1a1d2e] dark:text-[#eef0f6] focus:outline-none focus:border-[#5c7cfa] focus:ring-2 focus:ring-[#5c7cfa]/20 transition-colors"
                autoFocus
              />
            </div>
            <div className="flex items-center gap-2">
              <Button onClick={handleSave} isLoading={updateTeam.isPending}>
                Save
              </Button>
              <Button variant="secondary" onClick={cancelEditing}>
                Cancel
              </Button>
            </div>
          </div>
        ) : (
          /* Display Info */
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
            <div className="flex items-start gap-3">
              <div className="flex-shrink-0 w-10 h-10 rounded-lg bg-[#f0f4ff] dark:bg-[#5c7cfa]/10 flex items-center justify-center">
                <Users className="h-5 w-5 text-[#5c7cfa] dark:text-[#91a7ff]" />
              </div>
              <div>
                <p className="text-xs font-medium uppercase tracking-wider text-[#9ca0b8] dark:text-[#5c6180]">
                  Members
                </p>
                <p className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                  {team.memberCount}
                </p>
              </div>
            </div>

            <div className="flex items-start gap-3">
              <div className="flex-shrink-0 w-10 h-10 rounded-lg bg-[#f0f4ff] dark:bg-[#5c7cfa]/10 flex items-center justify-center">
                <Mail className="h-5 w-5 text-[#5c7cfa] dark:text-[#91a7ff]" />
              </div>
              <div>
                <p className="text-xs font-medium uppercase tracking-wider text-[#9ca0b8] dark:text-[#5c6180]">
                  Owner
                </p>
                <p className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                  {team.ownerEmail}
                </p>
              </div>
            </div>

            <div className="flex items-start gap-3">
              <div className="flex-shrink-0 w-10 h-10 rounded-lg bg-[#f0f4ff] dark:bg-[#5c7cfa]/10 flex items-center justify-center">
                <Calendar className="h-5 w-5 text-[#5c7cfa] dark:text-[#91a7ff]" />
              </div>
              <div>
                <p className="text-xs font-medium uppercase tracking-wider text-[#9ca0b8] dark:text-[#5c6180]">
                  Created
                </p>
                <p className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                  {format(new Date(team.createdAt), "dd MMM yyyy, HH:mm")}
                </p>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Delete confirmation dialog */}
      {showDeleteDialog && (
        <ConfirmDialog
          title="Delete Team"
          message={`Are you sure you want to delete "${team.teamName}"? This action cannot be undone. All team data including properties, tenants, contracts, and financial records will be permanently removed.`}
          confirmLabel="Delete"
          cancelLabel="Cancel"
          variant="danger"
          isLoading={deleteTeam.isPending}
          onConfirm={handleDelete}
          onCancel={() => setShowDeleteDialog(false)}
        />
      )}
    </div>
  );
};
