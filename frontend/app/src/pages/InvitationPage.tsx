import { useCallback, useEffect, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Users,
  CheckCircle,
  XCircle,
  Clock,
  Loader2,
  LogIn,
  UserPlus,
} from 'lucide-react';
import { getInvitation } from '../generated/api/invitations/invitations';
import { useAcceptInvitation } from '../hooks/useTeamHooks';
import { useAuth } from '../context/AuthContext';
import { useFormatDate } from '@/hooks/useFormatDate';

const formatRole = (role: string) => {
  switch (role) {
    case 'TEAM_ADMIN':
      return 'Administrator';
    case 'TEAM_EDITOR':
      return 'Editor';
    case 'TEAM_VIEWER':
      return 'Viewer';
    default:
      return role;
  }
};

export const InvitationPage = () => {
  const { t } = useTranslation('admin');
  const { formatDate } = useFormatDate();
  const { token = '' } = useParams<{ token: string }>();
  const navigate = useNavigate();
  const { isAuthenticated, isLoading: authLoading, login } = useAuth();

  const queryClient = useQueryClient();

  const {
    data: invitation,
    isLoading,
    error,
  } = useQuery({
    queryKey: ['invitation', token],
    queryFn: () => getInvitation(token),
    enabled: !!token,
    retry: false,
  });

  const acceptMutation = useAcceptInvitation();

  const handleAcceptSuccess = useCallback(() => {
    sessionStorage.removeItem('pendingInvitation');
    // Clear all cached queries so the dashboard loads with fresh team data
    queryClient.clear();
    window.location.href = '/dashboard';
  }, [queryClient]);

  // Auto-accept when user comes back from login flow
  const autoAcceptTriggered = useRef(false);
  useEffect(() => {
    if (
      isAuthenticated &&
      !autoAcceptTriggered.current &&
      invitation &&
      !invitation.isAccepted &&
      !invitation.isExpired
    ) {
      const pendingToken = sessionStorage.getItem('pendingInvitation');
      if (pendingToken === token) {
        autoAcceptTriggered.current = true;
        acceptMutation.mutate(token, {
          onSuccess: handleAcceptSuccess,
        });
      }
    }
  }, [isAuthenticated, invitation, acceptMutation, handleAcceptSuccess, token]);

  const handleLogin = () => {
    // Store the invitation token for auto-accept after login
    sessionStorage.setItem('pendingInvitation', token);
    login(`${window.location.origin}/invitation/${token}`);
  };

  const handleRegister = () => {
    // Store the invitation token to redirect back after registration
    sessionStorage.setItem('pendingInvitation', token);
    navigate(`/register?invitation=${token}`);
  };

  const handleAccept = () => {
    acceptMutation.mutate(token, {
      onSuccess: handleAcceptSuccess,
    });
  };

  // Clear stale pendingInvitation when invitation is invalid/expired/accepted
  useEffect(() => {
    if (
      !isLoading &&
      (error || !invitation || invitation.isAccepted || invitation.isExpired)
    ) {
      sessionStorage.removeItem('pendingInvitation');
    }
  }, [isLoading, error, invitation]);

  if (isLoading || authLoading) {
    return (
      <div className="min-h-[100dvh] bg-surface-inset flex items-center justify-center">
        <div className="text-center">
          <Loader2 className="h-12 w-12 animate-spin text-primary-500 mx-auto" />
          <p className="mt-4 text-text-secondary">
            {t('common:invitation.loading')}
          </p>
        </div>
      </div>
    );
  }

  if (error || !invitation) {
    return (
      <div className="min-h-[100dvh] bg-surface-inset flex items-center justify-center p-4">
        <div className="bg-surface-card rounded-lg shadow-lg p-8 max-w-md w-full text-center">
          <XCircle className="h-16 w-16 text-error-text mx-auto mb-4" />
          <h1 className="text-2xl font-bold text-text-primary mb-2">
            {t('common:invitation.invalid.title')}
          </h1>
          <p className="text-text-secondary mb-6">
            {t('common:invitation.invalid.message')}
          </p>
          <button
            onClick={() => navigate('/login')}
            className="px-6 py-2 bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors"
          >
            {t('common:invitation.invalid.goToLogin')}
          </button>
        </div>
      </div>
    );
  }

  if (invitation.isAccepted) {
    return (
      <div className="min-h-[100dvh] bg-surface-inset flex items-center justify-center p-4">
        <div className="bg-surface-card rounded-lg shadow-lg p-8 max-w-md w-full text-center">
          <CheckCircle className="h-16 w-16 text-success-text mx-auto mb-4" />
          <h1 className="text-2xl font-bold text-text-primary mb-2">
            {t('common:invitation.accepted.title')}
          </h1>
          <p className="text-text-secondary mb-6">
            {t('common:invitation.accepted.message')}
          </p>
          <button
            onClick={() => navigate('/login')}
            className="px-6 py-2 bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors"
          >
            {t('common:invitation.accepted.goToLogin')}
          </button>
        </div>
      </div>
    );
  }

  if (invitation.isExpired) {
    return (
      <div className="min-h-[100dvh] bg-surface-inset flex items-center justify-center p-4">
        <div className="bg-surface-card rounded-lg shadow-lg p-8 max-w-md w-full text-center">
          <Clock className="h-16 w-16 text-warning-text mx-auto mb-4" />
          <h1 className="text-2xl font-bold text-text-primary mb-2">
            {t('common:invitation.expired.title')}
          </h1>
          <p className="text-text-secondary mb-6">
            {t('common:invitation.expired.message')}
          </p>
          <button
            onClick={() => navigate('/login')}
            className="px-6 py-2 bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors"
          >
            {t('common:invitation.expired.goToLogin')}
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-[100dvh] bg-surface-inset flex items-center justify-center p-4">
      <div className="bg-surface-card rounded-lg shadow-lg p-8 max-w-md w-full">
        <div className="text-center mb-6">
          <div className="h-16 w-16 bg-info-bg rounded-full flex items-center justify-center mx-auto mb-4">
            <Users className="h-8 w-8 text-primary-500 dark:text-primary-300" />
          </div>
          <h1 className="text-2xl font-bold text-text-primary mb-2">
            {t('common:invitation.youreInvited')}
          </h1>
          <p className="text-text-secondary">
            {t('common:invitation.inviterMessage', {
              name: invitation.inviterName,
            })}
          </p>
        </div>

        <div className="bg-surface-page rounded-lg p-4 mb-6">
          <h2 className="text-xl font-semibold text-text-primary mb-2">
            {invitation.teamName}
          </h2>
          <div className="space-y-2 text-sm text-text-secondary">
            <p>
              <span className="font-medium">
                {t('common:invitation.yourRole')}
              </span>{' '}
              {formatRole(invitation.role)}
            </p>
            <p>
              <span className="font-medium">
                {t('common:invitation.invitedEmail')}
              </span>{' '}
              {invitation.email}
            </p>
            <p>
              <span className="font-medium">
                {t('common:invitation.expires')}
              </span>{' '}
              {formatDate(invitation.expiresAt ?? '')}
            </p>
          </div>
        </div>

        {acceptMutation.isError && (
          <div className="bg-error-bg border border-error-border rounded-lg p-3 mb-4">
            <p className="text-sm text-error-text">
              {(acceptMutation.error as Error)?.message ||
                t('common:invitation.acceptError')}
            </p>
          </div>
        )}

        {isAuthenticated ? (
          <div className="space-y-3">
            <button
              onClick={handleAccept}
              disabled={acceptMutation.isPending}
              className="w-full px-6 py-3 bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors flex items-center justify-center gap-2 disabled:opacity-50"
            >
              {acceptMutation.isPending ? (
                <>
                  <Loader2 className="h-5 w-5 animate-spin" />
                  {t('common:invitation.accepting')}
                </>
              ) : (
                <>
                  <CheckCircle className="h-5 w-5" />
                  {t('common:invitation.acceptInvitation')}
                </>
              )}
            </button>
            <p className="text-xs text-center text-text-secondary">
              {t('common:invitation.joinNote')}
            </p>
          </div>
        ) : (
          <div className="space-y-3">
            <p className="text-sm text-text-secondary text-center mb-4">
              {t('common:invitation.loginRequired')}
            </p>
            <button
              onClick={handleLogin}
              className="w-full px-6 py-3 bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors flex items-center justify-center gap-2"
            >
              <LogIn className="h-5 w-5" />
              {t('common:invitation.loginToAccept')}
            </button>
            <button
              onClick={handleRegister}
              className="w-full px-6 py-3 border border-border-strong text-text-secondary rounded-lg hover:bg-surface-page transition-colors flex items-center justify-center gap-2"
            >
              <UserPlus className="h-5 w-5" />
              {t('common:invitation.createAccount')}
            </button>
          </div>
        )}
      </div>
    </div>
  );
};
