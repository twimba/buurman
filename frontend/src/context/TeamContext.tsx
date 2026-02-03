import { createContext, useContext, ReactNode, useCallback } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  getUserTeams,
  switchTeam as switchTeamApi,
  setDefaultTeam as setDefaultTeamApi,
  UserTeamResponse,
} from '../api/users';
import { useAuth } from '../contexts/AuthContext';

interface Team {
  id: string;
  name: string;
  identifier: string;
  role: 'TEAM_ADMIN' | 'TEAM_EDITOR' | 'TEAM_VIEWER';
  isOwner: boolean;
  isDefault: boolean;
  isActive: boolean;
  memberCount: number;
}

interface TeamContextType {
  teams: Team[];
  activeTeam: Team | null;
  defaultTeamId: string | null;
  isLoading: boolean;
  switchTeam: (teamId: string) => void;
  setAsDefaultTeam: (teamId: string) => void;
  canEditTeamSettings: boolean;
  canManageMembers: boolean;
  canEditData: boolean;
  refetchTeams: () => void;
}

const TeamContext = createContext<TeamContextType | undefined>(undefined);

const mapApiTeamToTeam = (apiTeam: UserTeamResponse): Team => ({
  id: apiTeam.teamId,
  name: apiTeam.teamName,
  identifier: apiTeam.identifier,
  role: apiTeam.role,
  isOwner: apiTeam.isOwner,
  isDefault: apiTeam.isDefault,
  isActive: apiTeam.isActive,
  memberCount: apiTeam.memberCount,
});

export const TeamProvider = ({ children }: { children: ReactNode }) => {
  const queryClient = useQueryClient();
  const { isAuthenticated } = useAuth();

  const {
    data: teamsData,
    isLoading,
    refetch,
  } = useQuery({
    queryKey: ['user-teams'],
    queryFn: getUserTeams,
    staleTime: 5 * 60 * 1000, // 5 minutes
    enabled: isAuthenticated, // Only fetch when authenticated
  });

  const teams: Team[] = teamsData?.map(mapApiTeamToTeam) ?? [];
  const activeTeam = teams.find((t) => t.isActive) ?? teams[0] ?? null;
  const defaultTeamId = teams.find((t) => t.isDefault)?.id ?? null;

  const switchTeamMutation = useMutation({
    mutationFn: switchTeamApi,
    onSuccess: () => {
      // Invalidate all team-dependent queries
      queryClient.invalidateQueries({ queryKey: ['user-teams'] });
      queryClient.invalidateQueries({ queryKey: ['properties'] });
      queryClient.invalidateQueries({ queryKey: ['tenants'] });
      queryClient.invalidateQueries({ queryKey: ['contracts'] });
      queryClient.invalidateQueries({ queryKey: ['payments'] });
      queryClient.invalidateQueries({ queryKey: ['expenses'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      // Refresh the page to ensure all data is for the new team
      window.location.reload();
    },
  });

  const setDefaultTeamMutation = useMutation({
    mutationFn: setDefaultTeamApi,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['user-teams'] });
    },
  });

  const switchTeam = useCallback(
    (teamId: string) => {
      switchTeamMutation.mutate(teamId);
    },
    [switchTeamMutation]
  );

  const setAsDefaultTeam = useCallback(
    (teamId: string) => {
      setDefaultTeamMutation.mutate(teamId);
    },
    [setDefaultTeamMutation]
  );

  const refetchTeams = useCallback(() => {
    refetch();
  }, [refetch]);

  const canEditTeamSettings = activeTeam?.role === 'TEAM_ADMIN';
  const canManageMembers = activeTeam?.role === 'TEAM_ADMIN';
  const canEditData =
    activeTeam?.role === 'TEAM_ADMIN' || activeTeam?.role === 'TEAM_EDITOR';

  return (
    <TeamContext.Provider
      value={{
        teams,
        activeTeam,
        defaultTeamId,
        isLoading,
        switchTeam,
        setAsDefaultTeam,
        canEditTeamSettings,
        canManageMembers,
        canEditData,
        refetchTeams,
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
