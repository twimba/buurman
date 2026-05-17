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
  AlertTriangle,
} from 'lucide-react';
import { useTranslation } from 'react-i18next';
import {
  useUserPreferences,
  useUpdateUserPreferences,
  useNotificationTypePreferences,
  useUpdateNotificationTypePreferences,
} from '../../hooks/useUserPreferencesHooks';
import { useTheme } from '../../context/ThemeContext';
import { useLocale } from '../../context/LocaleContext';
import { NotificationTypePreferenceEntry } from '../../api/users';

export const UserPreferencesSection = () => {
  const { t } = useTranslation('settings');
  const {
    data: preferencesData,
    isLoading,
    isError: isPreferencesError,
    refetch: refetchPreferences,
  } = useUserPreferences();
  const updatePreferencesMutation = useUpdateUserPreferences();
  const {
    data: notifTypeData,
    isLoading: notifTypeLoading,
    isError: isNotifTypeError,
    refetch: refetchNotifType,
  } = useNotificationTypePreferences();
  const updateNotifTypeMutation = useUpdateNotificationTypePreferences();
  const { setTheme } = useTheme();
  const { setLocale } = useLocale();

  const [preferences, setPreferences] = useState(() => ({
    theme: preferencesData?.theme || 'light',
    language: preferencesData?.language || 'en',
    timezone: preferencesData?.timezone || 'Europe/Amsterdam',
    dateFormat: preferencesData?.dateFormat || 'DD/MM/YYYY',
    emailNotifications: preferencesData?.emailNotifications ?? true,
    smsNotifications: preferencesData?.smsNotifications ?? false,
  }));
  const [typePrefs, setTypePrefs] = useState<NotificationTypePreferenceEntry[]>(
    () => notifTypeData?.preferences ?? []
  );
  const [hasGlobalChanges, setHasGlobalChanges] = useState(false);
  const [hasNotifTypeChanges, setHasNotifTypeChanges] = useState(false);

  const smsAvailable = notifTypeData?.smsAvailable ?? false;
  const smsFeatureEnabled = notifTypeData?.smsFeatureEnabled ?? false;
  const emailAvailable = notifTypeData?.emailAvailable ?? true;
  const phoneVerified = notifTypeData?.phoneVerified ?? true;

  // Sync global preferences from server
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
      smsNotifications: preferencesData.smsNotifications ?? false,
    });
  }

  // Sync notification type preferences from server
  const [lastSyncedNotifType, setLastSyncedNotifType] = useState(notifTypeData);
  if (notifTypeData && notifTypeData !== lastSyncedNotifType) {
    setLastSyncedNotifType(notifTypeData);
    setTypePrefs(notifTypeData.preferences);
  }

  const handleThemeChange = (theme: string) => {
    setPreferences((prev) => ({ ...prev, theme }));
    setHasGlobalChanges(true);
    setTheme(theme as 'light' | 'dark' | 'system');
  };

  const handleLanguageChange = (language: string) => {
    setPreferences((prev) => ({ ...prev, language }));
    setHasGlobalChanges(true);
    setLocale(language);
  };

  const handleTimezoneChange = (timezone: string) => {
    setPreferences((prev) => ({ ...prev, timezone }));
    setHasGlobalChanges(true);
  };

  const handleDateFormatChange = (dateFormat: string) => {
    setPreferences((prev) => ({ ...prev, dateFormat }));
    setHasGlobalChanges(true);
  };

  const handleGlobalToggle = (
    field: 'emailNotifications' | 'smsNotifications'
  ) => {
    setPreferences((prev) => {
      const newValue = !prev[field];
      const typeChannel =
        field === 'emailNotifications' ? 'emailEnabled' : 'smsEnabled';
      setTypePrefs((prevTypes) =>
        prevTypes.map((p) => ({ ...p, [typeChannel]: newValue }))
      );
      return { ...prev, [field]: newValue };
    });
    setHasGlobalChanges(true);
    setHasNotifTypeChanges(true);
  };

  const handleTypeToggle = (
    notificationType: string,
    channel: 'emailEnabled' | 'smsEnabled'
  ) => {
    setTypePrefs((prev) =>
      prev.map((p) =>
        p.notificationType === notificationType
          ? { ...p, [channel]: !p[channel] }
          : p
      )
    );
    setHasNotifTypeChanges(true);
  };

  const handleSave = () => {
    const promises: Promise<unknown>[] = [];

    if (hasGlobalChanges) {
      promises.push(updatePreferencesMutation.mutateAsync(preferences));
    }
    if (hasNotifTypeChanges) {
      promises.push(
        updateNotifTypeMutation.mutateAsync({
          preferences: typePrefs.map((p) => ({
            notificationType: p.notificationType,
            emailEnabled: p.emailEnabled,
            smsEnabled: p.smsEnabled,
          })),
        })
      );
    }

    Promise.all(promises).then(() => {
      setHasGlobalChanges(false);
      setHasNotifTypeChanges(false);
    });
  };

  const isSaving =
    updatePreferencesMutation.isPending || updateNotifTypeMutation.isPending;

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
    { value: 'pt', label: 'Português' },
    { value: 'es', label: 'Español' },
    { value: 'de', label: 'Deutsch' },
    { value: 'fr', label: 'Français' },
    { value: 'it', label: 'Italiano' },
    { value: 'sv', label: 'Svenska' },
    { value: 'fi', label: 'Suomi' },
    { value: 'el', label: 'Ελληνικά' },
    { value: 'pl', label: 'Polski' },
    { value: 'da', label: 'Dansk' },
    { value: 'nb', label: 'Norsk' },
  ];

  const dateFormats = [
    { value: 'DD/MM/YYYY', label: 'DD/MM/YYYY (31/12/2024)' },
    { value: 'MM/DD/YYYY', label: 'MM/DD/YYYY (12/31/2024)' },
    { value: 'YYYY-MM-DD', label: 'YYYY-MM-DD (2024-12-31)' },
  ];

  if (isLoading || notifTypeLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <Loader2 className="h-8 w-8 animate-spin text-primary-500 dark:text-primary-300" />
      </div>
    );
  }

  if (isPreferencesError || isNotifTypeError) {
    return (
      <div className="flex flex-col items-center justify-center py-12 gap-4">
        <AlertTriangle className="h-8 w-8 text-warning-text" />
        <p className="text-sm text-text-secondary">
          {t('preferences.failedToLoad')}
        </p>
        <button
          onClick={() => {
            refetchPreferences();
            refetchNotifType();
          }}
          className="px-4 py-2 bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors text-sm"
        >
          {t('preferences.tryAgain')}
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="bg-surface-card rounded-lg shadow-sm">
        <div className="p-6 border-b border-border-default">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-xl font-semibold text-text-primary">
                {t('preferences.title')}
              </h2>
              <p className="text-sm text-text-secondary mt-1">
                {t('preferences.subtitle')}
              </p>
            </div>
            {(hasGlobalChanges || hasNotifTypeChanges) && (
              <button
                onClick={handleSave}
                disabled={isSaving}
                className="px-4 py-2 bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors flex items-center gap-2 disabled:opacity-50"
              >
                {isSaving ? (
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
          {/* Theme Settings */}
          <div className="space-y-4">
            <div className="flex items-center gap-2">
              <Moon className="h-5 w-5 text-text-secondary " />
              <h3 className="text-lg font-semibold text-text-primary">
                {t('preferences.appearance.title')}
              </h3>
            </div>

            <div>
              <label className="block text-sm font-medium text-text-secondary mb-3">
                {t('preferences.appearance.theme')}
              </label>
              <div className="grid grid-cols-3 gap-3">
                <button
                  onClick={() => handleThemeChange('light')}
                  className={`p-4 border-2 rounded-lg transition-colors ${
                    preferences.theme === 'light'
                      ? 'border-primary-500 bg-primary-50'
                      : 'border-border-default hover:border-border-strong'
                  }`}
                >
                  <Sun className="h-6 w-6 mx-auto text-yellow-500 mb-2" />
                  <p className="text-sm font-medium text-text-primary">
                    {t('preferences.appearance.light')}
                  </p>
                </button>
                <button
                  onClick={() => handleThemeChange('dark')}
                  className={`p-4 border-2 rounded-lg transition-colors ${
                    preferences.theme === 'dark'
                      ? 'border-primary-500 bg-primary-50'
                      : 'border-border-default hover:border-border-strong'
                  }`}
                >
                  <Moon className="h-6 w-6 mx-auto text-indigo-500 mb-2" />
                  <p className="text-sm font-medium text-text-primary">
                    {t('preferences.appearance.dark')}
                  </p>
                </button>
                <button
                  onClick={() => handleThemeChange('system')}
                  className={`p-4 border-2 rounded-lg transition-colors ${
                    preferences.theme === 'system'
                      ? 'border-primary-500 bg-primary-50'
                      : 'border-border-default hover:border-border-strong'
                  }`}
                >
                  <div className="flex justify-center gap-1 mb-2">
                    <Sun className="h-5 w-5 text-yellow-500" />
                    <Moon className="h-5 w-5 text-indigo-500" />
                  </div>
                  <p className="text-sm font-medium text-text-primary">
                    {t('preferences.appearance.auto')}
                  </p>
                </button>
              </div>
              <p className="text-xs text-text-secondary mt-2">
                {t('preferences.appearance.autoDescription')}
              </p>
            </div>
          </div>

          {/* Language & Region */}
          <div className="space-y-4 pt-6 border-t border-border-default">
            <div className="flex items-center gap-2">
              <Globe className="h-5 w-5 text-text-secondary " />
              <h3 className="text-lg font-semibold text-text-primary">
                {t('preferences.languageRegion.title')}
              </h3>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-text-secondary mb-1">
                  {t('preferences.languageRegion.language')}
                </label>
                <select
                  value={preferences.language}
                  onChange={(e) => handleLanguageChange(e.target.value)}
                  className="w-full px-3 py-2 border border-border-strong rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                >
                  {languages.map((lang) => (
                    <option key={lang.value} value={lang.value}>
                      {lang.label}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-text-secondary mb-1">
                  {t('preferences.languageRegion.dateFormat')}
                </label>
                <select
                  value={preferences.dateFormat}
                  onChange={(e) => handleDateFormatChange(e.target.value)}
                  className="w-full px-3 py-2 border border-border-strong rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
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
          <div className="space-y-4 pt-6 border-t border-border-default">
            <div className="flex items-center gap-2">
              <Clock className="h-5 w-5 text-text-secondary " />
              <h3 className="text-lg font-semibold text-text-primary">
                {t('preferences.timezone.title')}
              </h3>
            </div>

            <div>
              <select
                value={preferences.timezone}
                onChange={(e) => handleTimezoneChange(e.target.value)}
                className="w-full px-3 py-2 border border-border-strong rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
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
          <div className="space-y-4 pt-6 border-t border-border-default">
            <div className="flex items-center gap-2">
              <Bell className="h-5 w-5 text-text-secondary " />
              <h3 className="text-lg font-semibold text-text-primary">
                {t('preferences.notifications.title')}
              </h3>
            </div>

            {/* Global Master Switches */}
            <div className="space-y-3">
              {emailAvailable && (
                <div className="flex items-center justify-between p-4 bg-surface-page rounded-lg">
                  <div className="flex items-center gap-3">
                    <Mail className="h-5 w-5 text-text-secondary " />
                    <div>
                      <p className="font-medium text-text-primary">
                        {t('preferences.notifications.email')}
                      </p>
                      <p className="text-sm text-text-secondary">
                        {t('preferences.notifications.emailDescription')}
                      </p>
                    </div>
                  </div>
                  <button
                    onClick={() => handleGlobalToggle('emailNotifications')}
                    className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                      preferences.emailNotifications
                        ? 'bg-primary-500'
                        : 'bg-neutral-200 dark:bg-neutral-700'
                    }`}
                  >
                    <span
                      className={`inline-block h-4 w-4 transform rounded-full bg-surface-card transition-transform ${
                        preferences.emailNotifications
                          ? 'translate-x-6'
                          : 'translate-x-1'
                      }`}
                    />
                  </button>
                </div>
              )}
              {!emailAvailable && (
                <div className="p-4 bg-surface-page rounded-lg">
                  <div className="flex items-center gap-3">
                    <Mail className="h-5 w-5 text-text-secondary " />
                    <div>
                      <p className="text-sm text-text-secondary">
                        {t('security.securityEmailNote')}
                      </p>
                    </div>
                  </div>
                </div>
              )}

              {smsFeatureEnabled && (
                <div
                  className={`flex items-center justify-between p-4 bg-surface-page rounded-lg ${!phoneVerified ? 'opacity-60' : ''}`}
                >
                  <div className="flex items-center gap-3">
                    <MessageSquare className="h-5 w-5 text-text-secondary" />
                    <div>
                      <p className="font-medium text-text-primary">
                        {t('preferences.notifications.sms')}
                      </p>
                      <p className="text-sm text-text-secondary">
                        {!phoneVerified
                          ? t('preferences.notifications.smsRequiresPhone')
                          : t('preferences.notifications.smsDescription')}
                      </p>
                    </div>
                  </div>
                  <button
                    onClick={() =>
                      smsAvailable && handleGlobalToggle('smsNotifications')
                    }
                    disabled={!smsAvailable}
                    title={
                      !phoneVerified
                        ? t('preferences.notifications.smsRequiresPhone')
                        : undefined
                    }
                    className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                      !smsAvailable
                        ? 'cursor-not-allowed bg-neutral-200 dark:bg-neutral-700'
                        : preferences.smsNotifications
                          ? 'bg-primary-500'
                          : 'bg-neutral-200 dark:bg-neutral-700'
                    }`}
                  >
                    <span
                      className={`inline-block h-4 w-4 transform rounded-full bg-surface-card transition-transform ${
                        preferences.smsNotifications && smsAvailable
                          ? 'translate-x-6'
                          : 'translate-x-1'
                      }`}
                    />
                  </button>
                </div>
              )}
            </div>

            {/* Per-Type Notification Grid — only shown when at least one channel is enabled */}
            {typePrefs.length > 0 &&
              (preferences.emailNotifications ||
                preferences.smsNotifications) && (
                <div className="mt-4">
                  <p className="text-sm font-medium text-text-secondary mb-3">
                    {t('preferences.notifications.configurePerType')}
                  </p>
                  <div className="border border-border-default rounded-lg overflow-hidden">
                    <div className="overflow-x-auto">
                      <table className="w-full text-sm">
                        <thead>
                          <tr className="bg-surface-page dark:bg-surface-card border-b border-border-default">
                            <th className="text-left px-4 py-3 font-medium text-text-secondary">
                              {t('preferences.notifications.notificationType')}
                            </th>
                            {emailAvailable && (
                              <th className="text-center px-4 py-3 font-medium text-text-secondary w-20">
                                <div className="flex items-center justify-center gap-1">
                                  <Mail className="h-3.5 w-3.5" />
                                  {t('common:labels.email')}
                                </div>
                              </th>
                            )}
                            {smsAvailable && (
                              <th className="text-center px-4 py-3 font-medium text-text-secondary w-20">
                                <div className="flex items-center justify-center gap-1">
                                  <MessageSquare className="h-3.5 w-3.5" />
                                  SMS
                                </div>
                              </th>
                            )}
                          </tr>
                        </thead>
                        <tbody>
                          {typePrefs.map((pref, index) => (
                            <tr
                              key={pref.notificationType}
                              className={
                                index < typePrefs.length - 1
                                  ? 'border-b border-border-default'
                                  : ''
                              }
                            >
                              <td className="px-4 py-3 text-text-primary">
                                {pref.displayName}
                              </td>
                              {emailAvailable && (
                                <td className="px-4 py-3 text-center">
                                  <input
                                    type="checkbox"
                                    checked={pref.emailEnabled}
                                    disabled={!preferences.emailNotifications}
                                    onChange={() =>
                                      handleTypeToggle(
                                        pref.notificationType,
                                        'emailEnabled'
                                      )
                                    }
                                    className="h-4 w-4 rounded border-border-strong text-primary-500 focus:ring-primary-500 disabled:opacity-40 disabled:cursor-not-allowed"
                                  />
                                </td>
                              )}
                              {smsAvailable && (
                                <td className="px-4 py-3 text-center">
                                  <input
                                    type="checkbox"
                                    checked={pref.smsEnabled}
                                    disabled={!preferences.smsNotifications}
                                    onChange={() =>
                                      handleTypeToggle(
                                        pref.notificationType,
                                        'smsEnabled'
                                      )
                                    }
                                    className="h-4 w-4 rounded border-border-strong text-primary-500 focus:ring-primary-500 disabled:opacity-40 disabled:cursor-not-allowed"
                                  />
                                </td>
                              )}
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </div>
                  {emailAvailable &&
                    !preferences.emailNotifications &&
                    smsAvailable &&
                    preferences.smsNotifications && (
                      <p className="text-xs text-text-secondary mt-2">
                        {t('preferences.notifications.emailColumnDisabled')}
                      </p>
                    )}
                  {emailAvailable &&
                    preferences.emailNotifications &&
                    smsAvailable &&
                    !preferences.smsNotifications && (
                      <p className="text-xs text-text-secondary mt-2">
                        {t('preferences.notifications.smsColumnDisabled')}
                      </p>
                    )}
                </div>
              )}
          </div>
        </div>
      </div>
    </div>
  );
};
