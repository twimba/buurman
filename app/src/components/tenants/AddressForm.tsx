import { useState, useEffect } from 'react';
import { X, Save } from 'lucide-react';
import {
  TenantAddressResponse,
  CreateTenantAddressRequest,
  UpdateTenantAddressRequest,
  AddressType,
  AddressStatus,
} from '@/types/tenant';
import { AddressMap } from '../common/AddressMap';
import { CountrySelector } from '../common/CountrySelector';
import { useTeamDefaults } from '@/hooks/useTeamDefaults';
import { useGeocode } from '@/hooks/useGeocodingHooks';

interface AddressFormProps {
  address?: TenantAddressResponse;
  onSubmit: (
    data: CreateTenantAddressRequest | UpdateTenantAddressRequest
  ) => Promise<void>;
  onCancel: () => void;
  isLoading: boolean;
  showTypeSelector?: boolean;
  showStatusSelector?: boolean;
}

export const AddressForm = ({
  address,
  onSubmit,
  onCancel,
  isLoading,
  showTypeSelector = true,
  showStatusSelector = true,
}: AddressFormProps) => {
  const { defaultCountry } = useTeamDefaults();
  const [errors, setErrors] = useState<Record<string, string>>({});
  const geocodeMutation = useGeocode();

  const [formData, setFormData] = useState({
    street: address?.street || '',
    city: address?.city || '',
    postalCode: address?.postalCode || '',
    country: address?.country || defaultCountry || '',
    addressType: address?.addressType || AddressType.CURRENT,
    status: address?.status || AddressStatus.ACTIVE,
    latitude: address?.latitude || null,
    longitude: address?.longitude || null,
  });

  // Debounce address changes for geocoding via backend (2 seconds)
  useEffect(() => {
    const timeoutId = setTimeout(() => {
      if (formData.street && formData.city && formData.country) {
        const hasChanged = address
          ? formData.street !== address.street ||
            formData.city !== address.city ||
            formData.postalCode !== address.postalCode ||
            formData.country !== address.country
          : true;

        if (hasChanged) {
          geocodeMutation.mutate(
            {
              street: formData.street,
              city: formData.city,
              postalCode: formData.postalCode,
              country: formData.country,
            },
            {
              onSuccess: (result) => {
                if (result) {
                  setFormData((prev) => ({
                    ...prev,
                    latitude: result.latitude,
                    longitude: result.longitude,
                  }));
                }
              },
            }
          );
        }
      }
    }, 2000);

    return () => clearTimeout(timeoutId);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [
    formData.street,
    formData.city,
    formData.postalCode,
    formData.country,
    address,
  ]);

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!formData.street.trim()) newErrors.street = 'Street is required';
    if (!formData.city.trim()) newErrors.city = 'City is required';
    if (!formData.country.trim()) newErrors.country = 'Country is required';

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    try {
      await onSubmit(formData);
    } catch (error) {
      console.error('Failed to save address:', error);
    }
  };

  const handleChange = (
    field: string,
    value: string | AddressType | AddressStatus | null
  ) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    if (errors[field]) {
      setErrors((prev) => ({ ...prev, [field]: '' }));
    }
  };

  const addressTypeLabels: Record<AddressType, string> = {
    [AddressType.CURRENT]: 'Current Address',
    [AddressType.MAILING]: 'Mailing Address',
    [AddressType.RELATIVE]: 'Relative/Emergency Contact',
    [AddressType.WORK]: 'Work Address',
    [AddressType.HISTORIC]: 'Historic Address',
  };

  return (
    <form
      onSubmit={handleSubmit}
      className="space-y-6 bg-white dark:bg-[#14161f] border border-[#e2e6f0] dark:border-[#2a2e3f] rounded-lg p-6"
    >
      <div className="flex justify-between items-center">
        <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
          {address ? 'Edit Address' : 'Add New Address'}
        </h3>
        <button
          type="button"
          onClick={onCancel}
          className="text-[#9ca0b8] dark:text-[#5c6180] hover:text-[#6b7194] dark:text-[#8b90a8] dark:hover:text-[#9ca0b8] dark:text-[#5c6180]"
        >
          <X className="h-5 w-5" />
        </button>
      </div>

      {/* Address Fields */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
            Street <span className="text-red-500">*</span>
          </label>
          <input
            type="text"
            value={formData.street}
            onChange={(e) => handleChange('street', e.target.value)}
            className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa]"
            placeholder="Main Street 123"
          />
          {errors.street && (
            <p className="text-red-600 text-sm mt-1">{errors.street}</p>
          )}
        </div>

        <div>
          <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
            City <span className="text-red-500">*</span>
          </label>
          <input
            type="text"
            value={formData.city}
            onChange={(e) => handleChange('city', e.target.value)}
            className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa]"
            placeholder="Amsterdam"
          />
          {errors.city && (
            <p className="text-red-600 text-sm mt-1">{errors.city}</p>
          )}
        </div>

        <div>
          <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
            Postal Code
          </label>
          <input
            type="text"
            value={formData.postalCode}
            onChange={(e) => handleChange('postalCode', e.target.value)}
            className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa]"
            placeholder="1012 AB"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
            Country <span className="text-red-500">*</span>
          </label>
          <CountrySelector
            value={formData.country}
            onChange={(v) => handleChange('country', v)}
          />
          {errors.country && (
            <p className="text-red-600 text-sm mt-1">{errors.country}</p>
          )}
        </div>

        {/* Address Type Selector */}
        {showTypeSelector && (
          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Address Type <span className="text-red-500">*</span>
            </label>
            <select
              value={formData.addressType}
              onChange={(e) =>
                handleChange('addressType', e.target.value as AddressType)
              }
              className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa]"
            >
              {Object.values(AddressType).map((type) => (
                <option key={type} value={type}>
                  {addressTypeLabels[type]}
                </option>
              ))}
            </select>
          </div>
        )}

        {/* Status Selector */}
        {showStatusSelector && (
          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Status <span className="text-red-500">*</span>
            </label>
            <select
              value={formData.status}
              onChange={(e) =>
                handleChange('status', e.target.value as AddressStatus)
              }
              className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa]"
            >
              <option value={AddressStatus.ACTIVE}>Active</option>
              <option value={AddressStatus.INACTIVE}>Inactive</option>
            </select>
          </div>
        )}
      </div>

      {/* Location Preview */}
      {formData.street && formData.city && formData.country && (
        <div>
          <h4 className="text-sm font-semibold text-[#3d4463] dark:text-[#c4c8db] mb-3">
            Location Preview
          </h4>
          <AddressMap
            street={formData.street}
            city={formData.city}
            latitude={formData.latitude}
            longitude={formData.longitude}
          />
        </div>
      )}

      {/* Action Buttons */}
      <div className="flex justify-end gap-3">
        <button
          type="button"
          onClick={onCancel}
          disabled={isLoading}
          className="px-4 py-2 text-[#3d4463] dark:text-[#c4c8db] bg-white dark:bg-[#14161f] border border-[#c9cfd9] dark:border-[#3a3f54] rounded hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] disabled:opacity-50"
        >
          Cancel
        </button>
        <button
          type="submit"
          disabled={isLoading}
          className="px-4 py-2 bg-[#5c7cfa] text-white rounded hover:bg-[#4c6ef5] disabled:opacity-50 flex items-center gap-2"
        >
          <Save className="h-4 w-4" />
          {isLoading ? 'Saving...' : 'Save Address'}
        </button>
      </div>
    </form>
  );
};
