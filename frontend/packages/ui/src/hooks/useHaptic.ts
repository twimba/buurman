/**
 * Tiny haptic feedback hook. iOS Safari doesn't expose the Vibration API
 * (no-op there); Android browsers + most modern PWAs do. Caller doesn't
 * need to feature-detect — the hook silently no-ops when unsupported.
 *
 * The patterns are tuned for UI interactions, not gameplay:
 *   - selection: 10 ms — radio/checkbox flip
 *   - light:     15 ms — primary tap / nav
 *   - medium:    30 ms — destructive confirm
 *   - heavy:     50 ms — error / blocked action
 *   - success:  [15, 60, 25] — multi-pulse for "done"
 *   - warning:  [30, 30, 30] — "are you sure?"
 *
 * Plays nicely with prefers-reduced-motion — also disabled there since
 * haptics are a motion proxy on touch devices.
 */

export type HapticPattern =
  | 'selection'
  | 'light'
  | 'medium'
  | 'heavy'
  | 'success'
  | 'warning';

const PATTERNS: Record<HapticPattern, number | number[]> = {
  selection: 10,
  light: 15,
  medium: 30,
  heavy: 50,
  success: [15, 60, 25],
  warning: [30, 30, 30],
};

export function useHaptic(): (pattern?: HapticPattern) => void {
  return (pattern: HapticPattern = 'light') => {
    if (typeof navigator === 'undefined' || !('vibrate' in navigator)) {
      return;
    }
    if (
      typeof window !== 'undefined' &&
      window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
    ) {
      return;
    }
    try {
      navigator.vibrate(PATTERNS[pattern]);
    } catch {
      // Vibration can throw on some browsers when called in non-user-gesture
      // contexts. Silently swallow — feedback is best-effort.
    }
  };
}
