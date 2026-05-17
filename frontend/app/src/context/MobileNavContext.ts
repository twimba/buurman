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
  /**
   * True while the iOS soft keyboard is up (visualViewport delta > 80px).
   * Bottom chrome (BottomTabBar, fixed CTAs) should hide while true.
   */
  isKeyboardOpen: boolean;
  /**
   * True when a page is currently rendering its own fixed bottom action bar
   * (e.g. MobileFormStepperFooter on Property/Contract forms). The BottomTabBar
   * hides while true so the two strips don't stack.
   */
  hasOwnBottomFooter: boolean;
  setHasOwnBottomFooter: (next: boolean) => void;
}

export const MobileNavContext = createContext<MobileNavContextValue>({
  open: () => {},
  close: () => {},
  isOpen: false,
  hasOwnMenuButton: false,
  setHasOwnMenuButton: () => {},
  isKeyboardOpen: false,
  hasOwnBottomFooter: false,
  setHasOwnBottomFooter: () => {},
});

export const useMobileNav = (): MobileNavContextValue =>
  useContext(MobileNavContext);
