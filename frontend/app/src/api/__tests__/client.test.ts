import { describe, it, expect, vi } from 'vitest';

// Avoid instantiating the real Keycloak singleton during the test.
vi.mock('../../config/keycloak', () => ({
  default: { authenticated: false, token: undefined },
}));

import client from '../client';

describe('api client query-param serialization', () => {
  // Spring binds `@RequestParam List<...>` from repeated keys (`tags=a&tags=b`), NOT from
  // axios's default bracket form (`tags[]=a`). Regressing this silently drops list filters
  // (financial-report property filter, contact tag filter).
  it('serializes array params as repeated keys without brackets', () => {
    const uri = client.getUri({
      url: '/contacts',
      params: { tags: ['LANDLORD', 'TENANT'], propertyIdentifiers: ['p1', 'p2'] },
    });

    expect(uri).toContain('tags=LANDLORD&tags=TENANT');
    expect(uri).toContain('propertyIdentifiers=p1&propertyIdentifiers=p2');
    // No `[]` brackets (raw or percent-encoded).
    expect(uri).not.toContain('tags[]');
    expect(uri).not.toContain('%5B%5D');
  });
});
