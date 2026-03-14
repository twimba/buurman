import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTabState } from '@/hooks/useTabState';
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
  const [activeTab, setActiveTab] = useTabState<TeamSettingsTab>('team', [
    'team',
    'teamPreferences',
    'paymentInstructions',
    'calendarFeeds',
    'subscription',
    'payments',
  ] as const);
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
            <div className="relative bg-surface-card rounded-2xl p-6 flex items-start gap-4">
              <div className="flex-shrink-0">
                <div className="relative">
                  <Construction className="h-8 w-8 text-primary-500 animate-bounce" />
                  <Sparkles className="h-4 w-4 text-warning-text absolute -top-1 -right-1 animate-pulse" />
                </div>
              </div>
              <div className="flex-1">
                <h3 className="text-lg font-bold text-text-primary mb-1">
                  Team Settings - Work in Progress
                </h3>
                <p className="text-text-secondary text-sm leading-relaxed">
                  Some features are fully functional while others are getting
                  their final polish. Backend integration coming soon!
                </p>
              </div>
              <button
                onClick={() => setShowBanner(false)}
                className="flex-shrink-0 p-2 rounded-lg hover:bg-surface-inset transition-colors group"
                aria-label="Dismiss banner"
              >
                <X className="h-5 w-5 text-text-muted group-hover:text-text-secondary " />
              </button>
            </div>
          </div>
        )}

        {/* Header */}
        <div className="mb-8">
          <div className="mb-6">
            <div className="flex items-center gap-3 mb-1">
              <Building className="h-8 w-8 text-primary-500 dark:text-primary-300" />
              <h1 className="text-3xl font-bold text-text-primary">
                Team Settings
              </h1>
            </div>
            <p className="text-text-secondary ml-11">
              Manage your team configuration, members, and billing
            </p>
          </div>

          {/* Active Team Info Bar */}
          <div className="mb-6 flex items-center justify-between p-4 bg-info-bg border border-info-border rounded-lg">
            <div className="flex items-center gap-3">
              <Shield className="h-5 w-5 text-primary-500 dark:text-primary-300" />
              <div>
                <p className="text-sm font-semibold text-text-primary">
                  Managing: {activeTeam?.name}
                </p>
                <p className="text-xs text-text-secondary">
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
              <div className="flex items-center gap-2 px-3 py-1.5 bg-warning-bg border border-warning-border rounded-lg">
                <AlertCircle className="h-4 w-4 text-warning-text" />
                <p className="text-xs font-medium text-warning-text">
                  View-only access
                </p>
              </div>
            )}
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
    <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-12 text-center">
      <div className="flex justify-center mb-4">
        <div className="h-16 w-16 rounded-full bg-warning-bg flex items-center justify-center">
          <Shield className="h-8 w-8 text-warning-text" />
        </div>
      </div>
      <h3 className="text-xl font-semibold text-text-primary mb-2">
        Admin Access Required
      </h3>
      <p className="text-text-secondary mb-4">
        You don&apos;t have permission to access <strong>{section}</strong> for
        this team.
      </p>
      <p className="text-sm text-text-secondary">
        Only team administrators can manage these settings. Contact your team
        owner if you need access.
      </p>
    </div>
  );
};
