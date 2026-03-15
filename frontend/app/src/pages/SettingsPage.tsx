import { User, Bell, Users, Settings as SettingsIcon } from 'lucide-react';
import { useTabState } from '@/hooks/useTabState';
import { UserProfileSection } from '@/components/settings/UserProfileSection';
import { UserPreferencesSection } from '@/components/settings/UserPreferencesSection';
import { MyTeamsSection } from '@/components/settings/MyTeamsSection';
import { ImpersonationGuard } from '@/components/ImpersonationGuard';

type SettingsTab = 'profile' | 'preferences' | 'teams';

const tabs = [
  { id: 'profile' as const, label: 'Profile & Security', icon: User },
  { id: 'preferences' as const, label: 'My Preferences', icon: Bell },
  { id: 'teams' as const, label: 'My Teams', icon: Users },
];

export const SettingsPage = () => {
  const [activeTab, setActiveTab] = useTabState<SettingsTab>('profile', [
    'profile',
    'preferences',
    'teams',
  ] as const);

  return (
    <div className="min-h-screen bg-background">
      <div className="px-4 py-8">
        {/* Header */}
        <div className="mb-8">
          <div className="mb-6">
            <div className="flex items-center gap-3 mb-1">
              <SettingsIcon className="h-8 w-8 text-primary-500 dark:text-primary-300" />
              <h1 className="text-3xl font-bold text-text-primary">Settings</h1>
            </div>
            <p className="text-text-secondary ml-11">
              Manage your personal profile and preferences
            </p>
          </div>

          {/* Tabs */}
          <div className="border-b border-border-default">
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
                          ? 'border-primary-500 text-primary-500'
                          : 'border-transparent text-text-secondary hover:text-text-primary hover:border-border-strong'
                      }
                    `}
                  >
                    <Icon
                      className={`h-5 w-5 ${
                        isActive
                          ? 'text-primary-500 dark:text-primary-300'
                          : 'text-text-muted group-hover:text-text-secondary'
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
          <ImpersonationGuard
            blockAlways
            fallback={
              <div className="text-center py-12 text-text-secondary">
                Settings cannot be modified during an impersonation session.
              </div>
            }
          >
            {activeTab === 'profile' && <UserProfileSection />}
            {activeTab === 'preferences' && <UserPreferencesSection />}
            {activeTab === 'teams' && <MyTeamsSection />}
          </ImpersonationGuard>
        </div>
      </div>
    </div>
  );
};
