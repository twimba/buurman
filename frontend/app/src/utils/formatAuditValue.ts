/**
 * Formats an audit log field value for display.
 *
 * Handles objects (e.g. money fields like { amount: 1200, currency: "EUR" }),
 * booleans, nulls, and primitives gracefully instead of producing "[object Object]".
 */
export function formatAuditValue(value: unknown): string {
  if (value == null) {
    return 'N/A';
  }
  if (typeof value === 'boolean') {
    return value ? 'Yes' : 'No';
  }
  if (typeof value === 'object') {
    const obj = value as Record<string, unknown>;

    // Money objects: { amount, currency }
    if ('amount' in obj && 'currency' in obj) {
      const amount = Number(obj.amount);
      const currency = String(obj.currency);
      try {
        return new Intl.NumberFormat(undefined, {
          style: 'currency',
          currency,
        }).format(amount);
      } catch {
        return `${currency} ${amount}`;
      }
    }

    // Generic object fallback: key=value pairs
    const entries = Object.entries(obj);
    if (entries.length === 0) {
      return 'N/A';
    }
    return entries.map(([k, v]) => `${k}: ${String(v)}`).join(', ');
  }
  return String(value);
}
