import { useState } from 'react';
import {
  User,
  Users,
  Building,
  Bell,
  CreditCard,
  Receipt,
  Calendar,
  Settings as SettingsIcon,
  Construction,
  Sparkles,
  X,
  Shield,
  AlertCircle,
} from 'lucide-react';
import { useTeam } from '@/context/TeamContext';
import { UserProfileSection } from '@/components/settings/UserProfileSection';
import { TeamSettingsSection } from '@/components/settings/TeamSettingsSection';
import { TeamPreferencesSection } from '@/components/settings/TeamPreferencesSection';
import { UserPreferencesSection } from '@/components/settings/UserPreferencesSection';
import { SubscriptionSection } from '@/components/settings/SubscriptionSection';
import { PaymentHistorySection } from '@/components/settings/PaymentHistorySection';
import { CalendarFeedsSection } from '@/components/settings/CalendarFeedsSection';

type SettingsTab =
  | 'profile'
  | 'team'
  | 'teamPreferences'
  | 'preferences'
  | 'calendarFeeds'
  | 'subscription'
  | 'payments';

const personalTabs = [
  { id: 'profile' as const, label: 'Profile & Security', icon: User },
  { id: 'preferences' as const, label: 'My Preferences', icon: Bell },
  { id: 'calendarFeeds' as const, label: 'Calendar Feeds', icon: Calendar },
];

const teamTabs = [
  { id: 'team' as const, label: 'Team Members', icon: Users },
  { id: 'teamPreferences' as const, label: 'Team Preferences', icon: Building },
  { id: 'subscription' as const, label: 'Subscription', icon: CreditCard },
  { id: 'payments' as const, label: 'Billing', icon: Receipt },
];

export const SettingsPage = () => {
  const [activeTab, setActiveTab] = useState<SettingsTab>('profile');
  const [showBanner, setShowBanner] = useState(true);
  const { activeTeam, canEditTeamSettings } = useTeam();

  // Determine if current tab requires team admin permissions
  const requiresAdminAccess =
    activeTab === 'team' ||
    activeTab === 'teamPreferences' ||
    activeTab === 'subscription' ||
    activeTab === 'payments';

  const hasRequiredPermissions = !requiresAdminAccess || canEditTeamSettings;

  return (
    <div className="min-h-screen bg-background">
      <div className="px-4 py-8">
        {/* Work in Progress Banner */}
        {showBanner && (
          <div className="mb-6 relative overflow-hidden rounded-2xl bg-gradient-to-r from-purple-600 via-pink-600 to-blue-600 p-[2px] shadow-xl">
            <div className="relative bg-white dark:bg-[#14161f] rounded-2xl p-6 flex items-start gap-4">
              <div className="flex-shrink-0">
                <div className="relative">
                  <Construction className="h-8 w-8 text-purple-600 animate-bounce" />
                  <Sparkles className="h-4 w-4 text-yellow-400 absolute -top-1 -right-1 animate-pulse" />
                </div>
              </div>
              <div className="flex-1">
                <h3 className="text-lg font-bold text-[#1a1d2e] dark:text-[#eef0f6] mb-1">
                  🚧 We&apos;re Cooking Something Special! 🚧
                </h3>
                <p className="text-[#3d4463] dark:text-[#c4c8db] text-sm leading-relaxed">
                  Our settings page is like a fine wine - still aging to
                  perfection! 🍷 Some features are fully functional (feel free
                  to click around!), while others are getting their final
                  polish. Think of it as a &quot;behind the scenes&quot; tour.
                  Backend magic coming soon! ✨
                </p>
                <div className="mt-3 flex items-center gap-2">
                  <div className="flex -space-x-2">
                    <div className="h-6 w-6 rounded-full bg-gradient-to-br from-purple-400 to-purple-600 border-2 border-white dark:border-[#2a2e3f] animate-pulse" />
                    <div
                      className="h-6 w-6 rounded-full bg-gradient-to-br from-pink-400 to-pink-600 border-2 border-white dark:border-[#2a2e3f] animate-pulse"
                      style={{ animationDelay: '150ms' }}
                    />
                    <div
                      className="h-6 w-6 rounded-full bg-gradient-to-br from-blue-400 to-blue-600 border-2 border-white dark:border-[#2a2e3f] animate-pulse"
                      style={{ animationDelay: '300ms' }}
                    />
                  </div>
                  <span className="text-xs text-[#6b7194] dark:text-[#8b90a8] italic">
                    Progress: Frontend 100% • Backend 0% • Vibes 200% 🎉
                  </span>
                </div>
              </div>
              <button
                onClick={() => setShowBanner(false)}
                className="flex-shrink-0 p-2 rounded-lg hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] transition-colors group"
                aria-label="Dismiss banner"
              >
                <X className="h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180] group-hover:text-[#6b7194] dark:text-[#8b90a8] dark:group-hover:text-[#c4c8db]" />
              </button>
            </div>
          </div>
        )}

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
              Manage your personal profile and team-specific settings
            </p>
          </div>

          {/* Active Team Info Bar - Only show for team tabs */}
          {requiresAdminAccess && (
            <div className="mb-6 flex items-center justify-between p-4 bg-gradient-to-r from-blue-50 to-purple-50 dark:from-blue-900/30 dark:to-purple-900/30 border border-blue-100 dark:border-blue-800 rounded-lg">
              <div className="flex items-center gap-3">
                <Shield className="h-5 w-5 text-primary-500 dark:text-primary-300" />
                <div>
                  <p className="text-sm font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                    Managing: {activeTeam?.name}
                  </p>
                  <p className="text-xs text-[#6b7194] dark:text-[#8b90a8]">
                    Role:{' '}
                    {activeTeam?.role === 'TEAM_ADMIN'
                      ? 'Admin'
                      : activeTeam?.role === 'TEAM_EDITOR'
                        ? 'Editor'
                        : 'Viewer'}
                    {activeTeam?.isOwner && ' • Owner'}
                  </p>
                </div>
              </div>
              {!canEditTeamSettings && (
                <div className="flex items-center gap-2 px-3 py-1.5 bg-yellow-100 dark:bg-yellow-900/30 border border-yellow-200 dark:border-yellow-700 rounded-lg">
                  <AlertCircle className="h-4 w-4 text-yellow-700 dark:text-yellow-400" />
                  <p className="text-xs font-medium text-yellow-700 dark:text-yellow-400">
                    View-only access
                  </p>
                </div>
              )}
            </div>
          )}

          {/* Horizontal Tabs with Sections */}
          <div className="border-b border-[#e2e6f0] dark:border-[#2a2e3f]">
            <nav className="-mb-px flex space-x-1 overflow-x-auto">
              {/* Personal Section */}
              <div className="flex items-center gap-1">
                <div className="flex items-center px-3 py-2 text-xs font-semibold text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wide">
                  Personal
                </div>
                {personalTabs.map((tab) => {
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
                            : 'border-transparent text-[#6b7194] dark:text-[#8b90a8] hover:text-[#1a1d2e] dark:text-[#eef0f6] dark:hover:text-[#c4c8db] hover:border-[#c9cfd9] dark:hover:border-[#3a3f54]'
                        }
                      `}
                    >
                      <Icon
                        className={`h-5 w-5 ${
                          isActive
                            ? 'text-primary-500 dark:text-primary-300'
                            : 'text-[#9ca0b8] dark:text-[#5c6180] group-hover:text-[#6b7194] dark:text-[#8b90a8] dark:group-hover:text-[#c4c8db]'
                        }`}
                      />
                      {tab.label}
                    </button>
                  );
                })}
              </div>

              {/* Divider */}
              <div className="flex items-center px-2">
                <div className="h-8 w-px bg-[#c9cfd9] dark:bg-[#3a3f54]"></div>
              </div>

              {/* Team Section */}
              <div className="flex items-center gap-1">
                <div className="flex items-center px-3 py-2 text-xs font-semibold text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wide">
                  Team
                </div>
                {teamTabs.map((tab) => {
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
                            ? 'border-[#5c7cfa] text-blue-600'
                            : 'border-transparent text-[#6b7194] dark:text-[#8b90a8] hover:text-[#1a1d2e] dark:text-[#eef0f6] hover:border-[#c9cfd9]'
                        }
                      `}
                    >
                      <Icon
                        className={`h-5 w-5 ${
                          isActive
                            ? 'text-blue-600'
                            : 'text-[#9ca0b8] dark:text-[#5c6180] group-hover:text-[#3d4463] dark:hover:text-[#c4c8db]'
                        }`}
                      />
                      {tab.label}
                    </button>
                  );
                })}
              </div>
            </nav>
          </div>

          {/* Helper Text */}
          {!requiresAdminAccess && (
            <div className="mt-4 p-3 bg-blue-50 dark:bg-blue-900/20 border border-blue-100 dark:border-blue-800 rounded-lg">
              <p className="text-xs text-blue-800 dark:text-blue-300 flex items-center gap-2">
                <User className="h-4 w-4 flex-shrink-0" />
                <span>
                  <strong>Personal settings</strong> apply to your account
                  across all teams
                </span>
              </p>
            </div>
          )}
        </div>

        {/* Content Area */}
        <div className="pb-12">
          {activeTab === 'profile' && <UserProfileSection />}
          {activeTab === 'team' &&
            (hasRequiredPermissions ? (
              <TeamSettingsSection />
            ) : (
              <PermissionDenied section="Team Management" />
            ))}
          {activeTab === 'teamPreferences' &&
            (hasRequiredPermissions ? (
              <TeamPreferencesSection />
            ) : (
              <PermissionDenied section="Team Preferences" />
            ))}
          {activeTab === 'preferences' && <UserPreferencesSection />}
          {activeTab === 'calendarFeeds' && <CalendarFeedsSection />}
          {activeTab === 'subscription' &&
            (hasRequiredPermissions ? (
              <SubscriptionSection />
            ) : (
              <PermissionDenied section="Subscription Management" />
            ))}
          {activeTab === 'payments' &&
            (hasRequiredPermissions ? (
              <PaymentHistorySection />
            ) : (
              <PermissionDenied section="Billing History" />
            ))}
        </div>
      </div>
    </div>
  );
};

// Permission Denied Component
const PermissionDenied = ({ section }: { section: string }) => {
  return (
    <div className="bg-white dark:bg-[#14161f] rounded-xl shadow-sm p-12 text-center">
      <div className="flex justify-center mb-4">
        <div className="h-16 w-16 rounded-full bg-yellow-100 dark:bg-yellow-900/30 flex items-center justify-center">
          <Shield className="h-8 w-8 text-yellow-600" />
        </div>
      </div>
      <h3 className="text-xl font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-2">
        Admin Access Required
      </h3>
      <p className="text-[#6b7194] dark:text-[#8b90a8] mb-4">
        You don&apos;t have permission to access <strong>{section}</strong> for
        this team.
      </p>
      <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
        Only team administrators can manage these settings. Contact your team
        owner if you need access.
      </p>
    </div>
  );
};
