const legacyCopy = (text: string): boolean => {
  const textarea = document.createElement('textarea');
  textarea.value = text;
  textarea.setAttribute('readonly', '');
  textarea.setAttribute('aria-hidden', 'true');
  textarea.style.position = 'fixed';
  textarea.style.top = '0';
  textarea.style.left = '-9999px';
  document.body.appendChild(textarea);
  const previouslyFocused = document.activeElement as HTMLElement | null;
  try {
    textarea.select();
    textarea.setSelectionRange(0, text.length);
    return document.execCommand('copy');
  } catch {
    return false;
  } finally {
    document.body.removeChild(textarea);
    previouslyFocused?.focus?.();
  }
};

/**
 * Copies text to the clipboard. Returns whether it worked; never throws and never logs the text,
 * which may be a bearer credential.
 */
export const copyText = async (text: string): Promise<boolean> => {
  if (typeof navigator !== 'undefined' && navigator.clipboard?.writeText) {
    try {
      await navigator.clipboard.writeText(text);
      return true;
    } catch {
      // Permission denied or insecure context: fall through to the legacy path.
    }
  }
  return legacyCopy(text);
};

export const canShare = (): boolean =>
  typeof navigator !== 'undefined' && typeof navigator.share === 'function';
