import { createContext, useContext } from 'react';

interface MobileNavContextValue {
  /** Open the mobile drawer (no-op on `lg+`). */
  open: () => void;
  /** Close the mobile drawer. */
  close: () => void;
  /** Whether the drawer is currently open. */
  isOpen: boolean;
}

export const MobileNavContext = createContext<MobileNavContextValue>({
  open: () => {},
  close: () => {},
  isOpen: false,
});

export const useMobileNav = (): MobileNavContextValue =>
  useContext(MobileNavContext);
