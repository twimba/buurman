import { useState, useRef, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { createPortal } from 'react-dom';
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
  const { t } = useTranslation('common');
  const { teams, activeTeam, defaultTeamId, switchTeam, setAsDefaultTeam } =
    useTeam();
  const [isOpen, setIsOpen] = useState(false);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const [menuPos, setMenuPos] = useState({ top: 0, left: 0 });

  useEffect(() => {
    if (isOpen && triggerRef.current) {
      const rect = triggerRef.current.getBoundingClientRect();
      setMenuPos({
        top: rect.bottom - 72 * Math.min(teams.length, 4),
        left: rect.right + 8,
      });
    }
  }, [isOpen, teams.length]);

  const getRoleIcon = (role: string) => {
    switch (role) {
      case 'TEAM_ADMIN':
        return <Shield className="h-4 w-4 text-info-text" />;
      case 'TEAM_EDITOR':
        return (
          <Edit3 className="h-4 w-4 text-primary-500 dark:text-primary-300" />
        );
      case 'TEAM_VIEWER':
        return <Eye className="h-4 w-4 text-text-secondary " />;
      default:
        return null;
    }
  };

  const getRoleLabel = (role: string) => {
    switch (role) {
      case 'TEAM_ADMIN':
        return t('teamSwitcher.roles.admin');
      case 'TEAM_EDITOR':
        return t('teamSwitcher.roles.editor');
      case 'TEAM_VIEWER':
        return t('teamSwitcher.roles.viewer');
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
        ref={triggerRef}
        onClick={() => setIsOpen(!isOpen)}
        className="w-full flex items-center gap-2 px-3 py-2.5 bg-gradient-to-br from-surface-inset to-white dark:from-surface-card dark:to-surface-page border border-border-default rounded-lg hover:border-border-strong hover:shadow-md transition-all"
      >
        <div className="flex-shrink-0 h-8 w-8 rounded-lg bg-gradient-to-br from-primary-500 to-primary-600 flex items-center justify-center">
          <Building className="h-5 w-5 text-white" />
        </div>
        <div className="flex-1 text-left min-w-0">
          <div className="flex items-center gap-1.5">
            <p className="text-sm font-semibold text-text-primary truncate">
              {activeTeam?.name}
            </p>
            {activeTeam?.isOwner && (
              <Crown className="h-3 w-3 text-accent-500 flex-shrink-0" />
            )}
            {activeTeam?.identifier === defaultTeamId && (
              <Star className="h-3 w-3 text-primary-500 flex-shrink-0" />
            )}
          </div>
          <div className="flex items-center gap-1.5 mt-0.5">
            {activeTeam && getRoleIcon(activeTeam.role)}
            <p className="text-xs text-text-secondary">
              {activeTeam && getRoleLabel(activeTeam.role)}
            </p>
          </div>
        </div>
        <ChevronDown
          className={`h-4 w-4 text-text-muted transition-transform ${
            isOpen ? 'transform rotate-180' : ''
          }`}
        />
      </button>

      {/* Dropdown via portal to escape sidebar overflow:hidden */}
      {isOpen &&
        createPortal(
          <>
            {/* Backdrop */}
            <div
              className="fixed inset-0 z-[100]"
              onClick={() => setIsOpen(false)}
            />

            {/* Menu */}
            <div
              className="fixed w-72 bg-surface-card border border-border-default rounded-lg shadow-lg z-[101] overflow-hidden"
              style={{ top: menuPos.top, left: menuPos.left }}
            >
              <div className="p-2 border-b border-border-default bg-surface-page">
                <p className="text-xs font-semibold text-text-secondary uppercase tracking-wide px-3 py-1">
                  {t('teamSwitcher.yourTeams', { count: teams.length })}
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
                      className={`p-3 hover:bg-surface-inset cursor-pointer transition-colors border-b border-border-default last:border-b-0 ${
                        isActive ? 'bg-primary-50 dark:bg-primary-500/10' : ''
                      }`}
                    >
                      <div className="flex items-start gap-3">
                        <div className="flex-shrink-0 h-10 w-10 rounded-lg bg-gradient-to-br from-primary-500 to-primary-600 flex items-center justify-center">
                          <Building className="h-5 w-5 text-white" />
                        </div>

                        <div className="flex-1 min-w-0">
                          <div className="flex items-center gap-2 mb-1">
                            <p className="text-sm font-semibold text-text-primary truncate">
                              {team.name}
                            </p>
                            {team.isOwner && (
                              <Crown className="h-3.5 w-3.5 text-accent-500 flex-shrink-0" />
                            )}
                            {isDefault && (
                              <Star className="h-3.5 w-3.5 text-primary-500 flex-shrink-0" />
                            )}
                          </div>

                          <div className="flex items-center gap-3 text-xs text-text-secondary">
                            <div className="flex items-center gap-1">
                              {getRoleIcon(team.role)}
                              <span>{getRoleLabel(team.role)}</span>
                            </div>
                            <div className="flex items-center gap-1">
                              <Users className="h-3 w-3" />
                              <span>
                                {t('teamSwitcher.members', {
                                  count: team.memberCount,
                                })}
                              </span>
                            </div>
                          </div>

                          {!isDefault && (
                            <button
                              onClick={(e) =>
                                handleSetDefault(e, team.identifier)
                              }
                              className="mt-2 text-xs text-primary-500 dark:text-primary-300 hover:text-primary-600 dark:hover:text-primary-200 font-medium"
                            >
                              {t('teamSwitcher.setAsDefault')}
                            </button>
                          )}
                        </div>

                        {isActive && (
                          <Check className="h-5 w-5 text-primary-500 dark:text-primary-300 flex-shrink-0" />
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>

              <div className="p-3 border-t border-border-default bg-surface-page">
                <div className="flex items-start gap-2 text-xs text-text-secondary">
                  <div className="flex-shrink-0 mt-0.5">
                    <Star className="h-3.5 w-3.5 text-primary-500" />
                  </div>
                  <p>
                    <strong>{t('teamSwitcher.defaultTeam')}</strong>{' '}
                    {t('teamSwitcher.defaultTeamDescription')}
                  </p>
                </div>
              </div>
            </div>
          </>,
          document.body
        )}
    </div>
  );
};

TeamSwitcher.displayName = 'TeamSwitcher';
