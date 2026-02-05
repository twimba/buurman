import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { X, Save } from 'lucide-react';
import {
  ContractResponse,
  CreateContractRequest,
  ContractType,
  PaymentFrequency,
} from '@/types/contract';
import { RichTextEditor } from '@/components/common/RichTextEditor';
import { CurrencySelector } from '@/components/common/CurrencySelector';
import { PropertySelector } from '@/components/common/PropertySelector';
import { TenantSelector } from '@/components/common/TenantSelector';
import { useTeamDefaults } from '@/hooks/useTeamDefaults';

interface ContractFormProps {
  contract?: ContractResponse;
  onSubmit: (data: CreateContractRequest) => Promise<void>;
  isLoading: boolean;
  prefilledPropertyId?: string;
  prefilledTenantId?: string;
}

export const ContractForm = ({
  contract,
  onSubmit,
  isLoading,
  prefilledPropertyId,
  prefilledTenantId,
}: ContractFormProps) => {
  const navigate = useNavigate();
  const { defaultCurrency } = useTeamDefaults();
  const [errors, setErrors] = useState<Record<string, string>>({});

  const [formData, setFormData] = useState<CreateContractRequest>({
    propertyIdentifier:
      prefilledPropertyId || contract?.property.identifier || '',
    tenantIdentifier: prefilledTenantId || contract?.tenant.identifier || '',
    contractType: contract?.contractType || ContractType.FIXED_TERM,
    startDate: contract?.startDate || '',
    endDate: contract?.endDate || '',
    signedDate: contract?.signedDate || '',
    rentAmount: contract?.rentAmount || 0,
    depositAmount: contract?.depositAmount || undefined,
    securityDeposit: contract?.securityDeposit || undefined,
    currency: contract?.currency || defaultCurrency || 'EUR',
    paymentFrequency: contract?.paymentFrequency || PaymentFrequency.MONTHLY,
    paymentDueDay: contract?.paymentDueDay || 1,
    autoRenewal: contract?.autoRenewal || false,
    renewalNoticeDays: contract?.renewalNoticeDays || 30,
    terminationNoticeDays: contract?.terminationNoticeDays || 30,
    lateFeePercentage: contract?.lateFeePercentage || undefined,
    termsAndConditions: contract?.termsAndConditions || '',
    notes: contract?.notes || '',
  });

  const [contractIdentifier, setContractIdentifier] = useState(
    contract?.identifier
  );

  useEffect(() => {
    // Only update if contract identifier changed (editing a different contract)
    /* eslint-disable react-hooks/set-state-in-effect */
    if (contract && contract.identifier !== contractIdentifier) {
      setContractIdentifier(contract.identifier);
      setFormData({
        propertyIdentifier: contract.property.identifier,
        tenantIdentifier: contract.tenant.identifier,
        contractType: contract.contractType,
        startDate: contract.startDate,
        endDate: contract.endDate || '',
        signedDate: contract.signedDate || '',
        rentAmount: contract.rentAmount,
        depositAmount: contract.depositAmount || undefined,
        securityDeposit: contract.securityDeposit || undefined,
        currency: contract.currency,
        paymentFrequency: contract.paymentFrequency,
        paymentDueDay: contract.paymentDueDay || 1,
        autoRenewal: contract.autoRenewal,
        renewalNoticeDays: contract.renewalNoticeDays || 30,
        terminationNoticeDays: contract.terminationNoticeDays || 30,
        lateFeePercentage: contract.lateFeePercentage || undefined,
        termsAndConditions: contract.termsAndConditions || '',
        notes: contract.notes || '',
      });
    }
    /* eslint-enable react-hooks/set-state-in-effect */
  }, [contract, contractIdentifier]);

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!formData.propertyIdentifier)
      newErrors.propertyIdentifier = 'Property is required';
    if (!formData.tenantIdentifier)
      newErrors.tenantIdentifier = 'Tenant is required';
    if (!formData.startDate) newErrors.startDate = 'Start date is required';
    if (formData.rentAmount <= 0)
      newErrors.rentAmount = 'Rent amount must be greater than 0';

    // Validate end date for FIXED_TERM contracts
    if (
      formData.contractType === ContractType.FIXED_TERM &&
      !formData.endDate
    ) {
      newErrors.endDate = 'End date is required for fixed-term contracts';
    }

    // Validate date range
    if (formData.startDate && formData.endDate) {
      if (new Date(formData.endDate) < new Date(formData.startDate)) {
        newErrors.endDate = 'End date must be on or after start date';
      }
    }

    // Validate payment due day
    if (
      formData.paymentDueDay &&
      (formData.paymentDueDay < 1 || formData.paymentDueDay > 31)
    ) {
      newErrors.paymentDueDay = 'Payment due day must be between 1 and 31';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    try {
      await onSubmit(formData);
      // Navigate to contract detail page if editing, otherwise to list
      if (contract) {
        navigate(`/contracts/${contract.identifier}`);
      } else {
        navigate('/contracts');
      }
    } catch (error) {
      console.error('Failed to save contract:', error);
    }
  };

  const handleChange = (
    field: keyof CreateContractRequest,
    value: string | number | boolean | undefined
  ) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    if (errors[field]) {
      setErrors((prev) => ({ ...prev, [field]: '' }));
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      {/* Property and Tenant Selection */}
      <div>
        <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
          Contract Parties
        </h3>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Property <span className="text-red-500">*</span>
            </label>
            <PropertySelector
              value={formData.propertyIdentifier}
              onChange={(value) => handleChange('propertyIdentifier', value)}
              disabled={isLoading}
            />
            {errors.propertyIdentifier && (
              <p className="text-red-600 text-sm mt-1">
                {errors.propertyIdentifier}
              </p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Tenant <span className="text-red-500">*</span>
            </label>
            <TenantSelector
              value={formData.tenantIdentifier}
              onChange={(value) => handleChange('tenantIdentifier', value)}
              disabled={isLoading}
            />
            {errors.tenantIdentifier && (
              <p className="text-red-600 text-sm mt-1">
                {errors.tenantIdentifier}
              </p>
            )}
          </div>
        </div>
      </div>

      {/* Contract Details */}
      <div>
        <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
          Contract Details
        </h3>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Contract Type <span className="text-red-500">*</span>
            </label>
            <select
              value={formData.contractType}
              onChange={(e) =>
                handleChange('contractType', e.target.value as ContractType)
              }
              className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa]"
              disabled={isLoading}
            >
              <option value={ContractType.FIXED_TERM}>Fixed Term</option>
              <option value={ContractType.INDEFINITE}>Indefinite</option>
              <option value={ContractType.FURNISHED}>Furnished</option>
              <option value={ContractType.UNFURNISHED}>Unfurnished</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Start Date <span className="text-red-500">*</span>
            </label>
            <div className="flex gap-2">
              <input
                type="date"
                value={formData.startDate}
                onChange={(e) => handleChange('startDate', e.target.value)}
                className="flex-1 border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa]"
                disabled={isLoading}
              />
              <button
                type="button"
                onClick={() =>
                  handleChange(
                    'startDate',
                    new Date().toISOString().split('T')[0]
                  )
                }
                className="px-3 py-2 text-sm bg-[#f1f3f9] dark:bg-[#1e2130] hover:bg-[#e8ecf4] dark:bg-[#1e2130] dark:hover:bg-[#3a3f54] border border-[#c9cfd9] dark:border-[#3a3f54] rounded-md transition-colors"
                disabled={isLoading}
              >
                Today
              </button>
            </div>
            {errors.startDate && (
              <p className="text-red-600 text-sm mt-1">{errors.startDate}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              End Date{' '}
              {formData.contractType === ContractType.FIXED_TERM && (
                <span className="text-red-500">*</span>
              )}
            </label>
            <input
              type="date"
              value={formData.endDate}
              onChange={(e) => handleChange('endDate', e.target.value)}
              className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa]"
              disabled={isLoading}
            />
            {errors.endDate && (
              <p className="text-red-600 text-sm mt-1">{errors.endDate}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Signed Date
            </label>
            <div className="flex gap-2">
              <input
                type="date"
                value={formData.signedDate}
                onChange={(e) => handleChange('signedDate', e.target.value)}
                className="flex-1 border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa]"
                disabled={isLoading}
              />
              <button
                type="button"
                onClick={() =>
                  handleChange(
                    'signedDate',
                    new Date().toISOString().split('T')[0]
                  )
                }
                className="px-3 py-2 text-sm bg-[#f1f3f9] dark:bg-[#1e2130] hover:bg-[#e8ecf4] dark:bg-[#1e2130] dark:hover:bg-[#3a3f54] border border-[#c9cfd9] dark:border-[#3a3f54] rounded-md transition-colors"
                disabled={isLoading}
              >
                Today
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Financial Terms */}
      <div>
        <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
          Financial Terms
        </h3>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Rent Amount <span className="text-red-500">*</span>
            </label>
            <input
              type="number"
              step="0.01"
              value={formData.rentAmount}
              onChange={(e) =>
                handleChange('rentAmount', parseFloat(e.target.value) || 0)
              }
              className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa]"
              placeholder="1000.00"
              disabled={isLoading}
            />
            {errors.rentAmount && (
              <p className="text-red-600 text-sm mt-1">{errors.rentAmount}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Currency
            </label>
            <CurrencySelector
              value={formData.currency}
              onChange={(value) => handleChange('currency', value)}
              disabled={isLoading}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Deposit Amount
            </label>
            <input
              type="number"
              step="0.01"
              value={formData.depositAmount || ''}
              onChange={(e) =>
                handleChange(
                  'depositAmount',
                  e.target.value ? parseFloat(e.target.value) : undefined
                )
              }
              className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa]"
              placeholder="1000.00"
              disabled={isLoading}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Security Deposit
            </label>
            <input
              type="number"
              step="0.01"
              value={formData.securityDeposit || ''}
              onChange={(e) =>
                handleChange(
                  'securityDeposit',
                  e.target.value ? parseFloat(e.target.value) : undefined
                )
              }
              className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa]"
              placeholder="1000.00"
              disabled={isLoading}
            />
          </div>
        </div>
      </div>

      {/* Payment Terms */}
      <div>
        <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
          Payment Terms
        </h3>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Payment Frequency <span className="text-red-500">*</span>
            </label>
            <select
              value={formData.paymentFrequency}
              onChange={(e) =>
                handleChange(
                  'paymentFrequency',
                  e.target.value as PaymentFrequency
                )
              }
              className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa]"
              disabled={isLoading}
            >
              <option value={PaymentFrequency.MONTHLY}>Monthly</option>
              <option value={PaymentFrequency.QUARTERLY}>Quarterly</option>
              <option value={PaymentFrequency.ANNUALLY}>Annually</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Payment Due Day (1-31)
            </label>
            <input
              type="number"
              min="1"
              max="31"
              value={formData.paymentDueDay || ''}
              onChange={(e) =>
                handleChange(
                  'paymentDueDay',
                  e.target.value ? parseInt(e.target.value) : undefined
                )
              }
              className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa]"
              placeholder="1"
              disabled={isLoading}
            />
            {errors.paymentDueDay && (
              <p className="text-red-600 text-sm mt-1">
                {errors.paymentDueDay}
              </p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Late Fee Percentage
            </label>
            <input
              type="number"
              step="0.01"
              min="0"
              max="100"
              value={formData.lateFeePercentage || ''}
              onChange={(e) =>
                handleChange(
                  'lateFeePercentage',
                  e.target.value ? parseFloat(e.target.value) : undefined
                )
              }
              className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa]"
              placeholder="2.5"
              disabled={isLoading}
            />
          </div>
        </div>
      </div>

      {/* Renewal and Termination */}
      <div>
        <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
          Renewal and Termination
        </h3>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <div className="flex items-center">
            <input
              type="checkbox"
              id="autoRenewal"
              checked={formData.autoRenewal}
              onChange={(e) => handleChange('autoRenewal', e.target.checked)}
              className="h-4 w-4 text-[#5c7cfa] focus:ring-blue-500 border-[#c9cfd9] rounded"
              disabled={isLoading}
            />
            <label
              htmlFor="autoRenewal"
              className="ml-2 block text-sm text-[#1a1d2e] dark:text-[#eef0f6]"
            >
              Auto-renewal
            </label>
          </div>

          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Renewal Notice Days
            </label>
            <input
              type="number"
              min="0"
              value={formData.renewalNoticeDays || ''}
              onChange={(e) =>
                handleChange(
                  'renewalNoticeDays',
                  e.target.value ? parseInt(e.target.value) : undefined
                )
              }
              className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa]"
              placeholder="30"
              disabled={isLoading}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Termination Notice Days
            </label>
            <input
              type="number"
              min="0"
              value={formData.terminationNoticeDays || ''}
              onChange={(e) =>
                handleChange(
                  'terminationNoticeDays',
                  e.target.value ? parseInt(e.target.value) : undefined
                )
              }
              className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa]"
              placeholder="30"
              disabled={isLoading}
            />
          </div>
        </div>
      </div>

      {/* Terms and Conditions */}
      <div>
        <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
          Terms and Conditions
        </h3>
        <div>
          <RichTextEditor
            value={formData.termsAndConditions || ''}
            onChange={(value) => handleChange('termsAndConditions', value)}
            placeholder="Enter contract terms and conditions"
          />
        </div>
      </div>

      {/* Notes */}
      <div>
        <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
          Notes
        </h3>
        <div>
          <RichTextEditor
            value={formData.notes || ''}
            onChange={(value) => handleChange('notes', value)}
            placeholder="Add any additional notes"
          />
        </div>
      </div>

      {/* Actions */}
      <div className="flex gap-2 justify-end mt-6 pt-6 border-t border-[#e2e6f0] dark:border-[#2a2e3f]">
        <button
          type="button"
          onClick={() => navigate('/contracts')}
          className="border border-[#c9cfd9] dark:border-[#3a3f54] px-4 py-2 rounded hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] transition-colors flex items-center gap-2"
          disabled={isLoading}
        >
          <X className="h-4 w-4" />
          Cancel
        </button>
        <button
          type="submit"
          className="bg-[#5c7cfa] text-white px-4 py-2 rounded hover:bg-[#4c6ef5] transition-colors disabled:opacity-50 flex items-center gap-2"
          disabled={isLoading}
        >
          <Save className="h-4 w-4" />
          {isLoading
            ? 'Saving...'
            : contract
              ? 'Update Contract'
              : 'Create Contract'}
        </button>
      </div>
    </form>
  );
};
