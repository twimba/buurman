import { useTranslation } from 'react-i18next';
import { Globe, MapPin } from 'lucide-react';
import { Button, EmptyState } from '@buurman/ui';

export const CountryRow = ({ name }: { name: string }) => {
  const { t } = useTranslation('contracts');
  return (
    <dl className="flex items-baseline gap-2 text-sm">
      <dt className="text-text-secondary">
        {t('leaseAgreement.countryLabel')}
      </dt>
      <dd className="font-medium text-text-primary">{name}</dd>
    </dl>
  );
};

interface LeaseUnavailableStateProps {
  reason: 'country' | 'no-country';
  countryName?: string;
  /** Go to Documents (country) or Edit contract (no-country); no button when omitted. */
  onAction?: () => void;
}

export const LeaseUnavailableState = ({
  reason,
  countryName,
  onAction,
}: LeaseUnavailableStateProps) => {
  const { t } = useTranslation('contracts');
  const isNoCountry = reason === 'no-country';
  const keyBase = isNoCountry
    ? 'leaseAgreement.noCountry'
    : 'leaseAgreement.unavailable';
  const Icon = isNoCountry ? MapPin : Globe;

  return (
    <EmptyState
      variant="inline"
      tone={isNoCountry ? 'info' : 'neutral'}
      icon={<Icon className="h-full w-full" />}
      title={t(`${keyBase}.title`)}
      description={t(`${keyBase}.description`)}
      className="rounded-lg border border-border-subtle bg-surface-inset px-4 py-8 sm:px-8 sm:py-10 [&_h3]:text-balance [&_h3]:break-words [&_p]:max-w-md [&_p]:break-words"
      actions={
        <div className="flex flex-col items-center gap-4">
          {!isNoCountry && countryName && <CountryRow name={countryName} />}
          {onAction && (
            <Button
              variant={isNoCountry ? 'primary' : 'secondary'}
              className="w-full sm:w-auto"
              onClick={onAction}
            >
              {t(`${keyBase}.action`)}
            </Button>
          )}
        </div>
      }
    />
  );
};

LeaseUnavailableState.displayName = 'LeaseUnavailableState';
