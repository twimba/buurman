import { useEffect } from 'react';

/**
 * Publishes the iOS on-screen keyboard intrusion as a CSS variable `--kbd-inset`
 * (in pixels) on `<html>`. Sticky footers (e.g. Sheet action bars) can ride above
 * the keyboard with:
 *
 *     bottom: calc(var(--kbd-inset, 0px) + var(--safe-bottom));
 *
 * On desktop / Android the value stays at `0px` because `visualViewport.height`
 * equals `window.innerHeight`.
 *
 * Mount once at the app root.
 */
export function useKeyboardInset(): void {
  useEffect(() => {
    const vv = window.visualViewport;
    if (!vv) {
      return;
    }
    const apply = () => {
      const inset = window.innerHeight - vv.height - vv.offsetTop;
      document.documentElement.style.setProperty(
        '--kbd-inset',
        `${Math.max(0, Math.round(inset))}px`
      );
    };
    vv.addEventListener('resize', apply);
    vv.addEventListener('scroll', apply);
    apply();
    return () => {
      vv.removeEventListener('resize', apply);
      vv.removeEventListener('scroll', apply);
      document.documentElement.style.setProperty('--kbd-inset', '0px');
    };
  }, []);
}
