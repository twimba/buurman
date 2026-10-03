import type { BadgeColorVariant } from '@buurman/ui';
import { LeaseKind } from '../generated/models';

export interface LeaseKindMeta {
  label: string;
  color: BadgeColorVariant;
  oneLine: string;
  /** How a contract ends up with this kind (see LeaseKindResolver on the backend). */
  appliesWhen: string;
  /** Mirror of LeaseKind.fallbackChain(): most specific first, always ends at LEGACY. */
  fallbackChain: LeaseKind[];
  fallbackNote: string;
}

const GOES_TO_LEGACY =
  'Falls back to Legacy when this country has no clause set or document for the kind.';

/**
 * Single source of truth for lease kind help copy. Derived from the backend:
 * - LeaseKindResolver (kind from contract lease regime, property category, unit furnished flag)
 * - LeaseKind.fallbackChain() / LeaseDocumentLocator (kind outranks language when falling back).
 * Update here when either changes; the test fails if a LeaseKind has no entry.
 */
export const LEASE_KIND_META: Record<LeaseKind, LeaseKindMeta> = {
  LEGACY: {
    label: 'Legacy (example text)',
    color: 'gray',
    oneLine:
      'Placeholder lease with example clauses, used when no real document or clause set exists.',
    appliesWhen:
      'Never derived from a contract directly. It is the last step of every fallback chain, so it is used when a country has no clause set or document for the contract’s own kind.',
    fallbackChain: [LeaseKind.LEGACY],
    fallbackNote: 'Final safety net: never falls back further.',
  },
  RESIDENTIAL: {
    label: 'Residential',
    color: 'blue',
    oneLine: 'Standard lease of a home.',
    appliesWhen:
      'The property category is residential (or not set) and the unit is not flagged furnished, and the contract’s lease regime is standard.',
    fallbackChain: [LeaseKind.RESIDENTIAL, LeaseKind.LEGACY],
    fallbackNote: GOES_TO_LEGACY,
  },
  RESIDENTIAL_FURNISHED: {
    label: 'Residential furnished',
    color: 'indigo',
    oneLine: 'Residential lease of a home let furnished.',
    appliesWhen:
      'The property category is residential, the unit’s residential details are flagged furnished, and the contract’s lease regime is standard.',
    fallbackChain: [
      LeaseKind.RESIDENTIAL_FURNISHED,
      LeaseKind.RESIDENTIAL,
      LeaseKind.LEGACY,
    ],
    fallbackNote:
      'Uses the Residential clause set until a furnished-specific one exists, then Legacy.',
  },
  COMMERCIAL: {
    label: 'Commercial',
    color: 'amber',
    oneLine: 'Lease of business premises.',
    appliesWhen:
      'The property category is commercial or industrial, and the contract’s lease regime is standard.',
    fallbackChain: [LeaseKind.COMMERCIAL, LeaseKind.LEGACY],
    fallbackNote: GOES_TO_LEGACY,
  },
  MIXED_USE: {
    label: 'Mixed use',
    color: 'violet',
    oneLine: 'Lease of premises used partly as a home and partly for business.',
    appliesWhen:
      'The property category is mixed use, and the contract’s lease regime is standard.',
    fallbackChain: [LeaseKind.MIXED_USE, LeaseKind.LEGACY],
    fallbackNote: GOES_TO_LEGACY,
  },
  AGRICULTURAL: {
    label: 'Agricultural',
    color: 'green',
    oneLine: 'Lease of farmland or agricultural buildings.',
    appliesWhen:
      'The property category is agricultural, and the contract’s lease regime is standard.',
    fallbackChain: [LeaseKind.AGRICULTURAL, LeaseKind.LEGACY],
    fallbackNote: GOES_TO_LEGACY,
  },
  SHORT_TERM: {
    label: 'Short term',
    color: 'cyan',
    oneLine: 'Lease with the short-term regime.',
    appliesWhen:
      'The contract’s lease regime is short term. This wins over the property category, so a short-term lease of any property type uses this kind.',
    fallbackChain: [LeaseKind.SHORT_TERM, LeaseKind.LEGACY],
    fallbackNote: GOES_TO_LEGACY,
  },
  STUDENT_MOBILITY: {
    label: 'Student / mobility',
    color: 'rose',
    oneLine: 'Lease with the student or mobility regime.',
    appliesWhen:
      'The contract’s lease regime is student or mobility. This wins over the property category (checked right after short term).',
    fallbackChain: [LeaseKind.STUDENT_MOBILITY, LeaseKind.LEGACY],
    fallbackNote: GOES_TO_LEGACY,
  },
};

export const LEASE_KIND_ORDER = Object.values(LeaseKind);

export const fallbackText = (kind: LeaseKind): string =>
  LEASE_KIND_META[kind].fallbackChain
    .slice(1)
    .map((k) => LEASE_KIND_META[k].label)
    .join(' → ') || 'none';
