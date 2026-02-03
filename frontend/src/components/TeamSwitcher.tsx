import { useState } from 'react';
import { useTeam } from '@/context/TeamContext';
import {
  Building,
  Check,
  ChevronDown,
  Crown,
  Users,
  Star,
  Shield,
  Eye,
  Edit3,
} from 'lucide-react';

export const TeamSwitcher = () => {
  const {
    teams,
    activeTeam,
    defaultTeamId,
    switchTeam,
    setAsDefaultTeam,
  } = useTeam();
  const [isOpen, setIsOpen] = useState(false);

  const getRoleIcon = (role: string) => {
    switch (role) {
      case 'TEAM_ADMIN':
        return <Shield className="h-4 w-4 text-purple-600" />;
      case 'TEAM_EDITOR':
        return <Edit3 className="h-4 w-4 text-blue-600" />;
      case 'TEAM_VIEWER':
        return <Eye className="h-4 w-4 text-gray-600" />;
      default:
        return null;
    }
  };

  const getRoleLabel = (role: string) => {
    switch (role) {
      case 'TEAM_ADMIN':
        return 'Admin';
      case 'TEAM_EDITOR':
        return 'Editor';
      case 'TEAM_VIEWER':
        return 'Viewer';
      default:
        return role;
    }
  };

  const handleTeamSwitch = (teamId: string) => {
    switchTeam(teamId);
    setIsOpen(false);
  };

  const handleSetDefault = (e: React.MouseEvent, teamId: string) => {
    e.stopPropagation();
    setAsDefaultTeam(teamId);
  };

  return (
    <div className="relative w-full">
      {/* Trigger Button */}
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="w-full flex items-center gap-2 px-3 py-2.5 bg-gradient-to-br from-gray-50 to-white border border-gray-200 rounded-lg hover:border-gray-300 hover:shadow-md transition-all"
      >
        <div className="flex-shrink-0 h-8 w-8 rounded-lg bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center">
          <Building className="h-5 w-5 text-white" />
        </div>
        <div className="flex-1 text-left min-w-0">
          <div className="flex items-center gap-1.5">
            <p className="text-sm font-semibold text-gray-900 truncate">
              {activeTeam?.name}
            </p>
            {activeTeam?.isOwner && (
              <Crown className="h-3 w-3 text-yellow-500 flex-shrink-0" />
            )}
            {activeTeam?.id === defaultTeamId && (
              <Star className="h-3 w-3 text-blue-500 flex-shrink-0" />
            )}
          </div>
          <div className="flex items-center gap-1.5 mt-0.5">
            {activeTeam && getRoleIcon(activeTeam.role)}
            <p className="text-xs text-gray-600">
              {activeTeam && getRoleLabel(activeTeam.role)}
            </p>
          </div>
        </div>
        <ChevronDown
          className={`h-4 w-4 text-gray-400 transition-transform ${
            isOpen ? 'transform rotate-180' : ''
          }`}
        />
      </button>

      {/* Dropdown Menu */}
      {isOpen && (
        <>
          {/* Backdrop */}
          <div
            className="fixed inset-0 z-10"
            onClick={() => setIsOpen(false)}
          />

          {/* Menu */}
          <div className="absolute top-full left-0 right-0 mt-2 bg-white border border-gray-200 rounded-lg shadow-lg z-20 overflow-hidden">
            <div className="p-2 border-b border-gray-100 bg-gray-50">
              <p className="text-xs font-semibold text-gray-600 uppercase tracking-wide px-3 py-1">
                Your Teams ({teams.length})
              </p>
            </div>

            <div className="max-h-[400px] overflow-y-auto">
              {teams.map((team) => {
                const isActive = team.id === activeTeam?.id;
                const isDefault = team.id === defaultTeamId;

                return (
                  <div
                    key={team.id}
                    onClick={() => handleTeamSwitch(team.id)}
                    className={`p-3 hover:bg-gray-50 cursor-pointer transition-colors border-b border-gray-100 last:border-b-0 ${
                      isActive ? 'bg-blue-50' : ''
                    }`}
                  >
                    <div className="flex items-start gap-3">
                      <div className="flex-shrink-0 h-10 w-10 rounded-lg bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center">
                        <Building className="h-5 w-5 text-white" />
                      </div>

                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2 mb-1">
                          <p className="text-sm font-semibold text-gray-900 truncate">
                            {team.name}
                          </p>
                          {team.isOwner && (
                            <Crown className="h-3.5 w-3.5 text-yellow-500 flex-shrink-0" />
                          )}
                          {isDefault && (
                            <Star className="h-3.5 w-3.5 text-blue-500 flex-shrink-0" />
                          )}
                        </div>

                        <div className="flex items-center gap-3 text-xs text-gray-600">
                          <div className="flex items-center gap-1">
                            {getRoleIcon(team.role)}
                            <span>{getRoleLabel(team.role)}</span>
                          </div>
                          <div className="flex items-center gap-1">
                            <Users className="h-3 w-3" />
                            <span>{team.memberCount} members</span>
                          </div>
                        </div>

                        {!isDefault && (
                          <button
                            onClick={(e) => handleSetDefault(e, team.id)}
                            className="mt-2 text-xs text-blue-600 hover:text-blue-700 font-medium"
                          >
                            Set as default
                          </button>
                        )}
                      </div>

                      {isActive && (
                        <Check className="h-5 w-5 text-blue-600 flex-shrink-0" />
                      )}
                    </div>
                  </div>
                );
              })}
            </div>

            <div className="p-3 border-t border-gray-200 bg-gray-50">
              <div className="flex items-start gap-2 text-xs text-gray-600">
                <div className="flex-shrink-0 mt-0.5">
                  <Star className="h-3.5 w-3.5 text-blue-500" />
                </div>
                <p>
                  <strong>Default team</strong> is used when you log in and for
                  email notifications
                </p>
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
};
