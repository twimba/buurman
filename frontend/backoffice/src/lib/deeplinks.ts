/**
 * Typed deep-link helpers for dashboard panels. Backend rows already carry a `deeplink`; these are
 * for panel headers (title-as-link) and "view all" affordances. Target pages may not yet honour
 * every query param — adding those filters is tracked as follow-up work.
 */
const qs = (
  params: Record<string, string | number | boolean | undefined>
): string => {
  const entries = Object.entries(params).filter(([, v]) => v !== undefined);
  if (entries.length === 0) {
    return '';
  }
  const search = new URLSearchParams(
    entries.map(([k, v]) => [k, String(v)])
  ).toString();
  return `?${search}`;
};

export const dl = {
  teams: () => '/teams',
  notifications: (params: { status?: string } = {}) =>
    `/notifications${qs(params)}`,
  scheduler: (params: { jobName?: string; status?: string } = {}) =>
    `/scheduler${qs(params)}`,
  impersonation: (params: { status?: string } = {}) =>
    `/impersonation${qs(params)}`,
  registrationInvitations: () => '/registration-invitations',
};
