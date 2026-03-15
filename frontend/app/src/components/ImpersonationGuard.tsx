import type { ReactNode } from 'react';
import { useImpersonation } from '../context/ImpersonationContext';

interface ImpersonationGuardProps {
  children: ReactNode;
  blockInReadOnly?: boolean;
  blockAlways?: boolean;
  fallback?: ReactNode;
}

export function ImpersonationGuard({
  children,
  blockInReadOnly = false,
  blockAlways = false,
  fallback,
}: ImpersonationGuardProps) {
  const { active, mode } = useImpersonation();

  if (!active) {
    return <>{children}</>;
  }

  if (blockAlways) {
    return (
      <>
        {fallback ?? (
          <span className="text-sm text-text-muted">
            Not available during impersonation
          </span>
        )}
      </>
    );
  }

  if (blockInReadOnly && mode === 'READ_ONLY') {
    return (
      <>
        {fallback ?? (
          <span className="text-sm text-text-muted">Read-only mode</span>
        )}
      </>
    );
  }

  return <>{children}</>;
}
