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
    name: tenant?.name || '',
    email: tenant?.email || '',
    phone: tenant?.phone || '',
    taxNumber: tenant?.taxNumber || '',
    idNumber: tenant?.idNumber || '',
    additionalInfo: tenant?.additionalInfo || '',
  });

  useEffect(() => {
    if (tenant) {
      setFormData({
        name: tenant.name,
        email: tenant.email,
        phone: tenant.phone || '',
        taxNumber: tenant.taxNumber || '',
        idNumber: tenant.idNumber || '',
        additionalInfo: tenant.additionalInfo || '',
      });
    }
  }, [tenant]);

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!formData.name.trim()) newErrors.name = 'Name is required';
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
        <h3 className="text-lg font-semibold text-gray-900 mb-4">
          Personal Information
        </h3>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Name <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              value={formData.name}
              onChange={(e) => handleChange('name', e.target.value)}
              className="w-full border border-gray-300 rounded px-3 py-2 focus:border-blue-600 focus:ring-1 focus:ring-blue-600"
              placeholder="John Doe"
            />
            {errors.name && (
              <p className="text-red-600 text-sm mt-1">{errors.name}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Email <span className="text-red-500">*</span>
            </label>
            <input
              type="email"
              value={formData.email}
              onChange={(e) => handleChange('email', e.target.value)}
              className="w-full border border-gray-300 rounded px-3 py-2 focus:border-blue-600 focus:ring-1 focus:ring-blue-600"
              placeholder="john@example.com"
            />
            {errors.email && (
              <p className="text-red-600 text-sm mt-1">{errors.email}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Phone
            </label>
            <input
              type="tel"
              value={formData.phone || ''}
              onChange={(e) => handleChange('phone', e.target.value)}
              className="w-full border border-gray-300 rounded px-3 py-2 focus:border-blue-600 focus:ring-1 focus:ring-blue-600"
              placeholder="+31 6 1234 5678"
            />
          </div>

        </div>
      </div>

      {/* Additional Information */}
      <div>
        <h3 className="text-lg font-semibold text-gray-900 mb-4">
          Additional Information
        </h3>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Notes
          </label>
          <RichTextEditor
            value={formData.additionalInfo || ''}
            onChange={(value) => handleChange('additionalInfo', value)}
            placeholder="Add any additional information about the tenant (tax number, ID number, notes, etc.)"
          />
        </div>
      </div>

      {/* Actions */}
      <div className="flex gap-2 justify-end mt-6 pt-6 border-t">
        <button
          type="button"
          onClick={() => navigate('/tenants')}
          className="border border-gray-300 px-4 py-2 rounded hover:bg-gray-50 transition-colors flex items-center gap-2"
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
