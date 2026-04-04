import { useTeam } from '../context/TeamContext';
import { useTeamSettings } from './useTeamHooks';

/**
 * Hook that provides team-level default values for currency and country.
 * Returns undefined for each if no team setting has been configured,
 * allowing forms to show "no default" state.
 */
export const useTeamDefaults = () => {
  const { activeTeam } = useTeam();
  const { data: settings } = useTeamSettings(activeTeam?.identifier);

  return {
    defaultCurrency: settings?.regional?.defaultCurrency || 'EUR',
    defaultCountryCode: settings?.regional?.defaultCountryCode || undefined,
    defaultDateFormat: settings?.regional?.dateFormat || undefined,
    isLoading: !settings,
    activeTeamId: activeTeam?.identifier,
  };
};
