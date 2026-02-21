import { useTeam } from '../context/TeamContext';
import { useCurrentTeam, useTeamSettings } from './useTeamHooks';

/**
 * Hook that provides team-level default values for currency and country.
 * Returns undefined for each if no team setting has been configured,
 * allowing forms to show "no default" state.
 */
export const useTeamDefaults = () => {
  const { activeTeam } = useTeam();
  const { data: team } = useCurrentTeam();
  const { data: settings } = useTeamSettings(team?.identifier);

  return {
    defaultCurrency: settings?.regional?.defaultCurrency || undefined,
    defaultCountry: settings?.regional?.defaultCountry || undefined,
    defaultDateFormat: settings?.regional?.dateFormat || undefined,
    isLoading: !settings,
    activeTeamId: activeTeam?.identifier,
  };
};
