import { describe, it, expect, vi, beforeEach } from 'vitest';

// Mock the shared axios client so we can assert how customInstance calls it.
const clientMock = vi.fn();
vi.mock('../client', () => ({ default: (cfg: unknown) => clientMock(cfg) }));

import { customInstance } from '../orval-client';

describe('customInstance (orval mutator)', () => {
  beforeEach(() => {
    clientMock.mockReset();
    clientMock.mockResolvedValue({ data: { ok: true } });
  });

  it('unwraps the response data', async () => {
    const result = await customInstance({ url: '/x', method: 'GET' });
    expect(result).toEqual({ ok: true });
  });

  it('passes the generated config through to the client', async () => {
    await customInstance({ url: '/properties', method: 'GET', params: { page: 0 } });
    expect(clientMock).toHaveBeenCalledWith(
      expect.objectContaining({ url: '/properties', method: 'GET' })
    );
    expect(clientMock.mock.calls[0][0].params).toMatchObject({ page: 0 });
  });

  it('merges per-call options params/headers with the generated config', async () => {
    await customInstance(
      { url: '/x', method: 'GET', params: { page: 0 }, headers: { 'X-A': '1' } },
      { params: { size: 50 }, headers: { 'X-B': '2' }, responseType: 'blob' }
    );
    const cfg = clientMock.mock.calls[0][0];
    expect(cfg.params).toEqual({ page: 0, size: 50 });
    expect(cfg.headers).toEqual({ 'X-A': '1', 'X-B': '2' });
    expect(cfg.responseType).toBe('blob');
  });
});
