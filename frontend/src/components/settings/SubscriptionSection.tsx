import { useState } from 'react';
import {
  CreditCard,
  Check,
  ArrowRight,
  Calendar,
  Zap,
  Plus,
  Edit,
  Trash2,
} from 'lucide-react';

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
  const [showUpgradeModal, setShowUpgradeModal] = useState(false);
  const [selectedPlan, setSelectedPlan] = useState<string | null>(null);

  // Mock data - will be replaced with actual subscription data
  const currentSubscription = {
    planName: 'Big',
    planPrice: 2,
    currency: 'EUR',
    billingPeriod: 'month',
    nextRenewal: '2026-03-01',
    status: 'active',
    propertiesLimit: 3,
    currentProperties: 2,
    teamMembersLimit: 5,
    documentsLimit: 500,
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
      price: 0,
      priceLabel: '€0/forever',
      properties: 1,
      teamMembers: 1,
      documents: 0,
      features: [
        '1 Property',
        '1 Team Member',
        '0 Documents (coming soon)',
        'Essential Features',
      ],
      badge: '🎁 FREE',
    },
    {
      id: 'basic',
      name: 'Basic',
      price: 1,
      priceLabel: '€1/month',
      properties: 1,
      teamMembers: 1,
      documents: 100,
      features: [
        '1 Property',
        '1 Team Member',
        '100 Documents',
        'All Core Features',
      ],
    },
    {
      id: 'big',
      name: 'Big',
      price: 2,
      priceLabel: '€2/month',
      properties: 3,
      teamMembers: 5,
      documents: 500,
      features: [
        '3 Properties',
        '5 Team Members',
        '500 Documents',
        'Priority Support',
        'Advanced Reports',
      ],
      popular: true,
    },
    {
      id: 'mega',
      name: 'Mega',
      price: 10,
      priceLabel: '€10/month',
      properties: 15,
      teamMembers: -1,
      documents: 3000,
      features: [
        '15 Properties',
        'Unlimited Team Members',
        '3,000 Documents',
        'Priority Support',
        'Advanced Reports',
        'Custom Features',
      ],
    },
  ];

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
      {/* Current Plan Card */}
      <div className="bg-white rounded-lg shadow">
        <div className="p-6 border-b border-gray-200">
          <h2 className="text-xl font-semibold text-gray-900">
            Current Subscription
          </h2>
          <p className="text-sm text-gray-600 mt-1">
            Manage your subscription plan and billing
          </p>
        </div>

        <div className="p-6">
          <div className="flex items-start justify-between">
            <div>
              <div className="flex items-center gap-3">
                <h3 className="text-2xl font-bold text-gray-900">
                  {currentSubscription.planName}
                </h3>
                <span className="px-3 py-1 bg-green-100 text-green-800 text-sm font-semibold rounded">
                  {currentSubscription.status === 'active' ? 'Active' : currentSubscription.status}
                </span>
              </div>
              <p className="text-3xl font-bold text-blue-600 mt-2">
                €{currentSubscription.planPrice}
                <span className="text-lg font-normal text-gray-600">
                  /{currentSubscription.billingPeriod}
                </span>
              </p>
            </div>
          </div>

          <div className="mt-6 grid grid-cols-3 gap-4">
            <div className="p-4 bg-gray-50 rounded-lg">
              <p className="text-sm text-gray-600">Properties</p>
              <p className="text-2xl font-semibold text-gray-900 mt-1">
                {currentSubscription.currentProperties} /{' '}
                {currentSubscription.propertiesLimit}
              </p>
              <div className="mt-2 w-full bg-gray-200 rounded-full h-2">
                <div
                  className="bg-blue-600 h-2 rounded-full"
                  style={{
                    width: `${(currentSubscription.currentProperties / currentSubscription.propertiesLimit) * 100}%`,
                  }}
                />
              </div>
            </div>

            <div className="p-4 bg-gray-50 rounded-lg">
              <p className="text-sm text-gray-600">Team Members</p>
              <p className="text-2xl font-semibold text-gray-900 mt-1">
                3 / {currentSubscription.teamMembersLimit}
              </p>
              <div className="mt-2 w-full bg-gray-200 rounded-full h-2">
                <div
                  className="bg-green-600 h-2 rounded-full"
                  style={{ width: `${(3 / currentSubscription.teamMembersLimit) * 100}%` }}
                />
              </div>
            </div>

            <div className="p-4 bg-gray-50 rounded-lg">
              <p className="text-sm text-gray-600">Next Renewal</p>
              <div className="flex items-center gap-2 mt-1">
                <Calendar className="h-5 w-5 text-gray-600" />
                <p className="text-lg font-semibold text-gray-900">
                  {new Date(currentSubscription.nextRenewal).toLocaleDateString()}
                </p>
              </div>
              <p className="text-xs text-gray-600 mt-1">Auto-renews</p>
            </div>
          </div>

          <div className="mt-6 flex gap-3">
            <button className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center justify-center gap-2">
              <Zap className="h-4 w-4" />
              Change Plan
            </button>
            <button className="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors">
              Cancel Subscription
            </button>
          </div>
        </div>
      </div>

      {/* Available Plans */}
      <div className="bg-white rounded-lg shadow">
        <div className="p-6 border-b border-gray-200">
          <h2 className="text-xl font-semibold text-gray-900">
            Available Plans
          </h2>
          <p className="text-sm text-gray-600 mt-1">
            Choose the plan that best fits your needs
          </p>
        </div>

        <div className="p-6">
          <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6">
            {plans.map((plan) => (
              <div
                key={plan.id}
                className={`relative border-2 rounded-2xl p-6 transition-all hover:shadow-lg ${
                  plan.id === 'free'
                    ? 'border-green-500 bg-gradient-to-br from-white to-green-50'
                    : plan.popular
                      ? 'border-blue-600 shadow-lg bg-gradient-to-br from-white to-blue-50'
                      : 'border-gray-200 hover:border-gray-300'
                }`}
              >
                {plan.badge && (
                  <div className="absolute -top-3 left-1/2 transform -translate-x-1/2">
                    <span className="px-3 py-1 bg-green-500 text-white text-xs font-bold rounded-full">
                      {plan.badge}
                    </span>
                  </div>
                )}
                {plan.popular && (
                  <div className="absolute -top-3 right-6">
                    <span className="px-3 py-1 bg-blue-600 text-white text-xs font-semibold rounded-full">
                      Most Popular
                    </span>
                  </div>
                )}

                <h3 className="text-2xl font-bold text-gray-900">
                  {plan.name}
                </h3>
                <div className="mt-3">
                  <span className="text-4xl font-black text-blue-600">
                    €{plan.price}
                  </span>
                  <span className="text-gray-600">
                    /{plan.id === 'free' ? 'forever' : 'month'}
                  </span>
                </div>

                <ul className="mt-6 space-y-3">
                  {plan.features.map((feature, index) => (
                    <li key={index} className="flex items-start gap-2">
                      <Check className="h-5 w-5 text-green-600 flex-shrink-0 mt-0.5" />
                      <span className="text-sm text-gray-700">{feature}</span>
                    </li>
                  ))}
                </ul>

                <button
                  onClick={() => handleUpgrade(plan.id)}
                  disabled={plan.name === currentSubscription.planName}
                  className={`mt-6 w-full px-4 py-2 rounded-lg transition-all font-semibold flex items-center justify-center gap-2 ${
                    plan.name === currentSubscription.planName
                      ? 'bg-gray-100 text-gray-400 cursor-not-allowed'
                      : plan.popular
                        ? 'bg-blue-600 text-white hover:bg-blue-700 shadow-md hover:shadow-lg'
                        : plan.id === 'free'
                          ? 'bg-green-600 text-white hover:bg-green-700'
                          : 'bg-white border-2 border-gray-300 text-gray-700 hover:bg-gray-50'
                  }`}
                >
                  {plan.name === currentSubscription.planName ? (
                    'Current Plan'
                  ) : (
                    <>
                      {plan.price === 0
                        ? 'Downgrade to Free'
                        : plan.price < currentSubscription.planPrice
                          ? 'Downgrade'
                          : 'Upgrade'}
                      <ArrowRight className="h-4 w-4" />
                    </>
                  )}
                </button>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Payment Methods */}
      <div className="bg-white rounded-lg shadow">
        <div className="p-6 border-b border-gray-200">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-xl font-semibold text-gray-900">
                Payment Methods
              </h2>
              <p className="text-sm text-gray-600 mt-1">
                Manage your payment methods
              </p>
            </div>
            <button
              onClick={() => {
                // TODO: Implement add payment method
              }}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2"
            >
              <Plus className="h-4 w-4" />
              Add Payment Method
            </button>
          </div>
        </div>

        <div className="divide-y divide-gray-200">
          {paymentMethods.map((method) => (
            <div key={method.id} className="p-6 hover:bg-gray-50">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-4">
                  <div className="h-12 w-12 rounded-lg bg-gray-100 flex items-center justify-center">
                    <CreditCard className="h-6 w-6 text-gray-600" />
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <p className="font-semibold text-gray-900">
                        {method.type === 'card'
                          ? `${method.brand} •••• ${method.last4}`
                          : `SEPA •••• ${method.last4}`}
                      </p>
                      {method.isDefault && (
                        <span className="px-2 py-0.5 bg-blue-100 text-blue-800 text-xs font-semibold rounded">
                          Default
                        </span>
                      )}
                    </div>
                    {method.type === 'card' && (
                      <p className="text-sm text-gray-600">
                        Expires {method.expiryMonth}/{method.expiryYear}
                      </p>
                    )}
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  {!method.isDefault && (
                    <button className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg transition-colors">
                      <Edit className="h-4 w-4" />
                    </button>
                  )}
                  <button className="p-2 text-red-600 hover:bg-red-50 rounded-lg transition-colors">
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
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-xl max-w-md w-full mx-4">
            <div className="p-6 border-b border-gray-200">
              <h3 className="text-xl font-semibold text-gray-900">
                Confirm Plan Change
              </h3>
            </div>

            <div className="p-6">
              <p className="text-gray-700">
                Are you sure you want to change to the{' '}
                <strong>{plans.find((p) => p.id === selectedPlan)?.name}</strong>{' '}
                plan? Your billing will be adjusted accordingly.
              </p>
              <div className="mt-4 p-4 bg-blue-50 rounded-lg">
                <p className="text-sm text-blue-800">
                  You'll be charged €
                  {plans.find((p) => p.id === selectedPlan)?.price} starting from
                  your next billing cycle.
                </p>
              </div>
            </div>

            <div className="p-6 border-t border-gray-200 flex justify-end gap-3">
              <button
                onClick={() => {
                  setShowUpgradeModal(false);
                  setSelectedPlan(null);
                }}
                className="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={confirmUpgrade}
                className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
              >
                Confirm Change
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
