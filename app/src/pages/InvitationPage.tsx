import { useCallback, useEffect, useRef } from 'react';
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
import { getInvitation } from '../api/teams';
import { useAcceptInvitation } from '../hooks/useTeamHooks';
import { useAuth } from '../contexts/AuthContext';
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
    localStorage.removeItem('pendingInvitation');
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
      const pendingToken = localStorage.getItem('pendingInvitation');
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
    localStorage.setItem('pendingInvitation', token);
    login(`${window.location.origin}/invitation/${token}`);
  };

  const handleRegister = () => {
    // Store the invitation token to redirect back after registration
    localStorage.setItem('pendingInvitation', token);
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
      localStorage.removeItem('pendingInvitation');
    }
  }, [isLoading, error, invitation]);

  if (isLoading || authLoading) {
    return (
      <div className="min-h-screen bg-surface-inset flex items-center justify-center">
        <div className="text-center">
          <Loader2 className="h-12 w-12 animate-spin text-primary-500 mx-auto" />
          <p className="mt-4 text-text-secondary">Loading invitation...</p>
        </div>
      </div>
    );
  }

  if (error || !invitation) {
    return (
      <div className="min-h-screen bg-surface-inset flex items-center justify-center p-4">
        <div className="bg-surface-card rounded-lg shadow-lg p-8 max-w-md w-full text-center">
          <XCircle className="h-16 w-16 text-error-text mx-auto mb-4" />
          <h1 className="text-2xl font-bold text-text-primary mb-2">
            Invalid Invitation
          </h1>
          <p className="text-text-secondary mb-6">
            This invitation link is invalid, has already been used, or has
            expired.
          </p>
          <button
            onClick={() => navigate('/login')}
            className="px-6 py-2 bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors"
          >
            Go to Login
          </button>
        </div>
      </div>
    );
  }

  if (invitation.isAccepted) {
    return (
      <div className="min-h-screen bg-surface-inset flex items-center justify-center p-4">
        <div className="bg-surface-card rounded-lg shadow-lg p-8 max-w-md w-full text-center">
          <CheckCircle className="h-16 w-16 text-success-text mx-auto mb-4" />
          <h1 className="text-2xl font-bold text-text-primary mb-2">
            Already Accepted
          </h1>
          <p className="text-text-secondary mb-6">
            This invitation has already been accepted. You can log in to access
            the team.
          </p>
          <button
            onClick={() => navigate('/login')}
            className="px-6 py-2 bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors"
          >
            Go to Login
          </button>
        </div>
      </div>
    );
  }

  if (invitation.isExpired) {
    return (
      <div className="min-h-screen bg-surface-inset flex items-center justify-center p-4">
        <div className="bg-surface-card rounded-lg shadow-lg p-8 max-w-md w-full text-center">
          <Clock className="h-16 w-16 text-warning-text mx-auto mb-4" />
          <h1 className="text-2xl font-bold text-text-primary mb-2">
            Invitation Expired
          </h1>
          <p className="text-text-secondary mb-6">
            This invitation has expired. Please contact the team administrator
            to request a new invitation.
          </p>
          <button
            onClick={() => navigate('/login')}
            className="px-6 py-2 bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors"
          >
            Go to Login
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-surface-inset flex items-center justify-center p-4">
      <div className="bg-surface-card rounded-lg shadow-lg p-8 max-w-md w-full">
        <div className="text-center mb-6">
          <div className="h-16 w-16 bg-info-bg rounded-full flex items-center justify-center mx-auto mb-4">
            <Users className="h-8 w-8 text-primary-500 dark:text-primary-300" />
          </div>
          <h1 className="text-2xl font-bold text-text-primary mb-2">
            You&apos;re Invited!
          </h1>
          <p className="text-text-secondary">
            <span className="font-medium">{invitation.inviterName}</span> has
            invited you to join
          </p>
        </div>

        <div className="bg-surface-page rounded-lg p-4 mb-6">
          <h2 className="text-xl font-semibold text-text-primary mb-2">
            {invitation.teamName}
          </h2>
          <div className="space-y-2 text-sm text-text-secondary">
            <p>
              <span className="font-medium">Your role:</span>{' '}
              {formatRole(invitation.role)}
            </p>
            <p>
              <span className="font-medium">Invited email:</span>{' '}
              {invitation.email}
            </p>
            <p>
              <span className="font-medium">Expires:</span>{' '}
              {formatDate(invitation.expiresAt)}
            </p>
          </div>
        </div>

        {acceptMutation.isError && (
          <div className="bg-error-bg border border-error-border rounded-lg p-3 mb-4">
            <p className="text-sm text-error-text">
              {(acceptMutation.error as Error)?.message ||
                'Failed to accept invitation. Please try again.'}
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
                  Accepting...
                </>
              ) : (
                <>
                  <CheckCircle className="h-5 w-5" />
                  Accept Invitation
                </>
              )}
            </button>
            <p className="text-xs text-center text-text-secondary">
              By accepting, you&apos;ll join this team with the specified role.
            </p>
          </div>
        ) : (
          <div className="space-y-3">
            <p className="text-sm text-text-secondary text-center mb-4">
              To accept this invitation, please log in or create an account.
            </p>
            <button
              onClick={handleLogin}
              className="w-full px-6 py-3 bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors flex items-center justify-center gap-2"
            >
              <LogIn className="h-5 w-5" />
              Log In to Accept
            </button>
            <button
              onClick={handleRegister}
              className="w-full px-6 py-3 border border-border-strong text-text-secondary rounded-lg hover:bg-surface-page transition-colors flex items-center justify-center gap-2"
            >
              <UserPlus className="h-5 w-5" />
              Create Account
            </button>
          </div>
        )}
      </div>
    </div>
  );
};
