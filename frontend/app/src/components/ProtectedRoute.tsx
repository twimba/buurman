import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../context/AuthContext';
import { useImpersonation } from '../context/ImpersonationContext';
import { useCurrentUser } from '../hooks/useAuthHooks';

interface ProtectedRouteProps {
  children?: React.ReactNode;
  requireVerification?: boolean;
}

const ProtectedRoute: React.FC<ProtectedRouteProps> = ({
  children,
  requireVerification = true,
}) => {
  const { t } = useTranslation('common');
  const { isAuthenticated, isLoading } = useAuth();
  const { active: isImpersonating } = useImpersonation();

  // Impersonation sessions authenticate via a self-signed JWT — no Keycloak session needed
  const effectivelyAuthenticated = isAuthenticated || isImpersonating;

  const { data: user, isLoading: isUserLoading } = useCurrentUser(
    effectivelyAuthenticated
  );

  if (isLoading || (effectivelyAuthenticated && isUserLoading)) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-lg">{t('buttons.loading')}</div>
      </div>
    );
  }

  if (!effectivelyAuthenticated) {
    const redirect = encodeURIComponent(
      window.location.pathname + window.location.search
    );
    return <Navigate to={`/login?redirect=${redirect}`} replace />;
  }

  // Skip email verification during impersonation — admin is viewing as the user
  if (requireVerification && !isImpersonating && user && !user.emailVerified) {
    return <Navigate to="/verify-email" replace />;
  }

  return children ? <>{children}</> : <Outlet />;
};

ProtectedRoute.displayName = 'ProtectedRoute';

export default ProtectedRoute;
