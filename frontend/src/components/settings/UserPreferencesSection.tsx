import { useState } from 'react';
import { Bell, Moon, Sun, Mail, Save } from 'lucide-react';

export const UserPreferencesSection = () => {
  const [hasChanges, setHasChanges] = useState(false);

  // Mock data - will be replaced with actual user preferences
  const [preferences, setPreferences] = useState({
    theme: 'light',
    emailNotifications: {
      newContract: true,
      paymentDue: true,
      paymentReceived: true,
      expenseAdded: true,
      contractExpiring: true,
      teamInvitation: true,
    },
    inAppNotifications: {
      newContract: true,
      paymentDue: true,
      paymentReceived: false,
      expenseAdded: false,
      contractExpiring: true,
      teamInvitation: true,
    },
    language: 'en',
  });

  const handleThemeChange = (theme: string) => {
    setPreferences({ ...preferences, theme });
    setHasChanges(true);
  };

  const handleEmailNotificationToggle = (key: string) => {
    setPreferences({
      ...preferences,
      emailNotifications: {
        ...preferences.emailNotifications,
        [key]: !preferences.emailNotifications[key as keyof typeof preferences.emailNotifications],
      },
    });
    setHasChanges(true);
  };

  const handleInAppNotificationToggle = (key: string) => {
    setPreferences({
      ...preferences,
      inAppNotifications: {
        ...preferences.inAppNotifications,
        [key]: !preferences.inAppNotifications[key as keyof typeof preferences.inAppNotifications],
      },
    });
    setHasChanges(true);
  };

  const handleSave = () => {
    // TODO: API call to save preferences
    setHasChanges(false);
  };

  const notificationItems = [
    {
      key: 'newContract',
      label: 'New Contract Created',
      description: 'When a new rental contract is created',
    },
    {
      key: 'paymentDue',
      label: 'Payment Due',
      description: 'Reminders for upcoming payment due dates',
    },
    {
      key: 'paymentReceived',
      label: 'Payment Received',
      description: 'When a payment is marked as received',
    },
    {
      key: 'expenseAdded',
      label: 'New Expense',
      description: 'When an expense is added to a property',
    },
    {
      key: 'contractExpiring',
      label: 'Contract Expiring',
      description: 'When a contract is about to expire (30 days notice)',
    },
    {
      key: 'teamInvitation',
      label: 'Team Invitation',
      description: 'When someone invites you to join their team',
    },
  ];

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
                className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2"
              >
                <Save className="h-4 w-4" />
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
                  onClick={() => handleThemeChange('auto')}
                  className={`p-4 border-2 rounded-lg transition-colors ${
                    preferences.theme === 'auto'
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

          {/* Email Notifications */}
          <div className="space-y-4 pt-6 border-t border-gray-200">
            <div className="flex items-center gap-2">
              <Mail className="h-5 w-5 text-gray-600" />
              <h3 className="text-lg font-semibold text-gray-900">
                Email Notifications
              </h3>
            </div>

            <div className="space-y-3">
              {notificationItems.map((item) => (
                <div
                  key={item.key}
                  className="flex items-center justify-between p-3 bg-gray-50 rounded-lg"
                >
                  <div>
                    <p className="font-medium text-gray-900">{item.label}</p>
                    <p className="text-sm text-gray-600">{item.description}</p>
                  </div>
                  <button
                    onClick={() => handleEmailNotificationToggle(item.key)}
                    className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                      preferences.emailNotifications[item.key as keyof typeof preferences.emailNotifications]
                        ? 'bg-blue-600'
                        : 'bg-gray-300'
                    }`}
                  >
                    <span
                      className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                        preferences.emailNotifications[item.key as keyof typeof preferences.emailNotifications]
                          ? 'translate-x-6'
                          : 'translate-x-1'
                      }`}
                    />
                  </button>
                </div>
              ))}
            </div>
          </div>

          {/* In-App Notifications */}
          <div className="space-y-4 pt-6 border-t border-gray-200">
            <div className="flex items-center gap-2">
              <Bell className="h-5 w-5 text-gray-600" />
              <h3 className="text-lg font-semibold text-gray-900">
                In-App Notifications
              </h3>
            </div>

            <div className="space-y-3">
              {notificationItems.map((item) => (
                <div
                  key={item.key}
                  className="flex items-center justify-between p-3 bg-gray-50 rounded-lg"
                >
                  <div>
                    <p className="font-medium text-gray-900">{item.label}</p>
                    <p className="text-sm text-gray-600">{item.description}</p>
                  </div>
                  <button
                    onClick={() => handleInAppNotificationToggle(item.key)}
                    className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                      preferences.inAppNotifications[item.key as keyof typeof preferences.inAppNotifications]
                        ? 'bg-blue-600'
                        : 'bg-gray-300'
                    }`}
                  >
                    <span
                      className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                        preferences.inAppNotifications[item.key as keyof typeof preferences.inAppNotifications]
                          ? 'translate-x-6'
                          : 'translate-x-1'
                      }`}
                    />
                  </button>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
