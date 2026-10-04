import type { LeaseRegime } from '@/generated/models';

// Regimes a landlord can currently choose. Slice 0 ships only STANDARD; later slices append
// SHORT_TERM / STUDENT_OR_MOBILITY here as the matching clause content lands. The select in
// the contract form stays hidden until more than one regime is available.
export const AVAILABLE_LEASE_REGIMES: readonly LeaseRegime[] = ['STANDARD'];
