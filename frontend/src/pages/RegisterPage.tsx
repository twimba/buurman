import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useRegister } from '../hooks/useAuthHooks';
import { UserPlus, Home, Users, FileText, TrendingUp } from 'lucide-react';

const RegisterPage: React.FC = () => {
  const navigate = useNavigate();
  const registerMutation = useRegister();

  const [formData, setFormData] = useState({
    email: '',
    firstName: '',
    lastName: '',
    password: '',
    confirmPassword: '',
  });

  const [errors, setErrors] = useState<Record<string, string>>({});

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
    setErrors({ ...errors, [e.target.name]: '' });
  };

  const validate = () => {
    const newErrors: Record<string, string> = {};

    if (!formData.email) newErrors.email = 'Email is required';
    if (!formData.firstName) newErrors.firstName = 'First name is required';
    if (!formData.lastName) newErrors.lastName = 'Last name is required';
    if (!formData.password) newErrors.password = 'Password is required';
    else if (formData.password.length < 8)
      newErrors.password = 'Password must be at least 8 characters';
    if (formData.password !== formData.confirmPassword)
      newErrors.confirmPassword = 'Passwords do not match';

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!validate()) return;

    try {
      await registerMutation.mutateAsync({
        email: formData.email,
        firstName: formData.firstName,
        lastName: formData.lastName,
        password: formData.password,
      });
      alert('Registration successful! Please login.');
      navigate('/login');
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } } };
      setErrors({
        submit: err.response?.data?.message || 'Registration failed',
      });
    }
  };

  return (
    <div className="flex min-h-screen bg-gradient-to-br from-blue-50 via-white to-blue-50">
      {/* Left Side - Branding */}
      <div className="hidden lg:flex lg:w-1/2 bg-gradient-to-br from-blue-600 to-blue-800 p-12 flex-col justify-between text-white">
        <div>
          <div className="flex items-center gap-4 mb-8">
            <img
              src="/assets/logo/logo_square.png"
              alt="Buurman"
              className="h-20 w-20 rounded-xl shadow-2xl ring-4 ring-white ring-opacity-30"
            />
            <h1 className="text-4xl font-black tracking-tight bg-gradient-to-r from-white to-blue-100 bg-clip-text text-transparent">
              Buurman
            </h1>
          </div>
          <p className="text-xl text-blue-100 mb-12">
            Property management made simple for small landlords
          </p>

          {/* Features */}
          <div className="space-y-6">
            <div className="flex items-start gap-4">
              <div className="bg-blue-500 bg-opacity-30 p-3 rounded-lg">
                <Home className="h-6 w-6" />
              </div>
              <div>
                <h3 className="font-semibold text-lg mb-1">
                  Manage Properties
                </h3>
                <p className="text-blue-100">
                  Keep track of all your rental properties in one place
                </p>
              </div>
            </div>

            <div className="flex items-start gap-4">
              <div className="bg-blue-500 bg-opacity-30 p-3 rounded-lg">
                <Users className="h-6 w-6" />
              </div>
              <div>
                <h3 className="font-semibold text-lg mb-1">Track Tenants</h3>
                <p className="text-blue-100">
                  Manage tenant information and lease agreements
                </p>
              </div>
            </div>

            <div className="flex items-start gap-4">
              <div className="bg-blue-500 bg-opacity-30 p-3 rounded-lg">
                <FileText className="h-6 w-6" />
              </div>
              <div>
                <h3 className="font-semibold text-lg mb-1">Handle Finances</h3>
                <p className="text-blue-100">
                  Monitor payments, expenses, and financial reports
                </p>
              </div>
            </div>

            <div className="flex items-start gap-4">
              <div className="bg-blue-500 bg-opacity-30 p-3 rounded-lg">
                <TrendingUp className="h-6 w-6" />
              </div>
              <div>
                <h3 className="font-semibold text-lg mb-1">
                  Grow Your Business
                </h3>
                <p className="text-blue-100">
                  Scale your rental portfolio with confidence
                </p>
              </div>
            </div>
          </div>
        </div>

        <div className="text-sm text-blue-200">
          © 2026 Buurman. Simple property management.
        </div>
      </div>

      {/* Right Side - Register Form */}
      <div className="flex-1 flex items-center justify-center p-8">
        <div className="w-full max-w-md">
          {/* Mobile Logo */}
          <div className="lg:hidden flex flex-col items-center mb-8">
            <img
              src="/assets/logo/logo_square.png"
              alt="Buurman"
              className="h-24 w-24 rounded-xl shadow-2xl mb-4"
            />
            <h1 className="text-4xl font-black tracking-tight bg-gradient-to-r from-blue-600 to-blue-800 bg-clip-text text-transparent">
              Buurman
            </h1>
            <p className="text-gray-600 dark:text-gray-400 mt-2 text-center">
              Property management for small landlords
            </p>
          </div>

          <div className="bg-white dark:bg-gray-800 rounded-2xl shadow-xl p-8 lg:p-10">
            <div className="mb-8">
              <h2 className="text-2xl font-bold text-gray-900 dark:text-gray-100 mb-2">
                Create your account
              </h2>
              <p className="text-gray-600 dark:text-gray-400">
                Start managing your properties today
              </p>
            </div>

            <form onSubmit={handleSubmit} className="space-y-5">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                    First Name
                  </label>
                  <input
                    type="text"
                    name="firstName"
                    value={formData.firstName}
                    onChange={handleChange}
                    className="w-full border border-gray-300 dark:border-gray-600 rounded-lg px-4 py-2.5 focus:border-blue-600 focus:ring-2 focus:ring-blue-600 focus:ring-opacity-20 transition-colors"
                    placeholder="John"
                  />
                  {errors.firstName && (
                    <p className="text-red-600 text-xs mt-1">
                      {errors.firstName}
                    </p>
                  )}
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                    Last Name
                  </label>
                  <input
                    type="text"
                    name="lastName"
                    value={formData.lastName}
                    onChange={handleChange}
                    className="w-full border border-gray-300 dark:border-gray-600 rounded-lg px-4 py-2.5 focus:border-blue-600 focus:ring-2 focus:ring-blue-600 focus:ring-opacity-20 transition-colors"
                    placeholder="Doe"
                  />
                  {errors.lastName && (
                    <p className="text-red-600 text-xs mt-1">
                      {errors.lastName}
                    </p>
                  )}
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                  Email
                </label>
                <input
                  type="email"
                  name="email"
                  value={formData.email}
                  onChange={handleChange}
                  className="w-full border border-gray-300 dark:border-gray-600 rounded-lg px-4 py-2.5 focus:border-blue-600 focus:ring-2 focus:ring-blue-600 focus:ring-opacity-20 transition-colors"
                  placeholder="john.doe@example.com"
                />
                {errors.email && (
                  <p className="text-red-600 text-xs mt-1">{errors.email}</p>
                )}
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                  Password
                </label>
                <input
                  type="password"
                  name="password"
                  value={formData.password}
                  onChange={handleChange}
                  className="w-full border border-gray-300 dark:border-gray-600 rounded-lg px-4 py-2.5 focus:border-blue-600 focus:ring-2 focus:ring-blue-600 focus:ring-opacity-20 transition-colors"
                  placeholder="At least 8 characters"
                />
                {errors.password && (
                  <p className="text-red-600 text-xs mt-1">{errors.password}</p>
                )}
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                  Confirm Password
                </label>
                <input
                  type="password"
                  name="confirmPassword"
                  value={formData.confirmPassword}
                  onChange={handleChange}
                  className="w-full border border-gray-300 dark:border-gray-600 rounded-lg px-4 py-2.5 focus:border-blue-600 focus:ring-2 focus:ring-blue-600 focus:ring-opacity-20 transition-colors"
                  placeholder="Re-enter your password"
                />
                {errors.confirmPassword && (
                  <p className="text-red-600 text-xs mt-1">
                    {errors.confirmPassword}
                  </p>
                )}
              </div>

              {errors.submit && (
                <div className="bg-red-50 border border-red-200 rounded-lg p-3">
                  <p className="text-red-600 text-sm">{errors.submit}</p>
                </div>
              )}

              <button
                type="submit"
                disabled={registerMutation.isPending}
                className="w-full bg-blue-600 text-white py-3.5 px-6 rounded-lg hover:bg-blue-700 transition-all duration-200 font-semibold flex items-center justify-center gap-3 shadow-lg hover:shadow-xl transform hover:-translate-y-0.5 disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none"
              >
                <UserPlus className="h-5 w-5" />
                {registerMutation.isPending
                  ? 'Creating account...'
                  : 'Create account'}
              </button>
            </form>

            <div className="mt-8 text-center">
              <p className="text-sm text-gray-600 dark:text-gray-400">
                Already have an account?{' '}
                <a
                  href="/login"
                  className="text-blue-600 hover:text-blue-700 font-semibold hover:underline"
                >
                  Sign in here
                </a>
              </p>
            </div>

            {/* Divider */}
            <div className="mt-8 pt-6 border-t border-gray-200 dark:border-gray-700">
              <p className="text-xs text-gray-500 dark:text-gray-400 text-center">
                By creating an account, you agree to our Terms of Service
              </p>
            </div>
          </div>

          {/* Additional Info */}
          <div className="mt-6 text-center">
            <p className="text-sm text-gray-500 dark:text-gray-400">
              Need help?{' '}
              <a href="#" className="text-blue-600 hover:underline">
                Contact support
              </a>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default RegisterPage;
