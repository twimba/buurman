import { Lock } from 'lucide-react';
import { useImpersonation } from '../context/ImpersonationContext';
import { endImpersonationSession } from '../generated/api/impersonation/impersonation';

function formatTime(seconds: number): string {
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${m}:${s.toString().padStart(2, '0')}`;
}

export function ImpersonationBanner() {
  const {
    active,
    adminEmail,
    adminName,
    reason,
    mode,
    remainingSeconds,
    endImpersonation,
  } = useImpersonation();

  if (!active) {
    return null;
  }

  const displayName = adminName || adminEmail;
  const isReadOnly = mode === 'READ_ONLY';

  const handleEnd = async () => {
    try {
      await endImpersonationSession();
    } catch {
      // Session may already be ended on server
    }
    endImpersonation();
  };

  return (
    <div className="fixed top-0 left-0 right-0 z-[9999] bg-amber-500 text-amber-950 px-4 py-2 text-center text-sm font-medium shadow-md">
      <span>
        Impersonation active — admin: <strong>{displayName}</strong>
        {isReadOnly && (
          <span className="inline-flex items-center gap-1 ml-1">
            <Lock className="inline h-3 w-3" />
            (read-only)
          </span>
        )}
        {' · '}
        Time remaining: <strong>{formatTime(remainingSeconds)}</strong>
      </span>
      {reason && (
        <span
          className="text-amber-900 text-xs opacity-80 ml-2"
          title={reason.replace(/<[^>]*>/g, '')}
          dangerouslySetInnerHTML={{ __html: reason }}
        />
      )}
      <button
        onClick={handleEnd}
        className="ml-4 px-3 py-1 bg-amber-700 text-white rounded text-xs font-semibold hover:bg-amber-800 transition-colors"
      >
        End Session
      </button>
    </div>
  );
}
