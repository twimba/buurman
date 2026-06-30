// Thin re-exports of generated models + frontend-only UI label maps.

import {
  OccupancyPeriodResponseType,
  OccupancyPeriodResponseEndReason,
} from '../generated/models';

// Enums re-exported as values (orval enums are const + type of same name).
export {
  OccupancyPeriodResponseType as OccupancyType,
  type OccupancyPeriodResponseType,
  OccupancyPeriodResponseEndReason as OccupancyEndReason,
  type OccupancyPeriodResponseEndReason,
} from '../generated/models';

export type {
  OccupancyPeriodResponse,
  CreateOccupancyPeriodRequest,
  UpdateOccupancyPeriodRequest,
  EndOccupancyPeriodRequest,
  PropertyTimelineResponse,
  TimelineEntry,
  FinancingEntry as FinancingTimelineEntry,
} from '../generated/models';

export const OCCUPANCY_TYPE_LABELS: Record<
  OccupancyPeriodResponseType,
  string
> = {
  [OccupancyPeriodResponseType.PERSONAL]: 'Personal',
  [OccupancyPeriodResponseType.FAMILY]: 'Family',
  [OccupancyPeriodResponseType.BUSINESS]: 'Business',
};

export const OCCUPANCY_END_REASON_LABELS: Record<
  OccupancyPeriodResponseEndReason,
  string
> = {
  [OccupancyPeriodResponseEndReason.CONVERTING_TO_RENTAL]:
    'Converting to Rental',
  [OccupancyPeriodResponseEndReason.SELLING]: 'Selling',
  [OccupancyPeriodResponseEndReason.RENOVATION]: 'Renovation',
  [OccupancyPeriodResponseEndReason.OTHER]: 'Other',
};
