import { useState, useEffect } from 'react';
import {
  DollarSign,
  MapPin,
  Calendar,
  Save,
  Loader2,
} from 'lucide-react';
import {
  useCurrentTeam,
  useTeamSettings,
  useUpdateTeamSettings,
} from '../../hooks/useTeamHooks';
import { useTeam } from '../../context/TeamContext';
import { CurrencySelector } from '../common/CurrencySelector';
import { countries } from '../../utils/countries';

export const TeamPreferencesSection = () => {
  const { canEditTeamSettings } = useTeam();
  const { data: team } = useCurrentTeam();
  const { data: settingsData, isLoading } = useTeamSettings(team?.teamId);
  const updateSettingsMutation = useUpdateTeamSettings(team?.teamId || '');

  const [hasChanges, setHasChanges] = useState(false);
  const [preferences, setPreferences] = useState({
    defaultCurrency: 'EUR',
    defaultCountry: 'Netherlands',
    fiscalYearStart: '01',
  });

  useEffect(() => {
    if (settingsData?.regional) {
      setPreferences({
        defaultCurrency: settingsData.regional.defaultCurrency || 'EUR',
        defaultCountry: settingsData.regional.defaultCountry || 'Netherlands',
        fiscalYearStart: settingsData.regional.fiscalYearStartMonth || '01',
      });
    }
  }, [settingsData]);

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
          defaultCurrency: preferences.defaultCurrency,
          defaultCountry: preferences.defaultCountry,
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
        <Loader2 className="h-8 w-8 animate-spin text-blue-600" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="bg-white rounded-lg shadow">
        <div className="p-6 border-b border-gray-200">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-xl font-semibold text-gray-900">
                Team Preferences
              </h2>
              <p className="text-sm text-gray-600 mt-1">
                Configure default settings for your team
              </p>
            </div>
            {hasChanges && canEditTeamSettings && (
              <button
                onClick={handleSave}
                disabled={updateSettingsMutation.isPending}
                className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2 disabled:opacity-50"
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
              <DollarSign className="h-5 w-5 text-gray-600" />
              <h3 className="text-lg font-semibold text-gray-900">
                Currency Settings
              </h3>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Default Currency
              </label>
              <CurrencySelector
                value={preferences.defaultCurrency}
                onChange={(value) =>
                  handlePreferenceChange('defaultCurrency', value)
                }
                disabled={!canEditTeamSettings}
              />
              <p className="text-xs text-gray-600 mt-1">
                This will be the default currency for rent, expenses, and
                payments
              </p>
            </div>
          </div>

          {/* Location Settings */}
          <div className="space-y-4 pt-6 border-t border-gray-200">
            <div className="flex items-center gap-2">
              <MapPin className="h-5 w-5 text-gray-600" />
              <h3 className="text-lg font-semibold text-gray-900">
                Location Settings
              </h3>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Default Country
              </label>
              <select
                value={preferences.defaultCountry}
                onChange={(e) =>
                  handlePreferenceChange('defaultCountry', e.target.value)
                }
                disabled={!canEditTeamSettings}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:bg-gray-100 disabled:cursor-not-allowed"
              >
                {countries.map((country) => (
                  <option key={country.code} value={country.name}>
                    {country.flag} {country.name}
                  </option>
                ))}
              </select>
              <p className="text-xs text-gray-600 mt-1">
                Default country for new properties
              </p>
            </div>
          </div>

          {/* Fiscal Year Settings */}
          <div className="space-y-4 pt-6 border-t border-gray-200">
            <div className="flex items-center gap-2">
              <Calendar className="h-5 w-5 text-gray-600" />
              <h3 className="text-lg font-semibold text-gray-900">
                Fiscal Year
              </h3>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Fiscal Year Start
              </label>
              <select
                value={preferences.fiscalYearStart}
                onChange={(e) =>
                  handlePreferenceChange('fiscalYearStart', e.target.value)
                }
                disabled={!canEditTeamSettings}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:bg-gray-100 disabled:cursor-not-allowed"
              >
                {months.map((month) => (
                  <option key={month.value} value={month.value}>
                    {month.label}
                  </option>
                ))}
              </select>
              <p className="text-xs text-gray-600 mt-1">
                First month of your fiscal year for financial reports
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
