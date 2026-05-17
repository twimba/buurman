import { User, Bell, Users, Settings as SettingsIcon } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useTabState } from '@/hooks/useTabState';
import { UserProfileSection } from '@/components/settings/UserProfileSection';
import { UserPreferencesSection } from '@/components/settings/UserPreferencesSection';
import { MyTeamsSection } from '@/components/settings/MyTeamsSection';
import { ImpersonationGuard } from '@/components/ImpersonationGuard';
import { ListPageHeader } from '@buurman/ui';
import { MobileMenuButton } from '@/components/MobileMenuButton';

type SettingsTab = 'profile' | 'preferences' | 'teams';

export const SettingsPage = () => {
  const { t } = useTranslation('settings');
  const [activeTab, setActiveTab] = useTabState<SettingsTab>('profile', [
    'profile',
    'preferences',
    'teams',
  ] as const);

  const tabs = [
    { id: 'profile' as const, label: t('tabs.profile'), icon: User },
    { id: 'preferences' as const, label: t('tabs.preferences'), icon: Bell },
    { id: 'teams' as const, label: t('tabs.teams'), icon: Users },
  ];

  return (
    <div className="min-h-screen bg-background">
      <div className="px-4 py-4 md:py-8">
        <ListPageHeader
          title={t('page.title')}
          subtitle={t('page.subtitle')}
          icon={SettingsIcon}
          mobileLeading={<MobileMenuButton />}
        />
        <div className="mb-8">
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
                {t('page.impersonationBlocked')}
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
