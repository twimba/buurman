import React from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useNavigate } from 'react-router-dom';
import { LogIn, Home, Users, FileText, TrendingUp, Sparkles, ArrowRight } from 'lucide-react';

const LoginPage: React.FC = () => {
  const { login, isAuthenticated } = useAuth();
  const navigate = useNavigate();

  React.useEffect(() => {
    if (isAuthenticated) {
      navigate('/dashboard');
    }
  }, [isAuthenticated, navigate]);

  return (
    <div className="flex min-h-screen bg-gradient-to-br from-[#f0f4ff] via-white to-[#f8f9fc]">
      {/* Left Side - Branding */}
      <div className="hidden lg:flex lg:w-1/2 bg-gradient-to-br from-[#364fc7] via-[#4c6ef5] to-[#5c7cfa] p-12 flex-col justify-between text-white">
        <div>
          <div className="flex items-center gap-4 mb-8">
            <img
              src="/assets/logo/logo_square.png"
              alt="Buurman"
              className="h-20 w-20 rounded-xl shadow-2xl ring-4 ring-white ring-opacity-30"
            />
            <h1 className="text-4xl font-black tracking-tight bg-gradient-to-r from-white to-[#bac8ff] bg-clip-text text-transparent">
              Buurman
            </h1>
          </div>
          <p className="text-xl text-[#bac8ff] mb-12">
            Property management made simple for small landlords
          </p>

          {/* Features */}
          <div className="space-y-6">
            <div className="flex items-start gap-4">
              <div className="bg-[#5c7cfa]/30 p-3 rounded-lg">
                <Home className="h-6 w-6" />
              </div>
              <div>
                <h3 className="font-semibold text-lg mb-1">
                  Manage Properties
                </h3>
                <p className="text-[#bac8ff]">
                  Keep track of all your rental properties in one place
                </p>
              </div>
            </div>

            <div className="flex items-start gap-4">
              <div className="bg-[#5c7cfa]/30 p-3 rounded-lg">
                <Users className="h-6 w-6" />
              </div>
              <div>
                <h3 className="font-semibold text-lg mb-1">Track Tenants</h3>
                <p className="text-[#bac8ff]">
                  Manage tenant information and lease agreements
                </p>
              </div>
            </div>

            <div className="flex items-start gap-4">
              <div className="bg-[#5c7cfa]/30 p-3 rounded-lg">
                <FileText className="h-6 w-6" />
              </div>
              <div>
                <h3 className="font-semibold text-lg mb-1">Handle Finances</h3>
                <p className="text-[#bac8ff]">
                  Monitor payments, expenses, and financial reports
                </p>
              </div>
            </div>

            <div className="flex items-start gap-4">
              <div className="bg-[#5c7cfa]/30 p-3 rounded-lg">
                <TrendingUp className="h-6 w-6" />
              </div>
              <div>
                <h3 className="font-semibold text-lg mb-1">
                  Grow Your Business
                </h3>
                <p className="text-[#bac8ff]">
                  Scale your rental portfolio with confidence
                </p>
              </div>
            </div>
          </div>
        </div>

        <div className="text-sm text-[#91a7ff]">
          © 2026 Buurman. Simple property management.
        </div>
      </div>

      {/* Right Side - Login Form */}
      <div className="flex-1 flex items-center justify-center p-8">
        <div className="w-full max-w-md">
          {/* Mobile Logo */}
          <div className="lg:hidden flex flex-col items-center mb-8">
            <img
              src="/assets/logo/logo_square.png"
              alt="Buurman"
              className="h-24 w-24 rounded-xl shadow-2xl mb-4"
            />
            <h1 className="text-4xl font-black tracking-tight bg-gradient-to-r from-[#5c7cfa] to-[#364fc7] bg-clip-text text-transparent">
              Buurman
            </h1>
            <p className="text-[#6b7194] dark:text-[#8b90a8] mt-2 text-center">
              Property management for small landlords
            </p>
          </div>

          <div className="bg-white dark:bg-[#14161f] rounded-2xl shadow-xl p-8 lg:p-10 relative overflow-hidden">
            {/* Decorative gradient accent */}
            <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-[#364fc7] via-[#5c7cfa] to-[#91a7ff]" />

            <div className="mb-8 text-center">
              <div className="inline-flex items-center gap-1.5 bg-[#5c7cfa]/10 text-[#4c6ef5] px-3 py-1 rounded-full text-xs font-medium mb-4">
                <Sparkles className="h-3.5 w-3.5" />
                Free for small landlords
              </div>
              <h2 className="text-2xl font-bold text-[#1a1d2e] dark:text-[#eef0f6] mb-2">
                Welcome back
              </h2>
              <p className="text-[#6b7194] dark:text-[#8b90a8]">
                Your properties are waiting for you
              </p>
            </div>

            <button
              onClick={() => login()}
              className="group w-full bg-gradient-to-r from-[#4263eb] to-[#5c7cfa] text-white py-3.5 px-6 rounded-xl hover:from-[#3b5bdb] hover:to-[#4c6ef5] transition-all duration-200 font-semibold flex items-center justify-center gap-3 shadow-lg hover:shadow-xl transform hover:-translate-y-0.5"
            >
              <LogIn className="h-5 w-5" />
              Sign in to your account
              <ArrowRight className="h-4 w-4 opacity-0 -ml-4 group-hover:opacity-100 group-hover:ml-0 transition-all duration-200" />
            </button>

            <div className="mt-6 relative">
              <div className="absolute inset-0 flex items-center">
                <div className="w-full border-t border-[#e2e6f0] dark:border-[#2a2e3f]" />
              </div>
              <div className="relative flex justify-center text-xs">
                <span className="bg-white dark:bg-[#14161f] px-3 text-[#6b7194] dark:text-[#8b90a8]">or</span>
              </div>
            </div>

            <a
              href="/register"
              className="mt-6 w-full py-3 px-6 rounded-xl border-2 border-[#e2e6f0] dark:border-[#2a2e3f] text-[#3d4463] dark:text-[#c4c8db] font-semibold flex items-center justify-center gap-2 hover:border-[#5c7cfa] hover:text-[#5c7cfa] transition-all duration-200 hover:bg-[#f0f4ff] dark:hover:bg-[#5c7cfa]/10"
            >
              Create a free account
              <ArrowRight className="h-4 w-4" />
            </a>

          </div>

          {/* Additional Info */}
          <div className="mt-6 text-center">
            <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
              Need help?{' '}
              <a href="https://www.buurman.io/support" target="_blank" rel="noopener noreferrer" className="text-[#5c7cfa] hover:underline">
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
