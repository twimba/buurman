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
} from 'lucide-react';

interface TeamMember {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  role: 'TEAM_ADMIN' | 'TEAM_EDITOR' | 'TEAM_VIEWER';
  avatarUrl?: string;
  joinedAt: string;
  isCurrentUser?: boolean;
}

export const TeamSettingsSection = () => {
  const [isEditingTeamName, setIsEditingTeamName] = useState(false);
  const [showInviteModal, setShowInviteModal] = useState(false);
  const [showRemoveModal, setShowRemoveModal] = useState(false);
  const [selectedMember, setSelectedMember] = useState<TeamMember | null>(null);

  // Mock data - will be replaced with actual team data
  const [teamName, setTeamName] = useState('My Property Management Team');
  const [inviteEmail, setInviteEmail] = useState('');
  const [inviteRole, setInviteRole] = useState<TeamMember['role']>('TEAM_VIEWER');

  const [teamMembers] = useState<TeamMember[]>([
    {
      id: '1',
      firstName: 'John',
      lastName: 'Doe',
      email: 'john.doe@example.com',
      role: 'TEAM_ADMIN',
      joinedAt: '2024-01-15',
      isCurrentUser: true,
    },
    {
      id: '2',
      firstName: 'Jane',
      lastName: 'Smith',
      email: 'jane.smith@example.com',
      role: 'TEAM_EDITOR',
      joinedAt: '2024-02-20',
    },
    {
      id: '3',
      firstName: 'Bob',
      lastName: 'Johnson',
      email: 'bob.johnson@example.com',
      role: 'TEAM_VIEWER',
      joinedAt: '2024-03-10',
    },
  ]);

  const roleLabels = {
    TEAM_ADMIN: 'Admin',
    TEAM_EDITOR: 'Editor',
    TEAM_VIEWER: 'Viewer',
  };

  const roleDescriptions = {
    TEAM_ADMIN: 'Full access to all features and settings',
    TEAM_EDITOR: 'Can create and edit properties, tenants, and contracts',
    TEAM_VIEWER: 'Read-only access to all data',
  };

  const roleColors = {
    TEAM_ADMIN: 'bg-purple-100 text-purple-800',
    TEAM_EDITOR: 'bg-blue-100 text-blue-800',
    TEAM_VIEWER: 'bg-gray-100 text-gray-800',
  };

  const handleSaveTeamName = () => {
    // TODO: API call to save team name
    setIsEditingTeamName(false);
  };

  const handleInviteMember = () => {
    // TODO: API call to invite member
    setShowInviteModal(false);
    setInviteEmail('');
    setInviteRole('TEAM_VIEWER');
  };

  const handleRemoveMember = () => {
    // TODO: API call to remove member
    setShowRemoveModal(false);
    setSelectedMember(null);
  };

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
                <p className="text-lg font-semibold text-gray-900">{teamName}</p>
              )}
            </div>
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
                  className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2"
                >
                  <Save className="h-4 w-4" />
                  Save
                </button>
              </div>
            ) : (
              <button
                onClick={() => setIsEditingTeamName(true)}
                className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2"
              >
                <Edit3 className="h-4 w-4" />
                Edit
              </button>
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
                {teamMembers.length} member{teamMembers.length !== 1 ? 's' : ''}
              </p>
            </div>
            <button
              onClick={() => setShowInviteModal(true)}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2"
            >
              <UserPlus className="h-4 w-4" />
              Invite Member
            </button>
          </div>
        </div>

        <div className="divide-y divide-gray-200">
          {teamMembers.map((member) => (
            <div key={member.id} className="p-6 hover:bg-gray-50">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-4">
                  <div className="h-12 w-12 rounded-full bg-blue-100 flex items-center justify-center">
                    <Users className="h-6 w-6 text-blue-600" />
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <p className="font-semibold text-gray-900">
                        {member.firstName} {member.lastName}
                      </p>
                      {member.isCurrentUser && (
                        <span className="px-2 py-0.5 bg-green-100 text-green-800 text-xs font-semibold rounded">
                          You
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
                      className={`px-3 py-1 text-xs font-semibold rounded ${roleColors[member.role]}`}
                    >
                      {roleLabels[member.role]}
                    </span>
                    <p className="text-xs text-gray-600 mt-1">
                      {roleDescriptions[member.role]}
                    </p>
                  </div>
                  {!member.isCurrentUser && (
                    <button
                      onClick={() => {
                        setSelectedMember(member);
                        setShowRemoveModal(true);
                      }}
                      className="p-2 text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
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
                  {roleDescriptions[role as TeamMember['role']]}
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
                  onChange={(e) =>
                    setInviteRole(e.target.value as TeamMember['role'])
                  }
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                >
                  {Object.entries(roleLabels).map(([role, label]) => (
                    <option key={role} value={role}>
                      {label} - {roleDescriptions[role as TeamMember['role']]}
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
                className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
              >
                Send Invitation
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
                <span className="font-semibold">
                  {selectedMember.firstName} {selectedMember.lastName}
                </span>{' '}
                from your team? They will lose access to all team data.
              </p>
            </div>

            <div className="p-6 border-t border-gray-200 flex justify-end gap-3">
              <button
                onClick={() => {
                  setShowRemoveModal(false);
                  setSelectedMember(null);
                }}
                className="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={handleRemoveMember}
                className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors"
              >
                Remove Member
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
