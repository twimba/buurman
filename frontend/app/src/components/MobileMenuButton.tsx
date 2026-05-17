import { Menu } from 'lucide-react';
import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useMobileNav } from '@/context/MobileNavContext';

/**
 * Hamburger trigger meant to live inside a sticky page header on phone. Registers
 * itself with MobileNavContext so the Sidebar's floating fallback hamburger hides.
 *
 * Hidden via `md:hidden` — on tablet/desktop the sidebar is always present.
 */
export const MobileMenuButton = () => {
  const { t } = useTranslation('navigation');
  const { open, setHasOwnMenuButton } = useMobileNav();

  useEffect(() => {
    setHasOwnMenuButton(true);
    return () => setHasOwnMenuButton(false);
  }, [setHasOwnMenuButton]);

  return (
    <button
      type="button"
      onClick={open}
      aria-label={t('accessibility.openMenu', 'Open menu')}
      aria-controls="primary-sidebar"
      className="md:hidden inline-flex items-center justify-center min-h-touch min-w-touch p-2 -ml-2 rounded-lg text-text-primary hover:bg-surface-inset focus-ring"
    >
      <Menu className="h-6 w-6" />
    </button>
  );
};

MobileMenuButton.displayName = 'MobileMenuButton';
