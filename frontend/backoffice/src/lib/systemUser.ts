/**
 * The built-in platform account used by automated/system actions. It is not a real person: it must
 * never be disabled or deleted, and the UI marks it explicitly so Buurmies don't mistake it for a
 * customer account. Details remain viewable.
 */
export const SYSTEM_USER_EMAIL = 'system@buurman.io';

/** True for the built-in system account (case-insensitive on email). */
export const isSystemUser = (email?: string | null): boolean =>
  (email ?? '').trim().toLowerCase() === SYSTEM_USER_EMAIL;
