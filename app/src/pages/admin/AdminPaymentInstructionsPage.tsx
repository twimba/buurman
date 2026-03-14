import { CreditCard } from 'lucide-react';
import { PaymentInstructionsSection } from '@/components/settings/PaymentInstructionsSection';

export const AdminPaymentInstructionsPage = () => {
  return (
    <div className="min-h-screen bg-background">
      <div className="px-4 py-8">
        <div className="mb-8">
          <div className="flex items-center gap-3 mb-1">
            <CreditCard className="h-8 w-8 text-primary-500 dark:text-primary-300" />
            <h1 className="text-3xl font-bold text-text-primary">
              Payment Instructions
            </h1>
          </div>
          <p className="text-text-secondary ml-11">
            Manage payment instruction templates for your team
          </p>
        </div>
        <PaymentInstructionsSection />
      </div>
    </div>
  );
};
