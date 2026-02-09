import React, { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  useVerifyEmail,
  useResendVerification,
  useCurrentUser,
} from '../hooks/useAuthHooks';
import { Mail, CheckCircle, AlertCircle } from 'lucide-react';

const RESEND_COOLDOWN_SECONDS = 60;

export const VerifyEmailPage: React.FC = () => {
  const navigate = useNavigate();
  const { data: user } = useCurrentUser();
  const verifyMutation = useVerifyEmail();
  const resendMutation = useResendVerification();

  const [digits, setDigits] = useState<string[]>(['', '', '', '', '', '']);
  const [error, setError] = useState('');
  const [verified, setVerified] = useState(false);
  const [cooldown, setCooldown] = useState(0);
  const inputRefs = useRef<(HTMLInputElement | null)[]>([]);

  // Redirect if already verified
  useEffect(() => {
    if (user?.emailVerified) {
      navigate('/dashboard', { replace: true });
    }
  }, [user, navigate]);

  // Cooldown timer
  useEffect(() => {
    if (cooldown <= 0) return;
    const timer = setTimeout(() => setCooldown((c) => c - 1), 1000);
    return () => clearTimeout(timer);
  }, [cooldown]);

  const handleChange = (index: number, value: string) => {
    if (!/^\d*$/.test(value)) return;

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
    if (pasted.length === 0) return;

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
      setError('Please enter all 6 digits');
      return;
    }

    try {
      await verifyMutation.mutateAsync(code);
      setVerified(true);
      setTimeout(() => navigate('/dashboard', { replace: true }), 2000);
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string } } };
      setError(error.response?.data?.message || 'Invalid verification code');
      setDigits(['', '', '', '', '', '']);
      inputRefs.current[0]?.focus();
    }
  };

  const handleResend = async () => {
    if (cooldown > 0 || resendMutation.isPending) return;
    try {
      await resendMutation.mutateAsync();
      setCooldown(RESEND_COOLDOWN_SECONDS);
    } catch {
      // Error handled by hook
    }
  };

  if (verified) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-blue-50 via-white to-blue-50">
        <div className="w-full max-w-md p-8">
          <div className="bg-white rounded-2xl shadow-xl p-8 text-center">
            <div className="bg-green-100 rounded-full p-4 inline-flex mb-5">
              <CheckCircle className="h-12 w-12 text-green-600" />
            </div>
            <h2 className="text-2xl font-bold text-[#1a1d2e] mb-2">
              Email verified!
            </h2>
            <p className="text-[#6b7194]">Redirecting to dashboard...</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-blue-50 via-white to-blue-50">
      <div className="w-full max-w-md p-8">
        <div className="bg-white dark:bg-[#14161f] rounded-2xl shadow-xl p-8">
          <div className="text-center mb-8">
            <div className="bg-blue-100 dark:bg-blue-900/30 rounded-full p-4 inline-flex mb-5">
              <Mail className="h-10 w-10 text-blue-600 dark:text-blue-400" />
            </div>
            <h2 className="text-2xl font-bold text-[#1a1d2e] dark:text-[#eef0f6] mb-2">
              Check your email
            </h2>
            <p className="text-[#6b7194] dark:text-[#8b90a8]">
              We sent a 6-digit verification code to
            </p>
            {user?.email && (
              <p className="text-[#1a1d2e] dark:text-[#eef0f6] font-medium mt-1">
                {user.email}
              </p>
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
                  className="w-12 h-14 text-center text-2xl font-bold border border-[#c9cfd9] dark:border-[#3a3f54] rounded-lg focus:border-[#5c7cfa] focus:ring-2 focus:ring-[#5c7cfa] focus:ring-opacity-20 transition-colors bg-white dark:bg-[#1a1d2e] text-[#1a1d2e] dark:text-[#eef0f6]"
                />
              ))}
            </div>

            {error && (
              <div className="flex items-center gap-2 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-3 mb-4">
                <AlertCircle className="h-4 w-4 text-red-600 dark:text-red-400 flex-shrink-0" />
                <p className="text-red-600 dark:text-red-400 text-sm">
                  {error}
                </p>
              </div>
            )}

            <button
              type="submit"
              disabled={verifyMutation.isPending || digits.some((d) => !d)}
              className="w-full bg-[#5c7cfa] text-white py-3 px-6 rounded-lg hover:bg-[#4c6ef5] transition-all duration-200 font-semibold shadow-lg hover:shadow-xl disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {verifyMutation.isPending ? 'Verifying...' : 'Verify email'}
            </button>
          </form>

          <div className="mt-6 text-center">
            <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
              Didn&apos;t receive the code?{' '}
              <button
                onClick={handleResend}
                disabled={cooldown > 0 || resendMutation.isPending}
                className="text-[#5c7cfa] hover:text-[#4263eb] font-semibold hover:underline disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {cooldown > 0
                  ? `Resend in ${cooldown}s`
                  : resendMutation.isPending
                    ? 'Sending...'
                    : 'Resend code'}
              </button>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};
