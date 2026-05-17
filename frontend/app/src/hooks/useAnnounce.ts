import { useCallback } from 'react';

const LIVE_REGION_ID = 'a11y-live';

function ensureLiveRegion(): HTMLDivElement {
  let el = document.getElementById(LIVE_REGION_ID) as HTMLDivElement | null;
  if (!el) {
    el = document.createElement('div');
    el.id = LIVE_REGION_ID;
    el.setAttribute('role', 'status');
    el.setAttribute('aria-live', 'polite');
    el.setAttribute('aria-atomic', 'true');
    // Visually hidden but accessible.
    el.style.cssText = [
      'position:absolute',
      'width:1px',
      'height:1px',
      'padding:0',
      'margin:-1px',
      'overflow:hidden',
      'clip:rect(0,0,0,0)',
      'white-space:nowrap',
      'border:0',
    ].join(';');
    document.body.appendChild(el);
  }
  return el;
}

/**
 * Imperative screen-reader announcer. Writes a brief message to a live region
 * so VoiceOver / TalkBack / JAWS announce success / progress / errors without
 * a visible toast.
 *
 * Use for: refresh complete, mark paid, async errors, route changes the SR
 * wouldn't otherwise notice.
 */
export function useAnnounce(): (
  message: string,
  options?: { assertive?: boolean }
) => void {
  return useCallback((message, options) => {
    const el = ensureLiveRegion();
    el.setAttribute(
      'aria-live',
      options?.assertive ? 'assertive' : 'polite'
    );
    // Clear and rewrite — SRs only fire on content change.
    el.textContent = '';
    window.requestAnimationFrame(() => {
      el.textContent = message;
    });
  }, []);
}
