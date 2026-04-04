import React from 'react';
import { useAuth } from '../context/AuthContext';
import { useNavigate, useSearchParams } from 'react-router-dom';
import {
  LogIn,
  Home,
  Users,
  FileText,
  TrendingUp,
  Sparkles,
  ArrowRight,
  Play,
  Eye,
  EyeOff,
} from 'lucide-react';
import { PublicBroadcastBanner } from '../components/common/BroadcastBanner';

import { env } from '../config/env';

const DEMO_EMAIL = env('VITE_DEMO_EMAIL') || 'demo.user@demo.buurman.io';
const DEMO_PASSWORD = env('VITE_DEMO_PASSWORD') || '';

const sanitizeRedirect = (url: string | null): string | null => {
  if (!url) {
    return null;
  }
  try {
    const resolved = new URL(url, window.location.origin);
    if (resolved.origin !== window.location.origin) {
      return null;
    }
    return resolved.pathname + resolved.search + resolved.hash;
  } catch {
    return null;
  }
};

const LoginPage: React.FC = () => {
  const { login, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const isDemo = searchParams.get('demo') === 'true';
  const redirect = sanitizeRedirect(searchParams.get('redirect'));
  const [showPassword, setShowPassword] = React.useState(false);

  React.useEffect(() => {
    if (isAuthenticated) {
      navigate(redirect || '/dashboard');
    }
  }, [isAuthenticated, navigate, redirect]);

  return (
    <div className="flex min-h-screen bg-gradient-to-br from-primary-50 via-white to-neutral-25">
      {/* Left Side - Branding */}
      <div className="hidden lg:flex lg:w-1/2 bg-gradient-to-br from-primary-800 via-primary-500 to-primary-400 p-12 flex-col justify-between text-white">
        <div>
          <div className="flex items-center gap-4 mb-8">
            <img
              src="/assets/logo/logo_square.png"
              alt="Buurman"
              className="h-20 w-20 rounded-xl shadow-2xl ring-4 ring-white ring-opacity-30"
            />
            <h1 className="text-4xl font-black tracking-tight bg-gradient-to-r from-white to-primary-200 bg-clip-text text-transparent">
              Buurman
            </h1>
          </div>
          <p className="text-xl text-primary-200 mb-12">
            Property management made simple for small landlords
          </p>

          {/* Features */}
          <div className="space-y-6">
            <div className="flex items-start gap-4">
              <div className="bg-primary-500/30 p-3 rounded-lg">
                <Home className="h-6 w-6" />
              </div>
              <div>
                <h3 className="font-semibold text-lg mb-1">
                  Manage Properties
                </h3>
                <p className="text-primary-200">
                  Keep track of all your rental properties in one place
                </p>
              </div>
            </div>

            <div className="flex items-start gap-4">
              <div className="bg-primary-500/30 p-3 rounded-lg">
                <Users className="h-6 w-6" />
              </div>
              <div>
                <h3 className="font-semibold text-lg mb-1">Track Contacts</h3>
                <p className="text-primary-200">
                  Manage contact information and lease agreements
                </p>
              </div>
            </div>

            <div className="flex items-start gap-4">
              <div className="bg-primary-500/30 p-3 rounded-lg">
                <FileText className="h-6 w-6" />
              </div>
              <div>
                <h3 className="font-semibold text-lg mb-1">Handle Finances</h3>
                <p className="text-primary-200">
                  Monitor payments, expenses, and financial reports
                </p>
              </div>
            </div>

            <div className="flex items-start gap-4">
              <div className="bg-primary-500/30 p-3 rounded-lg">
                <TrendingUp className="h-6 w-6" />
              </div>
              <div>
                <h3 className="font-semibold text-lg mb-1">
                  Grow Your Business
                </h3>
                <p className="text-primary-200">
                  Scale your rental portfolio with confidence
                </p>
              </div>
            </div>
          </div>
        </div>

        <div className="text-sm text-primary-300">
          © 2026 Buurman. Simple property management.
        </div>
      </div>

      {/* Right Side - Login Form */}
      <div className="flex-1 flex items-center justify-center p-8">
        <div className="w-full max-w-md">
          <PublicBroadcastBanner context="login" />
          {/* Mobile Logo */}
          <div className="lg:hidden flex flex-col items-center mb-8">
            <img
              src="/assets/logo/logo_square.png"
              alt="Buurman"
              className="h-24 w-24 rounded-xl shadow-2xl mb-4"
            />
            <h1 className="text-4xl font-black tracking-tight bg-gradient-to-r from-primary-500 to-primary-800 bg-clip-text text-transparent">
              Buurman
            </h1>
            <p className="text-text-secondary mt-2 text-center">
              Property management for small landlords
            </p>
          </div>

          <div className="bg-surface-card rounded-2xl shadow-xl p-8 lg:p-10 relative overflow-hidden">
            {/* Decorative gradient accent */}
            <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-primary-900 via-primary-500 to-primary-300" />

            {isDemo ? (
              <>
                <div className="mb-6 text-center">
                  <div className="inline-flex items-center gap-1.5 bg-warning-bg text-warning-text px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wider mb-4 border border-warning-border">
                    <Play className="h-3.5 w-3.5" />
                    Live Demo
                  </div>
                  <h2 className="text-2xl font-bold text-text-primary mb-2">
                    Take It for a Spin!
                  </h2>
                  <p className="text-text-secondary">
                    Fully loaded demo &mdash; poke around, we won&apos;t judge
                  </p>
                </div>

                <div className="bg-warning-bg border border-warning-border rounded-xl p-4 mb-6 text-left">
                  <p className="text-xs font-semibold text-warning-text uppercase tracking-wider mb-3">
                    Demo credentials
                  </p>
                  <div className="space-y-2">
                    <div className="flex items-center justify-between bg-surface-card rounded-lg px-3 py-2 border border-warning-border">
                      <div>
                        <span className="text-[10px] text-warning-text font-medium uppercase tracking-wider">
                          Email
                        </span>
                        <p className="text-sm font-mono font-semibold text-text-primary">
                          {DEMO_EMAIL}
                        </p>
                      </div>
                    </div>
                    <div className="flex items-center justify-between bg-surface-card rounded-lg px-3 py-2 border border-warning-border">
                      <div>
                        <span className="text-[10px] text-warning-text font-medium uppercase tracking-wider">
                          Password
                        </span>
                        <p className="text-sm font-mono font-semibold text-text-primary">
                          {showPassword ? DEMO_PASSWORD : '••••••••'}
                        </p>
                      </div>
                      <button
                        type="button"
                        onClick={() => setShowPassword(!showPassword)}
                        className="text-warning-text hover:text-warning-text/80 transition-colors p-1"
                      >
                        {showPassword ? (
                          <EyeOff className="h-4 w-4" />
                        ) : (
                          <Eye className="h-4 w-4" />
                        )}
                      </button>
                    </div>
                  </div>
                </div>

                <button
                  onClick={() => login(undefined, DEMO_EMAIL)}
                  className="group w-full bg-gradient-to-r from-red-500 to-amber-500 text-white py-3.5 px-6 rounded-xl hover:from-red-600 hover:to-amber-600 transition-all duration-200 font-semibold flex items-center justify-center gap-3 shadow-lg hover:shadow-xl transform hover:-translate-y-0.5"
                >
                  <Play className="h-5 w-5" />
                  Launch Demo
                  <ArrowRight className="h-4 w-4 opacity-0 -ml-4 group-hover:opacity-100 group-hover:ml-0 transition-all duration-200" />
                </button>

                <p className="text-center text-xs text-text-secondary mt-3 italic">
                  Use the credentials above on the Keycloak login screen
                </p>

                <div className="mt-5 relative">
                  <div className="absolute inset-0 flex items-center">
                    <div className="w-full border-t border-border-default " />
                  </div>
                  <div className="relative flex justify-center text-xs">
                    <span className="bg-surface-card px-3 text-text-secondary">
                      or create your own account
                    </span>
                  </div>
                </div>

                <a
                  href="/register"
                  className="mt-5 w-full py-3 px-6 rounded-lg border-2 border-border-default text-text-secondary font-semibold flex items-center justify-center gap-2 hover:border-primary-500 hover:text-primary-500 transition-all duration-200 hover:bg-primary-50"
                >
                  Get Started Free
                  <ArrowRight className="h-4 w-4" />
                </a>
              </>
            ) : (
              <>
                <div className="mb-8 text-center">
                  <div className="inline-flex items-center gap-1.5 bg-primary-500/10 text-primary-600 px-3 py-1 rounded-full text-xs font-medium mb-4">
                    <Sparkles className="h-3.5 w-3.5" />
                    Free for small landlords
                  </div>
                  <h2 className="text-2xl font-bold text-text-primary mb-2">
                    Welcome back
                  </h2>
                  <p className="text-text-secondary">
                    Your properties are waiting for you
                  </p>
                </div>

                <button
                  onClick={() =>
                    login(
                      redirect
                        ? `${window.location.origin}${redirect}`
                        : undefined
                    )
                  }
                  className="group w-full bg-gradient-to-r from-primary-600 to-primary-500 text-white py-3.5 px-6 rounded-xl hover:from-primary-700 hover:to-primary-600 transition-all duration-200 font-semibold flex items-center justify-center gap-3 shadow-lg hover:shadow-xl transform hover:-translate-y-0.5"
                >
                  <LogIn className="h-5 w-5" />
                  Sign in to your account
                  <ArrowRight className="h-4 w-4 opacity-0 -ml-4 group-hover:opacity-100 group-hover:ml-0 transition-all duration-200" />
                </button>

                <div className="mt-6 relative">
                  <div className="absolute inset-0 flex items-center">
                    <div className="w-full border-t border-border-default " />
                  </div>
                  <div className="relative flex justify-center text-xs">
                    <span className="bg-surface-card px-3 text-text-secondary">
                      or
                    </span>
                  </div>
                </div>

                <a
                  href="/register"
                  className="mt-6 w-full py-3 px-6 rounded-lg border-2 border-border-default text-text-secondary font-semibold flex items-center justify-center gap-2 hover:border-primary-500 hover:text-primary-500 transition-all duration-200 hover:bg-primary-50"
                >
                  Create a free account
                  <ArrowRight className="h-4 w-4" />
                </a>
              </>
            )}
          </div>

          {/* Additional Info */}
          <div className="mt-6 text-center">
            <p className="text-sm text-text-secondary">
              Need help?{' '}
              <a
                href="https://www.buurman.io/support"
                target="_blank"
                rel="noopener noreferrer"
                className="text-primary-500 hover:underline"
              >
                Contact support
              </a>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;
