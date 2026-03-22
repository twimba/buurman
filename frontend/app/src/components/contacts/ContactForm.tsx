import { useState, useEffect, useMemo, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { X, Save } from 'lucide-react';
import {
  ContactResponse,
  ContactType,
  CONTACT_TYPE_LABELS,
  CreateContactRequest,
} from '@/types/contact';
import { RichTextEditor } from '@/components/common/RichTextEditor';
import { PhoneInput, validatePhoneE164 } from '@/components/common/PhoneInput';
import { ConfirmDialog } from '@buurman/ui';
import { trackEvent } from '@/utils/analytics';
import { AnalyticsEvent } from '@/constants/analyticsEvents';
import {
  useDuplicateCheck,
  DuplicateContactWarning,
} from '@/components/contacts/DuplicateContactWarning';

interface ContactFormProps {
  contact?: ContactResponse;
  onSubmit: (data: CreateContactRequest) => Promise<void>;
  isLoading: boolean;
}

const FIELDS_CLEARED_BY_TYPE_CHANGE: Record<string, string[]> = {
  'INDIVIDUAL->COMPANY': ['Date of birth', 'ID number', 'ID expiry date'],
  'INDIVIDUAL->SERVICE_PROVIDER': [
    'Date of birth',
    'ID number',
    'ID expiry date',
  ],
  'COMPANY->INDIVIDUAL': [
    'Company name',
    'Trade name',
    'Industry',
    'Website',
    'Invoice email',
  ],
  'SERVICE_PROVIDER->INDIVIDUAL': [
    'Company name',
    'Trade name',
    'Industry',
    'Website',
    'Invoice email',
  ],
};

const inputClass =
  'w-full border border-border-strong rounded px-3 py-2 focus:border-primary-500 focus:ring-1 focus:ring-primary-500 bg-surface-card text-text-primary';

export const ContactForm = ({
  contact,
  onSubmit,
  isLoading,
}: ContactFormProps) => {
  const navigate = useNavigate();
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [showTypeChangeDialog, setShowTypeChangeDialog] = useState(false);
  const [pendingType, setPendingType] = useState<ContactType | null>(null);
  const {
    matches: duplicateMatches,
    dismissed: dismissedDuplicates,
    setDismissed: setDismissedDuplicates,
    check: checkForDuplicates,
    blocking: duplicateBlocking,
  } = useDuplicateCheck();

  const originalType = (contact?.contactType as ContactType) ?? 'INDIVIDUAL';

  const [formData, setFormData] = useState<CreateContactRequest>(() => ({
    contactType: originalType,
    firstName: contact?.firstName ?? '',
    lastName: contact?.lastName ?? '',
    email: contact?.email ?? '',
    phone: contact?.phone ?? '',
    taxNumber: contact?.taxNumber ?? '',
    idNumber: contact?.idNumber ?? '',
    idExpiryDate: contact?.idExpiryDate ?? '',
    dateOfBirth: contact?.dateOfBirth ?? '',
    companyName: contact?.companyName ?? '',
    tradeName: contact?.tradeName ?? '',
    industry: contact?.industry ?? '',
    website: contact?.website ?? '',
    invoiceEmail: contact?.invoiceEmail ?? '',
    notes: contact?.notes ?? '',
  }));
  const [contactIdentifier, setContactIdentifier] = useState(
    contact?.identifier
  );

  useEffect(() => {
    if (contact && contact.identifier !== contactIdentifier) {
      // Sync external contact prop to local form state
      /* eslint-disable react-hooks/set-state-in-effect */
      setContactIdentifier(contact.identifier);
      setFormData({
        contactType: (contact.contactType as ContactType) ?? 'INDIVIDUAL',
        firstName: contact.firstName ?? '',
        lastName: contact.lastName ?? '',
        email: contact.email ?? '',
        phone: contact.phone ?? '',
        taxNumber: contact.taxNumber ?? '',
        idNumber: contact.idNumber ?? '',
        idExpiryDate: contact.idExpiryDate ?? '',
        dateOfBirth: contact.dateOfBirth ?? '',
        companyName: contact.companyName ?? '',
        tradeName: contact.tradeName ?? '',
        industry: contact.industry ?? '',
        website: contact.website ?? '',
        invoiceEmail: contact.invoiceEmail ?? '',
        notes: contact.notes ?? '',
      });
      /* eslint-enable react-hooks/set-state-in-effect */
    }
  }, [contact, contactIdentifier]);

  // Trigger duplicate check on relevant field changes (skip when editing)
  const triggerDuplicateCheck = useCallback(
    (data: Pick<CreateContactRequest, 'firstName' | 'lastName' | 'companyName' | 'email' | 'phone'>) => {
      if (!contact) {
        checkForDuplicates(data);
      }
    },
    [contact, checkForDuplicates]
  );

  const isIndividual = formData.contactType === 'INDIVIDUAL';
  const isCompanyLike =
    formData.contactType === 'COMPANY' ||
    formData.contactType === 'SERVICE_PROVIDER';

  const fieldsCleared = useMemo(() => {
    if (!contact || !pendingType) {
      return [];
    }
    const key = `${originalType}->${pendingType}`;
    return FIELDS_CLEARED_BY_TYPE_CHANGE[key] ?? [];
  }, [contact, originalType, pendingType]);

  const handleTypeChange = (newType: ContactType) => {
    if (contact && newType !== originalType) {
      setPendingType(newType);
      setShowTypeChangeDialog(true);
    } else {
      setFormData((prev) => ({ ...prev, contactType: newType }));
    }
  };

  const confirmTypeChange = () => {
    if (pendingType) {
      setFormData((prev) => ({ ...prev, contactType: pendingType }));
      trackEvent(AnalyticsEvent.CONTACT_TYPE_CHANGED, {
        fromType: originalType,
        toType: pendingType,
      });
      setPendingType(null);
      setShowTypeChangeDialog(false);
    }
  };

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (isIndividual) {
      if (!formData.firstName?.trim()) {
        newErrors.firstName = 'First name is required for individual contacts';
      }
    } else {
      if (!formData.companyName?.trim()) {
        newErrors.companyName =
          formData.contactType === 'SERVICE_PROVIDER'
            ? 'Business name is required'
            : 'Company name is required';
      }
    }

    if (
      formData.email?.trim() &&
      !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email ?? '')
    ) {
      newErrors.email = 'Email must be valid';
    }
    if (
      formData.invoiceEmail?.trim() &&
      !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.invoiceEmail ?? '')
    ) {
      newErrors.invoiceEmail = 'Invoice email must be valid';
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
      const payload: CreateContactRequest = {
        ...formData,
        firstName: formData.firstName?.trim() || undefined,
        lastName: formData.lastName?.trim() || undefined,
        email: formData.email?.trim() || undefined,
        phone: formData.phone?.trim() || undefined,
        companyName: formData.companyName?.trim() || undefined,
        tradeName: formData.tradeName?.trim() || undefined,
        industry: formData.industry?.trim() || undefined,
        website: formData.website?.trim() || undefined,
        invoiceEmail: formData.invoiceEmail?.trim() || undefined,
        taxNumber: formData.taxNumber?.trim() || undefined,
        idNumber: formData.idNumber?.trim() || undefined,
        dateOfBirth: formData.dateOfBirth || undefined,
        idExpiryDate: formData.idExpiryDate || undefined,
        notes: formData.notes?.trim() || undefined,
      };
      await onSubmit(payload);
      navigate(contact ? `/contacts/${contact.identifier}` : '/contacts');
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
    const updated = { ...formData, [field]: value };
    setFormData(updated);
    if (errors[field]) {
      setErrors((prev) => ({ ...prev, [field]: '' }));
    }
    if (
      ['email', 'phone', 'firstName', 'lastName', 'companyName'].includes(field)
    ) {
      triggerDuplicateCheck(updated);
    }
  };

  return (
    <>
      <form
        onSubmit={handleSubmit}
        onKeyDown={handleCmdEnter}
        className="space-y-6"
      >
        {/* Contact Type Selector */}
        <div>
          <h3 className="text-lg font-semibold text-text-primary mb-4">
            Contact Type
          </h3>
          <div className="flex gap-3">
            {(
              ['INDIVIDUAL', 'COMPANY', 'SERVICE_PROVIDER'] as ContactType[]
            ).map((type) => (
              <button
                key={type}
                type="button"
                onClick={() => handleTypeChange(type)}
                className={`px-4 py-2 rounded-lg border text-sm font-medium transition-colors ${
                  formData.contactType === type
                    ? 'border-primary-500 bg-primary-50 text-primary-700'
                    : 'border-border-strong bg-surface-card text-text-secondary hover:bg-surface-inset'
                }`}
              >
                {CONTACT_TYPE_LABELS[type]}
              </button>
            ))}
          </div>
        </div>

        {/* Duplicate Detection Warning */}
        <DuplicateContactWarning
          matches={duplicateMatches}
          dismissed={dismissedDuplicates}
          onDismiss={() => setDismissedDuplicates(true)}
        />

        {/* Name Fields */}
        <div>
          <h3 className="text-lg font-semibold text-text-primary mb-4">
            {isIndividual ? 'Personal Information' : 'Company Information'}
          </h3>
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
            {isCompanyLike && (
              <div className="lg:col-span-2">
                <label className="block text-sm font-medium text-text-secondary mb-1">
                  {formData.contactType === 'SERVICE_PROVIDER'
                    ? 'Business Name'
                    : 'Company Name'}{' '}
                  <span className="text-error-text">*</span>
                </label>
                <input
                  type="text"
                  value={formData.companyName ?? ''}
                  onChange={(e) => handleChange('companyName', e.target.value)}
                  className={inputClass}
                  placeholder={
                    formData.contactType === 'SERVICE_PROVIDER'
                      ? 'Loodgietersbedrijf Smit'
                      : 'Van der Berg Vastgoed B.V.'
                  }
                />
                {errors.companyName && (
                  <p className="text-error-text text-sm mt-1">
                    {errors.companyName}
                  </p>
                )}
              </div>
            )}

            {isCompanyLike && (
              <>
                <div>
                  <label className="block text-sm font-medium text-text-secondary mb-1">
                    Trade Name
                  </label>
                  <input
                    type="text"
                    value={formData.tradeName ?? ''}
                    onChange={(e) => handleChange('tradeName', e.target.value)}
                    className={inputClass}
                    placeholder="VdB Vastgoed"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-text-secondary mb-1">
                    Industry
                  </label>
                  <input
                    type="text"
                    value={formData.industry ?? ''}
                    onChange={(e) => handleChange('industry', e.target.value)}
                    className={inputClass}
                    placeholder="Real Estate"
                  />
                </div>
              </>
            )}

            <div>
              <label className="block text-sm font-medium text-text-secondary mb-1">
                {isCompanyLike ? 'Contact Person First Name' : 'First Name'}
                {isIndividual && <span className="text-error-text"> *</span>}
              </label>
              <input
                type="text"
                value={formData.firstName ?? ''}
                onChange={(e) => handleChange('firstName', e.target.value)}
                className={inputClass}
                placeholder="Jan"
              />
              {errors.firstName && (
                <p className="text-error-text text-sm mt-1">
                  {errors.firstName}
                </p>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-text-secondary mb-1">
                {isCompanyLike ? 'Contact Person Last Name' : 'Last Name'}
              </label>
              <input
                type="text"
                value={formData.lastName ?? ''}
                onChange={(e) => handleChange('lastName', e.target.value)}
                className={inputClass}
                placeholder="De Vries"
              />
            </div>
          </div>
        </div>

        {/* Contact Details */}
        <div>
          <h3 className="text-lg font-semibold text-text-primary mb-4">
            Contact Details
          </h3>
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-text-secondary mb-1">
                Email
              </label>
              <input
                type="email"
                value={formData.email ?? ''}
                onChange={(e) => handleChange('email', e.target.value)}
                className={inputClass}
                placeholder="jan@example.com"
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

            {isCompanyLike && (
              <>
                <div>
                  <label className="block text-sm font-medium text-text-secondary mb-1">
                    Invoice Email
                  </label>
                  <input
                    type="email"
                    value={formData.invoiceEmail ?? ''}
                    onChange={(e) =>
                      handleChange('invoiceEmail', e.target.value)
                    }
                    className={inputClass}
                    placeholder="factuur@company.nl"
                  />
                  {errors.invoiceEmail && (
                    <p className="text-error-text text-sm mt-1">
                      {errors.invoiceEmail}
                    </p>
                  )}
                </div>
                <div>
                  <label className="block text-sm font-medium text-text-secondary mb-1">
                    Website
                  </label>
                  <input
                    type="url"
                    value={formData.website ?? ''}
                    onChange={(e) => handleChange('website', e.target.value)}
                    className={inputClass}
                    placeholder="https://www.company.nl"
                  />
                </div>
              </>
            )}
          </div>
        </div>

        {/* Identification */}
        <div>
          <h3 className="text-lg font-semibold text-text-primary mb-4">
            Identification
          </h3>
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-text-secondary mb-1">
                Tax Number
              </label>
              <input
                type="text"
                value={formData.taxNumber ?? ''}
                onChange={(e) => handleChange('taxNumber', e.target.value)}
                className={inputClass}
                placeholder="123456789"
              />
            </div>

            {isIndividual && (
              <>
                <div>
                  <label className="block text-sm font-medium text-text-secondary mb-1">
                    Government ID Number
                  </label>
                  <input
                    type="text"
                    value={formData.idNumber ?? ''}
                    onChange={(e) => handleChange('idNumber', e.target.value)}
                    className={inputClass}
                    placeholder="AB123456"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-text-secondary mb-1">
                    ID Expiry Date
                  </label>
                  <input
                    type="date"
                    value={formData.idExpiryDate ?? ''}
                    onChange={(e) =>
                      handleChange('idExpiryDate', e.target.value)
                    }
                    className={inputClass}
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-text-secondary mb-1">
                    Date of Birth
                  </label>
                  <input
                    type="date"
                    value={formData.dateOfBirth ?? ''}
                    onChange={(e) =>
                      handleChange('dateOfBirth', e.target.value)
                    }
                    className={inputClass}
                  />
                </div>
              </>
            )}
          </div>
        </div>

        {/* Notes */}
        <div>
          <h3 className="text-lg font-semibold text-text-primary mb-4">
            Notes
          </h3>
          <RichTextEditor
            value={formData.notes ?? ''}
            onChange={(value) => handleChange('notes', value)}
            placeholder="Add any additional information about the contact"
            onSubmit={submitForm}
          />
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
          <div className="relative group/submit">
            <button
              type="submit"
              className="bg-primary-500 text-white px-4 py-2 rounded hover:bg-primary-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
              disabled={isLoading || (!contact && duplicateBlocking)}
            >
              <Save className="h-4 w-4" />
              {isLoading
                ? 'Saving...'
                : contact
                  ? 'Update Contact'
                  : 'Create Contact'}
            </button>
            {!contact && duplicateBlocking && (
              <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 px-3 py-2 text-xs font-medium text-white bg-neutral-800 dark:bg-neutral-700 rounded-lg whitespace-nowrap opacity-0 group-hover/submit:opacity-100 transition-opacity duration-150 shadow-lg pointer-events-none">
                Review the duplicate warning above and dismiss it to continue
                <div className="absolute top-full left-1/2 -translate-x-1/2 -mt-px border-4 border-transparent border-t-neutral-800 dark:border-t-neutral-700" />
              </div>
            )}
          </div>
        </div>
      </form>

      {/* Type Change Confirmation Dialog */}
      {showTypeChangeDialog && pendingType && (
        <ConfirmDialog
          title="Change contact type?"
          message={
            fieldsCleared.length > 0
              ? `Changing from ${CONTACT_TYPE_LABELS[originalType]} to ${CONTACT_TYPE_LABELS[pendingType]} will clear the following fields:\n\n${fieldsCleared.map((f) => `- ${f}`).join('\n')}\n\nThis cannot be undone after saving.`
              : `Change contact type from ${CONTACT_TYPE_LABELS[originalType]} to ${CONTACT_TYPE_LABELS[pendingType]}?`
          }
          variant="default"
          confirmLabel="Change Type"
          onConfirm={confirmTypeChange}
          onCancel={() => {
            setShowTypeChangeDialog(false);
            setPendingType(null);
          }}
        />
      )}
    </>
  );
};
