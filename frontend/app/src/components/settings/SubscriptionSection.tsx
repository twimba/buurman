import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  CreditCard,
  Check,
  X as XIcon,
  ArrowRight,
  Calendar,
  Zap,
  Plus,
  Edit,
  Trash2,
} from 'lucide-react';
import { useFormatDate } from '@/hooks/useFormatDate';

interface PaymentMethod {
  id: string;
  type: 'card' | 'sepa';
  last4: string;
  brand?: string;
  expiryMonth?: string;
  expiryYear?: string;
  isDefault: boolean;
}

export const SubscriptionSection = () => {
  const { t } = useTranslation('settings');
  const { formatDate } = useFormatDate();
  const [showUpgradeModal, setShowUpgradeModal] = useState(false);
  const [selectedPlan, setSelectedPlan] = useState<string | null>(null);
  const [isAnnual, setIsAnnual] = useState(true);

  // Mock data - will be replaced with actual subscription data
  const currentSubscription = {
    planName: 'Big',
    planPrice: 10,
    currency: 'EUR',
    billingPeriod: 'year' as const,
    nextRenewal: '2027-02-15',
    status: 'active',
    propertiesLimit: 5,
    currentProperties: 2,
    teamMembersLimit: 5,
    documentsLimit: 1000,
  };

  const [paymentMethods] = useState<PaymentMethod[]>([
    {
      id: '1',
      type: 'card',
      last4: '4242',
      brand: 'Visa',
      expiryMonth: '12',
      expiryYear: '2025',
      isDefault: true,
    },
    {
      id: '2',
      type: 'sepa',
      last4: '3456',
      isDefault: false,
    },
  ]);

  const plans = [
    {
      id: 'free',
      name: 'Free',
      annualPrice: 0,
      monthlyPrice: 0,
      properties: 1,
      teamMembers: 1,
      features: ['1 Property', '1 Team Member', 'Reports', 'Community Support'],
      negativeFeatures: ['No Documents or Photos', 'No SMS Notifications'],
      badge: 'FREE FOREVER',
      badgeColor: 'green' as const,
    },
    {
      id: 'basic',
      name: 'Basic',
      annualPrice: 1,
      monthlyPrice: 1.5,
      annualTotal: 12,
      properties: 1,
      teamMembers: 1,
      features: [
        '1 Property',
        '1 Team Member',
        '200 Documents or Photos',
        'Reports',
        'Email Support',
      ],
      negativeFeatures: ['No SMS Notifications'],
    },
    {
      id: 'big',
      name: 'Big',
      annualPrice: 10,
      monthlyPrice: 12.5,
      annualTotal: 120,
      properties: 5,
      teamMembers: 5,
      features: [
        '5 Properties',
        '5 Team Members',
        '1,000 Documents or Photos',
        'Reports',
        'Export to Excel',
        'SMS Notifications',
        'Email Support',
      ],
      popular: true,
      badge: 'Most Popular',
      badgeColor: 'blue' as const,
    },
    {
      id: 'mega',
      name: 'Mega',
      annualPrice: 50,
      monthlyPrice: 62.5,
      annualTotal: 600,
      properties: -1,
      teamMembers: -1,
      features: [
        'Unlimited Properties',
        'Unlimited Team Members',
        '5,000 Documents or Photos',
        'Advanced Reports',
        'Export to Excel',
        'SMS Notifications',
        'API Access',
        'Email Support',
      ],
    },
  ];

  const getPrice = (plan: (typeof plans)[number]) =>
    isAnnual ? plan.annualPrice : plan.monthlyPrice;

  const handleUpgrade = (planId: string) => {
    setSelectedPlan(planId);
    setShowUpgradeModal(true);
  };

  const confirmUpgrade = () => {
    // TODO: API call to upgrade subscription
    setShowUpgradeModal(false);
    setSelectedPlan(null);
  };

  return (
    <div className="space-y-6">
      {/* Early Access Banner */}
      <div className="relative overflow-hidden rounded-2xl bg-gradient-to-r from-primary-500 via-purple-500 to-pink-500 p-6 text-white shadow-lg">
        <div className="absolute -right-6 -top-6 text-8xl opacity-20 rotate-12 select-none">
          🎉
        </div>
        <div className="absolute -left-4 -bottom-4 text-7xl opacity-15 -rotate-12 select-none">
          🚀
        </div>
        <div className="relative">
          <h2 className="text-2xl font-extrabold">
            {t('subscription.earlyAccess.title')} 🎁
          </h2>
          <p className="mt-2 text-white/90 text-base max-w-2xl">
            We&apos;re still building Buurman and this page is just a preview of
            what&apos;s coming. For now, enjoy <strong>all features</strong>{' '}
            with zero limits and zero cost. Go wild! 🏠✨
          </p>
          <p className="mt-3 text-sm text-white/70 italic">
            We&apos;ll give you plenty of notice before billing goes live. No
            surprises, promise.
          </p>
        </div>
      </div>

      {/* Current Plan Card */}
      <div className="bg-surface-card rounded-lg shadow-sm border border-border-default">
        <div className="p-6 border-b border-border-default">
          <h2 className="text-xl font-semibold text-text-primary">
            {t('subscription.currentPlan.title')}
          </h2>
          <p className="text-sm text-text-secondary mt-1">
            {t('subscription.currentPlan.subtitle')}
          </p>
        </div>

        <div className="p-6">
          <div className="flex items-start justify-between">
            <div>
              <div className="flex items-center gap-3">
                <h3 className="text-2xl font-bold text-text-primary">
                  {currentSubscription.planName}
                </h3>
                <span className="px-3 py-1 bg-success-bg text-success-text text-sm font-semibold rounded">
                  {currentSubscription.status === 'active'
                    ? 'Active'
                    : currentSubscription.status}
                </span>
              </div>
              <p className="text-3xl font-bold text-primary-500 mt-2">
                €{currentSubscription.planPrice}
                <span className="text-lg font-normal text-text-secondary">
                  /{currentSubscription.billingPeriod}
                </span>
              </p>
            </div>
          </div>

          <div className="mt-6 grid grid-cols-3 gap-4">
            <div className="p-4 bg-surface-page rounded-lg">
              <p className="text-sm text-text-secondary">
                {t('subscription.currentPlan.properties')}
              </p>
              <p className="text-2xl font-semibold text-text-primary mt-1">
                {currentSubscription.currentProperties} /{' '}
                {currentSubscription.propertiesLimit}
              </p>
              <div className="mt-2 w-full bg-surface-inset dark:bg-neutral-700 rounded-full h-2">
                <div
                  className="bg-primary-500 h-2 rounded-full"
                  style={{
                    width: `${(currentSubscription.currentProperties / currentSubscription.propertiesLimit) * 100}%`,
                  }}
                />
              </div>
            </div>

            <div className="p-4 bg-surface-page rounded-lg">
              <p className="text-sm text-text-secondary">
                {t('subscription.currentPlan.teamMembers')}
              </p>
              <p className="text-2xl font-semibold text-text-primary mt-1">
                3 / {currentSubscription.teamMembersLimit}
              </p>
              <div className="mt-2 w-full bg-surface-inset dark:bg-neutral-700 rounded-full h-2">
                <div
                  className="bg-green-600 h-2 rounded-full"
                  style={{
                    width: `${(3 / currentSubscription.teamMembersLimit) * 100}%`,
                  }}
                />
              </div>
            </div>

            <div className="p-4 bg-surface-page rounded-lg">
              <p className="text-sm text-text-secondary">
                {t('subscription.currentPlan.nextRenewal')}
              </p>
              <div className="flex items-center gap-2 mt-1">
                <Calendar className="h-5 w-5 text-text-secondary " />
                <p className="text-lg font-semibold text-text-primary">
                  {formatDate(currentSubscription.nextRenewal)}
                </p>
              </div>
              <p className="text-xs text-text-secondary mt-1">
                {t('subscription.currentPlan.autoRenews')}
              </p>
            </div>
          </div>

          <div className="mt-6 flex gap-3">
            <button className="flex-1 px-4 py-2 bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors flex items-center justify-center gap-2">
              <Zap className="h-4 w-4" />
              {t('subscription.currentPlan.changePlan')}
            </button>
            <button className="px-4 py-2 border border-border-strong text-text-secondary rounded-lg hover:bg-surface-inset transition-colors">
              {t('subscription.currentPlan.cancelSubscription')}
            </button>
          </div>
        </div>
      </div>

      {/* Available Plans */}
      <div className="bg-surface-card rounded-lg shadow-sm border border-border-default">
        <div className="p-6 border-b border-border-default">
          <h2 className="text-xl font-semibold text-text-primary">
            {t('subscription.plans.title')}
          </h2>
          <p className="text-sm text-text-secondary mt-1">
            {t('subscription.plans.subtitle')}
          </p>
        </div>

        <div className="p-6">
          {/* Billing Toggle */}
          <div className="flex items-center justify-center gap-3 mb-8">
            <span
              className={`text-sm font-medium ${!isAnnual ? 'text-text-primary ' : 'text-text-secondary '}`}
            >
              {t('subscription.plans.monthly')}
            </span>
            <button
              onClick={() => setIsAnnual(!isAnnual)}
              className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                isAnnual ? 'bg-primary-500' : 'bg-neutral-200 dark:bg-neutral-700'
              }`}
            >
              <span
                className={`inline-block h-4 w-4 transform rounded-full bg-surface-card transition-transform ${
                  isAnnual ? 'translate-x-6' : 'translate-x-1'
                }`}
              />
            </button>
            <span
              className={`text-sm font-medium ${isAnnual ? 'text-text-primary ' : 'text-text-secondary '}`}
            >
              {t('subscription.plans.annual')}
            </span>
            {isAnnual && (
              <span className="px-2 py-0.5 text-xs font-bold bg-success-bg text-success-text rounded-full">
                {t('subscription.plans.save20')}
              </span>
            )}
          </div>

          <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6">
            {plans.map((plan) => {
              const price = getPrice(plan);
              return (
                <div
                  key={plan.id}
                  className={`relative border-2 rounded-2xl p-6 transition-all hover:shadow-lg ${
                    plan.id === 'free'
                      ? 'border-green-500 bg-gradient-to-br from-white dark:from-surface-page to-green-50 dark:to-surface-raised'
                      : plan.popular
                        ? 'border-primary-500 shadow-lg bg-gradient-to-br from-white dark:from-surface-page to-blue-50 dark:to-surface-raised'
                        : 'border-border-default hover:border-border-strong'
                  }`}
                >
                  {plan.badge && (
                    <div className="absolute -top-3 left-1/2 transform -translate-x-1/2">
                      <span
                        className={`px-3 py-1 text-white text-xs font-bold rounded-full whitespace-nowrap ${
                          plan.badgeColor === 'green'
                            ? 'bg-green-500'
                            : 'bg-primary-500'
                        }`}
                      >
                        {plan.badge}
                      </span>
                    </div>
                  )}

                  <h3 className="text-2xl font-bold text-text-primary">
                    {plan.name}
                  </h3>
                  <div className="mt-3">
                    <span className="text-4xl font-black text-primary-500 dark:text-primary-300">
                      €{price % 1 === 0 ? price : price.toFixed(2)}
                    </span>
                    <span className="text-text-secondary">
                      {plan.id === 'free'
                        ? t('subscription.plans.forever')
                        : t('subscription.plans.perMonth')}
                    </span>
                  </div>
                  {plan.id !== 'free' && (
                    <p className="text-xs text-text-secondary mt-1">
                      {isAnnual && plan.annualTotal
                        ? `Billed annually at €${plan.annualTotal}`
                        : 'Billed monthly, cancel anytime'}
                    </p>
                  )}
                  {plan.id === 'free' && (
                    <p className="text-xs text-text-secondary mt-1">
                      Perfect to get started
                    </p>
                  )}

                  <ul className="mt-6 space-y-3">
                    {plan.features.map((feature, index) => (
                      <li key={index} className="flex items-start gap-2">
                        <Check className="h-5 w-5 text-success-text flex-shrink-0 mt-0.5" />
                        <span className="text-sm text-text-secondary">
                          {feature}
                        </span>
                      </li>
                    ))}
                    {plan.negativeFeatures?.map((feature, index) => (
                      <li
                        key={`neg-${index}`}
                        className="flex items-start gap-2"
                      >
                        <XIcon className="h-5 w-5 text-error-text flex-shrink-0 mt-0.5" />
                        <span className="text-sm text-error-text">
                          {feature}
                        </span>
                      </li>
                    ))}
                  </ul>

                  <button
                    onClick={() => handleUpgrade(plan.id)}
                    disabled={plan.name === currentSubscription.planName}
                    className={`mt-6 w-full px-4 py-2 rounded-lg transition-all font-semibold flex items-center justify-center gap-2 ${
                      plan.name === currentSubscription.planName
                        ? 'bg-surface-inset text-text-muted cursor-not-allowed'
                        : plan.popular
                          ? 'bg-primary-500 text-white hover:bg-primary-600 shadow-md hover:shadow-lg'
                          : plan.id === 'free'
                            ? 'bg-green-600 text-white hover:bg-green-700'
                            : 'bg-surface-card border-2 border-border-strong text-text-secondary hover:bg-surface-inset'
                    }`}
                  >
                    {plan.name === currentSubscription.planName ? (
                      t('subscription.plans.currentPlan')
                    ) : (
                      <>
                        {price === 0
                          ? t('subscription.plans.downgradeToFree')
                          : price < currentSubscription.planPrice
                            ? t('subscription.plans.downgrade')
                            : t('subscription.plans.upgrade')}
                        <ArrowRight className="h-4 w-4" />
                      </>
                    )}
                  </button>
                </div>
              );
            })}
          </div>
        </div>
      </div>

      {/* Payment Methods */}
      <div className="bg-surface-card rounded-lg shadow-sm border border-border-default">
        <div className="p-6 border-b border-border-default">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-xl font-semibold text-text-primary">
                {t('subscription.paymentMethods.title')}
              </h2>
              <p className="text-sm text-text-secondary mt-1">
                {t('subscription.paymentMethods.subtitle')}
              </p>
            </div>
            <button
              onClick={() => {
                // TODO: Implement add payment method
              }}
              className="px-4 py-2 bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors flex items-center gap-2"
            >
              <Plus className="h-4 w-4" />
              Add Payment Method
            </button>
          </div>
        </div>

        <div className="divide-y divide-border-default">
          {paymentMethods.map((method) => (
            <div key={method.id} className="p-6 hover:bg-surface-inset">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-4">
                  <div className="h-12 w-12 rounded-lg bg-surface-inset flex items-center justify-center">
                    <CreditCard className="h-6 w-6 text-text-secondary " />
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <p className="font-semibold text-text-primary">
                        {method.type === 'card'
                          ? `${method.brand} •••• ${method.last4}`
                          : `SEPA •••• ${method.last4}`}
                      </p>
                      {method.isDefault && (
                        <span className="px-2 py-0.5 bg-info-bg text-info-text text-xs font-semibold rounded">
                          {t('subscription.paymentMethods.default')}
                        </span>
                      )}
                    </div>
                    {method.type === 'card' && (
                      <p className="text-sm text-text-secondary">
                        {t('subscription.paymentMethods.expires', {
                          month: method.expiryMonth,
                          year: method.expiryYear,
                        })}
                      </p>
                    )}
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  {!method.isDefault && (
                    <button className="p-2 text-primary-500 hover:bg-primary-50 rounded-lg transition-colors">
                      <Edit className="h-4 w-4" />
                    </button>
                  )}
                  <button className="p-2 text-error-text hover:bg-error-bg rounded-lg transition-colors">
                    <Trash2 className="h-4 w-4" />
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Upgrade Modal */}
      {showUpgradeModal && selectedPlan && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50">
          <div className="bg-surface-card rounded-lg shadow-xl max-w-md w-full mx-4">
            <div className="p-6 border-b border-border-default">
              <h3 className="text-xl font-semibold text-text-primary">
                {t('subscription.confirmModal.title')}
              </h3>
            </div>

            <div className="p-6">
              <p className="text-text-secondary">
                Are you sure you want to change to the{' '}
                <strong>
                  {plans.find((p) => p.id === selectedPlan)?.name}
                </strong>{' '}
                plan? Your billing will be adjusted accordingly.
              </p>
              <div className="mt-4 p-4 bg-info-bg rounded-lg">
                <p className="text-sm text-info-text">
                  {(() => {
                    const plan = plans.find((p) => p.id === selectedPlan);
                    if (!plan) {
                      return null;
                    }
                    const price = getPrice(plan);
                    return `You'll be charged €${price % 1 === 0 ? price : price.toFixed(2)}/month${isAnnual && plan.annualTotal ? ` (€${plan.annualTotal} billed annually)` : ''} starting from your next billing cycle.`;
                  })()}
                </p>
              </div>
            </div>

            <div className="p-6 border-t border-border-default flex justify-end gap-3">
              <button
                onClick={() => {
                  setShowUpgradeModal(false);
                  setSelectedPlan(null);
                }}
                className="px-4 py-2 border border-border-strong text-text-secondary rounded-lg hover:bg-surface-inset transition-colors"
              >
                {t('common:buttons.cancel')}
              </button>
              <button
                onClick={confirmUpgrade}
                className="px-4 py-2 bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors"
              >
                {t('subscription.confirmModal.confirmChange')}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
