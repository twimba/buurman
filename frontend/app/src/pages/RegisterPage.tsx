import React, { useState, useEffect, useMemo, useRef } from 'react';
import { useTranslation } from 'react-i18next';
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
import { PublicBroadcastBanner } from '../components/common/BroadcastBanner';
import PublicLanguageSelector from '../components/common/PublicLanguageSelector';
import { useForceLightMode } from '../hooks/useForceLightMode';

const RegisterPage: React.FC = () => {
  const { t, i18n } = useTranslation('common');
  useForceLightMode();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const invitationToken = searchParams.get('invitation') ?? undefined;
  const invitationCodeParam = searchParams.get('code') ?? '';
  const { data: invitation } = useInvitation(invitationToken);
  const registerMutation = useRegister();
  const { data: registrationConfig } = useRegistrationConfig();
  const validateCodeMutation = useValidateInvitationCode();

  const invitationRequired = registrationConfig?.invitationRequired ?? false;

  const [invitationCode, setInvitationCode] = useState(invitationCodeParam);
  const [codeValidated, setCodeValidated] = useState(false);
  const [codeError, setCodeError] = useState('');

  const handleValidateCode = async (code: string) => {
    if (!code.trim()) {
      return;
    }
    setCodeError('');
    try {
      const result = await validateCodeMutation.mutateAsync(code.trim());
      if (result.valid) {
        setCodeValidated(true);
        setCodeError('');
      } else {
        setCodeValidated(false);
        setCodeError(t('auth.invalidCode'));
      }
    } catch {
      setCodeValidated(false);
      setCodeError(t('auth.validateCodeFailed'));
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
      {
        key: 'length',
        label: t('auth.passwordRules.length'),
        met: p.length >= 8,
      },
      {
        key: 'upper',
        label: t('auth.passwordRules.upper'),
        met: /[A-Z]/.test(p),
      },
      {
        key: 'lower',
        label: t('auth.passwordRules.lower'),
        met: /[a-z]/.test(p),
      },
      { key: 'digit', label: t('auth.passwordRules.digit'), met: /\d/.test(p) },
      {
        key: 'special',
        label: t('auth.passwordRules.special'),
        met: /[^A-Za-z0-9]/.test(p),
      },
      {
        key: 'notEmail',
        label: t('auth.passwordRules.notEmail'),
        met: p.length > 0 && (!email || p.toLowerCase() !== email),
      },
    ];
  }, [formData.password, formData.email, t]);

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

    if (!formData.email) {
      newErrors.email = t('auth.validation.emailRequired');
    }
    if (!formData.firstName) {
      newErrors.firstName = t('auth.validation.firstNameRequired');
    }
    if (!formData.lastName) {
      newErrors.lastName = t('auth.validation.lastNameRequired');
    }
    if (!formData.password) {
      newErrors.password = t('auth.validation.passwordRequired');
    } else if (!allRulesMet) {
      newErrors.password = t('auth.validation.passwordRequirements');
    }
    if (formData.password !== formData.confirmPassword) {
      newErrors.confirmPassword = t('auth.validation.passwordsMismatch');
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const submitForm = async () => {
    if (!validate()) {
      return;
    }

    try {
      await registerMutation.mutateAsync({
        email: formData.email,
        firstName: formData.firstName,
        lastName: formData.lastName,
        password: formData.password,
        language: i18n.language,
        invitationToken,
        registrationInvitationCode:
          invitationRequired && !invitationToken
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
      const err = error as {
        response?: { data?: { message?: string; detail?: string } };
      };
      const detail = err.response?.data?.detail ?? '';
      const isEmailTaken = detail.toLowerCase().includes('email already');
      setErrors({
        submit: err.response?.data?.message || t('auth.registrationFailed'),
        ...(isEmailTaken ? { emailTaken: 'true' } : {}),
      });
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await submitForm();
  };

  const handleCmdEnter = (e: React.KeyboardEvent) => {
    if ((e.metaKey || e.ctrlKey) && e.key === 'Enter') {
      e.preventDefault();
      submitForm();
    }
  };

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
          <p className="text-xl text-primary-200 mb-12">{t('auth.tagline')}</p>

          {/* Features */}
          <div className="space-y-6">
            <div className="flex items-start gap-4">
              <div className="bg-primary-500/30 p-3 rounded-lg">
                <Home className="h-6 w-6" />
              </div>
              <div>
                <h3 className="font-semibold text-lg mb-1">
                  {t('auth.features.manageProperties')}
                </h3>
                <p className="text-primary-200">
                  {t('auth.features.managePropertiesDesc')}
                </p>
              </div>
            </div>

            <div className="flex items-start gap-4">
              <div className="bg-primary-500/30 p-3 rounded-lg">
                <Users className="h-6 w-6" />
              </div>
              <div>
                <h3 className="font-semibold text-lg mb-1">
                  {t('auth.features.trackContacts')}
                </h3>
                <p className="text-primary-200">
                  {t('auth.features.trackContactsDesc')}
                </p>
              </div>
            </div>

            <div className="flex items-start gap-4">
              <div className="bg-primary-500/30 p-3 rounded-lg">
                <FileText className="h-6 w-6" />
              </div>
              <div>
                <h3 className="font-semibold text-lg mb-1">
                  {t('auth.features.handleFinances')}
                </h3>
                <p className="text-primary-200">
                  {t('auth.features.handleFinancesDesc')}
                </p>
              </div>
            </div>

            <div className="flex items-start gap-4">
              <div className="bg-primary-500/30 p-3 rounded-lg">
                <TrendingUp className="h-6 w-6" />
              </div>
              <div>
                <h3 className="font-semibold text-lg mb-1">
                  {t('auth.features.growBusiness')}
                </h3>
                <p className="text-primary-200">
                  {t('auth.features.growBusinessDesc')}
                </p>
              </div>
            </div>
          </div>
        </div>

        <div className="text-sm text-primary-300">
          © 2026 Buurman. Simple property management.
        </div>
      </div>

      {/* Right Side - Register Form */}
      <div className="flex-1 flex items-center justify-center p-8">
        <div className="w-full max-w-md">
          <PublicBroadcastBanner context="register" />
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
              {t('auth.taglineShort')}
            </p>
          </div>

          <div className="bg-surface-card rounded-2xl shadow-xl p-8 lg:p-10 relative overflow-hidden">
            {/* Decorative gradient accent */}
            <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-primary-900 via-primary-500 to-primary-300" />

            {registrationSuccess ? (
              <div className="flex flex-col items-center text-center py-8">
                <div className="relative mb-5">
                  <div className="bg-success-bg rounded-full p-5">
                    <PartyPopper className="h-10 w-10 text-success-text" />
                  </div>
                  <div className="absolute -top-1 -right-1 bg-success-text rounded-full p-1">
                    <Check className="h-3.5 w-3.5 text-white" />
                  </div>
                </div>
                <h2 className="text-2xl font-bold text-text-primary mb-2">
                  {t('auth.allSet')}
                </h2>
                <p className="text-text-secondary mb-6">
                  {t('auth.verificationSent')}
                </p>
                <a
                  href="/login"
                  className="group inline-flex items-center gap-2 bg-gradient-to-r from-primary-600 to-primary-500 text-white py-2.5 px-6 rounded-xl font-semibold shadow-lg hover:shadow-xl transition-all duration-200 hover:-translate-y-0.5"
                >
                  {t('auth.signInNow')}
                  <ArrowRight className="h-4 w-4 group-hover:translate-x-0.5 transition-transform" />
                </a>
              </div>
            ) : (
              <>
                <div className="mb-8 text-center">
                  <div className="inline-flex items-center gap-1.5 bg-primary-500/10 text-primary-600 px-3 py-1 rounded-full text-xs font-medium mb-4">
                    <Sparkles className="h-3.5 w-3.5" />
                    {t('auth.takesLessThanMinute')}
                  </div>
                  <h2 className="text-2xl font-bold text-text-primary mb-2">
                    {t('auth.createFreeAccount')}
                  </h2>
                  <p className="text-text-secondary">
                    {t('auth.joinLandlords')}
                  </p>
                </div>

                <form
                  onSubmit={handleSubmit}
                  onKeyDown={handleCmdEnter}
                  className="space-y-5"
                >
                  {/* Invitation Code Gate — skip when user has a team invitation */}
                  {invitationRequired && !invitationToken && (
                    <div
                      className={`rounded-lg border-2 p-4 transition-all duration-300 ${
                        codeValidated
                          ? 'border-success-border bg-success-bg'
                          : 'border-primary-200 bg-primary-50/50'
                      }`}
                    >
                      <div className="flex items-center gap-2 mb-3">
                        <Ticket className="h-4 w-4 text-primary-500" />
                        <span className="text-sm font-medium text-text-secondary">
                          {t('auth.invitationCode')}
                        </span>
                        {codeValidated && (
                          <span className="ml-auto inline-flex items-center gap-1 text-xs font-medium text-success-text">
                            <ShieldCheck className="h-3.5 w-3.5" />
                            {t('auth.verified')}
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
                              ? 'border-success-border bg-success-bg text-success-text'
                              : 'border-border-strong focus:border-primary-500 focus:ring-2 focus:ring-primary-500 focus:ring-opacity-20'
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
                            className="px-4 py-2.5 bg-primary-500 text-white text-sm font-medium rounded-lg hover:bg-primary-600 disabled:opacity-50 disabled:cursor-not-allowed transition-colors flex items-center gap-2 shrink-0"
                          >
                            {validateCodeMutation.isPending ? (
                              <Loader2 className="h-4 w-4 animate-spin" />
                            ) : (
                              t('auth.verify')
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
                            className="px-3 py-2.5 text-text-secondary hover:text-text-secondary text-sm rounded-lg hover:bg-surface-card/50 transition-colors shrink-0"
                          >
                            {t('auth.change')}
                          </button>
                        )}
                      </div>
                      {codeError && (
                        <div className="flex items-center gap-1.5 mt-2">
                          <XCircle className="h-3.5 w-3.5 text-error-text shrink-0" />
                          <span className="text-xs text-error-text">
                            {codeError}
                          </span>
                        </div>
                      )}
                      {!codeValidated && !codeError && (
                        <p className="text-xs text-text-secondary mt-2">
                          {t('auth.enterCodeHint')}
                        </p>
                      )}
                    </div>
                  )}

                  <div
                    className={
                      invitationRequired && !codeValidated && !invitationToken
                        ? 'opacity-40 pointer-events-none select-none'
                        : ''
                    }
                  >
                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <label className="block text-sm font-medium text-text-secondary mb-1.5">
                          {t('auth.firstName')}
                        </label>
                        <input
                          type="text"
                          name="firstName"
                          value={formData.firstName}
                          onChange={handleChange}
                          className="w-full border border-border-strong rounded-lg px-4 py-2.5 focus:border-primary-500 focus:ring-2 focus:ring-primary-500 focus:ring-opacity-20 transition-colors"
                          placeholder="John"
                        />
                        {errors.firstName && (
                          <p className="text-error-text text-xs mt-1">
                            {errors.firstName}
                          </p>
                        )}
                      </div>

                      <div>
                        <label className="block text-sm font-medium text-text-secondary mb-1.5">
                          {t('auth.lastName')}
                        </label>
                        <input
                          type="text"
                          name="lastName"
                          value={formData.lastName}
                          onChange={handleChange}
                          className="w-full border border-border-strong rounded-lg px-4 py-2.5 focus:border-primary-500 focus:ring-2 focus:ring-primary-500 focus:ring-opacity-20 transition-colors"
                          placeholder="Doe"
                        />
                        {errors.lastName && (
                          <p className="text-error-text text-xs mt-1">
                            {errors.lastName}
                          </p>
                        )}
                      </div>
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-text-secondary mb-1.5">
                        {t('auth.email')}
                      </label>
                      <input
                        type="email"
                        name="email"
                        value={formData.email}
                        onChange={handleChange}
                        readOnly={!!invitationToken && !!invitation?.email}
                        className={`w-full border border-border-strong rounded-lg px-4 py-2.5 focus:border-primary-500 focus:ring-2 focus:ring-primary-500 focus:ring-opacity-20 transition-colors ${invitationToken && invitation?.email ? 'bg-surface-inset text-text-secondary cursor-not-allowed' : ''}`}
                        placeholder="john.doe@example.com"
                      />
                      {invitationToken && invitation?.email && (
                        <p className="text-xs text-text-secondary mt-1">
                          {t('auth.emailPrefilled')}
                        </p>
                      )}
                      {errors.email && (
                        <p className="text-error-text text-xs mt-1">
                          {errors.email}
                        </p>
                      )}
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-text-secondary mb-1.5">
                        {t('auth.password')}
                      </label>
                      <div className="relative">
                        <input
                          type={showPassword ? 'text' : 'password'}
                          name="password"
                          value={formData.password}
                          onChange={handleChange}
                          className={`w-full border rounded-lg px-4 py-2.5 pr-11 focus:ring-2 focus:ring-primary-500 focus:ring-opacity-20 transition-colors ${
                            formData.password.length > 0 && allRulesMet
                              ? 'border-success-border focus:border-success-text'
                              : 'border-border-strong focus:border-primary-500'
                          }`}
                          placeholder={t('auth.createStrongPassword')}
                        />
                        <button
                          type="button"
                          onClick={() => setShowPassword((prev) => !prev)}
                          className="absolute right-2 top-1/2 -translate-y-1/2 p-1 rounded-md border border-transparent text-text-muted hover:text-primary-500 hover:bg-primary-50 hover:border-primary-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-500/20 focus-visible:border-primary-500 transition-colors"
                          aria-label={t('accessibility.togglePasswordVisibility')}
                        >
                          {showPassword ? (
                            <EyeOff className="h-[1.125rem] w-[1.125rem]" />
                          ) : (
                            <Eye className="h-[1.125rem] w-[1.125rem]" />
                          )}
                        </button>
                      </div>
                      {errors.password && (
                        <p className="text-error-text text-xs mt-1">
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
                              <Check className="h-3.5 w-3.5 text-success-text shrink-0" />
                            ) : (
                              <Circle className="h-3.5 w-3.5 text-text-disabled shrink-0" />
                            )}
                            <span
                              className={`text-xs ${rule.met ? 'text-success-text' : 'text-text-secondary '}`}
                            >
                              {rule.label}
                            </span>
                          </div>
                        ))}
                      </div>
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-text-secondary mb-1.5">
                        {t('auth.confirmPassword')}
                      </label>
                      <div className="relative">
                        <input
                          type={showConfirmPassword ? 'text' : 'password'}
                          name="confirmPassword"
                          value={formData.confirmPassword}
                          onChange={handleChange}
                          className={`w-full border rounded-lg px-4 py-2.5 pr-11 focus:ring-2 focus:ring-primary-500 focus:ring-opacity-20 transition-colors ${
                            passwordsMatch
                              ? 'border-success-border focus:border-success-text'
                              : passwordsMismatch
                                ? 'border-error-border focus:border-error-text'
                                : 'border-border-strong focus:border-primary-500'
                          }`}
                          placeholder={t('auth.reenterPassword')}
                        />
                        <button
                          type="button"
                          onClick={() =>
                            setShowConfirmPassword((prev) => !prev)
                          }
                          className="absolute right-2 top-1/2 -translate-y-1/2 p-1 rounded-md border border-transparent text-text-muted hover:text-primary-500 hover:bg-primary-50 hover:border-primary-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-500/20 focus-visible:border-primary-500 transition-colors"
                          aria-label={t('accessibility.togglePasswordVisibility')}
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
                              <Check className="h-3.5 w-3.5 text-success-text" />
                              <span className="text-xs text-success-text">
                                {t('auth.passwordsMatch')}
                              </span>
                            </>
                          ) : (
                            <>
                              <Circle className="h-3.5 w-3.5 text-error-text" />
                              <span className="text-xs text-error-text">
                                {t('auth.passwordsDoNotMatch')}
                              </span>
                            </>
                          )}
                        </div>
                      )}
                      {errors.confirmPassword &&
                        !formData.confirmPassword.length && (
                          <p className="text-error-text text-xs mt-1">
                            {errors.confirmPassword}
                          </p>
                        )}
                    </div>

                    {errors.submit && (
                      <div className="bg-error-bg border border-error-border rounded-lg p-3">
                        <p className="text-error-text text-sm">
                          {errors.submit}
                          {errors.emailTaken && (
                            <>
                              {' '}
                              <a
                                href="/login"
                                className="text-primary-500 hover:text-primary-600 font-semibold hover:underline"
                              >
                                {t('auth.goToLogin')}
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
                        (invitationRequired &&
                          !codeValidated &&
                          !invitationToken)
                      }
                      className="group w-full mt-3 bg-gradient-to-r from-primary-600 to-primary-500 text-white py-3.5 px-6 rounded-xl hover:from-primary-700 hover:to-primary-600 transition-all duration-200 font-semibold flex items-center justify-center gap-3 shadow-lg hover:shadow-xl transform hover:-translate-y-0.5 disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none disabled:hover:shadow-lg"
                    >
                      <UserPlus className="h-5 w-5" />
                      {registerMutation.isPending
                        ? t('auth.creatingAccount')
                        : t('auth.getStarted')}
                      {!registerMutation.isPending && (
                        <ArrowRight className="h-4 w-4 opacity-0 -ml-4 group-hover:opacity-100 group-hover:ml-0 transition-all duration-200 group-disabled:hidden" />
                      )}
                    </button>
                  </div>
                </form>

                <div className="mt-6 relative">
                  <div className="absolute inset-0 flex items-center">
                    <div className="w-full border-t border-border-default " />
                  </div>
                  <div className="relative flex justify-center text-xs">
                    <span className="bg-surface-card px-3 text-text-secondary">
                      {t('auth.alreadyHaveAccount')}
                    </span>
                  </div>
                </div>

                <a
                  href="/login"
                  className="mt-6 w-full py-3 px-6 rounded-lg border-2 border-border-default text-text-secondary font-semibold flex items-center justify-center gap-2 hover:border-primary-500 hover:text-primary-500 transition-all duration-200 hover:bg-primary-50"
                >
                  {t('auth.signInInstead')}
                  <ArrowRight className="h-4 w-4" />
                </a>

                <p className="mt-5 text-xs text-text-secondary text-center">
                  {t('auth.byCreatingAccount')}{' '}
                  <a
                    href="https://www.buurman.io/terms"
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-primary-500 hover:text-primary-600 hover:underline"
                  >
                    {t('auth.termsOfService')}
                  </a>
                </p>
              </>
            )}
          </div>

          {/* Additional Info */}
          <div className="mt-6 text-center space-y-3">
            <p className="text-sm text-text-secondary">
              {t('auth.needHelp')}{' '}
              <a
                href="https://www.buurman.io/support"
                target="_blank"
                rel="noopener noreferrer"
                className="text-primary-500 hover:underline"
              >
                {t('auth.contactSupport')}
              </a>
            </p>
            <PublicLanguageSelector />
          </div>
        </div>
      </div>
    </div>
  );
};

export default RegisterPage;
