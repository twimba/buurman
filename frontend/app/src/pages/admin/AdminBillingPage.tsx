import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Receipt } from 'lucide-react';
import { useTeam } from '@/context/TeamContext';
import { useTranslation } from 'react-i18next';
import { SubscriptionSection } from '@/components/settings/SubscriptionSection';
import { PaymentHistorySection } from '@/components/settings/PaymentHistorySection';
import { ImpersonationGuard } from '@/components/ImpersonationGuard';

export const AdminBillingPage = () => {
  const { t } = useTranslation('admin');
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
              {t('billing.title')}
            </h1>
          </div>
          <p className="text-text-secondary ml-11">
            {t('billing.subtitle')}
          </p>
        </div>
        <ImpersonationGuard
          blockAlways
          fallback={
            <div className="text-center py-12 text-text-secondary">
              {t('billing.impersonationBlocked')}
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
