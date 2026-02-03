import { useState, useEffect, useCallback } from 'react';
import { X, Save } from 'lucide-react';
import {
  TenantAddressResponse,
  CreateTenantAddressRequest,
  UpdateTenantAddressRequest,
  AddressType,
  AddressStatus,
} from '@/types/tenant';
import { AddressMap } from '../common/AddressMap';
import { countries } from '@/utils/countries';
import { useTeamDefaults } from '@/hooks/useTeamDefaults';

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

  // Separate state for committed address values (used for map display)
  const [committedAddress, setCommittedAddress] = useState({
    street: address?.street || '',
    city: address?.city || '',
    postalCode: address?.postalCode || '',
    country: address?.country || '',
  });

  // Track if address has changed to determine if we need new coordinates
  const [shouldRegeocode, setShouldRegeocode] = useState(false);

  // Debounce address changes for map updates (2 seconds)
  useEffect(() => {
    const timeoutId = setTimeout(() => {
      // Check if address actually changed from the original
      const hasChanged = address
        ? formData.street !== address.street ||
          formData.city !== address.city ||
          formData.postalCode !== address.postalCode ||
          formData.country !== address.country
        : true;

      // If address changed, trigger re-geocoding
      if (hasChanged) {
        setShouldRegeocode(true);
      }

      setCommittedAddress({
        street: formData.street,
        city: formData.city,
        postalCode: formData.postalCode,
        country: formData.country,
      });
    }, 2000);

    return () => clearTimeout(timeoutId);
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

  // Immediately commit address values on blur
  const handleAddressBlur = () => {
    // Check if address actually changed from the original
    const hasChanged = address
      ? formData.street !== address.street ||
        formData.city !== address.city ||
        formData.postalCode !== address.postalCode ||
        formData.country !== address.country
      : true;

    // If address changed, trigger re-geocoding
    if (hasChanged) {
      setShouldRegeocode(true);
    }

    setCommittedAddress({
      street: formData.street,
      city: formData.city,
      postalCode: formData.postalCode,
      country: formData.country,
    });
  };

  // Handle geocoded coordinates from map
  const handleCoordinatesChange = useCallback((lat: number, lng: number) => {
    setFormData((prev) => ({
      ...prev,
      latitude: lat,
      longitude: lng,
    }));
    setShouldRegeocode(false); // Reset flag once new coordinates are obtained
  }, []);

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
      className="space-y-6 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-6"
    >
      <div className="flex justify-between items-center">
        <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">
          {address ? 'Edit Address' : 'Add New Address'}
        </h3>
        <button
          type="button"
          onClick={onCancel}
          className="text-gray-400 dark:text-gray-500 hover:text-gray-600 dark:hover:text-gray-400"
        >
          <X className="h-5 w-5" />
        </button>
      </div>

      {/* Address Fields */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
            Street <span className="text-red-500">*</span>
          </label>
          <input
            type="text"
            value={formData.street}
            onChange={(e) => handleChange('street', e.target.value)}
            onBlur={handleAddressBlur}
            className="w-full border border-gray-300 dark:border-gray-600 rounded px-3 py-2 focus:border-blue-600 focus:ring-1 focus:ring-blue-600"
            placeholder="Main Street 123"
          />
          {errors.street && (
            <p className="text-red-600 text-sm mt-1">{errors.street}</p>
          )}
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
            City <span className="text-red-500">*</span>
          </label>
          <input
            type="text"
            value={formData.city}
            onChange={(e) => handleChange('city', e.target.value)}
            onBlur={handleAddressBlur}
            className="w-full border border-gray-300 dark:border-gray-600 rounded px-3 py-2 focus:border-blue-600 focus:ring-1 focus:ring-blue-600"
            placeholder="Amsterdam"
          />
          {errors.city && (
            <p className="text-red-600 text-sm mt-1">{errors.city}</p>
          )}
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
            Postal Code
          </label>
          <input
            type="text"
            value={formData.postalCode}
            onChange={(e) => handleChange('postalCode', e.target.value)}
            onBlur={handleAddressBlur}
            className="w-full border border-gray-300 dark:border-gray-600 rounded px-3 py-2 focus:border-blue-600 focus:ring-1 focus:ring-blue-600"
            placeholder="1012 AB"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
            Country <span className="text-red-500">*</span>
          </label>
          <select
            value={formData.country}
            onChange={(e) => handleChange('country', e.target.value)}
            onBlur={handleAddressBlur}
            className="w-full border border-gray-300 dark:border-gray-600 rounded px-3 py-2 focus:border-blue-600 focus:ring-1 focus:ring-blue-600"
          >
            <option value="">Select a country</option>
            {countries.map((country) => (
              <option key={country.code} value={country.name}>
                {country.flag} {country.name}
              </option>
            ))}
          </select>
          {errors.country && (
            <p className="text-red-600 text-sm mt-1">{errors.country}</p>
          )}
        </div>

        {/* Address Type Selector */}
        {showTypeSelector && (
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Address Type <span className="text-red-500">*</span>
            </label>
            <select
              value={formData.addressType}
              onChange={(e) =>
                handleChange('addressType', e.target.value as AddressType)
              }
              className="w-full border border-gray-300 dark:border-gray-600 rounded px-3 py-2 focus:border-blue-600 focus:ring-1 focus:ring-blue-600"
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
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Status <span className="text-red-500">*</span>
            </label>
            <select
              value={formData.status}
              onChange={(e) =>
                handleChange('status', e.target.value as AddressStatus)
              }
              className="w-full border border-gray-300 dark:border-gray-600 rounded px-3 py-2 focus:border-blue-600 focus:ring-1 focus:ring-blue-600"
            >
              <option value={AddressStatus.ACTIVE}>Active</option>
              <option value={AddressStatus.INACTIVE}>Inactive</option>
            </select>
          </div>
        )}
      </div>

      {/* Location Preview */}
      {committedAddress.street &&
        committedAddress.city &&
        committedAddress.country && (
          <div>
            <h4 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">
              Location Preview
            </h4>
            <AddressMap
              street={committedAddress.street}
              city={committedAddress.city}
              postalCode={committedAddress.postalCode}
              country={committedAddress.country}
              latitude={shouldRegeocode ? null : formData.latitude}
              longitude={shouldRegeocode ? null : formData.longitude}
              onCoordinatesChange={handleCoordinatesChange}
            />
          </div>
        )}

      {/* Action Buttons */}
      <div className="flex justify-end gap-3">
        <button
          type="button"
          onClick={onCancel}
          disabled={isLoading}
          className="px-4 py-2 text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50"
        >
          Cancel
        </button>
        <button
          type="submit"
          disabled={isLoading}
          className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50 flex items-center gap-2"
        >
          <Save className="h-4 w-4" />
          {isLoading ? 'Saving...' : 'Save Address'}
        </button>
      </div>
    </form>
  );
};
