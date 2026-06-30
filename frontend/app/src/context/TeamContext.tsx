import { createContext, useContext, ReactNode, useCallback } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  getUserTeams,
  switchTeam as switchTeamApi,
  setDefaultTeam as setDefaultTeamApi,
} from '../generated/api/users/users';
import type { UserTeamResponse } from '../types/users';
import { useAuth } from './AuthContext';
import { useImpersonation } from './ImpersonationContext';
import { trackEvent } from '../utils/analytics';
import { AnalyticsEvent } from '../constants/analyticsEvents';

interface Team {
  identifier: string;
  name: string;
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
  identifier: apiTeam.identifier,
  name: apiTeam.teamName,
  role: apiTeam.role as Team['role'],
  isOwner: apiTeam.isOwner,
  isDefault: apiTeam.isDefault,
  isActive: apiTeam.isActive,
  memberCount: apiTeam.memberCount,
});

export const TeamProvider = ({ children }: { children: ReactNode }) => {
  const queryClient = useQueryClient();
  const { isAuthenticated, token } = useAuth();
  const { active: isImpersonating } = useImpersonation();

  const {
    data: teamsData,
    isLoading,
    refetch,
  } = useQuery({
    queryKey: ['user-teams'],
    queryFn: getUserTeams,
    staleTime: 30 * 1000, // 30 seconds
    enabled: (isAuthenticated && !!token) || isImpersonating,
  });

  const teams: Team[] = teamsData?.map(mapApiTeamToTeam) ?? [];
  const activeTeam = teams.find((t) => t.isActive) ?? teams[0] ?? null;
  const defaultTeamId = teams.find((t) => t.isDefault)?.identifier ?? null;

  const switchTeamMutation = useMutation({
    mutationFn: (teamIdentifier: string) => switchTeamApi({ teamIdentifier }),
    onSuccess: () => {
      trackEvent(AnalyticsEvent.TEAM_SWITCHED);
      // Clear all cached data and navigate to a known good state
      queryClient.clear();
      window.location.href = '/dashboard';
    },
  });

  const setDefaultTeamMutation = useMutation({
    mutationFn: (teamIdentifier: string) =>
      setDefaultTeamApi({ teamIdentifier }),
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
