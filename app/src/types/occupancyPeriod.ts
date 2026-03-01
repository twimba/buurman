// Enums — re-exported from generated
export {
  OccupancyPeriodResponseType as OccupancyType,
  type OccupancyPeriodResponseType,
  OccupancyPeriodResponseEndReason as OccupancyEndReason,
  type OccupancyPeriodResponseEndReason,
} from '../generated/models';

// Interfaces — kept manual (generated adds to optional fields)

import {
  OccupancyPeriodResponseType,
  OccupancyPeriodResponseEndReason,
} from '../generated/models';

// Request interfaces — manual (generated adds to all optional fields)

export interface CreateOccupancyPeriodRequest {
  startDate: string;
  type: OccupancyPeriodResponseType;
  endDate?: string;
  occupantName?: string;
  monthlyImputedRent?: number;
  notes?: string;
}

export interface UpdateOccupancyPeriodRequest {
  startDate?: string;
  type?: OccupancyPeriodResponseType;
  endDate?: string;
  occupantName?: string;
  monthlyImputedRent?: number;
  notes?: string;
}

export interface EndOccupancyPeriodRequest {
  endDate: string;
  endReason?: OccupancyPeriodResponseEndReason;
  notes?: string;
}

export interface OccupancyPeriodResponse {
  identifier: string;
  propertyIdentifier: string;
  startDate: string;
  endDate?: string;
  type: OccupancyPeriodResponseType;
  occupantName?: string;
  monthlyImputedRent?: number;
  endReason?: OccupancyPeriodResponseEndReason;
  notes?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface TimelineEntry {
  type: 'SELF_OCCUPANCY' | 'CONTRACT' | 'VACANCY';
  identifier: string;
  startDate: string;
  endDate?: string;
  description?: string;
  metadata?: string;
}

export interface FinancingTimelineEntry {
  identifier: string;
  startDate: string;
  endDate?: string;
  financingType: string;
  status: string;
  lenderName?: string;
  originalAmount: number;
  originalAmountCurrency: string;
  interestRate?: number;
}

export interface PropertyTimelineResponse {
  acquisitionDate?: string;
  entries: TimelineEntry[];
  financings: FinancingTimelineEntry[];
}

export const OCCUPANCY_TYPE_LABELS: Record<
  OccupancyPeriodResponseType,
  string
> = {
  [OccupancyPeriodResponseType.PERSONAL]: 'Personal',
  [OccupancyPeriodResponseType.FAMILY]: 'Family',
  [OccupancyPeriodResponseType.BUSINESS]: 'Business',
};

export const OCCUPANCY_END_REASON_LABELS: Record<OccupancyPeriodResponseEndReason, string> = {
  [OccupancyPeriodResponseEndReason.CONVERTING_TO_RENTAL]: 'Converting to Rental',
  [OccupancyPeriodResponseEndReason.SELLING]: 'Selling',
  [OccupancyPeriodResponseEndReason.RENOVATION]: 'Renovation',
  [OccupancyPeriodResponseEndReason.OTHER]: 'Other',
};
