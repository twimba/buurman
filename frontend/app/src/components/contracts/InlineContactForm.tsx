import { useEffect } from 'react';
import { ArrowLeft } from 'lucide-react';
import { CreateContactRequest } from '@/types/contact';
import { PhoneInput } from '@/components/common/PhoneInput';
import {
  useDuplicateCheck,
  DuplicateContactWarning,
} from '@/components/contacts/DuplicateContactWarning';

interface InlineContactFormProps {
  value: CreateContactRequest;
  onChange: (data: CreateContactRequest) => void;
  onSwitchToSelect: () => void;
  errors: Record<string, string>;
  errorPrefix: string;
  disabled?: boolean;
  onBlockingChange?: (blocking: boolean) => void;
}

export const InlineContactForm = ({
  value,
  onChange,
  onSwitchToSelect,
  errors,
  errorPrefix,
  disabled,
  onBlockingChange,
}: InlineContactFormProps) => {
  const inputClass =
    'w-full border border-border-strong rounded px-3 py-2 focus:border-primary-500 focus:ring-1 focus:ring-primary-500 bg-surface-card text-text-primary text-sm';

  const { matches, dismissed, setDismissed, check, blocking } =
    useDuplicateCheck();

  useEffect(() => {
    onBlockingChange?.(blocking);
  }, [blocking, onBlockingChange]);

  const handleChange = (updated: CreateContactRequest) => {
    onChange(updated);
    check(updated);
  };

  return (
    <div className="rounded-lg border border-border-strong bg-surface-page p-4 space-y-3">
      <button
        type="button"
        onClick={onSwitchToSelect}
        className="flex items-center gap-1 text-xs text-primary-500 hover:text-primary-600 transition-colors"
        disabled={disabled}
      >
        <ArrowLeft className="h-3 w-3" />
        Select existing contact
      </button>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <div>
          <label className="block text-xs font-medium text-text-secondary mb-1">
            First Name <span className="text-error-text">*</span>
          </label>
          <input
            type="text"
            value={value.firstName}
            onChange={(e) =>
              handleChange({ ...value, firstName: e.target.value })
            }
            className={inputClass}
            placeholder="John"
            disabled={disabled}
          />
          {errors[`${errorPrefix}_firstName`] && (
            <p className="text-error-text text-xs mt-1">
              {errors[`${errorPrefix}_firstName`]}
            </p>
          )}
        </div>
        <div>
          <label className="block text-xs font-medium text-text-secondary mb-1">
            Last Name
          </label>
          <input
            type="text"
            value={value.lastName ?? ''}
            onChange={(e) =>
              handleChange({ ...value, lastName: e.target.value })
            }
            className={inputClass}
            placeholder="Doe"
            disabled={disabled}
          />
        </div>
        <div>
          <label className="block text-xs font-medium text-text-secondary mb-1">
            Email
          </label>
          <input
            type="email"
            value={value.email}
            onChange={(e) => handleChange({ ...value, email: e.target.value })}
            className={inputClass}
            placeholder="john@example.com"
            disabled={disabled}
          />
          {errors[`${errorPrefix}_email`] && (
            <p className="text-error-text text-xs mt-1">
              {errors[`${errorPrefix}_email`]}
            </p>
          )}
        </div>
        <div>
          <label className="block text-xs font-medium text-text-secondary mb-1">
            Phone
          </label>
          <PhoneInput
            value={value.phone ?? null}
            onChange={(e164) => handleChange({ ...value, phone: e164 ?? '' })}
            error={errors[`${errorPrefix}_phone`]}
          />
        </div>
        <div>
          <label className="block text-xs font-medium text-text-secondary mb-1">
            Tax Number
          </label>
          <input
            type="text"
            value={value.taxNumber ?? ''}
            onChange={(e) =>
              handleChange({ ...value, taxNumber: e.target.value })
            }
            className={inputClass}
            placeholder="123456789"
            disabled={disabled}
          />
        </div>
        <div>
          <label className="block text-xs font-medium text-text-secondary mb-1">
            ID Number
          </label>
          <input
            type="text"
            value={value.idNumber ?? ''}
            onChange={(e) =>
              handleChange({ ...value, idNumber: e.target.value })
            }
            className={inputClass}
            placeholder="AB123456"
            disabled={disabled}
          />
        </div>
      </div>
      <DuplicateContactWarning
        matches={matches}
        dismissed={dismissed}
        onDismiss={() => setDismissed(true)}
        compact
      />
    </div>
  );
};
