import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import DOMPurify from 'dompurify';
import { Lock } from 'lucide-react';
import { useImpersonation } from '../context/ImpersonationContext';
import { endImpersonationSession } from '../generated/api/impersonation/impersonation';

function formatTime(seconds: number): string {
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${m}:${s.toString().padStart(2, '0')}`;
}

export function ImpersonationBanner() {
  const { t } = useTranslation('common');
  const {
    active,
    adminEmail,
    adminName,
    reason,
    mode,
    remainingSeconds,
    endImpersonation,
  } = useImpersonation();
  const [ended, setEnded] = useState(false);

  if (!active && !ended) {
    return null;
  }

  if (ended) {
    return (
      <div className="fixed top-0 left-0 right-0 z-[9999] bg-emerald-500 text-white px-4 py-3 text-center text-sm font-medium shadow-md">
        {t('impersonation.sessionEnded')}
      </div>
    );
  }

  const displayName = adminName || adminEmail;
  const isReadOnly = mode === 'READ_ONLY';

  const handleEnd = async () => {
    try {
      await endImpersonationSession();
    } catch (err) {
      console.error('Failed to end impersonation session on server:', err);
    }
    endImpersonation();
    setEnded(true);
  };

  return (
    <div className="fixed top-0 left-0 right-0 z-[9999] bg-amber-500 text-amber-950 px-4 py-2 text-center text-sm font-medium shadow-md">
      <span>
        {t('impersonation.active')} — {t('impersonation.admin')}: <strong>{displayName}</strong>
        {isReadOnly && (
          <span className="inline-flex items-center gap-1 ml-1">
            <Lock className="inline h-3 w-3" />
            ({t('impersonation.readOnly')})
          </span>
        )}
        {' · '}
        {t('impersonation.timeRemaining')}: <strong>{formatTime(remainingSeconds)}</strong>
      </span>
      {reason && (
        <span
          className="text-amber-900 text-xs opacity-80 ml-2"
          title={reason.replace(/<[^>]*>/g, '')}
          dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(reason) }}
        />
      )}
      <button
        onClick={handleEnd}
        className="ml-4 px-3 py-1 bg-amber-700 text-white rounded text-xs font-semibold hover:bg-amber-800 transition-colors"
      >
        {t('impersonation.endSession')}
      </button>
    </div>
  );
}

ImpersonationBanner.displayName = 'ImpersonationBanner';
