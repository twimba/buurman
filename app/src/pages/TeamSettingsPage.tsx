import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Users,
  Building,
  CreditCard,
  Receipt,
  Calendar,
  Construction,
  Sparkles,
  X,
  Shield,
  AlertCircle,
} from 'lucide-react';
import { useTeam } from '@/context/TeamContext';
import { TeamSettingsSection } from '@/components/settings/TeamSettingsSection';
import { TeamPreferencesSection } from '@/components/settings/TeamPreferencesSection';
import { SubscriptionSection } from '@/components/settings/SubscriptionSection';
import { PaymentHistorySection } from '@/components/settings/PaymentHistorySection';
import { PaymentInstructionsSection } from '@/components/settings/PaymentInstructionsSection';
import { CalendarFeedsSection } from '@/components/settings/CalendarFeedsSection';

type TeamSettingsTab =
  | 'team'
  | 'teamPreferences'
  | 'paymentInstructions'
  | 'calendarFeeds'
  | 'subscription'
  | 'payments';

const tabs = [
  { id: 'team' as const, label: 'Team Members', icon: Users },
  {
    id: 'teamPreferences' as const,
    label: 'Team Preferences',
    icon: Building,
  },
  {
    id: 'paymentInstructions' as const,
    label: 'Payment Instructions',
    icon: CreditCard,
  },
  {
    id: 'calendarFeeds' as const,
    label: 'Calendar Feeds',
    icon: Calendar,
  },
  { id: 'subscription' as const, label: 'Subscription', icon: CreditCard },
  { id: 'payments' as const, label: 'Billing', icon: Receipt },
];

export const TeamSettingsPage = () => {
  const [activeTab, setActiveTab] = useState<TeamSettingsTab>('team');
  const [showBanner, setShowBanner] = useState(true);
  const { activeTeam, canEditTeamSettings, isLoading } = useTeam();
  const navigate = useNavigate();

  useEffect(() => {
    if (!isLoading && !canEditTeamSettings) {
      navigate('/dashboard', { replace: true });
    }
  }, [isLoading, canEditTeamSettings, navigate]);

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
                  Team Settings - Work in Progress
                </h3>
                <p className="text-[#3d4463] dark:text-[#c4c8db] text-sm leading-relaxed">
                  Some features are fully functional while others are getting
                  their final polish. Backend integration coming soon!
                </p>
              </div>
              <button
                onClick={() => setShowBanner(false)}
                className="flex-shrink-0 p-2 rounded-lg hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] transition-colors group"
                aria-label="Dismiss banner"
              >
                <X className="h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180] group-hover:text-[#6b7194] dark:group-hover:text-[#c4c8db]" />
              </button>
            </div>
          </div>
        )}

        {/* Header */}
        <div className="mb-8">
          <div className="mb-6">
            <div className="flex items-center gap-3 mb-1">
              <Building className="h-8 w-8 text-primary-500 dark:text-primary-300" />
              <h1 className="text-3xl font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
                Team Settings
              </h1>
            </div>
            <p className="text-[#6b7194] dark:text-[#8b90a8] ml-11">
              Manage your team configuration, members, and billing
            </p>
          </div>

          {/* Active Team Info Bar */}
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
                  {activeTeam?.isOwner && ' \u2022 Owner'}
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
          {activeTab === 'team' &&
            (canEditTeamSettings ? (
              <TeamSettingsSection />
            ) : (
              <PermissionDenied section="Team Management" />
            ))}
          {activeTab === 'teamPreferences' &&
            (canEditTeamSettings ? (
              <TeamPreferencesSection />
            ) : (
              <PermissionDenied section="Team Preferences" />
            ))}
          {activeTab === 'paymentInstructions' && (
            <PaymentInstructionsSection />
          )}
          {activeTab === 'calendarFeeds' &&
            (canEditTeamSettings ? (
              <CalendarFeedsSection />
            ) : (
              <PermissionDenied section="Calendar Feeds" />
            ))}
          {activeTab === 'subscription' &&
            (canEditTeamSettings ? (
              <SubscriptionSection />
            ) : (
              <PermissionDenied section="Subscription Management" />
            ))}
          {activeTab === 'payments' &&
            (canEditTeamSettings ? (
              <PaymentHistorySection />
            ) : (
              <PermissionDenied section="Billing History" />
            ))}
        </div>
      </div>
    </div>
  );
};

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
