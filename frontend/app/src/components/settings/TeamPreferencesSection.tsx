import { useState } from 'react';
import {
  DollarSign,
  Calendar,
  Save,
  Loader2,
  MapPin,
  Building,
  ArrowRightLeft,
} from 'lucide-react';
import {
  useCurrentTeam,
  useTeamSettings,
  useUpdateTeamSettings,
  useUpdateTeam,
} from '../../hooks/useTeamHooks';
import { useTeam } from '../../context/TeamContext';
import { useTranslation } from 'react-i18next';
import { CountrySelector } from '../common/CountrySelector';
import { CurrencyChangeModal } from './CurrencyChangeModal';
import { useCurrencies, getCurrencySymbol } from '@/hooks/useCurrencies';

export const TeamPreferencesSection = () => {
  const { t } = useTranslation('settings');
  const { canEditTeamSettings } = useTeam();
  const { data: team } = useCurrentTeam();
  const { data: settingsData, isLoading } = useTeamSettings(team?.identifier);
  const updateSettingsMutation = useUpdateTeamSettings(team?.identifier ?? '');
  const updateTeamMutation = useUpdateTeam(team?.identifier ?? '');

  const [hasChanges, setHasChanges] = useState(false);
  const [teamName, setTeamName] = useState('');
  const [teamNameSynced, setTeamNameSynced] = useState(false);
  const [showCurrencyChange, setShowCurrencyChange] = useState(false);
  const { data: currencies } = useCurrencies();
  const [preferences, setPreferences] = useState({
    defaultCurrency: '',
    defaultCountryCode: '',
    fiscalYearStart: '01',
  });

  // Sync team name from server
  if (team?.teamName && !teamNameSynced) {
    setTeamName(team.teamName);
    setTeamNameSynced(true);
  }

  const [lastSyncedSettings, setLastSyncedSettings] = useState(settingsData);
  if (settingsData?.regional && settingsData !== lastSyncedSettings) {
    setLastSyncedSettings(settingsData);
    setPreferences({
      defaultCurrency: settingsData.regional.defaultCurrency ?? '',
      defaultCountryCode: settingsData.regional.defaultCountryCode ?? '',
      fiscalYearStart: settingsData.regional.fiscalYearStartMonth ?? '01',
    });
  }

  const months = [
    { value: '01', label: 'January' },
    { value: '02', label: 'February' },
    { value: '03', label: 'March' },
    { value: '04', label: 'April' },
    { value: '05', label: 'May' },
    { value: '06', label: 'June' },
    { value: '07', label: 'July' },
    { value: '08', label: 'August' },
    { value: '09', label: 'September' },
    { value: '10', label: 'October' },
    { value: '11', label: 'November' },
    { value: '12', label: 'December' },
  ];

  const handlePreferenceChange = (key: string, value: string) => {
    setPreferences((prev) => ({ ...prev, [key]: value }));
    setHasChanges(true);
  };

  const handleSave = () => {
    const saveSettings = () =>
      updateSettingsMutation.mutateAsync({
        regional: {
          defaultCountryCode: preferences.defaultCountryCode || undefined,
          fiscalYearStartMonth: preferences.fiscalYearStart,
        },
      });

    const nameChanged = teamName.trim() && teamName.trim() !== team?.teamName;
    const saveTeamName = nameChanged
      ? () => updateTeamMutation.mutateAsync({ name: teamName.trim() })
      : () => Promise.resolve();

    Promise.all([saveSettings(), saveTeamName()]).then(() =>
      setHasChanges(false)
    );
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <Loader2 className="h-8 w-8 animate-spin text-primary-500 dark:text-primary-300" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="bg-surface-card rounded-lg shadow-sm border border-border-default">
        <div className="p-6 border-b border-border-default">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-xl font-semibold text-text-primary">
                {t('teamPreferences.title')}
              </h2>
              <p className="text-sm text-text-secondary mt-1">
                {t('teamPreferences.subtitle')}
              </p>
            </div>
            {hasChanges && canEditTeamSettings && (
              <button
                onClick={handleSave}
                disabled={updateSettingsMutation.isPending}
                className="px-4 py-2 bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors flex items-center gap-2 disabled:opacity-50"
              >
                {updateSettingsMutation.isPending ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <Save className="h-4 w-4" />
                )}
                {t('common:buttons.saveChanges')}
              </button>
            )}
          </div>
        </div>

        <div className="p-6 space-y-6">
          {/* Team Name */}
          <div className="space-y-4">
            <div className="flex items-center gap-2">
              <Building className="h-5 w-5 text-text-secondary " />
              <h3 className="text-lg font-semibold text-text-primary">
                {t('teamPreferences.teamIdentity.title')}
              </h3>
            </div>

            <div>
              <label className="block text-sm font-medium text-text-secondary mb-1">
                {t('teamPreferences.teamIdentity.teamName')}
              </label>
              <input
                type="text"
                value={teamName}
                onChange={(e) => {
                  setTeamName(e.target.value);
                  setHasChanges(true);
                }}
                disabled={!canEditTeamSettings}
                className="w-full px-3 py-2 border border-border-strong rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent disabled:bg-surface-inset disabled:cursor-not-allowed bg-surface-card text-text-primary"
                placeholder={t('teamPreferences.teamIdentity.placeholder')}
              />
              <p className="text-xs text-text-secondary mt-1">
                {t('teamPreferences.teamIdentity.description')}
              </p>
            </div>
          </div>

          {/* Currency Settings */}
          <div className="space-y-4 pt-6 border-t border-border-default">
            <div className="flex items-center gap-2">
              <DollarSign className="h-5 w-5 text-text-secondary " />
              <h3 className="text-lg font-semibold text-text-primary">
                {t('teamPreferences.currency.title')}
              </h3>
            </div>

            <div>
              <label className="block text-sm font-medium text-text-secondary mb-1">
                {t('teamPreferences.currency.teamCurrency')}
              </label>
              <div className="flex items-center gap-3">
                <div className="flex-1 px-3 py-2 bg-surface-inset border border-border-strong rounded-lg text-text-primary font-medium">
                  {preferences.defaultCurrency || 'EUR'} (
                  {getCurrencySymbol(
                    currencies,
                    preferences.defaultCurrency || 'EUR'
                  )}
                  )
                </div>
                {canEditTeamSettings && (
                  <button
                    onClick={() => setShowCurrencyChange(true)}
                    className="px-3 py-2 text-sm font-medium text-warning-text bg-warning-bg border border-warning-border rounded-lg hover:opacity-90 transition-colors flex items-center gap-1.5"
                  >
                    <ArrowRightLeft className="h-4 w-4" />
                    {t('teamPreferences.currency.change')}
                  </button>
                )}
              </div>
              <p className="text-xs text-text-secondary mt-1">
                {t('teamPreferences.currency.description')}
              </p>
            </div>
          </div>

          {/* Default Country */}
          <div className="space-y-4 pt-6 border-t border-border-default">
            <div className="flex items-center gap-2">
              <MapPin className="h-5 w-5 text-text-secondary " />
              <h3 className="text-lg font-semibold text-text-primary">
                {t('teamPreferences.defaultCountry.title')}
              </h3>
            </div>

            <div>
              <label className="block text-sm font-medium text-text-secondary mb-1">
                {t('teamPreferences.defaultCountry.label')}
              </label>
              <CountrySelector
                value={preferences.defaultCountryCode}
                onChange={(v) =>
                  handlePreferenceChange('defaultCountryCode', v)
                }
                disabled={!canEditTeamSettings}
                placeholder={t('teamPreferences.defaultCountry.placeholder')}
              />
              <p className="text-xs text-text-secondary mt-1">
                {t('teamPreferences.defaultCountry.description')}
              </p>
            </div>
          </div>

          {/* Fiscal Year Settings */}
          <div className="space-y-4 pt-6 border-t border-border-default">
            <div className="flex items-center gap-2">
              <Calendar className="h-5 w-5 text-text-secondary " />
              <h3 className="text-lg font-semibold text-text-primary">
                {t('teamPreferences.fiscalYear.title')}
              </h3>
            </div>

            <div>
              <label className="block text-sm font-medium text-text-secondary mb-1">
                {t('teamPreferences.fiscalYear.fiscalYearStart')}
              </label>
              <select
                value={preferences.fiscalYearStart}
                onChange={(e) =>
                  handlePreferenceChange('fiscalYearStart', e.target.value)
                }
                disabled={!canEditTeamSettings}
                className="w-full px-3 py-2 border border-border-strong rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent disabled:bg-surface-inset disabled:cursor-not-allowed"
              >
                {months.map((month) => (
                  <option key={month.value} value={month.value}>
                    {month.label}
                  </option>
                ))}
              </select>
              <p className="text-xs text-text-secondary mt-1">
                {t('teamPreferences.fiscalYear.description')}
              </p>
            </div>
          </div>
        </div>
      </div>

      {showCurrencyChange && team && (
        <CurrencyChangeModal
          teamIdentifier={team.identifier}
          currentCurrency={preferences.defaultCurrency || 'EUR'}
          onClose={() => setShowCurrencyChange(false)}
          onSuccess={() => setShowCurrencyChange(false)}
        />
      )}
    </div>
  );
};
