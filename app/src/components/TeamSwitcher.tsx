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
        return (
          <Shield className="h-4 w-4 text-purple-600 dark:text-purple-400" />
        );
      case 'TEAM_EDITOR':
        return <Edit3 className="h-4 w-4 text-[#5c7cfa] dark:text-[#91a7ff]" />;
      case 'TEAM_VIEWER':
        return <Eye className="h-4 w-4 text-[#6b7194] dark:text-[#8b90a8]" />;
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
        className="w-full flex items-center gap-2 px-3 py-2.5 bg-gradient-to-br from-[#f1f3f9] to-white dark:from-[#1a1d28] dark:to-[#14161f] border border-[#e2e6f0] dark:border-[#2a2e3f] rounded-lg hover:border-[#c9cfd9] dark:hover:border-[#3a3f54] hover:shadow-md transition-all"
      >
        <div className="flex-shrink-0 h-8 w-8 rounded-lg bg-gradient-to-br from-[#5c7cfa] to-[#4263eb] flex items-center justify-center">
          <Building className="h-5 w-5 text-white" />
        </div>
        <div className="flex-1 text-left min-w-0">
          <div className="flex items-center gap-1.5">
            <p className="text-sm font-semibold text-[#1a1d2e] dark:text-[#eef0f6] truncate">
              {activeTeam?.name}
            </p>
            {activeTeam?.isOwner && (
              <Crown className="h-3 w-3 text-accent-500 flex-shrink-0" />
            )}
            {activeTeam?.identifier === defaultTeamId && (
              <Star className="h-3 w-3 text-[#5c7cfa] flex-shrink-0" />
            )}
          </div>
          <div className="flex items-center gap-1.5 mt-0.5">
            {activeTeam && getRoleIcon(activeTeam.role)}
            <p className="text-xs text-[#6b7194] dark:text-[#8b90a8]">
              {activeTeam && getRoleLabel(activeTeam.role)}
            </p>
          </div>
        </div>
        <ChevronDown
          className={`h-4 w-4 text-[#9ca0b8] dark:text-[#5c6180] transition-transform ${
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
          <div className="absolute left-full bottom-0 ml-2 w-72 bg-white dark:bg-[#14161f] border border-[#e2e6f0] dark:border-[#2a2e3f] rounded-xl shadow-lg z-20 overflow-hidden">
            <div className="p-2 border-b border-[#edf0f7] dark:border-[#2a2e3f] bg-[#f8f9fc] dark:bg-[#1a1d28]">
              <p className="text-xs font-semibold text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wide px-3 py-1">
                Your Teams ({teams.length})
              </p>
            </div>

            <div className="max-h-[400px] overflow-y-auto">
              {teams.map((team) => {
                const isActive = team.identifier === activeTeam?.identifier;
                const isDefault = team.identifier === defaultTeamId;

                return (
                  <div
                    key={team.identifier}
                    onClick={() => handleTeamSwitch(team.identifier)}
                    className={`p-3 hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] cursor-pointer transition-colors border-b border-[#edf0f7] dark:border-[#2a2e3f] last:border-b-0 ${
                      isActive ? 'bg-[#f0f4ff] dark:bg-[#5c7cfa]/10' : ''
                    }`}
                  >
                    <div className="flex items-start gap-3">
                      <div className="flex-shrink-0 h-10 w-10 rounded-lg bg-gradient-to-br from-[#5c7cfa] to-[#4263eb] flex items-center justify-center">
                        <Building className="h-5 w-5 text-white" />
                      </div>

                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2 mb-1">
                          <p className="text-sm font-semibold text-[#1a1d2e] dark:text-[#eef0f6] truncate">
                            {team.name}
                          </p>
                          {team.isOwner && (
                            <Crown className="h-3.5 w-3.5 text-accent-500 flex-shrink-0" />
                          )}
                          {isDefault && (
                            <Star className="h-3.5 w-3.5 text-[#5c7cfa] flex-shrink-0" />
                          )}
                        </div>

                        <div className="flex items-center gap-3 text-xs text-[#6b7194] dark:text-[#8b90a8]">
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
                            onClick={(e) =>
                              handleSetDefault(e, team.identifier)
                            }
                            className="mt-2 text-xs text-[#5c7cfa] dark:text-[#91a7ff] hover:text-[#4263eb] dark:hover:text-[#bac8ff] font-medium"
                          >
                            Set as default
                          </button>
                        )}
                      </div>

                      {isActive && (
                        <Check className="h-5 w-5 text-[#5c7cfa] dark:text-[#91a7ff] flex-shrink-0" />
                      )}
                    </div>
                  </div>
                );
              })}
            </div>

            <div className="p-3 border-t border-[#e2e6f0] dark:border-[#2a2e3f] bg-[#f8f9fc] dark:bg-[#1a1d28]">
              <div className="flex items-start gap-2 text-xs text-[#6b7194] dark:text-[#8b90a8]">
                <div className="flex-shrink-0 mt-0.5">
                  <Star className="h-3.5 w-3.5 text-[#5c7cfa]" />
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
