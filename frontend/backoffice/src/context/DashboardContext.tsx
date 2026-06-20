import {
  createContext,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from 'react';

interface DashboardContextValue {
  /** When true, all panel polling is disabled (screencast / demo mode). Resets on reload. */
  paused: boolean;
  togglePaused: () => void;
  /** When true, the bento grid is in layout-edit mode (drag/hide/add). */
  editing: boolean;
  toggleEditing: () => void;
}

const DashboardContext = createContext<DashboardContextValue | null>(null);

export const DashboardProvider = ({ children }: { children: ReactNode }) => {
  const [paused, setPaused] = useState(false);
  const [editing, setEditing] = useState(false);
  const value = useMemo(
    () => ({
      paused,
      togglePaused: () => setPaused((p) => !p),
      editing,
      toggleEditing: () => setEditing((e) => !e),
    }),
    [paused, editing]
  );
  return (
    <DashboardContext.Provider value={value}>
      {children}
    </DashboardContext.Provider>
  );
};

export const useDashboardContext = (): DashboardContextValue => {
  const ctx = useContext(DashboardContext);
  if (!ctx) {
    throw new Error(
      'useDashboardContext must be used within a DashboardProvider'
    );
  }
  return ctx;
};
