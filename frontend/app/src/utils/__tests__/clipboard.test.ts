import { afterEach, describe, expect, it, vi } from 'vitest';
import { canShare, copyText } from '../clipboard';

const setClipboard = (value: unknown) =>
  Object.defineProperty(navigator, 'clipboard', {
    value,
    configurable: true,
  });

describe('copyText', () => {
  afterEach(() => {
    vi.restoreAllMocks();
    setClipboard(undefined);
    Reflect.deleteProperty(document, 'execCommand');
  });

  it('uses the async clipboard API when available', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);
    setClipboard({ writeText });

    await expect(copyText('hello')).resolves.toBe(true);
    expect(writeText).toHaveBeenCalledWith('hello');
  });

  it('falls back to execCommand when the clipboard API is missing', async () => {
    setClipboard(undefined);
    const execCommand = vi.fn().mockReturnValue(true);
    Object.defineProperty(document, 'execCommand', {
      value: execCommand,
      configurable: true,
    });

    await expect(copyText('hello')).resolves.toBe(true);
    expect(execCommand).toHaveBeenCalledWith('copy');
    expect(document.querySelector('textarea')).toBeNull();
  });

  it('falls back to execCommand when the clipboard API rejects', async () => {
    setClipboard({ writeText: vi.fn().mockRejectedValue(new Error('denied')) });
    const execCommand = vi.fn().mockReturnValue(true);
    Object.defineProperty(document, 'execCommand', {
      value: execCommand,
      configurable: true,
    });

    await expect(copyText('hello')).resolves.toBe(true);
    expect(execCommand).toHaveBeenCalledWith('copy');
  });

  it('returns false when every strategy fails', async () => {
    setClipboard({ writeText: vi.fn().mockRejectedValue(new Error('denied')) });
    Object.defineProperty(document, 'execCommand', {
      value: vi.fn().mockReturnValue(false),
      configurable: true,
    });

    await expect(copyText('hello')).resolves.toBe(false);
    expect(document.querySelector('textarea')).toBeNull();
  });
});

describe('canShare', () => {
  afterEach(() => {
    Reflect.deleteProperty(navigator, 'share');
  });

  it('is false without navigator.share and true with it', () => {
    expect(canShare()).toBe(false);
    Object.defineProperty(navigator, 'share', {
      value: vi.fn(),
      configurable: true,
    });
    expect(canShare()).toBe(true);
  });
});
