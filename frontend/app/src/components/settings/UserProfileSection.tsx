import { useState, useEffect, useRef, useCallback } from 'react';
import {
  User,
  Mail,
  Lock,
  Camera,
  Save,
  X,
  Loader2,
  CheckCircle2,
  AlertTriangle,
  MessageSquare,
} from 'lucide-react';
import {
  useCurrentUser,
  useUpdateUserProfile,
  useVerifyPhone,
  useResendPhoneVerification,
  useCancelPhoneVerification,
  usePhonePolicy,
} from '../../hooks/useUserPreferencesHooks';
import { useTranslation } from 'react-i18next';
import { PhoneInput, validatePhoneE164 } from '@/components/common/PhoneInput';
import { parsePhoneNumber } from 'libphonenumber-js/max';
import type { PhoneNumberPolicyResponse } from '@/types/users';

const NUMBER_TYPE_LABELS: Record<string, string> = {
  MOBILE: 'mobile',
  FIXED_LINE: 'fixed line',
  FIXED_LINE_OR_MOBILE: 'fixed line or mobile',
  VOIP: 'VoIP',
  TOLL_FREE: 'toll-free',
  PREMIUM_RATE: 'premium rate',
  SHARED_COST: 'shared cost',
  PERSONAL_NUMBER: 'personal number',
  PAGER: 'pager',
  UAN: 'UAN',
};

function validatePhoneAgainstPolicy(
  phone: string,
  policy: PhoneNumberPolicyResponse
): string | null {
  try {
    const parsed = parsePhoneNumber(phone);
    if (!parsed) {
      return null;
    }

    const country = parsed.country;
    if (!country) {
      return null;
    }

    const allowedTypes = policy.policyMatrix[country];
    if (!allowedTypes || allowedTypes.length === 0) {
      return 'Phone numbers from this country are not allowed';
    }

    const phoneType = parsed.getType();
    if (phoneType) {
      const mappedType = phoneType
        .replace(/_/g, '')
        .replace(/\s+/g, '_')
        .toUpperCase();
      const typeMap: Record<string, string> = {
        MOBILE: 'MOBILE',
        FIXED_LINE: 'FIXED_LINE',
        FIXED_LINE_OR_MOBILE: 'FIXED_LINE_OR_MOBILE',
        VOIP: 'VOIP',
        TOLL_FREE: 'TOLL_FREE',
        PREMIUM_RATE: 'PREMIUM_RATE',
        SHARED_COST: 'SHARED_COST',
        PERSONAL_NUMBER: 'PERSONAL_NUMBER',
        PAGER: 'PAGER',
        UAN: 'UAN',
      };
      const policyType = typeMap[mappedType];
      if (policyType && !allowedTypes.includes(policyType)) {
        const label =
          NUMBER_TYPE_LABELS[policyType] || policyType.toLowerCase();
        return `This phone number type (${label}) is not allowed`;
      }
    }
  } catch {
    // Parse failed — let server-side validation handle it
  }
  return null;
}

export const UserProfileSection = () => {
  const { t } = useTranslation('settings');
  const { data: currentUser, isLoading, isError, refetch } = useCurrentUser();
  const updateProfileMutation = useUpdateUserProfile();
  const verifyPhoneMutation = useVerifyPhone();
  const resendMutation = useResendPhoneVerification();
  const cancelVerificationMutation = useCancelPhoneVerification();
  const { data: phonePolicy } = usePhonePolicy();

  const [isEditing, setIsEditing] = useState(false);
  const [userData, setUserData] = useState(() => ({
    firstName: currentUser?.firstName ?? '',
    lastName: currentUser?.lastName ?? '',
    email: currentUser?.email ?? '',
    phone: currentUser?.phone ?? '',
    avatarUrl: null as string | null,
  }));

  const [lastSyncedUser, setLastSyncedUser] = useState(currentUser);
  if (currentUser && currentUser !== lastSyncedUser) {
    setLastSyncedUser(currentUser);
    setUserData({
      firstName: currentUser.firstName ?? '',
      lastName: currentUser.lastName ?? '',
      email: currentUser.email ?? '',
      phone: currentUser.phone ?? '',
      avatarUrl: null,
    });
  }

  // Phone verification state
  const [showVerification, setShowVerification] = useState(false);
  const [verificationCode, setVerificationCode] = useState('');
  const COOLDOWN_KEY = 'buurman-phone-verify-cooldown';
  const [cooldown, setCooldown] = useState(() => {
    try {
      const expiresAt = Number(localStorage.getItem(COOLDOWN_KEY) ?? 0);
      const remaining = Math.ceil((expiresAt - Date.now()) / 1000);
      return remaining > 0 ? remaining : 0;
    } catch {
      return 0;
    }
  });
  const cooldownRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const codeInputRef = useRef<HTMLInputElement>(null);

  const startCooldown = useCallback(() => {
    const seconds = 60;
    setCooldown(seconds);
    try {
      localStorage.setItem(COOLDOWN_KEY, String(Date.now() + seconds * 1000));
    } catch {
      /* noop */
    }
    if (cooldownRef.current) {
      clearInterval(cooldownRef.current);
    }
    cooldownRef.current = setInterval(() => {
      setCooldown((prev) => {
        if (prev <= 1) {
          if (cooldownRef.current) {
            clearInterval(cooldownRef.current);
          }
          try {
            localStorage.removeItem(COOLDOWN_KEY);
          } catch {
            /* noop */
          }
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
  }, []);

  // Resume cooldown tick on mount if time remains
  useEffect(() => {
    if (cooldown > 0 && !cooldownRef.current) {
      cooldownRef.current = setInterval(() => {
        setCooldown((prev) => {
          if (prev <= 1) {
            if (cooldownRef.current) {
              clearInterval(cooldownRef.current);
            }
            cooldownRef.current = null;
            try {
              localStorage.removeItem(COOLDOWN_KEY);
            } catch {
              /* noop */
            }
            return 0;
          }
          return prev - 1;
        });
      }, 1000);
    }
    return () => {
      if (cooldownRef.current) {
        clearInterval(cooldownRef.current);
      }
      cooldownRef.current = null;
    };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // Auto-focus code input when verification UI shows
  useEffect(() => {
    if (showVerification && codeInputRef.current) {
      codeInputRef.current.focus();
    }
  }, [showVerification]);

  const handleStartEdit = () => {
    setIsEditing(true);
  };

  const handleCancelEdit = () => {
    if (currentUser) {
      setUserData({
        firstName: currentUser.firstName ?? '',
        lastName: currentUser.lastName ?? '',
        email: currentUser.email ?? '',
        phone: currentUser.phone ?? '',
        avatarUrl: null,
      });
    }
    setIsEditing(false);
    setShowVerification(false);
    setVerificationCode('');
  };

  const [phoneError, setPhoneError] = useState('');

  const handleSaveProfile = () => {
    const phoneErr = validatePhoneE164(userData.phone || null);
    if (phoneErr) {
      setPhoneError(phoneErr);
      return;
    }
    // Validate against phone number policy (country + type)
    if (userData.phone && phonePolicy) {
      const policyErr = validatePhoneAgainstPolicy(userData.phone, phonePolicy);
      if (policyErr) {
        setPhoneError(policyErr);
        return;
      }
    }
    setPhoneError('');
    updateProfileMutation.mutate(
      {
        firstName: userData.firstName,
        lastName: userData.lastName,
        phone: userData.phone || undefined,
      },
      {
        onSuccess: (data) => {
          setIsEditing(false);
          // If phone is set but not verified, show verification UI
          if (data.phone && !data.phoneVerified) {
            setShowVerification(true);
            setVerificationCode('');
            startCooldown();
          } else {
            setShowVerification(false);
          }
        },
      }
    );
  };

  const handleVerifyCode = useCallback(
    (code: string) => {
      if (code.length !== 6) {
        return;
      }
      verifyPhoneMutation.mutate(code, {
        onSuccess: () => {
          setShowVerification(false);
          setVerificationCode('');
        },
      });
    },
    [verifyPhoneMutation]
  );

  const handleCodeChange = (value: string) => {
    const cleaned = value.replace(/\D/g, '').slice(0, 6);
    setVerificationCode(cleaned);
    if (cleaned.length === 6) {
      handleVerifyCode(cleaned);
    }
  };

  const handleResend = () => {
    if (cooldown > 0) {
      return;
    }
    resendMutation.mutate(undefined, {
      onSuccess: () => {
        setVerificationCode('');
        startCooldown();
      },
    });
  };

  const handleAvatarUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const previewUrl = URL.createObjectURL(file);
      setUserData({ ...userData, avatarUrl: previewUrl });
    }
  };

  const phoneVerified = currentUser?.phoneVerified ?? false;
  const hasPhone = !!currentUser?.phone;
  const needsVerification = hasPhone && !phoneVerified;

  const allowedCountryCodes = phonePolicy
    ? Object.keys(phonePolicy.policyMatrix).filter(
        (code) => phonePolicy.policyMatrix[code]?.length > 0
      )
    : undefined;

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <Loader2 className="h-8 w-8 animate-spin text-primary-500 dark:text-primary-300" />
      </div>
    );
  }

  if (isError) {
    return (
      <div className="flex flex-col items-center justify-center py-12 gap-4">
        <AlertTriangle className="h-8 w-8 text-warning-text" />
        <p className="text-sm text-text-secondary">
          {t('profile.failedToLoad')}
        </p>
        <button
          onClick={() => refetch()}
          className="px-4 py-2 bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors text-sm"
        >
          {t('profile.tryAgain')}
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Profile Information Card */}
      <div className="bg-surface-card rounded-lg shadow-sm">
        <div className="p-6 border-b border-border-default">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-xl font-semibold text-text-primary">
                {t('profile.title')}
              </h2>
              <p className="text-sm text-text-secondary mt-1">
                {t('profile.subtitle')}
              </p>
            </div>
            {!isEditing && (
              <button
                onClick={handleStartEdit}
                className="px-4 py-2 bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors"
              >
                {t('profile.editProfile')}
              </button>
            )}
          </div>
        </div>

        <div className="p-6">
          {/* Avatar Section */}
          <div className="flex items-center gap-6 mb-6 pb-6 border-b border-border-default">
            <div className="relative">
              {userData.avatarUrl ? (
                <img
                  src={userData.avatarUrl}
                  alt="Profile"
                  className="h-24 w-24 rounded-full object-cover"
                />
              ) : (
                <div className="h-24 w-24 rounded-full bg-info-bg flex items-center justify-center">
                  <User className="h-12 w-12 text-primary-500 dark:text-primary-300" />
                </div>
              )}
              {isEditing && (
                <label
                  htmlFor="avatar-upload"
                  className="absolute bottom-0 right-0 h-8 w-8 bg-primary-500 rounded-full flex items-center justify-center cursor-pointer hover:bg-primary-600 transition-colors"
                >
                  <Camera className="h-4 w-4 text-white" />
                  <input
                    id="avatar-upload"
                    type="file"
                    accept="image/*"
                    onChange={handleAvatarUpload}
                    className="hidden"
                  />
                </label>
              )}
            </div>
            <div>
              <h3 className="text-lg font-semibold text-text-primary">
                {userData.firstName} {userData.lastName}
              </h3>
              <p className="text-sm text-text-secondary">{userData.email}</p>
            </div>
          </div>

          {/* Profile Form */}
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-text-secondary mb-1">
                  {t('profile.firstName')}
                </label>
                <input
                  type="text"
                  value={userData.firstName}
                  onChange={(e) =>
                    setUserData({ ...userData, firstName: e.target.value })
                  }
                  disabled={!isEditing}
                  className="w-full px-3 py-2 border border-border-strong rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent disabled:bg-surface-inset disabled:cursor-not-allowed"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-text-secondary mb-1">
                  {t('profile.lastName')}
                </label>
                <input
                  type="text"
                  value={userData.lastName}
                  onChange={(e) =>
                    setUserData({ ...userData, lastName: e.target.value })
                  }
                  disabled={!isEditing}
                  className="w-full px-3 py-2 border border-border-strong rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent disabled:bg-surface-inset disabled:cursor-not-allowed"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-text-secondary mb-1">
                {t('profile.emailAddress')}
              </label>
              <div className="relative">
                <Mail className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-text-muted " />
                <input
                  type="email"
                  value={userData.email}
                  disabled
                  className="w-full pl-10 pr-3 py-2 border border-border-strong rounded-lg bg-surface-inset cursor-not-allowed"
                />
              </div>
              <p className="text-xs text-text-secondary mt-1">
                {t('profile.emailChangeNote')}
              </p>
            </div>

            <div>
              <div className="flex items-center gap-2 mb-1">
                <label className="block text-sm font-medium text-text-secondary">
                  {t('profile.phoneNumber')}
                </label>
                {hasPhone &&
                  !isEditing &&
                  (phoneVerified ? (
                    <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-success-bg text-success-text">
                      <CheckCircle2 className="h-3 w-3" />
                      {t('profile.verified')}
                    </span>
                  ) : (
                    <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-warning-bg text-warning-text">
                      <AlertTriangle className="h-3 w-3" />
                      {t('profile.unverified')}
                    </span>
                  ))}
              </div>
              <PhoneInput
                value={userData.phone ?? null}
                onChange={(e164) => {
                  setUserData({ ...userData, phone: e164 ?? '' });
                  if (phoneError) {
                    setPhoneError('');
                  }
                }}
                disabled={!isEditing}
                error={phoneError}
                allowedCountryCodes={allowedCountryCodes}
              />
            </div>

            {/* Phone Verification UI */}
            {!isEditing && (needsVerification || showVerification) && (
              <div className="rounded-lg border border-primary-500/20 bg-gradient-to-br from-primary-50 to-white dark:from-primary-500/5 dark:to-transparent p-4">
                <div className="flex items-center gap-3">
                  <div className="p-2 rounded-lg bg-primary-500/10 shrink-0">
                    <MessageSquare className="h-4 w-4 text-primary-500" />
                  </div>
                  <p className="text-sm font-semibold text-text-primary">
                    {t('profile.verification.enterCode')}{' '}
                    <span className="text-primary-500">
                      {currentUser?.phone}
                    </span>
                  </p>
                </div>
                <div className="flex items-center gap-2 mt-3 ml-11">
                  <input
                    ref={codeInputRef}
                    type="text"
                    inputMode="numeric"
                    autoComplete="one-time-code"
                    placeholder="000000"
                    value={verificationCode}
                    onChange={(e) => handleCodeChange(e.target.value)}
                    disabled={verifyPhoneMutation.isPending}
                    className="w-28 px-3 py-1.5 text-center text-base font-mono tracking-[0.3em] border border-border-strong rounded-lg bg-surface-card text-text-primary focus:ring-2 focus:ring-primary-500/20 focus:border-primary-500 disabled:opacity-50 transition-colors"
                  />
                  {verifyPhoneMutation.isPending && (
                    <Loader2 className="h-4 w-4 animate-spin text-primary-500" />
                  )}
                </div>
                {verifyPhoneMutation.isError && (
                  <p className="text-xs text-error-text mt-2 ml-11">
                    {t('profile.verification.invalidCode')}
                  </p>
                )}
                <div className="mt-2 ml-11 flex items-center gap-3 text-xs">
                  <button
                    onClick={handleResend}
                    disabled={cooldown > 0 || resendMutation.isPending}
                    className="font-medium text-primary-500 hover:text-primary-600 disabled:text-text-secondary disabled:cursor-not-allowed transition-colors"
                  >
                    {resendMutation.isPending
                      ? t('profile.verification.sending')
                      : cooldown > 0
                        ? t('profile.verification.resendIn', {
                            seconds: cooldown,
                          })
                        : t('profile.verification.resendCode')}
                  </button>
                  <span className="text-text-disabled">|</span>
                  <button
                    onClick={() => {
                      cancelVerificationMutation.mutate(undefined, {
                        onSuccess: () => {
                          setShowVerification(false);
                          setVerificationCode('');
                          setIsEditing(true);
                        },
                      });
                    }}
                    disabled={cancelVerificationMutation.isPending}
                    className="font-medium text-text-secondary hover:text-text-secondary disabled:cursor-not-allowed transition-colors"
                  >
                    {cancelVerificationMutation.isPending
                      ? t('profile.verification.cancelling')
                      : t('profile.verification.changeNumber')}
                  </button>
                </div>
              </div>
            )}

            {isEditing && (
              <div className="flex justify-end gap-3 pt-4 border-t border-border-default">
                <button
                  onClick={handleCancelEdit}
                  className="px-4 py-2 border border-border-strong text-text-secondary rounded-lg hover:bg-surface-inset transition-colors flex items-center gap-2"
                >
                  <X className="h-4 w-4" />
                  {t('common:buttons.cancel')}
                </button>
                <button
                  onClick={handleSaveProfile}
                  disabled={updateProfileMutation.isPending}
                  className="px-4 py-2 bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors flex items-center gap-2 disabled:opacity-50"
                >
                  {updateProfileMutation.isPending ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : (
                    <Save className="h-4 w-4" />
                  )}
                  {t('common:buttons.saveChanges')}
                </button>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Security Settings Card */}
      <div className="bg-surface-card rounded-lg shadow-sm">
        <div className="p-6 border-b border-border-default">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-xl font-semibold text-text-primary">
                {t('security.title')}
              </h2>
              <p className="text-sm text-text-secondary mt-1">
                {t('security.subtitle')}
              </p>
            </div>
          </div>
        </div>

        <div className="p-6">
          <div className="flex items-center justify-between p-4 bg-surface-page rounded-lg">
            <div className="flex items-center gap-3">
              <Lock className="h-5 w-5 text-text-secondary " />
              <div>
                <p className="font-medium text-text-primary">
                  {t('security.password')}
                </p>
                <p className="text-sm text-text-secondary">
                  {t('security.passwordNote')}
                </p>
              </div>
            </div>
            <a
              href="/auth/change-password"
              className="px-4 py-2 bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors"
            >
              {t('security.changePassword')}
            </a>
          </div>
        </div>
      </div>
    </div>
  );
};
