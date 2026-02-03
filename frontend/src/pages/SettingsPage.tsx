import { useState } from 'react';
import {
  User,
  Users,
  Building,
  Bell,
  CreditCard,
  Receipt,
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

type SettingsTab =
  | 'profile'
  | 'team'
  | 'teamPreferences'
  | 'preferences'
  | 'subscription'
  | 'payments';

const personalTabs = [
  { id: 'profile' as const, label: 'Profile & Security', icon: User },
  { id: 'preferences' as const, label: 'My Preferences', icon: Bell },
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
      <div className="max-w-6xl mx-auto px-4 py-8">
        {/* Work in Progress Banner */}
        {showBanner && (
          <div className="mb-6 relative overflow-hidden rounded-2xl bg-gradient-to-r from-purple-600 via-pink-600 to-blue-600 p-[2px] shadow-xl">
            <div className="relative bg-white rounded-2xl p-6 flex items-start gap-4">
              <div className="flex-shrink-0">
                <div className="relative">
                  <Construction className="h-8 w-8 text-purple-600 animate-bounce" />
                  <Sparkles className="h-4 w-4 text-yellow-400 absolute -top-1 -right-1 animate-pulse" />
                </div>
              </div>
              <div className="flex-1">
                <h3 className="text-lg font-bold text-gray-900 mb-1">
                  🚧 We're Cooking Something Special! 🚧
                </h3>
                <p className="text-gray-700 text-sm leading-relaxed">
                  Our settings page is like a fine wine - still aging to perfection! 🍷
                  Some features are fully functional (feel free to click around!), while
                  others are getting their final polish. Think of it as a "behind the
                  scenes" tour. Backend magic coming soon! ✨
                </p>
                <div className="mt-3 flex items-center gap-2">
                  <div className="flex -space-x-2">
                    <div className="h-6 w-6 rounded-full bg-gradient-to-br from-purple-400 to-purple-600 border-2 border-white animate-pulse" />
                    <div className="h-6 w-6 rounded-full bg-gradient-to-br from-pink-400 to-pink-600 border-2 border-white animate-pulse" style={{ animationDelay: '150ms' }} />
                    <div className="h-6 w-6 rounded-full bg-gradient-to-br from-blue-400 to-blue-600 border-2 border-white animate-pulse" style={{ animationDelay: '300ms' }} />
                  </div>
                  <span className="text-xs text-gray-600 italic">
                    Progress: Frontend 100% • Backend 0% • Vibes 200% 🎉
                  </span>
                </div>
              </div>
              <button
                onClick={() => setShowBanner(false)}
                className="flex-shrink-0 p-2 rounded-lg hover:bg-gray-100 transition-colors group"
                aria-label="Dismiss banner"
              >
                <X className="h-5 w-5 text-gray-400 group-hover:text-gray-600" />
              </button>
            </div>
          </div>
        )}

        {/* Header */}
        <div className="mb-8">
          <div className="mb-6">
            <div className="flex items-center gap-3 mb-2">
              <SettingsIcon className="h-7 w-7 text-gray-900" />
              <h1 className="text-3xl font-bold text-gray-900">Settings</h1>
            </div>
            <p className="text-sm text-gray-600 ml-10">
              Manage your personal profile and team-specific settings
            </p>
          </div>

          {/* Active Team Info Bar - Only show for team tabs */}
          {requiresAdminAccess && (
            <div className="mb-6 flex items-center justify-between p-4 bg-gradient-to-r from-blue-50 to-purple-50 border border-blue-100 rounded-lg">
              <div className="flex items-center gap-3">
                <Shield className="h-5 w-5 text-blue-600" />
                <div>
                  <p className="text-sm font-semibold text-gray-900">
                    Managing: {activeTeam?.name}
                  </p>
                  <p className="text-xs text-gray-600">
                    Role: {activeTeam?.role === 'TEAM_ADMIN' ? 'Admin' : activeTeam?.role === 'TEAM_EDITOR' ? 'Editor' : 'Viewer'}
                    {activeTeam?.isOwner && ' • Owner'}
                  </p>
                </div>
              </div>
              {!canEditTeamSettings && (
                <div className="flex items-center gap-2 px-3 py-1.5 bg-yellow-100 border border-yellow-200 rounded-lg">
                  <AlertCircle className="h-4 w-4 text-yellow-700" />
                  <p className="text-xs font-medium text-yellow-700">
                    View-only access
                  </p>
                </div>
              )}
            </div>
          )}

          {/* Horizontal Tabs with Sections */}
          <div className="border-b border-gray-200">
            <nav className="-mb-px flex space-x-1 overflow-x-auto">
              {/* Personal Section */}
              <div className="flex items-center gap-1">
                <div className="flex items-center px-3 py-2 text-xs font-semibold text-gray-500 uppercase tracking-wide">
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
                            ? 'border-blue-600 text-blue-600'
                            : 'border-transparent text-gray-600 hover:text-gray-900 hover:border-gray-300'
                        }
                      `}
                    >
                      <Icon
                        className={`h-5 w-5 ${
                          isActive
                            ? 'text-blue-600'
                            : 'text-gray-400 group-hover:text-gray-600'
                        }`}
                      />
                      {tab.label}
                    </button>
                  );
                })}
              </div>

              {/* Divider */}
              <div className="flex items-center px-2">
                <div className="h-8 w-px bg-gray-300"></div>
              </div>

              {/* Team Section */}
              <div className="flex items-center gap-1">
                <div className="flex items-center px-3 py-2 text-xs font-semibold text-gray-500 uppercase tracking-wide">
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
                            ? 'border-blue-600 text-blue-600'
                            : 'border-transparent text-gray-600 hover:text-gray-900 hover:border-gray-300'
                        }
                      `}
                    >
                      <Icon
                        className={`h-5 w-5 ${
                          isActive
                            ? 'text-blue-600'
                            : 'text-gray-400 group-hover:text-gray-600'
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
            <div className="mt-4 p-3 bg-blue-50 border border-blue-100 rounded-lg">
              <p className="text-xs text-blue-800 flex items-center gap-2">
                <User className="h-4 w-4 flex-shrink-0" />
                <span>
                  <strong>Personal settings</strong> apply to your account across all teams
                </span>
              </p>
            </div>
          )}
        </div>

        {/* Content Area */}
        <div className="pb-12">
          {activeTab === 'profile' && <UserProfileSection />}
          {activeTab === 'team' && (
            hasRequiredPermissions ? (
              <TeamSettingsSection />
            ) : (
              <PermissionDenied section="Team Management" />
            )
          )}
          {activeTab === 'teamPreferences' && (
            hasRequiredPermissions ? (
              <TeamPreferencesSection />
            ) : (
              <PermissionDenied section="Team Preferences" />
            )
          )}
          {activeTab === 'preferences' && <UserPreferencesSection />}
          {activeTab === 'subscription' && (
            hasRequiredPermissions ? (
              <SubscriptionSection />
            ) : (
              <PermissionDenied section="Subscription Management" />
            )
          )}
          {activeTab === 'payments' && (
            hasRequiredPermissions ? (
              <PaymentHistorySection />
            ) : (
              <PermissionDenied section="Billing History" />
            )
          )}
        </div>
      </div>
    </div>
  );
};

// Permission Denied Component
const PermissionDenied = ({ section }: { section: string }) => {
  return (
    <div className="bg-white rounded-lg shadow p-12 text-center">
      <div className="flex justify-center mb-4">
        <div className="h-16 w-16 rounded-full bg-yellow-100 flex items-center justify-center">
          <Shield className="h-8 w-8 text-yellow-600" />
        </div>
      </div>
      <h3 className="text-xl font-semibold text-gray-900 mb-2">
        Admin Access Required
      </h3>
      <p className="text-gray-600 mb-4">
        You don't have permission to access <strong>{section}</strong> for this
        team.
      </p>
      <p className="text-sm text-gray-500">
        Only team administrators can manage these settings. Contact your team
        owner if you need access.
      </p>
    </div>
  );
};
