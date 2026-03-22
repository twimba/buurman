import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { X, Save } from 'lucide-react';
import { ContactResponse, CreateContactRequest } from '@/types/contact';
import { RichTextEditor } from '@/components/common/RichTextEditor';
import { PhoneInput, validatePhoneE164 } from '@/components/common/PhoneInput';

interface ContactFormProps {
  contact?: ContactResponse;
  onSubmit: (data: CreateContactRequest) => Promise<void>;
  isLoading: boolean;
}

export const ContactForm = ({
  contact,
  onSubmit,
  isLoading,
}: ContactFormProps) => {
  const navigate = useNavigate();
  const [errors, setErrors] = useState<Record<string, string>>({});

  const [formData, setFormData] = useState<CreateContactRequest>({
    firstName: contact?.firstName ?? '',
    lastName: contact?.lastName ?? '',
    email: contact?.email ?? '',
    phone: contact?.phone ?? '',
    taxNumber: contact?.taxNumber ?? '',
    idNumber: contact?.idNumber ?? '',
    additionalInfo: contact?.additionalInfo ?? '',
  });
  const [contactIdentifier, setContactIdentifier] = useState(
    contact?.identifier
  );

  useEffect(() => {
    // Only update if contact identifier changed (editing a different contact)
    /* eslint-disable react-hooks/set-state-in-effect */
    if (contact && contact.identifier !== contactIdentifier) {
      setContactIdentifier(contact.identifier);
      setFormData({
        firstName: contact.firstName,
        lastName: contact.lastName ?? '',
        email: contact.email,
        phone: contact.phone ?? '',
        taxNumber: contact?.taxNumber ?? '',
        idNumber: contact?.idNumber ?? '',
        additionalInfo: contact.additionalInfo ?? '',
      });
    }
    /* eslint-enable react-hooks/set-state-in-effect */
  }, [contact, contactIdentifier]);

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!formData.firstName.trim()) {
      newErrors.firstName = 'First name is required';
    }
    if (
      formData.email?.trim() &&
      !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email ?? '')
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
      navigate(
        contact ? `/contacts/${contact.identifier}` : '/contacts'
      );
    } catch (error) {
      console.error('Failed to save contact:', error);
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

  const handleChange = (field: keyof CreateContactRequest, value: string) => {
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
        <h3 className="text-lg font-semibold text-text-primary mb-4">
          Personal Information
        </h3>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">
              First Name <span className="text-error-text">*</span>
            </label>
            <input
              type="text"
              value={formData.firstName}
              onChange={(e) => handleChange('firstName', e.target.value)}
              className="w-full border border-border-strong rounded px-3 py-2 focus:border-primary-500 focus:ring-1 focus:ring-primary-500 bg-surface-card text-text-primary"
              placeholder="John"
            />
            {errors.firstName && (
              <p className="text-error-text text-sm mt-1">{errors.firstName}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">
              Last Name
            </label>
            <input
              type="text"
              value={formData.lastName ?? ''}
              onChange={(e) => handleChange('lastName', e.target.value)}
              className="w-full border border-border-strong rounded px-3 py-2 focus:border-primary-500 focus:ring-1 focus:ring-primary-500 bg-surface-card text-text-primary"
              placeholder="Doe"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">
              Email
            </label>
            <input
              type="email"
              value={formData.email}
              onChange={(e) => handleChange('email', e.target.value)}
              className="w-full border border-border-strong rounded px-3 py-2 focus:border-primary-500 focus:ring-1 focus:ring-primary-500 bg-surface-card text-text-primary"
              placeholder="john@example.com"
            />
            {errors.email && (
              <p className="text-error-text text-sm mt-1">{errors.email}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">
              Phone
            </label>
            <PhoneInput
              value={formData.phone ?? null}
              onChange={(e164) => handleChange('phone', e164 ?? '')}
              error={errors.phone}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">
              Tax Number
            </label>
            <input
              type="text"
              value={formData.taxNumber ?? ''}
              onChange={(e) => handleChange('taxNumber', e.target.value)}
              className="w-full border border-border-strong rounded px-3 py-2 focus:border-primary-500 focus:ring-1 focus:ring-primary-500 bg-surface-card text-text-primary"
              placeholder="123456789"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">
              Government ID Number
            </label>
            <input
              type="text"
              value={formData.idNumber ?? ''}
              onChange={(e) => handleChange('idNumber', e.target.value)}
              className="w-full border border-border-strong rounded px-3 py-2 focus:border-primary-500 focus:ring-1 focus:ring-primary-500 bg-surface-card text-text-primary"
              placeholder="AB123456"
            />
          </div>
        </div>
      </div>

      {/* Additional Information */}
      <div>
        <h3 className="text-lg font-semibold text-text-primary mb-4">
          Additional Information
        </h3>
        <div>
          <RichTextEditor
            value={formData.additionalInfo ?? ''}
            onChange={(value) => handleChange('additionalInfo', value)}
            placeholder="Add any additional information about the contact"
            onSubmit={submitForm}
          />
        </div>
      </div>

      {/* Actions */}
      <div className="flex gap-2 justify-end mt-6 pt-6 border-t border-border-default">
        <button
          type="button"
          onClick={() => navigate('/contacts')}
          className="border border-border-strong px-4 py-2 rounded hover:bg-surface-inset transition-colors flex items-center gap-2 text-text-secondary"
          disabled={isLoading}
        >
          <X className="h-4 w-4" />
          Cancel
        </button>
        <button
          type="submit"
          className="bg-primary-500 text-white px-4 py-2 rounded hover:bg-primary-600 transition-colors disabled:opacity-50 flex items-center gap-2"
          disabled={isLoading}
        >
          <Save className="h-4 w-4" />
          {isLoading
            ? 'Saving...'
            : contact
              ? 'Update Contact'
              : 'Create Contact'}
        </button>
      </div>
    </form>
  );
};
