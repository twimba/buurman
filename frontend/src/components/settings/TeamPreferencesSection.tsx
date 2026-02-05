import { useState } from 'react';
import { DollarSign, Calendar, Save, Loader2, MapPin } from 'lucide-react';
import {
  useCurrentTeam,
  useTeamSettings,
  useUpdateTeamSettings,
} from '../../hooks/useTeamHooks';
import { useTeam } from '../../context/TeamContext';
import { CurrencySelector } from '../common/CurrencySelector';
import { CountrySelector } from '../common/CountrySelector';

export const TeamPreferencesSection = () => {
  const { canEditTeamSettings } = useTeam();
  const { data: team } = useCurrentTeam();
  const { data: settingsData, isLoading } = useTeamSettings(team?.identifier);
  const updateSettingsMutation = useUpdateTeamSettings(team?.identifier || '');

  const [hasChanges, setHasChanges] = useState(false);
  const [preferences, setPreferences] = useState({
    defaultCurrency: '',
    defaultCountry: '',
    fiscalYearStart: '01',
  });

  const [lastSyncedSettings, setLastSyncedSettings] = useState(settingsData);
  if (settingsData?.regional && settingsData !== lastSyncedSettings) {
    setLastSyncedSettings(settingsData);
    setPreferences({
      defaultCurrency: settingsData.regional.defaultCurrency || '',
      defaultCountry: settingsData.regional.defaultCountry || '',
      fiscalYearStart: settingsData.regional.fiscalYearStartMonth || '01',
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
    updateSettingsMutation.mutate(
      {
        regional: {
          defaultCurrency: preferences.defaultCurrency || undefined,
          defaultCountry: preferences.defaultCountry || undefined,
          fiscalYearStartMonth: preferences.fiscalYearStart,
        },
      },
      {
        onSuccess: () => setHasChanges(false),
      }
    );
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <Loader2 className="h-8 w-8 animate-spin text-[#5c7cfa] dark:text-[#91a7ff]" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="bg-white dark:bg-[#14161f] rounded-lg shadow">
        <div className="p-6 border-b border-[#e2e6f0] dark:border-[#2a2e3f]">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-xl font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                Team Preferences
              </h2>
              <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] mt-1">
                Configure default settings for your team
              </p>
            </div>
            {hasChanges && canEditTeamSettings && (
              <button
                onClick={handleSave}
                disabled={updateSettingsMutation.isPending}
                className="px-4 py-2 bg-[#5c7cfa] text-white rounded-lg hover:bg-[#4c6ef5] transition-colors flex items-center gap-2 disabled:opacity-50"
              >
                {updateSettingsMutation.isPending ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <Save className="h-4 w-4" />
                )}
                Save Changes
              </button>
            )}
          </div>
        </div>

        <div className="p-6 space-y-6">
          {/* Currency Settings */}
          <div className="space-y-4">
            <div className="flex items-center gap-2">
              <DollarSign className="h-5 w-5 text-[#6b7194] dark:text-[#8b90a8]" />
              <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                Currency Settings
              </h3>
            </div>

            <div>
              <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
                Default Currency
              </label>
              <CurrencySelector
                value={preferences.defaultCurrency}
                onChange={(value) =>
                  handlePreferenceChange('defaultCurrency', value)
                }
                disabled={!canEditTeamSettings}
              />
              <p className="text-xs text-[#6b7194] dark:text-[#8b90a8] mt-1">
                This will be the default currency for rent, expenses, and
                payments
              </p>
            </div>
          </div>

          {/* Default Country */}
          <div className="space-y-4 pt-6 border-t border-[#e2e6f0] dark:border-[#2a2e3f]">
            <div className="flex items-center gap-2">
              <MapPin className="h-5 w-5 text-[#6b7194] dark:text-[#8b90a8]" />
              <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                Default Country
              </h3>
            </div>

            <div>
              <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
                Default Country
              </label>
              <CountrySelector
                value={preferences.defaultCountry}
                onChange={(v) => handlePreferenceChange('defaultCountry', v)}
                disabled={!canEditTeamSettings}
                placeholder="No default (select each time)"
              />
              <p className="text-xs text-[#6b7194] dark:text-[#8b90a8] mt-1">
                This will be the pre-selected country for new properties and
                tenant addresses
              </p>
            </div>
          </div>

          {/* Fiscal Year Settings */}
          <div className="space-y-4 pt-6 border-t border-[#e2e6f0] dark:border-[#2a2e3f]">
            <div className="flex items-center gap-2">
              <Calendar className="h-5 w-5 text-[#6b7194] dark:text-[#8b90a8]" />
              <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                Fiscal Year
              </h3>
            </div>

            <div>
              <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
                Fiscal Year Start
              </label>
              <select
                value={preferences.fiscalYearStart}
                onChange={(e) =>
                  handlePreferenceChange('fiscalYearStart', e.target.value)
                }
                disabled={!canEditTeamSettings}
                className="w-full px-3 py-2 border border-[#c9cfd9] rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:bg-[#f1f3f9] dark:bg-[#1e2130] disabled:cursor-not-allowed"
              >
                {months.map((month) => (
                  <option key={month.value} value={month.value}>
                    {month.label}
                  </option>
                ))}
              </select>
              <p className="text-xs text-[#6b7194] dark:text-[#8b90a8] mt-1">
                First month of your fiscal year for financial reports
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
