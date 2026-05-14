import React, { useState, useRef, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useSearchParams } from 'react-router-dom';
import {
  useVerifyEmail,
  useVerifyEmailByToken,
  useResendVerification,
  useCurrentUser,
} from '../hooks/useAuthHooks';
import { useAuth } from '../context/AuthContext';
import { Mail, CheckCircle, AlertCircle, Loader2 } from 'lucide-react';
import { useForceLightMode } from '../hooks/useForceLightMode';

const RESEND_COOLDOWN_SECONDS = 60;

export const VerifyEmailPage: React.FC = () => {
  const { t } = useTranslation('admin');
  useForceLightMode();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');

  const { isAuthenticated } = useAuth();
  const { data: user } = useCurrentUser(isAuthenticated);
  const verifyMutation = useVerifyEmail();
  const verifyTokenMutation = useVerifyEmailByToken();
  const resendMutation = useResendVerification();

  const [digits, setDigits] = useState<string[]>(['', '', '', '', '', '']);
  const [error, setError] = useState('');
  const [verified, setVerified] = useState(false);
  const [tokenVerifying, setTokenVerifying] = useState(!!token);
  const [cooldown, setCooldown] = useState(0);
  const inputRefs = useRef<(HTMLInputElement | null)[]>([]);
  const tokenVerifiedRef = useRef(false);

  // Redirect if already verified
  useEffect(() => {
    if (user?.emailVerified) {
      navigate('/dashboard', { replace: true });
    }
  }, [user, navigate]);

  // Auto-verify via token
  useEffect(() => {
    if (!token || tokenVerifiedRef.current) {
      return;
    }
    tokenVerifiedRef.current = true;

    verifyTokenMutation.mutate(token, {
      onSuccess: () => {
        setVerified(true);
        setTokenVerifying(false);
        setTimeout(() => navigate('/dashboard', { replace: true }), 2000);
      },
      onError: (err: unknown) => {
        setTokenVerifying(false);
        const error = err as { response?: { data?: { message?: string } } };
        setError(
          error.response?.data?.message || t('common:verifyEmail.invalidLink')
        );
      },
    });
    // verifyTokenMutation excluded — not referentially stable, tokenVerifiedRef guards double-fire
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token, navigate]);

  // Cooldown timer
  useEffect(() => {
    if (cooldown <= 0) {
      return;
    }
    const timer = setTimeout(() => setCooldown((c) => c - 1), 1000);
    return () => clearTimeout(timer);
  }, [cooldown]);

  const handleChange = (index: number, value: string) => {
    if (!/^\d*$/.test(value)) {
      return;
    }

    const newDigits = [...digits];
    newDigits[index] = value.slice(-1);
    setDigits(newDigits);
    setError('');

    if (value && index < 5) {
      inputRefs.current[index + 1]?.focus();
    }
  };

  const handleKeyDown = (index: number, e: React.KeyboardEvent) => {
    if (e.key === 'Backspace' && !digits[index] && index > 0) {
      inputRefs.current[index - 1]?.focus();
    }
  };

  const handlePaste = (e: React.ClipboardEvent) => {
    e.preventDefault();
    const pasted = e.clipboardData
      .getData('text')
      .replace(/\D/g, '')
      .slice(0, 6);
    if (pasted.length === 0) {
      return;
    }

    const newDigits = [...digits];
    for (let i = 0; i < pasted.length && i < 6; i++) {
      newDigits[i] = pasted[i];
    }
    setDigits(newDigits);
    setError('');

    const focusIndex = Math.min(pasted.length, 5);
    inputRefs.current[focusIndex]?.focus();
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const code = digits.join('');
    if (code.length !== 6) {
      setError(t('common:verifyEmail.enterAllDigits'));
      return;
    }

    try {
      await verifyMutation.mutateAsync(code);
      setVerified(true);
      setTimeout(() => navigate('/dashboard', { replace: true }), 2000);
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string } } };
      setError(
        error.response?.data?.message || t('common:verifyEmail.invalidCode')
      );
      setDigits(['', '', '', '', '', '']);
      inputRefs.current[0]?.focus();
    }
  };

  const handleResend = async () => {
    if (cooldown > 0 || resendMutation.isPending) {
      return;
    }
    try {
      await resendMutation.mutateAsync();
      setCooldown(RESEND_COOLDOWN_SECONDS);
    } catch {
      // Error handled by hook
    }
  };

  if (tokenVerifying) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-blue-50 via-white to-blue-50">
        <div className="w-full max-w-md p-8">
          <div className="bg-surface-card rounded-2xl shadow-xl p-8 text-center">
            <Loader2 className="h-12 w-12 text-primary-500 animate-spin mx-auto mb-5" />
            <h2 className="text-2xl font-bold text-text-primary mb-2">
              {t('common:verifyEmail.verifying')}
            </h2>
            <p className="text-text-secondary">
              {t('common:verifyEmail.pleaseWait')}
            </p>
          </div>
        </div>
      </div>
    );
  }

  if (verified) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-blue-50 via-white to-blue-50">
        <div className="w-full max-w-md p-8">
          <div className="bg-surface-card rounded-2xl shadow-xl p-8 text-center">
            <div className="bg-success-bg rounded-full p-4 inline-flex mb-5">
              <CheckCircle className="h-12 w-12 text-success-text" />
            </div>
            <h2 className="text-2xl font-bold text-text-primary mb-2">
              {t('common:verifyEmail.verified')}
            </h2>
            <p className="text-text-secondary">
              {t('common:verifyEmail.redirecting')}
            </p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-blue-50 via-white to-blue-50">
      <div className="w-full max-w-md p-8">
        <div className="bg-surface-card rounded-2xl shadow-xl p-8">
          <div className="text-center mb-8">
            <div className="bg-info-bg rounded-full p-4 inline-flex mb-5">
              <Mail className="h-10 w-10 text-info-text" />
            </div>
            <h2 className="text-2xl font-bold text-text-primary mb-2">
              {t('common:verifyEmail.checkEmail')}
            </h2>
            <p className="text-text-secondary">
              {t('common:verifyEmail.codeSent')}
            </p>
            {user?.email && (
              <p className="text-text-primary font-medium mt-1">{user.email}</p>
            )}
          </div>

          <form onSubmit={handleSubmit}>
            <div
              className="flex justify-center gap-3 mb-6"
              onPaste={handlePaste}
            >
              {digits.map((digit, index) => (
                <input
                  key={index}
                  ref={(el) => {
                    inputRefs.current[index] = el;
                  }}
                  type="text"
                  inputMode="numeric"
                  maxLength={1}
                  value={digit}
                  onChange={(e) => handleChange(index, e.target.value)}
                  onKeyDown={(e) => handleKeyDown(index, e)}
                  className="w-12 h-14 text-center text-2xl font-bold border border-border-strong rounded-lg focus:border-primary-500 focus:ring-2 focus:ring-primary-500 focus:ring-opacity-20 transition-colors bg-surface-card text-text-primary"
                />
              ))}
            </div>

            {error && (
              <div className="flex items-center gap-2 bg-error-bg border border-error-border rounded-lg p-3 mb-4">
                <AlertCircle className="h-4 w-4 text-error-text flex-shrink-0" />
                <p className="text-error-text text-sm">{error}</p>
              </div>
            )}

            <button
              type="submit"
              disabled={verifyMutation.isPending || digits.some((d) => !d)}
              className="w-full bg-primary-500 text-white py-3 px-6 rounded-lg hover:bg-primary-600 transition-all duration-200 font-semibold shadow-lg hover:shadow-xl disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {verifyMutation.isPending
                ? t('common:verifyEmail.verifyingButton')
                : t('common:verifyEmail.verifyButton')}
            </button>
          </form>

          <div className="mt-6 text-center">
            <p className="text-sm text-text-secondary">
              {t('common:verifyEmail.didntReceive')}{' '}
              <button
                onClick={handleResend}
                disabled={cooldown > 0 || resendMutation.isPending}
                className="text-primary-500 hover:text-primary-600 font-semibold hover:underline disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {cooldown > 0
                  ? t('common:verifyEmail.resendIn', { seconds: cooldown })
                  : resendMutation.isPending
                    ? t('common:verifyEmail.sending')
                    : t('common:verifyEmail.resendCode')}
              </button>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};
