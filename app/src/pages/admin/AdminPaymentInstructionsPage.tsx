import { CreditCard } from 'lucide-react';
import { PaymentInstructionsSection } from '@/components/settings/PaymentInstructionsSection';

export const AdminPaymentInstructionsPage = () => {
  return (
    <div className="min-h-screen bg-background">
      <div className="px-4 py-8">
        <div className="mb-8">
          <div className="flex items-center gap-3 mb-1">
            <CreditCard className="h-8 w-8 text-primary-500 dark:text-primary-300" />
            <h1 className="text-3xl font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
              Payment Instructions
            </h1>
          </div>
          <p className="text-[#6b7194] dark:text-[#8b90a8] ml-11">
            Manage payment instruction templates for your team
          </p>
        </div>
        <PaymentInstructionsSection />
      </div>
    </div>
  );
};
