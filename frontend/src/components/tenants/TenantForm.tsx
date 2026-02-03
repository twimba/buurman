import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { X, Save } from 'lucide-react';
import { TenantResponse, CreateTenantRequest } from '@/types/tenant';
import { RichTextEditor } from '@/components/common/RichTextEditor';

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
  const [tenantId, setTenantId] = useState(tenant?.id);

  useEffect(() => {
    // Only update if tenant ID changed (editing a different tenant)
    /* eslint-disable react-hooks/set-state-in-effect */
    if (tenant && tenant.id !== tenantId) {
      setTenantId(tenant.id);
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
  }, [tenant, tenantId]);

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!formData.firstName.trim())
      newErrors.firstName = 'First name is required';
    if (!formData.email.trim()) newErrors.email = 'Email is required';
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      newErrors.email = 'Email must be valid';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    try {
      await onSubmit(formData);
      navigate('/tenants');
    } catch (error) {
      console.error('Failed to save tenant:', error);
    }
  };

  const handleChange = (field: keyof CreateTenantRequest, value: string) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    if (errors[field]) {
      setErrors((prev) => ({ ...prev, [field]: '' }));
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      {/* Personal Information */}
      <div>
        <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">
          Personal Information
        </h3>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              First Name <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              value={formData.firstName}
              onChange={(e) => handleChange('firstName', e.target.value)}
              className="w-full border border-gray-300 dark:border-gray-600 rounded px-3 py-2 focus:border-blue-600 focus:ring-1 focus:ring-blue-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100"
              placeholder="John"
            />
            {errors.firstName && (
              <p className="text-red-600 dark:text-red-400 text-sm mt-1">
                {errors.firstName}
              </p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Last Name
            </label>
            <input
              type="text"
              value={formData.lastName || ''}
              onChange={(e) => handleChange('lastName', e.target.value)}
              className="w-full border border-gray-300 dark:border-gray-600 rounded px-3 py-2 focus:border-blue-600 focus:ring-1 focus:ring-blue-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100"
              placeholder="Doe"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Email <span className="text-red-500">*</span>
            </label>
            <input
              type="email"
              value={formData.email}
              onChange={(e) => handleChange('email', e.target.value)}
              className="w-full border border-gray-300 dark:border-gray-600 rounded px-3 py-2 focus:border-blue-600 focus:ring-1 focus:ring-blue-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100"
              placeholder="john@example.com"
            />
            {errors.email && (
              <p className="text-red-600 dark:text-red-400 text-sm mt-1">
                {errors.email}
              </p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Phone
            </label>
            <input
              type="tel"
              value={formData.phone || ''}
              onChange={(e) => handleChange('phone', e.target.value)}
              className="w-full border border-gray-300 dark:border-gray-600 rounded px-3 py-2 focus:border-blue-600 focus:ring-1 focus:ring-blue-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100"
              placeholder="+31 6 1234 5678"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Tax Number
            </label>
            <input
              type="text"
              value={formData.taxNumber || ''}
              onChange={(e) => handleChange('taxNumber', e.target.value)}
              className="w-full border border-gray-300 dark:border-gray-600 rounded px-3 py-2 focus:border-blue-600 focus:ring-1 focus:ring-blue-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100"
              placeholder="123456789"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Government ID Number
            </label>
            <input
              type="text"
              value={formData.idNumber || ''}
              onChange={(e) => handleChange('idNumber', e.target.value)}
              className="w-full border border-gray-300 dark:border-gray-600 rounded px-3 py-2 focus:border-blue-600 focus:ring-1 focus:ring-blue-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100"
              placeholder="AB123456"
            />
          </div>
        </div>
      </div>

      {/* Additional Information */}
      <div>
        <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">
          Additional Information
        </h3>
        <div>
          <RichTextEditor
            value={formData.additionalInfo || ''}
            onChange={(value) => handleChange('additionalInfo', value)}
            placeholder="Add any additional information about the tenant"
          />
        </div>
      </div>

      {/* Actions */}
      <div className="flex gap-2 justify-end mt-6 pt-6 border-t border-gray-200 dark:border-gray-700">
        <button
          type="button"
          onClick={() => navigate('/tenants')}
          className="border border-gray-300 dark:border-gray-600 px-4 py-2 rounded hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors flex items-center gap-2 text-gray-700 dark:text-gray-300"
          disabled={isLoading}
        >
          <X className="h-4 w-4" />
          Cancel
        </button>
        <button
          type="submit"
          className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 transition-colors disabled:opacity-50 flex items-center gap-2"
          disabled={isLoading}
        >
          <Save className="h-4 w-4" />
          {isLoading ? 'Saving...' : tenant ? 'Update Tenant' : 'Create Tenant'}
        </button>
      </div>
    </form>
  );
};
