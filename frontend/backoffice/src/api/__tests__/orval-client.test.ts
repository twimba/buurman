import { describe, it, expect, vi, beforeEach } from 'vitest';

// Mock the shared axios client so we can assert how customInstance calls it.
const clientMock = vi.fn();
vi.mock('../client', () => ({ default: (cfg: unknown) => clientMock(cfg) }));

import { customInstance } from '../orval-client';

describe('backoffice customInstance (orval mutator)', () => {
  beforeEach(() => {
    clientMock.mockReset();
    clientMock.mockResolvedValue({ data: { ok: true } });
  });

  it('unwraps the response data', async () => {
    const result = await customInstance({ url: '/backoffice/teams', method: 'GET' });
    expect(result).toEqual({ ok: true });
  });

  it('strips the /backoffice prefix (client baseURL already includes it)', async () => {
    await customInstance({ url: '/backoffice/teams', method: 'GET' });
    expect(clientMock.mock.calls[0][0].url).toBe('/teams');
  });

  it('leaves non-prefixed urls untouched', async () => {
    await customInstance({ url: '/teams', method: 'GET' });
    expect(clientMock.mock.calls[0][0].url).toBe('/teams');
  });

  it('merges per-call options params/headers with the generated config', async () => {
    await customInstance(
      { url: '/backoffice/x', method: 'GET', params: { page: 0 }, headers: { 'X-A': '1' } },
      { params: { size: 50 }, headers: { 'X-B': '2' } }
    );
    const cfg = clientMock.mock.calls[0][0];
    expect(cfg.params).toEqual({ page: 0, size: 50 });
    expect(cfg.headers).toEqual({ 'X-A': '1', 'X-B': '2' });
  });
});
