import client from './client';

export interface CacheInfo {
  name: string;
  type: string;
  description: string;
  maxSize: number;
  ttlSeconds: number;
  refreshPolicy: string;
  entryCount: number;
  hitCount: number;
  missCount: number;
  hitRate: number;
  loadCount: number;
  averageLoadTimeMs: number;
  evictionCount: number;
  lastInvalidatedAt: string | null;
}

export interface CacheEntry {
  key: string;
  summary: string;
  details: Record<string, unknown>[];
}

export interface CacheDetail extends CacheInfo {
  entries: CacheEntry[];
}

export const cachesApi = {
  list: () => client.get<CacheInfo[]>('/caches'),
  getDetail: (cacheName: string) =>
    client.get<CacheDetail>(`/caches/${cacheName}`),
  invalidate: (cacheName: string) =>
    client.post(`/caches/${cacheName}/invalidate`),
};
