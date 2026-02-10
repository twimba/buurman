import { useState } from 'react';
import {
  Bell,
  Moon,
  Sun,
  Mail,
  MessageSquare,
  Save,
  Loader2,
  Globe,
  Clock,
} from 'lucide-react';
import {
  useUserPreferences,
  useUpdateUserPreferences,
} from '../../hooks/useUserPreferencesHooks';
import { useTheme } from '../../context/ThemeContext';

export const UserPreferencesSection = () => {
  const { data: preferencesData, isLoading } = useUserPreferences();
  const updatePreferencesMutation = useUpdateUserPreferences();
  const { setTheme } = useTheme();

  const [preferences, setPreferences] = useState({
    theme: 'light',
    language: 'en',
    timezone: 'Europe/Amsterdam',
    dateFormat: 'DD/MM/YYYY',
    emailNotifications: true,
    inAppNotifications: true,
    smsNotifications: false,
  });
  const [hasChanges, setHasChanges] = useState(false);

  const [lastSyncedPreferences, setLastSyncedPreferences] =
    useState(preferencesData);
  if (preferencesData && preferencesData !== lastSyncedPreferences) {
    setLastSyncedPreferences(preferencesData);
    setPreferences({
      theme: preferencesData.theme || 'light',
      language: preferencesData.language || 'en',
      timezone: preferencesData.timezone || 'Europe/Amsterdam',
      dateFormat: preferencesData.dateFormat || 'DD/MM/YYYY',
      emailNotifications: preferencesData.emailNotifications ?? true,
      inAppNotifications: preferencesData.inAppNotifications ?? true,
      smsNotifications: preferencesData.smsNotifications ?? false,
    });
  }

  const handleThemeChange = (theme: string) => {
    setPreferences((prev) => ({ ...prev, theme }));
    setHasChanges(true);
    // Apply theme immediately without waiting for save
    setTheme(theme as 'light' | 'dark' | 'system');
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

  const handleSmsNotificationsToggle = () => {
    setPreferences((prev) => ({
      ...prev,
      smsNotifications: !prev.smsNotifications,
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
        <Loader2 className="h-8 w-8 animate-spin text-[#5c7cfa] dark:text-[#91a7ff]" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="bg-white dark:bg-[#14161f] rounded-lg shadow-sm">
        <div className="p-6 border-b border-[#e2e6f0] dark:border-[#2a2e3f]">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-xl font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                User Preferences
              </h2>
              <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] mt-1">
                Customize your personal experience
              </p>
            </div>
            {hasChanges && (
              <button
                onClick={handleSave}
                disabled={updatePreferencesMutation.isPending}
                className="px-4 py-2 bg-[#5c7cfa] text-white rounded-lg hover:bg-[#4c6ef5] transition-colors flex items-center gap-2 disabled:opacity-50"
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
              <Moon className="h-5 w-5 text-[#6b7194] dark:text-[#8b90a8]" />
              <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                Appearance
              </h3>
            </div>

            <div>
              <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-3">
                Theme
              </label>
              <div className="grid grid-cols-3 gap-3">
                <button
                  onClick={() => handleThemeChange('light')}
                  className={`p-4 border-2 rounded-lg transition-colors ${
                    preferences.theme === 'light'
                      ? 'border-[#5c7cfa] dark:border-blue-400 bg-primary-50 dark:bg-primary-500/10'
                      : 'border-[#e2e6f0] dark:border-[#3a3f54] hover:border-[#c9cfd9] dark:hover:border-[#c9cfd9] dark:border-[#3a3f54]'
                  }`}
                >
                  <Sun className="h-6 w-6 mx-auto text-yellow-500 mb-2" />
                  <p className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                    Light
                  </p>
                </button>
                <button
                  onClick={() => handleThemeChange('dark')}
                  className={`p-4 border-2 rounded-lg transition-colors ${
                    preferences.theme === 'dark'
                      ? 'border-[#5c7cfa] dark:border-blue-400 bg-primary-50 dark:bg-primary-500/10'
                      : 'border-[#e2e6f0] dark:border-[#3a3f54] hover:border-[#c9cfd9] dark:hover:border-[#c9cfd9] dark:border-[#3a3f54]'
                  }`}
                >
                  <Moon className="h-6 w-6 mx-auto text-indigo-500 mb-2" />
                  <p className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                    Dark
                  </p>
                </button>
                <button
                  onClick={() => handleThemeChange('system')}
                  className={`p-4 border-2 rounded-lg transition-colors ${
                    preferences.theme === 'system'
                      ? 'border-[#5c7cfa] dark:border-blue-400 bg-primary-50 dark:bg-primary-500/10'
                      : 'border-[#e2e6f0] dark:border-[#3a3f54] hover:border-[#c9cfd9] dark:hover:border-[#c9cfd9] dark:border-[#3a3f54]'
                  }`}
                >
                  <div className="flex justify-center gap-1 mb-2">
                    <Sun className="h-5 w-5 text-yellow-500" />
                    <Moon className="h-5 w-5 text-indigo-500" />
                  </div>
                  <p className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                    Auto
                  </p>
                </button>
              </div>
              <p className="text-xs text-[#6b7194] dark:text-[#8b90a8] mt-2">
                Auto mode follows your system preferences
              </p>
            </div>
          </div>

          {/* Language & Region */}
          <div className="space-y-4 pt-6 border-t border-[#e2e6f0] dark:border-[#2a2e3f]">
            <div className="flex items-center gap-2">
              <Globe className="h-5 w-5 text-[#6b7194] dark:text-[#8b90a8]" />
              <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                Language & Region
              </h3>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
                  Language
                </label>
                <select
                  value={preferences.language}
                  onChange={(e) => handleLanguageChange(e.target.value)}
                  className="w-full px-3 py-2 border border-[#c9cfd9] dark:border-[#3a3f54] rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent dark:bg-[#1e2130] dark:text-[#eef0f6]"
                >
                  {languages.map((lang) => (
                    <option key={lang.value} value={lang.value}>
                      {lang.label}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
                  Date Format
                </label>
                <select
                  value={preferences.dateFormat}
                  onChange={(e) => handleDateFormatChange(e.target.value)}
                  className="w-full px-3 py-2 border border-[#c9cfd9] dark:border-[#3a3f54] rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent dark:bg-[#1e2130] dark:text-[#eef0f6]"
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
          <div className="space-y-4 pt-6 border-t border-[#e2e6f0] dark:border-[#2a2e3f]">
            <div className="flex items-center gap-2">
              <Clock className="h-5 w-5 text-[#6b7194] dark:text-[#8b90a8]" />
              <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                Timezone
              </h3>
            </div>

            <div>
              <select
                value={preferences.timezone}
                onChange={(e) => handleTimezoneChange(e.target.value)}
                className="w-full px-3 py-2 border border-[#c9cfd9] dark:border-[#3a3f54] rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent dark:bg-[#1e2130] dark:text-[#eef0f6]"
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
          <div className="space-y-4 pt-6 border-t border-[#e2e6f0] dark:border-[#2a2e3f]">
            <div className="flex items-center gap-2">
              <Bell className="h-5 w-5 text-[#6b7194] dark:text-[#8b90a8]" />
              <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                Notifications
              </h3>
            </div>

            <div className="space-y-3">
              <div className="flex items-center justify-between p-4 bg-[#f8f9fc] dark:bg-[#0c0d14] dark:bg-[#1e2130] rounded-lg">
                <div className="flex items-center gap-3">
                  <Mail className="h-5 w-5 text-[#6b7194] dark:text-[#8b90a8]" />
                  <div>
                    <p className="font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                      Email Notifications
                    </p>
                    <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                      Receive important updates via email
                    </p>
                  </div>
                </div>
                <button
                  onClick={handleEmailNotificationsToggle}
                  className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                    preferences.emailNotifications
                      ? 'bg-[#5c7cfa]'
                      : 'bg-[#c9cfd9] dark:bg-[#3a3f54]'
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

              <div className="flex items-center justify-between p-4 bg-[#f8f9fc] dark:bg-[#0c0d14] dark:bg-[#1e2130] rounded-lg">
                <div className="flex items-center gap-3">
                  <Bell className="h-5 w-5 text-[#6b7194] dark:text-[#8b90a8]" />
                  <div>
                    <p className="font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                      In-App Notifications
                    </p>
                    <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                      Show notifications within the application
                    </p>
                  </div>
                </div>
                <button
                  onClick={handleInAppNotificationsToggle}
                  className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                    preferences.inAppNotifications
                      ? 'bg-[#5c7cfa]'
                      : 'bg-[#c9cfd9] dark:bg-[#3a3f54]'
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

              <div className="flex items-center justify-between p-4 bg-[#f8f9fc] dark:bg-[#0c0d14] dark:bg-[#1e2130] rounded-lg">
                <div className="flex items-center gap-3">
                  <MessageSquare className="h-5 w-5 text-[#6b7194] dark:text-[#8b90a8]" />
                  <div>
                    <p className="font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                      SMS Notifications
                    </p>
                    <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                      Receive critical alerts via text message (requires phone
                      number)
                    </p>
                  </div>
                </div>
                <button
                  onClick={handleSmsNotificationsToggle}
                  className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                    preferences.smsNotifications
                      ? 'bg-[#5c7cfa]'
                      : 'bg-[#c9cfd9] dark:bg-[#3a3f54]'
                  }`}
                >
                  <span
                    className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                      preferences.smsNotifications
                        ? 'translate-x-6'
                        : 'translate-x-1'
                    }`}
                  />
                </button>
              </div>
            </div>

            <p className="text-xs text-[#6b7194] dark:text-[#8b90a8]">
              Team-specific notification settings can be configured in the Team
              Preferences section
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};
