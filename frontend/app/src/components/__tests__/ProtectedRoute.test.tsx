import { screen } from '@testing-library/react';
import { renderWithProviders } from '@/test/test-utils';
import ProtectedRoute from '../ProtectedRoute';
import { useAuth } from '@/context/AuthContext';
import { useImpersonation } from '@/context/ImpersonationContext';
import { useCurrentUser } from '@/hooks/useAuthHooks';

vi.mock('@/context/AuthContext', () => ({
  useAuth: vi.fn(),
}));

vi.mock('@/context/ImpersonationContext', () => ({
  useImpersonation: vi.fn(),
}));

vi.mock('@/hooks/useAuthHooks', () => ({
  useCurrentUser: vi.fn(),
}));

const mockUseAuth = vi.mocked(useAuth);
const mockUseImpersonation = vi.mocked(useImpersonation);
const mockUseCurrentUser = vi.mocked(useCurrentUser);

function defaultImpersonation() {
  mockUseImpersonation.mockReturnValue({
    active: false,
    token: null,
    sessionIdentifier: null,
    adminEmail: null,
    adminName: null,
    reason: null,
    targetTeamIdentifier: null,
    mode: null,
    expiresAt: null,
    targetUserEmail: null,
    startImpersonation: vi.fn(),
    endImpersonation: vi.fn(),
    remainingSeconds: 0,
  });
}

describe('ProtectedRoute', () => {
  beforeEach(() => {
    defaultImpersonation();
  });

  it('shows loading spinner when auth is loading', () => {
    mockUseAuth.mockReturnValue({
      isAuthenticated: false,
      isLoading: true,
      login: vi.fn(),
      logout: vi.fn(),
      token: undefined,
      keycloak: {} as any,
    });
    mockUseCurrentUser.mockReturnValue({
      data: undefined,
      isLoading: false,
    } as any);

    renderWithProviders(
      <ProtectedRoute>
        <div>Protected Content</div>
      </ProtectedRoute>
    );

    expect(screen.getByText('Loading...')).toBeInTheDocument();
    expect(screen.queryByText('Protected Content')).not.toBeInTheDocument();
  });

  it('redirects to /login when not authenticated', () => {
    mockUseAuth.mockReturnValue({
      isAuthenticated: false,
      isLoading: false,
      login: vi.fn(),
      logout: vi.fn(),
      token: undefined,
      keycloak: {} as any,
    });
    mockUseCurrentUser.mockReturnValue({
      data: undefined,
      isLoading: false,
    } as any);

    renderWithProviders(
      <ProtectedRoute>
        <div>Protected Content</div>
      </ProtectedRoute>
    );

    expect(screen.queryByText('Protected Content')).not.toBeInTheDocument();
    // Navigate component redirects, so the content should not appear
  });

  it('renders children when authenticated', () => {
    mockUseAuth.mockReturnValue({
      isAuthenticated: true,
      isLoading: false,
      login: vi.fn(),
      logout: vi.fn(),
      token: 'some-token',
      keycloak: {} as any,
    });
    mockUseCurrentUser.mockReturnValue({
      data: { emailVerified: true },
      isLoading: false,
    } as any);

    renderWithProviders(
      <ProtectedRoute>
        <div>Protected Content</div>
      </ProtectedRoute>
    );

    expect(screen.getByText('Protected Content')).toBeInTheDocument();
  });

  it('redirects to /verify-email when requireVerification=true and email not verified', () => {
    mockUseAuth.mockReturnValue({
      isAuthenticated: true,
      isLoading: false,
      login: vi.fn(),
      logout: vi.fn(),
      token: 'some-token',
      keycloak: {} as any,
    });
    mockUseCurrentUser.mockReturnValue({
      data: { emailVerified: false },
      isLoading: false,
    } as any);

    renderWithProviders(
      <ProtectedRoute requireVerification={true}>
        <div>Protected Content</div>
      </ProtectedRoute>
    );

    expect(screen.queryByText('Protected Content')).not.toBeInTheDocument();
  });
});
