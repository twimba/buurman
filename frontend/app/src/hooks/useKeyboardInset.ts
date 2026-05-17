import { useEffect, useState } from 'react';

const KEYBOARD_OPEN_THRESHOLD_PX = 80;

/**
 * Publishes the iOS on-screen keyboard intrusion as a CSS variable `--kbd-inset`
 * (in pixels) on `<html>`, AND toggles a `data-keyboard-open` attribute on
 * `<html>` when the inset crosses {@link KEYBOARD_OPEN_THRESHOLD_PX}. Sticky
 * footers can ride above the keyboard with:
 *
 *     bottom: calc(var(--kbd-inset, 0px) + var(--safe-bottom));
 *
 * Returns a boolean so consumers can conditionally hide chrome (e.g. the
 * bottom tab bar) when the keyboard is up — the data-attribute is a
 * fallback for CSS-only consumers.
 *
 * The threshold (80px) filters out small `visualViewport` jitter from
 * Safari's URL bar collapse — only a real keyboard pushes 200px+.
 *
 * On desktop / Android the value stays at `0px` because `visualViewport.height`
 * equals `window.innerHeight`.
 *
 * Mount once at the app root.
 */
export function useKeyboardInset(): boolean {
  const [isKeyboardOpen, setIsKeyboardOpen] = useState(false);

  useEffect(() => {
    const vv = window.visualViewport;
    if (!vv) {
      return;
    }
    // Baseline `visualViewport.height` at mount represents "no keyboard,
    // browser chrome in its current state". Comparing against this rather
    // than `window.innerHeight` makes the math robust under
    // `interactive-widget=resizes-content`, where window.innerHeight
    // SHRINKS with the keyboard and the original formula reads ~0.
    let baselineHeight = vv.height;

    const apply = () => {
      // If the layout viewport grew (rotation, browser-UI hide), refresh
      // baseline so the next keyboard-open computation is correct.
      if (vv.height > baselineHeight) {
        baselineHeight = vv.height;
      }
      const inset = Math.max(0, Math.round(baselineHeight - vv.height));
      document.documentElement.style.setProperty('--kbd-inset', `${inset}px`);
      const open = inset > KEYBOARD_OPEN_THRESHOLD_PX;
      if (open) {
        document.documentElement.setAttribute('data-keyboard-open', 'true');
      } else {
        document.documentElement.removeAttribute('data-keyboard-open');
      }
      setIsKeyboardOpen(open);
    };
    vv.addEventListener('resize', apply);
    vv.addEventListener('scroll', apply);
    apply();
    return () => {
      vv.removeEventListener('resize', apply);
      vv.removeEventListener('scroll', apply);
      document.documentElement.style.setProperty('--kbd-inset', '0px');
      document.documentElement.removeAttribute('data-keyboard-open');
    };
  }, []);

  return isKeyboardOpen;
}
