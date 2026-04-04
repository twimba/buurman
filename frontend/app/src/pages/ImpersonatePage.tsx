import { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { useImpersonation } from '../context/ImpersonationContext';
import { exchangeImpersonationToken } from '../generated/api/impersonation/impersonation';
import { LoadingSpinner } from '@buurman/ui';

export function ImpersonatePage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { startImpersonation } = useImpersonation();
  const token = searchParams.get('token');
  const [error, setError] = useState<string | null>(
    token ? null : 'Missing session token'
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
          err instanceof Error ? err.message : 'Failed to exchange token'
        );
      }
    };

    exchange();
  }, [token, startImpersonation, navigate]);

  if (error) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <h1 className="text-xl font-semibold text-text-primary mb-2">
            Impersonation Failed
          </h1>
          <p className="text-text-secondary">{error}</p>
          <button
            onClick={() => window.close()}
            className="mt-4 text-sm text-primary-500 hover:underline"
          >
            Close this tab
          </button>
        </div>
      </div>
    );
  }

  return <LoadingSpinner message="Setting up impersonation session..." />;
}
