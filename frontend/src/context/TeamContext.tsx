import { createContext, useContext, useState, useEffect, ReactNode } from 'react';

interface Team {
  id: string;
  name: string;
  role: 'TEAM_ADMIN' | 'TEAM_EDITOR' | 'TEAM_VIEWER';
  isOwner: boolean;
  memberCount: number;
}

interface TeamContextType {
  teams: Team[];
  activeTeam: Team | null;
  defaultTeamId: string | null;
  switchTeam: (teamId: string) => void;
  setAsDefaultTeam: (teamId: string) => void;
  canEditTeamSettings: boolean;
  canManageMembers: boolean;
}

const TeamContext = createContext<TeamContextType | undefined>(undefined);

export const TeamProvider = ({ children }: { children: ReactNode }) => {
  // Mock data - will be replaced with API calls
  const [teams] = useState<Team[]>([
    {
      id: '1',
      name: 'My Property Empire',
      role: 'TEAM_ADMIN',
      isOwner: true,
      memberCount: 1,
    },
    {
      id: '2',
      name: 'Downtown Rentals Co.',
      role: 'TEAM_EDITOR',
      isOwner: false,
      memberCount: 5,
    },
    {
      id: '3',
      name: 'Beach Properties LLC',
      role: 'TEAM_VIEWER',
      isOwner: false,
      memberCount: 12,
    },
  ]);

  const [defaultTeamId, setDefaultTeamId] = useState<string>('1');
  const [activeTeamId, setActiveTeamId] = useState<string>('1');

  const activeTeam = teams.find((t) => t.id === activeTeamId) || teams[0];

  const switchTeam = (teamId: string) => {
    setActiveTeamId(teamId);
    // TODO: API call to switch team context
    // This should update the JWT or session with the new team context
    localStorage.setItem('activeTeamId', teamId);
  };

  const setAsDefaultTeam = (teamId: string) => {
    setDefaultTeamId(teamId);
    // TODO: API call to update user's default team
  };

  // Load active team from localStorage on mount
  useEffect(() => {
    const savedTeamId = localStorage.getItem('activeTeamId');
    if (savedTeamId && teams.some((t) => t.id === savedTeamId)) {
      setActiveTeamId(savedTeamId);
    }
  }, [teams]);

  const canEditTeamSettings = activeTeam?.role === 'TEAM_ADMIN';
  const canManageMembers = activeTeam?.role === 'TEAM_ADMIN';

  return (
    <TeamContext.Provider
      value={{
        teams,
        activeTeam,
        defaultTeamId,
        switchTeam,
        setAsDefaultTeam,
        canEditTeamSettings,
        canManageMembers,
      }}
    >
      {children}
    </TeamContext.Provider>
  );
};

export const useTeam = () => {
  const context = useContext(TeamContext);
  if (context === undefined) {
    throw new Error('useTeam must be used within a TeamProvider');
  }
  return context;
};
