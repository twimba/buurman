import { useState, useEffect } from 'react';
import { X, Save } from 'lucide-react';
import {
  ContactAddressResponse,
  CreateContactAddressRequest,
  UpdateContactAddressRequest,
  AddressType,
  AddressStatus,
} from '@/types/contact';
import { InteractiveMap } from '../common/InteractiveMap';
import { CountrySelector } from '../common/CountrySelector';
import { useTeamDefaults } from '@/hooks/useTeamDefaults';
import { useGeocode } from '@/hooks/useGeocodingHooks';
import { useTranslation } from 'react-i18next';

interface AddressFormProps {
  address?: ContactAddressResponse;
  onSubmit: (
    data: CreateContactAddressRequest | UpdateContactAddressRequest
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
  const { t } = useTranslation('tenants');
  const { defaultCountryCode } = useTeamDefaults();
  const [errors, setErrors] = useState<Record<string, string>>({});
  const geocodeMutation = useGeocode();
  const [addressDirty, setAddressDirty] = useState(false);

  const [formData, setFormData] = useState({
    street: address?.street ?? '',
    city: address?.city ?? '',
    postalCode: address?.postalCode ?? '',
    countryCode: address?.countryCode || defaultCountryCode || '',
    addressType: address?.addressType ?? AddressType.CURRENT,
    status: address?.status ?? AddressStatus.ACTIVE,
    latitude: address?.latitude ?? undefined,
    longitude: address?.longitude ?? undefined,
    geocodeAccuracy: address?.geocodeAccuracy ?? undefined,
  });

  // Debounce address changes for geocoding via backend (2 seconds)
  useEffect(() => {
    if (formData.street && formData.city && formData.countryCode) {
      const hasChanged = address
        ? formData.street !== address.street ||
          formData.city !== address.city ||
          formData.postalCode !== address.postalCode ||
          formData.countryCode !== address.countryCode
        : true;

      if (hasChanged) {
        setAddressDirty(true);
      }
    }

    const timeoutId = setTimeout(() => {
      if (formData.street && formData.city && formData.countryCode) {
        const hasChanged = address
          ? formData.street !== address.street ||
            formData.city !== address.city ||
            formData.postalCode !== address.postalCode ||
            formData.countryCode !== address.countryCode
          : true;

        if (hasChanged) {
          geocodeMutation.mutate(
            {
              street: formData.street,
              city: formData.city,
              postalCode: formData.postalCode,
              countryCode: formData.countryCode,
            },
            {
              onSuccess: (result) => {
                setAddressDirty(false);
                if (result) {
                  setFormData((prev) => ({
                    ...prev,
                    latitude: result.latitude,
                    longitude: result.longitude,
                    geocodeAccuracy: result.accuracy ?? undefined,
                  }));
                } else {
                  setFormData((prev) => ({
                    ...prev,
                    latitude: undefined,
                    longitude: undefined,
                    geocodeAccuracy: undefined,
                  }));
                }
              },
              onError: () => {
                setAddressDirty(false);
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
    formData.countryCode,
    address,
  ]);

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!formData.street.trim()) {
      newErrors.street = t('addresses.validation.streetRequired');
    }
    if (!formData.city.trim()) {
      newErrors.city = t('addresses.validation.cityRequired');
    }
    if (!formData.countryCode.trim()) {
      newErrors.countryCode = t('addresses.validation.countryRequired');
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const submitForm = async () => {
    if (!validate()) {
      return;
    }

    try {
      await onSubmit(formData);
    } catch {
      // Mutation error handled by React Query onError
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
    [AddressType.CURRENT]: t('addresses.types.CURRENT'),
    [AddressType.MAILING]: t('addresses.types.MAILING'),
    [AddressType.RELATIVE]: t('addresses.types.RELATIVE'),
    [AddressType.WORK]: t('addresses.types.WORK'),
    [AddressType.HISTORIC]: t('addresses.types.HISTORIC'),
  };

  return (
    <form
      onSubmit={handleSubmit}
      onKeyDown={handleCmdEnter}
      className="space-y-6 bg-surface-card border border-border-default rounded-lg p-6"
    >
      <div className="flex justify-between items-center">
        <h3 className="text-lg font-semibold text-text-primary">
          {address ? t('addresses.editAddress') : t('addresses.addNewAddress')}
        </h3>
        <button
          type="button"
          onClick={onCancel}
          className="text-text-muted hover:text-text-secondary"
        >
          <X className="h-5 w-5" />
        </button>
      </div>

      {/* Address Fields */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label className="block text-sm font-medium text-text-secondary mb-1">
            Street <span className="text-error-text">*</span>
          </label>
          <input
            type="text"
            value={formData.street}
            onChange={(e) => handleChange('street', e.target.value)}
            className="w-full border border-border-strong rounded px-3 py-2 focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
            placeholder="Main Street 123"
          />
          {errors.street && (
            <p className="text-error-text text-sm mt-1">{errors.street}</p>
          )}
        </div>

        <div>
          <label className="block text-sm font-medium text-text-secondary mb-1">
            City <span className="text-error-text">*</span>
          </label>
          <input
            type="text"
            value={formData.city}
            onChange={(e) => handleChange('city', e.target.value)}
            className="w-full border border-border-strong rounded px-3 py-2 focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
            placeholder="Amsterdam"
          />
          {errors.city && (
            <p className="text-error-text text-sm mt-1">{errors.city}</p>
          )}
        </div>

        <div>
          <label className="block text-sm font-medium text-text-secondary mb-1">
            Postal Code
          </label>
          <input
            type="text"
            value={formData.postalCode}
            onChange={(e) => handleChange('postalCode', e.target.value)}
            className="w-full border border-border-strong rounded px-3 py-2 focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
            placeholder="1012 AB"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-text-secondary mb-1">
            Country <span className="text-error-text">*</span>
          </label>
          <CountrySelector
            value={formData.countryCode}
            onChange={(v) => handleChange('countryCode', v)}
          />
          {errors.countryCode && (
            <p className="text-error-text text-sm mt-1">{errors.countryCode}</p>
          )}
        </div>

        {/* Address Type Selector */}
        {showTypeSelector && (
          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">
              Address Type <span className="text-error-text">*</span>
            </label>
            <select
              value={formData.addressType}
              onChange={(e) =>
                handleChange('addressType', e.target.value as AddressType)
              }
              className="w-full border border-border-strong rounded px-3 py-2 focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
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
            <label className="block text-sm font-medium text-text-secondary mb-1">
              Status <span className="text-error-text">*</span>
            </label>
            <select
              value={formData.status}
              onChange={(e) =>
                handleChange('status', e.target.value as AddressStatus)
              }
              className="w-full border border-border-strong rounded px-3 py-2 focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
            >
              <option value={AddressStatus.ACTIVE}>
                {t('common:status.active')}
              </option>
              <option value={AddressStatus.INACTIVE}>
                {t('common:status.inactive')}
              </option>
            </select>
          </div>
        )}
      </div>

      {/* Location Preview */}
      {formData.street && formData.city && formData.countryCode && (
        <div>
          <h4 className="text-sm font-semibold text-text-secondary mb-3">
            Location Preview
          </h4>
          <InteractiveMap
            street={formData.street}
            city={formData.city}
            latitude={formData.latitude}
            longitude={formData.longitude}
            geocodeAccuracy={formData.geocodeAccuracy}
            isGeocoding={addressDirty || geocodeMutation.isPending}
            defaultCountryCode={formData.countryCode || defaultCountryCode}
            onLocationChange={(lat, lng) => {
              setFormData((prev) => ({
                ...prev,
                latitude: lat,
                longitude: lng,
                geocodeAccuracy: 'MANUAL',
              }));
            }}
          />
        </div>
      )}

      {/* Action Buttons */}
      <div className="flex justify-end gap-3">
        <button
          type="button"
          onClick={onCancel}
          disabled={isLoading}
          className="px-4 py-2 text-text-secondary bg-surface-card border border-border-strong rounded hover:bg-surface-inset disabled:opacity-50"
        >
          Cancel
        </button>
        <button
          type="submit"
          disabled={isLoading}
          className="px-4 py-2 bg-primary-500 text-white rounded hover:bg-primary-600 disabled:opacity-50 flex items-center gap-2"
        >
          <Save className="h-4 w-4" />
          {isLoading ? t('form.saving') : t('addresses.saveAddress')}
        </button>
      </div>
    </form>
  );
};
