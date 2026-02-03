import { useState } from 'react';
import { Building, DollarSign, MapPin, Calendar, Save } from 'lucide-react';

export const TeamPreferencesSection = () => {
  const [hasChanges, setHasChanges] = useState(false);

  // Mock data - will be replaced with actual team preferences
  const [preferences, setPreferences] = useState({
    defaultCurrency: 'EUR',
    defaultCountry: 'Netherlands',
    dateFormat: 'DD/MM/YYYY',
    fiscalYearStart: '01',
    timezone: 'Europe/Amsterdam',
  });

  const currencies = [
    { code: 'EUR', name: 'Euro (€)', symbol: '€' },
    { code: 'USD', name: 'US Dollar ($)', symbol: '$' },
    { code: 'GBP', name: 'British Pound (£)', symbol: '£' },
    { code: 'CHF', name: 'Swiss Franc (CHF)', symbol: 'CHF' },
  ];

  const countries = [
    'Netherlands',
    'Belgium',
    'Germany',
    'France',
    'United Kingdom',
    'Switzerland',
    'Other',
  ];

  const dateFormats = ['DD/MM/YYYY', 'MM/DD/YYYY', 'YYYY-MM-DD'];

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

  const timezones = [
    'Europe/Amsterdam',
    'Europe/Brussels',
    'Europe/London',
    'Europe/Paris',
    'Europe/Zurich',
    'UTC',
  ];

  const handlePreferenceChange = (key: string, value: string) => {
    setPreferences({ ...preferences, [key]: value });
    setHasChanges(true);
  };

  const handleSave = () => {
    // TODO: API call to save preferences
    setHasChanges(false);
  };

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
            {hasChanges && (
              <button
                onClick={handleSave}
                className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2"
              >
                <Save className="h-4 w-4" />
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
              <select
                value={preferences.defaultCurrency}
                onChange={(e) =>
                  handlePreferenceChange('defaultCurrency', e.target.value)
                }
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              >
                {currencies.map((currency) => (
                  <option key={currency.code} value={currency.code}>
                    {currency.name}
                  </option>
                ))}
              </select>
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
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              >
                {countries.map((country) => (
                  <option key={country} value={country}>
                    {country}
                  </option>
                ))}
              </select>
              <p className="text-xs text-gray-600 mt-1">
                Default country for new properties
              </p>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Timezone
              </label>
              <select
                value={preferences.timezone}
                onChange={(e) =>
                  handlePreferenceChange('timezone', e.target.value)
                }
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              >
                {timezones.map((tz) => (
                  <option key={tz} value={tz}>
                    {tz}
                  </option>
                ))}
              </select>
              <p className="text-xs text-gray-600 mt-1">
                Timezone for displaying dates and times
              </p>
            </div>
          </div>

          {/* Date & Time Settings */}
          <div className="space-y-4 pt-6 border-t border-gray-200">
            <div className="flex items-center gap-2">
              <Calendar className="h-5 w-5 text-gray-600" />
              <h3 className="text-lg font-semibold text-gray-900">
                Date & Time Settings
              </h3>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Date Format
              </label>
              <select
                value={preferences.dateFormat}
                onChange={(e) =>
                  handlePreferenceChange('dateFormat', e.target.value)
                }
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              >
                {dateFormats.map((format) => (
                  <option key={format} value={format}>
                    {format} (e.g., {new Date().toLocaleDateString()})
                  </option>
                ))}
              </select>
              <p className="text-xs text-gray-600 mt-1">
                How dates are displayed throughout the application
              </p>
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
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
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

          {/* Business Settings */}
          <div className="space-y-4 pt-6 border-t border-gray-200">
            <div className="flex items-center gap-2">
              <Building className="h-5 w-5 text-gray-600" />
              <h3 className="text-lg font-semibold text-gray-900">
                Business Settings
              </h3>
            </div>

            <div className="p-4 bg-blue-50 rounded-lg">
              <p className="text-sm text-blue-800">
                <strong>Note:</strong> Changing these preferences will affect
                how data is displayed for all team members. Existing data will
                not be modified.
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
