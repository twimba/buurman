/**
 * Extracted from LoginPage.tsx for testability.
 * Tests the sanitizeRedirect logic that prevents open-redirect attacks.
 */
const sanitizeRedirect = (url: string | null): string | null => {
  if (!url) {
    return null;
  }
  try {
    const resolved = new URL(url, window.location.origin);
    if (resolved.origin !== window.location.origin) {
      return null;
    }
    return resolved.pathname + resolved.search + resolved.hash;
  } catch {
    return null;
  }
};

describe('sanitizeRedirect', () => {
  it('returns null for null input', () => {
    expect(sanitizeRedirect(null)).toBeNull();
  });

  it('returns null for empty string', () => {
    expect(sanitizeRedirect('')).toBeNull();
  });

  it('returns path for valid relative URL', () => {
    expect(sanitizeRedirect('/dashboard')).toBe('/dashboard');
  });

  it('returns path for deeper relative URL', () => {
    expect(sanitizeRedirect('/properties/prop_abc123')).toBe(
      '/properties/prop_abc123'
    );
  });

  it('returns null for absolute URL with different origin', () => {
    expect(sanitizeRedirect('https://evil.com/steal')).toBeNull();
  });

  it('returns null for protocol-relative URL', () => {
    expect(sanitizeRedirect('//evil.com/path')).toBeNull();
  });

  it('returns null for javascript: URI', () => {
     
    expect(sanitizeRedirect('javascript:alert(1)')).toBeNull();
  });

  it('returns null for data: URI', () => {
    expect(sanitizeRedirect('data:text/html,<h1>pwned</h1>')).toBeNull();
  });

  it('preserves query strings and hash', () => {
    expect(sanitizeRedirect('/dashboard?tab=1#section')).toBe(
      '/dashboard?tab=1#section'
    );
  });

  it('allows same-origin absolute URL and strips origin', () => {
    // jsdom default origin is http://localhost
    const sameOrigin = `${window.location.origin}/settings`;
    expect(sanitizeRedirect(sameOrigin)).toBe('/settings');
  });
});
