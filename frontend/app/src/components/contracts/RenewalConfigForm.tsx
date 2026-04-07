import { Download } from 'lucide-react';
import { useJurisdictionDefaults } from '@/hooks/useContractExtensionHooks';
import type {
  RenewalMode,
  RentAdjustmentType,
} from '@/types/contractExtension';
import { useTranslation } from 'react-i18next';

interface RenewalConfigFormProps {
  renewalMode: RenewalMode;
  renewalTermMonths?: number;
  maxRenewals?: number;
  landlordNoticeDays?: number;
  contactNoticeDays?: number;
  requiresContactConfirmation?: boolean;
  rentAdjustmentType: RentAdjustmentType;
  rentAdjustmentValue?: number;
  regionCode?: string;
  countryCode?: string;
  onChange: (field: string, value: unknown) => void;
  disabled?: boolean;
}

export const RenewalConfigForm = ({
  renewalMode,
  renewalTermMonths,
  maxRenewals,
  landlordNoticeDays,
  contactNoticeDays,
  requiresContactConfirmation,
  rentAdjustmentType,
  rentAdjustmentValue,
  regionCode,
  countryCode,
  onChange,
  disabled = false,
}: RenewalConfigFormProps) => {
  const { t } = useTranslation('contracts');

  const RENEWAL_MODE_LABELS: Record<RenewalMode, string> = {
    NONE: t('renewalConfig.modes.NONE'),
    AUTOMATIC: t('renewalConfig.modes.AUTOMATIC'),
    MANUAL: t('renewalConfig.modes.MANUAL'),
  };

  const ADJUSTMENT_TYPE_LABELS: Record<RentAdjustmentType, string> = {
    NONE: t('renewalConfig.adjustmentTypes.NONE'),
    FIXED_PERCENTAGE: t('renewalConfig.adjustmentTypes.FIXED_PERCENTAGE'),
    FIXED_AMOUNT: t('renewalConfig.adjustmentTypes.FIXED_AMOUNT'),
    MANUAL: t('renewalConfig.adjustmentTypes.MANUAL'),
  };

  const RENEWAL_MODE_DESCRIPTIONS: Record<RenewalMode, string> = {
    NONE: '',
    AUTOMATIC: t('renewalConfig.modeDescriptions.AUTOMATIC'),
    MANUAL: t('renewalConfig.modeDescriptions.MANUAL'),
  };

  const { data: jurisdictionDefaults, isLoading: loadingDefaults } =
    useJurisdictionDefaults(
      countryCode,
      regionCode,
      undefined, // landlordType — set in country-specific section
      undefined // furnished
    );

  const handleLoadDefaults = () => {
    if (!jurisdictionDefaults) {
      return;
    }
    const d = jurisdictionDefaults.defaults;
    if (d.renewalMode) {
      onChange('renewalMode', d.renewalMode);
    }
    if (d.renewalTermMonths) {
      onChange('renewalTermMonths', parseInt(d.renewalTermMonths, 10));
    }
    if (d.landlordNoticeDays) {
      onChange('landlordNoticeDays', parseInt(d.landlordNoticeDays, 10));
    }
    if (d.contactNoticeDays) {
      onChange('contactNoticeDays', parseInt(d.contactNoticeDays, 10));
    }
    if (d.rentAdjustmentType) {
      onChange('rentAdjustmentType', d.rentAdjustmentType);
    }
    if (d.requiresContactConfirmation) {
      onChange(
        'requiresContactConfirmation',
        d.requiresContactConfirmation === 'true'
      );
    }
  };

  const isRenewalEnabled = renewalMode !== 'NONE';

  return (
    <div className="space-y-4">
      <h4 className="text-base font-semibold text-text-primary">
        {t('renewalConfig.title')}
      </h4>

      {/* Load Defaults Button */}
      {countryCode && (
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={handleLoadDefaults}
            disabled={disabled || loadingDefaults || !jurisdictionDefaults}
            className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium text-primary-500 bg-primary-500/10 rounded-md hover:bg-primary-500/20 transition-colors disabled:opacity-50"
          >
            <Download className="h-3.5 w-3.5" />
            {loadingDefaults
              ? t('common:buttons.loading')
              : t('form.loadJurisdictionDefaults')}
          </button>
          {jurisdictionDefaults?.disclaimer && (
            <p className="text-[11px] text-text-muted italic">
              {jurisdictionDefaults.disclaimer}
            </p>
          )}
        </div>
      )}

      {/* Renewal Mode */}
      <div>
        <label
          htmlFor="renewalMode"
          className="block text-sm font-medium text-text-secondary mb-1"
        >
          {t('renewalConfig.renewalMode')}
        </label>
        <select
          id="renewalMode"
          value={renewalMode}
          onChange={(e) => onChange('renewalMode', e.target.value)}
          className="w-full px-3 py-2 border border-border-strong rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500 bg-surface-card text-text-primary"
          disabled={disabled}
        >
          {Object.entries(RENEWAL_MODE_LABELS).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </select>
        {RENEWAL_MODE_DESCRIPTIONS[renewalMode] && (
          <p className="text-xs text-text-muted mt-1">
            {RENEWAL_MODE_DESCRIPTIONS[renewalMode]}
          </p>
        )}
      </div>

      {isRenewalEnabled && (
        <>
          {/* Term & Max Renewals */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label
                htmlFor="renewalTermMonths"
                className="block text-sm font-medium text-text-secondary mb-1"
              >
                {t('form.renewalTermMonths')}
              </label>
              <input
                id="renewalTermMonths"
                type="number"
                min={1}
                value={renewalTermMonths ?? ''}
                onChange={(e) =>
                  onChange(
                    'renewalTermMonths',
                    e.target.value ? parseInt(e.target.value, 10) : undefined
                  )
                }
                className="w-full px-3 py-2 border border-border-strong rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500 bg-surface-card text-text-primary"
                disabled={disabled}
                placeholder="12"
              />
            </div>
            <div>
              <label
                htmlFor="maxRenewals"
                className="block text-sm font-medium text-text-secondary mb-1"
              >
                {t('form.maxRenewals')}
              </label>
              <input
                id="maxRenewals"
                type="number"
                min={1}
                value={maxRenewals ?? ''}
                onChange={(e) =>
                  onChange(
                    'maxRenewals',
                    e.target.value ? parseInt(e.target.value, 10) : undefined
                  )
                }
                className="w-full px-3 py-2 border border-border-strong rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500 bg-surface-card text-text-primary"
                disabled={disabled}
                placeholder="Unlimited"
              />
            </div>
          </div>

          {/* Notice Days */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label
                htmlFor="landlordNoticeDays"
                className="block text-sm font-medium text-text-secondary mb-1"
              >
                {t('form.landlordNoticeDays')}
              </label>
              <input
                id="landlordNoticeDays"
                type="number"
                min={0}
                value={landlordNoticeDays ?? ''}
                onChange={(e) =>
                  onChange(
                    'landlordNoticeDays',
                    e.target.value ? parseInt(e.target.value, 10) : undefined
                  )
                }
                className="w-full px-3 py-2 border border-border-strong rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500 bg-surface-card text-text-primary"
                disabled={disabled}
                placeholder="90"
              />
            </div>
            <div>
              <label
                htmlFor="contactNoticeDays"
                className="block text-sm font-medium text-text-secondary mb-1"
              >
                {t('form.contactNoticeDays')}
              </label>
              <input
                id="contactNoticeDays"
                type="number"
                min={0}
                value={contactNoticeDays ?? ''}
                onChange={(e) =>
                  onChange(
                    'contactNoticeDays',
                    e.target.value ? parseInt(e.target.value, 10) : undefined
                  )
                }
                className="w-full px-3 py-2 border border-border-strong rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500 bg-surface-card text-text-primary"
                disabled={disabled}
                placeholder="90"
              />
            </div>
          </div>

          {/* Contact Confirmation Toggle */}
          <div className="flex items-center justify-between">
            <label
              htmlFor="requiresContactConfirmation"
              className="text-sm font-medium text-text-secondary"
            >
              {t('form.requiresContactConfirmation')}
            </label>
            <button
              type="button"
              id="requiresContactConfirmation"
              role="switch"
              aria-checked={requiresContactConfirmation ?? false}
              onClick={() =>
                onChange(
                  'requiresContactConfirmation',
                  !(requiresContactConfirmation ?? false)
                )
              }
              disabled={disabled}
              className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2 ${
                requiresContactConfirmation
                  ? 'bg-primary-500'
                  : 'bg-surface-inset'
              } ${disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'}`}
            >
              <span
                className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                  requiresContactConfirmation
                    ? 'translate-x-6'
                    : 'translate-x-1'
                }`}
              />
            </button>
          </div>

          {/* Rent Adjustment */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label
                htmlFor="rentAdjustmentType"
                className="block text-sm font-medium text-text-secondary mb-1"
              >
                {t('form.rentAdjustmentType')}
              </label>
              <select
                id="rentAdjustmentType"
                value={rentAdjustmentType}
                onChange={(e) => onChange('rentAdjustmentType', e.target.value)}
                className="w-full px-3 py-2 border border-border-strong rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500 bg-surface-card text-text-primary"
                disabled={disabled}
              >
                {Object.entries(ADJUSTMENT_TYPE_LABELS).map(
                  ([value, label]) => (
                    <option key={value} value={value}>
                      {label}
                    </option>
                  )
                )}
              </select>
            </div>
            {(rentAdjustmentType === 'FIXED_PERCENTAGE' ||
              rentAdjustmentType === 'FIXED_AMOUNT') && (
              <div>
                <label
                  htmlFor="rentAdjustmentValue"
                  className="block text-sm font-medium text-text-secondary mb-1"
                >
                  {t('form.adjustmentValue')}
                  {rentAdjustmentType === 'FIXED_PERCENTAGE' ? ' (%)' : ''}
                </label>
                <input
                  id="rentAdjustmentValue"
                  type="number"
                  step="0.01"
                  value={rentAdjustmentValue ?? ''}
                  onChange={(e) =>
                    onChange(
                      'rentAdjustmentValue',
                      e.target.value ? parseFloat(e.target.value) : undefined
                    )
                  }
                  className="w-full px-3 py-2 border border-border-strong rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500 bg-surface-card text-text-primary"
                  disabled={disabled}
                  placeholder={
                    rentAdjustmentType === 'FIXED_PERCENTAGE'
                      ? 'e.g. 3.5'
                      : 'e.g. 50.00'
                  }
                />
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
};
