// Runtime config (injected by docker-entrypoint.sh) with build-time fallback
const cfg =
  (window as unknown as Record<string, Record<string, string>>).__CONFIG__ ??
  {};
export const env = (key: string): string =>
  cfg[key] || import.meta.env[key] || "";
