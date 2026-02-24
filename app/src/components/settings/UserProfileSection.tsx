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
import { PhoneInput, validatePhoneE164 } from '@/components/common/PhoneInput';
import { parsePhoneNumber } from 'libphonenumber-js/max';
import type { PhoneNumberPolicyResponse } from '@/api/users';

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
        .replace(/_/g, ' ')
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
  const { data: currentUser, isLoading, isError, refetch } = useCurrentUser();
  const updateProfileMutation = useUpdateUserProfile();
  const verifyPhoneMutation = useVerifyPhone();
  const resendMutation = useResendPhoneVerification();
  const cancelVerificationMutation = useCancelPhoneVerification();
  const { data: phonePolicy } = usePhonePolicy();

  const [isEditing, setIsEditing] = useState(false);
  const [userData, setUserData] = useState(() => ({
    firstName: currentUser?.firstName || '',
    lastName: currentUser?.lastName || '',
    email: currentUser?.email || '',
    phone: currentUser?.phone || '',
    avatarUrl: null as string | null,
  }));

  const [lastSyncedUser, setLastSyncedUser] = useState(currentUser);
  if (currentUser && currentUser !== lastSyncedUser) {
    setLastSyncedUser(currentUser);
    setUserData({
      firstName: currentUser.firstName || '',
      lastName: currentUser.lastName || '',
      email: currentUser.email || '',
      phone: currentUser.phone || '',
      avatarUrl: null,
    });
  }

  // Phone verification state
  const [showVerification, setShowVerification] = useState(false);
  const [verificationCode, setVerificationCode] = useState('');
  const COOLDOWN_KEY = 'buurman-phone-verify-cooldown';
  const [cooldown, setCooldown] = useState(() => {
    try {
      const expiresAt = Number(localStorage.getItem(COOLDOWN_KEY) || 0);
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
        firstName: currentUser.firstName || '',
        lastName: currentUser.lastName || '',
        email: currentUser.email || '',
        phone: currentUser.phone || '',
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
        phone: userData.phone || null,
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
        <Loader2 className="h-8 w-8 animate-spin text-[#5c7cfa] dark:text-[#91a7ff]" />
      </div>
    );
  }

  if (isError) {
    return (
      <div className="flex flex-col items-center justify-center py-12 gap-4">
        <AlertTriangle className="h-8 w-8 text-amber-500" />
        <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
          Failed to load profile data
        </p>
        <button
          onClick={() => refetch()}
          className="px-4 py-2 bg-[#5c7cfa] text-white rounded-lg hover:bg-[#4c6ef5] transition-colors text-sm"
        >
          Try Again
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Profile Information Card */}
      <div className="bg-white dark:bg-[#14161f] rounded-lg shadow-sm">
        <div className="p-6 border-b border-[#e2e6f0] dark:border-[#2a2e3f]">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-xl font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                Profile Information
              </h2>
              <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] mt-1">
                Update your personal information and avatar
              </p>
            </div>
            {!isEditing && (
              <button
                onClick={handleStartEdit}
                className="px-4 py-2 bg-[#5c7cfa] text-white rounded-lg hover:bg-[#4c6ef5] dark:hover:bg-[#5c7cfa] transition-colors"
              >
                Edit Profile
              </button>
            )}
          </div>
        </div>

        <div className="p-6">
          {/* Avatar Section */}
          <div className="flex items-center gap-6 mb-6 pb-6 border-b border-[#e2e6f0] dark:border-[#2a2e3f]">
            <div className="relative">
              {userData.avatarUrl ? (
                <img
                  src={userData.avatarUrl}
                  alt="Profile"
                  className="h-24 w-24 rounded-full object-cover"
                />
              ) : (
                <div className="h-24 w-24 rounded-full bg-blue-100 dark:bg-blue-900 flex items-center justify-center">
                  <User className="h-12 w-12 text-primary-500 dark:text-primary-300" />
                </div>
              )}
              {isEditing && (
                <label
                  htmlFor="avatar-upload"
                  className="absolute bottom-0 right-0 h-8 w-8 bg-[#5c7cfa] dark:bg-blue-700 rounded-full flex items-center justify-center cursor-pointer hover:bg-[#4c6ef5] dark:hover:bg-[#5c7cfa] transition-colors"
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
              <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                {userData.firstName} {userData.lastName}
              </h3>
              <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                {userData.email}
              </p>
            </div>
          </div>

          {/* Profile Form */}
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
                  First Name
                </label>
                <input
                  type="text"
                  value={userData.firstName}
                  onChange={(e) =>
                    setUserData({ ...userData, firstName: e.target.value })
                  }
                  disabled={!isEditing}
                  className="w-full px-3 py-2 border border-[#c9cfd9] dark:border-[#3a3f54] dark:bg-[#1e2130] dark:text-[#eef0f6] rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:bg-[#f1f3f9] dark:bg-[#1e2130] dark:disabled:bg-[#3a3f54] disabled:cursor-not-allowed"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
                  Last Name
                </label>
                <input
                  type="text"
                  value={userData.lastName}
                  onChange={(e) =>
                    setUserData({ ...userData, lastName: e.target.value })
                  }
                  disabled={!isEditing}
                  className="w-full px-3 py-2 border border-[#c9cfd9] dark:border-[#3a3f54] dark:bg-[#1e2130] dark:text-[#eef0f6] rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:bg-[#f1f3f9] dark:bg-[#1e2130] dark:disabled:bg-[#3a3f54] disabled:cursor-not-allowed"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
                Email Address
              </label>
              <div className="relative">
                <Mail className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180]" />
                <input
                  type="email"
                  value={userData.email}
                  disabled
                  className="w-full pl-10 pr-3 py-2 border border-[#c9cfd9] dark:border-[#3a3f54] rounded-lg bg-[#f1f3f9] dark:bg-[#1e2130] dark:text-[#8b90a8] cursor-not-allowed"
                />
              </div>
              <p className="text-xs text-[#6b7194] dark:text-[#8b90a8] dark:text-[#5c6180] mt-1">
                Email address cannot be changed. Contact support if you need to
                update it.
              </p>
            </div>

            <div>
              <div className="flex items-center gap-2 mb-1">
                <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db]">
                  Phone Number
                </label>
                {hasPhone &&
                  !isEditing &&
                  (phoneVerified ? (
                    <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400">
                      <CheckCircle2 className="h-3 w-3" />
                      Verified
                    </span>
                  ) : (
                    <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400">
                      <AlertTriangle className="h-3 w-3" />
                      Unverified
                    </span>
                  ))}
              </div>
              <PhoneInput
                value={userData.phone || null}
                onChange={(e164) => {
                  setUserData({ ...userData, phone: e164 || '' });
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
              <div className="rounded-xl border border-[#5c7cfa]/20 dark:border-[#5c7cfa]/15 bg-gradient-to-br from-[#f0f4ff] to-white dark:from-[#5c7cfa]/5 dark:to-transparent p-4">
                <div className="flex items-center gap-3">
                  <div className="p-2 rounded-lg bg-[#5c7cfa]/10 shrink-0">
                    <MessageSquare className="h-4 w-4 text-[#5c7cfa]" />
                  </div>
                  <p className="text-sm font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                    Almost there! Enter the code sent to{' '}
                    <span className="text-[#5c7cfa]">{currentUser?.phone}</span>
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
                    className="w-28 px-3 py-1.5 text-center text-base font-mono tracking-[0.3em] border border-[#c9cfd9] dark:border-[#3a3f54] rounded-lg bg-white dark:bg-[#1a1d2e] text-[#1a1d2e] dark:text-[#eef0f6] focus:ring-2 focus:ring-[#5c7cfa]/20 focus:border-[#5c7cfa] disabled:opacity-50 transition-colors"
                  />
                  {verifyPhoneMutation.isPending && (
                    <Loader2 className="h-4 w-4 animate-spin text-[#5c7cfa]" />
                  )}
                </div>
                {verifyPhoneMutation.isError && (
                  <p className="text-xs text-red-600 dark:text-red-400 mt-2 ml-11">
                    Invalid or expired code. Please try again.
                  </p>
                )}
                <div className="mt-2 ml-11 flex items-center gap-3 text-xs">
                  <button
                    onClick={handleResend}
                    disabled={cooldown > 0 || resendMutation.isPending}
                    className="font-medium text-[#5c7cfa] hover:text-[#4263eb] disabled:text-[#6b7194] dark:disabled:text-[#5c6180] disabled:cursor-not-allowed transition-colors"
                  >
                    {resendMutation.isPending
                      ? 'Sending...'
                      : cooldown > 0
                        ? `Resend in ${cooldown}s`
                        : 'Resend code'}
                  </button>
                  <span className="text-[#c9cfd9] dark:text-[#3a3f54]">|</span>
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
                    className="font-medium text-[#6b7194] dark:text-[#8b90a8] hover:text-[#3d4463] dark:hover:text-[#c4c8db] disabled:cursor-not-allowed transition-colors"
                  >
                    {cancelVerificationMutation.isPending
                      ? 'Cancelling...'
                      : 'Change number'}
                  </button>
                </div>
              </div>
            )}

            {isEditing && (
              <div className="flex justify-end gap-3 pt-4 border-t border-[#e2e6f0] dark:border-[#2a2e3f]">
                <button
                  onClick={handleCancelEdit}
                  className="px-4 py-2 border border-[#c9cfd9] dark:border-[#3a3f54] text-[#3d4463] dark:text-[#c4c8db] rounded-lg hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] transition-colors flex items-center gap-2"
                >
                  <X className="h-4 w-4" />
                  Cancel
                </button>
                <button
                  onClick={handleSaveProfile}
                  disabled={updateProfileMutation.isPending}
                  className="px-4 py-2 bg-[#5c7cfa] text-white rounded-lg hover:bg-[#4c6ef5] transition-colors flex items-center gap-2 disabled:opacity-50"
                >
                  {updateProfileMutation.isPending ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : (
                    <Save className="h-4 w-4" />
                  )}
                  Save Changes
                </button>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Security Settings Card */}
      <div className="bg-white dark:bg-[#14161f] rounded-lg shadow-sm">
        <div className="p-6 border-b border-[#e2e6f0] dark:border-[#2a2e3f]">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-xl font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                Security Settings
              </h2>
              <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] mt-1">
                Manage your password and security preferences
              </p>
            </div>
          </div>
        </div>

        <div className="p-6">
          <div className="flex items-center justify-between p-4 bg-[#f8f9fc] dark:bg-[#0c0d14] dark:bg-[#1e2130] rounded-lg">
            <div className="flex items-center gap-3">
              <Lock className="h-5 w-5 text-[#6b7194] dark:text-[#8b90a8]" />
              <div>
                <p className="font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                  Password
                </p>
                <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                  Password changes are managed through your identity provider
                </p>
              </div>
            </div>
            <a
              href="/auth/change-password"
              className="px-4 py-2 bg-[#5c7cfa] text-white rounded-lg hover:bg-[#4c6ef5] transition-colors"
            >
              Change Password
            </a>
          </div>
        </div>
      </div>
    </div>
  );
};
