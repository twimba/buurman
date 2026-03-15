import {
  createContext,
  useContext,
  useState,
  useCallback,
  useEffect,
  type ReactNode,
} from 'react';
import { setImpersonating } from '../utils/analytics';

interface ImpersonationState {
  active: boolean;
  token: string | null;
  sessionIdentifier: string | null;
  adminEmail: string | null;
  adminName: string | null;
  reason: string | null;
  targetTeamIdentifier: string | null;
  mode: string | null;
  expiresAt: number | null;
  targetUserEmail: string | null;
}

interface ImpersonationContextType extends ImpersonationState {
  startImpersonation: (data: {
    token: string;
    sessionIdentifier: string;
    adminEmail: string;
    adminName: string;
    mode: string;
    expiresIn: number;
    targetUserEmail: string;
    reason?: string;
    targetTeamIdentifier?: string;
  }) => void;
  endImpersonation: () => void;
  remainingSeconds: number;
}

const ImpersonationContext = createContext<ImpersonationContextType | null>(
  null
);

const STORAGE_KEY = 'buurman-impersonation';

function loadState(): ImpersonationState {
  try {
    const stored = sessionStorage.getItem(STORAGE_KEY);
    if (stored) {
      const parsed = JSON.parse(stored) as ImpersonationState;
      // Check if expired
      if (parsed.expiresAt && Date.now() < parsed.expiresAt) {
        return parsed;
      }
      sessionStorage.removeItem(STORAGE_KEY);
    }
  } catch {
    // ignore
  }
  return {
    active: false,
    token: null,
    sessionIdentifier: null,
    adminEmail: null,
    adminName: null,
    reason: null,
    targetTeamIdentifier: null,
    mode: null,
    expiresAt: null,
    targetUserEmail: null,
  };
}

export function ImpersonationProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<ImpersonationState>(loadState);
  const [remainingSeconds, setRemainingSeconds] = useState(0);

  // Persist state to sessionStorage
  useEffect(() => {
    if (state.active) {
      sessionStorage.setItem(STORAGE_KEY, JSON.stringify(state));
      setImpersonating(true);
    } else {
      sessionStorage.removeItem(STORAGE_KEY);
      setImpersonating(false);
    }
  }, [state]);

  // Countdown timer
  useEffect(() => {
    if (!state.active || !state.expiresAt) {
      return;
    }

    const expiresAt = state.expiresAt;
    const update = () => {
      const remaining = Math.max(
        0,
        Math.floor((expiresAt - Date.now()) / 1000)
      );
      setRemainingSeconds(remaining);
      if (remaining <= 0) {
        setState((s) => ({ ...s, active: false, token: null }));
      }
    };

    update();
    const interval = setInterval(update, 1000);
    return () => clearInterval(interval);
  }, [state.active, state.expiresAt]);

  const startImpersonation = useCallback(
    (data: {
      token: string;
      sessionIdentifier: string;
      adminEmail: string;
      adminName: string;
      mode: string;
      expiresIn: number;
      targetUserEmail: string;
      reason?: string;
      targetTeamIdentifier?: string;
    }) => {
      setState({
        active: true,
        token: data.token,
        sessionIdentifier: data.sessionIdentifier,
        adminEmail: data.adminEmail,
        adminName: data.adminName,
        reason: data.reason ?? null,
        targetTeamIdentifier: data.targetTeamIdentifier ?? null,
        mode: data.mode,
        expiresAt: Date.now() + data.expiresIn * 1000,
        targetUserEmail: data.targetUserEmail,
      });
    },
    []
  );

  const endImpersonation = useCallback(() => {
    setState({
      active: false,
      token: null,
      sessionIdentifier: null,
      adminEmail: null,
      adminName: null,
      reason: null,
      targetTeamIdentifier: null,
      mode: null,
      expiresAt: null,
      targetUserEmail: null,
    });
  }, []);

  return (
    <ImpersonationContext.Provider
      value={{
        ...state,
        startImpersonation,
        endImpersonation,
        remainingSeconds,
      }}
    >
      {children}
    </ImpersonationContext.Provider>
  );
}

export function useImpersonation() {
  const context = useContext(ImpersonationContext);
  if (!context) {
    throw new Error(
      'useImpersonation must be used within an ImpersonationProvider'
    );
  }
  return context;
}
