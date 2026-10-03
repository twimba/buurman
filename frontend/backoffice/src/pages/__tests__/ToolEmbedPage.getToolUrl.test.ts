// @vitest-environment jsdom
import { describe, expect, it, afterEach } from 'vitest';
import { getToolUrl } from '../ToolEmbedPage';

// Guards against two bugs in building an embedded tool's URL from the backoffice page's own
// location:
// 1. A per-workspace backoffice hostname (e.g. w2-backoffice.local.buurman.io) losing its
//    "w2-" prefix — the prefix must move onto the subdomain (w2-mailpit.local.buurman.io), not
//    stay stuck to the base domain.
// 2. The per-workspace HTTPS port (e.g. :2443) being dropped entirely — hostname never carries
//    a port, so every tool link silently fell back to the default port 443, which isn't where a
//    non-default workspace's Traefik listens (reported: every sidebar tool link 404ing from
//    https://w2-backoffice.local.buurman.io:2443).

describe('ToolEmbedPage getToolUrl', () => {
  const originalLocation = window.location;

  afterEach(() => {
    Object.defineProperty(window, 'location', {
      value: originalLocation,
      writable: true,
    });
  });

  const setLocation = (hostname: string, port = '') => {
    Object.defineProperty(window, 'location', {
      value: { ...originalLocation, hostname, port, protocol: 'https:' },
      writable: true,
    });
  };

  it('builds the base (unprefixed) tool URL from the backoffice hostname', () => {
    setLocation('backoffice.local.buurman.io');
    expect(getToolUrl('mailpit')).toBe('https://mailpit.local.buurman.io');
  });

  it('preserves the workspace prefix and moves it onto the subdomain', () => {
    setLocation('w2-backoffice.local.buurman.io');
    expect(getToolUrl('mailpit')).toBe('https://w2-mailpit.local.buurman.io');
    expect(getToolUrl('documenso')).toBe(
      'https://w2-documenso.local.buurman.io'
    );
  });

  it('carries over a non-default workspace port instead of dropping it', () => {
    setLocation('w2-backoffice.local.buurman.io', '2443');
    expect(getToolUrl('documenso')).toBe(
      'https://w2-documenso.local.buurman.io:2443'
    );
  });

  it('omits the port suffix when on the default port', () => {
    setLocation('backoffice.local.buurman.io', '');
    expect(getToolUrl('mailpit')).toBe('https://mailpit.local.buurman.io');
  });

  it('carries over a non-default port even without a workspace prefix', () => {
    setLocation('backoffice.local.buurman.io', '2443');
    expect(getToolUrl('mailpit')).toBe(
      'https://mailpit.local.buurman.io:2443'
    );
  });
});
