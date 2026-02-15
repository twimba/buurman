// Runtime config (injected by docker-entrypoint.sh) with build-time fallback
const cfg = (window as any).__CONFIG__ || {};
export const env = (key: string): string => cfg[key] || import.meta.env[key] || '';
