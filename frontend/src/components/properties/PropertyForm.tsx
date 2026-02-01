import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { X, Save } from 'lucide-react';
import {
  PropertyResponse,
  CreatePropertyRequest,
  PropertyType,
  PropertyStatus,
} from '@/types/property';

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
    bedrooms: property?.bedrooms || null,
    bathrooms: property?.bathrooms || null,
    squareMeters: property?.squareMeters || null,
    propertyType: property?.propertyType || PropertyType.APARTMENT,
    status: property?.status || PropertyStatus.VACANT,
  });

  useEffect(() => {
    if (property) {
      setFormData({
        street: property.street,
        city: property.city,
        postalCode: property.postalCode,
        country: property.country,
        bedrooms: property.bedrooms,
        bathrooms: property.bathrooms,
        squareMeters: property.squareMeters,
        propertyType: property.propertyType,
        status: property.status,
      });
    }
  }, [property]);

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
      navigate('/properties');
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
              className="w-full border border-gray-300 rounded px-3 py-2 focus:border-blue-600 focus:ring-1 focus:ring-blue-600"
            >
              <option value="Netherlands">Netherlands</option>
              <option value="Belgium">Belgium</option>
              <option value="Germany">Germany</option>
              <option value="France">France</option>
              <option value="Spain">Spain</option>
              <option value="Italy">Italy</option>
              <option value="Portugal">Portugal</option>
              <option value="United Kingdom">United Kingdom</option>
              <option value="Ireland">Ireland</option>
              <option value="Switzerland">Switzerland</option>
              <option value="Austria">Austria</option>
              <option value="Denmark">Denmark</option>
              <option value="Sweden">Sweden</option>
              <option value="Norway">Norway</option>
              <option value="Poland">Poland</option>
              <option value="Czech Republic">Czech Republic</option>
              <option value="Other">Other</option>
            </select>
            {errors.country && (
              <p className="text-red-600 text-sm mt-1">{errors.country}</p>
            )}
          </div>
        </div>
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
