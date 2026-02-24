import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { X, Save } from 'lucide-react';
import { TenantResponse, CreateTenantRequest } from '@/types/tenant';
import { RichTextEditor } from '@/components/common/RichTextEditor';
import { PhoneInput, validatePhoneE164 } from '@/components/common/PhoneInput';

interface TenantFormProps {
  tenant?: TenantResponse;
  onSubmit: (data: CreateTenantRequest) => Promise<void>;
  isLoading: boolean;
}

export const TenantForm = ({
  tenant,
  onSubmit,
  isLoading,
}: TenantFormProps) => {
  const navigate = useNavigate();
  const [errors, setErrors] = useState<Record<string, string>>({});

  const [formData, setFormData] = useState<CreateTenantRequest>({
    firstName: tenant?.firstName || '',
    lastName: tenant?.lastName || '',
    email: tenant?.email || '',
    phone: tenant?.phone || '',
    taxNumber: tenant?.taxNumber || '',
    idNumber: tenant?.idNumber || '',
    additionalInfo: tenant?.additionalInfo || '',
  });
  const [tenantIdentifier, setTenantIdentifier] = useState(tenant?.identifier);

  useEffect(() => {
    // Only update if tenant identifier changed (editing a different tenant)
    /* eslint-disable react-hooks/set-state-in-effect */
    if (tenant && tenant.identifier !== tenantIdentifier) {
      setTenantIdentifier(tenant.identifier);
      setFormData({
        firstName: tenant.firstName,
        lastName: tenant.lastName || '',
        email: tenant.email,
        phone: tenant.phone || '',
        taxNumber: tenant?.taxNumber || '',
        idNumber: tenant?.idNumber || '',
        additionalInfo: tenant.additionalInfo || '',
      });
    }
    /* eslint-enable react-hooks/set-state-in-effect */
  }, [tenant, tenantIdentifier]);

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!formData.firstName.trim()) {
      newErrors.firstName = 'First name is required';
    }
    if (
      formData.email?.trim() &&
      !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email!)
    ) {
      newErrors.email = 'Email must be valid';
    }
    if (formData.phone) {
      const phoneErr = validatePhoneE164(formData.phone);
      if (phoneErr) {
        newErrors.phone = phoneErr;
      }
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const submitForm = async () => {
    if (!validate()) {
      return;
    }

    try {
      await onSubmit({
        ...formData,
        email: formData.email?.trim() || undefined,
        phone: formData.phone?.trim() || undefined,
      });
      navigate(tenant ? `/tenants/${tenant.identifier}` : '/tenants');
    } catch (error) {
      console.error('Failed to save tenant:', error);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await submitForm();
  };

  const handleCmdEnter = (e: React.KeyboardEvent) => {
    if ((e.metaKey || e.ctrlKey) && e.key === 'Enter') {
      e.preventDefault();
      submitForm();
    }
  };

  const handleChange = (field: keyof CreateTenantRequest, value: string) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    if (errors[field]) {
      setErrors((prev) => ({ ...prev, [field]: '' }));
    }
  };

  return (
    <form
      onSubmit={handleSubmit}
      onKeyDown={handleCmdEnter}
      className="space-y-6"
    >
      {/* Personal Information */}
      <div>
        <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
          Personal Information
        </h3>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              First Name <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              value={formData.firstName}
              onChange={(e) => handleChange('firstName', e.target.value)}
              className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa] bg-white dark:bg-[#1e2130] text-[#1a1d2e] dark:text-[#eef0f6]"
              placeholder="John"
            />
            {errors.firstName && (
              <p className="text-red-600 dark:text-red-400 text-sm mt-1">
                {errors.firstName}
              </p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Last Name
            </label>
            <input
              type="text"
              value={formData.lastName || ''}
              onChange={(e) => handleChange('lastName', e.target.value)}
              className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa] bg-white dark:bg-[#1e2130] text-[#1a1d2e] dark:text-[#eef0f6]"
              placeholder="Doe"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Email
            </label>
            <input
              type="email"
              value={formData.email}
              onChange={(e) => handleChange('email', e.target.value)}
              className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa] bg-white dark:bg-[#1e2130] text-[#1a1d2e] dark:text-[#eef0f6]"
              placeholder="john@example.com"
            />
            {errors.email && (
              <p className="text-red-600 dark:text-red-400 text-sm mt-1">
                {errors.email}
              </p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Phone
            </label>
            <PhoneInput
              value={formData.phone || null}
              onChange={(e164) => handleChange('phone', e164 || '')}
              error={errors.phone}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Tax Number
            </label>
            <input
              type="text"
              value={formData.taxNumber || ''}
              onChange={(e) => handleChange('taxNumber', e.target.value)}
              className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa] bg-white dark:bg-[#1e2130] text-[#1a1d2e] dark:text-[#eef0f6]"
              placeholder="123456789"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Government ID Number
            </label>
            <input
              type="text"
              value={formData.idNumber || ''}
              onChange={(e) => handleChange('idNumber', e.target.value)}
              className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa] bg-white dark:bg-[#1e2130] text-[#1a1d2e] dark:text-[#eef0f6]"
              placeholder="AB123456"
            />
          </div>
        </div>
      </div>

      {/* Additional Information */}
      <div>
        <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
          Additional Information
        </h3>
        <div>
          <RichTextEditor
            value={formData.additionalInfo || ''}
            onChange={(value) => handleChange('additionalInfo', value)}
            placeholder="Add any additional information about the tenant"
            onSubmit={submitForm}
          />
        </div>
      </div>

      {/* Actions */}
      <div className="flex gap-2 justify-end mt-6 pt-6 border-t border-[#e2e6f0] dark:border-[#2a2e3f]">
        <button
          type="button"
          onClick={() => navigate('/tenants')}
          className="border border-[#c9cfd9] dark:border-[#3a3f54] px-4 py-2 rounded hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] transition-colors flex items-center gap-2 text-[#3d4463] dark:text-[#c4c8db]"
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
          {isLoading ? 'Saving...' : tenant ? 'Update Tenant' : 'Create Tenant'}
        </button>
      </div>
    </form>
  );
};
