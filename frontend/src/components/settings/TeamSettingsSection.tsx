import { useState } from 'react';
import {
  Users,
  UserPlus,
  Mail,
  Trash2,
  Edit3,
  Save,
  X,
  Shield,
  Crown,
  Loader2,
} from 'lucide-react';
import {
  useCurrentTeam,
  useTeamMembers,
  useCreateInvitation,
  useRemoveMember,
  useUpdateMemberRole,
  useTransferOwnership,
  useUpdateTeam,
} from '../../hooks/useTeamHooks';
import { useTeam } from '../../context/TeamContext';

type Role = 'TEAM_ADMIN' | 'TEAM_EDITOR' | 'TEAM_VIEWER';

export const TeamSettingsSection = () => {
  const { canManageMembers } = useTeam();
  const { data: team, isLoading: teamLoading } = useCurrentTeam();
  const { data: members, isLoading: membersLoading } = useTeamMembers(
    team?.teamId
  );

  const [isEditingTeamName, setIsEditingTeamName] = useState(false);
  const [showInviteModal, setShowInviteModal] = useState(false);
  const [showRemoveModal, setShowRemoveModal] = useState(false);
  const [showTransferModal, setShowTransferModal] = useState(false);
  const [showRoleModal, setShowRoleModal] = useState(false);
  const [selectedMemberId, setSelectedMemberId] = useState<string | null>(null);
  const [teamName, setTeamName] = useState('');
  const [inviteEmail, setInviteEmail] = useState('');
  const [inviteRole, setInviteRole] = useState<Role>('TEAM_VIEWER');
  const [newRole, setNewRole] = useState<Role>('TEAM_VIEWER');

  const updateTeamMutation = useUpdateTeam(team?.teamId || '');
  const createInvitationMutation = useCreateInvitation(team?.teamId || '');
  const removeMemberMutation = useRemoveMember(team?.teamId || '');
  const updateRoleMutation = useUpdateMemberRole(team?.teamId || '');
  const transferOwnershipMutation = useTransferOwnership(team?.teamId || '');

  const roleLabels: Record<Role, string> = {
    TEAM_ADMIN: 'Admin',
    TEAM_EDITOR: 'Editor',
    TEAM_VIEWER: 'Viewer',
  };

  const roleDescriptions: Record<Role, string> = {
    TEAM_ADMIN: 'Full access to all features and settings',
    TEAM_EDITOR: 'Can create and edit properties, tenants, and contracts',
    TEAM_VIEWER: 'Read-only access to all data',
  };

  const roleColors: Record<Role, string> = {
    TEAM_ADMIN: 'bg-purple-100 text-purple-800',
    TEAM_EDITOR: 'bg-blue-100 text-blue-800',
    TEAM_VIEWER: 'bg-gray-100 text-gray-800',
  };

  const selectedMember = members?.find((m) => m.memberId === selectedMemberId);

  const handleStartEditTeamName = () => {
    setTeamName(team?.teamName || '');
    setIsEditingTeamName(true);
  };

  const handleSaveTeamName = () => {
    if (!teamName.trim()) return;
    updateTeamMutation.mutate(
      { name: teamName },
      {
        onSuccess: () => setIsEditingTeamName(false),
      }
    );
  };

  const handleInviteMember = () => {
    if (!inviteEmail.trim()) return;
    createInvitationMutation.mutate(
      { email: inviteEmail, role: inviteRole },
      {
        onSuccess: () => {
          setShowInviteModal(false);
          setInviteEmail('');
          setInviteRole('TEAM_VIEWER');
        },
      }
    );
  };

  const handleRemoveMember = () => {
    if (!selectedMemberId) return;
    removeMemberMutation.mutate(selectedMemberId, {
      onSuccess: () => {
        setShowRemoveModal(false);
        setSelectedMemberId(null);
      },
    });
  };

  const handleUpdateRole = () => {
    if (!selectedMemberId) return;
    updateRoleMutation.mutate(
      { memberId: selectedMemberId, data: { role: newRole } },
      {
        onSuccess: () => {
          setShowRoleModal(false);
          setSelectedMemberId(null);
        },
      }
    );
  };

  const handleTransferOwnership = () => {
    if (!selectedMemberId) return;
    transferOwnershipMutation.mutate(selectedMemberId, {
      onSuccess: () => {
        setShowTransferModal(false);
        setSelectedMemberId(null);
      },
    });
  };

  if (teamLoading || membersLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <Loader2 className="h-8 w-8 animate-spin text-blue-600" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Team Name Card */}
      <div className="bg-white rounded-lg shadow">
        <div className="p-6 border-b border-gray-200">
          <h2 className="text-xl font-semibold text-gray-900">Team Settings</h2>
          <p className="text-sm text-gray-600 mt-1">
            Manage your team name and basic information
          </p>
        </div>

        <div className="p-6">
          <div className="flex items-center gap-4">
            <div className="flex-1">
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Team Name
              </label>
              {isEditingTeamName ? (
                <input
                  type="text"
                  value={teamName}
                  onChange={(e) => setTeamName(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
              ) : (
                <p className="text-lg font-semibold text-gray-900">
                  {team?.teamName}
                </p>
              )}
            </div>
            {canManageMembers && (
              <>
                {isEditingTeamName ? (
                  <div className="flex gap-2">
                    <button
                      onClick={() => setIsEditingTeamName(false)}
                      className="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors flex items-center gap-2"
                    >
                      <X className="h-4 w-4" />
                      Cancel
                    </button>
                    <button
                      onClick={handleSaveTeamName}
                      disabled={updateTeamMutation.isPending}
                      className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2 disabled:opacity-50"
                    >
                      {updateTeamMutation.isPending ? (
                        <Loader2 className="h-4 w-4 animate-spin" />
                      ) : (
                        <Save className="h-4 w-4" />
                      )}
                      Save
                    </button>
                  </div>
                ) : (
                  <button
                    onClick={handleStartEditTeamName}
                    className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2"
                  >
                    <Edit3 className="h-4 w-4" />
                    Edit
                  </button>
                )}
              </>
            )}
          </div>
        </div>
      </div>

      {/* Team Members Card */}
      <div className="bg-white rounded-lg shadow">
        <div className="p-6 border-b border-gray-200">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-xl font-semibold text-gray-900">
                Team Members
              </h2>
              <p className="text-sm text-gray-600 mt-1">
                {members?.length || 0} member{members?.length !== 1 ? 's' : ''}
              </p>
            </div>
            {canManageMembers && (
              <button
                onClick={() => setShowInviteModal(true)}
                className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2"
              >
                <UserPlus className="h-4 w-4" />
                Invite Member
              </button>
            )}
          </div>
        </div>

        <div className="divide-y divide-gray-200">
          {members?.map((member) => (
            <div key={member.memberId} className="p-6 hover:bg-gray-50">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-4">
                  <div className="h-12 w-12 rounded-full bg-blue-100 flex items-center justify-center">
                    <Users className="h-6 w-6 text-blue-600" />
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <p className="font-semibold text-gray-900">
                        {member.name}
                      </p>
                      {member.isCurrentUser && (
                        <span className="px-2 py-0.5 bg-green-100 text-green-800 text-xs font-semibold rounded">
                          You
                        </span>
                      )}
                      {member.isOwner && (
                        <span className="px-2 py-0.5 bg-yellow-100 text-yellow-800 text-xs font-semibold rounded flex items-center gap-1">
                          <Crown className="h-3 w-3" />
                          Owner
                        </span>
                      )}
                    </div>
                    <p className="text-sm text-gray-600">{member.email}</p>
                    <p className="text-xs text-gray-500 mt-1">
                      Joined {new Date(member.joinedAt).toLocaleDateString()}
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-3">
                  <div className="text-right">
                    <span
                      className={`px-3 py-1 text-xs font-semibold rounded ${roleColors[member.role as Role]}`}
                    >
                      {roleLabels[member.role as Role]}
                    </span>
                    <p className="text-xs text-gray-600 mt-1">
                      {roleDescriptions[member.role as Role]}
                    </p>
                  </div>
                  {canManageMembers && !member.isCurrentUser && (
                    <div className="flex gap-1">
                      <button
                        onClick={() => {
                          setSelectedMemberId(member.memberId);
                          setNewRole(member.role as Role);
                          setShowRoleModal(true);
                        }}
                        className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                        title="Change role"
                      >
                        <Edit3 className="h-4 w-4" />
                      </button>
                      {!member.isOwner && (
                        <button
                          onClick={() => {
                            setSelectedMemberId(member.memberId);
                            setShowTransferModal(true);
                          }}
                          className="p-2 text-yellow-600 hover:bg-yellow-50 rounded-lg transition-colors"
                          title="Transfer ownership"
                        >
                          <Crown className="h-4 w-4" />
                        </button>
                      )}
                      <button
                        onClick={() => {
                          setSelectedMemberId(member.memberId);
                          setShowRemoveModal(true);
                        }}
                        className="p-2 text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                        title="Remove member"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </div>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Roles Reference Card */}
      <div className="bg-white rounded-lg shadow">
        <div className="p-6 border-b border-gray-200">
          <h2 className="text-xl font-semibold text-gray-900">
            Roles & Permissions
          </h2>
          <p className="text-sm text-gray-600 mt-1">
            Understanding team member roles
          </p>
        </div>

        <div className="p-6 space-y-4">
          {Object.entries(roleLabels).map(([role, label]) => (
            <div
              key={role}
              className="flex items-start gap-3 p-4 bg-gray-50 rounded-lg"
            >
              <Shield className="h-5 w-5 text-gray-600 mt-0.5" />
              <div>
                <p className="font-semibold text-gray-900">{label}</p>
                <p className="text-sm text-gray-600">
                  {roleDescriptions[role as Role]}
                </p>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Invite Member Modal */}
      {showInviteModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-xl max-w-md w-full mx-4">
            <div className="p-6 border-b border-gray-200">
              <h3 className="text-xl font-semibold text-gray-900">
                Invite Team Member
              </h3>
              <p className="text-sm text-gray-600 mt-1">
                Send an invitation to join your team
              </p>
            </div>

            <div className="p-6 space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Email Address
                </label>
                <div className="relative">
                  <Mail className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-gray-400" />
                  <input
                    type="email"
                    value={inviteEmail}
                    onChange={(e) => setInviteEmail(e.target.value)}
                    placeholder="member@example.com"
                    className="w-full pl-10 pr-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Role
                </label>
                <select
                  value={inviteRole}
                  onChange={(e) => setInviteRole(e.target.value as Role)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                >
                  {Object.entries(roleLabels).map(([role, label]) => (
                    <option key={role} value={role}>
                      {label} - {roleDescriptions[role as Role]}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className="p-6 border-t border-gray-200 flex justify-end gap-3">
              <button
                onClick={() => setShowInviteModal(false)}
                className="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={handleInviteMember}
                disabled={createInvitationMutation.isPending || !inviteEmail}
                className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50 flex items-center gap-2"
              >
                {createInvitationMutation.isPending && (
                  <Loader2 className="h-4 w-4 animate-spin" />
                )}
                Send Invitation
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Change Role Modal */}
      {showRoleModal && selectedMember && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-xl max-w-md w-full mx-4">
            <div className="p-6 border-b border-gray-200">
              <h3 className="text-xl font-semibold text-gray-900">
                Change Member Role
              </h3>
              <p className="text-sm text-gray-600 mt-1">
                Update role for {selectedMember.name}
              </p>
            </div>

            <div className="p-6">
              <label className="block text-sm font-medium text-gray-700 mb-1">
                New Role
              </label>
              <select
                value={newRole}
                onChange={(e) => setNewRole(e.target.value as Role)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              >
                {Object.entries(roleLabels).map(([role, label]) => (
                  <option key={role} value={role}>
                    {label} - {roleDescriptions[role as Role]}
                  </option>
                ))}
              </select>
            </div>

            <div className="p-6 border-t border-gray-200 flex justify-end gap-3">
              <button
                onClick={() => {
                  setShowRoleModal(false);
                  setSelectedMemberId(null);
                }}
                className="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={handleUpdateRole}
                disabled={updateRoleMutation.isPending}
                className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50 flex items-center gap-2"
              >
                {updateRoleMutation.isPending && (
                  <Loader2 className="h-4 w-4 animate-spin" />
                )}
                Update Role
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Transfer Ownership Modal */}
      {showTransferModal && selectedMember && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-xl max-w-md w-full mx-4">
            <div className="p-6 border-b border-gray-200">
              <h3 className="text-xl font-semibold text-gray-900">
                Transfer Ownership
              </h3>
            </div>

            <div className="p-6">
              <p className="text-gray-700">
                Are you sure you want to transfer team ownership to{' '}
                <span className="font-semibold">{selectedMember.name}</span>?
                This action cannot be undone by you. The new owner will have
                full control of the team.
              </p>
            </div>

            <div className="p-6 border-t border-gray-200 flex justify-end gap-3">
              <button
                onClick={() => {
                  setShowTransferModal(false);
                  setSelectedMemberId(null);
                }}
                className="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={handleTransferOwnership}
                disabled={transferOwnershipMutation.isPending}
                className="px-4 py-2 bg-yellow-600 text-white rounded-lg hover:bg-yellow-700 transition-colors disabled:opacity-50 flex items-center gap-2"
              >
                {transferOwnershipMutation.isPending && (
                  <Loader2 className="h-4 w-4 animate-spin" />
                )}
                Transfer Ownership
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Remove Member Modal */}
      {showRemoveModal && selectedMember && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-xl max-w-md w-full mx-4">
            <div className="p-6 border-b border-gray-200">
              <h3 className="text-xl font-semibold text-gray-900">
                Remove Team Member
              </h3>
            </div>

            <div className="p-6">
              <p className="text-gray-700">
                Are you sure you want to remove{' '}
                <span className="font-semibold">{selectedMember.name}</span>{' '}
                from your team? They will lose access to all team data.
              </p>
            </div>

            <div className="p-6 border-t border-gray-200 flex justify-end gap-3">
              <button
                onClick={() => {
                  setShowRemoveModal(false);
                  setSelectedMemberId(null);
                }}
                className="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={handleRemoveMember}
                disabled={removeMemberMutation.isPending}
                className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors disabled:opacity-50 flex items-center gap-2"
              >
                {removeMemberMutation.isPending && (
                  <Loader2 className="h-4 w-4 animate-spin" />
                )}
                Remove Member
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
