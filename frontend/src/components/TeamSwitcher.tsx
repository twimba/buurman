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
  const { teams, activeTeam, defaultTeamId, switchTeam, setAsDefaultTeam } =
    useTeam();
  const [isOpen, setIsOpen] = useState(false);

  const getRoleIcon = (role: string) => {
    switch (role) {
      case 'TEAM_ADMIN':
        return <Shield className="h-4 w-4 text-purple-600" />;
      case 'TEAM_EDITOR':
        return <Edit3 className="h-4 w-4 text-blue-600 dark:text-blue-400" />;
      case 'TEAM_VIEWER':
        return <Eye className="h-4 w-4 text-gray-600 dark:text-gray-400" />;
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
        className="w-full flex items-center gap-2 px-3 py-2.5 bg-gradient-to-br from-gray-50 to-white dark:from-gray-800 dark:to-gray-900 border border-gray-200 dark:border-gray-700 rounded-lg hover:border-gray-300 dark:hover:border-gray-600 hover:shadow-md dark:hover:shadow-gray-900 transition-all"
      >
        <div className="flex-shrink-0 h-8 w-8 rounded-lg bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center">
          <Building className="h-5 w-5 text-white" />
        </div>
        <div className="flex-1 text-left min-w-0">
          <div className="flex items-center gap-1.5">
            <p className="text-sm font-semibold text-gray-900 dark:text-gray-100 truncate">
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
            <p className="text-xs text-gray-600 dark:text-gray-400">
              {activeTeam && getRoleLabel(activeTeam.role)}
            </p>
          </div>
        </div>
        <ChevronDown
          className={`h-4 w-4 text-gray-400 dark:text-gray-500 transition-transform ${
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

          {/* Menu - opens to the right, expanding upward */}
          <div className="absolute left-full bottom-0 ml-2 w-72 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg shadow-lg dark:shadow-gray-900 z-20 overflow-hidden">
            <div className="p-2 border-b border-gray-100 dark:border-gray-700 bg-gray-50 dark:bg-gray-800">
              <p className="text-xs font-semibold text-gray-600 dark:text-gray-400 uppercase tracking-wide px-3 py-1">
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
                    className={`p-3 hover:bg-gray-50 dark:hover:bg-gray-700 cursor-pointer transition-colors border-b border-gray-100 dark:border-gray-700 last:border-b-0 ${
                      isActive ? 'bg-blue-50 dark:bg-blue-900/30' : ''
                    }`}
                  >
                    <div className="flex items-start gap-3">
                      <div className="flex-shrink-0 h-10 w-10 rounded-lg bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center">
                        <Building className="h-5 w-5 text-white" />
                      </div>

                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2 mb-1">
                          <p className="text-sm font-semibold text-gray-900 dark:text-gray-100 truncate">
                            {team.name}
                          </p>
                          {team.isOwner && (
                            <Crown className="h-3.5 w-3.5 text-yellow-500 flex-shrink-0" />
                          )}
                          {isDefault && (
                            <Star className="h-3.5 w-3.5 text-blue-500 flex-shrink-0" />
                          )}
                        </div>

                        <div className="flex items-center gap-3 text-xs text-gray-600 dark:text-gray-400">
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
                            className="mt-2 text-xs text-blue-600 dark:text-blue-400 hover:text-blue-700 dark:hover:text-blue-300 font-medium"
                          >
                            Set as default
                          </button>
                        )}
                      </div>

                      {isActive && (
                        <Check className="h-5 w-5 text-blue-600 dark:text-blue-400 flex-shrink-0" />
                      )}
                    </div>
                  </div>
                );
              })}
            </div>

            <div className="p-3 border-t border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800">
              <div className="flex items-start gap-2 text-xs text-gray-600 dark:text-gray-400">
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
