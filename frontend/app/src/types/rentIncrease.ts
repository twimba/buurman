// Re-exported from generated models
export type {
  RentIncreasePreviewResponse,
  RentIncreaseCountrySummary,
  RentIncreaseContractPreview,
  ApplyRentIncreasesRequest,
  RentIncreaseItem,
  ApplyRentIncreasesResponse,
  RentIncreaseResult,
  RentIncreaseSummary,
  RentRegulationRuleResponse,
} from '../generated/models';

import type { RentIncreaseItem as RentIncreaseItemModel } from '../generated/models';

// Frontend-only: wizard carries the computed new rent for display/review.
// The apply request (generated RentIncreaseItem) only sends contractIdentifier,
// increasePercentage and effectiveDate — newRentAmount is derived client-side.
export interface RentIncreaseItemDraft extends RentIncreaseItemModel {
  newRentAmount: number;
}
