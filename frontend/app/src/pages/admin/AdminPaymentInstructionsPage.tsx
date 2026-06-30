import { CreditCard } from 'lucide-react';
import { PaymentInstructionsSection } from '@/components/settings/PaymentInstructionsSection';
import { useTranslation } from 'react-i18next';

export const AdminPaymentInstructionsPage = () => {
  const { t } = useTranslation('admin');
  return (
    <div className="min-h-full bg-background">
      <div className="px-4 py-8">
        <div className="mb-8">
          <div className="flex items-center gap-3 mb-1">
            <CreditCard className="h-8 w-8 text-primary-500 dark:text-primary-300" />
            <h1 className="text-3xl font-bold text-text-primary">
              {t('paymentInstructions.title')}
            </h1>
          </div>
          <p className="text-text-secondary ml-11">
            {t('paymentInstructions.pageSubtitle')}
          </p>
        </div>
        <PaymentInstructionsSection />
      </div>
    </div>
  );
};
