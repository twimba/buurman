import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { useCurrentUser } from '../hooks/useAuthHooks';

interface ProtectedRouteProps {
  children: React.ReactNode;
  requireVerification?: boolean;
}

const ProtectedRoute: React.FC<ProtectedRouteProps> = ({
  children,
  requireVerification = true,
}) => {
  const { isAuthenticated, isLoading } = useAuth();
  const { data: user, isLoading: isUserLoading } =
    useCurrentUser(isAuthenticated);

  if (isLoading || (isAuthenticated && isUserLoading)) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-lg">Loading...</div>
      </div>
    );
  }

  if (!isAuthenticated) {
    const redirect = encodeURIComponent(
      window.location.pathname + window.location.search
    );
    return <Navigate to={`/login?redirect=${redirect}`} replace />;
  }

  if (requireVerification && user && !user.emailVerified) {
    return <Navigate to="/verify-email" replace />;
  }

  return <>{children}</>;
};

export default ProtectedRoute;
