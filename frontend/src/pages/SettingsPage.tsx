import { useState } from 'react';
import { User, Bell, Settings as SettingsIcon } from 'lucide-react';
import { UserProfileSection } from '@/components/settings/UserProfileSection';
import { UserPreferencesSection } from '@/components/settings/UserPreferencesSection';

type SettingsTab = 'profile' | 'preferences';

const tabs = [
  { id: 'profile' as const, label: 'Profile & Security', icon: User },
  { id: 'preferences' as const, label: 'My Preferences', icon: Bell },
];

export const SettingsPage = () => {
  const [activeTab, setActiveTab] = useState<SettingsTab>('profile');

  return (
    <div className="min-h-screen bg-background">
      <div className="px-4 py-8">
        {/* Header */}
        <div className="mb-8">
          <div className="mb-6">
            <div className="flex items-center gap-3 mb-1">
              <SettingsIcon className="h-8 w-8 text-primary-500 dark:text-primary-300" />
              <h1 className="text-3xl font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
                Settings
              </h1>
            </div>
            <p className="text-[#6b7194] dark:text-[#8b90a8] ml-11">
              Manage your personal profile and preferences
            </p>
          </div>

          {/* Tabs */}
          <div className="border-b border-[#e2e6f0] dark:border-[#2a2e3f]">
            <nav className="-mb-px flex space-x-1 overflow-x-auto">
              {tabs.map((tab) => {
                const Icon = tab.icon;
                const isActive = activeTab === tab.id;
                return (
                  <button
                    key={tab.id}
                    onClick={() => setActiveTab(tab.id)}
                    className={`
                      group inline-flex items-center gap-2 py-4 px-3 border-b-2 font-medium text-sm whitespace-nowrap transition-colors
                      ${
                        isActive
                          ? 'border-[#5c7cfa] text-primary-500 dark:text-primary-300 dark:border-blue-400'
                          : 'border-transparent text-[#6b7194] dark:text-[#8b90a8] hover:text-[#1a1d2e] dark:hover:text-[#c4c8db] hover:border-[#c9cfd9] dark:hover:border-[#3a3f54]'
                      }
                    `}
                  >
                    <Icon
                      className={`h-5 w-5 ${
                        isActive
                          ? 'text-primary-500 dark:text-primary-300'
                          : 'text-[#9ca0b8] dark:text-[#5c6180] group-hover:text-[#6b7194] dark:group-hover:text-[#c4c8db]'
                      }`}
                    />
                    {tab.label}
                  </button>
                );
              })}
            </nav>
          </div>
        </div>

        {/* Content Area */}
        <div className="pb-12">
          {activeTab === 'profile' && <UserProfileSection />}
          {activeTab === 'preferences' && <UserPreferencesSection />}
        </div>
      </div>
    </div>
  );
};
