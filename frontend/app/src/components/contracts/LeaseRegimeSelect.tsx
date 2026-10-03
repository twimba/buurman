import { useTranslation } from 'react-i18next';
import type { LeaseRegime } from '@/generated/models';
import { AVAILABLE_LEASE_REGIMES } from './leaseRegimes';

interface LeaseRegimeSelectProps {
  value: LeaseRegime;
  onChange: (regime: LeaseRegime) => void;
  availableRegimes?: readonly LeaseRegime[];
  disabled?: boolean;
}

export const LeaseRegimeSelect = ({
  value,
  onChange,
  availableRegimes = AVAILABLE_LEASE_REGIMES,
  disabled,
}: LeaseRegimeSelectProps) => {
  const { t } = useTranslation('contracts');

  if (availableRegimes.length <= 1) {
    return null;
  }

  return (
    <div>
      <label
        htmlFor="lease-regime"
        className="block text-sm font-medium text-text-secondary mb-1"
      >
        {t('form.leaseRegime')}
      </label>
      <select
        id="lease-regime"
        value={value}
        onChange={(e) => onChange(e.target.value as LeaseRegime)}
        className="w-full border border-border-strong rounded px-3 py-2 focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
        disabled={disabled}
      >
        {availableRegimes.map((regime) => (
          <option key={regime} value={regime}>
            {t(`leaseRegime.${regime}`)}
          </option>
        ))}
      </select>
    </div>
  );
};
