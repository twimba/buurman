import React, { useState, useEffect, useMemo, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import {
  useRegister,
  useRegistrationConfig,
  useValidateInvitationCode,
} from '../hooks/useAuthHooks';
import { useInvitation } from '../hooks/useTeamHooks';
import {
  UserPlus,
  Home,
  Users,
  FileText,
  TrendingUp,
  Check,
  Circle,
  Eye,
  EyeOff,
  Sparkles,
  ArrowRight,
  PartyPopper,
  Ticket,
  Loader2,
  ShieldCheck,
  XCircle,
} from 'lucide-react';

const RegisterPage: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const invitationToken = searchParams.get('invitation') || undefined;
  const invitationCodeParam = searchParams.get('code') || '';
  const { data: invitation } = useInvitation(invitationToken);
  const registerMutation = useRegister();
  const { data: registrationConfig } = useRegistrationConfig();
  const validateCodeMutation = useValidateInvitationCode();

  const invitationRequired = registrationConfig?.invitationRequired ?? false;

  const [invitationCode, setInvitationCode] = useState(invitationCodeParam);
  const [codeValidated, setCodeValidated] = useState(false);
  const [codeError, setCodeError] = useState('');

  const handleValidateCode = async (code: string) => {
    if (!code.trim()) return;
    setCodeError('');
    try {
      const result = await validateCodeMutation.mutateAsync(code.trim());
      if (result.valid) {
        setCodeValidated(true);
        setCodeError('');
      } else {
        setCodeValidated(false);
        setCodeError('Invalid or expired invitation code');
      }
    } catch {
      setCodeValidated(false);
      setCodeError('Could not validate code. Please try again.');
    }
  };

  // Auto-validate code from URL param
  const autoValidated = useRef(false);
  useEffect(() => {
    if (invitationCodeParam && invitationRequired && !autoValidated.current) {
      autoValidated.current = true;
      handleValidateCode(invitationCodeParam);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [invitationCodeParam, invitationRequired]);

  const [formData, setFormData] = useState({
    email: '',
    firstName: '',
    lastName: '',
    password: '',
    confirmPassword: '',
  });

  // Pre-fill email from invitation
  const [emailPrefilled, setEmailPrefilled] = useState(false);
  useEffect(() => {
    if (invitation?.email && !emailPrefilled) {
      setFormData((prev) => ({ ...prev, email: invitation.email }));
      setEmailPrefilled(true);
    }
  }, [invitation?.email, emailPrefilled]);

  const [errors, setErrors] = useState<Record<string, string>>({});
  const [registrationSuccess, setRegistrationSuccess] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  const passwordRules = useMemo(() => {
    const p = formData.password;
    const email = formData.email.toLowerCase();
    return [
      { key: 'length', label: '8+ characters', met: p.length >= 8 },
      { key: 'upper', label: 'Uppercase letter', met: /[A-Z]/.test(p) },
      { key: 'lower', label: 'Lowercase letter', met: /[a-z]/.test(p) },
      { key: 'digit', label: 'Number', met: /\d/.test(p) },
      {
        key: 'special',
        label: 'Special character',
        met: /[^A-Za-z0-9]/.test(p),
      },
      {
        key: 'notEmail',
        label: 'Not your email',
        met: p.length > 0 && (!email || p.toLowerCase() !== email),
      },
    ];
  }, [formData.password, formData.email]);

  const allRulesMet = passwordRules.every((r) => r.met);
  const passwordsMatch =
    formData.password.length > 0 &&
    formData.confirmPassword.length > 0 &&
    formData.password === formData.confirmPassword;
  const passwordsMismatch =
    formData.confirmPassword.length > 0 &&
    formData.password !== formData.confirmPassword;

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
    else if (!allRulesMet)
      newErrors.password = 'Password does not meet all requirements';
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
        invitationToken,
        registrationInvitationCode: invitationRequired
          ? invitationCode.trim()
          : undefined,
      });
      // Clear pending invitation since it was auto-accepted during registration
      if (invitationToken) {
        localStorage.removeItem('pendingInvitation');
      }
      setRegistrationSuccess(true);
      setTimeout(() => navigate('/login'), 3000);
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } } };
      setErrors({
        submit: err.response?.data?.message || 'Registration failed',
      });
    }
  };

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

            {registrationSuccess ? (
              <div className="flex flex-col items-center text-center py-8">
                <div className="relative mb-5">
                  <div className="bg-emerald-100 dark:bg-emerald-900/30 rounded-full p-5">
                    <PartyPopper className="h-10 w-10 text-emerald-600 dark:text-emerald-400" />
                  </div>
                  <div className="absolute -top-1 -right-1 bg-emerald-500 rounded-full p-1">
                    <Check className="h-3.5 w-3.5 text-white" />
                  </div>
                </div>
                <h2 className="text-2xl font-bold text-[#1a1d2e] dark:text-[#eef0f6] mb-2">
                  You&apos;re all set!
                </h2>
                <p className="text-[#6b7194] dark:text-[#8b90a8] mb-6">
                  We&apos;ve sent a verification email to your inbox.
                  <br />
                  Check it and you&apos;re ready to go!
                </p>
                <a
                  href="/login"
                  className="group inline-flex items-center gap-2 bg-gradient-to-r from-[#4263eb] to-[#5c7cfa] text-white py-2.5 px-6 rounded-xl font-semibold shadow-lg hover:shadow-xl transition-all duration-200 hover:-translate-y-0.5"
                >
                  Sign in now
                  <ArrowRight className="h-4 w-4 group-hover:translate-x-0.5 transition-transform" />
                </a>
              </div>
            ) : (
              <>
                <div className="mb-8 text-center">
                  <div className="inline-flex items-center gap-1.5 bg-[#5c7cfa]/10 text-[#4c6ef5] px-3 py-1 rounded-full text-xs font-medium mb-4">
                    <Sparkles className="h-3.5 w-3.5" />
                    Takes less than a minute
                  </div>
                  <h2 className="text-2xl font-bold text-[#1a1d2e] dark:text-[#eef0f6] mb-2">
                    Create your free account
                  </h2>
                  <p className="text-[#6b7194] dark:text-[#8b90a8]">
                    Join landlords who manage smarter, not harder
                  </p>
                </div>

                <form onSubmit={handleSubmit} className="space-y-5">
                  {/* Invitation Code Gate */}
                  {invitationRequired && (
                    <div
                      className={`rounded-xl border-2 p-4 transition-all duration-300 ${
                        codeValidated
                          ? 'border-emerald-200 bg-emerald-50/50 dark:border-emerald-800 dark:bg-emerald-900/20'
                          : 'border-[#c3cbf9] bg-[#f0f4ff]/50 dark:border-[#3a3f54] dark:bg-[#1e2130]'
                      }`}
                    >
                      <div className="flex items-center gap-2 mb-3">
                        <Ticket className="h-4 w-4 text-[#5c7cfa]" />
                        <span className="text-sm font-medium text-[#3d4463] dark:text-[#c4c8db]">
                          Invitation Code
                        </span>
                        {codeValidated && (
                          <span className="ml-auto inline-flex items-center gap-1 text-xs font-medium text-emerald-600 dark:text-emerald-400">
                            <ShieldCheck className="h-3.5 w-3.5" />
                            Verified
                          </span>
                        )}
                      </div>
                      <div className="flex gap-2">
                        <input
                          type="text"
                          value={invitationCode}
                          onChange={(e) => {
                            setInvitationCode(e.target.value);
                            setCodeValidated(false);
                            setCodeError('');
                          }}
                          disabled={codeValidated}
                          className={`flex-1 border rounded-lg px-4 py-2.5 text-sm transition-colors ${
                            codeValidated
                              ? 'border-emerald-300 bg-emerald-50 text-emerald-700 dark:border-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300'
                              : 'border-[#c9cfd9] dark:border-[#3a3f54] focus:border-[#5c7cfa] focus:ring-2 focus:ring-[#5c7cfa] focus:ring-opacity-20'
                          }`}
                          placeholder="e.g. snowy-cat"
                        />
                        {!codeValidated && (
                          <button
                            type="button"
                            onClick={() => handleValidateCode(invitationCode)}
                            disabled={
                              !invitationCode.trim() ||
                              validateCodeMutation.isPending
                            }
                            className="px-4 py-2.5 bg-[#5c7cfa] text-white text-sm font-medium rounded-lg hover:bg-[#4c6ef5] disabled:opacity-50 disabled:cursor-not-allowed transition-colors flex items-center gap-2 shrink-0"
                          >
                            {validateCodeMutation.isPending ? (
                              <Loader2 className="h-4 w-4 animate-spin" />
                            ) : (
                              'Verify'
                            )}
                          </button>
                        )}
                        {codeValidated && (
                          <button
                            type="button"
                            onClick={() => {
                              setCodeValidated(false);
                              setInvitationCode('');
                              setCodeError('');
                            }}
                            className="px-3 py-2.5 text-[#6b7194] hover:text-[#3d4463] text-sm rounded-lg hover:bg-white/50 transition-colors shrink-0"
                          >
                            Change
                          </button>
                        )}
                      </div>
                      {codeError && (
                        <div className="flex items-center gap-1.5 mt-2">
                          <XCircle className="h-3.5 w-3.5 text-red-500 shrink-0" />
                          <span className="text-xs text-red-600 dark:text-red-400">
                            {codeError}
                          </span>
                        </div>
                      )}
                      {!codeValidated && !codeError && (
                        <p className="text-xs text-[#6b7194] dark:text-[#8b90a8] mt-2">
                          Enter the invitation code you received to continue
                        </p>
                      )}
                    </div>
                  )}

                  <div
                    className={
                      invitationRequired && !codeValidated
                        ? 'opacity-40 pointer-events-none select-none'
                        : ''
                    }
                  >
                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1.5">
                          First Name
                        </label>
                        <input
                          type="text"
                          name="firstName"
                          value={formData.firstName}
                          onChange={handleChange}
                          className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded-lg px-4 py-2.5 focus:border-[#5c7cfa] focus:ring-2 focus:ring-[#5c7cfa] focus:ring-opacity-20 transition-colors"
                          placeholder="John"
                        />
                        {errors.firstName && (
                          <p className="text-red-600 text-xs mt-1">
                            {errors.firstName}
                          </p>
                        )}
                      </div>

                      <div>
                        <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1.5">
                          Last Name
                        </label>
                        <input
                          type="text"
                          name="lastName"
                          value={formData.lastName}
                          onChange={handleChange}
                          className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded-lg px-4 py-2.5 focus:border-[#5c7cfa] focus:ring-2 focus:ring-[#5c7cfa] focus:ring-opacity-20 transition-colors"
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
                      <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1.5">
                        Email
                      </label>
                      <input
                        type="email"
                        name="email"
                        value={formData.email}
                        onChange={handleChange}
                        readOnly={!!invitationToken && !!invitation?.email}
                        className={`w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded-lg px-4 py-2.5 focus:border-[#5c7cfa] focus:ring-2 focus:ring-[#5c7cfa] focus:ring-opacity-20 transition-colors ${invitationToken && invitation?.email ? 'bg-[#f1f3f9] dark:bg-[#1e2130] text-[#6b7194] dark:text-[#8b90a8] cursor-not-allowed' : ''}`}
                        placeholder="john.doe@example.com"
                      />
                      {invitationToken && invitation?.email && (
                        <p className="text-xs text-[#6b7194] dark:text-[#8b90a8] mt-1">
                          Email is pre-filled from your invitation
                        </p>
                      )}
                      {errors.email && (
                        <p className="text-red-600 text-xs mt-1">
                          {errors.email}
                        </p>
                      )}
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1.5">
                        Password
                      </label>
                      <div className="relative">
                        <input
                          type={showPassword ? 'text' : 'password'}
                          name="password"
                          value={formData.password}
                          onChange={handleChange}
                          className={`w-full border rounded-lg px-4 py-2.5 pr-11 focus:ring-2 focus:ring-[#5c7cfa] focus:ring-opacity-20 transition-colors ${
                            formData.password.length > 0 && allRulesMet
                              ? 'border-emerald-300 dark:border-emerald-700 focus:border-emerald-400'
                              : 'border-[#c9cfd9] dark:border-[#3a3f54] focus:border-[#5c7cfa]'
                          }`}
                          placeholder="Create a strong password"
                        />
                        <button
                          type="button"
                          onClick={() => setShowPassword((prev) => !prev)}
                          className="absolute right-2 top-1/2 -translate-y-1/2 p-1 rounded-md border border-transparent text-[#9ca3af] hover:text-[#5c7cfa] hover:bg-[#eff3ff] hover:border-[#c3cbf9] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#5c7cfa]/20 focus-visible:border-[#5c7cfa] transition-colors"
                          aria-label="Toggle password visibility"
                        >
                          {showPassword ? (
                            <EyeOff className="h-[1.125rem] w-[1.125rem]" />
                          ) : (
                            <Eye className="h-[1.125rem] w-[1.125rem]" />
                          )}
                        </button>
                      </div>
                      {errors.password && (
                        <p className="text-red-600 text-xs mt-1">
                          {errors.password}
                        </p>
                      )}
                      <div className="grid grid-cols-2 gap-x-4 gap-y-1 mt-2">
                        {passwordRules.map((rule) => (
                          <div
                            key={rule.key}
                            className="flex items-center gap-1.5"
                          >
                            {rule.met ? (
                              <Check className="h-3.5 w-3.5 text-emerald-500 shrink-0" />
                            ) : (
                              <Circle className="h-3.5 w-3.5 text-[#c9cfd9] dark:text-[#3a3f54] shrink-0" />
                            )}
                            <span
                              className={`text-xs ${rule.met ? 'text-emerald-600 dark:text-emerald-400' : 'text-[#6b7194] dark:text-[#8b90a8]'}`}
                            >
                              {rule.label}
                            </span>
                          </div>
                        ))}
                      </div>
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1.5">
                        Confirm Password
                      </label>
                      <div className="relative">
                        <input
                          type={showConfirmPassword ? 'text' : 'password'}
                          name="confirmPassword"
                          value={formData.confirmPassword}
                          onChange={handleChange}
                          className={`w-full border rounded-lg px-4 py-2.5 pr-11 focus:ring-2 focus:ring-[#5c7cfa] focus:ring-opacity-20 transition-colors ${
                            passwordsMatch
                              ? 'border-emerald-300 dark:border-emerald-700 focus:border-emerald-400'
                              : passwordsMismatch
                                ? 'border-red-300 dark:border-red-700 focus:border-red-400'
                                : 'border-[#c9cfd9] dark:border-[#3a3f54] focus:border-[#5c7cfa]'
                          }`}
                          placeholder="Re-enter your password"
                        />
                        <button
                          type="button"
                          onClick={() =>
                            setShowConfirmPassword((prev) => !prev)
                          }
                          className="absolute right-2 top-1/2 -translate-y-1/2 p-1 rounded-md border border-transparent text-[#9ca3af] hover:text-[#5c7cfa] hover:bg-[#eff3ff] hover:border-[#c3cbf9] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#5c7cfa]/20 focus-visible:border-[#5c7cfa] transition-colors"
                          aria-label="Toggle password visibility"
                        >
                          {showConfirmPassword ? (
                            <EyeOff className="h-[1.125rem] w-[1.125rem]" />
                          ) : (
                            <Eye className="h-[1.125rem] w-[1.125rem]" />
                          )}
                        </button>
                      </div>
                      {formData.confirmPassword.length > 0 && (
                        <div className="flex items-center gap-1.5 mt-1.5">
                          {passwordsMatch ? (
                            <>
                              <Check className="h-3.5 w-3.5 text-emerald-500" />
                              <span className="text-xs text-emerald-600 dark:text-emerald-400">
                                Passwords match
                              </span>
                            </>
                          ) : (
                            <>
                              <Circle className="h-3.5 w-3.5 text-red-400" />
                              <span className="text-xs text-red-500 dark:text-red-400">
                                Passwords do not match
                              </span>
                            </>
                          )}
                        </div>
                      )}
                      {errors.confirmPassword &&
                        !formData.confirmPassword.length && (
                          <p className="text-red-600 text-xs mt-1">
                            {errors.confirmPassword}
                          </p>
                        )}
                    </div>

                    {errors.submit && (
                      <div className="bg-red-50 border border-red-200 rounded-lg p-3">
                        <p className="text-red-600 text-sm">
                          {errors.submit}
                          {errors.submit
                            .toLowerCase()
                            .includes('email already registered') && (
                            <>
                              {' '}
                              <a
                                href="/login"
                                className="text-[#5c7cfa] hover:text-[#4263eb] font-semibold hover:underline"
                              >
                                Go to login
                              </a>
                            </>
                          )}
                        </p>
                      </div>
                    )}

                    <button
                      type="submit"
                      disabled={
                        registerMutation.isPending ||
                        !allRulesMet ||
                        !passwordsMatch ||
                        (invitationRequired && !codeValidated)
                      }
                      className="group w-full mt-3 bg-gradient-to-r from-[#4263eb] to-[#5c7cfa] text-white py-3.5 px-6 rounded-xl hover:from-[#3b5bdb] hover:to-[#4c6ef5] transition-all duration-200 font-semibold flex items-center justify-center gap-3 shadow-lg hover:shadow-xl transform hover:-translate-y-0.5 disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none disabled:hover:shadow-lg"
                    >
                      <UserPlus className="h-5 w-5" />
                      {registerMutation.isPending
                        ? 'Creating account...'
                        : 'Get started for free'}
                      {!registerMutation.isPending && (
                        <ArrowRight className="h-4 w-4 opacity-0 -ml-4 group-hover:opacity-100 group-hover:ml-0 transition-all duration-200 group-disabled:hidden" />
                      )}
                    </button>
                  </div>
                </form>

                <div className="mt-6 relative">
                  <div className="absolute inset-0 flex items-center">
                    <div className="w-full border-t border-[#e2e6f0] dark:border-[#2a2e3f]" />
                  </div>
                  <div className="relative flex justify-center text-xs">
                    <span className="bg-white dark:bg-[#14161f] px-3 text-[#6b7194] dark:text-[#8b90a8]">
                      already have an account?
                    </span>
                  </div>
                </div>

                <a
                  href="/login"
                  className="mt-6 w-full py-3 px-6 rounded-xl border-2 border-[#e2e6f0] dark:border-[#2a2e3f] text-[#3d4463] dark:text-[#c4c8db] font-semibold flex items-center justify-center gap-2 hover:border-[#5c7cfa] hover:text-[#5c7cfa] transition-all duration-200 hover:bg-[#f0f4ff] dark:hover:bg-[#5c7cfa]/10"
                >
                  Sign in instead
                  <ArrowRight className="h-4 w-4" />
                </a>

                <p className="mt-5 text-xs text-[#6b7194] dark:text-[#8b90a8] text-center">
                  By creating an account, you agree to our{' '}
                  <a
                    href="https://www.buurman.io/terms"
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-[#5c7cfa] hover:text-[#4263eb] hover:underline"
                  >
                    Terms of Service
                  </a>
                </p>
              </>
            )}
          </div>

          {/* Additional Info */}
          <div className="mt-6 text-center">
            <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
              Need help?{' '}
              <a
                href="https://www.buurman.io/support"
                target="_blank"
                rel="noopener noreferrer"
                className="text-[#5c7cfa] hover:underline"
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

export default RegisterPage;
