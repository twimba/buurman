import { createContext, useContext } from 'react';

interface MobileNavContextValue {
  /** Open the mobile drawer (no-op on `lg+`). */
  open: () => void;
  /** Close the mobile drawer. */
  close: () => void;
  /** Whether the drawer is currently open. */
  isOpen: boolean;
  /**
   * True when a page-level header (e.g. ListPageHeader) is currently rendering
   * its own menu trigger. The Sidebar's floating hamburger hides while true to
   * avoid double-rendering.
   */
  hasOwnMenuButton: boolean;
  setHasOwnMenuButton: (next: boolean) => void;
}

export const MobileNavContext = createContext<MobileNavContextValue>({
  open: () => {},
  close: () => {},
  isOpen: false,
  hasOwnMenuButton: false,
  setHasOwnMenuButton: () => {},
});

export const useMobileNav = (): MobileNavContextValue =>
  useContext(MobileNavContext);
