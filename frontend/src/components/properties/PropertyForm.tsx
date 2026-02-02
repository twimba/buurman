import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { X, Save } from 'lucide-react';
import {
  PropertyResponse,
  CreatePropertyRequest,
  PropertyType,
  PropertyStatus,
} from '@/types/property';
import { PropertyMap } from './PropertyMap';
import { countries } from '@/utils/countries';

interface PropertyFormProps {
  property?: PropertyResponse;
  onSubmit: (data: CreatePropertyRequest) => Promise<void>;
  isLoading: boolean;
}

export const PropertyForm = ({
  property,
  onSubmit,
  isLoading,
}: PropertyFormProps) => {
  const navigate = useNavigate();
  const [errors, setErrors] = useState<Record<string, string>>({});

  const [formData, setFormData] = useState<CreatePropertyRequest>({
    street: property?.street || '',
    city: property?.city || '',
    postalCode: property?.postalCode || '',
    country: property?.country || 'Netherlands',
    latitude: property?.latitude || null,
    longitude: property?.longitude || null,
    bedrooms: property?.bedrooms || null,
    bathrooms: property?.bathrooms || null,
    squareMeters: property?.squareMeters || null,
    propertyType: property?.propertyType || PropertyType.APARTMENT,
    status: property?.status || PropertyStatus.VACANT,
  });

  // Separate state for committed address values (used for map display)
  const [committedAddress, setCommittedAddress] = useState({
    street: property?.street || '',
    city: property?.city || '',
    postalCode: property?.postalCode || '',
    country: property?.country || 'Netherlands',
  });

  // Track if address has changed to determine if we need new coordinates
  const [shouldRegeocode, setShouldRegeocode] = useState(false);

  useEffect(() => {
    if (property) {
      setFormData({
        street: property.street,
        city: property.city,
        postalCode: property.postalCode,
        country: property.country,
        latitude: property.latitude,
        longitude: property.longitude,
        bedrooms: property.bedrooms,
        bathrooms: property.bathrooms,
        squareMeters: property.squareMeters,
        propertyType: property.propertyType,
        status: property.status,
      });
      setCommittedAddress({
        street: property.street,
        city: property.city,
        postalCode: property.postalCode,
        country: property.country,
      });
    }
  }, [property]);

  // Debounce address changes for map updates (2 seconds)
  useEffect(() => {
    const timeoutId = setTimeout(() => {
      // Check if address actually changed from the original
      const hasChanged = property
        ? formData.street !== property.street ||
          formData.city !== property.city ||
          formData.postalCode !== property.postalCode ||
          formData.country !== property.country
        : true;

      // If address changed, trigger re-geocoding but keep existing coordinates
      // until new ones are obtained
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
    property,
  ]);

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!formData.street.trim()) newErrors.street = 'Street is required';
    if (!formData.city.trim()) newErrors.city = 'City is required';
    if (!formData.postalCode.trim())
      newErrors.postalCode = 'Postal code is required';
    if (!formData.country.trim()) newErrors.country = 'Country is required';

    if (formData.bedrooms !== null && formData.bedrooms < 0) {
      newErrors.bedrooms = 'Bedrooms must be non-negative';
    }
    if (formData.bathrooms !== null && formData.bathrooms < 0) {
      newErrors.bathrooms = 'Bathrooms must be non-negative';
    }
    if (formData.squareMeters !== null && formData.squareMeters <= 0) {
      newErrors.squareMeters = 'Square meters must be greater than 0';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    try {
      await onSubmit(formData);
      // Navigate to property detail page if editing, otherwise to list
      if (property) {
        navigate(`/properties/${property.id}`);
      } else {
        navigate('/properties');
      }
    } catch (error) {
      console.error('Failed to save property:', error);
    }
  };

  const handleChange = (
    field: keyof CreatePropertyRequest,
    value: string | number | null
  ) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    if (errors[field]) {
      setErrors((prev) => ({ ...prev, [field]: '' }));
    }
  };

  // Immediately commit address values on blur
  const handleAddressBlur = () => {
    // Check if address actually changed from the original
    const hasChanged = property
      ? formData.street !== property.street ||
        formData.city !== property.city ||
        formData.postalCode !== property.postalCode ||
        formData.country !== property.country
      : true;

    // If address changed, trigger re-geocoding but keep existing coordinates
    // until new ones are obtained
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

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      {/* Address Section */}
      <div>
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Address</h3>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Street <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              value={formData.street}
              onChange={(e) => handleChange('street', e.target.value)}
              onBlur={handleAddressBlur}
              className="w-full border border-gray-300 rounded px-3 py-2 focus:border-blue-600 focus:ring-1 focus:ring-blue-600"
              placeholder="Main Street 123"
            />
            {errors.street && (
              <p className="text-red-600 text-sm mt-1">{errors.street}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              City <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              value={formData.city}
              onChange={(e) => handleChange('city', e.target.value)}
              onBlur={handleAddressBlur}
              className="w-full border border-gray-300 rounded px-3 py-2 focus:border-blue-600 focus:ring-1 focus:ring-blue-600"
              placeholder="Amsterdam"
            />
            {errors.city && (
              <p className="text-red-600 text-sm mt-1">{errors.city}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Postal Code <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              value={formData.postalCode}
              onChange={(e) => handleChange('postalCode', e.target.value)}
              onBlur={handleAddressBlur}
              className="w-full border border-gray-300 rounded px-3 py-2 focus:border-blue-600 focus:ring-1 focus:ring-blue-600"
              placeholder="1012 AB"
            />
            {errors.postalCode && (
              <p className="text-red-600 text-sm mt-1">{errors.postalCode}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Country <span className="text-red-500">*</span>
            </label>
            <select
              value={formData.country}
              onChange={(e) => handleChange('country', e.target.value)}
              onBlur={handleAddressBlur}
              className="w-full border border-gray-300 rounded px-3 py-2 focus:border-blue-600 focus:ring-1 focus:ring-blue-600"
            >
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
        </div>

        {/* Location Preview */}
        {committedAddress.street &&
          committedAddress.city &&
          committedAddress.postalCode &&
          committedAddress.country && (
            <div className="mt-6">
              <h4 className="text-sm font-semibold text-gray-700 mb-3">
                Location Preview
              </h4>
              <PropertyMap
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
      </div>

      {/* Specifications Section */}
      <div>
        <h3 className="text-lg font-semibold text-gray-900 mb-4">
          Specifications
        </h3>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Bedrooms
            </label>
            <input
              type="number"
              min="0"
              value={formData.bedrooms ?? ''}
              onChange={(e) =>
                handleChange(
                  'bedrooms',
                  e.target.value ? parseInt(e.target.value) : null
                )
              }
              className="w-full border border-gray-300 rounded px-3 py-2 focus:border-blue-600 focus:ring-1 focus:ring-blue-600"
              placeholder="2"
            />
            {errors.bedrooms && (
              <p className="text-red-600 text-sm mt-1">{errors.bedrooms}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Bathrooms
            </label>
            <input
              type="number"
              min="0"
              value={formData.bathrooms ?? ''}
              onChange={(e) =>
                handleChange(
                  'bathrooms',
                  e.target.value ? parseInt(e.target.value) : null
                )
              }
              className="w-full border border-gray-300 rounded px-3 py-2 focus:border-blue-600 focus:ring-1 focus:ring-blue-600"
              placeholder="1"
            />
            {errors.bathrooms && (
              <p className="text-red-600 text-sm mt-1">{errors.bathrooms}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Square Meters
            </label>
            <input
              type="number"
              min="0"
              step="0.01"
              value={formData.squareMeters ?? ''}
              onChange={(e) =>
                handleChange(
                  'squareMeters',
                  e.target.value ? parseFloat(e.target.value) : null
                )
              }
              className="w-full border border-gray-300 rounded px-3 py-2 focus:border-blue-600 focus:ring-1 focus:ring-blue-600"
              placeholder="75.5"
            />
            {errors.squareMeters && (
              <p className="text-red-600 text-sm mt-1">{errors.squareMeters}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Property Type <span className="text-red-500">*</span>
            </label>
            <select
              value={formData.propertyType}
              onChange={(e) =>
                handleChange('propertyType', e.target.value as PropertyType)
              }
              className="w-full border border-gray-300 rounded px-3 py-2 focus:border-blue-600 focus:ring-1 focus:ring-blue-600"
            >
              <option value={PropertyType.APARTMENT}>Apartment</option>
              <option value={PropertyType.HOUSE}>House</option>
              <option value={PropertyType.STUDIO}>Studio</option>
              <option value={PropertyType.COMMERCIAL}>Commercial</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Status <span className="text-red-500">*</span>
            </label>
            <select
              value={formData.status}
              onChange={(e) =>
                handleChange('status', e.target.value as PropertyStatus)
              }
              className="w-full border border-gray-300 rounded px-3 py-2 focus:border-blue-600 focus:ring-1 focus:ring-blue-600"
            >
              <option value={PropertyStatus.VACANT}>Vacant</option>
              <option value={PropertyStatus.OCCUPIED}>Occupied</option>
              <option value={PropertyStatus.MAINTENANCE}>Maintenance</option>
              <option value={PropertyStatus.UNAVAILABLE}>Unavailable</option>
            </select>
          </div>
        </div>
      </div>

      {/* Actions */}
      <div className="flex gap-2 justify-end mt-6 pt-6 border-t">
        <button
          type="button"
          onClick={() => navigate('/properties')}
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
          {isLoading
            ? 'Saving...'
            : property
              ? 'Update Property'
              : 'Create Property'}
        </button>
      </div>
    </form>
  );
};
