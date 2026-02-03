import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation } from '@tanstack/react-query';
import {
  Users,
  CheckCircle,
  XCircle,
  Clock,
  Loader2,
  LogIn,
  UserPlus,
} from 'lucide-react';
import { getInvitation, acceptInvitation } from '../api/teams';
import { useAuth } from '../contexts/AuthContext';

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

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });
};

export const InvitationPage = () => {
  const { token } = useParams<{ token: string }>();
  const navigate = useNavigate();
  const { isAuthenticated, isLoading: authLoading, login } = useAuth();

  const {
    data: invitation,
    isLoading,
    error,
  } = useQuery({
    queryKey: ['invitation', token],
    queryFn: () => getInvitation(token!),
    enabled: !!token,
    retry: false,
  });

  const acceptMutation = useMutation({
    mutationFn: () => acceptInvitation(token!),
    onSuccess: () => {
      // Clear the pending invitation
      localStorage.removeItem('pendingInvitation');
      // Reload to refresh team data and redirect to dashboard
      window.location.href = '/dashboard';
    },
  });

  const handleLogin = () => {
    // Store the invitation token to redirect back after login
    localStorage.setItem('pendingInvitation', token!);
    login();
  };

  const handleRegister = () => {
    // Store the invitation token to redirect back after registration
    localStorage.setItem('pendingInvitation', token!);
    navigate(`/register?invitation=${token}`);
  };

  const handleAccept = () => {
    acceptMutation.mutate();
  };

  if (isLoading || authLoading) {
    return (
      <div className="min-h-screen bg-gray-100 flex items-center justify-center">
        <div className="text-center">
          <Loader2 className="h-12 w-12 animate-spin text-blue-600 mx-auto" />
          <p className="mt-4 text-gray-600">Loading invitation...</p>
        </div>
      </div>
    );
  }

  if (error || !invitation) {
    return (
      <div className="min-h-screen bg-gray-100 flex items-center justify-center p-4">
        <div className="bg-white rounded-lg shadow-lg p-8 max-w-md w-full text-center">
          <XCircle className="h-16 w-16 text-red-500 mx-auto mb-4" />
          <h1 className="text-2xl font-bold text-gray-900 mb-2">
            Invalid Invitation
          </h1>
          <p className="text-gray-600 mb-6">
            This invitation link is invalid, has already been used, or has
            expired.
          </p>
          <button
            onClick={() => navigate('/login')}
            className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
          >
            Go to Login
          </button>
        </div>
      </div>
    );
  }

  if (invitation.isAccepted) {
    return (
      <div className="min-h-screen bg-gray-100 flex items-center justify-center p-4">
        <div className="bg-white rounded-lg shadow-lg p-8 max-w-md w-full text-center">
          <CheckCircle className="h-16 w-16 text-green-500 mx-auto mb-4" />
          <h1 className="text-2xl font-bold text-gray-900 mb-2">
            Already Accepted
          </h1>
          <p className="text-gray-600 mb-6">
            This invitation has already been accepted. You can log in to access
            the team.
          </p>
          <button
            onClick={() => navigate('/login')}
            className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
          >
            Go to Login
          </button>
        </div>
      </div>
    );
  }

  if (invitation.isExpired) {
    return (
      <div className="min-h-screen bg-gray-100 flex items-center justify-center p-4">
        <div className="bg-white rounded-lg shadow-lg p-8 max-w-md w-full text-center">
          <Clock className="h-16 w-16 text-yellow-500 mx-auto mb-4" />
          <h1 className="text-2xl font-bold text-gray-900 mb-2">
            Invitation Expired
          </h1>
          <p className="text-gray-600 mb-6">
            This invitation has expired. Please contact the team administrator
            to request a new invitation.
          </p>
          <button
            onClick={() => navigate('/login')}
            className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
          >
            Go to Login
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-100 flex items-center justify-center p-4">
      <div className="bg-white rounded-lg shadow-lg p-8 max-w-md w-full">
        <div className="text-center mb-6">
          <div className="h-16 w-16 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <Users className="h-8 w-8 text-blue-600" />
          </div>
          <h1 className="text-2xl font-bold text-gray-900 mb-2">
            You're Invited!
          </h1>
          <p className="text-gray-600">
            <span className="font-medium">{invitation.inviterName}</span> has
            invited you to join
          </p>
        </div>

        <div className="bg-gray-50 rounded-lg p-4 mb-6">
          <h2 className="text-xl font-semibold text-gray-900 mb-2">
            {invitation.teamName}
          </h2>
          <div className="space-y-2 text-sm text-gray-600">
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
          <div className="bg-red-50 border border-red-200 rounded-lg p-3 mb-4">
            <p className="text-sm text-red-600">
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
              className="w-full px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center justify-center gap-2 disabled:opacity-50"
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
            <p className="text-xs text-center text-gray-500">
              By accepting, you'll join this team with the specified role.
            </p>
          </div>
        ) : (
          <div className="space-y-3">
            <p className="text-sm text-gray-600 text-center mb-4">
              To accept this invitation, please log in or create an account.
            </p>
            <button
              onClick={handleLogin}
              className="w-full px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center justify-center gap-2"
            >
              <LogIn className="h-5 w-5" />
              Log In to Accept
            </button>
            <button
              onClick={handleRegister}
              className="w-full px-6 py-3 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors flex items-center justify-center gap-2"
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
