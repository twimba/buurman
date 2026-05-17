import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { useImpersonation } from '../context/ImpersonationContext';
import { exchangeImpersonationToken } from '../generated/api/impersonation/impersonation';
import { LoadingSpinner } from '@buurman/ui';

export function ImpersonatePage() {
  const { t } = useTranslation('admin');
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { startImpersonation } = useImpersonation();
  const token = searchParams.get('token');
  const [error, setError] = useState<string | null>(
    token ? null : t('common:impersonation.missingToken')
  );

  // Prevent the session token in the URL from leaking via the Referrer header
  useEffect(() => {
    const meta = document.createElement('meta');
    meta.name = 'referrer';
    meta.content = 'no-referrer';
    document.head.appendChild(meta);
    return () => {
      document.head.removeChild(meta);
    };
  }, []);

  useEffect(() => {
    if (!token) {
      return;
    }

    const exchange = async () => {
      try {
        const data = await exchangeImpersonationToken({
          sessionToken: token,
        });

        startImpersonation({
          token: data.token,
          sessionIdentifier: data.sessionIdentifier,
          adminEmail: data.adminEmail,
          adminName: data.adminName,
          mode: data.mode,
          expiresIn: data.expiresIn,
          targetUserEmail: data.targetUserEmail,
          reason: data.reason ?? undefined,
          targetTeamIdentifier: data.targetTeamIdentifier ?? undefined,
        });

        // Remove the session token from browser history before navigating
        window.history.replaceState({}, '', '/impersonate');
        navigate('/dashboard', { replace: true });
      } catch (err) {
        setError(
          err instanceof Error
            ? err.message
            : t('common:impersonation.failedToExchange')
        );
      }
    };

    exchange();
  }, [token, startImpersonation, navigate, t]);

  if (error) {
    return (
      <div className="min-h-[100dvh] flex items-center justify-center">
        <div className="text-center">
          <h1 className="text-xl font-semibold text-text-primary mb-2">
            {t('common:impersonation.failed')}
          </h1>
          <p className="text-text-secondary">{error}</p>
          <button
            onClick={() => window.close()}
            className="mt-4 text-sm text-primary-500 hover:underline"
          >
            {t('common:impersonation.closeTab')}
          </button>
        </div>
      </div>
    );
  }

  return <LoadingSpinner message={t('common:impersonation.settingUp')} />;
}
