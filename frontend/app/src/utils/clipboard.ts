const legacyCopy = (text: string): boolean => {
  const previouslyFocused = document.activeElement as HTMLElement | null;
  // Inside an open dialog the textarea must live in the dialog: a focus trap (Radix) would
  // otherwise pull focus out of a body-level node and drop the selection mid-copy.
  const container =
    previouslyFocused?.closest<HTMLElement>('[role="dialog"]') ?? document.body;
  const textarea = document.createElement('textarea');
  textarea.value = text;
  textarea.setAttribute('readonly', '');
  textarea.setAttribute('aria-hidden', 'true');
  textarea.style.position = 'fixed';
  textarea.style.top = '0';
  textarea.style.left = '-9999px';
  container.appendChild(textarea);
  try {
    textarea.focus();
    textarea.select();
    textarea.setSelectionRange(0, text.length);
    const selectionIntact =
      textarea.selectionEnd - textarea.selectionStart === text.length;
    return document.execCommand('copy') && selectionIntact;
  } catch {
    return false;
  } finally {
    container.removeChild(textarea);
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
