import { useState, useEffect } from 'react';
import {
  Bell,
  Moon,
  Sun,
  Mail,
  Save,
  Loader2,
  Globe,
  Clock,
} from 'lucide-react';
import {
  useUserPreferences,
  useUpdateUserPreferences,
} from '../../hooks/useUserPreferencesHooks';

export const UserPreferencesSection = () => {
  const { data: preferencesData, isLoading } = useUserPreferences();
  const updatePreferencesMutation = useUpdateUserPreferences();

  const [preferences, setPreferences] = useState({
    theme: 'light',
    language: 'en',
    timezone: 'Europe/Amsterdam',
    dateFormat: 'DD/MM/YYYY',
    emailNotifications: true,
    inAppNotifications: true,
  });
  const [hasChanges, setHasChanges] = useState(false);

  useEffect(() => {
    if (preferencesData) {
      setPreferences({
        theme: preferencesData.theme || 'light',
        language: preferencesData.language || 'en',
        timezone: preferencesData.timezone || 'Europe/Amsterdam',
        dateFormat: preferencesData.dateFormat || 'DD/MM/YYYY',
        emailNotifications: preferencesData.emailNotifications ?? true,
        inAppNotifications: preferencesData.inAppNotifications ?? true,
      });
    }
  }, [preferencesData]);

  const handleThemeChange = (theme: string) => {
    setPreferences((prev) => ({ ...prev, theme }));
    setHasChanges(true);
  };

  const handleLanguageChange = (language: string) => {
    setPreferences((prev) => ({ ...prev, language }));
    setHasChanges(true);
  };

  const handleTimezoneChange = (timezone: string) => {
    setPreferences((prev) => ({ ...prev, timezone }));
    setHasChanges(true);
  };

  const handleDateFormatChange = (dateFormat: string) => {
    setPreferences((prev) => ({ ...prev, dateFormat }));
    setHasChanges(true);
  };

  const handleEmailNotificationsToggle = () => {
    setPreferences((prev) => ({
      ...prev,
      emailNotifications: !prev.emailNotifications,
    }));
    setHasChanges(true);
  };

  const handleInAppNotificationsToggle = () => {
    setPreferences((prev) => ({
      ...prev,
      inAppNotifications: !prev.inAppNotifications,
    }));
    setHasChanges(true);
  };

  const handleSave = () => {
    updatePreferencesMutation.mutate(preferences, {
      onSuccess: () => setHasChanges(false),
    });
  };

  const timezones = [
    { value: 'Europe/Amsterdam', label: 'Amsterdam (CET/CEST)' },
    { value: 'Europe/London', label: 'London (GMT/BST)' },
    { value: 'Europe/Paris', label: 'Paris (CET/CEST)' },
    { value: 'Europe/Berlin', label: 'Berlin (CET/CEST)' },
    { value: 'Europe/Brussels', label: 'Brussels (CET/CEST)' },
    { value: 'America/New_York', label: 'New York (EST/EDT)' },
    { value: 'America/Los_Angeles', label: 'Los Angeles (PST/PDT)' },
    { value: 'UTC', label: 'UTC' },
  ];

  const languages = [
    { value: 'en', label: 'English' },
    { value: 'nl', label: 'Nederlands' },
    { value: 'de', label: 'Deutsch' },
    { value: 'fr', label: 'Français' },
  ];

  const dateFormats = [
    { value: 'DD/MM/YYYY', label: 'DD/MM/YYYY (31/12/2024)' },
    { value: 'MM/DD/YYYY', label: 'MM/DD/YYYY (12/31/2024)' },
    { value: 'YYYY-MM-DD', label: 'YYYY-MM-DD (2024-12-31)' },
  ];

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
                User Preferences
              </h2>
              <p className="text-sm text-gray-600 mt-1">
                Customize your personal experience
              </p>
            </div>
            {hasChanges && (
              <button
                onClick={handleSave}
                disabled={updatePreferencesMutation.isPending}
                className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2 disabled:opacity-50"
              >
                {updatePreferencesMutation.isPending ? (
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
          {/* Theme Settings */}
          <div className="space-y-4">
            <div className="flex items-center gap-2">
              <Moon className="h-5 w-5 text-gray-600" />
              <h3 className="text-lg font-semibold text-gray-900">
                Appearance
              </h3>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-3">
                Theme
              </label>
              <div className="grid grid-cols-3 gap-3">
                <button
                  onClick={() => handleThemeChange('light')}
                  className={`p-4 border-2 rounded-lg transition-colors ${
                    preferences.theme === 'light'
                      ? 'border-blue-600 bg-blue-50'
                      : 'border-gray-200 hover:border-gray-300'
                  }`}
                >
                  <Sun className="h-6 w-6 mx-auto text-yellow-500 mb-2" />
                  <p className="text-sm font-medium text-gray-900">Light</p>
                </button>
                <button
                  onClick={() => handleThemeChange('dark')}
                  className={`p-4 border-2 rounded-lg transition-colors ${
                    preferences.theme === 'dark'
                      ? 'border-blue-600 bg-blue-50'
                      : 'border-gray-200 hover:border-gray-300'
                  }`}
                >
                  <Moon className="h-6 w-6 mx-auto text-indigo-500 mb-2" />
                  <p className="text-sm font-medium text-gray-900">Dark</p>
                </button>
                <button
                  onClick={() => handleThemeChange('system')}
                  className={`p-4 border-2 rounded-lg transition-colors ${
                    preferences.theme === 'system'
                      ? 'border-blue-600 bg-blue-50'
                      : 'border-gray-200 hover:border-gray-300'
                  }`}
                >
                  <div className="flex justify-center gap-1 mb-2">
                    <Sun className="h-5 w-5 text-yellow-500" />
                    <Moon className="h-5 w-5 text-indigo-500" />
                  </div>
                  <p className="text-sm font-medium text-gray-900">Auto</p>
                </button>
              </div>
              <p className="text-xs text-gray-600 mt-2">
                Auto mode follows your system preferences
              </p>
            </div>
          </div>

          {/* Language & Region */}
          <div className="space-y-4 pt-6 border-t border-gray-200">
            <div className="flex items-center gap-2">
              <Globe className="h-5 w-5 text-gray-600" />
              <h3 className="text-lg font-semibold text-gray-900">
                Language & Region
              </h3>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Language
                </label>
                <select
                  value={preferences.language}
                  onChange={(e) => handleLanguageChange(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                >
                  {languages.map((lang) => (
                    <option key={lang.value} value={lang.value}>
                      {lang.label}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Date Format
                </label>
                <select
                  value={preferences.dateFormat}
                  onChange={(e) => handleDateFormatChange(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                >
                  {dateFormats.map((format) => (
                    <option key={format.value} value={format.value}>
                      {format.label}
                    </option>
                  ))}
                </select>
              </div>
            </div>
          </div>

          {/* Timezone */}
          <div className="space-y-4 pt-6 border-t border-gray-200">
            <div className="flex items-center gap-2">
              <Clock className="h-5 w-5 text-gray-600" />
              <h3 className="text-lg font-semibold text-gray-900">Timezone</h3>
            </div>

            <div>
              <select
                value={preferences.timezone}
                onChange={(e) => handleTimezoneChange(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              >
                {timezones.map((tz) => (
                  <option key={tz.value} value={tz.value}>
                    {tz.label}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* Notification Settings */}
          <div className="space-y-4 pt-6 border-t border-gray-200">
            <div className="flex items-center gap-2">
              <Bell className="h-5 w-5 text-gray-600" />
              <h3 className="text-lg font-semibold text-gray-900">
                Notifications
              </h3>
            </div>

            <div className="space-y-3">
              <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
                <div className="flex items-center gap-3">
                  <Mail className="h-5 w-5 text-gray-600" />
                  <div>
                    <p className="font-medium text-gray-900">
                      Email Notifications
                    </p>
                    <p className="text-sm text-gray-600">
                      Receive important updates via email
                    </p>
                  </div>
                </div>
                <button
                  onClick={handleEmailNotificationsToggle}
                  className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                    preferences.emailNotifications
                      ? 'bg-blue-600'
                      : 'bg-gray-300'
                  }`}
                >
                  <span
                    className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                      preferences.emailNotifications
                        ? 'translate-x-6'
                        : 'translate-x-1'
                    }`}
                  />
                </button>
              </div>

              <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
                <div className="flex items-center gap-3">
                  <Bell className="h-5 w-5 text-gray-600" />
                  <div>
                    <p className="font-medium text-gray-900">
                      In-App Notifications
                    </p>
                    <p className="text-sm text-gray-600">
                      Show notifications within the application
                    </p>
                  </div>
                </div>
                <button
                  onClick={handleInAppNotificationsToggle}
                  className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                    preferences.inAppNotifications
                      ? 'bg-blue-600'
                      : 'bg-gray-300'
                  }`}
                >
                  <span
                    className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                      preferences.inAppNotifications
                        ? 'translate-x-6'
                        : 'translate-x-1'
                    }`}
                  />
                </button>
              </div>
            </div>

            <p className="text-xs text-gray-500">
              Team-specific notification settings can be configured in the Team
              Preferences section
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};
