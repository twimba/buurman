import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Receipt } from 'lucide-react';
import { useTeam } from '@/context/TeamContext';
import { SubscriptionSection } from '@/components/settings/SubscriptionSection';
import { PaymentHistorySection } from '@/components/settings/PaymentHistorySection';
import { ImpersonationGuard } from '@/components/ImpersonationGuard';

export const AdminBillingPage = () => {
  const { canEditTeamSettings, isLoading } = useTeam();
  const navigate = useNavigate();

  useEffect(() => {
    if (!isLoading && !canEditTeamSettings) {
      navigate('/dashboard', { replace: true });
    }
  }, [isLoading, canEditTeamSettings, navigate]);

  if (isLoading || !canEditTeamSettings) {
    return null;
  }

  return (
    <div className="min-h-screen bg-background">
      <div className="px-4 py-8">
        <div className="mb-8">
          <div className="flex items-center gap-3 mb-1">
            <Receipt className="h-8 w-8 text-primary-500 dark:text-primary-300" />
            <h1 className="text-3xl font-bold text-text-primary">
              Subscription & Billing
            </h1>
          </div>
          <p className="text-text-secondary ml-11">
            Manage your subscription plan and view billing history
          </p>
        </div>
        <ImpersonationGuard
          blockAlways
          fallback={
            <div className="text-center py-12 text-text-secondary">
              Billing operations are not available during an impersonation
              session.
            </div>
          }
        >
          <div className="space-y-8">
            <SubscriptionSection />
            <PaymentHistorySection />
          </div>
        </ImpersonationGuard>
      </div>
    </div>
  );
};
