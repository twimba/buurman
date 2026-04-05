import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { useImpersonation } from '../context/ImpersonationContext';

interface ImpersonationGuardProps {
  children: ReactNode;
  blockInReadOnly?: boolean;
  blockAlways?: boolean;
  fallback?: ReactNode;
}

export function ImpersonationGuard({
  children,
  blockInReadOnly = false,
  blockAlways = false,
  fallback,
}: ImpersonationGuardProps) {
  const { t } = useTranslation('common');
  const { active, mode } = useImpersonation();

  if (!active) {
    return <>{children}</>;
  }

  if (blockAlways) {
    return (
      <>
        {fallback ?? (
          <span className="text-sm text-text-muted">
            {t('impersonation.notAvailable')}
          </span>
        )}
      </>
    );
  }

  if (blockInReadOnly && mode === 'READ_ONLY') {
    return (
      <>
        {fallback ?? (
          <span className="text-sm text-text-muted">{t('impersonation.readOnly')}</span>
        )}
      </>
    );
  }

  return <>{children}</>;
}
